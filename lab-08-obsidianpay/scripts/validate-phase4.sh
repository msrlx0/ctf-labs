#!/usr/bin/env bash
#
# validate-phase4.sh — valida a Fase 4 do Lab 08 (armazenamento local inseguro).
#
# - Verifica scripts de fases anteriores.
# - Verifica arquivos de storage (InsecureSessionStore, ObsidianLocalDb,
#   LocalCacheManager, LocalStateScreen).
# - Grepa strings-chave (SharedPreferences, SQLiteOpenHelper, tabelas, filesDir,
#   cacheDir, getExternalFilesDir, obsidianpay_local.db) e referências de UI.
# - Guards de docs públicos (sem FLAG{ / sem credenciais internas).
# - Confere que nenhum lab 1..7 foi alterado.
# - Opcional: RUN_BACKEND_TESTS=1 roda phase1/2; build best-effort se houver SDK.
# - Sai com exit 1 se faltar arquivo/strings obrigatórias.
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$LAB_DIR"
APP="android-app"
SRC="$APP/app/src/main/java/com/obsidianpay/mobile"
STORAGE="$SRC/storage"
MAIN="$APP/app/src/main"

pass() { printf '  \033[32m[PASS]\033[0m %s\n' "$1"; }
warn() { printf '  \033[33m[WARN]\033[0m %s\n' "$1"; }
info() { printf '\033[36m[*]\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m[FAIL]\033[0m %s\n' "$1" >&2; FAILED=1; }

FAILED=0

need_file() { if [ -f "$1" ]; then pass "arquivo: $1"; else fail "ausente: $1"; fi; }

need_grep() {
  # $1 = pattern (ERE), $2 = file, $3 = descrição
  if [ -f "$2" ] && grep -Eq "$1" "$2"; then pass "$3"; else fail "$3 (esperado /$1/ em $2)"; fi
}

need_grep_tree() {
  # $1 = pattern (fixed), $2 = dir, $3 = descrição
  if grep -rqF "$1" "$2" 2>/dev/null; then pass "$3"; else fail "$3 (esperado '$1' em $2)"; fi
}

info "Diretório do lab: $LAB_DIR"

# --- Scripts anteriores ------------------------------------------------------
info "Conferindo scripts de fases anteriores..."
need_file "scripts/validate-phase1.sh"
need_file "scripts/validate-phase2.sh"
need_file "scripts/validate-phase3.sh"

# --- Arquivos de storage -----------------------------------------------------
info "Conferindo arquivos de storage local..."
need_file "$STORAGE/InsecureSessionStore.kt"
need_file "$STORAGE/ObsidianLocalDb.kt"
need_file "$STORAGE/LocalCacheManager.kt"
need_file "$SRC/ui/LocalStateScreen.kt"

# --- Conteúdos-chave ---------------------------------------------------------
info "Conferindo conteúdos-chave de storage..."
need_grep "SharedPreferences" "$STORAGE/InsecureSessionStore.kt" "InsecureSessionStore usa SharedPreferences"
need_grep "SQLiteOpenHelper" "$STORAGE/ObsidianLocalDb.kt" "ObsidianLocalDb usa SQLiteOpenHelper"
need_grep "obsidianpay_local\.db" "$STORAGE/ObsidianLocalDb.kt" "DB obsidianpay_local.db definido"
need_grep "cached_receipts" "$STORAGE/ObsidianLocalDb.kt" "tabela cached_receipts"
need_grep "cached_cards" "$STORAGE/ObsidianLocalDb.kt" "tabela cached_cards"
need_grep "debug_events" "$STORAGE/ObsidianLocalDb.kt" "tabela debug_events"
need_grep "filesDir" "$STORAGE/LocalCacheManager.kt" "LocalCacheManager usa filesDir"
need_grep "cacheDir" "$STORAGE/LocalCacheManager.kt" "LocalCacheManager usa cacheDir"
need_grep "getExternalFilesDir" "$STORAGE/LocalCacheManager.kt" "LocalCacheManager usa getExternalFilesDir (external app-specific)"

# --- Referências de UI -------------------------------------------------------
info "Conferindo referências de UI ao estado local..."
need_grep_tree "Local State" "$MAIN" "UI referencia 'Local State'"
need_grep_tree "LocalStateScreen" "$MAIN" "UI referencia LocalStateScreen"
need_grep_tree "Cached Receipts" "$MAIN" "UI referencia 'Cached Receipts'"
need_grep_tree "Cached Cards" "$MAIN" "UI referencia 'Cached Cards'"
need_grep_tree "Local Artifacts" "$MAIN" "UI referencia 'Local Artifacts'"

# --- Guards de docs públicos -------------------------------------------------
info "Conferindo docs públicos..."
if grep -RIl "FLAG{" README.md STUDENT-GUIDE.md docs/ android-app/README.md >/dev/null 2>&1; then
  fail "FLAG{ encontrado em documento público"
else
  pass "sem FLAG{ em docs públicos"
fi
if grep -RInE "analyst123|operator123" README.md STUDENT-GUIDE.md android-app/README.md >/dev/null 2>&1; then
  fail "credencial interna em documento público"
else
  pass "sem credenciais internas em README/STUDENT-GUIDE/app README"
fi

# --- Labs anteriores intocados ----------------------------------------------
info "Conferindo que nenhum lab 1..7 foi alterado..."
if command -v git >/dev/null 2>&1 && git -C "$LAB_DIR/.." rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  if git -C "$LAB_DIR/.." status --porcelain | grep -E "lab-0[1-7]" >/dev/null 2>&1; then
    fail "há alterações em labs 01..07"
  else
    pass "nenhum lab 01..07 alterado"
  fi
else
  warn "git indisponível: pulei a checagem de labs anteriores."
fi

# --- Backend opcional --------------------------------------------------------
if [ "${RUN_BACKEND_TESTS:-0}" = "1" ]; then
  info "RUN_BACKEND_TESTS=1: executando validações de backend..."
  bash scripts/validate-phase1.sh || fail "validate-phase1.sh falhou"
  bash scripts/validate-phase2.sh || fail "validate-phase2.sh falhou"
else
  warn "Pulando testes de backend (defina RUN_BACKEND_TESTS=1 para executá-los)."
fi

# --- Build best-effort -------------------------------------------------------
info "Tentando validar o build Android (best-effort)..."
HAS_SDK=0
if [ -n "${ANDROID_HOME:-}" ] || [ -n "${ANDROID_SDK_ROOT:-}" ] || [ -f "$APP/local.properties" ]; then HAS_SDK=1; fi
if [ -x "$APP/gradlew" ] && [ -f "$APP/gradle/wrapper/gradle-wrapper.jar" ] && [ "$HAS_SDK" = "1" ]; then
  info "Gradle wrapper + SDK detectados: rodando assembleDebug..."
  ( cd "$APP" && ./gradlew --console=plain :app:assembleDebug ) \
    && pass "assembleDebug concluído" || fail "assembleDebug falhou"
else
  warn "Build do APK não executado (Android SDK não detectado). Use Android Studio."
fi

echo
if [ "$FAILED" = "0" ]; then
  printf '\033[32m==> Fase 4 validada com sucesso (estrutura de storage).\033[0m\n'
  exit 0
else
  printf '\033[31m==> Fase 4: há falhas obrigatórias acima.\033[0m\n'
  exit 1
fi
