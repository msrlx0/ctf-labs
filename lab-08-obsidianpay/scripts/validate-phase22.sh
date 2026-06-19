#!/usr/bin/env bash
#
# validate-phase22.sh — valida a Fase 22A do Lab 08 (ObsidianPay Mobile):
# pipeline de build do APK no GitHub Actions + superfície de download +
# consistência da documentação pública.
#
# A Fase 22A NÃO redesenha o app/backend nem altera flags/contratos. Ela torna o
# GitHub o ponto oficial de distribuição do APK candidato a QA. Este script:
#
#   A. Confere que os arquivos novos/exigidos existem (workflow, DOWNLOAD.md,
#      package-android-apk.sh, validate-phase18/19/20).
#   B. Confere que o workflow YAML é parseável (parser seguro se houver; senão um
#      fallback estrutural focado).
#   C. Workflow: JDK 17 (Temurin).
#   D. Workflow: mira o gradlew real do Lab 08 (lab-08-obsidianpay/android-app).
#   E. Workflow: builda o APK debug (assembleDebug, --no-daemon).
#   F. Workflow: verifica a existência do APK e falha se ausente.
#   G. Workflow: cria o APK renomeado esperado (ObsidianPay-Lab08-v1.0.0-rc2.apk).
#   H. Workflow: gera o arquivo .sha256.
#   I. Workflow: faz upload apenas do APK e do checksum (sem caches/fonte/backend).
#   J. Workflow: NÃO cria release nem push de tag; não expõe segredos.
#   K. DOWNLOAD.md existe e tem os links/itens exigidos.
#   L. README do lab: status não contraditório (estável NÃO publicado + Fase 22A).
#   M. README do app Android: sem status de release obsoleto.
#   N. README raiz lista o Lab 08.
#   O. Docs/scripts/workflow públicos sem FLAG{ (anti-leak).
#   P. Docs/scripts/workflow públicos sem credenciais privadas (analyst123/operator123).
#   Q. Nenhum binário .apk foi adicionado ao Git.
#   R. Sem caminhos absolutos específicos de máquina no workflow/script.
#   S. Nenhum lab 1..7 alterado (conteúdo).
#   T. Shell scripts passam em `bash -n`.
#   U. Regressões: validate-phase18.sh, validate-phase19.sh, validate-phase20.sh.
#   V. `git diff --check` passa.
#
# Não exige Android SDK (o build real do APK roda no GitHub Actions / Android Studio).
# Sai com exit 1 se qualquer verificação obrigatória falhar.
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$LAB_DIR"

REPO_ROOT="$(git -C "$LAB_DIR" rev-parse --show-toplevel 2>/dev/null || (cd "$LAB_DIR/.." && pwd))"

WF="$REPO_ROOT/.github/workflows/lab08-android-apk.yml"
ROOT_README="$REPO_ROOT/README.md"
APP_README="android-app/README.md"
DOWNLOAD="DOWNLOAD.md"
PKG="scripts/package-android-apk.sh"

ARTIFACT_APK="ObsidianPay-Lab08-v1.0.0-rc2.apk"
ARTIFACT_NAME="obsidianpay-lab08-v1.0.0-rc2"

pass() { printf '  \033[32m[PASS]\033[0m %s\n' "$1"; }
warn() { printf '  \033[33m[WARN]\033[0m %s\n' "$1"; }
info() { printf '\033[36m[*]\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m[FAIL]\033[0m %s\n' "$1" >&2; FAILED=1; }

FAILED=0

need_file() { if [ -f "$1" ]; then pass "arquivo: $1"; else fail "ausente: $1"; fi; }

# Obs.: o `--` em todos os greps permite padrões que começam com '-' (ex.: '--no-daemon').
need_grep_file() {
  if [ -f "$2" ] && grep -qF -- "$1" "$2"; then pass "$3"; else fail "$3 (esperado '$1' em $2)"; fi
}

need_grep_re_file() {
  if [ -f "$2" ] && grep -Eq -- "$1" "$2"; then pass "$3"; else fail "$3 (esperado /$1/ em $2)"; fi
}

reject_grep_file() {
  if [ -f "$2" ] && grep -qF -- "$1" "$2"; then
    fail "$3 (string '$1' encontrada em $2)"
  else
    pass "$3"
  fi
}

