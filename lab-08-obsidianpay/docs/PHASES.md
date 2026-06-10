# Fases — Lab 08: ObsidianPay Mobile

Plano de fases do laboratório. Fases 1–5 implementadas.

| Fase | Foco | Status |
|---|---|---|
| **Fase 1** | Fundação: arquitetura, documentação base, backend mínimo e contratos de API. | ✅ Concluída |
| **Fase 2** | API mobile rica + primeiras vulnerabilidades de backend (IDOR, mass assignment, gates fracos, scaffolds de QR/WebView/vault). | ✅ Concluída |
| **Fase 3** | App Android base (Kotlin + Compose): telas, cliente HTTP, SharedPreferences inseguro, enumeração manual por ID, suporte/diagnostics, transfer preview. | ✅ Concluída |
| **Fase 4** | Armazenamento local inseguro: SharedPreferences rico, SQLite (`obsidianpay_local.db`), arquivos em filesDir/cacheDir, export app-specific externo, eventos de debug, cache offline. | ✅ Concluída |
| **Fase 5** | Deep links (`obsidianpay://transfer/support/receipt`), QR Payment (input textual), Web Support (WebView com JS), reflexão controlada no portal, cache de eventos deep link/QR/WebView. | ✅ Concluída |
| **Fase 6** | WebView **JavaScript bridge** (`ObsidianBridge` via `@JavascriptInterface`): leitura controlada de sessão/caches/artefatos locais a partir do portal de suporte; cadeia deep link/QR → WebView → bridge → cache local. | ✅ Atual |
| Fase 7 | Recon estático do app + trilha network/API (interceptação, legado/HTTP, pinning). | 🔜 Planejada |
| Fase 8 | Trilha storage/RE avançada: segredos hardcoded, cripto fraca, RE do binário. | 🔜 Planejada |
| Fase 9 | Trilha platform: componentes exportados (Service/Receiver/Provider). | 🔜 Planejada |
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

## Princípios entre fases

- Não quebrar o contrato da Fase 1 sem necessidade.
- Manter o produto realista (sem "menu de bugs").
- Nada de flags reais em arquivos públicos.
- Tudo **local only**, sem dependências externas.
