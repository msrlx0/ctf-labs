#!/usr/bin/env bash
#
# validate-phase10.sh — valida a Fase 10 do Lab 08 (Secure Vault / local auth:
# LocalAuthState, BiometricGate, VaultScreen, fallback PIN fraco, estado local
# de auth inseguro, backend vault-mobile status/unlock, eventos locais).
#
# - Verifica scripts de fases anteriores (phase1..phase9).
# - Verifica os arquivos novos do pacote auth/ e VaultScreen.
# - Grepa o código por strings-chave da fase.
# - Confere o backend (vault-mobile/status, vault-mobile/unlock, client-asserted, etc.).
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
AUTH_PKG="$SRC/auth"
VAULT_SCREEN="$SRC/ui/VaultScreen.kt"
HOME_SCREEN="$SRC/ui/HomeScreen.kt"
MAIN_ACTIVITY="$SRC/../MainActivity.kt"
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
for n in 1 2 3 4 5 6 7 8 9; do need_file "scripts/validate-phase$n.sh"; done

# --- Arquivos novos ----------------------------------------------------------
info "Conferindo arquivos da Fase 10..."
need_file "$AUTH_PKG/LocalAuthState.kt"
need_file "$AUTH_PKG/BiometricGate.kt"
need_file "$VAULT_SCREEN"

# --- LocalAuthState ----------------------------------------------------------
info "Conferindo LocalAuthState..."
need_grep_tree "LocalAuthState" "$AUTH_PKG" "classe LocalAuthState"
need_grep_tree "getWeakFallbackPin" "$AUTH_PKG" "método getWeakFallbackPin"
need_grep_tree "validateFallbackPin" "$AUTH_PKG" "método validateFallbackPin"
need_grep_tree "isVaultUnlocked" "$AUTH_PKG" "método isVaultUnlocked"
need_grep_tree "markVaultUnlocked" "$AUTH_PKG" "método markVaultUnlocked"
need_grep_tree "markVaultLocked" "$AUTH_PKG" "método markVaultLocked"
need_grep_tree "getLastUnlockReason" "$AUTH_PKG" "método getLastUnlockReason"
need_grep_tree "buildAuthDecision" "$AUTH_PKG" "método buildAuthDecision"
need_grep_tree "AuthDecision" "$AUTH_PKG" "data class AuthDecision"

# --- BiometricGate -----------------------------------------------------------
info "Conferindo BiometricGate..."
need_grep_tree "BiometricGate" "$AUTH_PKG" "classe BiometricGate"
need_grep_tree "canUseBiometric" "$AUTH_PKG" "método canUseBiometric"
need_grep_tree "ObsidianPay Vault" "$AUTH_PKG" "buildPromptTitle ObsidianPay Vault"
need_grep_tree "Confirm your identity" "$AUTH_PKG" "buildPromptSubtitle"
need_grep_tree "biometric-result-hook" "$AUTH_PKG" "bypass hint biometric-result-hook"
need_grep_tree "force-auth-decision-true" "$AUTH_PKG" "bypass hint force-auth-decision-true"
need_grep_tree "patch-local-auth-state" "$AUTH_PKG" "bypass hint patch-local-auth-state"

# --- VaultScreen -------------------------------------------------------------
info "Conferindo VaultScreen..."
need_grep_file "Secure Vault" "$VAULT_SCREEN" "título Secure Vault"
need_grep_file "biometric_capability_checked" "$VAULT_SCREEN" "evento biometric_capability_checked"
need_grep_file "biometric_prompt_started" "$VAULT_SCREEN" "evento biometric_prompt_started"
need_grep_file "biometric_auth_result" "$VAULT_SCREEN" "evento biometric_auth_result"
need_grep_file "local_auth_success" "$VAULT_SCREEN" "evento local_auth_success"
need_grep_file "local_auth_failed" "$VAULT_SCREEN" "evento local_auth_failed"
# vault_unlocked_local é emitido por LocalCacheManager.saveVaultUnlocked — confere na árvore
need_grep_tree "vault_unlocked_local" "$SRC" "evento vault_unlocked_local"
need_grep_file "vault_locked_local" "$VAULT_SCREEN" "evento vault_locked_local"
need_grep_file "weak_pin_fallback_used" "$VAULT_SCREEN" "evento weak_pin_fallback_used"

# Botões obrigatórios
need_grep_file "Check Biometric" "$VAULT_SCREEN" "botão Check Biometric"
need_grep_file "Unlock with Biometric" "$VAULT_SCREEN" "botão Unlock with Biometric"
need_grep_file "Unlock with PIN" "$VAULT_SCREEN" "botão Unlock with PIN"
need_grep_file "Lock Vault" "$VAULT_SCREEN" "botão Lock Vault"
need_grep_file "Fetch Vault Status" "$VAULT_SCREEN" "botão Fetch Vault Status"
need_grep_file "Request Vault Unlock" "$VAULT_SCREEN" "botão Request Vault Unlock"

# VaultScreen chama LocalAuthState e BiometricGate
need_grep_file "LocalAuthState" "$VAULT_SCREEN" "VaultScreen usa LocalAuthState"
need_grep_file "BiometricGate" "$VAULT_SCREEN" "VaultScreen usa BiometricGate"