reject_grep_re_file() {
  if [ -f "$2" ] && grep -Eq -- "$1" "$2"; then
    fail "$3 (padrão /$1/ encontrado em $2)"
  else
    pass "$3"
  fi
}

reject_grep_tree() {
  if grep -rqF -- "$1" "$2" 2>/dev/null; then
    fail "$3 (string '$1' encontrada em $2)"
  else
    pass "$3"
  fi
}

info "Diretório do lab:  $LAB_DIR"
info "Raiz do repo:      $REPO_ROOT"

# --- A. Arquivos exigidos ----------------------------------------------------
info "A. Conferindo arquivos exigidos..."
need_file "$WF"
need_file "$DOWNLOAD"
need_file "$PKG"
need_file "scripts/validate-phase18.sh"
need_file "scripts/validate-phase19.sh"
need_file "scripts/validate-phase20.sh"

# --- B. Workflow YAML parseável ----------------------------------------------
info "B. Conferindo que o workflow YAML é parseável..."
yaml_parser=""
if command -v python3 >/dev/null 2>&1 && python3 -c 'import yaml' >/dev/null 2>&1; then
  yaml_parser="python3"
elif command -v ruby >/dev/null 2>&1 && ruby -ryaml -e '1' >/dev/null 2>&1; then
  yaml_parser="ruby"
fi

if [ -n "$yaml_parser" ]; then
  if [ "$yaml_parser" = "python3" ]; then
    if python3 -c 'import sys,yaml; yaml.safe_load(open(sys.argv[1]))' "$WF" >/dev/null 2>&1; then
      pass "workflow YAML parseável (python3/yaml)"
    else
      fail "workflow YAML inválido (python3/yaml)"
    fi
  else
    if ruby -ryaml -e 'YAML.safe_load(File.read(ARGV[0]))' "$WF" >/dev/null 2>&1; then
      pass "workflow YAML parseável (ruby/yaml)"
    else
      fail "workflow YAML inválido (ruby/yaml)"
    fi
  fi
else
  warn "nenhum parser YAML seguro disponível — usando fallback estrutural focado"
fi

# Checagens estruturais (sempre — também valem como requisitos de conteúdo):
need_grep_re_file '^name:[[:space:]]*Lab 08 Android APK' "$WF" "workflow tem nome 'Lab 08 Android APK'"
need_grep_re_file '^on:'                  "$WF" "workflow declara 'on:'"
need_grep_re_file 'workflow_dispatch:'    "$WF" "workflow suporta workflow_dispatch"
need_grep_re_file '^jobs:'                "$WF" "workflow declara 'jobs:'"
need_grep_re_file 'runs-on:[[:space:]]*ubuntu' "$WF" "workflow roda em runner Ubuntu"
need_grep_re_file 'steps:'                "$WF" "workflow declara 'steps:'"
need_grep_file 'actions/checkout'         "$WF" "workflow faz checkout do repositório"

# --- C. JDK 17 (Temurin) -----------------------------------------------------
info "C. Conferindo JDK 17 (Temurin)..."
need_grep_file 'actions/setup-java'   "$WF" "workflow usa setup-java"
need_grep_file 'temurin'              "$WF" "workflow usa distribuição Temurin"
need_grep_re_file "java-version:[[:space:]]*'?17'?" "$WF" "workflow usa JDK 17"

# --- D. Mira o gradlew real do Lab 08 ----------------------------------------
info "D. Conferindo alvo do build (gradlew real do Lab 08)..."
need_grep_file 'lab-08-obsidianpay/android-app' "$WF" "workflow aponta para lab-08-obsidianpay/android-app"
need_grep_file 'gradlew'             "$WF" "workflow usa o gradlew do projeto"
need_grep_file 'android-actions/setup-android' "$WF" "workflow configura o Android SDK"
need_grep_file 'platforms;android-34' "$WF" "workflow instala a platform android-34 (compileSdk/targetSdk 34)"
need_grep_file 'build-tools;34'      "$WF" "workflow instala build-tools 34"

