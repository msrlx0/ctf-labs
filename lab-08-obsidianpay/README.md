# Lab 08 — ObsidianPay Mobile

**Tema:** Segurança de aplicações mobile (Android) — backend + app
**Porta oficial:** http://127.0.0.1:8102
**Dificuldade alvo:** Hard / realista (acima de labs introdutórios como AndroGoat).

Backend mobile (porta `8102`), app Android (Kotlin + Jetpack Compose) e a
**Final Challenge Chain** (9 estágios) estão implementados. O foco atual é a
**distribuição do APK**: a validação em **celular físico está em andamento** e o
teste de runtime **ainda não está concluído**.

---

## 📥 Download e status do APK

> **APK estável: ainda NÃO publicado.** O que existe hoje é um **candidato a QA
> (`v1.0.0-rc2`)**, pendente de aprovação em **celular físico**.

A **Fase 22A** adiciona o **pipeline automatizado de build** do APK via GitHub
Actions ([`.github/workflows/lab08-android-apk.yml`](../.github/workflows/lab08-android-apk.yml)),
que gera o artefato candidato `ObsidianPay-Lab08-v1.0.0-rc2.apk` (+ `.sha256`)
para o smoke test em dispositivo físico. A **publicação estável** ocorrerá apenas
**depois** que esse smoke test passar.

- **Como baixar, verificar o SHA256 e instalar:** veja **[DOWNLOAD.md](./DOWNLOAD.md)**.
- Build manual no Android Studio: [docs/ANDROID-BUILD-CHECKLIST.md](./docs/ANDROID-BUILD-CHECKLIST.md).

> **Fase 23A — redesign de UI/UX + RC2.** O app Android foi redesenhado como um
> produto fintech coeso (Material 3, tema escuro "obsidian", navegação por abas
> Início / Transferir / Cartões / Segurança / Conta), preservando integralmente
> todas as vulnerabilidades intencionais, componentes exportados, deep links,
> WebView/bridge, armazenamento inseguro, biometria, detecção de root/emulador,
> rede/pinning e o contrato da Final Challenge Chain. O candidato passou de `rc1`
> para **`v1.0.0-rc2`** (`versionCode 2`). A configuração de rede agora usa
> **presets** (emulador `10.0.2.2:8102`, dispositivo físico via `adb reverse`
> `127.0.0.1:8102`, ou endpoint personalizado) — o app **não** usa mais nenhum
> default de runtime na porta `8080`.

---

## Descrição

ObsidianPay é uma **carteira digital / app de pagamentos** fictícia. O laboratório
simula o ecossistema de um app financeiro real: um aplicativo Android consumindo
uma API mobile. O objetivo é praticar análise de segurança mobile de forma
**manual e investigativa**, sem depender de scanners automáticos.

Diferente de apps "menu de vulnerabilidades", o ObsidianPay é desenhado para
parecer um produto real. As fraquezas estão integradas ao fluxo do produto e
precisam ser descobertas como em um pentest real.

## Objetivo educacional

- Entender a superfície de ataque de um app financeiro mobile.
- Praticar análise estática e dinâmica de forma manual.
- Conectar achados no cliente Android com falhas na API.
- Usar OWASP **MASVS/MASTG** como referência conceitual.

## Escopo autorizado

- **Somente local.** Todo o ambiente roda em `127.0.0.1`.
- Não exponha os containers na internet.
- Não use técnicas/payloads deste lab contra sistemas reais ou de terceiros.
- Alvo único e autorizado: `http://127.0.0.1:8102`.

---

## Pré-requisitos

**Agora (Fase 1):**
- Docker e Docker Compose
- `curl` (opcional, para validação)

**Fases futuras (componente Android):**
- Android Studio + Emulator
- `adb`
- Burp Suite
- JADX
- apktool
- Frida
- objection

---

## Como subir o backend

A partir da pasta do lab:

