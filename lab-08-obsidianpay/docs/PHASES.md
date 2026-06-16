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
| **Fase 8** | **Trilha de reverse engineering** (`security/`): segredos hardcoded fragmentados (`HardcodedSecrets`), cripto fraca didática (`WeakCrypto`: Base64/XOR/SHA-1), assinatura local fraca (`LegacyRequestSigner`) e fluxo **Device Trust** → endpoint interno `/api/mobile/internal/device-trust` (assinatura SHA-1 fraca aceita pelo backend) + `reverse-hint`. | ✅ Concluída |
| **Fase 9** | **Checagem de ambiente** (`environment/`): detecção de root (`RootDetector`) e emulador (`EmulatorDetector`) didáticos, `EnvironmentRiskEngine` calculando nível de risco local, tela `SecurityCheckScreen` + backend `POST /api/mobile/internal/environment-report` (monitor-only). Scaffold para futura exploração via Frida/patching. | ✅ Concluída |
| **Fase 10** | **Secure Vault / local auth** (`auth/`): `LocalAuthState` + `BiometricGate` scaffold + `VaultScreen`, fallback PIN fraco hardcoded, estado local de auth inseguro (SharedPreferences), backend `vault-mobile/status` e `vault-mobile/unlock` (server trusts client-side localAuth). | ✅ Concluída |
| **Fase 11** | **Network security / certificate pinning scaffold** (`network/`): `NetworkSecurityProfile`, `PinningPolicy`, `ApiHostOverrideScreen` (override de base URL emulador/celular físico), backend `GET /api/mobile/internal/network-profile`, cleartext local, modos didáticos de pinning. | ✅ Concluída |
| **Fase 12** | **App Integrity / NativeGate / TamperCheck scaffold** (`integrity/`): `NativeGate` (JNI opcional/fallback-safe), `TamperCheck` (debuggable/installer/signature/packageName), `IntegrityScreen`, backend `POST /api/mobile/internal/app-integrity` (report-only). | ✅ Concluída |
| **Fase 13** | **Dynamic Instrumentation scaffold**: scripts Frida didáticos (`tools/frida/`, 5 scripts por domínio), playbook ADB (`tools/adb/`), documentação de pentest mobile (`docs/mobile-pentest/`). | ✅ Concluída |
| **Fase 14** | **Final Challenge Chain**: cadeia oficial de 9 estágios (`api/src/challenge-chain.js`), flags internas (`api/src/flags.js`), scoring local e endpoints `challenge/progress`, `challenge/submit`, `challenge/scoreboard`, `internal/finalize-operator`; scoring público em `docs/CHALLENGE-SCORING.md`. | ✅ Concluída |
| **Fase 15** | **Documentação final**: `WALKTHROUGH.md` manual completo de instrutor (Stages 01–09 + final operator chain, passo a passo, com flags); `STUDENT-GUIDE.md` polido (objetivo final, trilha de raciocínio, progress/submit, checklist) sem spoilers; `README.md`/`PLAYBOOK.md` alinhados à cadeia; `CHALLENGE-SCORING.md` mais útil; guards anti-spoiler/anti-leak reforçados em `scripts/validate-phase15.sh`. | ✅ Concluída |
| **Fase 16** | **QA final / release readiness**: validação consolidada (`scripts/validate-phase16.sh`), revisão de docs (anti-spoiler/anti-leak), detecção de typos/placeholders perigosos, `docs/FINAL-QA.md` (matriz de validação + checklist de release) e `docs/ANDROID-BUILD-CHECKLIST.md` (preparação do build Android real no Android Studio). Não adiciona vulnerabilidades nem altera flags; build do APK continua best-effort no shell. | ✅ Concluída |
| **Fase 17** | **Android build readiness**: QA estrutural de Kotlin/Gradle/Manifest/recursos (`scripts/validate-phase17.sh`), build `assembleDebug` **best-effort** (WARN sem Android SDK, FAIL se o build falhar com SDK presente), seção "Erros comuns de build" no `docs/ANDROID-BUILD-CHECKLIST.md` e status de build no `docs/FINAL-QA.md`/`VALIDATION.md`. Não altera backend, app, flags nem os endpoints da Fase 14. | ✅ Concluída |
| **Fase 18** | **Public docs polish**: tabela final de vulnerabilidades no `README.md` (`Vulnerabilidades presentes`), trilha manual do aluno sem spoiler no `STUDENT-GUIDE.md` (`Passo a passo manual sugerido`), `WALKTHROUGH.md` mantido como material de instrutor (com flags), consistência final da documentação e `scripts/validate-phase18.sh`. Não altera backend, app, flags nem os endpoints da Fase 14. | ✅ Concluída |
| **Fase 19** | **Final instructor walkthrough + roadmap consolidation**: `WALKTHROUGH.md` reescrito como guia manual completo e para iniciante absoluto (Preparação, Burp, JADX, ADB, Frida, Stages 01–09, troubleshooting, com as flags reais); matriz final consolidada no `docs/VULNERABILITY-ROADMAP.md` (revisada contra o código; Exported Service e Native pinning corrigidos); `README.md` classificado em "Vulnerabilidades presentes" / "Scaffolds e técnicas educacionais" / "Recursos do CTF"; correção da pontuação total para 2100; `scripts/validate-phase19.sh`. Não altera backend, app, flags nem os endpoints da Fase 14. | ✅ Atual |

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

