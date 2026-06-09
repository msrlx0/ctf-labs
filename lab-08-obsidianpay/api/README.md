# ObsidianPay Mobile API

Backend mínimo do **Lab 08 — ObsidianPay Mobile** (Fase 1).

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

## Endpoints (Fase 1)

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| GET | `/` | não | Identifica a API ObsidianPay Mobile |
| GET | `/health` | não | Status, nome, versão e porta esperada |
| POST | `/api/mobile/login` | não | Login `guest` / `guest123`; retorna token didático |
| GET | `/api/mobile/profile` | Bearer | Perfil do usuário autenticado |
| GET | `/api/mobile/config` | não | Configuração mobile simulada |
| GET | `/api/mobile/receipt/:id` | Bearer | Recibo (Fase 1 expõe `1001` do guest) |
| POST | `/api/mobile/support/sync` | não | Stub legado de sincronização de suporte |

## Notas técnicas

- O token emitido na Fase 1 é **intencionalmente previsível/decodificável**.
  Não é segurança real: é uma costura didática para uma fase futura.
- O modelo de recibos já contém múltiplos registros para preparar estudos de
  autorização em fases futuras. A Fase 1 mantém o comportamento correto.
- Nenhum segredo real está presente neste backend.