```bash
cd lab-08-obsidianpay
docker compose up --build
```

O serviço sobe em `http://127.0.0.1:8102`.

## Como validar

```bash
curl -s http://127.0.0.1:8102/health
```

Resposta esperada (resumo):

```json
{ "status": "ok", "name": "ObsidianPay Mobile", "expectedPort": 8102 }
```

Para derrubar:

```bash
docker compose down
```

Para um teste completo, veja [VALIDATION.md](./VALIDATION.md) ou rode os scripts
[scripts/validate-phase1.sh](./scripts/validate-phase1.sh) e
[scripts/validate-phase2.sh](./scripts/validate-phase2.sh).

---

## Endpoints principais (visão geral)

A API mobile expõe, entre outros:

| Método | Rota | Descrição |
|---|---|---|
| GET | `/health` | Status do lab |
| GET | `/` | Identificação da API |
| POST | `/api/mobile/login` | Autenticação mobile |
| GET / PATCH | `/api/mobile/profile` | Perfil do usuário |
| GET | `/api/mobile/config` | Configuração mobile |
| GET | `/api/mobile/receipts` | Recibos |
| GET | `/api/mobile/cards` | Cartões |
| POST | `/api/mobile/support/sync` | Sincronização de suporte (legado) |
| GET | `/api/mobile/webview/support` | Portal de suporte (WebView) |
| POST | `/api/mobile/transfer/preview` | Prévia de transferência (QR/deep link) |

> Os contratos acima são os que o aplicativo Android consumirá nas próximas
> fases. A documentação **não** descreve como explorar a API: a investigação é
> parte do exercício.

---

## Conta de teste (Fase 1)

| Usuário | Senha | Papel |
|---|---|---|
| `guest` | `guest123` | customer |

Use **guest / guest123** no app e em `POST /api/mobile/login`.

> Estas são credenciais didáticas controladas, não segredos. Não há flags reais
> neste repositório público.

---

## Documentação

- **Guia do aluno (sem spoilers):** [STUDENT-GUIDE.md](./STUDENT-GUIDE.md) — por
  onde começar, como pensar a investigação e como participar da **Challenge
  Chain** (9 estágios) registrando progresso e evidências.
- **Pontuação da cadeia (público):** [docs/CHALLENGE-SCORING.md](./docs/CHALLENGE-SCORING.md).
- **Setup de pentest mobile:** [docs/mobile-pentest/SETUP.md](./docs/mobile-pentest/SETUP.md).
- **Checklist de build Android:** [docs/ANDROID-BUILD-CHECKLIST.md](./docs/ANDROID-BUILD-CHECKLIST.md).
- **QA final (release readiness):** [docs/FINAL-QA.md](./docs/FINAL-QA.md).
- **Validação técnica:** [VALIDATION.md](./VALIDATION.md).
- **Playbook de pentest mobile:** [docs/mobile-pentest/PLAYBOOK.md](./docs/mobile-pentest/PLAYBOOK.md).

> **`WALKTHROUGH.md` é material de instrutor** (contém a solução completa e as
> flags). Não é destinado ao aluno — comece sempre por
> [STUDENT-GUIDE.md](./STUDENT-GUIDE.md).

---

## Status atual e QA

- **Backend e app:** o backend mobile (porta `8102`), o código-fonte do app
  Android e a Challenge Chain de 9 estágios estão implementados e estáveis em
  runtime (clash JVM, crash da tela "Configuração", WebView em celular físico,
  `RootDetector` básico, `/health` em `version 1.0.0` com `challengeStages: 9` e
  o **Stage 03** com checkpoint real
  `/api/mobile/challenge/checkpoint/exported-components` — a flag continua só no
  backend).
