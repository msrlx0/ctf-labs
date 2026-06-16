#!/usr/bin/env bash
#
# validate-phase16.sh — valida a Fase 16 do Lab 08 (QA final / release readiness).
#
# A Fase 16 NÃO altera backend nem app: é o passe de QA final antes do build real
# do APK Android. Este script:
#   - Confere que os scripts validate-phase1..15 existem.
#   - Confere os arquivos obrigatórios da Fase 16 (FINAL-QA, ANDROID-BUILD-CHECKLIST)
#     e o conteúdo dos docs finais (FINAL-QA, ANDROID-BUILD-CHECKLIST, README,
#     STUDENT-GUIDE, WALKTHROUGH).
#   - Reforça os guards anti-leak: sem FLAG{ em docs públicos/tools; FLAG{
#     permitido apenas em WALKTHROUGH.md, api/src/flags.js e nos
#     validate-phase14/15/16.sh.
#   - Reforça a ausência de credenciais internas (analyst123/operator123), agora
#     também nos docs finais.
#   - Re-verifica os typos conhecidos no código-fonte Android.
#   - Falha em marcadores de rascunho perigosos nos docs finais (TODO/FIXME/TBD/
#     changeme/placeholder/lorem ipsum), com matching seguro (sem casar "todos").
#   - Confere que nenhum lab 1..7 foi alterado nos commits desta branch.
#   - Roda `bash scripts/validate-phase14.sh` e `bash scripts/validate-phase15.sh`
#     (estrutural sempre; dinâmico de Docker é herdado da Fase 14). Android SDK
#     continua best-effort (não falha se ausente); se houver SDK, tenta
#     `./gradlew tasks` sem falhar por SDK.
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

# Rejeita um padrão case-insensitive (literal) num único arquivo.
reject_grep_i_file() {
  if [ -e "$2" ] && grep -iqF "$1" "$2" 2>/dev/null; then
    fail "$3 (string '$1' encontrada em $2)"
  else
    pass "$3"
  fi
}

info "Diretório do lab: $LAB_DIR"

# --- A. Scripts de fases anteriores (1..15) ----------------------------------
info "A. Conferindo scripts de fases anteriores (1..15)..."
for n in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do need_file "scripts/validate-phase$n.sh"; done

# --- B. Arquivos obrigatórios ------------------------------------------------
info "B. Conferindo arquivos obrigatórios..."
need_file "docs/FINAL-QA.md"
need_file "docs/ANDROID-BUILD-CHECKLIST.md"
need_file "docs/CHALLENGE-SCORING.md"
need_file "STUDENT-GUIDE.md"
need_file "WALKTHROUGH.md"
need_file "README.md"

# --- C. Conteúdo obrigatório de FINAL-QA -------------------------------------
info "C. Conferindo docs/FINAL-QA.md..."
need_grep_file "Final QA"             "docs/FINAL-QA.md" "FINAL-QA contém 'Final QA'"
need_grep_file "127.0.0.1:8102"       "docs/FINAL-QA.md" "FINAL-QA contém backend URL 127.0.0.1:8102"
need_grep_file "10.0.2.2:8102"        "docs/FINAL-QA.md" "FINAL-QA contém emulador URL 10.0.2.2:8102"
need_grep_file "Anti-leak"            "docs/FINAL-QA.md" "FINAL-QA contém 'Anti-leak'"
need_grep_file "validate-phase16.sh"  "docs/FINAL-QA.md" "FINAL-QA referencia validate-phase16.sh"
need_grep_file "<flag_redacted>"      "docs/FINAL-QA.md" "FINAL-QA usa <flag_redacted>"

# --- D. Conteúdo obrigatório de ANDROID-BUILD-CHECKLIST ----------------------
info "D. Conferindo docs/ANDROID-BUILD-CHECKLIST.md..."
need_grep_file "Android Studio"       "docs/ANDROID-BUILD-CHECKLIST.md" "BUILD-CHECKLIST contém 'Android Studio'"
need_grep_file "Gradle"               "docs/ANDROID-BUILD-CHECKLIST.md" "BUILD-CHECKLIST contém 'Gradle'"
need_grep_file "debug APK"            "docs/ANDROID-BUILD-CHECKLIST.md" "BUILD-CHECKLIST contém 'debug APK'"
need_grep_file "10.0.2.2:8102"        "docs/ANDROID-BUILD-CHECKLIST.md" "BUILD-CHECKLIST contém 10.0.2.2:8102"
need_grep_file "API Host"             "docs/ANDROID-BUILD-CHECKLIST.md" "BUILD-CHECKLIST contém 'API Host'"
need_grep_file "guest / guest123"     "docs/ANDROID-BUILD-CHECKLIST.md" "BUILD-CHECKLIST contém 'guest / guest123'"
need_grep_file "emulador"             "docs/ANDROID-BUILD-CHECKLIST.md" "BUILD-CHECKLIST contém 'emulador'"
need_grep_file "celular físico"       "docs/ANDROID-BUILD-CHECKLIST.md" "BUILD-CHECKLIST contém 'celular físico'"

