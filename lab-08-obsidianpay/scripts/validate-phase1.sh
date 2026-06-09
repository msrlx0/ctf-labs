#!/usr/bin/env bash
#
# validate-phase1.sh — valida a Fase 1 do Lab 08 (ObsidianPay Mobile).
#
# Sobe o backend via docker compose, testa os endpoints principais e derruba o
# ambiente ao final. Falha com exit 1 se qualquer verificação não passar.
#
# Uso:
#   bash scripts/validate-phase1.sh
#
set -euo pipefail

BASE_URL="http://127.0.0.1:8102"
# Resolve o diretório do lab (pai da pasta scripts/) e entra nele.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$LAB_DIR"

# --- helpers ----------------------------------------------------------------
pass() { printf '  \033[32m[PASS]\033[0m %s\n' "$1"; }
info() { printf '\033[36m[*]\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m[FAIL]\033[0m %s\n' "$1" >&2; exit 1; }

# Detecta o comando docker compose (plugin v2 ou binário legado).
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
# trap seguro: sempre tenta limpar ao sair (sucesso ou erro).
trap cleanup EXIT INT TERM

# Extrai um campo string de um JSON via python3 (sem depender de jq).
json_field() {
  # $1 = json, $2 = chave de topo
  python3 -c 'import sys,json
data=json.loads(sys.argv[1])
v=data.get(sys.argv[2])
print("" if v is None else v)' "$1" "$2"
}

info "Diretório do lab: $LAB_DIR"

# --- 1. compose config ------------------------------------------------------
info "Validando docker compose config..."
$DC config >/dev/null || fail "docker compose config inválido."
pass "docker compose config OK"

# --- 2. subir o backend -----------------------------------------------------
info "Subindo o backend (build + detached)..."
$DC up --build -d >/dev/null || fail "Falha ao subir o backend."
pass "Backend iniciado"

# --- 3. aguardar /health ----------------------------------------------------
info "Aguardando /health responder..."
HEALTH=""
for i in $(seq 1 30); do
  if HEALTH="$(curl -fsS "$BASE_URL/health" 2>/dev/null)"; then
    break
  fi
  sleep 2
done
[ -n "$HEALTH" ] || fail "/health não respondeu a tempo."
STATUS="$(json_field "$HEALTH" status)"
[ "$STATUS" = "ok" ] || fail "/health status inesperado: '$STATUS'"
pass "/health status=ok"

# --- 4. raiz ----------------------------------------------------------------
info "Testando / (identificação)..."
ROOT="$(curl -fsS "$BASE_URL/" 2>/dev/null)" || fail "/ não respondeu."
case "$ROOT" in
  *"ObsidianPay Mobile API"*) pass "/ identifica ObsidianPay Mobile API" ;;
  *) fail "/ não identificou o serviço esperado." ;;
esac

# --- 5. login guest/guest123 ------------------------------------------------
info "Login guest/guest123..."
LOGIN="$(curl -fsS -X POST "$BASE_URL/api/mobile/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"guest","password":"guest123"}' 2>/dev/null)" \
  || fail "Login não respondeu."
TOKEN="$(json_field "$LOGIN" token)"
[ -n "$TOKEN" ] || fail "Login não retornou token."
pass "Login OK (token recebido)"

# --- 6. profile com Bearer --------------------------------------------------
info "Testando /api/mobile/profile com Bearer..."
PROFILE="$(curl -fsS "$BASE_URL/api/mobile/profile" \
  -H "Authorization: Bearer $TOKEN" 2>/dev/null)" \
  || fail "/profile não respondeu com token válido."
PUSER="$(json_field "$PROFILE" username)"
[ "$PUSER" = "guest" ] || fail "/profile username inesperado: '$PUSER'"
pass "/profile retornou o usuário guest"

# 6b. profile sem token deve dar 401
CODE="$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/api/mobile/profile")"
[ "$CODE" = "401" ] || fail "/profile sem token deveria ser 401, foi $CODE"
pass "/profile sem token retorna 401"

# --- 7. config --------------------------------------------------------------
info "Testando /api/mobile/config..."
CONFIG="$(curl -fsS "$BASE_URL/api/mobile/config" 2>/dev/null)" || fail "/config não respondeu."
APIV="$(json_field "$CONFIG" apiVersion)"
[ -n "$APIV" ] || fail "/config sem apiVersion."
pass "/config OK (apiVersion=$APIV)"

# --- 8. receipt/1001 --------------------------------------------------------
info "Testando /api/mobile/receipt/1001..."
RECEIPT="$(curl -fsS "$BASE_URL/api/mobile/receipt/1001" \
  -H "Authorization: Bearer $TOKEN" 2>/dev/null)" \
  || fail "/receipt/1001 não respondeu."
RREF="$(json_field "$RECEIPT" reference)"
[ -n "$RREF" ] || fail "/receipt/1001 sem reference."
pass "/receipt/1001 OK (reference=$RREF)"

# --- 9. support/sync --------------------------------------------------------
info "Testando /api/mobile/support/sync..."
SYNC="$(curl -fsS -X POST "$BASE_URL/api/mobile/support/sync" \
  -H 'Content-Type: application/json' \
  -d '{"message":"ping","ticketRef":"OP-SUP-1"}' 2>/dev/null)" \
  || fail "/support/sync não respondeu."
ECHO="$(json_field "$SYNC" echo)"
[ "$ECHO" = "ping" ] || fail "/support/sync echo inesperado: '$ECHO'"
pass "/support/sync OK (echo=ping)"

printf '\n\033[32m==> Fase 1 validada com sucesso.\033[0m\n'
# cleanup roda via trap EXIT.
