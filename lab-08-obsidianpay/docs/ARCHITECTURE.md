# Arquitetura — Lab 08: ObsidianPay Mobile

Documento técnico da arquitetura do laboratório. Vale tanto para a Fase 1 (atual)
quanto como contrato para as fases futuras.

---

## 1. Componentes do lab

| Componente | Estado | Descrição |
|---|---|---|
| ObsidianPay Mobile API | ✅ Fase 2 | Backend Node.js + Express em `127.0.0.1:8102`. |
| App Android (base) | ✅ Fase 3 | Cliente Kotlin + Compose em `android-app/`; consome a API via `10.0.2.2:8102`. |
| Documentação | ✅ Fase 1 | README, guia do aluno, walkthrough interno, roadmap. |
| Scripts de validação | ✅ Fase 1 | `scripts/validate-phase1.sh`. |

```
lab-08-obsidianpay/
├── api/                 # backend Node.js + Express (Fase 1)
│   ├── src/server.js
│   ├── src/data.js
│   ├── Dockerfile
│   └── package.json
├── android-app/         # placeholder do app (fases futuras)
├── docs/                # arquitetura, roadmap, fases
├── scripts/             # validação
├── evidence/            # evidências de execução (futuro)
├── validation/          # artefatos de validação (futuro)
├── docker-compose.yml
├── README.md
├── STUDENT-GUIDE.md
├── WALKTHROUGH.md
└── VALIDATION.md
```

---

## 2. Backend local (Fase 2)

- **Stack:** Node.js 20 + Express.
- **Persistência:** estado em memória (`api/src/data.js`), organizado por
  domínio (usuários, recibos, cartões, feature flags, config). SQLite/persistência
  podem ser introduzidos quando uma vulnerabilidade de storage exigir.
- **Bind:** `0.0.0.0:8102` dentro do container; publicado em `127.0.0.1:8102`.
- **Token:** didático e previsível (`obsidian-mobile-token-<username>-<userId>`),
  validado por helper; endpoints protegidos retornam `401` sem token válido.
- **Contrato de API (Fase 2):**
  - `GET /` · `GET /health` — identificação e status.
  - `POST /api/mobile/login` — autenticação mobile (token Bearer didático).
  - `GET` / `PATCH /api/mobile/profile` — leitura e atualização de perfil.
  - `GET /api/mobile/config` — config mobile (versões, schemes de deep link,
    chaves de storage do cliente, feature flags).
  - `GET /api/mobile/receipts` · `GET /api/mobile/receipts/:receiptId` — recibos.
  - `GET /api/mobile/receipt/:id` — endpoint singular de compatibilidade (Fase 1).
  - `GET /api/mobile/cards` · `GET /api/mobile/cards/:cardId` — cartões (número
    mascarado na saída).
  - `POST /api/mobile/support/sync` — stub legado de sincronização.
  - `GET /api/mobile/support/diagnostics` — diagnósticos atrás de header de debug.
  - `POST /api/mobile/transfer/preview` — prévia de transferência (QR/deep link).
  - `GET /api/mobile/webview/support` — portal HTML para WebView futura.
  - `GET /api/mobile/legacy/routes` — enumeração de rotas internas/futuras.
  - `GET /api/mobile/internal/vault-status` — status interno com gate por papel.
- **Respostas de erro:** sempre JSON (`{ error, message }`).
- **Controles intencionalmente fracos:** alguns endpoints da Fase 2 implementam
  fronteiras de segurança propositalmente frágeis (ver §5 e o roadmap). São
  alvos de estudo, não garantias de segurança.

---

## 3. App Android (base — Fase 3)

A Fase 3 entrega o **app base** em `android-app/` (Kotlin + Jetpack Compose):

- **Telas:** Login, Início, Recibos, Cartões, Suporte, Prévia de transferência,
  Configuração.
- **Cliente HTTP:** `ApiClient` (OkHttp) consumindo a API mobile.
- **Storage local (Fase 4):** `InsecureSessionStore` (SharedPreferences em texto
  puro), `ObsidianLocalDb` (SQLite `obsidianpay_local.db`) e arquivos em
  `filesDir`/`cacheDir` + export em external app-specific. Orquestrado por
  `LocalCacheManager`. Tudo intencionalmente inseguro.
- **Build:** AGP 8.5.2, Kotlin 1.9.24, Gradle 8.7 (wrapper incluído), minSdk 24,
  target/compile SDK 34.

### Fluxo de deep link / QR / WebView + bridge (Fases 5–6)