# --- E. Conteúdo obrigatório de README ---------------------------------------
info "E. Conferindo README.md..."
need_grep_file "docs/ANDROID-BUILD-CHECKLIST.md" "README.md" "README aponta para docs/ANDROID-BUILD-CHECKLIST.md"
need_grep_file "docs/FINAL-QA.md"                "README.md" "README aponta para docs/FINAL-QA.md"
need_grep_file "STUDENT-GUIDE.md"                "README.md" "README aponta para STUDENT-GUIDE.md"
need_grep_file "8102"                            "README.md" "README contém a porta 8102"

# --- F. Conteúdo obrigatório de STUDENT-GUIDE --------------------------------
info "F. Conferindo STUDENT-GUIDE.md..."
need_grep_file "Checklist do aluno"             "STUDENT-GUIDE.md" "STUDENT-GUIDE contém 'Checklist do aluno'"
need_grep_file "Como registrar progresso"       "STUDENT-GUIDE.md" "STUDENT-GUIDE contém 'Como registrar progresso'"
need_grep_file "evidência"                      "STUDENT-GUIDE.md" "STUDENT-GUIDE contém 'evidência'"
need_grep_file "/api/mobile/challenge/progress" "STUDENT-GUIDE.md" "STUDENT-GUIDE referencia endpoint progress"
need_grep_file "/api/mobile/challenge/submit"   "STUDENT-GUIDE.md" "STUDENT-GUIDE referencia endpoint submit"
need_grep_file "<flag_redacted>"                "STUDENT-GUIDE.md" "STUDENT-GUIDE usa <flag_redacted>"

# --- G. Conteúdo obrigatório de WALKTHROUGH (instrutor) ----------------------
info "G. Conferindo WALKTHROUGH.md (instrutor)..."
need_grep_file "Checklist de encerramento"                 "WALKTHROUGH.md" "WALKTHROUGH contém 'Checklist de encerramento'"
need_grep_file "Final Operator Chain"                      "WALKTHROUGH.md" "WALKTHROUGH contém 'Final Operator Chain'"
need_grep_file "FLAG{obsidianpay_final_operator_chain_09}" "WALKTHROUGH.md" "WALKTHROUGH contém a flag final"

