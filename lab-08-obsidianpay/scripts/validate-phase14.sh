#!/usr/bin/env bash
#
# validate-phase14.sh — valida a Fase 14 do Lab 08 (Final Challenge Chain).
#
# Estrutural (sem dependências):
#   - Arquivos obrigatórios: api/src/flags.js, api/src/challenge-chain.js,
#     docs/CHALLENGE-SCORING.md, scripts/validate-phase14.sh.
#   - server.js registra os 4 endpoints da cadeia.
#   - flags.js contém >= 9 ocorrências únicas de FLAG{obsidianpay_.
#   - challenge-chain.js define os 9 stage ids e NÃO contém valores de flag.
#   - WALKTHROUGH.md contém FLAG{obsidianpay_.
#   - Guards de docs públicos (sem FLAG{) e sem credenciais internas.
#   - Scripts das fases 1..13 presentes e nenhum lab 1..7 alterado.
#
# Dinâmico (best-effort, só se Docker disponível):
#   - sobe o backend, faz login guest, exercita progress/submit/scoreboard.
#
# Não exige Android SDK. Sai com exit 1 se qualquer verificação obrigatória
# falhar.
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$LAB_DIR"

BASE_URL="http://127.0.0.1:8102"
API_SRC="api/src"
SERVER="$API_SRC/server.js"
FLAGS_FILE="$API_SRC/flags.js"
CHAIN_FILE="$API_SRC/challenge-chain.js"

pass() { printf '  \033[32m[PASS]\033[0m %s\n' "$1"; }
warn() { printf '  \033[33m[WARN]\033[0m %s\n' "$1"; }
info() { printf '\033[36m[*]\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m[FAIL]\033[0m %s\n' "$1" >&2; FAILED=1; }

FAILED=0

need_file() { if [ -f "$1" ]; then pass "arquivo: $1"; else fail "ausente: $1"; fi; }

need_grep_file() {
  if [ -f "$2" ] && grep -qF "$1" "$2"; then pass "$3"; else fail "$3 (esperado '$1' em $2)"; fi
}

reject_grep_tree() {
  if grep -rqF "$1" "$2" 2>/dev/null; then
    fail "$3 (string '$1' encontrada em $2)"
  else
    pass "$3"
  fi
}

info "Diretório do lab: $LAB_DIR"

# --- Scripts de fases anteriores ---------------------------------------------
info "Conferindo scripts de fases anteriores (1..13)..."
for n in 1 2 3 4 5 6 7 8 9 10 11 12 13; do need_file "scripts/validate-phase$n.sh"; done

# --- Arquivos obrigatórios da Fase 14 ----------------------------------------
info "Conferindo arquivos obrigatórios da Fase 14..."
need_file "$FLAGS_FILE"
need_file "$CHAIN_FILE"
need_file "docs/CHALLENGE-SCORING.md"
need_file "scripts/validate-phase14.sh"

# --- Endpoints no server.js --------------------------------------------------
info "Conferindo endpoints da cadeia em server.js..."
need_grep_file "/api/mobile/challenge/progress"        "$SERVER" "endpoint progress em server.js"
need_grep_file "/api/mobile/challenge/submit"          "$SERVER" "endpoint submit em server.js"
need_grep_file "/api/mobile/challenge/scoreboard"      "$SERVER" "endpoint scoreboard em server.js"
need_grep_file "/api/mobile/internal/finalize-operator" "$SERVER" "endpoint finalize-operator em server.js"

# server.js deve importar flags.js e challenge-chain.js
need_grep_file "require('./flags')"           "$SERVER" "server.js importa flags.js"
need_grep_file "require('./challenge-chain')" "$SERVER" "server.js importa challenge-chain.js"

# --- flags.js: >= 9 flags únicas ---------------------------------------------
info "Conferindo flags em flags.js..."
UNIQUE_FLAGS=$(grep -oE 'FLAG\{obsidianpay_[a-z0-9_]+\}' "$FLAGS_FILE" 2>/dev/null | sort -u | wc -l | tr -d ' ')
if [ "${UNIQUE_FLAGS:-0}" -ge 9 ]; then
  pass "flags.js contém $UNIQUE_FLAGS flags únicas FLAG{obsidianpay_ (>= 9)"
else
  fail "flags.js deveria ter >= 9 flags únicas FLAG{obsidianpay_, achou ${UNIQUE_FLAGS:-0}"
fi

# helpers exportados
need_grep_file "getFlagByStageId" "$FLAGS_FILE" "helper getFlagByStageId em flags.js"
need_grep_file "getAllFlags"      "$FLAGS_FILE" "helper getAllFlags em flags.js"
need_grep_file "isValidFlag"      "$FLAGS_FILE" "helper isValidFlag em flags.js"

