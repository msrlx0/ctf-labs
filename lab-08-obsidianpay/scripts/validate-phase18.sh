#!/usr/bin/env bash
#
# validate-phase18.sh — valida a Fase 18 do Lab 08 (acabamento de documentação
# pública).
#
# A Fase 18 NÃO altera backend, app, flags nem endpoints da Fase 14: é o passe de
# acabamento da documentação pública. Especificamente:
#
#   A. Confere que os scripts validate-phase1..17 existem.
#   B. README.md: seção "Vulnerabilidades presentes", as 4 colunas
#      (Categoria / Vulnerabilidade / Onde aparece no lab / O que o aluno aprende)
#      e os 22 termos-chave de vulnerabilidade.
#   C. STUDENT-GUIDE.md: seção "Passo a passo manual sugerido", as estações
#      manuais e o placeholder <flag_redacted> (sem FLAG{).
#   D. WALKTHROUGH.md (instrutor-facing): "Stage 01", "Stage 09",
#      "Final Operator Chain" e a flag final.
#   E. Anti-leak: sem FLAG{ em docs públicos/tools.
#   F. Credenciais internas: sem analyst123/operator123 em material público.
#   G. Typos conhecidos (mesmos guards da Fase 17), com cuidado para NÃO casar com
#      a forma correta LegacyRequestSigner.
#   H. Confere que nenhum lab 1..7 foi alterado (commits da branch + working tree,
#      ignorando ruído de modo de arquivo do mount Windows/WSL).
#   I. Roda scripts/validate-phase16.sh e scripts/validate-phase17.sh.
#   J. Não exige Android SDK (build real continua no Android Studio).
#
# Sai com exit 1 se qualquer verificação obrigatória falhar.
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$LAB_DIR"

APP="android-app"
SRC="$APP/app/src/main/java/com/obsidianpay/mobile"
NSC="$APP/app/src/main/res/xml/network_security_config.xml"

pass() { printf '  \033[32m[PASS]\033[0m %s\n' "$1"; }
warn() { printf '  \033[33m[WARN]\033[0m %s\n' "$1"; }
info() { printf '\033[36m[*]\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m[FAIL]\033[0m %s\n' "$1" >&2; FAILED=1; }

FAILED=0

need_file() { if [ -f "$1" ]; then pass "arquivo: $1"; else fail "ausente: $1"; fi; }

# Exige um literal (grep -F) em um único arquivo.
need_grep_file() {
  if [ -f "$2" ] && grep -qF "$1" "$2"; then pass "$3"; else fail "$3 (esperado '$1' em $2)"; fi
}

# Exige um padrão (regex) em um único arquivo.
need_grep_re_file() {
  if [ -f "$2" ] && grep -Eq "$1" "$2"; then pass "$3"; else fail "$3 (esperado /$1/ em $2)"; fi
}

# Rejeita um literal numa árvore/arquivo.
reject_grep_tree() {
  if grep -rqF "$1" "$2" 2>/dev/null; then
    fail "$3 (string '$1' encontrada em $2)"
  else
    pass "$3"
  fi
}

# Rejeita um padrão (regex) numa árvore/arquivo.
reject_grep_re_tree() {
  if grep -rEq "$1" "$2" 2>/dev/null; then
    fail "$3 (padrão /$1/ encontrado em $2)"
  else
    pass "$3"
  fi
}

info "Diretório do lab: $LAB_DIR"

# --- A. Scripts de fases anteriores (1..17) ----------------------------------
info "A. Conferindo scripts de fases anteriores (1..17)..."
for n in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17; do need_file "scripts/validate-phase$n.sh"; done

# --- B. README.md: tabela de vulnerabilidades --------------------------------
info "B. Conferindo README.md (tabela de vulnerabilidades)..."
need_grep_file "Vulnerabilidades presentes" "README.md" "README tem seção 'Vulnerabilidades presentes'"

# Linha-cabeçalho da tabela com as 4 colunas, na ordem.
need_grep_re_file 'Categoria.*Vulnerabilidade.*Onde aparece no lab.*O que o aluno aprende' \
  "README.md" "README tem as colunas Categoria/Vulnerabilidade/Onde aparece no lab/O que o aluno aprende"

info "B. Conferindo termos-chave de vulnerabilidade no README..."
README_TERMS=(
  "Insecure Mobile Storage"
  "IDOR"
  "Mass Assignment"
  "Information Disclosure"
  "WebView"
  "JavaScript Bridge"
  "Deep Link"
  "Exported Activity"
  "BroadcastReceiver"
  "ContentProvider"
  "Hardcoded Secrets"
  "Weak Crypto"
  "Device Trust"
  "Root Detection"
  "Emulator Detection"
  "Biometric"
  "Network Security"
  "Certificate Pinning"
  "Native"
  "Anti-Tamper"
  "Dynamic Instrumentation"
  "Challenge Chain"
)
for term in "${README_TERMS[@]}"; do
  need_grep_file "$term" "README.md" "README cita '$term'"
done

# --- C. STUDENT-GUIDE.md: passo a passo manual sem spoiler -------------------
info "C. Conferindo STUDENT-GUIDE.md (passo a passo manual)..."
need_grep_file "Passo a passo manual sugerido" "STUDENT-GUIDE.md" "STUDENT-GUIDE tem seção 'Passo a passo manual sugerido'"

GUIDE_STATIONS=(
  "Preparar o backend"
  "Preparar o app"
  "Login inicial"
  "Recon mobile"
  "Armazenamento local"
  "WebView"
  "Componentes exportados"
  "Reverse engineering"
  "Device Trust"
  "Network"
  "Submissão de progresso"
  "Evidências finais"
  "<flag_redacted>"
)
for st in "${GUIDE_STATIONS[@]}"; do
  need_grep_file "$st" "STUDENT-GUIDE.md" "STUDENT-GUIDE cobre '$st'"