## Escopo da Fase 9 (entregue)

- Pacote `environment/` com a trilha de detecção de ambiente:
  - `RootDetector.kt` — verifica paths `/system/bin/su`/`/system/xbin/su`/…,
    pacotes suspeitos (`com.topjohnwu.magisk`, `eu.chainfire.supersu`…),
    build tag `test-keys`, props `ro.debuggable`/`ro.secure`.
  - `EmulatorDetector.kt` — verifica `Build.FINGERPRINT`/`MODEL`/`MANUFACTURER`/
    `BRAND`/`DEVICE`/`PRODUCT`/`HARDWARE` para sinais de AVD, Genymotion, vbox86.
  - `EnvironmentRiskEngine.kt` — orquestra os dois detectores, calcula
    `riskLevel` (`low`/`medium`/`high`) e gera JSON com `bypassHintId`
    (`env-check-local-only`, `hooks-change-return-values`, `patch-risk-engine-result`).
- `ui/SecurityCheckScreen.kt` — tela "Security Check": exibe resultado e sinais,
  botões "Run Security Check" / "Send Environment Report" / "Show Local Signals" /
  "Clear Local Report"; registra eventos
  `environment_check_started`, `root_detection_completed`,
  `emulator_detection_completed`, `environment_risk_calculated`,
  `environment_report_sent`, `environment_report_cached`.
- `ApiClient` ganha `sendEnvironmentReport`; `HomeScreen` tem botão "Security Check";
  `LocalStateScreen` mostra o estado dos novos campos.
- `Constants` ganha `ENVIRONMENT_REPORT_PATH`, `KEY_LAST_ENVIRONMENT_REPORT`,
  `KEY_LAST_ENVIRONMENT_RESPONSE`; `InsecureSessionStore` e `LocalCacheManager`
  ganham os métodos de cache correspondentes.
- Backend: `POST /api/mobile/internal/environment-report` (monitor-only, não bloqueia
  por root/emulador); `data.js` ganha `environmentConfig` com
  `enableEnvironmentChecks` e `environmentReportPath`.
- Script `scripts/validate-phase9.sh`.

## Escopo da Fase 10 (entregue)