# flags específicas esperadas
for fl in \
  "FLAG{obsidianpay_mobile_recon_01}" \
  "FLAG{obsidianpay_insecure_storage_02}" \
  "FLAG{obsidianpay_exported_components_03}" \
  "FLAG{obsidianpay_webview_bridge_04}" \
  "FLAG{obsidianpay_device_trust_05}" \
  "FLAG{obsidianpay_biometric_vault_06}" \
  "FLAG{obsidianpay_network_pinning_07}" \
  "FLAG{obsidianpay_integrity_bypass_08}" \
  "FLAG{obsidianpay_final_operator_chain_09}" ; do
  need_grep_file "$fl" "$FLAGS_FILE" "flag $fl em flags.js"
done

# --- challenge-chain.js: 9 stage ids, sem valores de flag --------------------
info "Conferindo challenge-chain.js..."
need_grep_file "obsidianpay-mobile-final-chain" "$CHAIN_FILE" "chainId em challenge-chain.js"
for st in \
  "stage-01-recon" \
  "stage-02-insecure-storage" \
  "stage-03-exported-components" \
  "stage-04-webview-bridge" \
  "stage-05-device-trust" \
  "stage-06-biometric-vault" \
  "stage-07-network-pinning" \
  "stage-08-app-integrity" \
  "stage-09-final-operator-chain" ; do
  need_grep_file "$st" "$CHAIN_FILE" "stage id $st em challenge-chain.js"
done
# campos obrigatórios por estágio
for fld in flagKey hintLevel1 hintLevel2 evidenceExpected publicSummary instructorNote points difficulty category title ; do
  need_grep_file "$fld" "$CHAIN_FILE" "campo $fld em challenge-chain.js"
done
# challenge-chain.js NÃO deve conter valores de flag
reject_grep_tree "FLAG{" "$CHAIN_FILE" "sem valores de FLAG{ em challenge-chain.js (apenas flagKey)"

# --- WALKTHROUGH deve conter flags -------------------------------------------
info "Conferindo WALKTHROUGH.md (material de instrutor, contém flags)..."
need_grep_file "FLAG{obsidianpay_" "WALKTHROUGH.md" "WALKTHROUGH.md contém FLAG{obsidianpay_"

# --- Guards de docs públicos (sem FLAG{) -------------------------------------
info "Verificando que docs públicos NÃO contêm FLAG{..."
PUBLIC_DOCS=(
  "README.md"
  "STUDENT-GUIDE.md"
  "docs/ARCHITECTURE.md"
  "docs/PHASES.md"
  "docs/VULNERABILITY-ROADMAP.md"
  "docs/CHALLENGE-SCORING.md"
  "docs/mobile-pentest/SETUP.md"
  "docs/mobile-pentest/PLAYBOOK.md"
  "android-app/README.md"
)
for doc in "${PUBLIC_DOCS[@]}"; do
  if [ -e "$doc" ]; then
    reject_grep_tree "FLAG{" "$doc" "sem FLAG{ em $doc"
  else
    warn "doc público ausente (pulando): $doc"
  fi
done
reject_grep_tree "FLAG{" "tools" "sem FLAG{ em tools/"

# --- Guards de credenciais internas em docs públicos / tools -----------------
info "Verificando ausência de credenciais internas (analyst123/operator123)..."
for doc in README.md STUDENT-GUIDE.md android-app/README.md; do
  reject_grep_tree "analyst123"  "$doc" "sem analyst123 em $doc"
  reject_grep_tree "operator123" "$doc" "sem operator123 em $doc"
done
reject_grep_tree "analyst123"  "tools" "sem analyst123 em tools/"
reject_grep_tree "operator123" "tools" "sem operator123 em tools/"

# --- Nenhum lab 1..7 alterado ------------------------------------------------
info "Verificando que labs 1..7 não foram alterados nesta branch..."
REPO_ROOT="$(git -C "$LAB_DIR" rev-parse --show-toplevel 2>/dev/null || echo "$LAB_DIR/..")"
BRANCH_CHANGED=$(git -C "$REPO_ROOT" log origin/main..HEAD --name-only --format='' 2>/dev/null || \
                 git -C "$REPO_ROOT" diff --name-only HEAD~15..HEAD 2>/dev/null || true)
for lab_num in 1 2 3 4 5 6 7; do
  if echo "$BRANCH_CHANGED" | grep -q "lab-0${lab_num}-"; then
    fail "lab-0${lab_num} foi alterado nos commits desta branch"
  else
    pass "lab-0${lab_num} não alterado nos commits desta branch"
  fi
done

# --- Teste dinâmico (best-effort, só com Docker) -----------------------------
DC=""
if docker compose version >/dev/null 2>&1; then
  DC="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  DC="docker-compose"
fi

