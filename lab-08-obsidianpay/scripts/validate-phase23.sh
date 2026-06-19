#!/usr/bin/env bash
#
# validate-phase23.sh — valida a Fase 23A do Lab 08 (ObsidianPay Mobile):
# redesign de UI/UX fintech (Material 3, tema escuro, navegação por abas),
# correção de rede (presets emulador/celular físico, sem default 8080) e
# preparação do candidato RC2 — preservando TODAS as vulnerabilidades, os
# componentes exportados, deep links, WebView/bridge, armazenamento inseguro,
# biometria, detecção de root/emulador, rede/pinning e o contrato da Final
# Challenge Chain.
#
# A Fase 23A NÃO corrige/remove vulnerabilidades intencionais nem altera os
# contratos de API/flags. Este script valida (entre outros):
#   - branch correta e labs 1..7 intactos;
#   - arquivos Android exigidos (tema, componentes, telas redesenhadas);
#   - pacotes debug/release, compileSdk/targetSdk/minSdk, versionCode/versionName;
#   - ausência de default de runtime 8080; presets 127.0.0.1:8102 e 10.0.2.2:8102;
#   - nomes RC2 no workflow, no packaging e no DOWNLOAD.md;
#   - ausência de APK rastreado/presente para commit;
#   - anti-leak (sem FLAG{ / credenciais privadas em material público);
#   - preservação dos componentes exportados, MAIN/LAUNCHER, deep links, ações e
#     authority sensíveis, WebView bridge, storage inseguro, biometria, detecção
#     de root/emulador e rede/pinning;
#   - sintaxe shell (bash -n), consistência do Gradle e git diff --check;
#   - regressões: validate-phase18/19/20/22 (a 22 já encadeia 18/19/20).
# Opcionalmente, se o Android SDK estiver disponível, executa o build real.
#
# Sai com exit 1 se qualquer verificação obrigatória falhar.
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$LAB_DIR"

REPO_ROOT="$(git -C "$LAB_DIR" rev-parse --show-toplevel 2>/dev/null || (cd "$LAB_DIR/.." && pwd))"

APP="android-app"
SRC="$APP/app/src/main/java/com/obsidianpay/mobile"
UI="$SRC/ui"
APP_GRADLE="$APP/app/build.gradle"
MANIFEST="$APP/app/src/main/AndroidManifest.xml"
MAINACT="$SRC/MainActivity.kt"
APIHOST="$UI/ApiHostOverrideScreen.kt"
WF="$REPO_ROOT/.github/workflows/lab08-android-apk.yml"
DOWNLOAD="DOWNLOAD.md"
PKG="scripts/package-android-apk.sh"

ARTIFACT_APK="ObsidianPay-Lab08-v1.0.0-rc2.apk"
ARTIFACT_NAME="obsidianpay-lab08-v1.0.0-rc2"
EXPECTED_BRANCH="lab-08-mobile-obsidianpay"

pass() { printf '  \033[32m[PASS]\033[0m %s\n' "$1"; }
warn() { printf '  \033[33m[WARN]\033[0m %s\n' "$1"; }
info() { printf '\033[36m[*]\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m[FAIL]\033[0m %s\n' "$1" >&2; FAILED=1; }

FAILED=0

need_file() { if [ -f "$1" ]; then pass "arquivo: $1"; else fail "ausente: $1"; fi; }

# O '--' permite padrões que começam com '-'.
need_grep_file() {
  if [ -f "$2" ] && grep -qF -- "$1" "$2"; then pass "$3"; else fail "$3 (esperado '$1' em $2)"; fi
}
need_grep_re_file() {
  if [ -f "$2" ] && grep -Eq -- "$1" "$2"; then pass "$3"; else fail "$3 (esperado /$1/ em $2)"; fi
}
reject_grep_file() {
  if [ -f "$2" ] && grep -qF -- "$1" "$2"; then fail "$3 (string '$1' em $2)"; else pass "$3"; fi
}
need_grep_tree() {
  if grep -rqF -- "$1" "$2" 2>/dev/null; then pass "$3"; else fail "$3 (esperado '$1' em $2)"; fi
}
reject_grep_tree() {
  if grep -rqF -- "$1" "$2" 2>/dev/null; then fail "$3 (string '$1' em $2)"; else pass "$3"; fi
}

