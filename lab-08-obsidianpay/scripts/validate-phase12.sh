#!/usr/bin/env bash
#
# validate-phase12.sh — valida a Fase 12 do Lab 08 (App Integrity /
# NativeGate / TamperCheck scaffold: NativeGate.kt, TamperCheck.kt,
# IntegrityScreen.kt, AppIntegrity backend endpoint, storage/events/constants).
#
# - Verifica scripts de fases anteriores (phase1..phase11).
# - Verifica os novos arquivos do pacote integrity/ e IntegrityScreen.
# - Grepa o código por strings-chave da fase.
# - Confere o backend (app-integrity endpoint, config fields).
# - Confere MainActivity e HomeScreen (IntegrityScreen / App Integrity).
# - Guards de docs públicos (sem FLAG{ / sem credenciais internas).
# - Guards de typos de fases anteriores.
# - Confere que nenhum lab 1..7 foi alterado.
# - Build best-effort se houver Gradle + Android SDK.
# - Sai com exit 1 se arquivos obrigatórios ou strings-chave faltarem.
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$LAB_DIR"

APP="android-app"
SRC="$APP/app/src/main/java/com/obsidianpay/mobile"
INTEGRITY_PKG="$SRC/integrity"
INTEGRITY_SCREEN="$SRC/ui/IntegrityScreen.kt"
HOME_SCREEN="$SRC/ui/HomeScreen.kt"
MAIN_ACTIVITY="$SRC/MainActivity.kt"
LOCAL_STATE_SCREEN="$SRC/ui/LocalStateScreen.kt"
API_CLIENT="$SRC/api/ApiClient.kt"
CONSTANTS="$SRC/util/Constants.kt"
SESSION_STORE="$SRC/storage/InsecureSessionStore.kt"
CACHE_MANAGER="$SRC/storage/LocalCacheManager.kt"
SERVER="api/src/server.js"
DATA="api/src/data.js"

pass() { printf '  \033[32m[PASS]\033[0m %s\n' "$1"; }
warn() { printf '  \033[33m[WARN]\033[0m %s\n' "$1"; }
info() { printf '\033[36m[*]\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m[FAIL]\033[0m %s\n' "$1" >&2; FAILED=1; }

FAILED=0

need_file() { if [ -f "$1" ]; then pass "arquivo: $1"; else fail "ausente: $1"; fi; }

need_grep_file() {
  if [ -f "$2" ] && grep -qF "$1" "$2"; then pass "$3"; else fail "$3 (esperado '$1' em $2)"; fi
}

need_grep_tree() {
  if grep -rqF "$1" "$2" 2>/dev/null; then pass "$3"; else fail "$3 (esperado '$1' em $2)"; fi
}

reject_grep_re_tree() {
  if grep -rEq "$1" "$2" 2>/dev/null; then
    fail "$3 (padrão /$1/ encontrado em $2)"
  else
    pass "$3"
  fi
}

reject_grep_tree() {
  if grep -rqF "$1" "$2" 2>/dev/null; then
    fail "$3 (string '$1' encontrada em $2)"
  else
    pass "$3"
  fi
}

info "Diretório do lab: $LAB_DIR"

# --- Scripts anteriores ------------------------------------------------------
info "Conferindo scripts de fases anteriores..."
for n in 1 2 3 4 5 6 7 8 9 10 11; do need_file "scripts/validate-phase$n.sh"; done

# --- Arquivos obrigatórios ---------------------------------------------------
info "Conferindo arquivos obrigatórios da Fase 12..."
need_file "$INTEGRITY_PKG/NativeGate.kt"
need_file "$INTEGRITY_PKG/TamperCheck.kt"
need_file "$INTEGRITY_SCREEN"

# --- Strings-chave no código -------------------------------------------------
info "Conferindo strings-chave no pacote integrity/..."

need_grep_tree "NativeGate"                     "$SRC"                    "NativeGate referenciado"
need_grep_tree "TamperCheck"                    "$SRC"                    "TamperCheck referenciado"
need_grep_tree "IntegrityScreen"                "$SRC"                    "IntegrityScreen referenciado"
need_grep_tree "App Integrity"                  "$SRC"                    "string 'App Integrity' no código"

need_grep_file "obsidian_native_gate"           "$INTEGRITY_PKG/NativeGate.kt"  "string obsidian_native_gate"
need_grep_file "native-library-missing-fallback" "$INTEGRITY_PKG/NativeGate.kt" "string native-library-missing-fallback"
need_grep_file "jni-return-value-hook"          "$INTEGRITY_PKG/NativeGate.kt"  "string jni-return-value-hook"
need_grep_file "patch-native-gate-result"       "$INTEGRITY_PKG/NativeGate.kt"  "string patch-native-gate-result"
need_grep_file "strings-libobsidian-native"     "$INTEGRITY_PKG/NativeGate.kt"  "string strings-libobsidian-native"
need_grep_file "native-gate-kotlin-fallback"    "$INTEGRITY_PKG/NativeGate.kt"  "string native-gate-kotlin-fallback"