- Pacote `auth/` com dois componentes:
  - `LocalAuthState.kt` — gerencia estado local de auth do vault: `isVaultUnlocked`,
    `markVaultUnlocked/Locked`, `getWeakFallbackPin` (hardcoded "0420"),
    `validateFallbackPin` e `buildAuthDecision`.
  - `BiometricGate.kt` — scaffold de biometria: `canUseBiometric` (scaffold, sempre true),
    `buildPromptTitle`/`Subtitle`/`Description`, `shouldAllowFallback`, `buildBypassHintId`
    e constantes `BYPASS_HINT_*` (`biometric-result-hook`, `force-auth-decision-true`,
    `patch-local-auth-state`).
- `ui/VaultScreen.kt` — tela "Secure Vault": status card, botões Check Biometric /
  Unlock with Biometric (scaffold) / Unlock with PIN / Lock Vault / Fetch Vault Status /
  Request Vault Unlock; registra eventos biometric/auth/vault.
- `InsecureSessionStore` + `LocalCacheManager` com suporte a vault unlocked,
  unlock reason, last vault status/unlock JSON, `clearVaultState()`.
- `ApiClient` com `getMobileVaultStatus` e `requestMobileVaultUnlock`.
- `Constants` com `VAULT_MOBILE_STATUS_PATH`, `VAULT_MOBILE_UNLOCK_PATH` e keys de storage.
- `HomeScreen` com botão "Secure Vault"; `MainActivity` com `Screen.Vault`;
  `LocalStateScreen` com seção de vault/auth.
- Backend: `GET /api/mobile/internal/vault-mobile/status` (retorna policy),
  `POST /api/mobile/internal/vault-mobile/unlock` (trusts `localAuth === true`);
  `data.js` com `mobileVaultConfig` e `enableBiometricVault: true` em `featureFlags`.
- Script `scripts/validate-phase10.sh`.

## Escopo da Fase 11 (entregue)

- Pacote `network/` com dois objetos de política:
  - `NetworkSecurityProfile.kt` — constantes de base URL (`DEFAULT_EMULATOR_BASE_URL`,
    `DEFAULT_LOCALHOST_BASE_URL`, `SAMPLE_PHONE_BASE_URL`), perfis de rede
    (`cleartext-local`, `burp-proxy-ready`, `pinning-scaffold`), helpers
    `normalizeBaseUrl`, `isCleartext`, `isLikelyEmulatorUrl`,
    `isLikelyPhoneLanUrl`, `buildProfile` e `buildBypassHintId`.
  - `PinningPolicy.kt` — scaffold didático de certificate pinning: modos
    `disabled-local-lab`, `report-only`, `strict-scaffold`; `SAMPLE_PIN_SHA256`
    (placeholder); `shouldAttachCertificatePinner` (false para HTTP local);
    `buildPinningBypassHints` com hints `trust-user-ca`,
    `okhttp-certificate-pinner-hook`, `trust-manager-hook`,
    `user-ca-not-trusted-by-default`, `report-only`.
- `ui/ApiHostOverrideScreen.kt` — tela "API Host" para troca de base URL em
  runtime (emulador ↔ celular físico); salva override em SharedPreferences;
  botões "Use Emulator Default" / "Use Phone LAN Example" / "Save Base URL" /
  "Clear Override" / "Fetch Network Profile"; registra eventos
  `api_base_url_override_saved`, `api_base_url_override_cleared`,
  `network_profile_fetched`, `pinning_mode_observed`.
- `ApiClient` atualizado: `setBaseUrlForSession` / `getBaseUrl`, comentário de
  scaffold de `CertificatePinner` / `PinningPolicy`, método `getNetworkProfile`.
- `Constants` com `NETWORK_PROFILE_PATH`, `KEY_API_BASE_URL_OVERRIDE`,
  `KEY_LAST_NETWORK_PROFILE_JSON`, `KEY_LAST_PINNING_MODE`, `KEY_LAST_PINNING_HINT`.
- `InsecureSessionStore` com getters/setters para os novos campos de rede;
  `getAllDebugValues`/`getSafeDebugValuesForProvider` incluem os novos campos.
- `MainActivity` restaura o override de base URL na inicialização; adiciona
  `Screen.ApiHost` e o case de navegação.
