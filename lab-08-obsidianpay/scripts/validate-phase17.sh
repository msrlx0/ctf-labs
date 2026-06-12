#!/usr/bin/env bash
#
# validate-phase17.sh — valida a Fase 17 do Lab 08 (Android build readiness).
#
# A Fase 17 NÃO altera backend, app, flags nem endpoints da Fase 14: é o passe de
# preparação para o build real do APK no Android Studio. Este script faz uma
# inspeção estrutural forte do projeto Android (Gradle/Manifest/recursos/Kotlin)
# e, se houver Android SDK, tenta o build real do debug APK. Especificamente:
#
#   A. Confere que os scripts validate-phase1..16 existem.
#   B. Confere os arquivos Android obrigatórios (Gradle, Manifest, recursos,
#      classes-núcleo, todas as telas e os pacotes do app).
#   C. Confere Gradle/Manifest: namespace/applicationId, minSdk/targetSdk/
#      compileSdk, INTERNET, usesCleartextTraffic, networkSecurityConfig, os três
#      componentes exportados, a authority do provider, o scheme e os hosts.
#   D. Confere conteúdo Kotlin obrigatório (navegação, bridge, detectores,
#      auth/integrity/network).
#   E. Falha em typos críticos de Kotlin/Manifest/network-config conhecidos.
#   F. Reforça anti-leak: sem FLAG{ em docs públicos/tools.
#   G. Reforça ausência de credenciais internas (analyst123/operator123) em
#      material público.
#   H. Confere que nenhum lab 1..7 foi alterado (commits da branch + working tree,
#      ignorando ruído de modo de arquivo do mount Windows/WSL).
#   I. Roda scripts/validate-phase14.sh, validate-phase15.sh e validate-phase16.sh.
#   J. Build Android best-effort: se ./gradlew existir e o Android SDK for
#      detectado (ANDROID_HOME / ANDROID_SDK_ROOT / local.properties sdk.dir),
#      roda `./gradlew --no-daemon :app:assembleDebug` e FALHA se o build falhar.
#      Sem SDK, emite WARN e NÃO falha (build real é feito no Android Studio).
#
# Sai com exit 1 se qualquer verificação obrigatória falhar.
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$LAB_DIR"

APP="android-app"
SRC="$APP/app/src/main/java/com/obsidianpay/mobile"
MANIFEST="$APP/app/src/main/AndroidManifest.xml"
RES="$APP/app/src/main/res"
NSC="$RES/xml/network_security_config.xml"

pass() { printf '  \033[32m[PASS]\033[0m %s\n' "$1"; }
warn() { printf '  \033[33m[WARN]\033[0m %s\n' "$1"; }
info() { printf '\033[36m[*]\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m[FAIL]\033[0m %s\n' "$1" >&2; FAILED=1; }

FAILED=0

need_file() { if [ -f "$1" ]; then pass "arquivo: $1"; else fail "ausente: $1"; fi; }
need_dir()  { if [ -d "$1" ]; then pass "pacote: $1";  else fail "pacote ausente: $1"; fi; }

# Exige um literal (grep -F) em um único arquivo.
need_grep_file() {
  if [ -f "$2" ] && grep -qF "$1" "$2"; then pass "$3"; else fail "$3 (esperado '$1' em $2)"; fi
}

# Exige um padrão (regex) em um único arquivo.
need_grep_re_file() {
  if [ -f "$2" ] && grep -Eq "$1" "$2"; then pass "$3"; else fail "$3 (esperado /$1/ em $2)"; fi
}

# Exige um literal em qualquer arquivo de uma árvore (grep -rF).
need_grep_tree() {
  if grep -rqF "$1" "$2" 2>/dev/null; then pass "$3"; else fail "$3 (esperado '$1' em $2)"; fi
}

# Rejeita um literal numa árvore/arquivo.
reject_grep_tree() {
  if grep -rqF "$1" "$2" 2>/dev/null; then
    fail "$3 (string '$1' encontrada em $2)"
  else
    pass "$3"
  fi
}

# Rejeita um padrão (regex) numa árvore/arquivo.
reject_grep_re_tree() {
  if grep -rEq "$1" "$2" 2>/dev/null; then
    fail "$3 (padrão /$1/ encontrado em $2)"
  else
    pass "$3"
  fi
}

info "Diretório do lab: $LAB_DIR"

# --- A. Scripts de fases anteriores (1..16) ----------------------------------
info "A. Conferindo scripts de fases anteriores (1..16)..."
for n in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16; do need_file "scripts/validate-phase$n.sh"; done

