# VALIDATION — Lab 08: ObsidianPay Mobile (Fase 1)

Checklist técnico para validar a fundação e o backend da Fase 1.
Todos os comandos assumem que você está na pasta do lab:

```bash
cd lab-08-obsidianpay
```

> Atalho: `bash scripts/validate-phase1.sh` executa o fluxo abaixo de ponta a
> ponta e falha com `exit 1` se algum endpoint não responder como esperado.

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
