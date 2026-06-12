#!/usr/bin/env bash
#
# validate-phase13.sh — valida a Fase 13 do Lab 08 (Dynamic Instrumentation
# scaffold: scripts Frida, playbook ADB, docs de pentest mobile).
#
# - Verifica scripts de fases anteriores (phase1..phase12).
# - Verifica os novos diretórios e arquivos de tools/frida, tools/adb, docs/mobile-pentest.
# - Grepa strings-chave nos scripts Frida e ADB.
# - Verifica hint IDs nos scripts.
# - Guards de docs públicos (sem FLAG{ / sem credenciais internas).
# - Guards de tools/ (sem FLAG{ / sem credenciais / sem apps reais externos).
# - Guards de typos de todas as fases anteriores.
# - Confere que nenhum lab 1..7 foi alterado.
# - Não exige Frida instalado, adb instalado nem Android SDK.
# - Sai com exit 1 se arquivos obrigatórios ou strings-chave faltarem.
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$LAB_DIR"

APP="android-app"
SRC="$APP/app/src/main/java/com/obsidianpay/mobile"
FRIDA_DIR="tools/frida"
ADB_DIR="tools/adb"
DOCS_DIR="docs/mobile-pentest"

pass() { printf '  \033[32m[PASS]\033[0m %s\n' "$1"; }
warn() { printf '  \033[33m[WARN]\033[0m %s\n' "$1"; }
info() { printf '\033[36m[*]\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m[FAIL]\033[0m %s\n' "$1" >&2; FAILED=1; }

FAILED=0

need_file() { if [ -f "$1" ]; then pass "arquivo: $1"; else fail "ausente: $1"; fi; }
need_dir()  { if [ -d "$1" ]; then pass "diretório: $1"; else fail "ausente: $1"; fi; }

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
for n in 1 2 3 4 5 6 7 8 9 10 11 12; do need_file "scripts/validate-phase$n.sh"; done

# --- Diretórios obrigatórios -------------------------------------------------
info "Conferindo diretórios obrigatórios da Fase 13..."
need_dir "$DOCS_DIR"
need_dir "$FRIDA_DIR"
need_dir "$ADB_DIR"

# --- Arquivos obrigatórios ---------------------------------------------------
info "Conferindo arquivos de docs/mobile-pentest/..."
need_file "$DOCS_DIR/SETUP.md"
need_file "$DOCS_DIR/PLAYBOOK.md"
need_file "$DOCS_DIR/INSTRUCTOR-NOTES.md"

info "Conferindo arquivos de tools/frida/..."
need_file "$FRIDA_DIR/README.md"
need_file "$FRIDA_DIR/01-environment-bypass.js"
need_file "$FRIDA_DIR/02-biometric-vault-bypass.js"
need_file "$FRIDA_DIR/03-network-pinning-observer.js"
need_file "$FRIDA_DIR/04-integrity-native-bypass.js"
need_file "$FRIDA_DIR/05-webview-bridge-observer.js"

info "Conferindo arquivos de tools/adb/..."
need_file "$ADB_DIR/README.md"
need_file "$ADB_DIR/lab08-adb-playbook.sh"

# --- Strings obrigatórias em tools/frida/ ------------------------------------
info "Conferindo strings obrigatórias nos scripts Frida..."

need_grep_tree "com.obsidianpay.mobile"   "$FRIDA_DIR"  "com.obsidianpay.mobile nos scripts Frida"
need_grep_tree "Java.perform"             "$FRIDA_DIR"  "Java.perform nos scripts Frida"
need_grep_tree "[ObsidianPay Lab]"        "$FRIDA_DIR"  "[ObsidianPay Lab] nos scripts Frida"