# --- B. Arquivos Android obrigatórios ----------------------------------------
info "B. Conferindo arquivos Android obrigatórios (Gradle/Manifest/recursos)..."
need_file "$APP/settings.gradle"
need_file "$APP/build.gradle"
need_file "$APP/gradle.properties"
need_file "$APP/app/build.gradle"
need_file "$MANIFEST"
need_file "$NSC"
need_file "$RES/values/strings.xml"
need_file "$RES/values/colors.xml"
need_file "$RES/values/themes.xml"

info "B. Conferindo classes-núcleo do app..."
need_file "$SRC/MainActivity.kt"
need_file "$SRC/api/ApiClient.kt"
need_file "$SRC/api/ApiModels.kt"
need_file "$SRC/api/ApiResult.kt"
need_file "$SRC/storage/InsecureSessionStore.kt"

info "B. Conferindo todas as telas principais (ui/)..."
for screen in \
  LoginScreen HomeScreen ReceiptsScreen CardsScreen SupportScreen \
  TransferPreviewScreen LocalStateScreen QrInputScreen WebViewSupportScreen \
  DeviceTrustScreen SecurityCheckScreen VaultScreen ApiHostOverrideScreen \
  IntegrityScreen ; do
  need_file "$SRC/ui/$screen.kt"
done

info "B. Conferindo pacotes do app..."
for pkg in auth deeplink environment integrity network platform security storage webview ; do
  need_dir "$SRC/$pkg"
done

# --- C. Gradle / Manifest ----------------------------------------------------
info "C. Conferindo Gradle (namespace/applicationId, SDKs)..."
# namespace OU applicationId com.obsidianpay.mobile
if grep -Eq "(namespace|applicationId)[[:space:]]+['\"]com\.obsidianpay\.mobile['\"]" "$APP/app/build.gradle"; then
  pass "app/build.gradle define namespace/applicationId com.obsidianpay.mobile"
else
  fail "app/build.gradle sem namespace/applicationId com.obsidianpay.mobile"
fi
need_grep_re_file 'minSdk[[:space:]]+[0-9]+'     "$APP/app/build.gradle" "app/build.gradle define minSdk"
need_grep_re_file 'targetSdk[[:space:]]+[0-9]+'  "$APP/app/build.gradle" "app/build.gradle define targetSdk"
need_grep_re_file 'compileSdk[[:space:]]+[0-9]+' "$APP/app/build.gradle" "app/build.gradle define compileSdk"

info "C. Conferindo Manifest (permissões, cleartext, componentes)..."
need_grep_file "android.permission.INTERNET" "$MANIFEST" "Manifest declara permissão INTERNET"
need_grep_file "usesCleartextTraffic"        "$MANIFEST" "Manifest define usesCleartextTraffic"
need_grep_file "networkSecurityConfig"       "$MANIFEST" "Manifest define networkSecurityConfig"

need_grep_file ".platform.InternalOpsActivity"  "$MANIFEST" "Manifest declara InternalOpsActivity (exported)"
need_grep_file ".platform.DebugCommandReceiver" "$MANIFEST" "Manifest declara DebugCommandReceiver (exported)"
need_grep_file ".platform.ObsidianNotesProvider" "$MANIFEST" "Manifest declara ObsidianNotesProvider (exported)"
need_grep_file "com.obsidianpay.mobile.provider.notes" "$MANIFEST" "Manifest define authority provider.notes"

# Cada componente exportado deve, de fato, ter android:exported="true".
if grep -Eq 'android:exported="true"' "$MANIFEST"; then
  pass "Manifest contém componentes android:exported=\"true\""
else
  fail "Manifest sem android:exported=\"true\""
fi

info "C. Conferindo deep links (scheme/hosts)..."
need_grep_file 'android:scheme="obsidianpay"' "$MANIFEST" "Manifest define scheme obsidianpay"
need_grep_file 'android:host="transfer"' "$MANIFEST" "Manifest define host transfer"
need_grep_file 'android:host="support"'  "$MANIFEST" "Manifest define host support"
need_grep_file 'android:host="receipt"'  "$MANIFEST" "Manifest define host receipt"

# --- D. Conteúdo Kotlin obrigatório ------------------------------------------
info "D. Conferindo conteúdo Kotlin obrigatório..."
# MainActivity navega para todas as telas (referência a cada Composable).
for screen in \
  LoginScreen HomeScreen ReceiptsScreen CardsScreen SupportScreen \
  TransferPreviewScreen LocalStateScreen QrInputScreen WebViewSupportScreen \
  DeviceTrustScreen SecurityCheckScreen VaultScreen ApiHostOverrideScreen \
  IntegrityScreen ; do
  need_grep_file "$screen" "$SRC/MainActivity.kt" "MainActivity navega para $screen"
done

# HomeScreen contém botões principais.
need_grep_re_file 'Button\(' "$SRC/ui/HomeScreen.kt" "HomeScreen contém botões principais"