info "Diretório do lab:  $LAB_DIR"
info "Raiz do repo:      $REPO_ROOT"

# --- 1. Branch correta -------------------------------------------------------
info "1. Conferindo branch..."
CUR_BRANCH="$(git -C "$REPO_ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo "?")"
if [ "$CUR_BRANCH" = "$EXPECTED_BRANCH" ]; then pass "branch atual: $CUR_BRANCH"
else warn "branch atual '$CUR_BRANCH' != '$EXPECTED_BRANCH' (siga apenas se for intencional)"; fi

# --- 2. Labs 1..7 intactos ---------------------------------------------------
info "2. Verificando que labs 1..7 não foram alterados (conteúdo)..."
LAB_CHANGED="$( {
  git -C "$REPO_ROOT" log origin/main..HEAD --name-only --format='' 2>/dev/null
  git -C "$REPO_ROOT" diff --name-only -G'.' 2>/dev/null
  git -C "$REPO_ROOT" diff --cached --name-only -G'.' 2>/dev/null
} | sort -u )"
for n in 1 2 3 4 5 6 7; do
  if printf '%s\n' "$LAB_CHANGED" | grep -q "lab-0${n}-"; then fail "lab-0${n} alterado (conteúdo)"; else pass "lab-0${n} não alterado"; fi
done

# --- 3. Arquivos Android exigidos --------------------------------------------
info "3. Conferindo arquivos Android exigidos (incl. redesign)..."
for f in \
  "$MAINACT" \
  "$SRC/ui/theme/Color.kt" \
  "$SRC/ui/theme/Theme.kt" \
  "$SRC/ui/theme/Type.kt" \
  "$SRC/ui/components/ObsidianComponents.kt" \
  "$UI/LoginScreen.kt" "$UI/HomeScreen.kt" "$UI/TransferPreviewScreen.kt" \
  "$UI/CardsScreen.kt" "$UI/SecurityCenterScreen.kt" "$UI/VaultScreen.kt" \
  "$UI/SupportScreen.kt" "$UI/WebViewSupportScreen.kt" "$UI/MoreScreen.kt" \
  "$UI/SettingsScreen.kt" "$UI/ApiHostOverrideScreen.kt" "$UI/ReceiptsScreen.kt" \
  "$UI/QrInputScreen.kt" "$UI/LocalStateScreen.kt" "$UI/DeviceTrustScreen.kt" \
  "$UI/IntegrityScreen.kt" "$UI/SecurityCheckScreen.kt" \
  "$APP_GRADLE" "$MANIFEST" ; do
  need_file "$f"
done

# --- 4. Package IDs ----------------------------------------------------------
info "4. Conferindo package IDs (debug/release)..."
need_grep_re_file "applicationId 'com\.obsidianpay\.mobile'" "$APP_GRADLE" "release applicationId com.obsidianpay.mobile"
need_grep_re_file "applicationIdSuffix '\.debug'" "$APP_GRADLE" "debug applicationIdSuffix .debug"
need_grep_re_file "namespace 'com\.obsidianpay\.mobile'" "$APP_GRADLE" "namespace com.obsidianpay.mobile"

# --- 5/6/7. SDK levels -------------------------------------------------------
info "5/6/7. Conferindo compileSdk/targetSdk/minSdk..."
need_grep_re_file 'compileSdk 34' "$APP_GRADLE" "compileSdk 34"
need_grep_re_file 'targetSdk 34'  "$APP_GRADLE" "targetSdk 34"
need_grep_re_file 'minSdk 24'     "$APP_GRADLE" "minSdk 24"

# --- 8/9. Versionamento RC2 --------------------------------------------------
info "8/9. Conferindo versionCode/versionName RC2..."
need_grep_re_file 'versionCode 2'        "$APP_GRADLE" "versionCode 2"
need_grep_file    "versionName '1.0.0-rc2'" "$APP_GRADLE" "versionName 1.0.0-rc2"
reject_grep_file  "0.3.0-phase3"         "$APP_GRADLE" "sem versionName antigo (0.3.0-phase3)"

# --- 10. Sem default de runtime 8080 -----------------------------------------
info "10. Verificando ausência de default de runtime na porta 8080..."
if grep -rnq -- ':8080' "$APP/app/src" 2>/dev/null; then
  fail "encontrado ':8080' no código Android (default de runtime indevido)"