```
Deep link (obsidianpay://…) ─┐
QR payload (colado/digitado) ─┴─▶ DeepLinkRouter ─▶ tela interna:
                                                   ├─ TRANSFER → TransferPreview (+ API)
                                                   ├─ RECEIPT  → Receipts (+ API)
                                                   └─ SUPPORT  → WebView ─▶ 10.0.2.2:8102/api/mobile/webview/support
                                                                  │
                                                                  └─ window.ObsidianBridge (@JavascriptInterface)
                                                                        └─▶ ObsidianSupportBridge ─▶ InsecureSessionStore /
                                                                            LocalCacheManager / SQLite / cache (leitura local)
        (eventos: deep_link_opened / qr_payload_processed / webview_support_opened /
         webview_bridge_attached / webview_bridge_called / bridge_* → cache local)
```

> A bridge (`ObsidianBridge`) é controlada: expõe resumo de sessão (token só em
> preview), caches brutos já existentes e artefatos locais, mas **não** retorna
> marcadores de progresso nem credenciais internas. É uma fronteira de estudo,
> não uma feature segura.

### Fluxo de componentes Android exportados (Fase 7)

```
Outro app / adb ──┬─ am start  -a com.obsidianpay.mobile.INTERNAL_OPS  ─▶ InternalOpsActivity ─┐
                  ├─ am broadcast -a com.obsidianpay.mobile.DEBUG_COMMAND ─▶ DebugCommandReceiver ┤
                  └─ content query content://com.obsidianpay.mobile.provider.notes/{notes,debug,cache}
                                                              │                                   │
                                                              ▼                                   ▼
                                              ObsidianNotesProvider              LocalCacheManager / InsecureSessionStore
                                              (MatrixCursor, token_preview)      SQLite debug_events / SharedPreferences
        (eventos: exported_activity_opened / exported_receiver_called / external_debug_* /
         exported_provider_query → cache local)
```

> Os componentes do pacote `platform/` são **exportados de propósito** com
> actions/authority/extras previsíveis e sem auth forte. São controlados: só
> tocam o estado local do app, não executam comandos de sistema nem rede, e o
> provider só devolve `token_preview` (nunca o token inteiro). Fronteira de
> estudo, não feature segura.

### Fluxo de Device Trust / reverse engineering (Fase 8)

```
DeviceTrustScreen (ui/)
   │  monta credenciais locais a partir de security/:
   │   ├─ HardcodedSecrets  → client id + salt (fragmentados) + hint Base64 + rotas
   │   ├─ WeakCrypto        → base64Decode(hint), sha1Hex(...)
   │   └─ LegacyRequestSigner → sha1(username:deviceId:timestamp:salt) + headers
   ▼
ApiClient.checkDeviceTrust ──▶ POST 10.0.2.2:8102/api/mobile/internal/device-trust
   (X-Obsidian-Client/Device/Timestamp/Signature)        │
                                                          ▼
                                   backend recomputa a MESMA assinatura SHA-1 fraca
                                   (salt hardcoded em data.js → legacyMobileTrust)
                                   ok → {status:"trusted-legacy", mode:"legacy-attestation"}
                                          │
                                          ▼
                       LocalCacheManager / InsecureSessionStore (cache local)
        (eventos: device_trust_check_started / weak_signature_generated /
         device_trust_response_cached / encoded_hint_decoded → cache local)
```

> A trilha da Fase 8 é **falsa proteção por design**: Base64 não é cripto, a
> assinatura SHA-1 sem HMAC/nonce é forjável, e o salt/client id são didáticos e
> embutidos no cliente (e espelhados no backend) para serem recuperados em
> reverse engineering. Não há segredos reais nem flags nessas classes/endpoints.

### Fluxo de verificação de ambiente (Fase 9)

```
SecurityCheckScreen (ui/)
   │  acionada a partir do HomeScreen ("Security Check")
   ├─ EnvironmentRiskEngine.run(context)
   │   ├─ RootDetector.check(context)  → su paths, pacotes suspeitos, test-keys, props
   │   └─ EmulatorDetector.check()     → Build.FINGERPRINT/MODEL/HARDWARE/…
   │   → EnvironmentReport { root, emulator, rootScore, emulatorScore, riskLevel,
   │                          signals, generatedAt, bypassHintId }
   ├─ LocalCacheManager.saveLastEnvironmentReportJson(json)
   │   → InsecureSessionStore.KEY_LAST_ENVIRONMENT_REPORT (SharedPreferences)
   │   → db.addDebugEvent("environment_check_started" / "root_detection_completed" /
   │                       "emulator_detection_completed" / "environment_risk_calculated")
   └─ ApiClient.sendEnvironmentReport(token, json)
       → POST 10.0.2.2:8102/api/mobile/internal/environment-report
             │  (monitor-only: não bloqueia por root/emulador)
             ▼
         { status:"received", environmentStatus, riskLevel, serverPolicy:"monitor-only",
           nextStepHint:"client-side checks are advisory in this lab" }
         → LocalCacheManager.saveLastEnvironmentResponseJson(raw)
           → InsecureSessionStore.KEY_LAST_ENVIRONMENT_RESPONSE
           → db.addDebugEvent("environment_report_sent" / "environment_report_cached")
```