- `HomeScreen` com botão "API Host"; `LocalStateScreen` com seção "Network
  Security / API Host Override (Phase 11)" e eventos de rede.
- `network_security_config.xml` com comentários expandidos explicando cleartext
  para emulador/localhost e a abordagem para dispositivos físicos LAN.
- Backend: `data.js` com `networkProfileConfig`; `server.js` com endpoint
  `GET /api/mobile/internal/network-profile` (auth required, retorna perfil,
  `pinningMode:"report-only"`, `cleartextAllowed:true`, hints de bypass, nota
  de next step); `buildMobileConfig` expõe `enableNetworkProfile`,
  `networkProfilePath`, `pinningMode`, `cleartextAllowed`.
- Script `scripts/validate-phase11.sh`.

## Escopo da Fase 16 (entregue)

- **QA final consolidado**: `docs/FINAL-QA.md` com status do lab, portas/URLs
  (`127.0.0.1:8102`, `10.0.2.2:8102`, IP de LAN para celular físico), matriz de
  validação por domínio, lista dos scripts `validate-phase1..16`, política
  anti-leak e checklist de release.
- **Preparação do build Android real**: `docs/ANDROID-BUILD-CHECKLIST.md` com
  pré-requisitos, abertura do projeto, SDK/`local.properties`, Gradle sync, build
  do debug APK, instalação no emulador, configuração do API Host, smoke test e
  troubleshooting. Build documentado, não executado obrigatoriamente.
- **Atualização de docs**: `README.md` (status final + links), `STUDENT-GUIDE.md`
  (fechamento do aluno: antes de começar, checklist, quando pedir ajuda, falsos
  positivos, evidência) e `WALKTHROUGH.md` (encerramento de instrutor: reset do
  backend, validação de score, problemas de Android/emulador).
- **Guards de release**: `scripts/validate-phase16.sh` reforça anti-leak
  (marcadores de flag e credenciais internas), falha em typos conhecidos e em
  marcadores de rascunho perigosos
  (`TODO`/`FIXME`/`TBD`/`changeme`/`placeholder`/`lorem ipsum`) nos docs finais,
  e roda `validate-phase14.sh` + `validate-phase15.sh` internamente. Não exige
  Android SDK; não adiciona vulnerabilidades; não altera flags.

## Escopo da Fase 17 (entregue)

- **Android build readiness**: revisão estrutural do projeto Android
  (`android-app/`) preparando o build real do APK no Android Studio — sem alterar
  backend, app, flags ou os endpoints da Fase 14.
- **QA estrutural de Kotlin/Gradle/Manifest**: `scripts/validate-phase17.sh`
  confere os arquivos Gradle (namespace `com.obsidianpay.mobile`, `minSdk`/
  `targetSdk`/`compileSdk`), o `AndroidManifest` (`INTERNET`,
  `usesCleartextTraffic`, `networkSecurityConfig`, componentes exportados,
  authority `provider.notes`, scheme `obsidianpay`, hosts
  `transfer`/`support`/`receipt`), os recursos (`strings`/`colors`/`themes`/
  `network_security_config`), as 14 telas, os pacotes do app e o conteúdo
  Kotlin-chave (navegação, bridge, detectores, auth/integrity/network), além de
  reforçar os guards de typos e anti-leak.
- **`assembleDebug` best-effort**: sem Android SDK detectado
  (`ANDROID_HOME`/`ANDROID_SDK_ROOT`/`local.properties` `sdk.dir`), o build é
  **WARN** e não falha; com SDK detectado, o script roda
  `./gradlew --no-daemon :app:assembleDebug` e **falha** se o build falhar.
- **Docs atualizados**: seção "Erros comuns de build e como corrigir" no
  `docs/ANDROID-BUILD-CHECKLIST.md`; status da Fase 17 no `docs/FINAL-QA.md`;
  seção Fase 17 no `VALIDATION.md`; nota de build real no `README.md`; este
  roadmap e `docs/VULNERABILITY-ROADMAP.md`. Sem flags em docs públicos.