else
  pass "nenhum default de runtime 8080 no código Android"
fi

# --- 11/12. Presets de conexão -----------------------------------------------
info "11/12. Conferindo presets de conexão (físico/emulador)..."
need_grep_file '127.0.0.1:8102' "$APIHOST" "preset dispositivo físico usa 127.0.0.1:8102"
need_grep_file '10.0.2.2:8102'  "$APIHOST" "preset emulador usa 10.0.2.2:8102"
need_grep_file 'adb reverse'    "$APIHOST" "tela de conexão menciona adb reverse"

# --- 13. Workflow gera nomes RC2 ---------------------------------------------
info "13. Conferindo nomes RC2 no workflow..."
need_grep_file "$ARTIFACT_APK"  "$WF" "workflow gera $ARTIFACT_APK"
need_grep_file "$ARTIFACT_NAME" "$WF" "workflow usa o artefato $ARTIFACT_NAME"
reject_grep_file "v1.0.0-rc1"   "$WF" "workflow sem nomes rc1 remanescentes"

# --- 14. Packaging gera nomes RC2 --------------------------------------------
info "14. Conferindo nomes RC2 no packaging..."
need_grep_file "$ARTIFACT_APK" "$PKG" "packaging gera $ARTIFACT_APK"
reject_grep_file "v1.0.0-rc1"  "$PKG" "packaging sem nomes rc1 remanescentes"

# --- 15. DOWNLOAD.md consistente em RC2 --------------------------------------
info "15. Conferindo DOWNLOAD.md (RC2 consistente)..."
need_grep_file "$ARTIFACT_APK"          "$DOWNLOAD" "DOWNLOAD cita o APK RC2"
need_grep_file "$ARTIFACT_APK.sha256"   "$DOWNLOAD" "DOWNLOAD cita o checksum RC2"
need_grep_file "$ARTIFACT_NAME"         "$DOWNLOAD" "DOWNLOAD cita o artefato RC2"
reject_grep_file "v1.0.0-rc1"           "$DOWNLOAD" "DOWNLOAD sem nomes rc1 remanescentes"
need_grep_file 'adb reverse tcp:8102 tcp:8102' "$DOWNLOAD" "DOWNLOAD documenta adb reverse 8102"

# --- 16/17. Nenhum APK rastreado/presente ------------------------------------
info "16/17. Conferindo ausência de APK no Git/working tree..."
APK_TRACKED="$(git -C "$REPO_ROOT" ls-files '*.apk' 2>/dev/null)"
if [ -z "$APK_TRACKED" ]; then pass "nenhum .apk rastreado pelo Git"; else fail "APK(s) rastreado(s): $APK_TRACKED"; fi
APK_STATUS="$(git -C "$REPO_ROOT" status --porcelain 2>/dev/null | grep -E '\.apk( |$)' || true)"
if [ -z "$APK_STATUS" ]; then pass "nenhum .apk pendente para commit"; else fail "APK pendente no status: $APK_STATUS"; fi
need_grep_file 'ObsidianPay-Lab08-*.apk' "$REPO_ROOT/.gitignore" ".gitignore ignora os APKs renomeados"

# --- 18/19. Anti-leak (docs públicos) ----------------------------------------
info "18/19. Anti-leak: sem FLAG{ e sem credenciais privadas em material público..."
PUBLIC=(
  "README.md" "STUDENT-GUIDE.md" "DOWNLOAD.md" "android-app/README.md"
  "docs/ANDROID-BUILD-CHECKLIST.md" "docs/FINAL-QA.md" "docs/mobile-pentest/SETUP.md"
  "$PKG"
)
for f in "${PUBLIC[@]}"; do
  if [ -e "$f" ]; then
    reject_grep_tree "FLAG{"       "$f" "sem FLAG{ em $f"
    reject_grep_tree "analyst123"  "$f" "sem analyst123 em $f"
    reject_grep_tree "operator123" "$f" "sem operator123 em $f"
  else
    warn "ausente (pulando): $f"
  fi
done
reject_grep_tree "FLAG{" "$WF" "sem FLAG{ no workflow"
# A flag 03 não pode existir no app Android (APK).
reject_grep_tree "FLAG{obsidianpay_exported_components_03}" "$APP" "flag 03 NÃO está no app Android"

