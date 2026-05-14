# Validação - Lab 05 BlackGate - Fase 2

## Escopo

```text
http://localhost:8096
```

## Subir o lab

```bash
cd lab-05-blackgate
docker compose up --build
```

Em outro terminal:

```bash
docker compose ps
```

Resultado esperado:

- serviço `blackgate` em execução;
- porta `8096:3000`.

## Health check

```bash
curl -i http://localhost:8096/health
```

Resultado esperado:

- HTTP 200;
- JSON contendo `blackgate`;
- versão `1.1.0-phase2`.

## Enumeração pública

```bash
curl -i http://localhost:8096/robots.txt
curl -i http://localhost:8096/.well-known/security.txt
curl -i http://localhost:8096/api/status
curl -i http://localhost:8096/api/version
curl -i http://localhost:8096/api/client-config
curl -i http://localhost:8096/api/routes
curl -i http://localhost:8096/debug/ping
curl -i -H "X-Debug-Token: guest-debug" http://localhost:8096/debug/ping
```

Resultado esperado:

- endpoints respondem sem login;
- `/api/version` retorna `1.1.0-phase2`;
- debug com header retorna diagnostics limitados;
- nenhum segredo real é exposto.

## Login manual

Abra:

```text
http://localhost:8096
```

Resultado esperado:

- redireciona para `/login`;
- login com `operator / operator123` funciona;
- redireciona para `/dashboard`.

## Login por terminal e APIs autenticadas

```bash
rm -f /tmp/bg-cookie.txt

curl -i -c /tmp/bg-cookie.txt \
  -d "username=guest" \
  -d "password=guest123" \
  -X POST http://localhost:8096/login

curl -i -b /tmp/bg-cookie.txt http://localhost:8096/api/tickets/BG-1004
curl -i -b /tmp/bg-cookie.txt http://localhost:8096/api/tickets/BG-1005
curl -i -b /tmp/bg-cookie.txt http://localhost:8096/api/assets/api-core.internal
```

Resultado esperado:

- login retorna redirecionamento para `/dashboard`;
- tickets com metadata retornam dados limitados;
- asset por hostname retorna metadados;
- não há flag final.

## Rotas autenticadas no navegador

Após login, validar:

```text
/dashboard
/tickets
/assets
/security-policy
/logout
```

Resultado esperado:

- `/dashboard` mostra cards de métricas e cards da Fase 2;
- `/tickets` mostra tabela de tickets e links `API view`;
- `/assets` mostra inventário e links `API view`;
- `/security-policy` mostra política pública fictícia;
- `/logout` encerra a sessão.

## Respostas de erro JSON

Sem cookie, validar:

```bash
curl -i http://localhost:8096/api/tickets/BG-1004
curl -i http://localhost:8096/api/assets/api-core.internal
```

Resultado esperado:

```json
{
  "error": "authentication_required",
  "message": "Login required to access this resource."
}
```

## Docker

Conferir:

```bash
grep -n "8096:3000" docker-compose.yml
grep -n "ports:" docker-compose.yml
```

Resultado esperado:

- porta pública `8096:3000`;
- somente o serviço web exposto.

## Ausência de flags sensíveis

```bash
grep -R "fla[g]{" -n . || true
grep -R "FLA[G]{" -n . || true
```

Resultado esperado:

- nenhuma flag final exposta na Fase 2.

## Arquivos principais

Conferir:

```text
docker-compose.yml
Dockerfile
package.json
src/server.js
src/routes/
src/views/
src/public/css/style.css
src/public/js/app.js
README.md
STUDENT-GUIDE.md
WALKTHROUGH.md
VALIDATION.md
```
