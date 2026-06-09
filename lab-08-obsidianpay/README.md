# Lab 08 — ObsidianPay Mobile

**Tema:** Segurança de aplicações mobile (Android) — backend + app
**Porta oficial:** http://127.0.0.1:8102
**Status:** Fase 1 (fundação + backend mínimo). O APK Android será entregue em fases futuras.
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

Para um teste completo da Fase 1, veja [VALIDATION.md](./VALIDATION.md) ou rode
[scripts/validate-phase1.sh](./scripts/validate-phase1.sh).

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

## Estado atual

- ✅ Backend mínimo funcional (API mobile) na porta 8102
- ✅ Documentação base e arquitetura
- 🔜 APK Android (fases futuras)
- 🔜 Cadeias de vulnerabilidade do app

> **O APK Android ainda não foi entregue.** Esta fase estabelece a fundação,
> os contratos técnicos e o backend mínimo.
