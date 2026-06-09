#!/usr/bin/env bash
#
# validate-phase2.sh — valida a Fase 2 do Lab 08 (ObsidianPay Mobile).
#
# Sobe o backend, exercita os novos contratos mobile e as vulnerabilidades
# controladas (IDOR em receipts/cards, mass assignment, debug gate fraco,
# legacy routes, vault role gate, transfer preview, webview). Derruba o
# ambiente ao final. Falha com exit 1 em qualquer verificação inesperada.
#
# Uso:
#   bash scripts/validate-phase2.sh
#
set -euo pipefail

BASE_URL="http://127.0.0.1:8102"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$LAB_DIR"

pass() { printf '  \033[32m[PASS]\033[0m %s\n' "$1"; }
info() { printf '\033[36m[*]\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m[FAIL]\033[0m %s\n' "$1" >&2; exit 1; }

if docker compose version >/dev/null 2>&1; then
  DC="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  DC="docker-compose"
else
  fail "docker compose não encontrado."
fi

CLEANED=0
cleanup() {
  if [ "$CLEANED" -eq 0 ]; then
    CLEANED=1
    info "Derrubando o ambiente..."
    $DC down >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT INT TERM

# Extrai um campo (possivelmente aninhado por pontos) de um JSON via python3.
json_get() {
  # $1 = json, $2 = caminho tipo "normalizedPreview.amount"
  python3 -c 'import sys,json
data=json.loads(sys.argv[1])
cur=data
for part in sys.argv[2].split("."):
    if isinstance(cur,dict) and part in cur:
        cur=cur[part]
    else:
        cur=None
        break
print("" if cur is None else cur)' "$1" "$2"
}

login_token() {
  # $1 = username, $2 = password ; imprime o token
  local body
  body="$(curl -fsS -X POST "$BASE_URL/api/mobile/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" 2>/dev/null)" || return 1
  json_get "$body" token
}

http_code() {
  # $@ = args extras para o curl ; imprime o status HTTP
  curl -s -o /dev/null -w '%{http_code}' "$@"
}

info "Diretório do lab: $LAB_DIR"

# --- compose config + subir -------------------------------------------------
info "Validando docker compose config..."
$DC config >/dev/null || fail "docker compose config inválido."
pass "docker compose config OK"

info "Subindo o backend (build + detached)..."
$DC up --build -d >/dev/null || fail "Falha ao subir o backend."
pass "Backend iniciado"

info "Aguardando /health responder..."
HEALTH=""
for _ in $(seq 1 30); do
  if HEALTH="$(curl -fsS "$BASE_URL/health" 2>/dev/null)"; then break; fi
  sleep 2
done
[ -n "$HEALTH" ] || fail "/health não respondeu a tempo."
[ "$(json_get "$HEALTH" status)" = "ok" ] || fail "/health status inesperado."
pass "/health status=ok"

# --- login guest ------------------------------------------------------------
info "Login como guest..."
GUEST_TOKEN="$(login_token guest guest123)" || fail "Login guest falhou."
[ -n "$GUEST_TOKEN" ] || fail "Token guest vazio."
pass "Login guest OK"

# --- /receipts (lista do guest) ---------------------------------------------
info "Testando /api/mobile/receipts (guest)..."
RECEIPTS="$(curl -fsS "$BASE_URL/api/mobile/receipts" -H "Authorization: Bearer $GUEST_TOKEN")" \
  || fail "/receipts não respondeu."
[ "$(json_get "$RECEIPTS" count)" = "1" ] || fail "/receipts do guest deveria listar 1 recibo."
pass "/receipts lista apenas recibos do guest"

# --- /receipts/1001 (próprio) -----------------------------------------------
info "Testando /api/mobile/receipts/1001 (próprio)..."
R1001="$(curl -fsS "$BASE_URL/api/mobile/receipts/1001" -H "Authorization: Bearer $GUEST_TOKEN")" \
  || fail "/receipts/1001 não respondeu."
[ "$(json_get "$R1001" reference)" = "OP-RCPT-1001" ] || fail "/receipts/1001 reference inesperada."
pass "/receipts/1001 OK"

# --- IDOR: /receipts/1002 (do analyst) via token guest ----------------------
info "Testando IDOR controlado: /receipts/1002 com token guest..."
R1002="$(curl -fsS "$BASE_URL/api/mobile/receipts/1002" -H "Authorization: Bearer $GUEST_TOKEN")" \
  || fail "/receipts/1002 não respondeu."
[ "$(json_get "$R1002" ownerRole)" = "analyst" ] \
  || fail "IDOR não confirmado: /receipts/1002 não retornou recibo de analyst."
pass "IDOR confirmado: guest acessou recibo do analyst (1002)"

# --- /cards (lista do guest) ------------------------------------------------
info "Testando /api/mobile/cards (guest)..."
CARDS="$(curl -fsS "$BASE_URL/api/mobile/cards" -H "Authorization: Bearer $GUEST_TOKEN")" \
  || fail "/cards não respondeu."
[ "$(json_get "$CARDS" count)" = "1" ] || fail "/cards do guest deveria listar 1 cartão."
pass "/cards lista apenas cartões do guest"

# --- IDOR: /cards/card-analyst-01 via token guest ---------------------------
info "Testando IDOR controlado: /cards/card-analyst-01 com token guest..."
CARD_A="$(curl -fsS "$BASE_URL/api/mobile/cards/card-analyst-01" -H "Authorization: Bearer $GUEST_TOKEN")" \
  || fail "/cards/card-analyst-01 não respondeu."
[ "$(json_get "$CARD_A" ownerRole)" = "analyst" ] \
  || fail "IDOR de cartão não confirmado (ownerRole)."
MASKED="$(json_get "$CARD_A" maskedNumber)"
case "$MASKED" in *"2001") pass "IDOR de cartão confirmado (vaza ownerRole + ref; número mascarado: $MASKED)";; *) fail "Número do cartão não foi mascarado como esperado: $MASKED";; esac

