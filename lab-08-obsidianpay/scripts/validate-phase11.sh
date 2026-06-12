#!/usr/bin/env bash
#
# validate-phase11.sh — valida a Fase 11 do Lab 08 (Network Security /
# Certificate Pinning Scaffold: NetworkSecurityProfile, PinningPolicy,
# ApiHostOverrideScreen, base URL override, backend network-profile,
# cleartext local, modos didáticos de pinning).
#
# - Verifica scripts de fases anteriores (phase1..phase10).
# - Verifica os novos arquivos do pacote network/ e ApiHostOverrideScreen.
# - Grepa o código por strings-chave da fase.
# - Confere o backend (network-profile endpoint, config fields).
# - Confere manifest/network_security_config.
# - Confere MainActivity e HomeScreen (ApiHostOverrideScreen / API Host).
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
NET_PKG="$SRC/network"
API_HOST_SCREEN="$SRC/ui/ApiHostOverrideScreen.kt"
HOME_SCREEN="$SRC/ui/HomeScreen.kt"
MAIN_ACTIVITY="$SRC/MainActivity.kt"
SERVER="api/src/server.js"
DATA="api/src/data.js"
MANIFEST="$APP/app/src/main/AndroidManifest.xml"
NET_SEC_CONFIG="$APP/app/src/main/res/xml/network_security_config.xml"

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
for n in 1 2 3 4 5 6 7 8 9 10; do need_file "scripts/validate-phase$n.sh"; done

# --- Arquivos obrigatórios ---------------------------------------------------
info "Conferindo arquivos obrigatórios da Fase 11..."
need_file "$NET_PKG/NetworkSecurityProfile.kt"
need_file "$NET_PKG/PinningPolicy.kt"
need_file "$API_HOST_SCREEN"

# --- Strings-chave no código -------------------------------------------------
info "Conferindo strings-chave no pacote network/..."

need_grep_tree "NetworkSecurityProfile"         "$SRC"          "NetworkSecurityProfile referenciado"
need_grep_tree "PinningPolicy"                  "$SRC"          "PinningPolicy referenciado"
need_grep_tree "ApiHostOverrideScreen"          "$SRC"          "ApiHostOverrideScreen referenciado"
need_grep_file "API Host"                       "$API_HOST_SCREEN" "título 'API Host' em ApiHostOverrideScreen"

need_grep_file "DEFAULT_EMULATOR_BASE_URL"      "$NET_PKG/NetworkSecurityProfile.kt" "DEFAULT_EMULATOR_BASE_URL"
need_grep_file "DEFAULT_LOCALHOST_BASE_URL"     "$NET_PKG/NetworkSecurityProfile.kt" "DEFAULT_LOCALHOST_BASE_URL"
need_grep_file "SAMPLE_PHONE_BASE_URL"          "$NET_PKG/NetworkSecurityProfile.kt" "SAMPLE_PHONE_BASE_URL"
need_grep_file "cleartext-local"                "$NET_PKG/NetworkSecurityProfile.kt" "PROFILE_CLEAR_TEXT_LOCAL"
need_grep_file "burp-proxy-ready"               "$NET_PKG/NetworkSecurityProfile.kt" "PROFILE_BURP_PROXY_READY"
need_grep_file "pinning-scaffold"               "$NET_PKG/NetworkSecurityProfile.kt" "PROFILE_PINNING_SCAFFOLD"
need_grep_file "trust-user-ca"                  "$NET_PKG/NetworkSecurityProfile.kt" "hint trust-user-ca"
need_grep_file "okhttp-certificate-pinner-hook" "$NET_PKG/NetworkSecurityProfile.kt" "hint okhttp-certificate-pinner-hook"
need_grep_file "network-config-cleartext-override" "$NET_PKG/NetworkSecurityProfile.kt" "hint network-config-cleartext-override"