if [ -n "$DC" ] && docker ps >/dev/null 2>&1; then
  info "Docker disponível — teste dinâmico da cadeia..."

  CLEANED=0
  cleanup() {
    if [ "$CLEANED" -eq 0 ]; then
      CLEANED=1
      info "Derrubando o ambiente..."
      $DC down >/dev/null 2>&1 || true
    fi
  }
  trap cleanup EXIT INT TERM

  jget() { python3 -c 'import sys,json
try:
    d=json.load(sys.stdin)
except Exception:
    print(""); sys.exit(0)
for p in sys.argv[1].split("."):
    d = d[p] if isinstance(d,dict) and p in d else None
print("" if d is None else d)' "$1"; }

  if $DC up --build -d >/dev/null 2>&1; then
    pass "backend iniciado via Docker"

    HEALTH=""
    for _ in $(seq 1 30); do
      if HEALTH="$(curl -fsS "$BASE_URL/health" 2>/dev/null)"; then break; fi
      sleep 2
    done
    if [ -n "$HEALTH" ]; then
      pass "/health respondeu"

      TOKEN="$(curl -fsS -X POST "$BASE_URL/api/mobile/login" -H 'Content-Type: application/json' \
        -d '{"username":"guest","password":"guest123"}' | jget token)"
      if [ -n "$TOKEN" ]; then
        pass "login guest OK"

        # progress (sem flags)
        PROG="$(curl -fsS "$BASE_URL/api/mobile/challenge/progress" -H "Authorization: Bearer $TOKEN")"
        [ "$(echo "$PROG" | jget chainId)" = "obsidianpay-mobile-final-chain" ] \
          && pass "progress retorna chainId" || fail "progress não retornou chainId"
        case "$PROG" in *"FLAG{"*) fail "progress vazou FLAG{";; *) pass "progress não vaza flags";; esac

        # submit errado
        WRONG="$(curl -fsS -X POST "$BASE_URL/api/mobile/challenge/submit" -H "Authorization: Bearer $TOKEN" \
          -H 'Content-Type: application/json' -d '{"stageId":"stage-01-recon","flag":"FLAG{nope}"}')"
        [ "$(echo "$WRONG" | jget accepted)" = "False" ] \
          && pass "submit flag errada => accepted false" || fail "submit flag errada deveria recusar"

        # obter flag stage1 via checkpoint de recon e submeter
        F1="$(curl -fsS "$BASE_URL/api/mobile/config" -H 'X-Obsidian-Recon: mobile-config-review' | jget reconCheckpoint.flag)"
        if [ -n "$F1" ]; then
          pass "checkpoint stage-01 (recon) retornou flag"
          OK="$(curl -fsS -X POST "$BASE_URL/api/mobile/challenge/submit" -H "Authorization: Bearer $TOKEN" \
            -H 'Content-Type: application/json' -d "{\"stageId\":\"stage-01-recon\",\"flag\":\"$F1\",\"evidence\":\"recon\"}")"
          [ "$(echo "$OK" | jget accepted)" = "True" ] \
            && pass "submit flag stage-01 correta => accepted true" || fail "submit flag stage-01 correta falhou"
          [ "$(echo "$OK" | jget pointsAwarded)" = "100" ] \
            && pass "stage-01 awarded 100 pontos" || fail "stage-01 pontuação inesperada"

          # idempotência
          DUP="$(curl -fsS -X POST "$BASE_URL/api/mobile/challenge/submit" -H "Authorization: Bearer $TOKEN" \
            -H 'Content-Type: application/json' -d "{\"stageId\":\"stage-01-recon\",\"flag\":\"$F1\"}")"
          [ "$(echo "$DUP" | jget pointsAwarded)" = "0" ] \
            && pass "resubmit idempotente (0 pontos)" || fail "resubmit duplicou pontos"
        else
          fail "checkpoint de recon não retornou flag stage-01"
        fi

        # scoreboard
        SB="$(curl -fsS "$BASE_URL/api/mobile/challenge/scoreboard" -H "Authorization: Bearer $TOKEN")"
        [ "$(echo "$SB" | jget totalScore)" = "100" ] \
          && pass "scoreboard totalScore=100" || fail "scoreboard totalScore inesperado"
        [ "$(echo "$SB" | jget totalStages)" = "9" ] \
          && pass "scoreboard totalStages=9" || fail "scoreboard totalStages inesperado"

        # finalize sem header => 403
        CODE="$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE_URL/api/mobile/internal/finalize-operator" \
          -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{}')"
        [ "$CODE" = "403" ] && pass "finalize-operator sem header => 403" || fail "finalize-operator sem header deveria ser 403 (foi $CODE)"
      else
        fail "login guest falhou"
      fi
    else
      fail "/health não respondeu a tempo"
    fi
  else
    warn "falha ao subir o backend via Docker — pulando teste dinâmico"
  fi
else
  warn "Docker indisponível — pulando teste dinâmico (validação estrutural concluída)"
fi

# --- Resultado ---------------------------------------------------------------
echo ""
if [ "$FAILED" -eq 0 ]; then
  printf '\033[32m[OK] Fase 14 validada com sucesso.\033[0m\n'
  exit 0
else
  printf '\033[31m[FAIL] Fase 14: uma ou mais verificações falharam.\033[0m\n' >&2
  exit 1
fi
