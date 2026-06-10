#!/usr/bin/env bash
#
# validate-phase8.sh — valida a Fase 8 do Lab 08 (hardcoded secrets / weak crypto
# / weak request signing / device-trust / trilha de reverse engineering).
#
# - Verifica scripts de fases anteriores (phase1..phase7).
# - Verifica os arquivos novos do pacote security/ e a tela DeviceTrustScreen.
# - Grepa o código por constantes/métodos/headers/eventos da fase.
# - Confere o backend (device-trust, reverse-hint, assinatura fraca).
# - Reforça as checagens de typos de bridge/WebView (regex com limites).
# - Guards de docs públicos e das novas classes (sem FLAG{ / sem credenciais).
# - Confere que nenhum lab 1..7 foi alterado.
# - Build best-effort se houver Gradle + Android SDK.
# - Sai com exit 1 se faltar arquivo/strings obrigatórias.
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$LAB_DIR"
APP="android-app"
SRC="$APP/app/src/main/java/com/obsidianpay/mobile"
SECURITY="$SRC/security"
DEVICE_TRUST_SCREEN="$SRC/ui/DeviceTrustScreen.kt"
BRIDGE="$SRC/webview/ObsidianSupportBridge.kt"
WEBVIEW_SCREEN="$SRC/ui/WebViewSupportScreen.kt"
SERVER="api/src/server.js"
DATA="api/src/data.js"

pass() { printf '  \033[32m[PASS]\033[0m %s\n' "$1"; }
warn() { printf '  \033[33m[WARN]\033[0m %s\n' "$1"; }
info() { printf '\033[36m[*]\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m[FAIL]\033[0m %s\n' "$1" >&2; FAILED=1; }

FAILED=0

need_file() { if [ -f "$1" ]; then pass "arquivo: $1"; else fail "ausente: $1"; fi; }

need_grep_file() {
  # $1 = pattern (fixed string), $2 = file, $3 = descrição
  if [ -f "$2" ] && grep -qF "$1" "$2"; then pass "$3"; else fail "$3 (esperado '$1' em $2)"; fi
}

need_grep_tree() {
  # $1 = pattern (fixed), $2 = dir, $3 = descrição
  if grep -rqF "$1" "$2" 2>/dev/null; then pass "$3"; else fail "$3 (esperado '$1' em $2)"; fi
}

reject_grep_re_tree() {
  # $1 = pattern (ERE) que NÃO deve existir, $2 = dir, $3 = descrição.
  if grep -rEq "$1" "$2" 2>/dev/null; then
    fail "$3 (typo /$1/ encontrado em $2)"
  else
    pass "$3"
  fi
}

info "Diretório do lab: $LAB_DIR"

# --- Scripts anteriores ------------------------------------------------------
info "Conferindo scripts de fases anteriores..."
for n in 1 2 3 4 5 6 7; do need_file "scripts/validate-phase$n.sh"; done

# --- Arquivos novos ----------------------------------------------------------
info "Conferindo arquivos da Fase 8..."
need_file "$SECURITY/HardcodedSecrets.kt"
need_file "$SECURITY/WeakCrypto.kt"
need_file "$SECURITY/LegacyRequestSigner.kt"
need_file "$DEVICE_TRUST_SCREEN"

# --- HardcodedSecrets --------------------------------------------------------
info "Conferindo HardcodedSecrets..."
need_grep_tree "INTERNAL_CLIENT_PART" "$SECURITY" "constantes INTERNAL_CLIENT_PART"
need_grep_tree "LEGACY_SIGNING_SALT" "$SECURITY" "constantes LEGACY_SIGNING_SALT"
need_grep_tree "getInternalClientId" "$SECURITY" "método getInternalClientId"
need_grep_tree "getLegacySigningSalt" "$SECURITY" "método getLegacySigningSalt"
need_grep_tree "getEncodedOperatorHint" "$SECURITY" "método getEncodedOperatorHint"
need_grep_tree "getHiddenRoutes" "$SECURITY" "método getHiddenRoutes"

# --- WeakCrypto --------------------------------------------------------------
info "Conferindo WeakCrypto..."
need_grep_tree "base64Encode" "$SECURITY" "método base64Encode"
need_grep_tree "base64Decode" "$SECURITY" "método base64Decode"
need_grep_tree "weakXor" "$SECURITY" "método weakXor"
if grep -rqE "sha1Hex|md5Hex" "$SECURITY" 2>/dev/null; then
  pass "hash fraco (sha1Hex/md5Hex)"
else
  fail "hash fraco ausente (esperado sha1Hex ou md5Hex em $SECURITY)"
fi

# --- LegacyRequestSigner + headers -------------------------------------------
info "Conferindo LegacyRequestSigner e headers..."
need_grep_tree "X-Obsidian-Client" "$SRC" "header X-Obsidian-Client"
need_grep_tree "X-Obsidian-Device" "$SRC" "header X-Obsidian-Device"
need_grep_tree "X-Obsidian-Timestamp" "$SRC" "header X-Obsidian-Timestamp"
need_grep_tree "X-Obsidian-Signature" "$SRC" "header X-Obsidian-Signature"

