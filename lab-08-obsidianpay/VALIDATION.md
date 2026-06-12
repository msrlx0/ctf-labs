# VALIDATION — Lab 08: ObsidianPay Mobile

Checklist técnico de validação. Todos os comandos assumem que você está na pasta
do lab:

```bash
cd lab-08-obsidianpay
```

> Atalhos:
> - `bash scripts/validate-phase1.sh` — valida a fundação/contrato da Fase 1.
> - `bash scripts/validate-phase2.sh` — valida os contratos e as vulnerabilidades
>   controladas da Fase 2.
>
> Ambos sobem o ambiente, testam de ponta a ponta, derrubam ao final e falham
> com `exit 1` se algo não responder como esperado.

A seção abaixo (1–12) cobre a **Fase 1**. A seção **Fase 2** vem em seguida.

## Fase 10

Para validar a Fase 10 (Secure Vault / local auth), rode:

```bash
bash scripts/validate-phase10.sh
```

O script verifica:
- Existência de `auth/LocalAuthState.kt`, `auth/BiometricGate.kt` e `ui/VaultScreen.kt`.
- Strings-chave: `LocalAuthState`, `BiometricGate`, `Secure Vault`, `ObsidianPay Vault`,
  `Confirm your identity`, `getWeakFallbackPin`, `validateFallbackPin`,
  `weak_pin_fallback_used`, `biometric_capability_checked`, `biometric_prompt_started`,
  `biometric_auth_result`, `local_auth_success`, `local_auth_failed`,
  `vault_unlocked_local`, `vault_locked_local`, `biometric-result-hook`,
  `force-auth-decision-true`, `patch-local-auth-state`.
- Backend: rotas `vault-mobile/status` e `vault-mobile/unlock`, `enableMobileVault`,
  `mobileVaultStatusPath`, `mobileVaultUnlockPath`, `client-asserted`,
  `vault-access-granted`, `server trusts local auth assertion`.
- HomeScreen/MainActivity com `VaultScreen` e `Secure Vault`.
- Sem `FLAG{` em docs públicos e sem `analyst123`/`operator123` em README/STUDENT/app README.
- Sem typos de fases anteriores.
- Labs 1..7 intocados.

## Fase 1

---

## 1. Validar a definição do compose

```bash
docker compose config
```

Deve renderizar o serviço `obsidianpay-api` mapeando `127.0.0.1:8102:8102`.

## 2. Subir o backend

```bash
docker compose up --build -d
```

Aguarde o healthcheck ficar `healthy`:

```bash
docker compose ps
```

## 3. Health check

```bash
curl -s http://127.0.0.1:8102/health
```

Esperado: JSON com `"status": "ok"` e `"expectedPort": 8102`.

## 4. Raiz / identificação

```bash
curl -s http://127.0.0.1:8102/
```

Esperado: JSON identificando `ObsidianPay Mobile API`.

## 5. Login `guest` / `guest123`

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"guest","password":"guest123"}'
```

Esperado: JSON com `token`, `profile` e `featureFlags`.

## 6. Extrair o token

Com `python3` (recomendado, sem depender de `jq`):

```bash
TOKEN=$(curl -s -X POST http://127.0.0.1:8102/api/mobile/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"guest","password":"guest123"}' \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')
echo "$TOKEN"
```

> Alternativa manual: copie o valor do campo `token` da resposta do passo 5 e
> exporte: `export TOKEN='op_...'`.

## 7. Profile com Authorization: Bearer

```bash
curl -s http://127.0.0.1:8102/api/mobile/profile \
  -H "Authorization: Bearer $TOKEN"
```

Esperado: JSON do perfil do `guest` (id 1001, walletId, etc.).

Sem token deve retornar `401`:

```bash
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8102/api/mobile/profile
```

## 8. Config mobile

```bash
curl -s http://127.0.0.1:8102/api/mobile/config
```

Esperado: `apiVersion`, `supportSyncMode`, `legacySupportEndpoint`,
`mobileFeatureFlags` e `warning`.

## 9. Recibo 1001

```bash
curl -s http://127.0.0.1:8102/api/mobile/receipt/1001 \
  -H "Authorization: Bearer $TOKEN"
```

Esperado: JSON do recibo `1001` (dono = guest).

## 10. Support sync (stub legado)

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/support/sync \
  -H 'Content-Type: application/json' \
  -d '{"message":"ping","ticketRef":"OP-SUP-1"}'
```

Esperado: JSON com `accepted: true` e `echo: "ping"`.

## 11. Derrubar o ambiente

```bash
docker compose down
```

---

## 12. Verificação de git

```bash
git status
git diff --stat
```

Confirme que **apenas** arquivos sob `lab-08-obsidianpay/` aparecem como
adicionados e que **nenhum** lab anterior (01–07) foi alterado.

---

## Critérios de aceite (Fase 1)

- [ ] `docker compose config` funciona.
- [ ] `docker compose up --build` sobe a API em `127.0.0.1:8102`.
- [ ] `/health` retorna `status: ok`.
- [ ] Login `guest`/`guest123` funciona.
- [ ] `/profile` com Bearer funciona (e `401` sem token).
- [ ] `/config`, `/receipt/1001` e `/support/sync` respondem.
- [ ] `scripts/validate-phase1.sh` passa.
- [ ] `README.md` e `STUDENT-GUIDE.md` sem flags/solução.
- [ ] Nenhum lab anterior alterado.

---

## Fase 2

Suba o ambiente (se ainda não estiver no ar) e faça login como guest:

```bash
docker compose up --build -d
TOKEN=$(curl -s -X POST http://127.0.0.1:8102/api/mobile/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"guest","password":"guest123"}' \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')
```

### F2.1 — Recibos do usuário

```bash
curl -s http://127.0.0.1:8102/api/mobile/receipts -H "Authorization: Bearer $TOKEN"
curl -s http://127.0.0.1:8102/api/mobile/receipts/1001 -H "Authorization: Bearer $TOKEN"
```

### F2.2 — Recibo por id (autorização a nível de objeto)

```bash
# observe o que volta para um id que não é o seu
curl -s http://127.0.0.1:8102/api/mobile/receipts/1002 -H "Authorization: Bearer $TOKEN"
```

### F2.3 — Cartões

```bash
curl -s http://127.0.0.1:8102/api/mobile/cards -H "Authorization: Bearer $TOKEN"
curl -s http://127.0.0.1:8102/api/mobile/cards/card-analyst-01 -H "Authorization: Bearer $TOKEN"
```

### F2.4 — Atualização de perfil

```bash
curl -s -X PATCH http://127.0.0.1:8102/api/mobile/profile \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"displayName":"Guest X","plan":"privileged","dailyLimit":99999}'
```

### F2.5 — Diagnostics (header de debug)

```bash
# sem header -> 403
curl -s -o /dev/null -w '%{http_code}\n' \
  http://127.0.0.1:8102/api/mobile/support/diagnostics -H "Authorization: Bearer $TOKEN"
# com header -> 200
curl -s http://127.0.0.1:8102/api/mobile/support/diagnostics \
  -H "Authorization: Bearer $TOKEN" -H 'X-Obsidian-Debug: mobile-diagnostics'
```

### F2.6 — Transfer preview / WebView / Legacy routes

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/transfer/preview \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"toUserId":2001,"amount":"10","memo":"teste"}'

