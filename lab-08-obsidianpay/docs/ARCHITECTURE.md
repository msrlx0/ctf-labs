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

### Fluxo de storage local

```
App Android ─┬─▶ SharedPreferences (sessão, token, cache de perfil/config)
             ├─▶ SQLite obsidianpay_local.db (cached_receipts/cards, debug_events)
             ├─▶ filesDir/ (receipts/*.json, debug/export.json) e cacheDir/ (snapshot)
             └─▶ external app-specific (obsidian-export.txt)
                     ▲
                     └── alimentado pelas respostas da API em 10.0.2.2:8102
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
