# Final QA — Lab 08: ObsidianPay Mobile (Fase 16)

Documento de **QA final consolidado** do Lab 08, executado **antes do build real
do APK Android no Android Studio**. Reúne, num só lugar, o estado do lab, as
portas/URLs, a matriz de validação, a política anti-leak e o checklist de
release interno.

> **Sem flags reais.** Este documento é público e **não** contém valores de flag.
> Onde um exemplo de flag for necessário, usa-se o marcador `<flag_redacted>`.
> As flags reais vivem apenas em `api/src/flags.js` (e materializadas no
> `WALKTHROUGH.md`, que é material de instrutor).

---

## 1. Status do Lab 08

- **Tema:** segurança de aplicações mobile (Android) — backend + app.
- **Fase atual:** **Fase 16 — QA final** (preparação para o build real do APK).
- **Backend:** Node.js + Express, em memória, porta `8102` (Docker Compose).
- **App Android:** Kotlin + Jetpack Compose, em `android-app/` (código-fonte).
  O **APK final ainda não foi publicado** — o build é feito no Android Studio.
- **Cadeia de CTF:** `obsidianpay-mobile-final-chain`, 9 estágios, 2000 pontos
  (scoring local + submissão de flags). Ver `docs/CHALLENGE-SCORING.md`.

| Fase | Entrega | Status |
|---|---|---|
| 1–2 | Backend mobile + vulnerabilidades de API | ✅ |
| 3–4 | App Android base + armazenamento local inseguro | ✅ |
| 5–6 | Deep links / QR / WebView + JavaScript bridge | ✅ |
| 7 | Componentes Android exportados | ✅ |
| 8–9 | Reverse engineering / Device Trust + root/emulator check | ✅ |
| 10–11 | Secure Vault + Network Security / API Host override | ✅ |
| 12–13 | App Integrity / NativeGate + Dynamic Instrumentation scaffold | ✅ |
| 14 | Final Challenge Chain (flags, scoring, submit) | ✅ |
| 15 | Documentação final (walkthrough, student guide) | ✅ |
| 16 | QA final + preparação para build Android real | ✅ |
| **17** | **Android build readiness (Kotlin/Gradle/Manifest QA + assembleDebug best-effort)** | ✅ Atual |

---

## 2. Portas e URLs

| Ambiente | Base URL | Observação |
|---|---|---|
| **Backend (host)** | `http://127.0.0.1:8102` | A API roda no host via Docker Compose. Toda validação por `curl` usa esta URL. |
| **Emulador Android** | `http://10.0.2.2:8102` | `10.0.2.2` é o alias do emulador para o `127.0.0.1` do host. Padrão do app, sem configuração extra. |
| **Celular físico** | `http://<IP_DO_PC>:8102` | Configure o **API Host** com o IP LAN do PC (ex.: `http://192.168.0.50:8102`). O backend precisa estar acessível na rede. |

> No celular físico, `127.0.0.1` é o loopback do **próprio aparelho**, não do PC —
> por isso é obrigatório o IP de LAN via tela **API Host**.

---

## 3. Matriz de validação

Cada domínio do lab tem cobertura estrutural (e, quando aplicável, dinâmica via
Docker). Os scripts de fase ficam em `scripts/`.

