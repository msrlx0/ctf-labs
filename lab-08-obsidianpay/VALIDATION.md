# VALIDATION — Lab 08: ObsidianPay Mobile

Checklist técnico de validação. Todos os comandos assumem que você está na pasta
do lab:

```bash
cd lab-08-obsidianpay
```

> Atalhos:
> - `bash scripts/validate-phase1.sh` — valida a fundação/contrato da Fase 1.
> - `bash scripts/validate-phase2.sh` — valida os contratos e as vulnerabilidades
>   controladas da Fase 2.
>
> Ambos sobem o ambiente, testam de ponta a ponta, derrubam ao final e falham
> com `exit 1` se algo não responder como esperado.

A seção abaixo (1–12) cobre a **Fase 1**. A seção **Fase 2** vem em seguida.

## Fase 1

---

## 1. Validar a definição do compose

```bash
docker compose config
```

Deve renderizar o serviço `obsidianpay-api` mapeando `127.0.0.1:8102:8102`.

## 2. Subir o backend

```bash
docker compose up --build -d
```

Aguarde o healthcheck ficar `healthy`:

```bash
docker compose ps
```

## 3. Health check

```bash
curl -s http://127.0.0.1:8102/health
```

Esperado: JSON com `"status": "ok"` e `"expectedPort": 8102`.

## 4. Raiz / identificação

```bash
curl -s http://127.0.0.1:8102/
```

Esperado: JSON identificando `ObsidianPay Mobile API`.

## 5. Login `guest` / `guest123`

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"guest","password":"guest123"}'
```

Esperado: JSON com `token`, `profile` e `featureFlags`.

## 6. Extrair o token

Com `python3` (recomendado, sem depender de `jq`):

```bash
TOKEN=$(curl -s -X POST http://127.0.0.1:8102/api/mobile/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"guest","password":"guest123"}' \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')
echo "$TOKEN"
```

> Alternativa manual: copie o valor do campo `token` da resposta do passo 5 e
> exporte: `export TOKEN='op_...'`.

## 7. Profile com Authorization: Bearer

```bash
curl -s http://127.0.0.1:8102/api/mobile/profile \
  -H "Authorization: Bearer $TOKEN"
```

Esperado: JSON do perfil do `guest` (id 1001, walletId, etc.).

Sem token deve retornar `401`:

```bash
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8102/api/mobile/profile
```

## 8. Config mobile

```bash
curl -s http://127.0.0.1:8102/api/mobile/config
```

Esperado: `apiVersion`, `supportSyncMode`, `legacySupportEndpoint`,
`mobileFeatureFlags` e `warning`.

## 9. Recibo 1001

```bash
curl -s http://127.0.0.1:8102/api/mobile/receipt/1001 \
  -H "Authorization: Bearer $TOKEN"
```

Esperado: JSON do recibo `1001` (dono = guest).

## 10. Support sync (stub legado)

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/support/sync \
  -H 'Content-Type: application/json' \
  -d '{"message":"ping","ticketRef":"OP-SUP-1"}'
```

Esperado: JSON com `accepted: true` e `echo: "ping"`.

## 11. Derrubar o ambiente

```bash
docker compose down
```

---

## 12. Verificação de git

```bash
git status
git diff --stat
```

Confirme que **apenas** arquivos sob `lab-08-obsidianpay/` aparecem como
adicionados e que **nenhum** lab anterior (01–07) foi alterado.

---

## Critérios de aceite (Fase 1)

- [ ] `docker compose config` funciona.
- [ ] `docker compose up --build` sobe a API em `127.0.0.1:8102`.
- [ ] `/health` retorna `status: ok`.
- [ ] Login `guest`/`guest123` funciona.
- [ ] `/profile` com Bearer funciona (e `401` sem token).
- [ ] `/config`, `/receipt/1001` e `/support/sync` respondem.
- [ ] `scripts/validate-phase1.sh` passa.
- [ ] `README.md` e `STUDENT-GUIDE.md` sem flags/solução.
- [ ] Nenhum lab anterior alterado.

---

## Fase 2

Suba o ambiente (se ainda não estiver no ar) e faça login como guest:

```bash
docker compose up --build -d
TOKEN=$(curl -s -X POST http://127.0.0.1:8102/api/mobile/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"guest","password":"guest123"}' \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')
```

### F2.1 — Recibos do usuário

```bash
curl -s http://127.0.0.1:8102/api/mobile/receipts -H "Authorization: Bearer $TOKEN"
curl -s http://127.0.0.1:8102/api/mobile/receipts/1001 -H "Authorization: Bearer $TOKEN"
```

### F2.2 — Recibo por id (autorização a nível de objeto)

```bash
# observe o que volta para um id que não é o seu
curl -s http://127.0.0.1:8102/api/mobile/receipts/1002 -H "Authorization: Bearer $TOKEN"
```

### F2.3 — Cartões

```bash
curl -s http://127.0.0.1:8102/api/mobile/cards -H "Authorization: Bearer $TOKEN"
curl -s http://127.0.0.1:8102/api/mobile/cards/card-analyst-01 -H "Authorization: Bearer $TOKEN"
```

### F2.4 — Atualização de perfil

```bash
curl -s -X PATCH http://127.0.0.1:8102/api/mobile/profile \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"displayName":"Guest X","plan":"privileged","dailyLimit":99999}'
```

### F2.5 — Diagnostics (header de debug)

```bash
# sem header -> 403
curl -s -o /dev/null -w '%{http_code}\n' \
  http://127.0.0.1:8102/api/mobile/support/diagnostics -H "Authorization: Bearer $TOKEN"
# com header -> 200
curl -s http://127.0.0.1:8102/api/mobile/support/diagnostics \
  -H "Authorization: Bearer $TOKEN" -H 'X-Obsidian-Debug: mobile-diagnostics'
```

### F2.6 — Transfer preview / WebView / Legacy routes

```bash
curl -s -X POST http://127.0.0.1:8102/api/mobile/transfer/preview \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"toUserId":2001,"amount":"10","memo":"teste"}'

curl -s "http://127.0.0.1:8102/api/mobile/webview/support?topic=mobile"

curl -s http://127.0.0.1:8102/api/mobile/legacy/routes -H "Authorization: Bearer $TOKEN"
```

### F2.7 — Vault status (gate por papel)

```bash
# guest (customer) -> 403
curl -s -o /dev/null -w '%{http_code}\n' \
  http://127.0.0.1:8102/api/mobile/internal/vault-status -H "Authorization: Bearer $TOKEN"
```

Derrube ao final: `docker compose down`.

### Critérios de aceite (Fase 2)

- [ ] `scripts/validate-phase1.sh` continua passando.
- [ ] `scripts/validate-phase2.sh` passa.
- [ ] `/receipts` e `/cards` escopam pelo usuário; `/receipts/:id` e `/cards/:id`
      retornam objetos por id.
- [ ] `PATCH /profile` aplica campos enviados (incl. privilegiados).
- [ ] `/support/diagnostics` é `403` sem header e `200` com header correto.
- [ ] `/internal/vault-status` é `403` para customer e `200` para analyst.
- [ ] `README.md` e `STUDENT-GUIDE.md` **não** contêm `FLAG{`.