- O script roda `validate-phase14.sh`, `validate-phase15.sh` e
  `validate-phase16.sh` embutidos. Não exige Android SDK; não adiciona
  vulnerabilidades; não altera flags.

## Escopo da Fase 18 (entregue)

- **Public docs polish**: acabamento final da documentação pública no mesmo padrão
  dos outros labs — sem alterar backend, app, flags ou os endpoints da Fase 14.
- **Vulnerability table**: seção "Vulnerabilidades presentes" no `README.md`, com
  as colunas Categoria / Vulnerabilidade / Onde aparece no lab / O que o aluno
  aprende, cobrindo as 22 trilhas (de Insecure Mobile Storage à Challenge Chain),
  em linguagem simples e **sem** flags/headers finais/payloads.
- **Manual student path**: seção "Passo a passo manual sugerido" no
  `STUDENT-GUIDE.md` — 15 estações práticas e investigativas (backend, app, login,
  recon, storage, API/IDOR, WebView/bridge, deep links/QR, componentes exportados,
  reverse engineering, Device Trust, root/emulator/biometric/integrity,
  network/pinning, submissão de progresso e evidências finais), sem flags.
- **Final documentation consistency**: `WALKTHROUGH.md` permanece instrutor-facing
  (solução completa + flags); `VALIDATION.md`, `docs/PHASES.md` e
  `docs/VULNERABILITY-ROADMAP.md` atualizados; `scripts/validate-phase18.sh` valida
  README/STUDENT-GUIDE/WALKTHROUGH, anti-leak, typos e labs 1..7 intocados, e roda
  `validate-phase16.sh`/`validate-phase17.sh` embutidos. Não exige Android SDK.

## Escopo da Fase 19 (entregue)

- **Final instructor walkthrough (beginner-grade):** `WALKTHROUGH.md` reescrito do
  zero como guia manual completo, linear e para **iniciante absoluto** — abre com
  "Estado: Final — Fase 19" e cobre Preparação completa (repo, backend, Android
  Studio, emulador, instalação, API Host, login, evidências), Configuração do Burp
  Suite, Análise estática do APK (JADX), ADB para iniciantes, Frida para
  iniciantes, os 9 stages (cada um com objetivo, passos numerados, o que observar,
  por que é vulnerável, como obter/submeter a flag, evidência, erros comuns e
  checklist), troubleshooting e apêndices. Mantém as flags reais (instrutor).
- **Roadmap consolidation:** `docs/VULNERABILITY-ROADMAP.md` ganha a **matriz final
  consolidada** (ID / Trilha / Vulnerabilidade / Implementação real / Tipo / Stage
  relacionado / Status final), revisada **contra o código**. Exported Service e
  Native pinning marcados como **não implementados como desafio independente**;
  scaffold diferenciado de vulnerabilidade; Challenge Chain/Dynamic Instrumentation
  classificados como recurso/scaffold (não vulnerabilidades).
- **README classification cleanup:** `README.md` separa "Vulnerabilidades
  presentes", "Scaffolds e técnicas educacionais" e "Recursos do CTF". Sem flags.
- **Correção de inconsistência:** total da cadeia corrigido para **2100**
  (`WALKTHROUGH.md`, `STUDENT-GUIDE.md`, `docs/CHALLENGE-SCORING.md`,
  `docs/FINAL-QA.md`), batendo com `api/src/challenge-chain.js`.
- **Validação:** `scripts/validate-phase19.sh` (estrutura/anti-leak/estrutura por
  stage; roda `validate-phase17.sh` e `validate-phase18.sh`). Não exige Android SDK;
  não altera backend, app, flags ou endpoints.

## Princípios entre fases

- Não quebrar o contrato da Fase 1 sem necessidade.
- Manter o produto realista (sem "menu de bugs").
- Nada de flags reais em arquivos públicos.
- Tudo **local only**, sem dependências externas.