| # | Domínio | O que é validado | Script(s) principal(is) |
|---|---|---|---|
| 1 | **Backend** | `/health`, login `guest`/`guest123`, contratos mobile, porta `8102` | `validate-phase1.sh`, `validate-phase2.sh`, `validate-phase14.sh` |
| 2 | **Android app structure** | projeto Gradle, manifest, telas, `ApiClient` | `validate-phase3.sh` |
| 3 | **Mobile storage** | SharedPreferences inseguro, SQLite, arquivos locais/cache | `validate-phase4.sh` |
| 4 | **Deep links / QR** | rotas `obsidianpay://`, `QrInputScreen`, roteamento | `validate-phase5.sh` |
| 5 | **WebView / bridge** | `WebViewSupportScreen`, `ObsidianSupportBridge` (`@JavascriptInterface`) | `validate-phase6.sh` |
| 6 | **Exported components** | `InternalOpsActivity`, `DebugCommandReceiver`, `ObsidianNotesProvider` | `validate-phase7.sh` |
| 7 | **Weak crypto / device trust** | `HardcodedSecrets`, `WeakCrypto`, `LegacyRequestSigner`, device-trust | `validate-phase8.sh` |
| 8 | **Root / emulator** | `RootDetector`, `EmulatorDetector`, `EnvironmentRiskEngine` | `validate-phase9.sh` |
| 9 | **Biometric vault** | `LocalAuthState`, `BiometricGate`, `VaultScreen`, vault-mobile | `validate-phase10.sh` |
| 10 | **Network / pinning** | `NetworkSecurityProfile`, `PinningPolicy`, `ApiHostOverrideScreen` | `validate-phase11.sh` |
| 11 | **Native / integrity** | `NativeGate`, `TamperCheck`, `IntegrityScreen`, app-integrity | `validate-phase12.sh` |
| 12 | **Frida / ADB toolkit** | `tools/frida/` (5 scripts), `tools/adb/`, docs de pentest | `validate-phase13.sh` |
| 13 | **Challenge chain / scoring** | `flags.js`, `challenge-chain.js`, progress/submit/scoreboard/finalize | `validate-phase14.sh` |
| 14 | **Docs / student guide / walkthrough** | conteúdo dos docs, anti-spoiler, anti-leak | `validate-phase15.sh` |
| 15 | **Final QA / release readiness** | consolidação, anti-leak, typos, marcadores de rascunho, build checklist | `validate-phase16.sh` |

---

## 4. Scripts de validação

A suíte completa de scripts cobre da Fase 1 à Fase 16:

```
scripts/validate-phase1.sh    # fundação / contrato de API
scripts/validate-phase2.sh    # vulnerabilidades de backend
scripts/validate-phase3.sh    # app Android base
scripts/validate-phase4.sh    # armazenamento local inseguro
scripts/validate-phase5.sh    # deep links / QR / WebView
scripts/validate-phase6.sh    # WebView JavaScript bridge
scripts/validate-phase7.sh    # componentes Android exportados
scripts/validate-phase8.sh    # hardcoded secrets / weak crypto / device trust
scripts/validate-phase9.sh    # root / emulator check
scripts/validate-phase10.sh   # Secure Vault / local auth
scripts/validate-phase11.sh   # network security / pinning scaffold
scripts/validate-phase12.sh   # App Integrity / NativeGate / TamperCheck
scripts/validate-phase13.sh   # dynamic instrumentation (Frida/adb)
scripts/validate-phase14.sh   # final challenge chain / scoring
scripts/validate-phase15.sh   # documentação final (anti-spoiler / anti-leak)
scripts/validate-phase16.sh   # QA final / release readiness (este passe)
```

`validate-phase16.sh` confere a presença de todos os scripts acima, valida os
docs finais, reforça os guards anti-leak, falha em typos e marcadores de rascunho
conhecidos e roda `validate-phase14.sh` + `validate-phase15.sh` internamente.
**Não exige Android SDK** — o build do APK continua best-effort no shell.

---

## 5. Anti-leak policy

### Onde os marcadores de flag PODEM existir

- `WALKTHROUGH.md` (material de instrutor).
- `api/src/flags.js` (registro central das flags).
- `scripts/validate-phase14.sh`, `scripts/validate-phase15.sh`,
  `scripts/validate-phase16.sh` (verificam as flags).

### Onde os marcadores de flag NÃO podem existir

- `README.md`, `STUDENT-GUIDE.md`.
- `docs/ARCHITECTURE.md`, `docs/PHASES.md`, `docs/VULNERABILITY-ROADMAP.md`,
  `docs/CHALLENGE-SCORING.md`, `docs/FINAL-QA.md`,
  `docs/ANDROID-BUILD-CHECKLIST.md`.
- `docs/mobile-pentest/SETUP.md`, `docs/mobile-pentest/PLAYBOOK.md`.
- `android-app/README.md`.
- `tools/` (scripts Frida/ADB).

> Em exemplos públicos, use o marcador `<flag_redacted>` no lugar da flag real.

### Onde credenciais internas NÃO podem aparecer

As contas didáticas internas (não-públicas) servem para descoberta via
mobile/RE/storage e suas senhas **não** podem aparecer em material público:

- `README.md`, `STUDENT-GUIDE.md`, `android-app/README.md`.
- `docs/FINAL-QA.md`, `docs/ANDROID-BUILD-CHECKLIST.md`.
- `tools/`.