# --- 20/21/22/23. Manifest: exported + launcher + deep link + ações ----------
info "20-23. Conferindo manifest (exportados/launcher/deep link/ações sensíveis)..."
need_grep_file '.platform.InternalOpsActivity'   "$MANIFEST" "Activity exportada InternalOpsActivity presente"
need_grep_file '.platform.DebugCommandReceiver'  "$MANIFEST" "Receiver exportado DebugCommandReceiver presente"
need_grep_file '.platform.ObsidianNotesProvider' "$MANIFEST" "Provider exportado ObsidianNotesProvider presente"
need_grep_file 'android:exported="true"'         "$MANIFEST" "há componentes exportados (exported=true)"
need_grep_file 'android.intent.action.MAIN'      "$MANIFEST" "intent MAIN presente"
need_grep_file 'android.intent.category.LAUNCHER' "$MANIFEST" "category LAUNCHER presente"
need_grep_file 'android:scheme="obsidianpay"'    "$MANIFEST" "deep-link scheme obsidianpay presente"
need_grep_file 'com.obsidianpay.mobile.INTERNAL_OPS'  "$MANIFEST" "ação INTERNAL_OPS presente"
need_grep_file 'com.obsidianpay.mobile.DEBUG_COMMAND' "$MANIFEST" "ação DEBUG_COMMAND presente"
need_grep_file 'com.obsidianpay.mobile.provider.notes' "$MANIFEST" "authority provider.notes presente"

# --- 24. WebView bridge ------------------------------------------------------
info "24. Conferindo WebView bridge (intencionalmente vulnerável)..."
need_file "$SRC/webview/ObsidianSupportBridge.kt"
need_grep_file 'addJavascriptInterface' "$UI/WebViewSupportScreen.kt" "WebView anexa o bridge JS"
need_grep_file 'ObsidianBridge'          "$UI/WebViewSupportScreen.kt" "bridge exposto como ObsidianBridge"
need_grep_file 'javaScriptEnabled = true' "$UI/WebViewSupportScreen.kt" "JS habilitado no WebView (intencional)"
# Contrato de host efetivo preservado (Fase 20).
need_grep_file 'apiClient.getBaseUrl()'  "$UI/WebViewSupportScreen.kt" "WebView usa a base URL efetiva"
need_grep_file 'NetworkSecurityProfile.joinUrl' "$UI/WebViewSupportScreen.kt" "WebView normaliza a URL (joinUrl)"

# --- 25. Storage inseguro ----------------------------------------------------
info "25. Conferindo armazenamento inseguro (intencional)..."
need_file "$SRC/storage/InsecureSessionStore.kt"
need_grep_file 'getSharedPreferences' "$SRC/storage/InsecureSessionStore.kt" "usa SharedPreferences em texto puro"
need_grep_re_file 'fun getToken\(\)'  "$SRC/storage/InsecureSessionStore.kt" "getToken() preservado (sem clash JVM)"

# --- 26. Biometria -----------------------------------------------------------
info "26. Conferindo biometria/local-auth (bypass intencional)..."
need_file "$SRC/auth/BiometricGate.kt"
need_file "$SRC/auth/LocalAuthState.kt"
need_grep_file 'validateFallbackPin' "$SRC/auth/LocalAuthState.kt" "PIN fraco/fallback preservado"
need_grep_file 'biometric-result-hook' "$UI/VaultScreen.kt" "alvo de hook biométrico preservado no Vault"

# --- 27. Root/emulator -------------------------------------------------------
info "27. Conferindo detecção de root/emulador (contornável)..."
need_file "$SRC/environment/RootDetector.kt"
need_file "$SRC/environment/EmulatorDetector.kt"
need_grep_file '/system_ext/bin/su' "$SRC/environment/RootDetector.kt" "RootDetector verifica /system_ext/bin/su"

# --- 28. Rede / pinning ------------------------------------------------------
info "28. Conferindo rede / pinning (scaffold)..."
need_file "$SRC/network/NetworkSecurityProfile.kt"
need_file "$SRC/network/PinningPolicy.kt"

