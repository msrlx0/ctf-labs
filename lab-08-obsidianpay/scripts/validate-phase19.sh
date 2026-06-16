#!/usr/bin/env bash
#
# validate-phase19.sh — valida a Fase 19 do Lab 08 (walkthrough final de
# instrutor para iniciante + consolidação do roadmap + classificação do README).
#
# A Fase 19 NÃO altera backend, app, flags nem endpoints da Fase 14. Ela:
#
#   A. Confere que os scripts validate-phase1..18 existem.
#   B. WALKTHROUGH.md NÃO contém as expressões desatualizadas (app futuro / sem
#      cadeia final / Estado: Fase 12-13 / etc.).
#   C. WALKTHROUGH.md contém o estado final, as 5 partes de preparação, os 9
#      stages, a estrutura obrigatória de cada stage e os marcadores de scoreboard.
#   D. As 9 flags reais aparecem no WALKTHROUGH.md.
#   E. Cada stage (01..09) contém objetivo, passos numerados, flag, submit,
#      evidência, troubleshooting e checklist.
#   F. README.md tem as 3 seções (Vulnerabilidades presentes / Scaffolds e
#      técnicas educacionais / Recursos do CTF).
#   G. VULNERABILITY-ROADMAP.md: sem "atualizado na Fase 2"; tem "Status final",
#      "Tipo" e "Stage relacionado"; diferencia scaffold de vulnerabilidade; trata
#      Exported Service e Native Pinning como não implementados (conforme o código).
#   H. Anti-leak: docs públicos continuam sem FLAG{.
#   I. Nenhum lab 1..7 alterado (conteúdo; ignora ruído de modo do mount Win/WSL).
#   J. Roda scripts/validate-phase17.sh e scripts/validate-phase18.sh.
#   K. Não exige Android SDK.
#
# Sai com exit 1 se qualquer verificação obrigatória falhar.
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$LAB_DIR"

WT="WALKTHROUGH.md"
RM="README.md"
ROADMAP="docs/VULNERABILITY-ROADMAP.md"

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

# Rejeita um literal num único arquivo.
reject_grep_file() {
  if [ -f "$2" ] && grep -qF "$1" "$2"; then
    fail "$3 (string '$1' encontrada em $2)"
  else
    pass "$3"
  fi
}

# Rejeita um literal numa árvore/arquivo.
reject_grep_tree() {
  if grep -rqF "$1" "$2" 2>/dev/null; then
    fail "$3 (string '$1' encontrada em $2)"
  else
    pass "$3"
  fi
}

# Extrai o bloco de um stage: da linha "## Stage 0N " até a próxima "## " (nível 2).
stage_block() {
  local n="$1"
  awk -v s="## Stage 0${n} " '
    index($0, s) == 1 { grab = 1 }
    grab && /^## / && index($0, s) != 1 { grab = 0 }
    grab { print }
  ' "$WT"
}

info "Diretório do lab: $LAB_DIR"

# --- A. Scripts de fases anteriores (1..18) ----------------------------------
info "A. Conferindo scripts de fases anteriores (1..18)..."
for n in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18; do need_file "scripts/validate-phase$n.sh"; done

# --- B. WALKTHROUGH.md: SEM expressões desatualizadas ------------------------
info "B. Verificando que WALKTHROUGH.md não tem conteúdo desatualizado..."
need_file "$WT"
STALE=(
  "Estado: Fase 12"
  "Estado: Fase 13"
  "App Android (futuro)"
  "Cadeias futuras planejadas"
  "sem cadeia final completa"
  "cadeia/solução final do Lab 8"
  "Fase 1 (atual)"
  "fica para fase futura"
)
for s in "${STALE[@]}"; do
  reject_grep_file "$s" "$WT" "WALKTHROUGH sem '$s'"
done

# --- C. WALKTHROUGH.md: estado final + partes + estrutura --------------------
info "C. Conferindo conteúdo final obrigatório do WALKTHROUGH.md..."
WT_REQUIRED=(
  "Estado: Final"
  "Preparação completa"
  "Configuração do Burp Suite"
  "Análise estática do APK"
  "ADB para iniciantes"
  "Frida para iniciantes"
  "Stage 01"
  "Stage 02"
  "Stage 03"
  "Stage 04"
  "Stage 05"
  "Stage 06"
  "Stage 07"
  "Stage 08"
  "Stage 09"
  "O que deve estar aberto"
  "Onde começar no aplicativo"
  "Passo a passo manual"
  "O que observar"
  "Por que isso é vulnerável"
  "Como obter a flag"
  "Como submeter a flag"
  "Evidência obrigatória"
  "Erros comuns"
  "Checklist da etapa"
  "Final Operator Chain"
  "completionPercent"
  "finalUnlocked"
)
for s in "${WT_REQUIRED[@]}"; do
  need_grep_file "$s" "$WT" "WALKTHROUGH contém '$s'"
done

# --- D. WALKTHROUGH.md: as 9 flags reais -------------------------------------
info "D. Conferindo as 9 flags reais no WALKTHROUGH.md..."
FLAGS=(
  "FLAG{obsidianpay_mobile_recon_01}"
  "FLAG{obsidianpay_insecure_storage_02}"
  "FLAG{obsidianpay_exported_components_03}"
  "FLAG{obsidianpay_webview_bridge_04}"
  "FLAG{obsidianpay_device_trust_05}"
  "FLAG{obsidianpay_biometric_vault_06}"
  "FLAG{obsidianpay_network_pinning_07}"
  "FLAG{obsidianpay_integrity_bypass_08}"
  "FLAG{obsidianpay_final_operator_chain_09}"
)
for f in "${FLAGS[@]}"; do
  need_grep_file "$f" "$WT" "WALKTHROUGH contém $f"