# --- H. Anti-leak: docs públicos / tools NÃO contêm FLAG{ --------------------
info "H. Verificando que docs públicos/tools NÃO contêm FLAG{..."
PUBLIC_DOCS=(
  "README.md"
  "STUDENT-GUIDE.md"
  "docs/ARCHITECTURE.md"
  "docs/PHASES.md"
  "docs/VULNERABILITY-ROADMAP.md"
  "docs/CHALLENGE-SCORING.md"
  "docs/FINAL-QA.md"
  "docs/ANDROID-BUILD-CHECKLIST.md"
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

# FLAG{ é permitido apenas nestes arquivos (sanity) --------------------------
info "H. Confirmando que FLAG{ aparece nos arquivos permitidos..."
need_grep_file "FLAG{" "WALKTHROUGH.md"              "FLAG{ permitido em WALKTHROUGH.md"
need_grep_file "FLAG{" "api/src/flags.js"            "FLAG{ permitido em api/src/flags.js"
need_grep_file "FLAG{" "scripts/validate-phase14.sh" "FLAG{ permitido em scripts/validate-phase14.sh"
need_grep_file "FLAG{" "scripts/validate-phase15.sh" "FLAG{ permitido em scripts/validate-phase15.sh"
need_grep_file "FLAG{" "scripts/validate-phase16.sh" "FLAG{ permitido em scripts/validate-phase16.sh"

# --- I. Credenciais internas (analyst123/operator123) ------------------------
info "I. Verificando ausência de credenciais internas..."
for doc in \
  README.md \
  STUDENT-GUIDE.md \
  android-app/README.md \
  docs/FINAL-QA.md \
  docs/ANDROID-BUILD-CHECKLIST.md ; do
  if [ -e "$doc" ]; then
    reject_grep_tree "analyst123"  "$doc" "sem analyst123 em $doc"
    reject_grep_tree "operator123" "$doc" "sem operator123 em $doc"
  fi
done
reject_grep_tree "analyst123"  "tools" "sem analyst123 em tools/"
reject_grep_tree "operator123" "tools" "sem operator123 em tools/"

# --- J. Typos conhecidos (código-fonte Android) ------------------------------
info "J. Verificando ausência de typos conhecidos em $SRC..."
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
  reject_grep_re_tree 'getSessionSummar\('     "$SRC" "sem typo 'getSessionSummar('"
  reject_grep_re_tree '@JavascriptInterfac$'   "$SRC" "sem typo '@JavascriptInterfac'"
  reject_grep_re_tree 'LegacyRequestSigne\b'   "$SRC" "sem typo 'LegacyRequestSigne'"
  reject_grep_re_tree 'App Integrit([^y]|$)'   "$SRC" "sem typo 'App Integrit' (sem y)"
else
  warn "código-fonte Android ausente ($SRC) — pulando guards de typos (best-effort)"
fi

# --- K. Marcadores de rascunho perigosos nos docs finais ---------------------
# "docs finais" = os novos docs da Fase 16. Matching seguro:
#   - TODO/FIXME/TBD/XXX: case-sensitive + whole word (não casa "todos").
#   - changeme/placeholder/lorem ipsum: case-insensitive literal.
info "K. Verificando marcadores de rascunho nos docs finais..."
FINAL_DOCS=("docs/FINAL-QA.md" "docs/ANDROID-BUILD-CHECKLIST.md")
for doc in "${FINAL_DOCS[@]}"; do
  reject_grep_re_tree '\b(TODO|FIXME|TBD|XXX)\b' "$doc" "sem TODO/FIXME/TBD/XXX em $doc"
  reject_grep_i_file "changeme"     "$doc" "sem 'changeme' em $doc"
  reject_grep_i_file "placeholder"  "$doc" "sem 'placeholder' em $doc"
  reject_grep_i_file "lorem ipsum"  "$doc" "sem 'lorem ipsum' em $doc"
done

# --- L. Nenhum lab 1..7 alterado nos commits desta branch --------------------
# Usa os commits da branch (origin/main..HEAD) — robusto contra ruído de
# working-tree (ex.: mudanças de modo de arquivo no mount Windows/WSL).
info "L. Verificando que labs 1..7 não foram alterados nos commits desta branch..."
REPO_ROOT="$(git -C "$LAB_DIR" rev-parse --show-toplevel 2>/dev/null || echo "$LAB_DIR/..")"
BRANCH_CHANGED=$(git -C "$REPO_ROOT" log origin/main..HEAD --name-only --format='' 2>/dev/null || \
                 git -C "$REPO_ROOT" diff --name-only HEAD~17..HEAD 2>/dev/null || true)
for lab_num in 1 2 3 4 5 6 7; do
  if echo "$BRANCH_CHANGED" | grep -q "lab-0${lab_num}-"; then
    fail "lab-0${lab_num} foi alterado nos commits desta branch"
  else
    pass "lab-0${lab_num} não alterado nos commits desta branch"
  fi
done

# --- O. Android SDK (best-effort, nunca falha por ausência) ------------------
info "O. Android build (best-effort; não exige Android SDK)..."
SDK_DIR=""
if [ -n "${ANDROID_HOME:-}" ]; then SDK_DIR="$ANDROID_HOME"; fi
if [ -z "$SDK_DIR" ] && [ -n "${ANDROID_SDK_ROOT:-}" ]; then SDK_DIR="$ANDROID_SDK_ROOT"; fi
if [ -z "$SDK_DIR" ] && [ -f "$APP/local.properties" ]; then
  SDK_DIR="$(grep -E '^sdk\.dir=' "$APP/local.properties" 2>/dev/null | head -n1 | cut -d= -f2-)"
fi
if [ -n "$SDK_DIR" ] && [ -d "$SDK_DIR" ] && [ -x "$APP/gradlew" ]; then
  info "Android SDK detectado em '$SDK_DIR' — tentando ./gradlew tasks (best-effort)..."
  if ( cd "$APP" && ./gradlew tasks >/dev/null 2>&1 ); then
    pass "./gradlew tasks executou (best-effort)"
  else
    warn "./gradlew tasks não concluiu — best-effort, não falha a Fase 16"
  fi
else
  warn "Android SDK não detectado — pulando build (best-effort, esperado neste ambiente)"
fi

# --- M. Rodar validações anteriores (Fase 14 e Fase 15) ----------------------
# Dinâmico de Docker é herdado da Fase 14 (não repetimos aqui — item N).
info "M. Executando scripts/validate-phase14.sh..."
if [ -f "scripts/validate-phase14.sh" ]; then
  if bash scripts/validate-phase14.sh; then
    pass "validate-phase14.sh passou"
  else
    fail "validate-phase14.sh falhou"
  fi
else
  fail "scripts/validate-phase14.sh ausente"
fi

info "M. Executando scripts/validate-phase15.sh..."
if [ -f "scripts/validate-phase15.sh" ]; then
  if bash scripts/validate-phase15.sh; then
    pass "validate-phase15.sh passou"
  else
    fail "validate-phase15.sh falhou"
  fi
else
  fail "scripts/validate-phase15.sh ausente"
fi

# --- Resultado ---------------------------------------------------------------
echo ""
if [ "$FAILED" -eq 0 ]; then
  printf '\033[32m[OK] Fase 16 validada com sucesso.\033[0m\n'
  exit 0
else
  printf '\033[31m[FAIL] Fase 16: uma ou mais verificações falharam.\033[0m\n' >&2
  exit 1
fi