# --- E. Build do APK debug ---------------------------------------------------
info "E. Conferindo build do APK debug..."
need_grep_file 'assembleDebug'       "$WF" "workflow roda assembleDebug"
need_grep_file '--no-daemon'         "$WF" "workflow usa --no-daemon"

# --- F. Verificação de existência do APK -------------------------------------
info "F. Conferindo verificação de existência do APK..."
need_grep_file 'app/build/outputs/apk/debug/app-debug.apk' "$WF" "workflow referencia o caminho real do app-debug.apk"
need_grep_re_file 'if \[ ! -f' "$WF" "workflow testa a existência do APK"
need_grep_re_file 'exit 1|::error::' "$WF" "workflow falha claramente quando o APK não existe"

# --- G. APK renomeado esperado -----------------------------------------------
info "G. Conferindo APK renomeado esperado..."
need_grep_file "$ARTIFACT_APK" "$WF" "workflow cria $ARTIFACT_APK"

# --- H. Arquivo .sha256 ------------------------------------------------------
info "H. Conferindo geração do .sha256..."
need_grep_file 'sha256sum'           "$WF" "workflow calcula sha256"
# O nome do .sha256 é montado via variável (\"${ARTIFACT_APK}.sha256\"); valida o sufixo .sha256.
need_grep_re_file '\.apk\.sha256|ARTIFACT_APK[}]*\.sha256' "$WF" "workflow gera o arquivo .sha256 do APK"

# --- I. Upload apenas do APK + checksum --------------------------------------
info "I. Conferindo upload (apenas APK + checksum)..."
need_grep_file 'actions/upload-artifact' "$WF" "workflow faz upload de artefato"
need_grep_file "$ARTIFACT_NAME"      "$WF" "workflow usa o nome de artefato $ARTIFACT_NAME"
# Não pode subir caches/fonte/backend/walkthrough/flags.
reject_grep_file 'node_modules' "$WF" "workflow NÃO faz upload de node_modules"
reject_grep_re_file '\.gradle/caches|/\.gradle' "$WF" "workflow NÃO faz upload de caches do Gradle"
reject_grep_file 'WALKTHROUGH' "$WF" "workflow não referencia WALKTHROUGH"
reject_grep_file 'flags.js' "$WF" "workflow não referencia flags.js"
reject_grep_re_file '^[[:space:]]*path:[[:space:]]*\.[[:space:]]*$' "$WF" "workflow não faz upload do repositório inteiro"

# --- J. Sem release / sem tag / sem segredos ---------------------------------
info "J. Conferindo ausência de release/tag/segredos..."
reject_grep_re_file 'git[[:space:]]+push'   "$WF" "workflow NÃO faz git push"
reject_grep_re_file 'git[[:space:]]+tag'    "$WF" "workflow NÃO cria git tag"
reject_grep_re_file 'gh[[:space:]]+release' "$WF" "workflow NÃO usa gh release"
reject_grep_file 'create-release'           "$WF" "workflow NÃO usa actions/create-release"
reject_grep_file 'action-gh-release'        "$WF" "workflow NÃO usa softprops/action-gh-release"
reject_grep_file 'release-action'           "$WF" "workflow NÃO usa ncipollo/release-action"
reject_grep_re_file 'secrets\.'             "$WF" "workflow não referencia secrets."