curl -s "http://127.0.0.1:8102/api/mobile/webview/support?topic=mobile"

curl -s http://127.0.0.1:8102/api/mobile/legacy/routes -H "Authorization: Bearer $TOKEN"
```

### F2.7 — Vault status (gate por papel)

```bash
# guest (customer) -> 403
curl -s -o /dev/null -w '%{http_code}\n' \
  http://127.0.0.1:8102/api/mobile/internal/vault-status -H "Authorization: Bearer $TOKEN"
```

Derrube ao final: `docker compose down`.

### Critérios de aceite (Fase 2)

- [ ] `scripts/validate-phase1.sh` continua passando.
- [ ] `scripts/validate-phase2.sh` passa.
- [ ] `/receipts` e `/cards` escopam pelo usuário; `/receipts/:id` e `/cards/:id`
      retornam objetos por id.
- [ ] `PATCH /profile` aplica campos enviados (incl. privilegiados).
- [ ] `/support/diagnostics` é `403` sem header e `200` com header correto.
- [ ] `/internal/vault-status` é `403` para customer e `200` para analyst.
- [ ] `README.md` e `STUDENT-GUIDE.md` sem flags (sem o marcador de progresso).

---

## Fase 3 — app Android base

> Atalho: `bash scripts/validate-phase3.sh` valida a estrutura do app. Para
> também rodar os testes de backend, use `RUN_BACKEND_TESTS=1 bash
> scripts/validate-phase3.sh`.

### F3.1 — Estrutura do projeto Android

```bash
ls android-app/settings.gradle android-app/build.gradle android-app/app/build.gradle
ls android-app/app/src/main/AndroidManifest.xml
ls android-app/app/src/main/res/xml/network_security_config.xml
ls android-app/app/src/main/java/com/obsidianpay/mobile/util/Constants.kt
ls android-app/app/src/main/java/com/obsidianpay/mobile/api/ApiClient.kt
ls android-app/app/src/main/java/com/obsidianpay/mobile/storage/InsecureSessionStore.kt
ls android-app/app/src/main/java/com/obsidianpay/mobile/ui/
```

### F3.2 — Conteúdo-chave

```bash
grep -q 'android.permission.INTERNET' android-app/app/src/main/AndroidManifest.xml && echo OK
grep -q '10.0.2.2' android-app/app/src/main/res/xml/network_security_config.xml && echo OK
grep -q 'http://10.0.2.2:8102' android-app/app/src/main/java/com/obsidianpay/mobile/util/Constants.kt && echo OK
grep -q 'SharedPreferences' android-app/app/src/main/java/com/obsidianpay/mobile/storage/InsecureSessionStore.kt && echo OK
grep -qi 'okhttp' android-app/app/src/main/java/com/obsidianpay/mobile/api/ApiClient.kt && echo OK
```

### F3.3 — Build (se houver Gradle + Android SDK)

```bash
cd android-app
./gradlew tasks            # baixa o Gradle na 1ª vez; requer JDK 17+
./gradlew assembleDebug    # requer Android SDK (sdk.dir / ANDROID_HOME)
```

Sem Android SDK, o `assembleDebug` falha apenas em "SDK location not found" — isso
é esperado neste ambiente. Use o Android Studio para o build completo.

### Critérios de aceite (Fase 3)

- [ ] `scripts/validate-phase1.sh` e `scripts/validate-phase2.sh` continuam passando.
- [ ] `scripts/validate-phase3.sh` passa (estrutura Android presente).
- [ ] Manifest declara `INTERNET`; `network_security_config` cobre `10.0.2.2`.
- [ ] `Constants.kt` define `http://10.0.2.2:8102`.
- [ ] App tem as 6 telas e cliente HTTP (OkHttp).
- [ ] `README.md` e `STUDENT-GUIDE.md` sem flags e sem credenciais internas.
- [ ] `git diff --stat` mostra apenas `lab-08-obsidianpay/`.

---

## Fase 4 — armazenamento local inseguro (app)

> Atalho: `bash scripts/validate-phase4.sh` valida a estrutura de storage local.
> Para também rodar os testes de backend: `RUN_BACKEND_TESTS=1 bash
> scripts/validate-phase4.sh`.

### F4.1 — Arquivos de storage

```bash
SRC=android-app/app/src/main/java/com/obsidianpay/mobile/storage
ls $SRC/InsecureSessionStore.kt $SRC/ObsidianLocalDb.kt $SRC/LocalCacheManager.kt
ls android-app/app/src/main/java/com/obsidianpay/mobile/ui/LocalStateScreen.kt
```

### F4.2 — Conteúdo-chave

```bash
grep -q 'SharedPreferences' $SRC/InsecureSessionStore.kt && echo OK
grep -q 'SQLiteOpenHelper'  $SRC/ObsidianLocalDb.kt && echo OK
grep -q 'obsidianpay_local.db' $SRC/ObsidianLocalDb.kt && echo OK
grep -Eq 'cached_receipts|cached_cards|debug_events' $SRC/ObsidianLocalDb.kt && echo OK
grep -Eq 'filesDir|cacheDir|getExternalFilesDir' $SRC/LocalCacheManager.kt && echo OK
```

### F4.3 — UI referencia o estado local

```bash
grep -rqE 'Local State|LocalStateScreen' android-app/app/src/main && echo OK
grep -rq 'Cached Receipts' android-app/app/src/main && echo OK
grep -rq 'Cached Cards'    android-app/app/src/main && echo OK
grep -rq 'Local Artifacts' android-app/app/src/main && echo OK
```

### F4.4 — Build (se houver Gradle + Android SDK)

```bash
cd android-app && ./gradlew assembleDebug   # requer Android SDK
```

Sem SDK, o build falha apenas em "SDK location not found" (esperado).

### Inspeção em runtime (apenas com app instalado no emulador)

Depois de usar o app, os artefatos locais ficam sob o sandbox do app, ex.:

```bash
adb shell run-as com.obsidianpay.mobile.debug ls -R /data/data/com.obsidianpay.mobile.debug/
```

> O que observar lá faz parte do exercício; este documento não entrega extração.

### Critérios de aceite (Fase 4)

- [ ] `validate-phase1/2/3.sh` continuam passando.
- [ ] `validate-phase4.sh` passa.
- [ ] App tem SharedPreferences + SQLiteOpenHelper + arquivos locais/cache.
- [ ] UI expõe Local State / Cached Receipts / Cached Cards / Local Artifacts.
- [ ] Docs públicos sem flags; README/STUDENT-GUIDE/app README sem credenciais internas.
- [ ] `git diff --stat` mostra apenas `lab-08-obsidianpay/`.

---

