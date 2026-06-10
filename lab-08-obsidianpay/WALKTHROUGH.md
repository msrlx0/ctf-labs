# WALKTHROUGH (Instrutor) — Lab 08: ObsidianPay Mobile

> **Documento interno do instrutor.** Não é material do aluno.
>
> **Estado: Fase 6.** Este walkthrough descreve a arquitetura, as cadeias
> futuras em alto nível, as **vulnerabilidades de backend da Fase 2**, o **app
> Android base da Fase 3**, o **armazenamento local inseguro da Fase 4**, os
> **deep links / QR / WebView da Fase 5** e, agora, a **WebView JavaScript bridge
> da Fase 6** (visão de instrutor, sem cadeia final longa).
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
| 20 | Anti-analysis/Auth | Root detection bypass | planned |
| 21 | Anti-analysis/Auth | Emulator detection bypass | planned |
| 22 | Anti-analysis/Auth | Biometric vault backend gate | scaffolded |
| 23 | Anti-analysis/Auth | Binary patching | planned |
| 24 | Anti-analysis/Auth | API broken access control | implemented-backend |
| 25 | Anti-analysis/Auth | Mass assignment | implemented-backend |

---

## 5. Notas de manutenção

- Flags reais **não** entram em `README.md` nem `STUDENT-GUIDE.md`.
- Soluções e payloads só serão adicionados aqui (ou em SOLUTION.md) quando a
  vulnerabilidade correspondente for implementada.
- Manter tudo **local**: porta `127.0.0.1:8102`, sem dependências externas.