# Classes obrigatórias
need_grep_tree "RootDetector"             "$FRIDA_DIR"  "RootDetector nos scripts Frida"
need_grep_tree "EmulatorDetector"         "$FRIDA_DIR"  "EmulatorDetector nos scripts Frida"
need_grep_tree "EnvironmentRiskEngine"    "$FRIDA_DIR"  "EnvironmentRiskEngine nos scripts Frida"
need_grep_tree "LocalAuthState"           "$FRIDA_DIR"  "LocalAuthState nos scripts Frida"
need_grep_tree "BiometricGate"            "$FRIDA_DIR"  "BiometricGate nos scripts Frida"
need_grep_tree "PinningPolicy"            "$FRIDA_DIR"  "PinningPolicy nos scripts Frida"
need_grep_tree "CertificatePinner"        "$FRIDA_DIR"  "CertificatePinner nos scripts Frida"
need_grep_tree "NativeGate"               "$FRIDA_DIR"  "NativeGate nos scripts Frida"
need_grep_tree "TamperCheck"              "$FRIDA_DIR"  "TamperCheck nos scripts Frida"
need_grep_tree "ObsidianBridge"           "$FRIDA_DIR"  "ObsidianBridge nos scripts Frida"
need_grep_tree "ObsidianSupportBridge"    "$FRIDA_DIR"  "ObsidianSupportBridge nos scripts Frida"

# --- Strings obrigatórias em 01-environment-bypass.js -----------------------
info "Conferindo strings em 01-environment-bypass.js..."
SCRIPT01="$FRIDA_DIR/01-environment-bypass.js"
need_grep_file "Java.perform"             "$SCRIPT01"   "Java.perform em 01"
need_grep_file "com.obsidianpay.mobile.environment" "$SCRIPT01" "package environment em 01"
need_grep_file "RootDetector"             "$SCRIPT01"   "RootDetector em 01"
need_grep_file "EmulatorDetector"         "$SCRIPT01"   "EmulatorDetector em 01"
need_grep_file "EnvironmentRiskEngine"    "$SCRIPT01"   "EnvironmentRiskEngine em 01"
need_grep_file "hooks-change-return-values" "$SCRIPT01" "hint hooks-change-return-values em 01"
need_grep_file "patch-risk-engine-result"   "$SCRIPT01" "hint patch-risk-engine-result em 01"
need_grep_file "env-check-local-only"       "$SCRIPT01" "hint env-check-local-only em 01"
need_grep_file "[ObsidianPay Lab]"        "$SCRIPT01"   "[ObsidianPay Lab] em 01"

# --- Strings obrigatórias em 02-biometric-vault-bypass.js -------------------
info "Conferindo strings em 02-biometric-vault-bypass.js..."
SCRIPT02="$FRIDA_DIR/02-biometric-vault-bypass.js"
need_grep_file "Java.perform"             "$SCRIPT02"   "Java.perform em 02"
need_grep_file "LocalAuthState"           "$SCRIPT02"   "LocalAuthState em 02"
need_grep_file "BiometricGate"            "$SCRIPT02"   "BiometricGate em 02"
need_grep_file "VaultScreen"              "$SCRIPT02"   "VaultScreen em 02"
need_grep_file "validateFallbackPin"      "$SCRIPT02"   "validateFallbackPin em 02"
need_grep_file "isVaultUnlocked"          "$SCRIPT02"   "isVaultUnlocked em 02"
need_grep_file "biometric-result-hook"    "$SCRIPT02"   "hint biometric-result-hook em 02"
need_grep_file "force-auth-decision-true" "$SCRIPT02"   "hint force-auth-decision-true em 02"
need_grep_file "patch-local-auth-state"   "$SCRIPT02"   "hint patch-local-auth-state em 02"
need_grep_file "[ObsidianPay Lab]"        "$SCRIPT02"   "[ObsidianPay Lab] em 02"

# --- Strings obrigatórias em 03-network-pinning-observer.js -----------------
info "Conferindo strings em 03-network-pinning-observer.js..."
SCRIPT03="$FRIDA_DIR/03-network-pinning-observer.js"
need_grep_file "Java.perform"                    "$SCRIPT03" "Java.perform em 03"
need_grep_file "PinningPolicy"                   "$SCRIPT03" "PinningPolicy em 03"
need_grep_file "NetworkSecurityProfile"          "$SCRIPT03" "NetworkSecurityProfile em 03"
need_grep_file "CertificatePinner"               "$SCRIPT03" "CertificatePinner em 03"
need_grep_file "okhttp-certificate-pinner-hook"  "$SCRIPT03" "hint okhttp-certificate-pinner-hook em 03"
need_grep_file "trust-user-ca"                   "$SCRIPT03" "hint trust-user-ca em 03"
need_grep_file "network-config-cleartext-override" "$SCRIPT03" "hint network-config-cleartext-override em 03"
need_grep_file "[ObsidianPay Lab]"               "$SCRIPT03" "[ObsidianPay Lab] em 03"