A conta pública `guest` / `guest123` é a única credencial documentada para o
aluno.

---

## 6. Checklist antes do release

Execute na ordem; cada item deve passar antes de gerar o APK real.

- [ ] **`docker compose config`** renderiza o serviço mapeando `127.0.0.1:8102`.
- [ ] **`bash scripts/validate-phase14.sh`** passa (estrutural + dinâmico se houver Docker).
- [ ] **`bash scripts/validate-phase15.sh`** passa (documentação / anti-leak).
- [ ] **`bash scripts/validate-phase16.sh`** passa (QA final / release readiness).
- [ ] **Android Studio sync** — abrir `android-app/` e concluir o Gradle sync sem erros.
- [ ] **APK debug build** — `./gradlew assembleDebug` gera o APK debug (no Android Studio / com SDK).
- [ ] **Instalação em emulador** — instalar o APK debug num emulador (API 24+).
- [ ] **Login guest** — autenticar com `guest` / `guest123` no app.
- [ ] **API Host correto** — emulador em `http://10.0.2.2:8102`; celular físico em `http://<IP_DO_PC>:8102`.
- [ ] **Teste manual stage 01** — `GET /api/mobile/config` no modo de revisão devolve o checkpoint de recon.
- [ ] **Teste manual stage 04** — `GET /api/mobile/webview/support` (modo de auditoria) devolve o checkpoint da bridge.

> Os passos de Android Studio / APK / emulador são **best-effort no shell**: a
> validação automatizada não exige Android SDK. A preparação do build real está
> documentada em `docs/ANDROID-BUILD-CHECKLIST.md` e deve ser executada
> manualmente no Android Studio.

---

## 7. Status da Fase 17 — Android build readiness

A Fase 17 prepara o projeto Android para o **build real no Android Studio** sem
alterar backend, app, flags ou os endpoints da Fase 14. Entrega o script
`scripts/validate-phase17.sh`, que faz a inspeção estrutural forte do projeto
(Gradle/Manifest/recursos/Kotlin) e tenta o build real em modo best-effort.

- **Fase 17 preparada:** revisão estrutural do projeto Android (namespace
  `com.obsidianpay.mobile`, `minSdk`/`targetSdk`/`compileSdk`, `INTERNET`,
  `usesCleartextTraffic`, `networkSecurityConfig`, componentes exportados,
  scheme/hosts de deep link, todas as telas e pacotes, conteúdo Kotlin-chave e
  guards de typos).
- **Build shell best-effort:** `validate-phase17.sh` **não exige Android SDK**.
  Sem SDK detectado (via `ANDROID_HOME`/`ANDROID_SDK_ROOT`/`local.properties`
  `sdk.dir`), o build é apenas **WARN** — não falha. Com SDK detectado, o script
  roda `./gradlew --no-daemon :app:assembleDebug` e **falha** se o build falhar.
- **Build real obrigatório no Android Studio antes do release final:** a
  validação de shell não substitui o Android Studio. O APK debug real deve ser
  gerado e instalado conforme `docs/ANDROID-BUILD-CHECKLIST.md`.

### Critérios para considerar o APK validado

- [ ] **Gradle sync** conclui em `android-app/` sem erros no Android Studio.
- [ ] **`./gradlew assembleDebug`** gera `app/build/outputs/apk/debug/app-debug.apk`.
- [ ] **Instalação** do APK debug num emulador (API 24+) ou device.
- [ ] **Login `guest` / `guest123`** funciona no app.
- [ ] **API Host correto:** emulador em `http://10.0.2.2:8102`; celular físico em
      `http://<IP_DO_PC>:8102` (backend acessível fora de `127.0.0.1`).
- [ ] **Telas principais abrem** (Home, Receipts, Support, Web Support, API Host,
      Device Trust, Vault, App Integrity).
- [ ] **`bash scripts/validate-phase17.sh`** passa (estrutural + 14/15/16 embutidos).

---

## 8. Notas finais

- Ambiente **local apenas** (`127.0.0.1:8102`); nada deste lab deve ser usado
  contra apps ou sistemas reais.
- As flags reais não saem de `api/src/flags.js` / `WALKTHROUGH.md`.
- A pendência conhecida de release é o **build real do APK** (e a publicação do
  artefato), que depende do Android Studio.
