#!/usr/bin/env bash
#
# validate-phase5.sh — valida a Fase 5 do Lab 08 (deep links, QR, WebView).
#
# - Verifica scripts de fases anteriores.
# - Verifica arquivos novos (DeepLinkRouter, QrInputScreen, WebViewSupportScreen).
# - Grepa o Manifest (deep links) e o código (eventos, WebView, base URL, endpoint).
# - Verifica referências de UI (QR Payment, Process Payload, Open Web Support).
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
MANIFEST="$APP/app/src/main/AndroidManifest.xml"

pass() { printf '  \033[32m[PASS]\033[0m %s\n' "$1"; }
warn() { printf '  \033[33m[WARN]\033[0m %s\n' "$1"; }
info() { printf '\033[36m[*]\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m[FAIL]\033[0m %s\n' "$1" >&2; FAILED=1; }

FAILED=0

need_file() { if [ -f "$1" ]; then pass "arquivo: $1"; else fail "ausente: $1"; fi; }

need_grep_file() {
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
need_file "scripts/validate-phase4.sh"

# --- Arquivos novos ----------------------------------------------------------
info "Conferindo arquivos da Fase 5..."
need_file "$SRC/deeplink/DeepLinkRouter.kt"
need_file "$SRC/deeplink/DeepLinkModels.kt"
need_file "$SRC/ui/QrInputScreen.kt"
need_file "$SRC/ui/WebViewSupportScreen.kt"

# --- Manifest deep links -----------------------------------------------------
info "Conferindo deep links no Manifest..."
need_grep_file "obsidianpay" "$MANIFEST" "Manifest contém scheme obsidianpay"
need_grep_file "transfer" "$MANIFEST" "Manifest contém host transfer"
need_grep_file "support" "$MANIFEST" "Manifest contém host support"
need_grep_file "receipt" "$MANIFEST" "Manifest contém host receipt"
need_grep_file "android.intent.action.VIEW" "$MANIFEST" "Manifest contém action VIEW"
need_grep_file "android.intent.category.BROWSABLE" "$MANIFEST" "Manifest contém category BROWSABLE"

# --- Conteúdos-chave de código ----------------------------------------------
info "Conferindo conteúdos-chave de código..."
need_grep_tree "deep_link_opened" "$SRC" "evento deep_link_opened"
need_grep_tree "qr_payload_processed" "$SRC" "evento qr_payload_processed"
need_grep_tree "webview_support_opened" "$SRC" "evento webview_support_opened"
need_grep_tree "WebView" "$SRC" "uso de WebView"
need_grep_file "javaScriptEnabled" "$SRC/ui/WebViewSupportScreen.kt" "WebView habilita JavaScript"
need_grep_file "domStorageEnabled" "$SRC/ui/WebViewSupportScreen.kt" "WebView habilita DOM storage"
need_grep_tree "/api/mobile/webview/support" "$SRC" "referência ao endpoint webview/support"
if grep -rqF "10.0.2.2:8102" "$SRC" || grep -rqF "DEFAULT_BASE_URL" "$SRC"; then
  pass "base URL local referenciada (10.0.2.2:8102 / DEFAULT_BASE_URL)"
else
  fail "base URL local não referenciada"
fi

# --- UI ----------------------------------------------------------------------
info "Conferindo referências de UI..."
need_grep_tree "QR Payment" "$APP/app/src/main" "UI: QR Payment"
need_grep_tree "Process Payload" "$APP/app/src/main" "UI: Process Payload"
need_grep_tree "Open Web Support" "$APP/app/src/main" "UI: Open Web Support"

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
  printf '\033[32m==> Fase 5 validada com sucesso (estrutura).\033[0m\n'
  exit 0
else
  printf '\033[31m==> Fase 5: há falhas obrigatórias acima.\033[0m\n'
  exit 1
fi
