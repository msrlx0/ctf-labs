# Fases — Lab 08: ObsidianPay Mobile

Plano de fases do laboratório. Fases 1–5 implementadas.

| Fase | Foco | Status |
|---|---|---|
| **Fase 1** | Fundação: arquitetura, documentação base, backend mínimo e contratos de API. | ✅ Concluída |
| **Fase 2** | API mobile rica + primeiras vulnerabilidades de backend (IDOR, mass assignment, gates fracos, scaffolds de QR/WebView/vault). | ✅ Concluída |
| **Fase 3** | App Android base (Kotlin + Compose): telas, cliente HTTP, SharedPreferences inseguro, enumeração manual por ID, suporte/diagnostics, transfer preview. | ✅ Concluída |
| **Fase 4** | Armazenamento local inseguro: SharedPreferences rico, SQLite (`obsidianpay_local.db`), arquivos em filesDir/cacheDir, export app-specific externo, eventos de debug, cache offline. | ✅ Concluída |
| **Fase 5** | Deep links (`obsidianpay://transfer/support/receipt`), QR Payment (input textual), Web Support (WebView com JS), reflexão controlada no portal, cache de eventos deep link/QR/WebView. | ✅ Concluída |
| **Fase 6** | WebView **JavaScript bridge** (`ObsidianBridge` via `@JavascriptInterface`): leitura controlada de sessão/caches/artefatos locais a partir do portal de suporte; cadeia deep link/QR → WebView → bridge → cache local. | ✅ Concluída |
| **Fase 7** | **Componentes Android exportados** (`platform/`): Activity interna (`InternalOpsActivity`), BroadcastReceiver de debug (`DebugCommandReceiver`) e ContentProvider (`ObsidianNotesProvider`) — todos `exported=true` com actions/authority/extras previsíveis, integrados ao cache/SQLite/debug events de forma controlada. | ✅ Concluída |
| **Fase 8** | **Trilha de reverse engineering** (`security/`): segredos hardcoded fragmentados (`HardcodedSecrets`), cripto fraca didática (`WeakCrypto`: Base64/XOR/SHA-1), assinatura local fraca (`LegacyRequestSigner`) e fluxo **Device Trust** → endpoint interno `/api/mobile/internal/device-trust` (assinatura SHA-1 fraca aceita pelo backend) + `reverse-hint`. | ✅ Atual |
| Fase 9 | Trilha network/API: interceptação HTTPS, pinning, lib nativa. | 🔜 Planejada |
| Fase 10 | Trilha anti-analysis/auth: root/emulador/biometria, binary patching, BAC/mass assignment. | 🔜 Planejada |
| Fase 11 | Consolidação: cadeias completas, SOLUTION.md, evidências e validação ponta a ponta. | 🔜 Planejada |

## Escopo da Fase 1 (entregue)

- Backend Node.js + Express em `127.0.0.1:8102`.
- Endpoints: `/`, `/health`, `login`, `profile`, `config`, `receipt/:id`, `support/sync`.
- Conta de teste `guest` / `guest123`; token didático previsível.
- Docker Compose com healthcheck.
- Documentação: README, STUDENT-GUIDE, WALKTHROUGH (interno), ARCHITECTURE,
  VULNERABILITY-ROADMAP.
- Script `scripts/validate-phase1.sh`.

## Escopo da Fase 3 (entregue)

- App Android base em `android-app/` (Kotlin + Jetpack Compose, OkHttp).
- Telas: Login, Início, Recibos, Cartões, Suporte, Prévia de transferência, Config.
- `ApiClient` cobrindo os contratos da API mobile; base URL `http://10.0.2.2:8102`.
- `InsecureSessionStore` (SharedPreferences em texto puro, intencional).
- Gradle wrapper (8.7), AGP 8.5.2, Kotlin 1.9.24, minSdk 24 / SDK 34.
- `network_security_config` liberando cleartext só para hosts locais.
- Script `scripts/validate-phase3.sh`.

## Escopo da Fase 4 (entregue)

- `InsecureSessionStore` ampliado (SharedPreferences em texto puro): sessão,
  cache de perfil/config, últimos support sync/diagnostics/transfer preview,
  últimos IDs abertos, timestamp, baseUrlHint.
- `ObsidianLocalDb` (SQLite `obsidianpay_local.db`): `cached_receipts`,
  `cached_cards` (com `rawJson`) e `debug_events`.
- `LocalCacheManager`: orquestra prefs + SQLite + arquivos
  (`filesDir`/`cacheDir`) + export app-specific externo.
- Telas atualizadas para cachear respostas + tela interna `LocalStateScreen`.
- Script `scripts/validate-phase4.sh`.

## Escopo da Fase 5 (entregue)

- Deep links no `AndroidManifest` (`obsidianpay://transfer/support/receipt`).
- `deeplink/DeepLinkRouter.kt` + `DeepLinkModels.kt` (parse permissivo).
- Roteamento em `MainActivity` (intent inicial + `onNewIntent` + deep link pendente pós-login).
- `ui/QrInputScreen.kt` (QR Payment por input textual) e
  `ui/WebViewSupportScreen.kt` (WebView com JS/DOM storage, sem bridge).
- Backend `GET /api/mobile/webview/support` reflete `topic`/`message`.
- Cache local de eventos: `deep_link_opened`, `qr_payload_processed`,
  `webview_support_opened` + chaves `last_deep_link/last_qr_payload/last_webview_url`.
