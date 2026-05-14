# Validação - Lab 05 BlackGate - Fase 1

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
- versão `1.0.0-phase1`.

## Login manual

Abra:

```text
http://localhost:8096
```

Resultado esperado:

- redireciona para `/login`;
- login com `operator / operator123` funciona;
- redireciona para `/dashboard`.

## Rotas autenticadas

Após login, validar no navegador:

```text
/dashboard
/tickets
/assets
/logout
```

Resultado esperado:

- `/dashboard` mostra cards de métricas;
- `/tickets` mostra tabela de tickets;
- `/assets` mostra inventário de ativos;
- `/logout` encerra a sessão.

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

- nenhuma flag final exposta na Fase 1.

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