# --- K. DOWNLOAD.md: links e itens exigidos ----------------------------------
info "K. Conferindo DOWNLOAD.md (links e itens)..."
need_grep_file "$ARTIFACT_APK"          "$DOWNLOAD" "DOWNLOAD cita o APK candidato"
need_grep_file "$ARTIFACT_APK.sha256"   "$DOWNLOAD" "DOWNLOAD cita o arquivo .sha256"
need_grep_file "$ARTIFACT_NAME"         "$DOWNLOAD" "DOWNLOAD cita o nome do artefato do Actions"
need_grep_file 'lab08-android-apk.yml'  "$DOWNLOAD" "DOWNLOAD aponta para o workflow do Lab 08"
need_grep_re_file 'API 24|Android 7'    "$DOWNLOAD" "DOWNLOAD informa o Android mínimo (minSdk 24)"
need_grep_file 'adb install -r'         "$DOWNLOAD" "DOWNLOAD documenta adb install -r"
need_grep_file 'com.obsidianpay.mobile.debug' "$DOWNLOAD" "DOWNLOAD cita o pacote debug"
need_grep_re_file 'pm list packages'    "$DOWNLOAD" "DOWNLOAD documenta a verificação do pacote"
need_grep_file 'adb uninstall'          "$DOWNLOAD" "DOWNLOAD documenta a desinstalação"
need_grep_file 'Get-FileHash'           "$DOWNLOAD" "DOWNLOAD documenta verificação de hash no PowerShell"
need_grep_file 'sha256sum'              "$DOWNLOAD" "DOWNLOAD documenta verificação de hash no Linux"
need_grep_file '10.0.2.2:8102'          "$DOWNLOAD" "DOWNLOAD cita o API Host do emulador"
need_grep_file 'adb reverse'            "$DOWNLOAD" "DOWNLOAD documenta a alternativa adb reverse"
need_grep_file 'guest123'               "$DOWNLOAD" "DOWNLOAD documenta a credencial pública guest"
need_grep_file 'docker compose up'      "$DOWNLOAD" "DOWNLOAD documenta como subir o backend"
need_grep_file 'STUDENT-GUIDE.md'       "$DOWNLOAD" "DOWNLOAD linka o STUDENT-GUIDE"
need_grep_file 'docs/mobile-pentest/SETUP.md' "$DOWNLOAD" "DOWNLOAD linka o SETUP de pentest mobile"
need_grep_re_file '\]\(\./README\.md\)' "$DOWNLOAD" "DOWNLOAD linka de volta o README do lab"

# --- L. README do lab: status não contraditório ------------------------------
info "L. Conferindo README do lab (status coerente)..."
need_grep_file 'DOWNLOAD.md'   "README.md" "README do lab linka DOWNLOAD.md"
need_grep_file 'Fase 22A'      "README.md" "README do lab cita a Fase 22A (pipeline de QA)"
need_grep_file 'ainda NÃO publicado' "README.md" "README do lab afirma que o APK estável ainda NÃO foi publicado"
need_grep_re_file 'celular físico|smoke test' "README.md" "README do lab cita a validação em celular físico"
# Não pode afirmar (concluído ✅) que o APK foi publicado.
reject_grep_re_file '✅.*APK.*publicad' "README.md" "README do lab não afirma APK estável publicado (sem contradição)"

# --- M. README do app: sem status de release obsoleto ------------------------
info "M. Conferindo README do app Android (sem status obsoleto)..."
need_file "$APP_README"
reject_grep_file 'Ainda não há APK final publicado' "$APP_README" "app README sem status de release obsoleto"
reject_grep_file 'App Android (Fase 12)' "$APP_README" "app README sem título de fase obsoleto"
need_grep_file '../DOWNLOAD.md' "$APP_README" "app README linka ../DOWNLOAD.md"
need_grep_file "$ARTIFACT_APK"  "$APP_README" "app README cita o artefato candidato a QA"
need_grep_file 'app/build/outputs/apk/debug/app-debug.apk' "$APP_README" "app README documenta o output do assembleDebug"
need_grep_file 'com.obsidianpay.mobile.debug' "$APP_README" "app README distingue o applicationId debug"

# --- N. README raiz lista o Lab 08 -------------------------------------------
info "N. Conferindo README raiz (tabela de labs)..."
need_grep_file 'lab-08-obsidianpay' "$ROOT_README" "README raiz referencia lab-08-obsidianpay"
need_grep_file '8102'               "$ROOT_README" "README raiz cita a porta 8102 do Lab 08"
need_grep_re_file 'ObsidianPay'     "$ROOT_README" "README raiz nomeia o Lab 08 (ObsidianPay)"

# --- O. Anti-leak: sem FLAG{ em material público -----------------------------
# Obs.: validate-phase14/15/22.sh PODEM conter o literal 'FLAG{' e os nomes de
# credenciais como padrões de busca (mesma convenção dos validadores anteriores);
# por isso os scripts validadores não são varridos aqui.
info "O. Anti-leak: sem FLAG{ em docs/scripts/workflow públicos..."
PUBLIC=(
  "README.md"
  "STUDENT-GUIDE.md"
  "DOWNLOAD.md"
  "android-app/README.md"
  "docs/ANDROID-BUILD-CHECKLIST.md"
  "docs/FINAL-QA.md"
  "docs/mobile-pentest/SETUP.md"
  "$PKG"
)
for f in "${PUBLIC[@]}"; do
  if [ -e "$f" ]; then reject_grep_tree "FLAG{" "$f" "sem FLAG{ em $f"; else warn "ausente (pulando): $f"; fi