## Fase 5 — deep links, QR e WebView

> Atalho: `bash scripts/validate-phase5.sh` (estrutura). Backend opcional com
> `RUN_BACKEND_TESTS=1`.

### F5.1 — Arquivos

```bash
SRC=android-app/app/src/main/java/com/obsidianpay/mobile
ls $SRC/deeplink/DeepLinkRouter.kt $SRC/ui/QrInputScreen.kt $SRC/ui/WebViewSupportScreen.kt
```

### F5.2 — Manifest e conteúdo-chave

```bash
grep -E 'obsidianpay|transfer|support|receipt|VIEW|BROWSABLE' android-app/app/src/main/AndroidManifest.xml
grep -rqE 'deep_link_opened|qr_payload_processed|webview_support_opened' $SRC && echo OK
grep -rq 'javaScriptEnabled' $SRC && echo OK
grep -rq '10.0.2.2:8102' $SRC || grep -rq 'DEFAULT_BASE_URL' $SRC && echo OK
grep -rq '/api/mobile/webview/support' $SRC && echo OK
```

### F5.3 — UI

```bash
grep -rq 'QR Payment'      $SRC && echo OK
grep -rq 'Process Payload' $SRC && echo OK
grep -rq 'Open Web Support' $SRC && echo OK
```

### F5.4 — Backend WebView support (com backend no ar)

```bash
curl -s "http://127.0.0.1:8102/api/mobile/webview/support?topic=mobile&message=hello"
```

Esperado: HTML com "ObsidianPay mobile support portal", `topic` e `message` refletidos.

### F5.5 — Deep link via adb (app instalado no emulador)

```bash
adb shell am start -a android.intent.action.VIEW \
  -d "obsidianpay://transfer?toUserId=2001&amount=10&memo=test" \
  com.obsidianpay.mobile.debug
```

### Critérios de aceite (Fase 5)

- [ ] `validate-phase1/2/3/4.sh` continuam passando.
- [ ] `validate-phase5.sh` passa.
- [ ] `DeepLinkRouter`, `QrInputScreen`, `WebViewSupportScreen` criados.
- [ ] Manifest aceita `obsidianpay://transfer/support/receipt`.
- [ ] Eventos locais `deep_link_opened/qr_payload_processed/webview_support_opened`.
- [ ] Docs públicos sem flags; sem credenciais internas em README/STUDENT/app README.
- [ ] `git diff --stat` mostra apenas `lab-08-obsidianpay/`.

---

## Fase 6 — WebView JavaScript bridge

> Atalho: `bash scripts/validate-phase6.sh` (estrutura). Backend anterior
> opcional com `RUN_BACKEND_TESTS=1`.
>
> O script também valida **typos comuns de bridge/WebView** que quebrariam o
> build Android sem aparecer numa checagem de string simples: `getSessionSummar`
> (falta o `y`), `@JavascriptInterfac` (falta o `e`) e `webVieClient` (em vez de
> `webViewClient`). As checagens usam regex com limites para não casar com os
> nomes corretos, e confirmam positivamente `fun getSessionSummary`,
> `@JavascriptInterface`, `logBridgeEvent`, `ObsidianBridge` e
> `addJavascriptInterface`. Qualquer typo crítico faz o script sair com exit 1.

### F6.1 — Arquivos

```bash
SRC=android-app/app/src/main/java/com/obsidianpay/mobile
ls $SRC/webview/ObsidianSupportBridge.kt $SRC/ui/WebViewSupportScreen.kt
```

### F6.2 — Bridge e WebView (conteúdo-chave)

```bash
grep -q '@JavascriptInterface' $SRC/webview/ObsidianSupportBridge.kt && echo OK
grep -Eq 'getSessionSummary|getCachedProfile|getCachedConfig|getBridgeInfo|logBridgeEvent' \
  $SRC/webview/ObsidianSupportBridge.kt && echo OK
grep -q 'addJavascriptInterface' $SRC/ui/WebViewSupportScreen.kt && echo OK
grep -q 'ObsidianBridge' $SRC/ui/WebViewSupportScreen.kt && echo OK
grep -q 'webViewClient' $SRC/ui/WebViewSupportScreen.kt && echo OK
# o typo NÃO deve existir:
grep -q 'webVieClient' $SRC/ui/WebViewSupportScreen.kt && echo 'TYPO!' || echo 'sem typo'
```

### F6.3 — Backend support portal (com backend no ar)

```bash
curl -s "http://127.0.0.1:8102/api/mobile/webview/support?topic=mobile&message=hello"
```

Esperado: HTML "ObsidianPay · Mobile Support Portal" que reflete `topic`/`message`
e contém o JavaScript que detecta `window.ObsidianBridge` (botões
`getBridgeInfo`/`getSessionSummary`/`getCachedConfig`).

### F6.4 — Eventos da bridge (app instalado no emulador)

Depois de abrir o **Web Support** e tocar nos botões de diagnóstico, a tela
interna **Local State** lista eventos como `webview_bridge_attached`,
`webview_bridge_called`, `bridge_get_cached_config`, etc.

### Critérios de aceite (Fase 6)

- [ ] `validate-phase1/2/3/4/5.sh` continuam passando.
- [ ] `validate-phase6.sh` passa.
- [ ] `webview/ObsidianSupportBridge.kt` criada com `@JavascriptInterface`.
- [ ] WebView usa `addJavascriptInterface(bridge, "ObsidianBridge")` (JS/DOM on).
- [ ] Sem o typo `webVieClient` na WebView.
- [ ] Backend reconhece `ObsidianBridge`/`getBridgeInfo`/`Mobile Support Portal`.
- [ ] Bridge e docs públicos sem `FLAG{` e sem credenciais internas.
- [ ] `git diff --stat` mostra apenas `lab-08-obsidianpay/`.

## Fase 7 — Componentes Android exportados

> Atalho: `bash scripts/validate-phase7.sh` (estrutura). Esse script também
> reforça os typos de bridge da Fase 6 (`getSessionSummar`, `@JavascriptInterfac`,
> `webVieClient`) com regex que **não** casa com os nomes corretos.

### F7.1 — Arquivos do pacote `platform/`

```bash
SRC=android-app/app/src/main/java/com/obsidianpay/mobile
ls $SRC/platform/InternalOpsActivity.kt \
   $SRC/platform/DebugCommandReceiver.kt \
   $SRC/platform/ObsidianNotesProvider.kt
```

### F7.2 — Manifest (componentes exportados)

```bash
M=android-app/app/src/main/AndroidManifest.xml
grep -q '.platform.InternalOpsActivity' $M && echo OK
grep -q '.platform.DebugCommandReceiver' $M && echo OK
grep -q '.platform.ObsidianNotesProvider' $M && echo OK
grep -q 'com.obsidianpay.mobile.INTERNAL_OPS' $M && echo OK
grep -q 'com.obsidianpay.mobile.DEBUG_COMMAND' $M && echo OK
grep -q 'com.obsidianpay.mobile.provider.notes' $M && echo OK
```