> A trilha da Fase 9 é **falsa proteção por design**: todos os checks rodam
> no processo do app (client-side), os resultados ficam em cleartext no
> SharedPreferences e no SQLite, e o backend apenas registra o que o cliente
> informa (policy `monitor-only`). Os `bypassHintId`s apontam o caminho:
> `env-check-local-only`, `hooks-change-return-values`,
> `patch-risk-engine-result`. O app **não bloqueia** mesmo com risco `high`.

### Fluxo de Secure Vault / local auth (Fase 10)

```
VaultScreen (ui/)
   │  acionada a partir do HomeScreen ("Secure Vault")
   ├─ BiometricGate.canUseBiometric(context)  → scaffold, sempre true
   ├─ BiometricGate.buildBypassHintId()       → "biometric-result-hook"
   ├─ LocalAuthState.validateFallbackPin(input) → compara com "0420" (hardcoded)
   ├─ LocalAuthState.markVaultUnlocked(store, reason)
   │   → InsecureSessionStore: obsidian.vault.unlocked = true (SharedPreferences)
   │   → LocalCacheManager.saveVaultUnlocked → db.addDebugEvent("vault_unlocked_local")
   │   (eventos: biometric_capability_checked / biometric_prompt_started /
   │    biometric_auth_result / local_auth_success / local_auth_failed /
   │    vault_unlocked_local / vault_locked_local / weak_pin_fallback_used)
   ├─ ApiClient.getMobileVaultStatus(token)
   │   → GET 10.0.2.2:8102/api/mobile/internal/vault-mobile/status
   │     { status:"locked", policy:"local-auth-required",
   │       allowedMethods:["biometric","fallback-pin"], serverTrust:"client-asserted" }
   │   → LocalCacheManager.saveLastVaultStatusJson → db.addDebugEvent("vault_status_cached")
   └─ ApiClient.requestMobileVaultUnlock(token, localAuth, method, bypassHintId)
       → POST 10.0.2.2:8102/api/mobile/internal/vault-mobile/unlock
         body: { localAuth: true, method: "biometric", bypassHintId: "..." }
         if localAuth===true → { status:"vault-access-granted",
                                  serverTrust:"client-asserted",
                                  nextStepHint:"server trusts local auth assertion in this lab" }
         if localAuth!==true → 403
       → LocalCacheManager.saveLastVaultUnlockJson → db.addDebugEvent("vault_unlock_response_cached")
```

> A trilha da Fase 10 é **falsa proteção por design**: o PIN fraco é hardcoded
> no APK (recuperável por análise estática), o estado de auth (`vault.unlocked`)
> fica em SharedPreferences em texto puro (manipulável por root), e o backend
> confia no campo `localAuth` sem qualquer verificação independente. Os
> `bypassHintId`s apontam o caminho: `biometric-result-hook`,
> `force-auth-decision-true`, `patch-local-auth-state`.

### Fluxo de network security / API host override (Fase 11)

```
ApiHostOverrideScreen (ui/)
   │  acionada a partir do HomeScreen ("API Host")
   ├─ NetworkSecurityProfile.normalizeBaseUrl(input)
   │   → valida e normaliza a URL digitada
   ├─ InsecureSessionStore.saveApiBaseUrlOverride(normalized)
   │   → obsidian.network.api_base_url_override (SharedPreferences, texto puro)
   ├─ ApiClient.setBaseUrlForSession(normalized)
   │   → atualiza currentBaseUrl para todas as chamadas subsequentes
   │   → loga PinningPolicy.shouldAttachCertificatePinner + buildPinningBypassHints
   │   (eventos: api_base_url_override_saved / api_base_url_override_cleared)
   └─ ApiClient.getNetworkProfile(token)
       → GET <currentBaseUrl>/api/mobile/internal/network-profile
         Authorization: Bearer <token>
         { status:"ok", profile:"burp-proxy-ready", pinningMode:"report-only",
           cleartextAllowed:true, defaultEmulatorBaseUrl:"http://10.0.2.2:8102",
           phoneLanExample:"http://192.168.0.50:8102",
           bypassHintIds:["trust-user-ca","okhttp-certificate-pinner-hook","network-config-cleartext-override"],
           nextStepHint:"configure the app base URL to reach the lab API from emulator or phone" }
       → InsecureSessionStore.saveLastNetworkProfileJson / saveLastPinningMode / saveLastPinningHint
         (eventos: network_profile_fetched / pinning_mode_observed)

MainActivity (inicialização)
   └─ store.getApiBaseUrlOverride()
       → se existe → ApiClient(override)   // restaura override após reinício
       → se não existe → ApiClient()        // usa DEFAULT_BASE_URL (10.0.2.2)
```

