# WALKTHROUGH (Instrutor) — Lab 08: ObsidianPay Mobile

> **Documento interno do instrutor.** Não é material do aluno.
>
> **Estado: Fase 4.** Este walkthrough descreve a arquitetura, as cadeias
> futuras em alto nível, as **vulnerabilidades de backend da Fase 2**, o **app
> Android base da Fase 3** e, agora, o **armazenamento local inseguro da Fase 4**
> (visão de instrutor, sem cadeia final longa).
> Ele **não** contém:
> - a cadeia/solução final do Lab 8,
> - payloads avançados ou exploits prontos extensos.
>
> Marcadores de progresso (`FLAG{...}`) existem apenas nos dados do backend
> (`api/src/data.js`), nunca em documentos públicos nem no app.

---

## 1. Visão geral da arquitetura planejada

```
┌──────────────────────────┐        HTTP (local)        ┌───────────────────────────┐
│  App Android (futuro)    │ ─────────────────────────▶ │  ObsidianPay Mobile API     │
│  - UI carteira/pagamentos│                            │  Node.js + Express          │
│  - storage local         │ ◀───────────────────────── │  127.0.0.1:8102             │
│  - WebView support center│        JSON/token          │  estado em memória (fase 1) │
│  - deep links / QR        │                            └───────────────────────────┘
└──────────────────────────┘
```

- **Fase 1 (atual):** apenas a API. Define contratos: login, profile, config,
  receipt, support/sync. Token didático previsível (costura para vuln futura).
- **Fases seguintes:** o APK Android consome esses contratos e introduz as
  cadeias de vulnerabilidade do lado cliente, conectadas à API.

O ObsidianPay deve sempre **parecer um produto financeiro real**. Vulnerabilidades
ficam embutidas no fluxo de produto, nunca como um "menu de bugs".

---

## 2. Cadeias futuras planejadas (alto nível)

Sequência didática pretendida — sem soluções nesta fase:

1. **Recon do APK** — inventário de telas, permissões, componentes, endpoints,
   strings e configs. Base para todo o resto.
2. **Interceptação de tráfego** — observar o diálogo app↔API; identificar o que
   trafega, como e onde a confiança de transporte pode ser quebrada (local).
3. **Armazenamento local** — onde o app guarda token/dados sensíveis e o que
   vaza em SharedPreferences, SQLite, cache e arquivos temporários.
4. **Componentes exportados** — Activities/Services/Receivers/Providers
   exportados e o que pode ser acionado por outro app.
5. **Deep link + WebView** — roteamento por deep link levando a uma WebView com
   configurações inseguras e/ou bridge JS exposta.
6. **Frida / pinning / root / biometria** — instrumentação dinâmica para estudar
   pinning, detecção de root/emulador e callbacks biométricos.
7. **API authorization** — fechar a cadeia explorando autorização da API
   (acesso a objetos, mass assignment, etc.), ligando cliente e servidor.

---

## 3. Costuras já plantadas na Fase 1

Itens já presentes no backend que sustentam vulnerabilidades futuras
(intencionalmente **inofensivos** agora):

- **Token previsível/decodificável** em `/api/mobile/login` — base para forja de
  token / sessão fraca no futuro.
- **Múltiplos recibos** no modelo (`data.js`), incluindo de outro `ownerUserId`
  — preparação para estudo de autorização a nível de objeto. A Fase 1 ainda
  **valida ownership** (comportamento correto).
- **`legacySupportEndpoint` em cleartext** e **`/api/mobile/support/sync`** como
  stub legado — gancho para tráfego HTTP/legacy.
- **`mobileFeatureFlags`** (biometric, qr, webview, deep link) — sinalizam as
  superfícies que o app vai expor.

---

## 3.1 Fase 2 — vulnerabilidades de backend introduzidas

A Fase 2 ativa, **no backend**, a primeira leva de falhas. Resumo de instrutor
(sem cadeia final longa):