# --- 29. Telas/destinos redesenhados existem ---------------------------------
info "29. Conferindo telas e navegação redesenhadas..."
need_grep_file 'ObsidianPayTheme'  "$MAINACT" "MainActivity aplica o tema ObsidianPayTheme"
need_grep_file 'NavigationBar'     "$MAINACT" "bottom navigation (NavigationBar) presente"
for tab in 'Início' 'Transferir' 'Cartões' 'Segurança' 'Conta'; do
  need_grep_file "$tab" "$MAINACT" "aba de navegação '$tab' presente"
done
need_grep_file 'fun SecurityCenterScreen' "$UI/SecurityCenterScreen.kt" "Security Center existe"
need_grep_file 'fun MoreScreen'           "$UI/MoreScreen.kt" "tela Conta/More existe"
need_grep_file 'fun SettingsScreen'       "$UI/SettingsScreen.kt" "tela Configurações existe"
# Contrato Fase 20 preservado em MainActivity (host efetivo, sem scroll aninhado).
need_grep_file 'effectiveBaseUrl(store.getApiBaseUrlOverride())' "$MAINACT" "override de host recuperado no startup"
reject_grep_file 'import androidx.compose.foundation.verticalScroll' "$MAINACT" "MainActivity sem verticalScroll (sem scroll aninhado)"

# --- 31. Consistência do Gradle ----------------------------------------------
info "31. Conferindo consistência do Gradle..."
need_grep_file "material-icons-extended" "$APP_GRADLE" "dependência de ícones Material disponível"
need_grep_re_file "compose true" "$APP_GRADLE" "Compose habilitado"

# --- 30/33. bash -n + git diff --check ---------------------------------------
info "30. Conferindo sintaxe (bash -n) dos scripts criados/modificados..."
for s in "scripts/validate-phase23.sh" "$PKG"; do
  if bash -n "$s" 2>/dev/null; then pass "bash -n OK: $s"; else fail "bash -n FALHOU: $s"; fi
done

info "33. Conferindo git diff --check..."
if git -C "$REPO_ROOT" diff --check >/dev/null 2>&1; then pass "git diff --check sem problemas"; else fail "git diff --check reportou problemas"; fi

# --- (opcional) Build Android real -------------------------------------------
info "Build Android (opcional, se SDK disponível)..."
SDK_DIR=""
if [ -n "${ANDROID_SDK_ROOT:-}" ]; then SDK_DIR="$ANDROID_SDK_ROOT";
elif [ -n "${ANDROID_HOME:-}" ]; then SDK_DIR="$ANDROID_HOME";
elif [ -f "$APP/local.properties" ] && grep -q '^sdk.dir=' "$APP/local.properties"; then
  SDK_DIR="$(sed -n 's/^sdk.dir=//p' "$APP/local.properties" | head -1)"
fi
if [ -n "$SDK_DIR" ] && [ -d "$SDK_DIR" ]; then
  info "Android SDK em: $SDK_DIR — executando assembleDebug."
  if ( cd "$APP" && ./gradlew --no-daemon clean :app:assembleDebug ); then
    APK_OUT="$APP/app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_OUT" ]; then pass "BUILD SUCCESSFUL — APK em $APK_OUT ($(du -h "$APK_OUT" | cut -f1))"; else fail "build OK mas APK ausente em $APK_OUT"; fi
  else
    fail "assembleDebug FALHOU com SDK disponível"
  fi
else
  warn "Android SDK não disponível neste host — build real NÃO executado (execute em ambiente com SDK)."
fi

# --- 32. Regressões (Fases 18/19/20/22; a 22 encadeia 18/19/20) --------------
info "32. Executando regressões (validate-phase22.sh encadeia 18/19/20)..."
for n in 18 19 20 22; do
  need_file "scripts/validate-phase$n.sh"
done
if [ -f "scripts/validate-phase22.sh" ]; then
  if bash scripts/validate-phase22.sh; then pass "validate-phase22.sh passou (com 18/19/20)"; else fail "validate-phase22.sh falhou"; fi
else
  fail "scripts/validate-phase22.sh ausente"
fi

# --- Resultado ---------------------------------------------------------------
echo ""
if [ "$FAILED" -eq 0 ]; then
  printf '\033[32m[OK] Fase 23A validada (redesign de UI + rede + RC2).\033[0m\n'
  exit 0
else
  printf '\033[31m[FAIL] Fase 23A: uma ou mais verificações falharam.\033[0m\n' >&2
  exit 1
fi