### F7.3 — Código (extras, eventos, provider seguro)

```bash
grep -Eq 'INTERNAL_ROUTE|SESSION_HINT|OPERATOR_MODE|RECEIPT_ID' $SRC/platform/*.kt && echo OK
grep -Eq 'exported_activity_opened|exported_receiver_called' $SRC/platform/*.kt && echo OK
grep -q 'MatrixCursor' $SRC/platform/ObsidianNotesProvider.kt && echo OK
# o provider só devolve preview do token, nunca o token inteiro:
grep -q 'token_preview' $SRC/platform/ObsidianNotesProvider.kt && echo OK
grep -q 'getSafeDebugValuesForProvider' $SRC/storage/InsecureSessionStore.kt && echo OK
```

### F7.4 — Efeitos no app (emulador, app instalado)

Com o app instalado, dispare os componentes por `adb` (exemplos completos no
`WALKTHROUGH.md`, documento de instrutor) e observe a tela interna **Local
State**: aparecem eventos `exported_*`/`external_debug_*`, `operatorHint` e o
`lastExportedEvent`. O `/debug` do provider mostra `token_preview` (mascarado),
nunca o token completo.

### Critérios de aceite (Fase 7)

- [ ] `validate-phase1/2/3/4/5/6.sh` continuam passando.
- [ ] `validate-phase7.sh` passa.
- [ ] `platform/InternalOpsActivity.kt` (Activity exportada) criada.
- [ ] `platform/DebugCommandReceiver.kt` (Receiver exportado) criado.
- [ ] `platform/ObsidianNotesProvider.kt` (Provider exportado) criado.
- [ ] Manifest com actions `INTERNAL_OPS`/`DEBUG_COMMAND` e authority `provider.notes`.
- [ ] Provider devolve apenas `token_preview` (nunca o token completo).
- [ ] Sem `FLAG{` em docs públicos e nos componentes.
- [ ] Sem `analyst123`/`operator123` em README/STUDENT-GUIDE/app README.
- [ ] `git diff --stat` mostra apenas `lab-08-obsidianpay/`.

## Fase 8 — Hardcoded secrets / weak crypto / Device Trust

> Atalho: `bash scripts/validate-phase8.sh` (estrutura). Reforça também os typos
> de bridge da Fase 6.
>
> O script valida ainda typos comuns de `LegacyRequestSigner`/`WeakCrypto` que
> quebrariam o build Android: falha em `LegacyRequestSigne`, `WeakCryptosha1Hex`
> e `WeakCryptomd5Hex`; exige `object LegacyRequestSigner`, a chamada qualificada
> `WeakCrypto.sha1Hex`/`WeakCrypto.md5Hex` e o `import` de `LegacyRequestSigner`
> em `DeviceTrustScreen.kt`.

### F8.1 — Arquivos do pacote `security/` + tela

```bash
SRC=android-app/app/src/main/java/com/obsidianpay/mobile
ls $SRC/security/HardcodedSecrets.kt \
   $SRC/security/WeakCrypto.kt \
   $SRC/security/LegacyRequestSigner.kt \
   $SRC/ui/DeviceTrustScreen.kt
```

### F8.2 — Código (segredos, cripto fraca, headers, eventos)

```bash
grep -Eq 'INTERNAL_CLIENT_PART|LEGACY_SIGNING_SALT' $SRC/security/HardcodedSecrets.kt && echo OK
grep -Eq 'base64Encode|base64Decode|weakXor' $SRC/security/WeakCrypto.kt && echo OK
grep -Eq 'sha1Hex|md5Hex' $SRC/security/WeakCrypto.kt && echo OK
grep -Eq 'X-Obsidian-(Client|Device|Timestamp|Signature)' $SRC/security/LegacyRequestSigner.kt && echo OK
grep -Eq 'device_trust_check_started|weak_signature_generated|device_trust_response_cached|encoded_hint_decoded' \
  $SRC/ui/DeviceTrustScreen.kt && echo OK
```

### F8.3 — Backend device-trust (com backend no ar)

```bash
SALT='obsidian-legacy-attestation-2026'
TS=1700000000000
SIG=$(printf '%s' "guest:android-emulator-obsidian:$TS:$SALT" | sha1sum | cut -d' ' -f1)
curl -s -X POST http://127.0.0.1:8102/api/mobile/internal/device-trust \
  -H "Authorization: Bearer obsidian-mobile-token-guest-1001" \
  -H "X-Obsidian-Client: obsidian-mobile-legacy-client" \
  -H "X-Obsidian-Device: android-emulator-obsidian" \
  -H "X-Obsidian-Timestamp: $TS" -H "X-Obsidian-Signature: $SIG" \
  -H 'Content-Type: application/json' \
  -d '{"deviceId":"android-emulator-obsidian","attestationMode":"legacy","operatorHint":"x"}'
```

Esperado: JSON com `"status":"trusted-legacy"` e `"mode":"legacy-attestation"`.
Assinatura errada → 403. `GET /api/mobile/internal/reverse-hint` com o
`X-Obsidian-Client` correto retorna a dica didática (sem flag).

### Critérios de aceite (Fase 8)

- [ ] `validate-phase1..7.sh` continuam passando.
- [ ] `validate-phase8.sh` passa.
- [ ] `security/HardcodedSecrets.kt`, `WeakCrypto.kt`, `LegacyRequestSigner.kt` criados.
- [ ] `ui/DeviceTrustScreen.kt` criado e acessível pela Início.
- [ ] Backend implementa `POST /internal/device-trust` (assinatura SHA-1 fraca) e `GET /internal/reverse-hint`.
- [ ] `config`/`legacy/routes` referenciam os paths internos.
- [ ] Sem `FLAG{` em docs públicos e nas novas classes/endpoints.
- [ ] Sem `analyst123`/`operator123` em README/STUDENT-GUIDE/app README e nas classes `security/`.
- [ ] `git diff --stat` mostra apenas `lab-08-obsidianpay/`.

---

## Fase 9 — ambiente / environment check

### F9.1 — Arquivos e pacote

```bash
SRC="android-app/app/src/main/java/com/obsidianpay/mobile"
ls $SRC/environment/RootDetector.kt
ls $SRC/environment/EmulatorDetector.kt
ls $SRC/environment/EnvironmentRiskEngine.kt
ls $SRC/ui/SecurityCheckScreen.kt
```

### F9.2 — Código (detectores, eventos, bypass hints)