need_grep_file "debuggable-build"               "$INTEGRITY_PKG/TamperCheck.kt" "string debuggable-build"
need_grep_file "unknown-installer"              "$INTEGRITY_PKG/TamperCheck.kt" "string unknown-installer"
need_grep_file "signature-hash-observed"        "$INTEGRITY_PKG/TamperCheck.kt" "string signature-hash-observed"
need_grep_file "package-name-check"             "$INTEGRITY_PKG/TamperCheck.kt" "string package-name-check"
need_grep_file "tamper-score"                   "$INTEGRITY_PKG/TamperCheck.kt" "string tamper-score"
need_grep_file "patch-debuggable-check"         "$INTEGRITY_PKG/TamperCheck.kt" "string patch-debuggable-check"
need_grep_file "hook-package-manager"           "$INTEGRITY_PKG/TamperCheck.kt" "string hook-package-manager"
need_grep_file "repackage-signature-mismatch"   "$INTEGRITY_PKG/TamperCheck.kt" "string repackage-signature-mismatch"

info "Conferindo eventos e botões em IntegrityScreen..."
need_grep_file "integrity_check_started"        "$INTEGRITY_SCREEN"       "evento integrity_check_started"
need_grep_file "tamper_check_completed"         "$INTEGRITY_SCREEN"       "evento tamper_check_completed"
need_grep_file "native_gate_checked"            "$INTEGRITY_SCREEN"       "evento native_gate_checked"
need_grep_file "tamper_score_calculated"        "$INTEGRITY_SCREEN"       "evento tamper_score_calculated"
need_grep_file "native_gate_hint_viewed"        "$INTEGRITY_SCREEN"       "evento native_gate_hint_viewed"
need_grep_file "integrity_report_sent"          "$INTEGRITY_SCREEN"       "evento integrity_report_sent"
need_grep_file "integrity_report_cached"        "$INTEGRITY_SCREEN"       "evento integrity_report_cached"
need_grep_file "integrity_state_cleared"        "$INTEGRITY_SCREEN"       "evento integrity_state_cleared"
need_grep_file "Run Integrity Check"            "$INTEGRITY_SCREEN"       "botão Run Integrity Check"
need_grep_file "Show Native Gate"               "$INTEGRITY_SCREEN"       "botão Show Native Gate"
need_grep_file "Send Integrity Report"          "$INTEGRITY_SCREEN"       "botão Send Integrity Report"
need_grep_file "Clear Integrity State"          "$INTEGRITY_SCREEN"       "botão Clear Integrity State"
need_grep_file 'cache.addEvent("integrity_check_started", "screen")' "$INTEGRITY_SCREEN" 'addEvent integrity_check_started com aspas corretas'

info "Conferindo ApiClient, Constants e storage..."
need_grep_file "APP_INTEGRITY_PATH"             "$CONSTANTS"              "APP_INTEGRITY_PATH em Constants"
need_grep_file "KEY_LAST_APP_INTEGRITY_REPORT"  "$CONSTANTS"              "KEY_LAST_APP_INTEGRITY_REPORT em Constants"
need_grep_file "KEY_LAST_APP_INTEGRITY_RESPONSE" "$CONSTANTS"             "KEY_LAST_APP_INTEGRITY_RESPONSE em Constants"
need_grep_file "KEY_LAST_NATIVE_GATE_STATUS"    "$CONSTANTS"              "KEY_LAST_NATIVE_GATE_STATUS em Constants"
need_grep_file "KEY_LAST_TAMPER_SCORE"          "$CONSTANTS"              "KEY_LAST_TAMPER_SCORE em Constants"
need_grep_file "KEY_LAST_SIGNATURE_HASH_PREVIEW" "$CONSTANTS"             "KEY_LAST_SIGNATURE_HASH_PREVIEW em Constants"

need_grep_file "sendAppIntegrityReport"         "$API_CLIENT"             "sendAppIntegrityReport em ApiClient"
need_grep_file "saveLastAppIntegrityReport"     "$SESSION_STORE"          "saveLastAppIntegrityReport em InsecureSessionStore"
need_grep_file "getLastAppIntegrityReport"      "$SESSION_STORE"          "getLastAppIntegrityReport em InsecureSessionStore"
need_grep_file "saveLastAppIntegrityResponse"   "$SESSION_STORE"          "saveLastAppIntegrityResponse em InsecureSessionStore"
need_grep_file "saveLastNativeGateStatus"       "$SESSION_STORE"          "saveLastNativeGateStatus em InsecureSessionStore"
need_grep_file "saveLastTamperScore"            "$SESSION_STORE"          "saveLastTamperScore em InsecureSessionStore"
need_grep_file "saveLastSignatureHashPreview"   "$SESSION_STORE"          "saveLastSignatureHashPreview em InsecureSessionStore"
need_grep_file "clearIntegrityState"            "$SESSION_STORE"          "clearIntegrityState em InsecureSessionStore"