done

# --- D. WALKTHROUGH.md: continua instrutor-facing (com flags) -----------------
info "D. Conferindo WALKTHROUGH.md (instrutor-facing)..."
need_grep_file "Stage 01"            "WALKTHROUGH.md" "WALKTHROUGH tem 'Stage 01'"
need_grep_file "Stage 09"            "WALKTHROUGH.md" "WALKTHROUGH tem 'Stage 09'"
need_grep_file "Final Operator Chain" "WALKTHROUGH.md" "WALKTHROUGH tem 'Final Operator Chain'"
need_grep_file "FLAG{obsidianpay_final_operator_chain_09}" "WALKTHROUGH.md" "WALKTHROUGH tem a flag final do operador"

# --- E. Anti-leak: docs públicos / tools NÃO contêm FLAG{ --------------------
info "E. Verificando que docs públicos/tools NÃO contêm FLAG{..."
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

# --- F. Credenciais internas (analyst123/operator123) ------------------------
info "F. Verificando ausência de credenciais internas em material público..."
CRED_DOCS=(
  "README.md"
  "STUDENT-GUIDE.md"
  "android-app/README.md"
  "docs/ARCHITECTURE.md"
  "docs/PHASES.md"
  "docs/VULNERABILITY-ROADMAP.md"
  "docs/CHALLENGE-SCORING.md"
  "docs/FINAL-QA.md"
  "docs/ANDROID-BUILD-CHECKLIST.md"
  "docs/mobile-pentest/SETUP.md"
  "docs/mobile-pentest/PLAYBOOK.md"
)
for doc in "${CRED_DOCS[@]}"; do
  if [ -e "$doc" ]; then
    reject_grep_tree "analyst123"  "$doc" "sem analyst123 em $doc"
    reject_grep_tree "operator123" "$doc" "sem operator123 em $doc"
  fi
done
reject_grep_tree "analyst123"  "tools" "sem analyst123 em tools/"
reject_grep_tree "operator123" "tools" "sem operator123 em tools/"

# --- G. Typos conhecidos -----------------------------------------------------
info "G. Verificando ausência de typos críticos..."
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
  # network_security_config.xml também é varrido para o typo de cleartext-override.
  reject_grep_tree "network-config-cleartext-overrie" "$NSC" "sem typo cleartext-override no network-security-config"
  # Formas que SÃO substring das corretas — usar âncora por regex:
  reject_grep_re_tree 'getSessionSummar\('   "$SRC" "sem typo 'getSessionSummar('"
  reject_grep_re_tree '@JavascriptInterfac$' "$SRC" "sem typo '@JavascriptInterfac'"
  # CUIDADO: 'LegacyRequestSigne' casa com o correto 'LegacyRequestSigner'.
  # Validar o typo apenas como palavra inteira (boundary), nunca por substring bruta.
  reject_grep_re_tree 'LegacyRequestSigne\b' "$SRC" "sem typo 'LegacyRequestSigne' (palavra inteira)"
else
  warn "código-fonte Android ausente ($SRC) — pulando guards de typos (best-effort)"
fi

# --- H. Nenhum lab 1..7 alterado ---------------------------------------------
# Combina commits da branch (origin/main..HEAD) com o working tree apenas por
# mudança de CONTEÚDO (git diff -G'.' ignora diffs só de modo do mount Win/WSL).
info "H. Verificando que labs 1..7 não foram alterados..."
REPO_ROOT="$(git -C "$LAB_DIR" rev-parse --show-toplevel 2>/dev/null || echo "$LAB_DIR/..")"
LAB_CHANGED="$( {
  git -C "$REPO_ROOT" log origin/main..HEAD --name-only --format='' 2>/dev/null
  git -C "$REPO_ROOT" diff --name-only -G'.' 2>/dev/null
  git -C "$REPO_ROOT" diff --cached --name-only -G'.' 2>/dev/null
} | sort -u )"
for lab_num in 1 2 3 4 5 6 7; do
  if printf '%s\n' "$LAB_CHANGED" | grep -q "lab-0${lab_num}-"; then
    fail "lab-0${lab_num} foi alterado (conteúdo)"
  else
    pass "lab-0${lab_num} não alterado"
  fi
done

# --- I. Rodar validações anteriores (Fases 16 e 17) --------------------------
# A Fase 17 já encadeia 14/15/16; rodar 16 e 17 cobre toda a cadeia anterior.
info "I. Executando scripts/validate-phase16.sh..."
if [ -f "scripts/validate-phase16.sh" ]; then
  if bash scripts/validate-phase16.sh; then pass "validate-phase16.sh passou"; else fail "validate-phase16.sh falhou"; fi
else
  fail "scripts/validate-phase16.sh ausente"
fi

info "I. Executando scripts/validate-phase17.sh..."
if [ -f "scripts/validate-phase17.sh" ]; then
  if bash scripts/validate-phase17.sh; then pass "validate-phase17.sh passou"; else fail "validate-phase17.sh falhou"; fi
else
  fail "scripts/validate-phase17.sh ausente"
fi

# --- J. Android SDK não exigido ----------------------------------------------
info "J. Android SDK não é exigido na Fase 18 (build real é feito no Android Studio)."

# --- Resultado ---------------------------------------------------------------
echo ""
if [ "$FAILED" -eq 0 ]; then
  printf '\033[32m[OK] Fase 18 validada com sucesso.\033[0m\n'
  exit 0
else
  printf '\033[31m[FAIL] Fase 18: uma ou mais verificações falharam.\033[0m\n' >&2
  exit 1
fi