need_grep_file "CertificatePinner"              "$NET_PKG/PinningPolicy.kt"           "string CertificatePinner em PinningPolicy"
need_grep_file "okhttp-certificate-pinner-hook" "$NET_PKG/PinningPolicy.kt"           "okhttp-certificate-pinner-hook em PinningPolicy"
need_grep_file "trust-manager-hook"             "$NET_PKG/PinningPolicy.kt"           "trust-manager-hook em PinningPolicy"
need_grep_file "user-ca-not-trusted-by-default" "$NET_PKG/PinningPolicy.kt"           "user-ca-not-trusted-by-default em PinningPolicy"
need_grep_file "report-only"                    "$NET_PKG/PinningPolicy.kt"           "report-only em PinningPolicy"
need_grep_file "PINNING_MODE_DISABLED"          "$NET_PKG/PinningPolicy.kt"           "PINNING_MODE_DISABLED"
need_grep_file "PINNING_MODE_REPORT_ONLY"       "$NET_PKG/PinningPolicy.kt"           "PINNING_MODE_REPORT_ONLY"
need_grep_file "PINNING_MODE_STRICT_SCAFFOLD"   "$NET_PKG/PinningPolicy.kt"           "PINNING_MODE_STRICT_SCAFFOLD"
need_grep_file "SAMPLE_PIN_SHA256"              "$NET_PKG/PinningPolicy.kt"           "SAMPLE_PIN_SHA256"

info "Conferindo ApiClient e eventos..."
need_grep_tree "setBaseUrlForSession"           "$SRC/api"      "setBaseUrlForSession em ApiClient"
need_grep_tree "getNetworkProfile"              "$SRC/api"      "getNetworkProfile em ApiClient"
need_grep_tree "CertificatePinner"              "$SRC/api"      "CertificatePinner scaffold em ApiClient"
need_grep_tree "PinningPolicy"                  "$SRC/api"      "PinningPolicy referenciado em ApiClient"
need_grep_tree "okhttp-certificate-pinner-hook" "$SRC/api"      "okhttp-certificate-pinner-hook em ApiClient"
need_grep_tree "buildPinningBypassHints"        "$SRC/api"      "buildPinningBypassHints referenciado"

need_grep_file "api_base_url_override_saved"    "$API_HOST_SCREEN" "evento api_base_url_override_saved"
need_grep_file "api_base_url_override_cleared"  "$API_HOST_SCREEN" "evento api_base_url_override_cleared"
need_grep_file "network_profile_fetched"        "$API_HOST_SCREEN" "evento network_profile_fetched"
need_grep_file "pinning_mode_observed"          "$API_HOST_SCREEN" "evento pinning_mode_observed"

# --- Backend -----------------------------------------------------------------
info "Conferindo backend..."
need_grep_file "/api/mobile/internal/network-profile" "$SERVER" "endpoint network-profile em server.js"
need_grep_file "enableNetworkProfile"           "$DATA"          "enableNetworkProfile em data.js"
need_grep_file "networkProfilePath"             "$DATA"          "networkProfilePath em data.js"
need_grep_file "pinningMode"                    "$DATA"          "pinningMode em data.js"
need_grep_file "cleartextAllowed"               "$DATA"          "cleartextAllowed em data.js"
need_grep_file "defaultEmulatorBaseUrl"         "$DATA"          "defaultEmulatorBaseUrl em data.js"
need_grep_file "phoneLanExample"                "$DATA"          "phoneLanExample em data.js"
need_grep_file "configure the app base URL"     "$DATA"          "nextStepHint note em data.js"
need_grep_file "enableNetworkProfile"           "$SERVER"        "enableNetworkProfile usado em server.js"
need_grep_file "networkProfileConfig"           "$SERVER"        "networkProfileConfig importado em server.js"
need_grep_file "burp-proxy-ready"               "$SERVER"        "burp-proxy-ready em server.js"
need_grep_file "report-only"                    "$SERVER"        "report-only em server.js"
need_grep_file "trust-user-ca"                  "$SERVER"        "trust-user-ca em server.js"
need_grep_file "okhttp-certificate-pinner-hook" "$SERVER"        "okhttp-certificate-pinner-hook em server.js"
need_grep_file "network-config-cleartext-override" "$SERVER"     "network-config-cleartext-override em server.js"
need_grep_file "configure the app base URL"     "$SERVER"        "nextStepHint em server.js"