- **IDOR / broken object-level authorization (receipts):**
  `GET /api/mobile/receipts/:receiptId` retorna qualquer recibo existente para
  qualquer token válido. `GET /api/mobile/receipts` (lista) e
  `GET /api/mobile/receipt/:id` (singular, compat. Fase 1) **mantêm** o escopo
  correto. Recibos `1002/1003/9001` pertencem a outros papéis; `9001` é o export
  legado com `metadata.internalNote` sensível.
- **IDOR (cards):** `GET /api/mobile/cards/:cardId` devolve qualquer cartão; o
  número é mascarado, mas `ownerRole` e `internalReference` vazam. A lista
  `GET /api/mobile/cards` continua escopada.
- **Mass assignment:** `PATCH /api/mobile/profile` aceita, além de
  `displayName/phone`, os campos privilegiados `role/plan/dailyLimit/
  kycApproved/supportTier`, mutando o usuário em memória.
- **Debug gate fraco:** `GET /api/mobile/support/diagnostics` exige apenas o
  header estático `X-Obsidian-Debug: mobile-diagnostics` (além de token).
- **Legacy route disclosure:** `GET /api/mobile/legacy/routes` enumera rotas
  internas/futuras.
- **Vault role gate:** `GET /api/mobile/internal/vault-status` nega `customer`
  (403) e responde diferente para `analyst`/`operator` (base para
  biometria/root/binary patching).
- **QR/deep link (scaffold):** `POST /api/mobile/transfer/preview` com validação
  fraca (amount string/numérico, memo sem sanitização forte), sem executar
  transferência.
- **WebView (scaffold):** `GET /api/mobile/webview/support` reflete `topic`/
  `message` em HTML (semente para WebView/bridge/XSS futuro).

Credenciais `analyst`/`operator` existem em `data.js` mas **não** são
documentadas para o aluno; servem para descoberta futura via mobile/RE/storage.

### Validação rápida (instrutor)

- `bash scripts/validate-phase2.sh` cobre todos os itens acima de ponta a ponta.
- `bash scripts/validate-phase1.sh` continua passando (compatibilidade).

## 3.2 Fase 3 — app Android base

A Fase 3 entrega o cliente Android (Kotlin + Compose) em `android-app/`,
consumindo a API local via `http://10.0.2.2:8102`. Pontos de instrutor:

- **App realista:** carteira ObsidianPay com telas de Login, Início, Recibos,
  Cartões, Suporte, Prévia de transferência e Configuração — **não** é um menu
  de vulnerabilidades.
- **Comunicação HTTP local:** `usesCleartextTraffic` + `network_security_config`
  liberam cleartext só para `10.0.2.2/127.0.0.1/localhost`. Semente para o
  estudo de interceptação/tráfego legado.
- **SharedPreferences inseguro:** `InsecureSessionStore` grava token, perfil
  (cache) e identificadores em texto puro. Semente para "insecure data storage".
- **Enumeração manual por ID:** as telas de Recibos e Cartões têm um campo
  "abrir por ID" que chama `/receipts/:id` e `/cards/:id`. Na UI isso é só uma
  busca; é a superfície que conecta o aluno ao IDOR já ativo no backend (Fase 2).
  A tela **não** chama isso de IDOR.
- **Support diagnostics:** a tela de Suporte tem botões para diagnostics com e
  sem o header de debug, expondo o gate fraco da Fase 2 pela UI.
- **Transfer preview:** tela liga ao endpoint de prévia (futuro QR/deep link).
- **Componentes exportados:** apenas `MainActivity` (launcher). Exported
  components vulneráveis ficam para fase futura (comentado no Manifest).

### Validação rápida (instrutor)

- `bash scripts/validate-phase3.sh` checa estrutura Android + (opcional) backend
  com `RUN_BACKEND_TESTS=1`, e tenta build se houver Gradle/SDK.
- Sem Android SDK, o build do APK não roda; o projeto compila a configuração
  Gradle e falha apenas na detecção do SDK (esperado).

## 3.3 Fase 4 — armazenamento local inseguro (app)

A Fase 4 enriquece o storage local do app, criando as superfícies de cache que
serão exploradas futuramente. Pontos de instrutor:

- **SharedPreferences em texto puro** (`InsecureSessionStore`): token, username,
  userId, role, plan, dailyLimit, kycApproved, `rawProfileJson`, `rawConfigJson`,
  últimos support sync/diagnostics/transfer preview, últimos IDs abertos,
  timestamp e baseUrlHint.
- **SQLite local** (`ObsidianLocalDb`, `obsidianpay_local.db`): tabelas
  `cached_receipts`, `cached_cards` (com `rawJson` em claro) e `debug_events`.
- **Arquivos internos**: `cacheDir/obsidian-support-last-sync.json`,
  `filesDir/receipts/receipt-<id>.json`, `filesDir/debug/obsidian-debug-export.json`.
- **External app-specific export** (scaffold): `getExternalFilesDir(null)/obsidian-export.txt`.
  É storage específico do app, **não** storage público global (isso fica para depois).
- **Log de eventos locais** (`debug_events`): login, abertura de recibo/cartão,
  support sync, diagnostics, transfer preview, clear.
- **`LocalCacheManager`** orquestra prefs + SQLite + arquivos. A tela interna
  **Local State** mostra o estado local (apresentada como ferramenta de
  suporte/dev, não como tela de "exploração").

### Caminhos Android prováveis (alto nível)

```
/data/data/com.obsidianpay.mobile/shared_prefs/    # SharedPreferences (token, cache)
/data/data/com.obsidianpay.mobile/databases/       # obsidianpay_local.db
/data/data/com.obsidianpay.mobile/files/           # receipts/, debug/
/data/data/com.obsidianpay.mobile/cache/           # snapshot de suporte
# Android/data/com.obsidianpay.mobile/files/        # export app-specific externo
```

> Estas são as superfícies de armazenamento local. A extração/encadeamento final
> (e flags) não entram aqui nesta fase.

## 4. Matriz de vulnerabilidades planejadas

Detalhe completo por trilha em [docs/VULNERABILITY-ROADMAP.md](./docs/VULNERABILITY-ROADMAP.md).
Resumo de status (atualizado na Fase 2):

| # | Trilha | Vulnerabilidade planejada | Status |
|---|---|---|---|
| 1 | Network/API | HTTP legacy sync | implemented-backend |
| 2 | Network/API | HTTPS interception | planned |
| 3 | Network/API | Certificate pinning bypass | planned |
| 4 | Network/API | Native pinning | planned |
| 5 | Storage/RE | SharedPreferences token leak | implemented-app |
| 6 | Storage/RE | SQLite sensitive data | implemented-app |
| 7 | Storage/RE | Temp/cache file leak | implemented-app |
| 8 | Storage/RE | Hardcoded/config secrets | planned |
| 9 | Storage/RE | Weak crypto | planned |
| 10 | Platform | Exported Activity | planned |
| 11 | Platform | Exported Service | planned |
| 12 | Platform | BroadcastReceiver debug trigger | planned |
| 13 | Platform | ContentProvider exposure | planned |
| 14 | Platform | Deep link abuse | planned |
| 15 | Platform | QR Code untrusted input | scaffolded |
| 16 | WebView | Unsafe WebView settings | scaffolded |
| 17 | WebView | JavaScript bridge exposure | planned |
| 18 | WebView | Deep link → WebView chain | planned |
| 19 | WebView | Local file / token disclosure | planned |
| 20 | Anti-analysis/Auth | Root detection bypass | planned |
| 21 | Anti-analysis/Auth | Emulator detection bypass | planned |
| 22 | Anti-analysis/Auth | Biometric vault backend gate | scaffolded |
| 23 | Anti-analysis/Auth | Binary patching | planned |
| 24 | Anti-analysis/Auth | API broken access control | implemented-backend |
| 25 | Anti-analysis/Auth | Mass assignment | implemented-backend |

---

## 5. Notas de manutenção

- Flags reais **não** entram em `README.md` nem `STUDENT-GUIDE.md`.
- Soluções e payloads só serão adicionados aqui (ou em SOLUTION.md) quando a
  vulnerabilidade correspondente for implementada.
- Manter tudo **local**: porta `127.0.0.1:8102`, sem dependências externas.