# --- Mass assignment: PATCH /profile ----------------------------------------
info "Testando mass assignment: PATCH /api/mobile/profile (plan + dailyLimit)..."
PATCHED="$(curl -fsS -X PATCH "$BASE_URL/api/mobile/profile" \
  -H "Authorization: Bearer $GUEST_TOKEN" -H 'Content-Type: application/json' \
  -d '{"displayName":"Guest X","plan":"privileged","dailyLimit":99999,"kycApproved":true}')" \
  || fail "PATCH /profile não respondeu."
[ "$(json_get "$PATCHED" profile.plan)" = "privileged" ] || fail "Mass assignment de plan falhou."
[ "$(json_get "$PATCHED" profile.dailyLimit)" = "99999" ] || fail "Mass assignment de dailyLimit falhou."
pass "Mass assignment confirmado (plan=privileged, dailyLimit=99999)"

# --- Debug gate fraco -------------------------------------------------------
info "Testando /support/diagnostics sem header (espera 403)..."
CODE="$(http_code "$BASE_URL/api/mobile/support/diagnostics" -H "Authorization: Bearer $GUEST_TOKEN")"
[ "$CODE" = "403" ] || fail "/support/diagnostics sem header deveria ser 403, foi $CODE."
pass "/support/diagnostics sem header retorna 403"

info "Testando /support/diagnostics com header correto (espera 200)..."
DIAG="$(curl -fsS "$BASE_URL/api/mobile/support/diagnostics" \
  -H "Authorization: Bearer $GUEST_TOKEN" -H 'X-Obsidian-Debug: mobile-diagnostics')" \
  || fail "/support/diagnostics com header não respondeu 2xx."
[ "$(json_get "$DIAG" buildChannel)" = "internal-dev" ] || fail "/diagnostics conteúdo inesperado."
pass "/support/diagnostics com header retorna 200 + diagnósticos"

# --- transfer/preview -------------------------------------------------------
info "Testando /api/mobile/transfer/preview..."
PREVIEW="$(curl -fsS -X POST "$BASE_URL/api/mobile/transfer/preview" \
  -H "Authorization: Bearer $GUEST_TOKEN" -H 'Content-Type: application/json' \
  -d '{"toUserId":2001,"amount":"10","memo":"teste"}')" \
  || fail "/transfer/preview não respondeu."
[ "$(json_get "$PREVIEW" normalizedPreview.amount)" = "10" ] || fail "transfer/preview amount não normalizado."
pass "/transfer/preview OK (amount string normalizado)"

# --- webview/support --------------------------------------------------------
info "Testando /api/mobile/webview/support?topic=mobile..."
WEBVIEW="$(curl -fsS "$BASE_URL/api/mobile/webview/support?topic=mobile")" \
  || fail "/webview/support não respondeu."
case "$WEBVIEW" in *"mobile"*) pass "/webview/support reflete topic";; *) fail "/webview/support não refletiu topic.";; esac

# --- legacy/routes ----------------------------------------------------------
info "Testando /api/mobile/legacy/routes..."
LEGACY="$(curl -fsS "$BASE_URL/api/mobile/legacy/routes" -H "Authorization: Bearer $GUEST_TOKEN")" \
  || fail "/legacy/routes não respondeu."
case "$LEGACY" in *"/api/mobile/internal/vault-status"*) pass "/legacy/routes lista rotas internas";; *) fail "/legacy/routes não listou as rotas esperadas.";; esac

# --- vault-status: customer 403 ---------------------------------------------
info "Testando /internal/vault-status como guest (espera 403)..."
CODE="$(http_code "$BASE_URL/api/mobile/internal/vault-status" -H "Authorization: Bearer $GUEST_TOKEN")"
[ "$CODE" = "403" ] || fail "vault-status como customer deveria ser 403, foi $CODE."
pass "/internal/vault-status nega customer (403)"

# --- vault-status: analyst 200 ----------------------------------------------
info "Login como analyst e teste /internal/vault-status (espera 200)..."
ANALYST_TOKEN="$(login_token analyst analyst123)" || fail "Login analyst falhou."
VAULT="$(curl -fsS "$BASE_URL/api/mobile/internal/vault-status" -H "Authorization: Bearer $ANALYST_TOKEN")" \
  || fail "vault-status como analyst não respondeu 2xx."
[ "$(json_get "$VAULT" role)" = "analyst" ] || fail "vault-status do analyst inesperado."
pass "/internal/vault-status permite analyst (200)"

printf '\n\033[32m==> Fase 2 validada com sucesso.\033[0m\n'
# cleanup roda via trap EXIT.
