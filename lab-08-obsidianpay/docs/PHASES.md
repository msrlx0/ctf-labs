# Fases — Lab 08: ObsidianPay Mobile

Plano de fases do laboratório. Fases 1–3 implementadas.

| Fase | Foco | Status |
|---|---|---|
| **Fase 1** | Fundação: arquitetura, documentação base, backend mínimo e contratos de API. | ✅ Concluída |
| **Fase 2** | API mobile rica + primeiras vulnerabilidades de backend (IDOR, mass assignment, gates fracos, scaffolds de QR/WebView/vault). | ✅ Concluída |
| **Fase 3** | App Android base (Kotlin + Compose): telas, cliente HTTP, SharedPreferences inseguro, enumeração manual por ID, suporte/diagnostics, transfer preview. | ✅ Atual |
| Fase 4 | Recon estático do app + trilha network/API (interceptação, legado/HTTP, pinning). | 🔜 Planejada |
| Fase 5 | Trilha storage/RE: SharedPreferences, SQLite, cache, segredos, cripto fraca. | 🔜 Planejada |
| Fase 6 | Trilha platform: componentes exportados, deep links, QR. | 🔜 Planejada |
| Fase 7 | Trilha WebView: settings inseguras, bridge JS, cadeia deep link → WebView. | 🔜 Planejada |
| Fase 8 | Trilha anti-analysis/auth: root/emulador/biometria, binary patching, BAC/mass assignment. | 🔜 Planejada |
| Fase 9 | Consolidação: cadeias completas, SOLUTION.md, evidências e validação ponta a ponta. | 🔜 Planejada |

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

## Princípios entre fases

- Não quebrar o contrato da Fase 1 sem necessidade.
- Manter o produto realista (sem "menu de bugs").
- Nada de flags reais em arquivos públicos.
- Tudo **local only**, sem dependências externas.