# ApiClient contém DEFAULT_BASE_URL/Constants e OkHttp.
need_grep_file "Constants"        "$SRC/api/ApiClient.kt" "ApiClient referencia Constants"
need_grep_file "DEFAULT_BASE_URL" "$SRC/api/ApiClient.kt" "ApiClient referencia DEFAULT_BASE_URL"
need_grep_re_file 'OkHttp'        "$SRC/api/ApiClient.kt" "ApiClient usa OkHttp"

# WebView / bridge.
need_grep_file "addJavascriptInterface" "$SRC/ui/WebViewSupportScreen.kt" "WebViewSupportScreen usa addJavascriptInterface"
need_grep_file "@JavascriptInterface"   "$SRC/webview/ObsidianSupportBridge.kt" "ObsidianSupportBridge tem @JavascriptInterface"

# Objetos de segurança / ambiente / auth / integridade / rede.
need_grep_tree "LegacyRequestSigner"     "$SRC/security"     "LegacyRequestSigner existe"
need_grep_tree "RootDetector"            "$SRC/environment"  "RootDetector existe"
need_grep_tree "EmulatorDetector"        "$SRC/environment"  "EmulatorDetector existe"
need_grep_tree "EnvironmentRiskEngine"   "$SRC/environment"  "EnvironmentRiskEngine existe"
need_grep_tree "LocalAuthState"          "$SRC/auth"         "LocalAuthState existe"
need_grep_tree "BiometricGate"           "$SRC/auth"         "BiometricGate existe"
need_grep_tree "NativeGate"              "$SRC/integrity"    "NativeGate existe"
need_grep_tree "TamperCheck"             "$SRC/integrity"    "TamperCheck existe"
need_grep_tree "PinningPolicy"           "$SRC/network"      "PinningPolicy existe"
need_grep_tree "NetworkSecurityProfile"  "$SRC/network"      "NetworkSecurityProfile existe"

# --- E. Typos críticos (código-fonte Android) --------------------------------
info "E. Verificando ausência de typos críticos..."
if [ -d "$SRC" ]; then
  # Literais seguros (não são substring das formas corretas):
  for t in \
    "network-config-cleartext-overrie" \
    "PinningPolicyy" \
    "TamperCheckk" \
    "getSessionSummay" \
    "webVieClient" \
    "WeakCryptosha1Hex" \
    "WeakCryptomd5Hex" \
    "apiClientsetBaseUrlForSession" \
    "getLastNativeGatStatus" ; do
    reject_grep_tree "$t" "$SRC" "sem typo '$t'"
  done
  # Também varre o network_security_config.xml para o typo de cleartext-override.
  reject_grep_tree "network-config-cleartext-overrie" "$NSC" "sem typo cleartext-override no network-security-config"
  # Formas que SÃO substring das corretas — usar âncora por regex:
  reject_grep_re_tree 'getSessionSummar\('   "$SRC" "sem typo 'getSessionSummar('"
  reject_grep_re_tree '@JavascriptInterfac$' "$SRC" "sem typo '@JavascriptInterfac'"
  reject_grep_re_tree 'LegacyRequestSigne\b' "$SRC" "sem typo 'LegacyRequestSigne'"
  # "App Integrit" sem 'y' — evita falso positivo com "App Integrity".
  reject_grep_re_tree 'App Integrit([^y]|$)' "$SRC" "sem typo 'App Integrit' (sem y)"
else
  warn "código-fonte Android ausente ($SRC) — pulando guards de typos (best-effort)"
fi

# --- F. Anti-leak: docs públicos / tools NÃO contêm FLAG{ --------------------
info "F. Verificando que docs públicos/tools NÃO contêm FLAG{..."
PUBLIC_DOCS=(
  "README.md"
  "STUDENT-GUIDE.md"
  "docs/ARCHITECTURE.md"
  "docs/PHASES.md"
  "docs/VULNERABILITY-ROADMAP.md"
  "docs/CHALLENGE-SCORING.md"
  "docs/FINAL-QA.md"
  "docs/ANDROID-BUILD-CHECKLIST.md"
  "docs/mobile-pentest/SETUP.md"
  "docs/mobile-pentest/PLAYBOOK.md"
  "android-app/README.md"
)
for doc in "${PUBLIC_DOCS[@]}"; do
  if [ -e "$doc" ]; then
    reject_grep_tree "FLAG{" "$doc" "sem FLAG{ em $doc"
  else
    warn "doc público ausente (pulando): $doc"
  fi
done
reject_grep_tree "FLAG{" "tools" "sem FLAG{ em tools/"

# --- G. Credenciais internas (analyst123/operator123) ------------------------
info "G. Verificando ausência de credenciais internas em material público..."
for doc in \
  README.md \
  STUDENT-GUIDE.md \
  android-app/README.md \
  docs/FINAL-QA.md \
  docs/ANDROID-BUILD-CHECKLIST.md ; do
  if [ -e "$doc" ]; then
    reject_grep_tree "analyst123"  "$doc" "sem analyst123 em $doc"
    reject_grep_tree "operator123" "$doc" "sem operator123 em $doc"
  fi
