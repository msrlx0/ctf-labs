#!/usr/bin/env bash
#
# validate-phase15.sh — valida a Fase 15 do Lab 08 (documentação final:
# walkthrough manual de instrutor, guia do aluno sem spoilers, scoring público,
# playbook alinhado à cadeia, e guards anti-spoiler/anti-leak).
#
# A Fase 15 NÃO altera backend nem app — é o passe final de documentação. Este
# script:
#   - Confere que os scripts validate-phase1..14 existem.
#   - Confere o conteúdo dos docs (WALKTHROUGH, STUDENT-GUIDE, README,
#     CHALLENGE-SCORING, PLAYBOOK).
#   - Reforça os guards anti-leak: sem FLAG{ em docs públicos/tools; FLAG{
#     permitido apenas em WALKTHROUGH.md, api/src/flags.js, validate-phase14.sh
#     e validate-phase15.sh.
#   - Reforça a ausência de credenciais internas (analyst123/operator123).
#   - Re-verifica os typos conhecidos no código-fonte Android.
#   - Confere que nenhum lab 1..7 foi alterado nesta branch.
#   - Chama `bash scripts/validate-phase14.sh` (estrutural sempre; dinâmico se
#     houver Docker). Android SDK continua best-effort (não falha se ausente).
#
# Sai com exit 1 se qualquer verificação obrigatória falhar.
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$LAB_DIR"

APP="android-app"
SRC="$APP/app/src/main/java/com/obsidianpay/mobile"

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

reject_grep_re_tree() {
  if grep -rEq "$1" "$2" 2>/dev/null; then
    fail "$3 (padrão /$1/ encontrado em $2)"
  else
    pass "$3"
  fi
}

info "Diretório do lab: $LAB_DIR"

# --- Scripts de fases anteriores (1..14) -------------------------------------
info "Conferindo scripts de fases anteriores (1..14)..."
for n in 1 2 3 4 5 6 7 8 9 10 11 12 13 14; do need_file "scripts/validate-phase$n.sh"; done
need_file "scripts/validate-phase14.sh"

# --- WALKTHROUGH.md (instrutor, contém flags) --------------------------------
info "Conferindo WALKTHROUGH.md (walkthrough manual completo)..."
for s in \
  "Stage 01" "Stage 02" "Stage 03" "Stage 04" "Stage 05" \
  "Stage 06" "Stage 07" "Stage 08" "Stage 09" ; do
  need_grep_file "$s" "WALKTHROUGH.md" "WALKTHROUGH.md contém '$s'"
done
need_grep_file "Final Operator Chain"                       "WALKTHROUGH.md" "WALKTHROUGH.md contém 'Final Operator Chain'"
need_grep_file "/api/mobile/challenge/submit"               "WALKTHROUGH.md" "WALKTHROUGH.md contém endpoint de submit"
need_grep_file "FLAG{obsidianpay_final_operator_chain_09}"  "WALKTHROUGH.md" "WALKTHROUGH.md contém a flag final"

# --- STUDENT-GUIDE.md (aluno, sem flags) -------------------------------------
info "Conferindo STUDENT-GUIDE.md (guia do aluno)..."
need_grep_file "Objetivo final"                  "STUDENT-GUIDE.md" "STUDENT-GUIDE.md contém 'Objetivo final'"
need_grep_file "Como registrar progresso"        "STUDENT-GUIDE.md" "STUDENT-GUIDE.md contém 'Como registrar progresso'"
need_grep_file "/api/mobile/challenge/progress"  "STUDENT-GUIDE.md" "STUDENT-GUIDE.md referencia endpoint progress"
need_grep_file "/api/mobile/challenge/submit"    "STUDENT-GUIDE.md" "STUDENT-GUIDE.md referencia endpoint submit"
need_grep_file "<flag_redacted>"                 "STUDENT-GUIDE.md" "STUDENT-GUIDE.md usa <flag_redacted>"

# --- README.md (público) -----------------------------------------------------
info "Conferindo README.md (público)..."
need_grep_file "Lab 08"            "README.md" "README.md contém 'Lab 08'"
need_grep_file "ObsidianPay Mobile" "README.md" "README.md contém 'ObsidianPay Mobile'"
need_grep_file "8102"             "README.md" "README.md contém a porta 8102"
need_grep_file "guest / guest123" "README.md" "README.md contém credencial pública 'guest / guest123'"
need_grep_file "STUDENT-GUIDE.md" "README.md" "README.md aponta para STUDENT-GUIDE.md"

# --- docs/CHALLENGE-SCORING.md (público) -------------------------------------
info "Conferindo docs/CHALLENGE-SCORING.md..."
need_grep_file "completionPercent" "docs/CHALLENGE-SCORING.md" "CHALLENGE-SCORING contém completionPercent"
need_grep_file "finalUnlocked"     "docs/CHALLENGE-SCORING.md" "CHALLENGE-SCORING contém finalUnlocked"
need_grep_file "idempotente"       "docs/CHALLENGE-SCORING.md" "CHALLENGE-SCORING explica idempotência"
need_grep_file "<flag_redacted>"   "docs/CHALLENGE-SCORING.md" "CHALLENGE-SCORING usa <flag_redacted>"