# --- Manifest / network security config -------------------------------------
info "Conferindo AndroidManifest e network_security_config..."
need_grep_file "usesCleartextTraffic"           "$MANIFEST"      "usesCleartextTraffic em AndroidManifest"
need_grep_file "network_security_config"        "$MANIFEST"      "network_security_config referenciado em AndroidManifest"
need_grep_file "10.0.2.2"                       "$NET_SEC_CONFIG" "10.0.2.2 em network_security_config"
need_grep_file "127.0.0.1"                      "$NET_SEC_CONFIG" "127.0.0.1 em network_security_config"
need_grep_file "localhost"                      "$NET_SEC_CONFIG" "localhost em network_security_config"

# --- MainActivity e HomeScreen -----------------------------------------------
info "Conferindo MainActivity e HomeScreen..."
need_grep_tree "ApiHostOverrideScreen"          "$MAIN_ACTIVITY" "ApiHostOverrideScreen em MainActivity"
need_grep_tree "ApiHost"                        "$MAIN_ACTIVITY" "Screen.ApiHost em MainActivity"
need_grep_file "API Host"                       "$HOME_SCREEN"   "botão API Host em HomeScreen"

# --- Guards de docs públicos -------------------------------------------------
info "Verificando docs públicos (sem FLAG{ e sem credenciais)..."
for doc in README.md STUDENT-GUIDE.md android-app/README.md; do
  reject_grep_tree "FLAG{"     "$doc" "sem FLAG{ em $doc"
  reject_grep_tree "analyst123" "$doc" "sem analyst123 em $doc"
  reject_grep_tree "operator123" "$doc" "sem operator123 em $doc"
done

# --- Guards das novas classes ------------------------------------------------
info "Verificando novas classes (sem FLAG{ e sem credenciais)..."
reject_grep_tree "FLAG{"      "$NET_PKG"        "sem FLAG{ em network/"
reject_grep_tree "analyst123" "$NET_PKG"        "sem analyst123 em network/"
reject_grep_tree "operator123" "$NET_PKG"       "sem operator123 em network/"
reject_grep_tree "FLAG{"      "$API_HOST_SCREEN" "sem FLAG{ em ApiHostOverrideScreen"
reject_grep_tree "analyst123" "$API_HOST_SCREEN" "sem analyst123 em ApiHostOverrideScreen"
reject_grep_tree "operator123" "$API_HOST_SCREEN" "sem operator123 em ApiHostOverrideScreen"

# --- Guards de typos de fases anteriores ------------------------------------
info "Verificando ausência de typos conhecidos..."
reject_grep_re_tree 'fun getSessionSummar\(\)'        "$SRC"  "sem typo getSessionSummar()"
reject_grep_re_tree '@JavascriptInterfac$'            "$SRC"  "sem typo @JavascriptInterfac"
reject_grep_re_tree 'webVieClient'                    "$SRC"  "sem typo webVieClient"
reject_grep_re_tree 'object LegacyRequestSigne \{'    "$SRC"  "sem typo LegacyRequestSigne {"
reject_grep_re_tree 'WeakCryptosha1Hex'               "$SRC"  "sem typo WeakCryptosha1Hex"
reject_grep_re_tree 'WeakCryptomd5Hex'                "$SRC"  "sem typo WeakCryptomd5Hex"

# --- Verificar que labs 1-7 não foram alterados ------------------------------
info "Verificando que labs 1..7 não foram alterados nesta branch..."
# Compare branch commits vs. main to check only Phase 11 changes (not pre-existing uncommitted mods).
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
  printf '\033[32m[OK] Fase 11 validada com sucesso.\033[0m\n'
  exit 0
else
  printf '\033[31m[FAIL] Fase 11: uma ou mais verificações falharam.\033[0m\n' >&2
  exit 1
fi