> A Fase 11 implementa **cleartext local e override de base URL** por design. O
> override é armazenado em texto puro no SharedPreferences — seam didático de
> armazenamento inseguro. O `CertificatePinner` existe apenas como scaffold
> comentado no `ApiClient`; para HTTP local não é ativado. Os `bypassHintId`s
> (`trust-user-ca`, `okhttp-certificate-pinner-hook`, `network-config-cleartext-override`)
> são âncoras para estudo futuro de Frida/proxy, não passos de solução.

### Fluxo de storage local

```
App Android ─┬─▶ SharedPreferences (sessão, token, cache de perfil/config,
             │                       api_base_url_override, network_profile, pinning_mode)
             ├─▶ SQLite obsidianpay_local.db (cached_receipts/cards, debug_events)
             ├─▶ filesDir/ (receipts/*.json, debug/export.json) e cacheDir/ (snapshot)
             └─▶ external app-specific (obsidian-export.txt)
                     ▲
                     └── alimentado pelas respostas da API em <currentBaseUrl>
                         (padrão: 10.0.2.2:8102; override: qualquer IP/porta)
```

### Fluxo de comunicação

```
┌────────────────────────┐    http://10.0.2.2:8102     ┌──────────────────────────┐    publish 127.0.0.1:8102   ┌──────────────────────┐
│  App Android (emulador)│ ─────────────────────────▶  │  Docker: obsidianpay-api  │ ◀────────────────────────── │  Host 127.0.0.1:8102 │
│  Compose + OkHttp      │                             │  Node.js + Express :8102  │                             │  (docker compose)    │
└────────────────────────┘                             └──────────────────────────┘                             └──────────────────────┘
```

No Android Emulator, `10.0.2.2` é o alias para o `127.0.0.1` do host, onde o
Docker publica a API na porta 8102.

O app **não** é um "menu de vulnerabilidades": as fraquezas ficam embutidas em um
produto que parece legítimo. Superfícies avançadas (exported components, deep
links, WebView bridge, criptografia, anti-tampering) chegam em fases futuras.

---

## 4. Modelo de ameaça educacional

- **Ator:** analista/pentester com o app instalado e o dispositivo/emulador sob
  seu controle (cenário de teste de app mobile).
- **Objetivo de aprendizado:** percorrer a cadeia recon → estática → dinâmica →
  cliente↔servidor, mapeando achados para OWASP MASVS/MASTG.
- **Fora de escopo:** qualquer alvo que não seja o ambiente local; ataques de
  rede contra terceiros; uso de scanners automáticos como atalho.

---

## 5. Fronteiras de segurança falsas/intencionais

Controles que **parecem** proteção, mas são propositalmente fracos (para estudo):

- **Token previsível** — aparenta sessão, mas é decodificável.
- **`legacySupportEndpoint` em HTTP** — sugere compatibilidade legada e tráfego
  em cleartext.
- **Validações de ownership/auth** — na Fase 1 são corretas, mas o modelo de
  dados já está preparado para flexibilizá-las em estudos de autorização.
- **Anti-tampering (futuro)** — root/emulator/biometria/pinning serão
  apresentados como barreiras "contornáveis" de forma controlada.

Essas fronteiras são **falsas por design**: existem para serem analisadas, não
para garantir segurança real.

---

## 6. Evolução por fases

- **Fase 1 (atual):** fundação, contratos de API, backend mínimo, documentação.
- **Fases futuras:** introdução do APK e ativação progressiva das trilhas de
  vulnerabilidade descritas em [VULNERABILITY-ROADMAP.md](./VULNERABILITY-ROADMAP.md).

Cada fase deve manter o backend retrocompatível com o contrato da Fase 1 sempre
que possível, evoluindo o comportamento apenas onde a vulnerabilidade exigir.
Veja o cronograma conceitual em [PHASES.md](./PHASES.md).

---

## 7. Porta e restrições

- **Porta oficial:** `127.0.0.1:8102`.
- **Restrição:** **local only.** Não expor na internet, não usar contra
  terceiros, sem dependências de serviços externos, sem segredos reais.