# --- Strings obrigatórias em 04-integrity-native-bypass.js ------------------
info "Conferindo strings em 04-integrity-native-bypass.js..."
SCRIPT04="$FRIDA_DIR/04-integrity-native-bypass.js"
need_grep_file "Java.perform"               "$SCRIPT04" "Java.perform em 04"
need_grep_file "NativeGate"                 "$SCRIPT04" "NativeGate em 04"
need_grep_file "TamperCheck"                "$SCRIPT04" "TamperCheck em 04"
need_grep_file "isNativeLibraryLoaded"      "$SCRIPT04" "isNativeLibraryLoaded em 04"
need_grep_file "getNativeGateStatus"        "$SCRIPT04" "getNativeGateStatus em 04"
need_grep_file "isDebuggable"               "$SCRIPT04" "isDebuggable em 04"
need_grep_file "getInstallerPackage"        "$SCRIPT04" "getInstallerPackage em 04"
need_grep_file "getPackageNameStatus"       "$SCRIPT04" "getPackageNameStatus em 04"
need_grep_file "jni-return-value-hook"      "$SCRIPT04" "hint jni-return-value-hook em 04"
need_grep_file "patch-native-gate-result"   "$SCRIPT04" "hint patch-native-gate-result em 04"
need_grep_file "hook-package-manager"       "$SCRIPT04" "hint hook-package-manager em 04"
need_grep_file "patch-debuggable-check"     "$SCRIPT04" "hint patch-debuggable-check em 04"
need_grep_file "repackage-signature-mismatch" "$SCRIPT04" "hint repackage-signature-mismatch em 04"
need_grep_file "[ObsidianPay Lab]"          "$SCRIPT04" "[ObsidianPay Lab] em 04"

# --- Strings obrigatórias em 05-webview-bridge-observer.js ------------------
info "Conferindo strings em 05-webview-bridge-observer.js..."
SCRIPT05="$FRIDA_DIR/05-webview-bridge-observer.js"
need_grep_file "Java.perform"               "$SCRIPT05" "Java.perform em 05"
need_grep_file "ObsidianBridge"             "$SCRIPT05" "ObsidianBridge em 05"
need_grep_file "addJavascriptInterface"     "$SCRIPT05" "addJavascriptInterface em 05"
need_grep_file "ObsidianSupportBridge"      "$SCRIPT05" "ObsidianSupportBridge em 05"
need_grep_file "getSessionSummary"          "$SCRIPT05" "getSessionSummary em 05"
need_grep_file "getCachedConfig"            "$SCRIPT05" "getCachedConfig em 05"
need_grep_file "logBridgeEvent"             "$SCRIPT05" "logBridgeEvent em 05"
need_grep_file "[ObsidianPay Lab]"          "$SCRIPT05" "[ObsidianPay Lab] em 05"

# --- Strings obrigatórias em tools/adb/ -------------------------------------
info "Conferindo strings em tools/adb/..."
ADB_PLAYBOOK="$ADB_DIR/lab08-adb-playbook.sh"
need_grep_file 'PACKAGE="com.obsidianpay.mobile"'   "$ADB_PLAYBOOK" "PACKAGE declaration em adb-playbook"
need_grep_file "adb install"                        "$ADB_PLAYBOOK" "adb install em adb-playbook"
need_grep_tree "adb shell am start"                 "$ADB_DIR"      "adb shell am start em tools/adb"
need_grep_tree "adb shell am broadcast"             "$ADB_DIR"      "adb shell am broadcast em tools/adb"
need_grep_tree "adb shell content query"            "$ADB_DIR"      "adb shell content query em tools/adb"
need_grep_tree "adb shell run-as"                   "$ADB_DIR"      "adb shell run-as em tools/adb"
need_grep_tree "adb logcat"                         "$ADB_DIR"      "adb logcat em tools/adb"
need_grep_tree "obsidianpay://transfer"             "$ADB_DIR"      "deep link obsidianpay://transfer em tools/adb"
need_grep_tree "com.obsidianpay.mobile.INTERNAL_OPS" "$ADB_DIR"    "INTERNAL_OPS em tools/adb"
need_grep_tree "com.obsidianpay.mobile.DEBUG_COMMAND" "$ADB_DIR"   "DEBUG_COMMAND em tools/adb"
need_grep_tree "content://com.obsidianpay.mobile.provider.notes/notes" "$ADB_DIR" "provider /notes em tools/adb"
need_grep_tree "content://com.obsidianpay.mobile.provider.notes/debug" "$ADB_DIR" "provider /debug em tools/adb"

