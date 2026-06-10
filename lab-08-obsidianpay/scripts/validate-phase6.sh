#!/usr/bin/env bash
#
# validate-phase6.sh — valida a Fase 6 do Lab 08 (WebView JavaScript bridge).
#
# - Verifica scripts de fases anteriores (phase1..phase5).
# - Verifica o novo arquivo da bridge (webview/ObsidianSupportBridge.kt).
# - Grepa o código por @JavascriptInterface, ObsidianBridge, addJavascriptInterface
#   e pelos métodos/eventos esperados da bridge.
# - Confere a WebView (webViewClient, JS/DOM on, addJavascriptInterface) e a
#   ausência do typo webVieClient.
# - Confere o portal de suporte no backend (server.js).
# - Guards de docs públicos (sem FLAG{ / sem credenciais internas).
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
WEBVIEW_SCREEN="$SRC/ui/WebViewSupportScreen.kt"
BRIDGE="$SRC/webview/ObsidianSupportBridge.kt"
SERVER="api/src/server.js"

pass() { printf '  \033[32m[PASS]\033[0m %s\n' "$1"; }
warn() { printf '  \033[33m[WARN]\033[0m %s\n' "$1"; }
info() { printf '\033[36m[*]\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m[FAIL]\033[0m %s\n' "$1" >&2; FAILED=1; }

FAILED=0

need_file() { if [ -f "$1" ]; then pass "arquivo: $1"; else fail "ausente: $1"; fi; }

need_grep_file() {
  # $1 = pattern (fixed string), $2 = file, $3 = descrição (opcional)
  local desc="${3:-contém '$1'}"
  if [ -f "$2" ] && grep -qF "$1" "$2"; then pass "$desc"; else fail "$desc (esperado '$1' em $2)"; fi
}

reject_grep_file() {
  # $1 = pattern (fixed string), $2 = file, $3 = descrição (opcional)
  local desc="${3:-sem '$1'}"
  if [ -f "$2" ] && grep -qF "$1" "$2"; then fail "$desc (encontrado '$1' em $2)"; else pass "$desc"; fi
}

need_grep_tree() {
  # $1 = pattern (fixed), $2 = dir, $3 = descrição (opcional)
  local desc="${3:-código contém '$1'}"
  if grep -rqF "$1" "$2" 2>/dev/null; then pass "$desc"; else fail "$desc (esperado '$1' em $2)"; fi
}

info "Diretório do lab: $LAB_DIR"

# --- Scripts anteriores ------------------------------------------------------
info "Conferindo scripts de fases anteriores..."
need_file "scripts/validate-phase1.sh"
need_file "scripts/validate-phase2.sh"
need_file "scripts/validate-phase3.sh"
need_file "scripts/validate-phase4.sh"
need_file "scripts/validate-phase5.sh"

# --- Arquivo novo da bridge --------------------------------------------------
info "Conferindo arquivos da Fase 6..."
need_file "$BRIDGE"
need_file "$WEBVIEW_SCREEN"

# --- Conteúdo-chave da bridge ------------------------------------------------
info "Conferindo a JavaScript bridge..."
need_grep_tree "@JavascriptInterface" "$SRC"
need_grep_tree "ObsidianBridge" "$SRC"
need_grep_tree "addJavascriptInterface" "$SRC"
for m in getSessionSummary getCachedProfile getCachedConfig getLastSupportSync \
         getLastTransferPreview getLocalArtifacts getBridgeInfo logBridgeEvent; do
  need_grep_tree "$m" "$SRC"
done
need_grep_tree "webview_bridge_called" "$SRC"
need_grep_tree "webview_bridge_attached" "$SRC"
need_grep_file "@JavascriptInterface" "$BRIDGE" "bridge usa @JavascriptInterface"

# --- WebView screen ----------------------------------------------------------
info "Conferindo WebViewSupportScreen..."
need_grep_file "webViewClient" "$WEBVIEW_SCREEN" "WebView configura webViewClient"
need_grep_file "settings.javaScriptEnabled = true" "$WEBVIEW_SCREEN" "WebView habilita JavaScript"
need_grep_file "settings.domStorageEnabled = true" "$WEBVIEW_SCREEN" "WebView habilita DOM storage"
need_grep_file "addJavascriptInterface" "$WEBVIEW_SCREEN" "WebView anexa a bridge"
need_grep_file "ObsidianBridge" "$WEBVIEW_SCREEN" "WebView usa nome ObsidianBridge"
reject_grep_file "webVieClient" "$WEBVIEW_SCREEN" "sem typo webVieClient na WebView"

# --- Backend support portal --------------------------------------------------
info "Conferindo o portal de suporte no backend..."
need_grep_file "ObsidianBridge" "$SERVER" "backend referencia ObsidianBridge"
need_grep_file "getBridgeInfo" "$SERVER" "backend referencia getBridgeInfo"
need_grep_file "getSessionSummary" "$SERVER" "backend referencia getSessionSummary"
need_grep_file "Mobile Support Portal" "$SERVER" "backend tem Mobile Support Portal"

# --- Guards da bridge (não vazar segredos) -----------------------------------
info "Conferindo guards da bridge..."
reject_grep_file "FLAG{" "$BRIDGE" "bridge sem FLAG{"
if grep -qE "analyst123|operator123" "$BRIDGE"; then
  fail "bridge contém credencial interna"
else
  pass "bridge sem credenciais internas"
fi

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
  printf '\033[32m==> Fase 6 validada com sucesso (estrutura).\033[0m\n'
  exit 0
else
  printf '\033[31m==> Fase 6: há falhas obrigatórias acima.\033[0m\n'
  exit 1
fi
