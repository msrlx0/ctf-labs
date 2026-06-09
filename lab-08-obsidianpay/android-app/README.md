# android-app/ (placeholder)

Esta pasta hospedará o **aplicativo Android ObsidianPay** nas próximas fases.

> **Status: Fase 1 — o APK ainda não foi entregue.**
> Nesta fase existe apenas o backend (`../api`) e a documentação.

## O que virá aqui (fases futuras)

- Projeto Android (Kotlin/Java) do app de carteira ObsidianPay.
- Telas realistas: login, saldo, recibos, suporte (WebView), QR/pagamentos.
- Consumo da API mobile já definida na Fase 1 (`http://127.0.0.1:8102`).
- Superfícies de estudo: componentes exportados, deep links, WebView/bridge,
  storage local, criptografia, controles anti-tampering.
- Artefatos de build (APK) e instruções de instalação no emulador.

## Pré-requisitos (para quando o app existir)

- Android Studio + Emulator
- `adb`
- JADX, apktool, Burp Suite, Frida, objection

Consulte [../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) e
[../docs/VULNERABILITY-ROADMAP.md](../docs/VULNERABILITY-ROADMAP.md) para o plano
técnico, e [../docs/PHASES.md](../docs/PHASES.md) para o cronograma.