```bash
grep -qF 'test-keys' $SRC/environment/RootDetector.kt && echo OK
grep -qF 'com.topjohnwu.magisk' $SRC/environment/RootDetector.kt && echo OK
grep -qF 'goldfish' $SRC/environment/EmulatorDetector.kt && echo OK
grep -qF 'ranchu' $SRC/environment/EmulatorDetector.kt && echo OK
grep -qF 'bypassHintId' $SRC/environment/EnvironmentRiskEngine.kt && echo OK
grep -qF 'env-check-local-only' $SRC/environment/EnvironmentRiskEngine.kt && echo OK
grep -qF 'hooks-change-return-values' $SRC/environment/EnvironmentRiskEngine.kt && echo OK
grep -qF 'patch-risk-engine-result' $SRC/environment/EnvironmentRiskEngine.kt && echo OK
grep -qF 'Security Check' $SRC/ui/SecurityCheckScreen.kt && echo OK
grep -qF 'environment_check_started' $SRC/ui/SecurityCheckScreen.kt && echo OK
grep -qF 'root_detection_completed' $SRC/ui/SecurityCheckScreen.kt && echo OK
grep -qF 'emulator_detection_completed' $SRC/ui/SecurityCheckScreen.kt && echo OK
grep -qF 'environment_risk_calculated' $SRC/ui/SecurityCheckScreen.kt && echo OK
grep -qF 'environment_report_sent' $SRC/ui/SecurityCheckScreen.kt && echo OK
grep -qF 'environment_report_cached' $SRC/storage/LocalCacheManager.kt && echo OK
```

### F9.3 — Backend (com backend no ar)

```bash
# deve receber o report e retornar monitor-only
TOKEN="obsidian-mobile-token-guest-1001"
curl -s -X POST http://127.0.0.1:8102/api/mobile/internal/environment-report \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"root":false,"emulator":true,"rootScore":0,"emulatorScore":2,"riskLevel":"medium","signals":["hardware:goldfish"],"bypassHintId":"env-check-local-only"}'
```

Esperado: `"status":"received"` e `"serverPolicy":"monitor-only"` e
`"nextStepHint":"client-side checks are advisory in this lab"`.

### Critérios de aceite (Fase 9)

- [ ] `validate-phase1..8.sh` continuam passando.
- [ ] `validate-phase9.sh` passa.
- [ ] `environment/RootDetector.kt`, `EmulatorDetector.kt`, `EnvironmentRiskEngine.kt` criados.
- [ ] `ui/SecurityCheckScreen.kt` criado e acessível pela Início.
- [ ] Backend implementa `POST /internal/environment-report` (monitor-only).
- [ ] `data.js` tem `enableEnvironmentChecks` e `environmentReportPath`.
- [ ] Sem `FLAG{` em docs públicos e nas novas classes.
- [ ] Sem `analyst123`/`operator123` em README/STUDENT-GUIDE/app README e nas classes `environment/`.
- [ ] `git diff --stat` mostra apenas `lab-08-obsidianpay/`.

---

## Fase 11 — Network Security / Certificate Pinning Scaffold

### F11.1 — Arquivos

```bash
SRC="android-app/app/src/main/java/com/obsidianpay/mobile"
test -f $SRC/network/NetworkSecurityProfile.kt && echo OK
test -f $SRC/network/PinningPolicy.kt && echo OK
test -f $SRC/ui/ApiHostOverrideScreen.kt && echo OK
```

### F11.2 — Código (strings-chave)

```bash
grep -rqF 'DEFAULT_EMULATOR_BASE_URL' $SRC/network/ && echo OK
grep -rqF 'DEFAULT_LOCALHOST_BASE_URL' $SRC/network/ && echo OK
grep -rqF 'SAMPLE_PHONE_BASE_URL' $SRC/network/ && echo OK
grep -rqF 'cleartext-local' $SRC/network/ && echo OK
grep -rqF 'burp-proxy-ready' $SRC/network/ && echo OK
grep -rqF 'pinning-scaffold' $SRC/network/ && echo OK
grep -rqF 'trust-user-ca' $SRC/network/ && echo OK
grep -rqF 'okhttp-certificate-pinner-hook' $SRC/network/ && echo OK
grep -rqF 'network-config-cleartext-override' $SRC/network/ && echo OK
grep -rqF 'CertificatePinner' $SRC/api/ && echo OK
grep -rqF 'user-ca-not-trusted-by-default' $SRC/network/ && echo OK
grep -rqF 'report-only' $SRC/network/ && echo OK
grep -rqF 'setBaseUrlForSession' $SRC/api/ && echo OK
grep -rqF 'getNetworkProfile' $SRC/api/ && echo OK
grep -rqF 'api_base_url_override_saved' $SRC/ui/ApiHostOverrideScreen.kt && echo OK
grep -rqF 'api_base_url_override_cleared' $SRC/ui/ApiHostOverrideScreen.kt && echo OK
grep -rqF 'network_profile_fetched' $SRC/ui/ApiHostOverrideScreen.kt && echo OK
grep -rqF 'pinning_mode_observed' $SRC/ui/ApiHostOverrideScreen.kt && echo OK
grep -rqF 'API Host' $SRC/ui/ApiHostOverrideScreen.kt && echo OK
```

### F11.3 — Backend (com backend no ar)

```bash
TOKEN="obsidian-mobile-token-guest-1001"
curl -s http://127.0.0.1:8102/api/mobile/internal/network-profile \
  -H "Authorization: Bearer $TOKEN"
```

Esperado: `"status":"ok"` e `"pinningMode":"report-only"` e `"cleartextAllowed":true` e
array `"bypassHintIds"` contendo `"trust-user-ca"`.

### Critérios de aceite (Fase 11)

- [ ] `validate-phase1..10.sh` continuam passando.
- [ ] `validate-phase11.sh` passa.
- [ ] `network/NetworkSecurityProfile.kt` e `network/PinningPolicy.kt` criados.
- [ ] `ui/ApiHostOverrideScreen.kt` criado e acessível pela Início.
- [ ] Backend implementa `GET /api/mobile/internal/network-profile` (auth required).
- [ ] `data.js` tem `networkProfileConfig` com `enableNetworkProfile`, `pinningMode`, `cleartextAllowed`.
- [ ] Sem `FLAG{` em docs públicos e nas novas classes `network/` e `ApiHostOverrideScreen`.
- [ ] Sem `analyst123`/`operator123` em README/STUDENT-GUIDE/app README.
- [ ] `git diff --stat` mostra apenas `lab-08-obsidianpay/`.

---

## Fase 12

Para validar a Fase 12 (App Integrity / NativeGate / TamperCheck), rode:

```bash
bash scripts/validate-phase12.sh
```

O script verifica:
- Existência de `integrity/NativeGate.kt`, `integrity/TamperCheck.kt` e `ui/IntegrityScreen.kt`.
- Strings-chave: `obsidian_native_gate`, `native-library-missing-fallback`, `jni-return-value-hook`,
  `patch-native-gate-result`, `strings-libobsidian-native`, `native-gate-kotlin-fallback`,
  `debuggable-build`, `unknown-installer`, `signature-hash-observed`, `package-name-check`,
  `tamper-score`, `patch-debuggable-check`, `hook-package-manager`, `repackage-signature-mismatch`.
- Eventos: `integrity_check_started`, `tamper_check_completed`, `native_gate_checked`,
  `tamper_score_calculated`, `native_gate_hint_viewed`, `integrity_report_sent`,
  `integrity_report_cached`, `integrity_state_cleared`.
