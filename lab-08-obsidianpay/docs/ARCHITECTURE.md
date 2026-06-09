# Arquitetura — Lab 08: ObsidianPay Mobile

Documento técnico da arquitetura do laboratório. Vale tanto para a Fase 1 (atual)
quanto como contrato para as fases futuras.

---

## 1. Componentes do lab

| Componente | Estado | Descrição |
|---|---|---|
| ObsidianPay Mobile API | ✅ Fase 1 | Backend Node.js + Express em `127.0.0.1:8102`. |
| App Android (APK) | 🔜 futuro | Cliente de carteira/pagamentos que consome a API. |
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

## 2. Backend local (Fase 1)

- **Stack:** Node.js 20 + Express.
- **Persistência:** estado em memória (`api/src/data.js`). SQLite/persistência
  podem ser introduzidos quando uma vulnerabilidade de storage exigir.
- **Bind:** `0.0.0.0:8102` dentro do container; publicado em `127.0.0.1:8102`.
- **Contrato de API (Fase 1):**
  - `GET /` — identificação do serviço.
  - `GET /health` — status/versão/porta.
  - `POST /api/mobile/login` — `guest`/`guest123` → token didático.
  - `GET /api/mobile/profile` — requer `Authorization: Bearer`.
  - `GET /api/mobile/config` — configuração mobile simulada.
  - `GET /api/mobile/receipt/:id` — recibo (ownership validado na Fase 1).
  - `POST /api/mobile/support/sync` — stub legado de sincronização.
- **Respostas de erro:** sempre JSON (`{ error, message }`).

---

## 3. App Android (futuro)

Planejado para fases seguintes. Espera-se que o app:

- Implemente fluxos reais de carteira (login, saldo, recibos, suporte, QR, pagamentos).
- Consuma a API mobile já definida na Fase 1.
- Exponha superfícies clássicas de Android: componentes exportados, deep links,
  WebView, storage local, criptografia, controles anti-tampering.

O APK **não** é um "menu de vulnerabilidades": as fraquezas ficam embutidas em
um produto que parece legítimo.

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