# --- Strings obrigatórias em docs/mobile-pentest/ ---------------------------
info "Conferindo strings em docs/mobile-pentest/SETUP.md..."
SETUP="$DOCS_DIR/SETUP.md"
need_grep_file "Android Emulator"          "$SETUP"  "Android Emulator em SETUP"
need_grep_file "physical Android device"   "$SETUP"  "physical Android device em SETUP"
need_grep_file "10.0.2.2"                  "$SETUP"  "10.0.2.2 em SETUP"
need_grep_file "API Host"                  "$SETUP"  "API Host em SETUP"
need_grep_file "Burp Suite"                "$SETUP"  "Burp Suite em SETUP"
need_grep_file "Frida"                     "$SETUP"  "Frida em SETUP"
need_grep_file "objection"                 "$SETUP"  "objection em SETUP"
need_grep_file "JADX"                      "$SETUP"  "JADX em SETUP"
need_grep_file "apktool"                   "$SETUP"  "apktool em SETUP"
need_grep_file "adb"                       "$SETUP"  "adb em SETUP"
need_grep_file "spawn mode"                "$FRIDA_DIR/README.md" "spawn mode em frida/README"
need_grep_file "attach mode"               "$FRIDA_DIR/README.md" "attach mode em frida/README"
need_grep_file "authorized local lab"      "$FRIDA_DIR/README.md" "authorized local lab em frida/README"
need_grep_file "frida -U -f"               "$FRIDA_DIR/README.md" "frida -U -f em frida/README"
need_grep_file "frida -U com.obsidianpay.mobile" "$FRIDA_DIR/README.md" "frida -U com.obsidianpay.mobile em frida/README"

info "Conferindo strings em docs/mobile-pentest/PLAYBOOK.md..."
PLAYBOOK="$DOCS_DIR/PLAYBOOK.md"
need_grep_file "Install APK"               "$PLAYBOOK" "Install APK em PLAYBOOK"
need_grep_file "Configure API Host"        "$PLAYBOOK" "Configure API Host em PLAYBOOK"
need_grep_file "Intercept traffic"         "$PLAYBOOK" "Intercept traffic em PLAYBOOK"
need_grep_file "Inspect local storage"     "$PLAYBOOK" "Inspect local storage em PLAYBOOK"
need_grep_file "Enumerate exported components" "$PLAYBOOK" "Enumerate exported components em PLAYBOOK"
need_grep_file "Query ContentProvider"     "$PLAYBOOK" "Query ContentProvider em PLAYBOOK"
need_grep_file "Trigger deep links"        "$PLAYBOOK" "Trigger deep links em PLAYBOOK"
need_grep_file "WebView bridge"            "$PLAYBOOK" "WebView bridge em PLAYBOOK"
need_grep_file "hardcoded secrets"         "$PLAYBOOK" "hardcoded secrets em PLAYBOOK"
need_grep_file "root/emulator"             "$PLAYBOOK" "root/emulator em PLAYBOOK"
need_grep_file "biometric vault"           "$PLAYBOOK" "biometric vault em PLAYBOOK"
need_grep_file "app integrity"             "$PLAYBOOK" "app integrity em PLAYBOOK"
need_grep_file "dynamic instrumentation"   "$PLAYBOOK" "dynamic instrumentation em PLAYBOOK"