# --- Typos de LegacyRequestSigner / WeakCrypto (quebrariam o build) -----------
info "Conferindo typos de LegacyRequestSigner / WeakCrypto..."
# Negativas: estes typos compilariam errado (símbolo inexistente) e devem falhar.
reject_grep_re_tree 'LegacyRequestSigne([^r]|$)' "$SRC" "sem typo 'LegacyRequestSigne' (esperado LegacyRequestSigner)"
reject_grep_re_tree 'WeakCryptosha1Hex' "$SRC" "sem typo 'WeakCryptosha1Hex' (esperado WeakCrypto.sha1Hex)"
reject_grep_re_tree 'WeakCryptomd5Hex' "$SRC" "sem typo 'WeakCryptomd5Hex' (esperado WeakCrypto.md5Hex)"
reject_grep_re_tree 'WeakCrypto[[:space:]]+sha1Hex' "$SRC" "sem 'WeakCrypto sha1Hex' sem ponto (esperado WeakCrypto.sha1Hex)"
reject_grep_re_tree 'WeakCrypto[[:space:]]+md5Hex' "$SRC" "sem 'WeakCrypto md5Hex' sem ponto (esperado WeakCrypto.md5Hex)"
# Positivas: o símbolo e a chamada qualificada corretos devem existir.
need_grep_tree "object LegacyRequestSigner" "$SRC" "declaração 'object LegacyRequestSigner'"
if grep -rqE "WeakCrypto\.(sha1Hex|md5Hex)" "$SRC" 2>/dev/null; then
  pass "chamada qualificada WeakCrypto.sha1Hex/md5Hex"
else
  fail "chamada qualificada ausente (esperado WeakCrypto.sha1Hex ou WeakCrypto.md5Hex em $SRC)"
fi
need_grep_file "import com.obsidianpay.mobile.security.LegacyRequestSigner" "$DEVICE_TRUST_SCREEN" \
  "DeviceTrustScreen importa LegacyRequestSigner"

# --- DeviceTrustScreen + eventos ---------------------------------------------
info "Conferindo DeviceTrustScreen e eventos..."
need_grep_tree "Device Trust" "$APP/app/src/main" "UI: Device Trust"
need_grep_tree "device_trust_check_started" "$SRC" "evento device_trust_check_started"
need_grep_tree "weak_signature_generated" "$SRC" "evento weak_signature_generated"
need_grep_tree "device_trust_response_cached" "$SRC" "evento device_trust_response_cached"
need_grep_tree "encoded_hint_decoded" "$SRC" "evento encoded_hint_decoded"

# --- Backend -----------------------------------------------------------------
info "Conferindo backend device-trust..."
need_grep_file "/api/mobile/internal/device-trust" "$SERVER" "backend tem rota device-trust"
need_grep_file "/api/mobile/internal/reverse-hint" "$SERVER" "backend tem rota reverse-hint"
need_grep_file "x-obsidian-signature" "$SERVER" "backend lê X-Obsidian-Signature"
need_grep_file "trusted-legacy" "$SERVER" "backend retorna trusted-legacy"
need_grep_file "legacy-attestation" "$SERVER" "backend retorna legacy-attestation"
if grep -qE "createHash\('sha1'\)|createHash\(\"sha1\"\)|createHash\('md5'\)" "$SERVER"; then
  pass "backend usa hash fraco (sha1/md5) para a assinatura"
else
  fail "backend não usa hash fraco esperado para a assinatura"
fi
need_grep_file "enableLegacyDeviceTrust" "$DATA" "config tem enableLegacyDeviceTrust"
need_grep_file "internalDeviceTrustPath" "$DATA" "config tem internalDeviceTrustPath"
need_grep_file "internalReverseHintPath" "$DATA" "config tem internalReverseHintPath"

# --- Reforço dos typos de bridge/WebView (Fase 6) ----------------------------
info "Reforçando checagem de typos de bridge/WebView..."
reject_grep_re_tree 'fun[[:space:]]+getSessionSummar\(' "$SRC" "sem 'fun getSessionSummar()' (typo)"
reject_grep_re_tree 'getSessionSummar([^y]|$)' "$SRC" "sem typo 'getSessionSummar' (esperado getSessionSummary)"
reject_grep_re_tree '@JavascriptInterfac([^e]|$)' "$SRC" "sem typo '@JavascriptInterfac' (esperado @JavascriptInterface)"
reject_grep_re_tree 'webVieClient' "$SRC" "sem typo 'webVieClient' (esperado webViewClient)"

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

# --- Guards nas novas classes ------------------------------------------------
info "Conferindo guards nas classes da Fase 8..."
for f in "$SECURITY/HardcodedSecrets.kt" "$SECURITY/WeakCrypto.kt" \
         "$SECURITY/LegacyRequestSigner.kt" "$DEVICE_TRUST_SCREEN"; do
  if grep -qF "FLAG{" "$f" 2>/dev/null; then fail "FLAG{ em $f"; else pass "sem FLAG{ em $(basename "$f")"; fi
  if grep -qE "analyst123|operator123" "$f" 2>/dev/null; then
    fail "credencial interna em $f"
  else
    pass "sem credencial interna em $(basename "$f")"
  fi
done

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
  info "RUN_BACKEND_TESTS=1: executando validações de backend anteriores..."
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
  printf '\033[32m==> Fase 8 validada com sucesso (estrutura).\033[0m\n'
  exit 0
else
  printf '\033[31m==> Fase 8: há falhas obrigatórias acima.\033[0m\n'
  exit 1
fi
