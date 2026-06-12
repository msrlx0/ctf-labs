# Lab 08 — ObsidianPay Mobile

**Tema:** Segurança de aplicações mobile (Android) — backend + app
**Porta oficial:** http://127.0.0.1:8102
**Status:** Fase 9 (app com **deep links, QR Payment, Web Support, WebView bridge, componentes Android internos, fluxo Device Trust legado e checagem local de ambiente/dispositivo**). O APK final ainda não foi publicado.
**Dificuldade alvo:** Hard / realista (acima de labs introdutórios como AndroGoat).

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

> Estas são credenciais didáticas controladas, não segredos. Não há flags reais
> neste repositório público.

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

## App Android

A partir da Fase 3 existe um **app Android base** (Kotlin + Jetpack Compose) em
[android-app/](./android-app/), que consome a API local.

- No **Android Emulator**, o app usa `http://10.0.2.2:8102` (alias do emulador
  para o `127.0.0.1` do host).
- Abra a pasta `android-app/` no Android Studio e rode em um emulador (API 24+).
- Login de teste: `guest` / `guest123`.

Detalhes de build e execução em [android-app/README.md](./android-app/README.md).

## Estado atual

- ✅ Backend mobile (API rica) na porta 8102 — Fase 2
- ✅ App Android base (telas + cliente HTTP) — Fase 3
- ✅ Cache local/offline do app (perfil, config, recibos, cartões) — Fase 4
- ✅ Deep links, QR Payment e Web Support no app — Fase 5
- ✅ WebView **support bridge** para suporte mobile assistido — Fase 6
- ✅ **Componentes Android internos** (integrações de operações/diagnóstico) — Fase 7
- ✅ **Device Trust** legado + configurações internas para análise mobile — **Fase 8**
- ✅ Documentação base e arquitetura
- 🔜 APK final publicado
- 🔜 Cadeias completas app ↔ API

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

> **O APK final ainda não foi publicado.** A Fase 3 entrega o código-fonte do
> app base. Trate o app e a API como alvos reais: explore, observe e questione.