- **Distribuição do APK (Fase 22A):** o GitHub passa a ser o ponto oficial de
  download. O workflow **Lab 08 Android APK** gera um **candidato a QA**
  (`ObsidianPay-Lab08-v1.0.0-rc2.apk` + `.sha256`) como artefato do GitHub
  Actions. Veja **[DOWNLOAD.md](./DOWNLOAD.md)**.
- **APK estável:** **ainda NÃO publicado.** A **validação em celular físico está
  em andamento**; o teste de runtime ponta a ponta **ainda não está concluído**.
  A publicação estável (GitHub Releases) ocorrerá só depois do smoke test passar.
- **Documentação de apoio:**
  - [DOWNLOAD.md](./DOWNLOAD.md) — download, verificação de SHA256 e instalação do APK.
  - [STUDENT-GUIDE.md](./STUDENT-GUIDE.md) — guia do aluno (sem spoilers).
  - [docs/CHALLENGE-SCORING.md](./docs/CHALLENGE-SCORING.md) — pontuação da cadeia.
  - [docs/mobile-pentest/SETUP.md](./docs/mobile-pentest/SETUP.md) — ambiente de pentest mobile.
  - [docs/ANDROID-BUILD-CHECKLIST.md](./docs/ANDROID-BUILD-CHECKLIST.md) — passo a passo do build no Android Studio.
  - [docs/FINAL-QA.md](./docs/FINAL-QA.md) — matriz de validação e checklist de release.

> **`WALKTHROUGH.md` é instrutor-facing** (solução completa + flags). O aluno
> nunca precisa dele.

---

## Temas que serão abordados (conceitual)

> Lista de **tópicos** que o lab cobrirá em fases futuras. É um mapa conceitual,
> **não** um passo a passo nem uma solução.

- Network interception
- Certificate pinning
- Insecure data storage
- Exported Android components
- Deep links
- WebView bridge
- QR Code input
- SQLite local injection
- Hardcoded secrets
- Root detection bypass
- Biometric auth bypass
- Binary patching
- API authorization flaws

Para o detalhamento por trilhas (com status), veja
[docs/VULNERABILITY-ROADMAP.md](./docs/VULNERABILITY-ROADMAP.md).

---

## Vulnerabilidades presentes

As tabelas abaixo resumem, em linguagem simples, **o que o ObsidianPay expõe de
propósito**. Elas servem de mapa: dizem **onde** olhar e **o que** cada item
ensina, mas **não** entregam flags, headers finais nem payloads — descobrir o
"como" é o exercício. Tudo roda em ambiente **local e autorizado**
(`127.0.0.1:8102` / emulador `10.0.2.2:8102`); nada aqui deve ser usado contra
apps ou sistemas de terceiros.

Esta primeira tabela lista as **vulnerabilidades reais e controles fracos** —
itens efetivamente exploráveis no lab. (Scaffolds educacionais e recursos do CTF
estão separados logo abaixo.)

