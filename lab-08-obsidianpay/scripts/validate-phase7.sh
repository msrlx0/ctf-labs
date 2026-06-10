#!/usr/bin/env bash
#
# validate-phase7.sh — valida a Fase 7 do Lab 08 (componentes Android exportados).
#
# - Verifica scripts de fases anteriores (phase1..phase6).
# - Verifica os arquivos novos do pacote platform/ (Activity/Receiver/Provider).
# - Confere o AndroidManifest (nomes, exported, actions, authority).
# - Grepa o código por extras previsíveis, eventos e helpers da fase.
# - Reforça as checagens de typos de bridge/WebView (regex com limites).
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
PLATFORM="$SRC/platform"
MANIFEST="$APP/app/src/main/AndroidManifest.xml"
BRIDGE="$SRC/webview/ObsidianSupportBridge.kt"
WEBVIEW_SCREEN="$SRC/ui/WebViewSupportScreen.kt"

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
  # Usa regex com limites para não casar com o nome correto (ex.: o typo
  # 'getSessionSummar' é substring do correto 'getSessionSummary').
  if grep -rEq "$1" "$2" 2>/dev/null; then
    fail "$3 (typo /$1/ encontrado em $2)"
  else
    pass "$3"
  fi
}

info "Diretório do lab: $LAB_DIR"

# --- Scripts anteriores ------------------------------------------------------
info "Conferindo scripts de fases anteriores..."
need_file "scripts/validate-phase1.sh"
need_file "scripts/validate-phase2.sh"
need_file "scripts/validate-phase3.sh"
need_file "scripts/validate-phase4.sh"
need_file "scripts/validate-phase5.sh"
need_file "scripts/validate-phase6.sh"

# --- Arquivos novos do pacote platform --------------------------------------
info "Conferindo arquivos da Fase 7 (platform/)..."
need_file "$PLATFORM/InternalOpsActivity.kt"
need_file "$PLATFORM/DebugCommandReceiver.kt"
need_file "$PLATFORM/ObsidianNotesProvider.kt"

# --- AndroidManifest ---------------------------------------------------------
info "Conferindo AndroidManifest (componentes exportados)..."
need_grep_file ".platform.InternalOpsActivity" "$MANIFEST" "Manifest declara InternalOpsActivity"
need_grep_file ".platform.DebugCommandReceiver" "$MANIFEST" "Manifest declara DebugCommandReceiver"
need_grep_file ".platform.ObsidianNotesProvider" "$MANIFEST" "Manifest declara ObsidianNotesProvider"
need_grep_file 'android:exported="true"' "$MANIFEST" "Manifest tem android:exported=\"true\""
need_grep_file "com.obsidianpay.mobile.INTERNAL_OPS" "$MANIFEST" "Manifest tem action INTERNAL_OPS"
need_grep_file "com.obsidianpay.mobile.DEBUG_COMMAND" "$MANIFEST" "Manifest tem action DEBUG_COMMAND"
need_grep_file "com.obsidianpay.mobile.provider.notes" "$MANIFEST" "Manifest tem authority provider.notes"
need_grep_file "<receiver" "$MANIFEST" "Manifest declara <receiver>"
need_grep_file "<provider" "$MANIFEST" "Manifest declara <provider>"

# --- Conteúdo-chave do código ------------------------------------------------
info "Conferindo extras previsíveis e eventos no código..."
need_grep_tree "obsidian.intent.extra.INTERNAL_ROUTE" "$SRC" "extra INTERNAL_ROUTE"
need_grep_tree "obsidian.intent.extra.SESSION_HINT" "$SRC" "extra SESSION_HINT"
need_grep_tree "obsidian.intent.extra.OPERATOR_MODE" "$SRC" "extra OPERATOR_MODE"
need_grep_tree "obsidian.intent.extra.RECEIPT_ID" "$SRC" "extra RECEIPT_ID"
need_grep_tree "exported_activity_opened" "$SRC" "evento exported_activity_opened"
need_grep_tree "exported_receiver_called" "$SRC" "evento exported_receiver_called"
need_grep_tree "external_debug_sync_marker" "$SRC" "comando external_debug_sync_marker"
need_grep_tree "write_debug_export" "$SRC" "comando write_debug_export"
need_grep_tree "set_last_receipt" "$SRC" "comando set_last_receipt"
need_grep_tree "enable_operator_hint" "$SRC" "comando enable_operator_hint"
need_grep_tree "MatrixCursor" "$SRC" "Provider usa MatrixCursor"
need_grep_tree "token_preview" "$SRC" "Provider expõe token_preview (mascarado)"
need_grep_tree "getSafeDebugValuesForProvider" "$SRC" "helper getSafeDebugValuesForProvider"
need_grep_tree "BroadcastReceiver" "$SRC" "Receiver estende BroadcastReceiver"
need_grep_tree "ContentProvider" "$SRC" "Provider estende ContentProvider"

# Provider não deve devolver o token inteiro: garante que a leitura do token na
# fonte do provider passa pelo preview, não pelo valor cru.
info "Conferindo que o Provider só expõe preview do token..."
need_grep_file "getTokenPreview" "$SRC/storage/InsecureSessionStore.kt" "store tem getTokenPreview (token mascarado)"
if grep -qF "getToken()" "$PLATFORM/ObsidianNotesProvider.kt" 2>/dev/null; then
  fail "Provider parece ler getToken() diretamente (use token_preview)"
else
  pass "Provider não lê getToken() diretamente"
fi

# --- Reforço dos typos de bridge/WebView (Fase 6) ----------------------------
# Regex com limites: pegam o typo mas NÃO casam com o nome correto.
info "Reforçando checagem de typos de bridge/WebView..."
reject_grep_re_tree 'fun[[:space:]]+getSessionSummar\(' "$SRC" "sem 'fun getSessionSummar()' (typo)"
reject_grep_re_tree 'getSessionSummar([^y]|$)' "$SRC" "sem typo 'getSessionSummar' (esperado getSessionSummary)"
reject_grep_re_tree '@JavascriptInterfac([^e]|$)' "$SRC" "sem typo '@JavascriptInterfac' (esperado @JavascriptInterface)"
reject_grep_re_tree 'webVieClient' "$SRC" "sem typo 'webVieClient' (esperado webViewClient)"
need_grep_file "fun getSessionSummary" "$BRIDGE" "bridge declara 'fun getSessionSummary'"
need_grep_file "@JavascriptInterface" "$BRIDGE" "bridge usa @JavascriptInterface"
need_grep_file "webViewClient" "$WEBVIEW_SCREEN" "WebView configura webViewClient"

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

# Guards adicionais nos próprios componentes exportados.
info "Conferindo guards dos componentes exportados..."
for f in "$PLATFORM/InternalOpsActivity.kt" "$PLATFORM/DebugCommandReceiver.kt" \
         "$PLATFORM/ObsidianNotesProvider.kt"; do
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
  printf '\033[32m==> Fase 7 validada com sucesso (estrutura).\033[0m\n'
  exit 0
else
  printf '\033[31m==> Fase 7: há falhas obrigatórias acima.\033[0m\n'
  exit 1
fi