# --- docs/mobile-pentest/PLAYBOOK.md (público) -------------------------------
info "Conferindo docs/mobile-pentest/PLAYBOOK.md..."
need_grep_file "stage-01"                "docs/mobile-pentest/PLAYBOOK.md" "PLAYBOOK referencia stage-01"
need_grep_file "stage-09"                "docs/mobile-pentest/PLAYBOOK.md" "PLAYBOOK referencia stage-09"
need_grep_file "dynamic instrumentation" "docs/mobile-pentest/PLAYBOOK.md" "PLAYBOOK menciona dynamic instrumentation"
need_grep_file "ContentProvider"         "docs/mobile-pentest/PLAYBOOK.md" "PLAYBOOK menciona ContentProvider"
need_grep_file "WebView bridge"          "docs/mobile-pentest/PLAYBOOK.md" "PLAYBOOK menciona WebView bridge"
need_grep_file "App Integrity"           "docs/mobile-pentest/PLAYBOOK.md" "PLAYBOOK menciona App Integrity"

# --- Guards anti-leak: docs públicos NÃO contêm FLAG{ ------------------------
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

# --- FLAG{ é permitido apenas nestes arquivos (sanity) -----------------------
info "Confirmando que FLAG{ aparece nos arquivos permitidos..."
need_grep_file "FLAG{" "WALKTHROUGH.md"               "FLAG{ permitido em WALKTHROUGH.md"
need_grep_file "FLAG{" "api/src/flags.js"             "FLAG{ permitido em api/src/flags.js"
need_grep_file "FLAG{" "scripts/validate-phase14.sh"  "FLAG{ permitido em scripts/validate-phase14.sh"
need_grep_file "FLAG{" "scripts/validate-phase15.sh"  "FLAG{ permitido em scripts/validate-phase15.sh"

# --- Guards de credenciais internas ------------------------------------------
info "Verificando ausência de credenciais internas (analyst123/operator123)..."
for doc in README.md STUDENT-GUIDE.md android-app/README.md; do
  if [ -e "$doc" ]; then
    reject_grep_tree "analyst123"  "$doc" "sem analyst123 em $doc"
    reject_grep_tree "operator123" "$doc" "sem operator123 em $doc"
  fi
done
reject_grep_tree "analyst123"  "tools" "sem analyst123 em tools/"
reject_grep_tree "operator123" "tools" "sem operator123 em tools/"

# --- Guards de typos conhecidos (código-fonte Android) -----------------------
info "Verificando ausência de typos conhecidos em $SRC..."
if [ -d "$SRC" ]; then
  # Literais seguros (não são substring das formas corretas):
  for t in \
    "network-config-cleartext-overrie" \
    "PinningPolicyy" \
    "TamperCheckk" \
    "getSessionSummay" \
    "webVieClient" \
    "WeakCryptosha1Hex" \
    "WeakCryptomd5Hex" \
    "apiClientsetBaseUrlForSession" \
    "getLastNativeGatStatus" ; do
    reject_grep_tree "$t" "$SRC" "sem typo '$t'"
  done
  # Formas que SÃO substring das corretas — usar âncora por regex:
  reject_grep_re_tree 'getSessionSummar\(\)'   "$SRC" "sem typo 'getSessionSummar()'"
  reject_grep_re_tree '@JavascriptInterfac$'   "$SRC" "sem typo '@JavascriptInterfac'"
  reject_grep_re_tree 'LegacyRequestSigne\b'   "$SRC" "sem typo 'LegacyRequestSigne'"
  reject_grep_re_tree 'App Integrit([^y]|$)'   "$SRC" "sem typo 'App Integrit' (sem y)"
else
  warn "código-fonte Android ausente ($SRC) — pulando guards de typos (best-effort)"
fi

# --- Nenhum lab 1..7 alterado ------------------------------------------------
info "Verificando que labs 1..7 não foram alterados nesta branch..."
REPO_ROOT="$(git -C "$LAB_DIR" rev-parse --show-toplevel 2>/dev/null || echo "$LAB_DIR/..")"
BRANCH_CHANGED=$(git -C "$REPO_ROOT" log origin/main..HEAD --name-only --format='' 2>/dev/null || \
                 git -C "$REPO_ROOT" diff --name-only HEAD~16..HEAD 2>/dev/null || true)
for lab_num in 1 2 3 4 5 6 7; do
  if echo "$BRANCH_CHANGED" | grep -q "lab-0${lab_num}-"; then
    fail "lab-0${lab_num} foi alterado nos commits desta branch"
  else
    pass "lab-0${lab_num} não alterado nos commits desta branch"
  fi
done

# --- Chamar a validação da Fase 14 (estrutural + dinâmico best-effort) -------
info "Executando scripts/validate-phase14.sh (Fase 14, dinâmico se houver Docker)..."
if [ -f "scripts/validate-phase14.sh" ]; then
  if bash scripts/validate-phase14.sh; then
    pass "validate-phase14.sh passou"
  else
    fail "validate-phase14.sh falhou"
  fi
else
  fail "scripts/validate-phase14.sh ausente"
fi

# --- Resultado ---------------------------------------------------------------
echo ""
if [ "$FAILED" -eq 0 ]; then
  printf '\033[32m[OK] Fase 15 validada com sucesso.\033[0m\n'
  exit 0
else
  printf '\033[31m[FAIL] Fase 15: uma ou mais verificações falharam.\033[0m\n' >&2
  exit 1
fi