| Categoria | Vulnerabilidade | Onde aparece no lab | O que o aluno aprende |
|---|---|---|---|
| Armazenamento mobile | **Insecure Mobile Storage** | SharedPreferences (`InsecureSessionStore`), SQLite (`obsidianpay_local.db`), `filesDir`/`cacheDir` e export app-specific externo; tela "Local State". | Procurar tokens, perfis, cache e artefatos deixados em claro no dispositivo. |
| API / Autorização | **API Broken Access Control / IDOR** | Recibos e cartões acessíveis por ID previsível (`/api/mobile/receipts`, `/api/mobile/cards` e detalhe por ID). | Testar acesso indevido a objetos de outros perfis em APIs mobile. |
| API / Autorização | **Mass Assignment** | `PATCH /api/mobile/profile` aceitando campos sensíveis além dos esperados. | Testar parâmetros extras no corpo de uma requisição. |
| Recon | **Information Disclosure** | `config`, diagnostics, rotas legadas, hints e respostas com metadados. | Mapear pistas e superfície do produto **sem** usar scanner. |
| WebView | **WebView Misconfiguration** | `WebViewSupportScreen` com JavaScript/DOM Storage e conteúdo controlado pelo portal de suporte. | Entender os riscos de uma WebView mal configurada em apps mobile. |
| WebView | **JavaScript Bridge Exposure** | `ObsidianBridge` (`@JavascriptInterface`) expõe métodos ao conteúdo da WebView. | Avaliar o impacto de uma bridge JS exposta ao conteúdo web. |
| Entrada não confiável | **Deep Link / QR Input Abuse** | Scheme `obsidianpay://` (transfer/support/receipt) e a tela QR Payment com payloads previsíveis. | Testar entrada via deep link e QR como vetor não confiável. |
| Componentes Android | **Exported Activity** | `InternalOpsActivity` exportada no `AndroidManifest`. | Reconhecer o risco de uma Activity exportada para outros apps. |
| Componentes Android | **Exported BroadcastReceiver** | `DebugCommandReceiver` exportado, com comandos previsíveis. | Entender o abuso de broadcasts previsíveis. |
| Componentes Android | **Exported ContentProvider** | `ObsidianNotesProvider` exportado (authority `provider.notes`, incl. path `/checkpoint` que consolida provas dos componentes). | Enumerar dados locais via `content://` e coletar provas para o checkpoint do Stage 03. |
| Reverse engineering | **Hardcoded Secrets** | `HardcodedSecrets` (segredos/rotas/valores internos fragmentados no binário). | Engenharia reversa básica de um APK com JADX/apktool. |
| Reverse engineering | **Weak Crypto / Legacy Signature** | `WeakCrypto` (Base64/XOR/SHA-1/MD5) e `LegacyRequestSigner`. | Por que assinatura/cripto client-side fraca é quebrável. |
| Confiança no cliente | **Device Trust** bypass | Fluxo Device Trust e endpoint interno baseado em headers montados no cliente. | Que confiar no que o cliente afirma é frágil. |
| Anti-análise | **Root Detection** bypass | `RootDetector` (monitor-only) na tela Security Check. | Que checks locais de root podem ser hookados/observados. |
| Anti-análise | **Emulator Detection** bypass | `EmulatorDetector` (monitor-only). | Observar/contornar a detecção de ambiente. |
| Autenticação local | **Biometric / Local Auth** bypass | `LocalAuthState` / `BiometricGate` e a tela Secure Vault. | O risco de usar autorização local como "prova" para o servidor. |
| Rede | **Network Security / Cleartext / API Host** | `network_security_config`, cleartext local e a tela API Host. | A diferença entre emulador, celular físico e backend local. |

## Scaffolds e técnicas educacionais

Estes itens **não são vulnerabilidades exploráveis** por si só: são estruturas
didáticas (scaffolds) e ferramentas para estudar técnicas de bypass de forma
controlada. No estado atual do lab eles são **report-only / observacionais** — o
servidor nunca bloqueia com base neles, e nenhum entrega flag sozinho.

| Categoria | Item | Onde aparece no lab | O que o aluno aprende |
|---|---|---|---|
| Rede | **Certificate Pinning** scaffold | `PinningPolicy` / `CertificatePinner` em Kotlin (modo `disabled-local-lab` / report-only); **sem pinning nativo real**. | Observar e entender o bypass conceitual de pinning em lab. |
| Integridade do app | **Native/JNI Integrity** scaffold (NativeGate) | `NativeGate` (gate nativo **opcional** com fallback Kotlin; nenhuma `.so` real é exigida). | Que um gate nativo também precisa de validação no servidor. |
| Integridade do app | **Anti-Tamper** checks (TamperCheck) | `TamperCheck` (debuggable/installer/signature/package), report-only. | Os limites de checks de integridade locais (e binary patching como técnica). |
| Instrumentação | **Dynamic Instrumentation** | Scripts Frida e playbook ADB do laboratório (`tools/`). | Observação/hooking controlado de um pacote local. |