done
reject_grep_tree "FLAG{" "$WF" "sem FLAG{ no workflow"

# --- P. Anti-leak: sem credenciais privadas ----------------------------------
info "P. Anti-leak: sem credenciais privadas em material público..."
for f in "README.md" "STUDENT-GUIDE.md" "DOWNLOAD.md" "android-app/README.md" "$PKG"; do
  if [ -e "$f" ]; then
    reject_grep_tree "analyst123"  "$f" "sem analyst123 em $f"
    reject_grep_tree "operator123" "$f" "sem operator123 em $f"
  fi
done
reject_grep_tree "analyst123"  "$WF" "sem analyst123 no workflow"
reject_grep_tree "operator123" "$WF" "sem operator123 no workflow"

# --- Q. Nenhum binário .apk no Git -------------------------------------------
info "Q. Conferindo que nenhum .apk foi adicionado ao Git..."
APK_TRACKED="$(git -C "$REPO_ROOT" ls-files '*.apk' 2>/dev/null)"
if [ -z "$APK_TRACKED" ]; then
  pass "nenhum binário .apk rastreado pelo Git"
else
  fail "binário(s) .apk rastreado(s) pelo Git: $APK_TRACKED"
fi
# E o .gitignore cobre o diretório de distribuição e o artefato renomeado.
need_grep_file 'lab-08-obsidianpay/android-app/dist/' "$REPO_ROOT/.gitignore" ".gitignore ignora a pasta dist do app"
need_grep_file 'ObsidianPay-Lab08-*.apk' "$REPO_ROOT/.gitignore" ".gitignore ignora os APKs renomeados"

# --- R. Sem caminhos absolutos de máquina ------------------------------------
info "R. Conferindo ausência de caminhos absolutos específicos de máquina..."
for f in "$WF" "$PKG"; do
  reject_grep_re_file '/home/[a-z]' "$f" "sem /home/<user> hardcoded em $(basename "$f")"
  reject_grep_re_file '/Users/'     "$f" "sem /Users/ hardcoded em $(basename "$f")"
  reject_grep_re_file 'C:\\\\'      "$f" "sem caminho C:\\ hardcoded em $(basename "$f")"
done

# --- S. Nenhum lab 1..7 alterado (conteúdo) ----------------------------------
info "S. Verificando que labs 1..7 não foram alterados (conteúdo)..."
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

# --- T. bash -n nos shell scripts --------------------------------------------
info "T. Conferindo sintaxe (bash -n) dos scripts criados/modificados..."
for s in "$PKG" "scripts/validate-phase22.sh"; do
  if bash -n "$s" 2>/dev/null; then pass "bash -n OK: $s"; else fail "bash -n FALHOU: $s"; fi
done

# --- U. Regressões (Fases 18, 19, 20) ----------------------------------------
info "U. Executando regressões (validate-phase18/19/20)..."
for n in 18 19 20; do
  if [ -f "scripts/validate-phase$n.sh" ]; then
    if bash "scripts/validate-phase$n.sh"; then pass "validate-phase$n.sh passou"; else fail "validate-phase$n.sh falhou"; fi
  else
    fail "scripts/validate-phase$n.sh ausente"
  fi
done

# --- V. git diff --check -----------------------------------------------------
info "V. Conferindo git diff --check..."
if git -C "$REPO_ROOT" diff --check >/dev/null 2>&1; then
  pass "git diff --check sem erros de whitespace/conflito"
else
  fail "git diff --check reportou problemas"
fi

# --- Resultado ---------------------------------------------------------------
echo ""
if [ "$FAILED" -eq 0 ]; then
  printf '\033[32m[OK] Fase 22A validada (pipeline de APK + download + docs).\033[0m\n'
  exit 0
else
  printf '\033[31m[FAIL] Fase 22A: uma ou mais verificações falharam.\033[0m\n' >&2
  exit 1
fi