need_grep_file "saveAppIntegrityReport"         "$CACHE_MANAGER"          "saveAppIntegrityReport em LocalCacheManager"
need_grep_file "saveAppIntegrityResponse"       "$CACHE_MANAGER"          "saveAppIntegrityResponse em LocalCacheManager"
need_grep_file 'fun getLastNativeGateStatus(): String?' "$SESSION_STORE"  "getLastNativeGateStatus correto em InsecureSessionStore"

info "Conferindo LocalStateScreen..."
need_grep_file "KEY_LAST_NATIVE_GATE_STATUS"    "$LOCAL_STATE_SCREEN"     "KEY_LAST_NATIVE_GATE_STATUS em LocalStateScreen"
need_grep_file "KEY_LAST_TAMPER_SCORE"          "$LOCAL_STATE_SCREEN"     "KEY_LAST_TAMPER_SCORE em LocalStateScreen"
need_grep_file "KEY_LAST_APP_INTEGRITY_REPORT"  "$LOCAL_STATE_SCREEN"     "KEY_LAST_APP_INTEGRITY_REPORT em LocalStateScreen"
need_grep_file "integrity_check_started"        "$LOCAL_STATE_SCREEN"     "evento integrity na LocalStateScreen"
need_grep_file 'App Integrity / NativeGate / TamperCheck' "$LOCAL_STATE_SCREEN" "seção App Integrity / NativeGate / TamperCheck em LocalStateScreen"

# --- Backend -----------------------------------------------------------------
info "Conferindo backend..."
need_grep_file "/api/mobile/internal/app-integrity" "$SERVER"             "endpoint app-integrity em server.js"
need_grep_file "enableAppIntegrity"             "$DATA"                   "enableAppIntegrity em data.js"
need_grep_file "appIntegrityPath"               "$DATA"                   "appIntegrityPath em data.js"
need_grep_file "integrityPolicy"                "$DATA"                   "integrityPolicy em data.js"
need_grep_file "nativeGatePolicy"               "$DATA"                   "nativeGatePolicy em data.js"
need_grep_file "report-only"                    "$DATA"                   "report-only em data.js"
need_grep_file "fallback-allowed"               "$DATA"                   "fallback-allowed em data.js"
need_grep_file "client-side integrity checks are patchable" "$DATA"       "nextStepHint note em data.js"
need_grep_file "appIntegrityConfig"             "$SERVER"                 "appIntegrityConfig importado em server.js"
need_grep_file "enableAppIntegrity"             "$SERVER"                 "enableAppIntegrity usado em server.js"
need_grep_file "report-only"                    "$SERVER"                 "report-only em server.js"
need_grep_file "fallback-allowed"               "$SERVER"                 "fallback-allowed em server.js"
need_grep_file "client-asserted-integrity"      "$SERVER"                 "client-asserted-integrity em server.js"
need_grep_file "client-side integrity checks are patchable" "$SERVER"     "nextStepHint em server.js"

info "Conferindo referências de /api/mobile/config (data.js)..."
need_grep_file "enableAppIntegrity"             "$DATA"                   "enableAppIntegrity em buildMobileConfig"

# --- MainActivity e HomeScreen -----------------------------------------------
info "Conferindo MainActivity e HomeScreen..."
need_grep_file "IntegrityScreen"                "$MAIN_ACTIVITY"          "IntegrityScreen em MainActivity"
need_grep_file "Integrity"                      "$MAIN_ACTIVITY"          "Screen.Integrity em MainActivity"
need_grep_file "App Integrity"                  "$HOME_SCREEN"            "botão App Integrity em HomeScreen"

# --- Guards de docs públicos -------------------------------------------------
info "Verificando docs públicos (sem FLAG{ e sem credenciais)..."
for doc in README.md STUDENT-GUIDE.md android-app/README.md; do
  reject_grep_tree "FLAG{"      "$doc"  "sem FLAG{ em $doc"
  reject_grep_tree "analyst123" "$doc"  "sem analyst123 em $doc"
  reject_grep_tree "operator123" "$doc" "sem operator123 em $doc"
done