done

# --- E. Cada stage tem a estrutura completa ----------------------------------
info "E. Conferindo estrutura por stage (objetivo/passos/flag/submit/evidência/troubleshooting/checklist)..."
for n in 1 2 3 4 5 6 7 8 9; do
  blk="$(stage_block "$n")"
  flag="${FLAGS[$((n-1))]}"
  if [ -z "$blk" ]; then
    fail "Stage 0$n: bloco não encontrado no WALKTHROUGH"
    continue
  fi
  check_blk() {
    # $1 = modo (-F literal | -E regex), $2 = padrão, $3 = rótulo
    if printf '%s\n' "$blk" | grep -q "$1" -- "$2"; then
      pass "Stage 0$n: $3"
    else
      fail "Stage 0$n: faltando $3"
    fi
  }
  check_blk -F "### Objetivo"            "objetivo"
  check_blk -E '^[[:space:]]*1\.'        "passos numerados"
  check_blk -F "$flag"                   "flag ($flag)"
  check_blk -F "Como submeter a flag"    "seção de submit"
  check_blk -F "Evidência obrigatória"   "evidência"
  check_blk -F "Erros comuns"            "troubleshooting"
  check_blk -F "Checklist da etapa"      "checklist"
done

# --- F. README.md: as 3 seções -----------------------------------------------
info "F. Conferindo as 3 seções de classificação no README.md..."
need_grep_file "## Vulnerabilidades presentes"        "$RM" "README tem '## Vulnerabilidades presentes'"
need_grep_file "## Scaffolds e técnicas educacionais" "$RM" "README tem '## Scaffolds e técnicas educacionais'"
need_grep_file "## Recursos do CTF"                   "$RM" "README tem '## Recursos do CTF'"
# A tabela de vulnerabilidades mantém as 4 colunas (compat. Fase 18).
need_grep_re_file 'Categoria.*Vulnerabilidade.*Onde aparece no lab.*O que o aluno aprende' \
  "$RM" "README mantém o cabeçalho de 4 colunas da tabela de vulnerabilidades"

# --- G. VULNERABILITY-ROADMAP.md: matriz final consolidada -------------------
info "G. Conferindo VULNERABILITY-ROADMAP.md (matriz final)..."
need_file "$ROADMAP"
reject_grep_file "atualizado na Fase 2" "$ROADMAP" "ROADMAP não apresenta a tabela como 'atualizado na Fase 2'"
need_grep_file "Status final"      "$ROADMAP" "ROADMAP contém coluna 'Status final'"
need_grep_file "Tipo"              "$ROADMAP" "ROADMAP contém coluna 'Tipo'"
need_grep_file "Stage relacionado" "$ROADMAP" "ROADMAP contém coluna 'Stage relacionado'"
# Diferencia scaffold de vulnerabilidade.
need_grep_file "Scaffold educacional"        "$ROADMAP" "ROADMAP usa o tipo 'Scaffold educacional'"
need_grep_file "Vulnerabilidade explorável"  "$ROADMAP" "ROADMAP usa o tipo 'Vulnerabilidade explorável'"
need_grep_file "Não implementado como desafio independente" "$ROADMAP" "ROADMAP usa 'Não implementado como desafio independente'"
# Exported Service e Native Pinning tratados corretamente (não implementados).
need_grep_re_file 'Exported Service.*[Nn]ão implementado' "$ROADMAP" "ROADMAP marca Exported Service como não implementado"
need_grep_re_file 'Native pinning.*[Nn]ão implementado'   "$ROADMAP" "ROADMAP marca Native pinning como não implementado"

# --- H. Anti-leak: docs públicos sem FLAG{ -----------------------------------
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

# --- I. Nenhum lab 1..7 alterado ---------------------------------------------
info "I. Verificando que labs 1..7 não foram alterados..."
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

# --- J. Rodar validações anteriores (Fases 17 e 18) --------------------------
info "J. Executando scripts/validate-phase17.sh..."
if [ -f "scripts/validate-phase17.sh" ]; then
  if bash scripts/validate-phase17.sh; then pass "validate-phase17.sh passou"; else fail "validate-phase17.sh falhou"; fi
else
  fail "scripts/validate-phase17.sh ausente"
fi

info "J. Executando scripts/validate-phase18.sh..."
if [ -f "scripts/validate-phase18.sh" ]; then
  if bash scripts/validate-phase18.sh; then pass "validate-phase18.sh passou"; else fail "validate-phase18.sh falhou"; fi
else
  fail "scripts/validate-phase18.sh ausente"
fi

# --- K. Android SDK não exigido ----------------------------------------------
info "K. Android SDK não é exigido na Fase 19 (build real é feito no Android Studio)."

# --- Resultado ---------------------------------------------------------------
echo ""
if [ "$FAILED" -eq 0 ]; then
  printf '\033[32m[OK] Fase 19 validada com sucesso.\033[0m\n'
  exit 0
else
  printf '\033[31m[FAIL] Fase 19: uma ou mais verificações falharam.\033[0m\n' >&2
  exit 1
fi
