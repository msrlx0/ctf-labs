# Fases — Lab 08: ObsidianPay Mobile

Plano de fases do laboratório. Apenas a Fase 1 está implementada.

| Fase | Foco | Status |
|---|---|---|
| **Fase 1** | Fundação: arquitetura, documentação base, backend mínimo e contratos de API. | ✅ Atual |
| Fase 2 | APK Android inicial: app de carteira realista consumindo a API; recon estático. | 🔜 Planejada |
| Fase 3 | Trilha 1 (network/API): interceptação, tráfego legado/HTTP, pinning. | 🔜 Planejada |
| Fase 4 | Trilha 2 (storage/RE): SharedPreferences, SQLite, cache, segredos, cripto fraca. | 🔜 Planejada |
| Fase 5 | Trilha 3 (platform): componentes exportados, deep links, QR. | 🔜 Planejada |
| Fase 6 | Trilha 4 (WebView): settings inseguras, bridge JS, cadeia deep link → WebView. | 🔜 Planejada |
| Fase 7 | Trilha 5 (anti-analysis/auth): root/emulador/biometria, binary patching, BAC/mass assignment. | 🔜 Planejada |
| Fase 8 | Consolidação: cadeias completas, SOLUTION.md, evidências e validação ponta a ponta. | 🔜 Planejada |

## Escopo da Fase 1 (entregue)

- Backend Node.js + Express em `127.0.0.1:8102`.
- Endpoints: `/`, `/health`, `login`, `profile`, `config`, `receipt/:id`, `support/sync`.
- Conta de teste `guest` / `guest123`; token didático previsível.
- Docker Compose com healthcheck.
- Documentação: README, STUDENT-GUIDE, WALKTHROUGH (interno), ARCHITECTURE,
  VULNERABILITY-ROADMAP.
- Script `scripts/validate-phase1.sh`.

## Princípios entre fases

- Não quebrar o contrato da Fase 1 sem necessidade.
- Manter o produto realista (sem "menu de bugs").
- Nada de flags reais em arquivos públicos.
- Tudo **local only**, sem dependências externas.