done
reject_grep_tree "analyst123"  "tools" "sem analyst123 em tools/"
reject_grep_tree "operator123" "tools" "sem operator123 em tools/"

# --- H. Nenhum lab 1..7 alterado ---------------------------------------------
# Combina duas visões robustas, ambas imunes ao ruído de modo de arquivo
# (100755->100644) do mount Windows/WSL:
#   - commits da branch (origin/main..HEAD);
#   - working tree apenas com mudança de CONTEÚDO (git diff -G'.' ignora
#     diffs só de modo).
info "H. Verificando que labs 1..7 não foram alterados..."
REPO_ROOT="$(git -C "$LAB_DIR" rev-parse --show-toplevel 2>/dev/null || echo "$LAB_DIR/..")"
LAB_CHANGED="$( {
  git -C "$REPO_ROOT" log origin/main..HEAD --name-only --format='' 2>/dev/null
  git -C "$REPO_ROOT" diff --name-only -G'.' 2>/dev/null
  git -C "$REPO_ROOT" diff --cached --name-only -G'.' 2>/dev/null
} | sort -u )"
for lab_num in 1 2 3 4 5 6 7; do
  if printf '%s\n' "$LAB_CHANGED" | grep -q "lab-0${lab_num}-"; then
    fail "lab-0${lab_num} foi alterado (conteúdo)"
  else
    pass "lab-0${lab_num} não alterado"
  fi
done

# --- I. Rodar validações anteriores (Fases 14, 15 e 16) ----------------------
info "I. Executando scripts/validate-phase14.sh..."
if [ -f "scripts/validate-phase14.sh" ]; then
  if bash scripts/validate-phase14.sh; then pass "validate-phase14.sh passou"; else fail "validate-phase14.sh falhou"; fi
else
  fail "scripts/validate-phase14.sh ausente"
fi

info "I. Executando scripts/validate-phase15.sh..."
if [ -f "scripts/validate-phase15.sh" ]; then
  if bash scripts/validate-phase15.sh; then pass "validate-phase15.sh passou"; else fail "validate-phase15.sh falhou"; fi
else
  fail "scripts/validate-phase15.sh ausente"
fi

info "I. Executando scripts/validate-phase16.sh..."
if [ -f "scripts/validate-phase16.sh" ]; then
  if bash scripts/validate-phase16.sh; then pass "validate-phase16.sh passou"; else fail "validate-phase16.sh falhou"; fi
else
  fail "scripts/validate-phase16.sh ausente"
fi

# --- J. Build Android best-effort --------------------------------------------
# Se o Android SDK for detectado, faz o build REAL do debug APK e FALHA se ele
# falhar. Sem SDK, emite WARN e NÃO falha (o build real é feito no Android Studio).
info "J. Build Android (best-effort; build real exige Android SDK)..."
SDK_DIR=""
if [ -n "${ANDROID_HOME:-}" ]; then SDK_DIR="$ANDROID_HOME"; fi
if [ -z "$SDK_DIR" ] && [ -n "${ANDROID_SDK_ROOT:-}" ]; then SDK_DIR="$ANDROID_SDK_ROOT"; fi
if [ -z "$SDK_DIR" ] && [ -f "$APP/local.properties" ]; then
  SDK_DIR="$(grep -E '^sdk\.dir=' "$APP/local.properties" 2>/dev/null | head -n1 | cut -d= -f2-)"
fi

if [ -f "$APP/gradlew" ] && [ -n "$SDK_DIR" ] && [ -d "$SDK_DIR" ]; then
  info "Android SDK detectado em '$SDK_DIR' — rodando ./gradlew --no-daemon :app:assembleDebug..."
  GRADLEW="./gradlew"
  [ -x "$APP/gradlew" ] || GRADLEW="sh ./gradlew"
  if ( cd "$APP" && $GRADLEW --no-daemon :app:assembleDebug ); then
    pass ":app:assembleDebug concluiu (debug APK gerado)"
  else
    fail ":app:assembleDebug falhou — corrija os erros de build antes do release"
  fi
else
  warn "Android SDK não detectado — build real deve ser feito no Android Studio conforme docs/ANDROID-BUILD-CHECKLIST.md"
fi

# --- Resultado ---------------------------------------------------------------
echo ""
if [ "$FAILED" -eq 0 ]; then
  printf '\033[32m[OK] Fase 17 validada com sucesso.\033[0m\n'
  exit 0
else
  printf '\033[31m[FAIL] Fase 17: uma ou mais verificações falharam.\033[0m\n' >&2
  exit 1
fi