# --- Storage / eventos adicionais --------------------------------------------
info "Conferindo storage e eventos vault no SRC..."
need_grep_tree "vault_status_cached" "$SRC" "evento vault_status_cached"
need_grep_tree "vault_unlock_response_cached" "$SRC" "evento vault_unlock_response_cached"
need_grep_tree "VAULT_MOBILE_STATUS_PATH" "$SRC" "constante VAULT_MOBILE_STATUS_PATH"
need_grep_tree "VAULT_MOBILE_UNLOCK_PATH" "$SRC" "constante VAULT_MOBILE_UNLOCK_PATH"
need_grep_tree "KEY_VAULT_UNLOCKED" "$SRC" "chave KEY_VAULT_UNLOCKED"
need_grep_tree "saveVaultUnlocked" "$SRC" "método saveVaultUnlocked"
need_grep_tree "getVaultUnlocked" "$SRC" "método getVaultUnlocked"
need_grep_tree "clearVaultState" "$SRC" "método clearVaultState"
need_grep_tree "getMobileVaultStatus" "$SRC" "método getMobileVaultStatus"
need_grep_tree "requestMobileVaultUnlock" "$SRC" "método requestMobileVaultUnlock"

# --- Backend -----------------------------------------------------------------
info "Conferindo backend vault-mobile..."
need_grep_file "/api/mobile/internal/vault-mobile/status" "$SERVER" "backend tem rota vault-mobile/status"
need_grep_file "/api/mobile/internal/vault-mobile/unlock" "$SERVER" "backend tem rota vault-mobile/unlock"
need_grep_file "enableMobileVault" "$DATA" "data tem enableMobileVault"
need_grep_file "mobileVaultStatusPath" "$DATA" "data tem mobileVaultStatusPath"
need_grep_file "mobileVaultUnlockPath" "$DATA" "data tem mobileVaultUnlockPath"
need_grep_file "client-asserted" "$SERVER" "backend retorna client-asserted"
need_grep_file "vault-access-granted" "$SERVER" "backend retorna vault-access-granted"
need_grep_file "server trusts local auth assertion" "$SERVER" "backend retorna nextStepHint"

# enableMobileVault e paths devem aparecer também no config endpoint
# enableMobileVault está em server.js via import; mobileVaultStatusPath/UnlockPath estão em data.js (buildMobileConfig)
need_grep_file "enableMobileVault" "$SERVER" "server expõe enableMobileVault no config"
need_grep_file "mobileVaultStatusPath" "$DATA" "data expõe mobileVaultStatusPath no buildMobileConfig"
need_grep_file "mobileVaultUnlockPath" "$DATA" "data expõe mobileVaultUnlockPath no buildMobileConfig"

# --- MainActivity / HomeScreen -----------------------------------------------
info "Conferindo MainActivity e HomeScreen..."
need_grep_tree "VaultScreen" "$SRC" "VaultScreen importado/usado"
need_grep_tree "Secure Vault" "$SRC" "texto Secure Vault no app"
need_grep_tree "Screen.Vault" "$SRC" "Screen.Vault na navegação"

# --- Reforço dos typos de fases anteriores -----------------------------------
info "Reforçando checagem de typos de fases anteriores..."
reject_grep_re_tree 'fun[[:space:]]+getSessionSummar\(' "$SRC" "sem typo getSessionSummar()"
reject_grep_re_tree 'getSessionSummar([^y]|$)' "$SRC" "sem typo getSessionSummar (sem y)"
reject_grep_re_tree '@JavascriptInterfac([^e]|$)' "$SRC" "sem typo @JavascriptInterfac"
reject_grep_re_tree 'webVieClient' "$SRC" "sem typo webVieClient"
reject_grep_re_tree 'object[[:space:]]+LegacyRequestSigne[[:space:]]*\{' "$SRC" "sem typo LegacyRequestSigne {"
reject_grep_re_tree '\bWeakCryptosha1Hex\b' "$SRC" "sem typo WeakCryptosha1Hex"
reject_grep_re_tree '\bWeakCryptomd5Hex\b' "$SRC" "sem typo WeakCryptomd5Hex"
# EnvironmentRiskEngine typo guard (herdado da Fase 9)
ENV_PKG="$SRC/environment"
if [ -f "$ENV_PKG/EnvironmentRiskEngine.kt" ]; then
  if grep -qE -- '->[[:space:]]+patch-risk-engine-result' "$ENV_PKG/EnvironmentRiskEngine.kt" 2>/dev/null; then
    fail 'typo: encontrado "-> patch-risk-engine-result" sem aspas de abertura em EnvironmentRiskEngine.kt'
  else
    pass 'sem typo "else -> patch-risk-engine-result" sem aspas'
  fi
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

# --- Guards nas novas classes ------------------------------------------------
info "Conferindo guards nas classes da Fase 10..."
for f in "$AUTH_PKG/LocalAuthState.kt" "$AUTH_PKG/BiometricGate.kt" "$VAULT_SCREEN"; do
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
  printf '\033[32m==> Fase 10 validada com sucesso (estrutura).\033[0m\n'
  exit 0
else
  printf '\033[31m==> Fase 10: há falhas obrigatórias acima.\033[0m\n'
  exit 1
fi