## Recursos do CTF

Infraestrutura de CTF do lab — **não são vulnerabilidades**, e sim o mecanismo de
progresso/pontuação. As flags reais vivem apenas em `api/src/flags.js` (e no
`WALKTHROUGH.md`, material de instrutor); **nenhuma flag aparece neste README**.

| Recurso | Onde aparece no lab | Para que serve |
|---|---|---|
| **Challenge Chain** (9 estágios) | `api/src/challenge-chain.js` + `challenge/progress` | Cadeia oficial de 9 estágios na ordem didática. |
| Submissão de flags | `POST /api/mobile/challenge/submit` | Validar a flag de cada estágio e pontuar (idempotente). |
| Scoreboard / progress | `challenge/scoreboard`, `challenge/progress` | Acompanhar `totalScore`, `completionPercent` e `finalUnlocked`. |
| Final chain | `POST /api/mobile/internal/finalize-operator` | Estágio final (header device-trust + 4 provas). |
| Flags | `api/src/flags.js` (privado) | Registro central das 9 flags — fora de docs públicos. |

> As tabelas são **informativas**, não um walkthrough: citam telas, arquivos e
> conceitos, mas a investigação (encontrar as flags e os caminhos exatos) é sua.
> A solução completa fica apenas em `WALKTHROUGH.md` (material de instrutor).

---

## App Android

A partir da Fase 3 existe um **app Android base** (Kotlin + Jetpack Compose) em
[android-app/](./android-app/), que consome a API local.

- No **Android Emulator**, o app usa `http://10.0.2.2:8102` (alias do emulador
  para o `127.0.0.1` do host).
- Em um **celular físico**, use a tela **API Host** para apontar o app ao IP do
  PC na rede, por exemplo `http://192.168.0.50:8102`. A **WebView de suporte**
  também segue essa base URL efetiva, funcionando no emulador **e** em celular
  físico (antes ficava fixa em `10.0.2.2`).
- **QA em celular físico sem expor a LAN:** use `adb reverse tcp:8102 tcp:8102` e
  configure o **API Host** para `http://127.0.0.1:8102` — o tráfego do aparelho é
  encaminhado ao backend local do PC pelo USB (ver [DOWNLOAD.md](./DOWNLOAD.md)).
- Abra a pasta `android-app/` no Android Studio e rode em um emulador (API 24+).
- Login de teste: `guest` / `guest123`.

> **Build Android real (Fase 20):** o build/instalação do APK é **obrigatório** e
> deve seguir [docs/ANDROID-BUILD-CHECKLIST.md](./docs/ANDROID-BUILD-CHECKLIST.md)
> no Android Studio (`./gradlew --no-daemon clean :app:assembleDebug` →
> `BUILD SUCCESSFUL`). A Fase 20 corrigiu o clash de assinatura JVM que impedia a
> compilação, o crash da tela "Configuração" e a WebView fixa. A validação de
> shell (`scripts/validate-phase20.sh`) roda o build real quando há Android SDK e
> apenas **avisa** quando não há — **não substitui** o Android Studio.

Detalhes de build e execução em [android-app/README.md](./android-app/README.md).

## Estado atual