info "Conferindo hint IDs em docs/mobile-pentest/INSTRUCTOR-NOTES.md..."
INOTES="$DOCS_DIR/INSTRUCTOR-NOTES.md"
need_grep_file "hooks-change-return-values"     "$INOTES" "hint hooks-change-return-values em INSTRUCTOR-NOTES"
need_grep_file "patch-risk-engine-result"       "$INOTES" "hint patch-risk-engine-result em INSTRUCTOR-NOTES"
need_grep_file "biometric-result-hook"          "$INOTES" "hint biometric-result-hook em INSTRUCTOR-NOTES"
need_grep_file "force-auth-decision-true"       "$INOTES" "hint force-auth-decision-true em INSTRUCTOR-NOTES"
need_grep_file "patch-local-auth-state"         "$INOTES" "hint patch-local-auth-state em INSTRUCTOR-NOTES"
need_grep_file "okhttp-certificate-pinner-hook" "$INOTES" "hint okhttp-certificate-pinner-hook em INSTRUCTOR-NOTES"
need_grep_file "trust-user-ca"                  "$INOTES" "hint trust-user-ca em INSTRUCTOR-NOTES"
need_grep_file "network-config-cleartext-override" "$INOTES" "hint network-config-cleartext-override em INSTRUCTOR-NOTES"
need_grep_file "jni-return-value-hook"          "$INOTES" "hint jni-return-value-hook em INSTRUCTOR-NOTES"
need_grep_file "patch-native-gate-result"       "$INOTES" "hint patch-native-gate-result em INSTRUCTOR-NOTES"
need_grep_file "hook-package-manager"           "$INOTES" "hint hook-package-manager em INSTRUCTOR-NOTES"
need_grep_file "patch-debuggable-check"         "$INOTES" "hint patch-debuggable-check em INSTRUCTOR-NOTES"

# --- Guards de docs públicos (sem FLAG{ / sem credenciais) -------------------
info "Verificando docs públicos (sem FLAG{ e sem credenciais)..."
for doc in README.md STUDENT-GUIDE.md android-app/README.md; do
  reject_grep_tree "FLAG{"       "$doc"  "sem FLAG{ em $doc"
  reject_grep_tree "analyst123"  "$doc"  "sem analyst123 em $doc"
  reject_grep_tree "operator123" "$doc"  "sem operator123 em $doc"
done

# --- Guards em tools/ (sem FLAG{ / sem credenciais / sem apps reais) ---------
info "Verificando tools/ (sem FLAG{, sem credenciais, sem apps reais externos)..."
reject_grep_tree "FLAG{"       "$FRIDA_DIR"  "sem FLAG{ em tools/frida"
reject_grep_tree "FLAG{"       "$ADB_DIR"    "sem FLAG{ em tools/adb"
reject_grep_tree "analyst123"  "$FRIDA_DIR"  "sem analyst123 em tools/frida"
reject_grep_tree "analyst123"  "$ADB_DIR"    "sem analyst123 em tools/adb"
reject_grep_tree "operator123" "$FRIDA_DIR"  "sem operator123 em tools/frida"
reject_grep_tree "operator123" "$ADB_DIR"    "sem operator123 em tools/adb"
reject_grep_tree "FLAG{"       "$DOCS_DIR"   "sem FLAG{ em docs/mobile-pentest"

# Verificar ausência de referências a apps reais externos (banco/social/mensageria)
reject_grep_tree "com.whatsapp"          "$FRIDA_DIR"  "sem referência a com.whatsapp em tools/frida"
reject_grep_tree "com.instagram.android" "$FRIDA_DIR"  "sem referência a com.instagram em tools/frida"
reject_grep_tree "com.facebook.katana"   "$FRIDA_DIR"  "sem referência a com.facebook em tools/frida"
reject_grep_tree "com.twitter.android"   "$FRIDA_DIR"  "sem referência a com.twitter em tools/frida"
reject_grep_tree "com.nubank"            "$FRIDA_DIR"  "sem referência a com.nubank em tools/frida"
reject_grep_tree "com.itau"              "$FRIDA_DIR"  "sem referência a com.itau em tools/frida"
reject_grep_tree "com.bradesco"          "$FRIDA_DIR"  "sem referência a com.bradesco em tools/frida"
reject_grep_tree "com.paypal"            "$FRIDA_DIR"  "sem referência a com.paypal em tools/frida"

# --- Guards de typos de fases anteriores ------------------------------------
info "Verificando ausência de typos conhecidos (código-fonte Android)..."
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
reject_grep_tree 'cache.addEvent(integrity_check_started"'  "$SRC"  "sem typo addEvent sem aspas de abertura"
reject_grep_tree 'getLastNativeGatStatus'                   "$SRC"  "sem typo getLastNativeGatStatus"
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
  printf '\033[32m[OK] Fase 13 validada com sucesso.\033[0m\n'
  exit 0
else
  printf '\033[31m[FAIL] Fase 13: uma ou mais verificações falharam.\033[0m\n' >&2
  exit 1
fi