# --- Guards das novas classes ------------------------------------------------
info "Verificando novas classes (sem FLAG{ e sem credenciais)..."
reject_grep_tree "FLAG{"      "$INTEGRITY_PKG"      "sem FLAG{ em integrity/"
reject_grep_tree "analyst123" "$INTEGRITY_PKG"      "sem analyst123 em integrity/"
reject_grep_tree "operator123" "$INTEGRITY_PKG"     "sem operator123 em integrity/"
reject_grep_tree "FLAG{"      "$INTEGRITY_SCREEN"   "sem FLAG{ em IntegrityScreen"
reject_grep_tree "analyst123" "$INTEGRITY_SCREEN"   "sem analyst123 em IntegrityScreen"
reject_grep_tree "operator123" "$INTEGRITY_SCREEN"  "sem operator123 em IntegrityScreen"

# --- Guards de typos de fases anteriores ------------------------------------
info "Verificando ausência de typos conhecidos..."
reject_grep_re_tree 'fun getSessionSummar\(\)'        "$SRC"  "sem typo getSessionSummar()"
reject_grep_re_tree '@JavascriptInterfac$'            "$SRC"  "sem typo @JavascriptInterfac"
reject_grep_re_tree 'webVieClient'                    "$SRC"  "sem typo webVieClient"
reject_grep_re_tree 'object LegacyRequestSigne \{'    "$SRC"  "sem typo LegacyRequestSigne {"
reject_grep_re_tree 'WeakCryptosha1Hex'               "$SRC"  "sem typo WeakCryptosha1Hex"
reject_grep_re_tree 'WeakCryptomd5Hex'                "$SRC"  "sem typo WeakCryptomd5Hex"
reject_grep_re_tree '->[[:space:]]+patch-risk-engine-result' "$SRC" "sem typo patch-risk-engine-result sem aspas"
reject_grep_tree 'bypassHintId =BiometricGate'        "$SRC"  "sem typo bypassHintId =BiometricGate"
reject_grep_tree 'ObsidianScaffold(title = API Host"' "$SRC"  "sem typo title sem aspas de abertura"
reject_grep_tree 'apiClientsetBaseUrlForSession'       "$SRC"  "sem typo apiClientsetBaseUrlForSession"
reject_grep_re_tree 'network-config-cleartext-overrid\b' "$SRC" "sem typo network-config-cleartext-overrid"
# Fase 12 — typos específicos
reject_grep_tree 'cache.addEvent(integrity_check_started"'  "$SRC"  "sem typo addEvent sem aspas de abertura"
reject_grep_tree 'getLastNativeGatStatus'                   "$SRC"  "sem typo getLastNativeGatStatus (falta e em Gate)"
reject_grep_re_tree 'App Integrit[^y]'                      "$SRC"  "sem typo App Integrit sem y"

# --- Verificar que labs 1-7 não foram alterados ------------------------------
info "Verificando que labs 1..7 não foram alterados nesta branch..."
REPO_ROOT="$(git -C "$LAB_DIR" rev-parse --show-toplevel 2>/dev/null || echo "$LAB_DIR/..")"
BRANCH_CHANGED=$(git -C "$REPO_ROOT" log origin/main..HEAD --name-only --format='' 2>/dev/null || \
                 git -C "$REPO_ROOT" diff --name-only HEAD~10..HEAD 2>/dev/null || true)
for lab_num in 1 2 3 4 5 6 7; do
  if echo "$BRANCH_CHANGED" | grep -q "lab-0${lab_num}-"; then
    fail "lab-0${lab_num} foi alterado nos commits desta branch"
  else
    pass "lab-0${lab_num} não alterado nos commits desta branch"
  fi
done

# --- Build best-effort -------------------------------------------------------
info "Tentando build (best-effort, não falha se SDK ausente)..."
GRADLE="$APP/gradlew"
if [ -x "$GRADLE" ]; then
  if command -v java >/dev/null 2>&1 && [ -n "${ANDROID_HOME:-}${ANDROID_SDK_ROOT:-}" ]; then
    info "Gradle + Java + Android SDK detectados. Rodando assembleDebug..."
    if (cd "$APP" && ./gradlew assembleDebug --no-daemon -q 2>&1); then
      pass "build Android bem-sucedido"
    else
      warn "build Android falhou (código não-zero) — verifique o ambiente"
    fi
  else
    warn "Android SDK / ANDROID_HOME não configurado — pulando build (validação estrutural continua)"
  fi
else
  warn "gradlew não encontrado em $GRADLE — pulando build"
fi

# --- Resultado ---------------------------------------------------------------
echo ""
if [ "$FAILED" -eq 0 ]; then
  printf '\033[32m[OK] Fase 12 validada com sucesso.\033[0m\n'
  exit 0
else
  printf '\033[31m[FAIL] Fase 12: uma ou mais verificações falharam.\033[0m\n' >&2
  exit 1
fi