- ApiClient, Constants e storage com as novas chaves.
- Backend com endpoint `POST /api/mobile/internal/app-integrity`.
- `data.js` com `appIntegrityConfig` (`enableAppIntegrity`, `integrityPolicy`, `nativeGatePolicy`).
- Docs públicos sem `FLAG{` e sem `analyst123`/`operator123`.

### F12.1 — Estrutural (sem backend)

```bash
bash scripts/validate-phase12.sh
```

Esperado: todos os checks PASS, sem necessidade de Android SDK ou NDK.

### F12.2 — Backend (com backend no ar)

```bash
TOKEN="obsidian-mobile-token-guest-1001"
curl -s -X POST http://127.0.0.1:8102/api/mobile/internal/app-integrity \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"tamperScore":0,"debuggable":true,"installerPackage":"unknown","packageNameStatus":"package-name-check:match","signatureHashPreview":"signature-hash-observed:abc123","nativeLibraryLoaded":false,"nativeGateStatus":"native-library-missing-fallback","bypassHintIds":[]}'
```

Esperado: `"status":"received"`, `"integrityDecision":"accepted"`, `"integrityPolicy":"report-only"`,
`"nativeGatePolicy":"fallback-allowed"`, `"serverTrust":"client-asserted-integrity"`.

### Critérios de aceite (Fase 12)

- [ ] `validate-phase1..11.sh` continuam passando.
- [ ] `validate-phase12.sh` passa.
- [ ] `integrity/NativeGate.kt` e `integrity/TamperCheck.kt` criados.
- [ ] `ui/IntegrityScreen.kt` criado e acessível pela Início.
- [ ] Backend implementa `POST /api/mobile/internal/app-integrity` (auth required).
- [ ] `data.js` tem `appIntegrityConfig` com `enableAppIntegrity`, `integrityPolicy`, `nativeGatePolicy`.
- [ ] Sem `FLAG{` em docs públicos e nas novas classes `integrity/` e `IntegrityScreen`.
- [ ] Sem `analyst123`/`operator123` em README/STUDENT-GUIDE/app README.
- [ ] `git diff --stat` mostra apenas `lab-08-obsidianpay/`.

---

## Fase 13 — Dynamic Instrumentation Scaffold

> Atalho: `bash scripts/validate-phase13.sh` (estrutura). Não exige Frida,
> adb, Android SDK nem dispositivo conectado.

### F13.1 — Estrutural (sem backend, sem dispositivo)

```bash
bash scripts/validate-phase13.sh
```

Esperado: todos os checks PASS.

### F13.2 — Verificar scripts Frida

```bash
ls tools/frida/
# esperado: README.md + 5 scripts .js
```

### F13.3 — Verificar playbook ADB

```bash
ls tools/adb/
# esperado: README.md + lab08-adb-playbook.sh
```

### F13.4 — Verificar docs de pentest

```bash
ls docs/mobile-pentest/
# esperado: SETUP.md + PLAYBOOK.md + INSTRUCTOR-NOTES.md
```

### Critérios de aceite (Fase 13)

- [ ] `validate-phase1..12.sh` continuam passando.
- [ ] `validate-phase13.sh` passa.
- [ ] `docs/mobile-pentest/SETUP.md`, `PLAYBOOK.md` e `INSTRUCTOR-NOTES.md` criados.
- [ ] `tools/frida/README.md` e 5 scripts `.js` criados.
- [ ] `tools/adb/README.md` e `lab08-adb-playbook.sh` criados.
- [ ] Scripts Frida contêm `Java.perform`, `[ObsidianPay Lab]` e hint IDs.
- [ ] Sem `FLAG{` em docs públicos e em tools/.
- [ ] Sem `analyst123`/`operator123` em README/STUDENT-GUIDE/app README.
- [ ] `git diff --stat` mostra apenas `lab-08-obsidianpay/`.

## Fase 14 — Final Challenge Chain

> Atalho: `bash scripts/validate-phase14.sh`. Faz validação estrutural sempre e,
> se houver Docker, sobe o backend e exercita a cadeia. Não exige Android SDK.

### F14.1 — Estrutural + dinâmico

```bash
bash scripts/validate-phase14.sh
```

Esperado: todos os checks PASS.

### F14.2 — Arquivos obrigatórios

```bash
ls -la api/src/flags.js api/src/challenge-chain.js \
       docs/CHALLENGE-SCORING.md scripts/validate-phase14.sh
```

### F14.3 — Exercitar a cadeia (com backend de pé)

```bash
docker compose up --build -d
TOKEN=$(curl -s -X POST http://127.0.0.1:8102/api/mobile/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"guest","password":"guest123"}' | jq -r .token)

curl -s http://127.0.0.1:8102/api/mobile/challenge/progress \
  -H "Authorization: Bearer $TOKEN" | jq .chainId
# => "obsidianpay-mobile-final-chain"  (sem flags na resposta)

curl -s http://127.0.0.1:8102/api/mobile/challenge/scoreboard \
  -H "Authorization: Bearer $TOKEN" | jq '{totalStages, completionPercent}'
docker compose down
```

### Critérios de aceite (Fase 14)

- [ ] `validate-phase1..13.sh` continuam passando.
- [ ] `validate-phase14.sh` passa.
- [ ] `api/src/flags.js`, `api/src/challenge-chain.js`, `docs/CHALLENGE-SCORING.md` e `scripts/validate-phase14.sh` criados.
- [ ] `server.js` registra `challenge/progress`, `challenge/submit`, `challenge/scoreboard`, `internal/finalize-operator`.
- [ ] `flags.js` contém as 9 flags `FLAG{obsidianpay_...}`.
- [ ] `challenge-chain.js` referencia apenas `flagKey` (sem valores de flag).
- [ ] `WALKTHROUGH.md` contém a seção "Fase 14 — Final Challenge Chain" com as flags.
- [ ] Sem `FLAG{` em docs públicos e em tools/; sem `analyst123`/`operator123` em README/STUDENT-GUIDE/app README/tools.
- [ ] `git diff --stat` mostra apenas `lab-08-obsidianpay/`.

## Fase 15 — Documentação final (walkthrough, guia do aluno, scoring)

> A Fase 15 não altera o backend nem o app: é o **passe final de documentação**.
> Entrega o `WALKTHROUGH.md` manual completo (instrutor), o `STUDENT-GUIDE.md` sem
> spoilers, o `README.md` final, o `PLAYBOOK.md` alinhado à cadeia, o
> `CHALLENGE-SCORING.md` mais útil e os guards anti-leak reforçados.
>
> Atalho: `bash scripts/validate-phase15.sh`. Roda sempre a validação estrutural,
> chama `validate-phase14.sh` internamente e tenta os testes dinâmicos de Docker
> via Fase 14. Não exige Android SDK.

### F15.1 — Validação da Fase 15

```bash
bash scripts/validate-phase15.sh
```

Esperado: todos os checks PASS (e a Fase 14 embutida também).

### F15.2 — Validação de docs (conteúdo)

