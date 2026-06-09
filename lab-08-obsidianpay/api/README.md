# ObsidianPay Mobile API

Backend do **Lab 08 — ObsidianPay Mobile** (Fase 2).

Node.js + Express, sem dependências externas de serviço. Estado em memória.
Escuta em `0.0.0.0:8102` dentro do container e é publicado em `127.0.0.1:8102`.

> Ambiente **somente local**. Não exponha este serviço na internet.

## Estrutura

```
api/
├── Dockerfile
├── .dockerignore
├── package.json
└── src/
    ├── server.js   # rotas, logging, auth didática, tratamento de erros
    └── data.js     # dados semente em memória (usuários, recibos, config)
```

## Rodar localmente (sem Docker)

```bash
cd api
npm install
PORT=8102 npm start
```

## Endpoints (Fase 2)

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| GET | `/` | não | Identifica a API ObsidianPay Mobile |
| GET | `/health` | não | Status, nome, versão e porta esperada |
| POST | `/api/mobile/login` | não | Login; retorna token didático Bearer |
| GET | `/api/mobile/profile` | Bearer | Perfil do usuário autenticado |
| PATCH | `/api/mobile/profile` | Bearer | Atualiza o perfil |
| GET | `/api/mobile/config` | não | Configuração mobile simulada |
| GET | `/api/mobile/receipts` | Bearer | Lista de recibos do usuário |
| GET | `/api/mobile/receipts/:receiptId` | Bearer | Recibo por id |
| GET | `/api/mobile/receipt/:id` | Bearer | Recibo (compat. Fase 1) |
| GET | `/api/mobile/cards` | Bearer | Cartões do usuário (número mascarado) |
| GET | `/api/mobile/cards/:cardId` | Bearer | Cartão por id (número mascarado) |
| POST | `/api/mobile/support/sync` | não | Stub legado de sincronização |
| GET | `/api/mobile/support/diagnostics` | Bearer + header | Diagnósticos mobile |
| POST | `/api/mobile/transfer/preview` | Bearer | Prévia de transferência (QR/deep link) |
| GET | `/api/mobile/webview/support` | não | Portal HTML para WebView |
| GET | `/api/mobile/legacy/routes` | Bearer | Rotas internas/legadas |
| GET | `/api/mobile/internal/vault-status` | Bearer | Status interno (gate por papel) |

## Exemplos (seguros)

```bash
# login + token (guest)
TOKEN=$(curl -s -X POST http://127.0.0.1:8102/api/mobile/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"guest","password":"guest123"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')

# perfil
curl -s http://127.0.0.1:8102/api/mobile/profile -H "Authorization: Bearer $TOKEN"

# recibos e cartões do próprio usuário
curl -s http://127.0.0.1:8102/api/mobile/receipts -H "Authorization: Bearer $TOKEN"
curl -s http://127.0.0.1:8102/api/mobile/cards    -H "Authorization: Bearer $TOKEN"
```

## Notas técnicas

- O token é **intencionalmente previsível** (`obsidian-mobile-token-<username>-<userId>`).
  Não é segurança real: é uma costura didática.
- A Fase 2 introduz, no backend, **vulnerabilidades controladas** (autorização a
  nível de objeto, mass assignment, gate de debug fraco, etc.). Os detalhes de
  exploração não ficam aqui — fazem parte do exercício.
- Marcadores de progresso existem apenas nos dados internos do lab
  (`src/data.js`), nunca em documentos públicos.
- Nenhum segredo de produção está presente neste backend.