- Script `scripts/validate-phase5.sh`.

## Escopo da Fase 6 (entregue)

- `webview/ObsidianSupportBridge.kt`: bridge `@JavascriptInterface` anexada à
  WebView de suporte com o nome `ObsidianBridge`. Métodos: `getSessionSummary`,
  `getCachedProfile`, `getCachedConfig`, `getLastSupportSync`,
  `getLastTransferPreview`, `getLocalArtifacts`, `getBridgeInfo`, `logBridgeEvent`.
- `ui/WebViewSupportScreen.kt`: `addJavascriptInterface(bridge, "ObsidianBridge")`
  com `javaScriptEnabled`/`domStorageEnabled`; evento `webview_bridge_attached`.
- Eventos da bridge no `debug_events`: `webview_bridge_called`,
  `bridge_get_cached_profile`, `bridge_get_cached_config`, `bridge_get_artifacts`,
  `bridge_log_event`.
- Backend `GET /api/mobile/webview/support`: portal "Mobile Support Portal" que
  detecta `window.ObsidianBridge` e oferece botões de diagnóstico assistido.
- `InsecureSessionStore` com getters extra; `LocalStateScreen` mostra navegação
  recente (deep link/QR/WebView) e eventos do Web Support.
- Limites por design: sem marcadores de progresso, sem credenciais internas,
  token apenas em preview. Script `scripts/validate-phase6.sh`.

## Escopo da Fase 7 (entregue)

- Pacote `platform/` com três componentes intencionalmente **exportados**:
  - `InternalOpsActivity.kt` — Activity de "Internal Operations" (`exported=true`,
    action `com.obsidianpay.mobile.INTERNAL_OPS`), lê extras previsíveis
    (`obsidian.intent.extra.INTERNAL_ROUTE/SESSION_HINT/OPERATOR_MODE/RECEIPT_ID`),
    registra `exported_activity_opened` e grava `lastOpenedReceiptId` quando há
    `RECEIPT_ID`.
  - `DebugCommandReceiver.kt` — BroadcastReceiver (`exported=true`, action
    `com.obsidianpay.mobile.DEBUG_COMMAND`) com comandos controlados
    (`sync_marker`, `set_last_receipt`, `write_debug_export`,
    `enable_operator_hint`); sempre registra `exported_receiver_called`. Sem
    comandos de sistema, sem rede, sem flags.
  - `ObsidianNotesProvider.kt` — ContentProvider (`exported=true`, authority
    `com.obsidianpay.mobile.provider.notes`) com `MatrixCursor` para `/notes`,
    `/debug` e `/cache`; o token só aparece como `token_preview` mascarado.
- `InsecureSessionStore`/`LocalCacheManager` ganham `getTokenPreview`,
  `getSafeDebugValuesForProvider`, `saveOperatorHint`, `saveExternalDebugCommand`
  e `recordExportedEvent`; `LocalStateScreen` mostra os eventos/estado desses
  componentes (sem rótulo de "vulnerabilidade").
- `AndroidManifest.xml` declara os três componentes exportados. Script
  `scripts/validate-phase7.sh` (inclui reforço dos typos de bridge da Fase 6).

## Escopo da Fase 8 (entregue)

- Pacote `security/` com a trilha de reverse engineering:
  - `HardcodedSecrets.kt` — segredos/config hardcoded **fragmentados**
    (`INTERNAL_CLIENT_PART_*`, `LEGACY_SIGNING_SALT_PART_*`, hint Base64,
    `getHiddenRoutes()`), reassemblados em runtime para análise estática.
  - `WeakCrypto.kt` — `base64Encode/Decode`, `weakXor`/`weakXorToBase64`/
    `weakXorFromBase64`, `sha1Hex`/`md5Hex` (tudo intencionalmente fraco).
  - `LegacyRequestSigner.kt` — `sign()` = SHA-1 de
    `username:deviceId:timestamp:salt` (sem HMAC) e `buildHeaders()`
    (`X-Obsidian-Client/Device/Timestamp/Signature`).
- `ui/DeviceTrustScreen.kt` — tela "Device Trust" que monta a assinatura fraca,
  decodifica o hint Base64 e chama `/api/mobile/internal/device-trust`; salva
  `lastDeviceTrustJson`/`lastLegacySignature`/`lastEncodedOperatorHint` e registra
  `device_trust_check_started`, `weak_signature_generated`,
  `device_trust_response_cached`, `encoded_hint_decoded`.
- `ApiClient` ganha `checkDeviceTrust`/`getReverseHint`; `HomeScreen` tem botão
  discreto "Device Trust"; `LocalStateScreen` mostra o estado do fluxo.
- Backend: `POST /api/mobile/internal/device-trust` (verifica a assinatura SHA-1
  fraca com salt hardcoded didático) e `GET /api/mobile/internal/reverse-hint`
  (gated pelo client id correto). `config`/`legacy/routes` referenciam os paths.
  Sem flags nas novas classes/endpoints. Script `scripts/validate-phase8.sh`.

## Princípios entre fases

- Não quebrar o contrato da Fase 1 sem necessidade.
- Manter o produto realista (sem "menu de bugs").
- Nada de flags reais em arquivos públicos.
- Tudo **local only**, sem dependências externas.
