#!/usr/bin/env bash
#
# validate-phase9.sh — valida a Fase 9 do Lab 08 (root detection, emulator
# detection, EnvironmentRiskEngine, SecurityCheckScreen, environment report
# backend, cache local, scaffold de bypass).
#
# - Verifica scripts de fases anteriores (phase1..phase8).
# - Verifica os arquivos novos do pacote environment/ e SecurityCheckScreen.
# - Grepa o código por strings-chave da fase.
# - Confere o backend (environment-report, enableEnvironmentChecks, monitor-only).
# - Reforça os guards de typos de fases anteriores.
# - Guards de docs públicos e das novas classes (sem FLAG{ / sem credenciais).
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
ENV_PKG="$SRC/environment"
SEC_SCREEN="$SRC/ui/SecurityCheckScreen.kt"
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

info "Diretório do lab: $LAB_DIR"

# --- Scripts anteriores ------------------------------------------------------
info "Conferindo scripts de fases anteriores..."
for n in 1 2 3 4 5 6 7 8; do need_file "scripts/validate-phase$n.sh"; done

# --- Arquivos novos ----------------------------------------------------------
info "Conferindo arquivos da Fase 9..."
need_file "$ENV_PKG/RootDetector.kt"
need_file "$ENV_PKG/EmulatorDetector.kt"
need_file "$ENV_PKG/EnvironmentRiskEngine.kt"
need_file "$SEC_SCREEN"

# --- RootDetector ------------------------------------------------------------
info "Conferindo RootDetector..."
need_grep_tree "RootDetector" "$ENV_PKG" "classe RootDetector"
need_grep_tree "test-keys" "$ENV_PKG" "check test-keys"
need_grep_tree "com.topjohnwu.magisk" "$ENV_PKG" "pacote magisk"
need_grep_tree "/system/bin/su" "$ENV_PKG" "path /system/bin/su"

# --- EmulatorDetector --------------------------------------------------------
info "Conferindo EmulatorDetector..."
need_grep_tree "EmulatorDetector" "$ENV_PKG" "classe EmulatorDetector"
need_grep_tree "goldfish" "$ENV_PKG" "hardware goldfish"
need_grep_tree "ranchu" "$ENV_PKG" "hardware ranchu"

# --- EnvironmentRiskEngine ---------------------------------------------------
info "Conferindo EnvironmentRiskEngine..."
need_grep_tree "EnvironmentRiskEngine" "$ENV_PKG" "classe EnvironmentRiskEngine"
need_grep_tree "bypassHintId" "$ENV_PKG" "campo bypassHintId"
need_grep_tree "env-check-local-only" "$ENV_PKG" "bypassHintId env-check-local-only"
need_grep_tree "hooks-change-return-values" "$ENV_PKG" "bypassHintId hooks-change-return-values"
need_grep_tree "patch-risk-engine-result" "$ENV_PKG" "bypassHintId patch-risk-engine-result"

# --- SecurityCheckScreen -----------------------------------------------------
info "Conferindo SecurityCheckScreen..."
need_grep_file "Security Check" "$SEC_SCREEN" "título Security Check"
need_grep_file "environment_check_started" "$SEC_SCREEN" "evento environment_check_started"
need_grep_file "root_detection_completed" "$SEC_SCREEN" "evento root_detection_completed"
need_grep_file "emulator_detection_completed" "$SEC_SCREEN" "evento emulator_detection_completed"
need_grep_file "environment_risk_calculated" "$SEC_SCREEN" "evento environment_risk_calculated"
need_grep_file "environment_report_sent" "$SEC_SCREEN" "evento environment_report_sent"
need_grep_tree "environment_report_cached" "$SRC" "evento environment_report_cached"

# Botões obrigatórios
need_grep_file "Run Security Check" "$SEC_SCREEN" "botão Run Security Check"
need_grep_file "Send Environment Report" "$SEC_SCREEN" "botão Send Environment Report"
need_grep_file "Show Local Signals" "$SEC_SCREEN" "botão Show Local Signals"
need_grep_file "Clear Local Report" "$SEC_SCREEN" "botão Clear Local Report"

# --- Backend -----------------------------------------------------------------
info "Conferindo backend environment-report..."
need_grep_file "/api/mobile/internal/environment-report" "$SERVER" "backend tem rota environment-report"
need_grep_file "enableEnvironmentChecks" "$DATA" "data tem enableEnvironmentChecks"
need_grep_file "environmentReportPath" "$DATA" "data tem environmentReportPath"
need_grep_file "monitor-only" "$SERVER" "backend retorna monitor-only"
need_grep_file "client-side checks are advisory" "$SERVER" "backend retorna nextStepHint"

# --- Verificações adicionais de keys/events no SRC ---------------------------
info "Conferindo chaves de Constants e storage..."
need_grep_tree "ENVIRONMENT_REPORT_PATH" "$SRC" "constante ENVIRONMENT_REPORT_PATH"
need_grep_tree "KEY_LAST_ENVIRONMENT_REPORT" "$SRC" "chave KEY_LAST_ENVIRONMENT_REPORT"
need_grep_tree "sendEnvironmentReport" "$SRC" "método sendEnvironmentReport"

# --- Reforço dos typos de bridge/WebView/LegacyRequestSigner -----------------
info "Reforçando checagem de typos de fases anteriores..."
reject_grep_re_tree 'fun[[:space:]]+getSessionSummar\(' "$SRC" "sem typo getSessionSummar()"
reject_grep_re_tree 'getSessionSummar([^y]|$)' "$SRC" "sem typo getSessionSummar (sem y)"
reject_grep_re_tree '@JavascriptInterfac([^e]|$)' "$SRC" "sem typo @JavascriptInterfac"
reject_grep_re_tree 'webVieClient' "$SRC" "sem typo webVieClient"
reject_grep_re_tree 'object[[:space:]]+LegacyRequestSigne[[:space:]]*\{' "$SRC" "sem typo LegacyRequestSigne {"
reject_grep_re_tree '\bWeakCryptosha1Hex\b' "$SRC" "sem typo WeakCryptosha1Hex"
reject_grep_re_tree '\bWeakCryptomd5Hex\b' "$SRC" "sem typo WeakCryptomd5Hex"

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
info "Conferindo guards nas classes da Fase 9..."
for f in "$ENV_PKG/RootDetector.kt" "$ENV_PKG/EmulatorDetector.kt" \
         "$ENV_PKG/EnvironmentRiskEngine.kt" "$SEC_SCREEN"; do
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
  printf '\033[32m==> Fase 9 validada com sucesso (estrutura).\033[0m\n'
  exit 0
else
  printf '\033[31m==> Fase 9: há falhas obrigatórias acima.\033[0m\n'
  exit 1
fi
