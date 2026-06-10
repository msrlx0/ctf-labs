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