O script confere que:

- `WALKTHROUGH.md` contém `Stage 01`..`Stage 09`, `Final Operator Chain`,
  `/api/mobile/challenge/submit` e a flag final do estágio 9 (material de
  instrutor).
- `STUDENT-GUIDE.md` contém `Objetivo final`, `Como registrar progresso`,
  `/api/mobile/challenge/progress`, `/api/mobile/challenge/submit` e
  `<flag_redacted>`.
- `README.md` contém `Lab 08`, `ObsidianPay Mobile`, `8102`, `guest / guest123`
  e aponta para `STUDENT-GUIDE.md`.
- `docs/CHALLENGE-SCORING.md` contém `completionPercent`, `finalUnlocked`,
  `idempotente` e `<flag_redacted>`.
- `docs/mobile-pentest/PLAYBOOK.md` contém `stage-01`, `stage-09`,
  `dynamic instrumentation`, `ContentProvider`, `WebView bridge` e `App Integrity`.

### F15.3 — Anti-leak (sem FLAG{ em docs públicos)

O script rejeita `FLAG{` em: `README.md`, `STUDENT-GUIDE.md`, `docs/ARCHITECTURE.md`,
`docs/PHASES.md`, `docs/VULNERABILITY-ROADMAP.md`, `docs/CHALLENGE-SCORING.md`,
`docs/mobile-pentest/SETUP.md`, `docs/mobile-pentest/PLAYBOOK.md`,
`android-app/README.md` e `tools/`.

`FLAG{` é permitido apenas em: `WALKTHROUGH.md`, `api/src/flags.js`,
`scripts/validate-phase14.sh` e `scripts/validate-phase15.sh`.

Também rejeita `analyst123`/`operator123` em `README.md`, `STUDENT-GUIDE.md`,
`android-app/README.md` e `tools/`.

### F15.4 — Scripts e typos

O script confere que `scripts/validate-phase1.sh`..`validate-phase14.sh` existem e
re-verifica os typos conhecidos (`network-config-cleartext-overrie`, `PinningPolicyy`,
`TamperCheckk`, `getSessionSummay`, `getSessionSummar`, `@JavascriptInterfac`,
`webVieClient`, `LegacyRequestSigne`, `WeakCryptosha1Hex`, `WeakCryptomd5Hex`,
`apiClientsetBaseUrlForSession`, `getLastNativeGatStatus`, `App Integrit`).

### F15.5 — Docker dynamic tests e Android SDK

- Os testes dinâmicos (login `guest`/`guest123` + progress/submit/scoreboard) são
  herdados da Fase 14 (rodam apenas se Docker estiver disponível — best-effort).
- Android SDK continua **best-effort**: a ausência do SDK não falha a Fase 15.

### Critérios de aceite (Fase 15)

- [ ] `validate-phase1..14.sh` continuam passando.
- [ ] `validate-phase15.sh` passa (inclui a Fase 14 embutida).
- [ ] `WALKTHROUGH.md` traz o walkthrough manual completo (Stages 01–09 + final), com flags.
- [ ] `STUDENT-GUIDE.md` cobre objetivo final, progresso/submit e troubleshooting — **sem** solução/flags.
- [ ] `README.md` aponta para `STUDENT-GUIDE.md` e marca `WALKTHROUGH.md` como instrutor.
- [ ] `docs/CHALLENGE-SCORING.md` e `docs/mobile-pentest/PLAYBOOK.md` alinhados à cadeia, sem flags.
- [ ] Sem `FLAG{` em docs públicos/tools; sem `analyst123`/`operator123` em README/STUDENT-GUIDE/app README/tools.
- [ ] Nenhum lab 1..7 alterado; `git diff --stat` mostra apenas `lab-08-obsidianpay/`.

---

## Fase 16 — QA final / release readiness

> A Fase 16 não altera o backend nem o app: é o **QA final consolidado** antes do
> build real do APK. Entrega `docs/FINAL-QA.md` (matriz de validação + checklist
> de release), `docs/ANDROID-BUILD-CHECKLIST.md` (build no Android Studio) e o
> script `scripts/validate-phase16.sh`, que roda `validate-phase14.sh` e
> `validate-phase15.sh` internamente. **Não exige Android SDK.**
>
> Atalho: `bash scripts/validate-phase16.sh`.

### F16.1 — Validação da Fase 16

```bash
bash scripts/validate-phase16.sh
```

Esperado: todos os checks PASS (e as Fases 14 e 15 embutidas também).

### F16.2 — Scripts e docs

O script confere que:

- `scripts/validate-phase1.sh`..`validate-phase15.sh` existem.
- Docs obrigatórios presentes: `docs/FINAL-QA.md`, `docs/ANDROID-BUILD-CHECKLIST.md`,
  `docs/CHALLENGE-SCORING.md`, `STUDENT-GUIDE.md`, `WALKTHROUGH.md`, `README.md`.
- `docs/FINAL-QA.md` contém `Final QA`, `127.0.0.1:8102`, `10.0.2.2:8102`,
  `Anti-leak`, `validate-phase16.sh` e `<flag_redacted>`.
- `docs/ANDROID-BUILD-CHECKLIST.md` contém `Android Studio`, `Gradle`,
  `debug APK`, `10.0.2.2:8102`, `API Host`, `guest / guest123`, `emulador` e
  `celular físico`.
- `README.md` aponta para `docs/ANDROID-BUILD-CHECKLIST.md` e `docs/FINAL-QA.md`.
- `STUDENT-GUIDE.md` contém `Checklist do aluno`, `Como registrar progresso`,
  `evidência` e referencia `challenge/progress`/`challenge/submit` com `<flag_redacted>`.
- `WALKTHROUGH.md` contém `Checklist de encerramento`, `Final Operator Chain` e a
  flag final do Stage 09.

### F16.3 — Anti-leak

- `FLAG{` rejeitado em `README.md`, `STUDENT-GUIDE.md`, `docs/ARCHITECTURE.md`,
  `docs/PHASES.md`, `docs/VULNERABILITY-ROADMAP.md`, `docs/CHALLENGE-SCORING.md`,
  `docs/FINAL-QA.md`, `docs/ANDROID-BUILD-CHECKLIST.md`,
  `docs/mobile-pentest/SETUP.md`, `docs/mobile-pentest/PLAYBOOK.md`,
  `android-app/README.md` e `tools/`.
- `FLAG{` permitido apenas em `WALKTHROUGH.md`, `api/src/flags.js` e nos
  `scripts/validate-phase14/15/16.sh`.
- `analyst123`/`operator123` rejeitados em `README.md`, `STUDENT-GUIDE.md`,
  `android-app/README.md`, `tools/`, `docs/FINAL-QA.md` e
  `docs/ANDROID-BUILD-CHECKLIST.md`.

### F16.4 — Typos e placeholders