- ✅ Backend mobile (API rica) na porta 8102 — Fase 2
- ✅ App Android base (telas + cliente HTTP) — Fase 3
- ✅ Cache local/offline do app (perfil, config, recibos, cartões) — Fase 4
- ✅ Deep links, QR Payment e Web Support no app — Fase 5
- ✅ WebView **support bridge** para suporte mobile assistido — Fase 6
- ✅ **Componentes Android internos** (integrações de operações/diagnóstico) — Fase 7
- ✅ **Device Trust** legado + configurações internas para análise mobile — **Fase 8**
- ✅ **Security Check** (root/emulator detection) — **Fase 9**
- ✅ **Secure Vault** com fluxo local de autenticação (biometria scaffold + fallback PIN fraco) — **Fase 10**
- ✅ **Network Security / API Host override** (emulador ↔ celular físico, pinning scaffold, backend network-profile) — **Fase 11**
- ✅ **App Integrity / NativeGate / TamperCheck scaffold** — **Fase 12**
- ✅ **Dynamic Instrumentation scaffold** (scripts Frida, playbook ADB, docs de pentest mobile) — **Fase 13**
- ✅ **Final Challenge Chain** (9 estágios, flags internas, scoring local, endpoint de submissão — ver `docs/CHALLENGE-SCORING.md`) — **Fase 14**
- ✅ **Runtime stabilization + build real + Stage 03 solucionável** (clash JVM, crash da "Configuração", WebView em celular físico, RootDetector básico, `/health 1.0.0`, checkpoint do Stage 03) — **Fase 20**
- ✅ Documentação base e arquitetura
- ✅ Pipeline automatizado de build do APK (GitHub Actions) — **Fase 22A**
- 🚧 Validação em celular físico (smoke test) — **em andamento**
- 🔜 APK estável publicado (após o smoke test em celular físico passar)

> A Fase 5 adiciona **deep links** (`obsidianpay://transfer|support|receipt`),
> uma tela **QR Payment** que interpreta payloads colados/digitados, e um
> **Web Support** em WebView que carrega o portal de suporte do backend local.

> A Fase 6 adiciona uma **support bridge** ao Web Support: a WebView passa a
> expor uma interface JavaScript (`ObsidianBridge`) usada pelo portal de suporte
> para mostrar contexto local do app (resumo de sessão, status, diagnóstico).
> Como em apps reais, observe **o que** essa ponte de suporte disponibiliza para
> a página — a investigação faz parte do exercício.

> A Fase 7 adiciona **componentes Android internos** que simulam integrações de
> um app real (uma tela interna de operações/diagnóstico, um gancho de automação
> de debug e um provedor de notas de suporte). Como em qualquer app Android, vale
> observar **quais** componentes o app expõe ao sistema e **o que** cada um
> disponibiliza — a investigação faz parte do exercício. (Sem flags.)

> A Fase 8 adiciona um fluxo **Device Trust** (checagem de segurança/atestação do
> dispositivo) e **configurações internas** embutidas no cliente. Como em apps
> reais que montam credenciais localmente, a análise estática (JADX/apktool/
> `strings`) do app revela como esse fluxo é construído. Vale observar **como** o
> app prepara essa confiança e **o que** isso implica — a investigação faz parte
> do exercício. (Sem flags; sem segredos reais.)

> A Fase 10 adiciona um **Secure Vault** com fluxo didático de autenticação
> local: um scaffold de biometria e um mecanismo de fallback. Como em apps
> financeiros reais, o app decide localmente se o vault está desbloqueado e informa
> o servidor — observe o que o servidor efetivamente verifica e **o que** isso
> implica para a segurança do fluxo. A investigação faz parte do exercício.

> A Fase 11 adiciona suporte a **API Host override** para facilitar testes em
> emulador (`10.0.2.2`) e celular físico (IP do PC na LAN). A tela "API Host"
> permite trocar a base URL sem rebuildar o app — o override é salvo localmente.
> A fase também introduz um scaffold de **certificate pinning** para estudo
> futuro: `NetworkSecurityProfile`, `PinningPolicy` e um comentário no `ApiClient`
> mostrando onde o `CertificatePinner` seria anexado. O backend expõe
> `/api/mobile/internal/network-profile` com o perfil de rede atual.

> **O APK estável ainda não foi publicado.** A Fase 22A entrega o pipeline de
> build e um **APK candidato a QA** (ver [DOWNLOAD.md](./DOWNLOAD.md)); a validação
> em celular físico está em andamento. Trate o app e a API como alvos reais:
> explore, observe e questione.