- Falha em typos conhecidos (`network-config-cleartext-overrie`, `PinningPolicyy`,
  `TamperCheckk`, `getSessionSummay`, `getSessionSummar(`, `@JavascriptInterfac`,
  `webVieClient`, `LegacyRequestSigne`, `WeakCryptosha1Hex`, `WeakCryptomd5Hex`,
  `apiClientsetBaseUrlForSession`, `getLastNativeGatStatus`, `App Integrit` sem `y`).
- Falha em placeholders perigosos (`TODO`, `FIXME`, `TBD`, `changeme`,
  `placeholder`, `lorem ipsum`) nos **docs finais** (`docs/FINAL-QA.md`,
  `docs/ANDROID-BUILD-CHECKLIST.md`).

### F16.5 — Android SDK e Docker

- **Android SDK** continua **best-effort**: a ausência do SDK não falha a Fase 16.
  Se houver SDK, o script pode rodar `./gradlew tasks`/`assembleDebug` sem falhar
  por ausência de SDK.
- Os testes dinâmicos de Docker (login + cadeia) são herdados das Fases 14/15.

### Critérios de aceite (Fase 16)

- [ ] `validate-phase1..15.sh` continuam passando.
- [ ] `validate-phase16.sh` passa (inclui as Fases 14 e 15 embutidas).
- [ ] `docs/FINAL-QA.md` e `docs/ANDROID-BUILD-CHECKLIST.md` criados.
- [ ] `README.md`/`STUDENT-GUIDE.md`/`WALKTHROUGH.md` atualizados com o fechamento da Fase 16.
- [ ] Docs públicos sem `FLAG{`; sem `analyst123`/`operator123` em material público.
- [ ] Sem typos/placeholders perigosos nos docs finais.
- [ ] Nenhum lab 1..7 alterado; `git diff --stat` mostra apenas `lab-08-obsidianpay/`.

---

## Fase 17 — Android build readiness

> A Fase 17 não altera o backend, o app, as flags nem os endpoints da Fase 14: é
> a **preparação para o build real do APK** no Android Studio. Entrega o script
> `scripts/validate-phase17.sh`, que faz a inspeção estrutural forte do projeto
> Android e tenta o build real best-effort, e atualiza os docs de build.
>
> Atalho: `bash scripts/validate-phase17.sh`. **Não exige Android SDK.**

### F17.1 — Rodar a validação

```bash
cd lab-08-obsidianpay
bash scripts/validate-phase17.sh
```

Esperado: todos os checks PASS. Sem Android SDK, o build é **WARN** (não falha);
as Fases 14, 15 e 16 são executadas embutidas.

### F17.2 — O que o script valida

- **Scripts anteriores:** `validate-phase1.sh`..`validate-phase16.sh` existem.
- **Arquivos Android obrigatórios:** `settings.gradle`, `build.gradle` (raiz),
  `gradle.properties`, `app/build.gradle`, `AndroidManifest.xml`,
  `network_security_config.xml`, `strings.xml`, `colors.xml`, `themes.xml`;
  classes-núcleo (`MainActivity`, `ApiClient`, `ApiModels`, `ApiResult`,
  `InsecureSessionStore`); todas as 14 telas (`LoginScreen`..`IntegrityScreen`);
  e os pacotes `auth/`, `deeplink/`, `environment/`, `integrity/`, `network/`,
  `platform/`, `security/`, `storage/`, `webview/`.
- **Gradle/Manifest:** namespace/applicationId `com.obsidianpay.mobile`,
  `minSdk`/`targetSdk`/`compileSdk`, permissão `INTERNET`, `usesCleartextTraffic`,
  `networkSecurityConfig`, os três componentes exportados (`InternalOpsActivity`,
  `DebugCommandReceiver`, `ObsidianNotesProvider`), a authority
  `com.obsidianpay.mobile.provider.notes`, o scheme `obsidianpay` e os hosts
  `transfer`/`support`/`receipt`.
- **Conteúdo Kotlin:** `MainActivity` navega para todas as telas; `HomeScreen`
  tem botões; `ApiClient` usa `Constants`/`DEFAULT_BASE_URL` e OkHttp;
  `WebViewSupportScreen` usa `addJavascriptInterface`; `ObsidianSupportBridge`
  tem `@JavascriptInterface`; `LegacyRequestSigner`, `RootDetector`,
  `EmulatorDetector`, `EnvironmentRiskEngine`, `LocalAuthState`, `BiometricGate`,
  `NativeGate`, `TamperCheck`, `PinningPolicy`, `NetworkSecurityProfile` existem.
- **Typos críticos:** falha em `network-config-cleartext-overrie`, `PinningPolicyy`,
  `TamperCheckk`, `getSessionSummay`, `getSessionSummar(`, `@JavascriptInterfac`,
  `webVieClient`, `LegacyRequestSigne`, `WeakCryptosha1Hex`, `WeakCryptomd5Hex`,
  `apiClientsetBaseUrlForSession`, `getLastNativeGatStatus`, `App Integrit` (sem `y`).
- **Anti-leak:** sem `FLAG{` em docs públicos/tools; sem `analyst123`/`operator123`
  em material público.
- **Labs 1..7 intocados** (commits da branch + working tree, ignorando ruído de
  modo de arquivo do mount Windows/WSL).
- **Validações anteriores:** roda `validate-phase14.sh`, `validate-phase15.sh` e
  `validate-phase16.sh`.

### F17.3 — Se o Android SDK não existir

A ausência de Android SDK é **esperada** no shell e **não falha** a Fase 17. O
script imprime:

```
[WARN] Android SDK não detectado — build real deve ser feito no Android Studio
       conforme docs/ANDROID-BUILD-CHECKLIST.md
```

O SDK é detectado via `ANDROID_HOME`, `ANDROID_SDK_ROOT` ou
`android-app/local.properties` (`sdk.dir=...`). Com SDK presente, o script roda
`./gradlew --no-daemon :app:assembleDebug` e **falha** se o build falhar.

### F17.4 — Build real (Android Studio / linha de comando)

```bash
# Android Studio: File > Open > selecione android-app/ e rode (Run ▶).
# Linha de comando (requer Android SDK + JDK 17+):
cd android-app
./gradlew --no-daemon :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Detalhes e erros comuns em `docs/ANDROID-BUILD-CHECKLIST.md` (seção 11).

### Critérios de aceite (Fase 17)

- [ ] `validate-phase1..16.sh` continuam passando.
- [ ] `validate-phase17.sh` passa (inclui as Fases 14, 15 e 16 embutidas).
- [ ] Estrutura Android validada (Gradle/Manifest/recursos/telas/pacotes/Kotlin).
- [ ] Sem Android SDK, o build é **WARN** (não FAIL); com SDK, `assembleDebug` passa.
- [ ] `docs/ANDROID-BUILD-CHECKLIST.md`/`docs/FINAL-QA.md` atualizados com a Fase 17.
- [ ] Docs públicos sem `FLAG{`; sem `analyst123`/`operator123` em material público.
- [ ] Nenhum lab 1..7 alterado; `git diff --stat` mostra apenas `lab-08-obsidianpay/`.
