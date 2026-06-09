#!/usr/bin/env bash
#
# validate-phase3.sh — valida a estrutura da Fase 3 do Lab 08 (app Android base).
#
# - Verifica a presença dos arquivos Android obrigatórios e alguns conteúdos-chave.
# - Confere que os scripts de backend (fase 1/2) existem.
# - Opcional: roda os testes de backend com RUN_BACKEND_TESTS=1.
# - Opcional: tenta o build se houver Gradle wrapper/Gradle + Android SDK.
# - NÃO falha apenas por ausência de Android SDK/Gradle: apenas avisa.
# - Falha (exit 1) se arquivos obrigatórios estiverem ausentes.
#
# Uso:
#   bash scripts/validate-phase3.sh
#   RUN_BACKEND_TESTS=1 bash scripts/validate-phase3.sh
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$LAB_DIR"
APP="android-app"

pass() { printf '  \033[32m[PASS]\033[0m %s\n' "$1"; }
warn() { printf '  \033[33m[WARN]\033[0m %s\n' "$1"; }
info() { printf '\033[36m[*]\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m[FAIL]\033[0m %s\n' "$1" >&2; FAILED=1; }

FAILED=0

need_file() {
  if [ -f "$1" ]; then pass "arquivo: $1"; else fail "ausente: $1"; fi
}

need_grep() {
  # $1 = pattern, $2 = file, $3 = descrição
  if [ -f "$2" ] && grep -q "$1" "$2"; then
    pass "$3"
  else
    fail "$3 (esperado '$1' em $2)"
  fi
}

info "Diretório do lab: $LAB_DIR"

# --- Scripts de backend ------------------------------------------------------
info "Conferindo scripts de backend (fase 1/2)..."
need_file "scripts/validate-phase1.sh"
need_file "scripts/validate-phase2.sh"

# --- Arquivos Gradle ---------------------------------------------------------
info "Conferindo arquivos Gradle..."
need_file "$APP/settings.gradle"
need_file "$APP/build.gradle"
need_file "$APP/gradle.properties"
need_file "$APP/app/build.gradle"
need_file "$APP/app/proguard-rules.pro"

# --- Manifest + recursos -----------------------------------------------------
info "Conferindo Manifest e recursos..."
need_file "$APP/app/src/main/AndroidManifest.xml"
need_file "$APP/app/src/main/res/xml/network_security_config.xml"
need_file "$APP/app/src/main/res/values/strings.xml"
need_file "$APP/app/src/main/res/values/colors.xml"
need_file "$APP/app/src/main/res/values/themes.xml"

# --- Kotlin: núcleo ----------------------------------------------------------
info "Conferindo fontes Kotlin (núcleo)..."
SRC="$APP/app/src/main/java/com/obsidianpay/mobile"
need_file "$SRC/MainActivity.kt"
need_file "$SRC/util/Constants.kt"
need_file "$SRC/api/ApiClient.kt"
need_file "$SRC/api/ApiModels.kt"
need_file "$SRC/api/ApiResult.kt"
need_file "$SRC/storage/InsecureSessionStore.kt"

# --- Kotlin: telas -----------------------------------------------------------
info "Conferindo telas (UI)..."
for screen in LoginScreen HomeScreen ReceiptsScreen CardsScreen SupportScreen TransferPreviewScreen; do
  need_file "$SRC/ui/$screen.kt"
done

# --- Conteúdos-chave ---------------------------------------------------------
info "Conferindo conteúdos-chave..."
need_grep "android.permission.INTERNET" "$APP/app/src/main/AndroidManifest.xml" "Manifest declara INTERNET"
need_grep "10.0.2.2" "$APP/app/src/main/res/xml/network_security_config.xml" "network_security_config cobre 10.0.2.2"
need_grep "http://10.0.2.2:8102" "$SRC/util/Constants.kt" "Constants.kt define base URL 10.0.2.2:8102"
need_grep "SharedPreferences" "$SRC/storage/InsecureSessionStore.kt" "InsecureSessionStore usa SharedPreferences"
if [ -f "$SRC/api/ApiClient.kt" ] && grep -qi "okhttp" "$SRC/api/ApiClient.kt"; then
  pass "ApiClient.kt menciona OkHttp"
else
  fail "ApiClient.kt deveria mencionar OkHttp"
fi

# --- Guards de segurança dos docs públicos -----------------------------------
info "Conferindo docs públicos (sem FLAG{ / sem credenciais internas)..."
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

# --- Backend (opcional) ------------------------------------------------------
if [ "${RUN_BACKEND_TESTS:-0}" = "1" ]; then
  info "RUN_BACKEND_TESTS=1: executando validações de backend..."
  bash scripts/validate-phase1.sh || fail "validate-phase1.sh falhou"
  bash scripts/validate-phase2.sh || fail "validate-phase2.sh falhou"
else
  warn "Pulando testes de backend (defina RUN_BACKEND_TESTS=1 para executá-los)."
fi

# --- Build Android (opcional, best-effort) -----------------------------------
info "Tentando validar o build Android (best-effort)..."
HAS_SDK=0
if [ -n "${ANDROID_HOME:-}" ] || [ -n "${ANDROID_SDK_ROOT:-}" ] || [ -f "$APP/local.properties" ]; then
  HAS_SDK=1
fi

if [ -x "$APP/gradlew" ] && [ -f "$APP/gradle/wrapper/gradle-wrapper.jar" ]; then
  if [ "$HAS_SDK" = "1" ]; then
    info "Gradle wrapper + Android SDK detectados: rodando assembleDebug..."
    ( cd "$APP" && ./gradlew --console=plain :app:assembleDebug ) \
      && pass "assembleDebug concluído" \
      || fail "assembleDebug falhou (verifique SDK/dependências)"
  else
    warn "Gradle wrapper presente, mas Android SDK não detectado."
    warn "Build do APK não executado. Use Android Studio ou defina ANDROID_HOME/local.properties."
  fi
elif command -v gradle >/dev/null 2>&1; then
  warn "gradle disponível, mas sem wrapper jar. Rode 'gradle wrapper --gradle-version 8.7' no app."
else
  warn "Sem Gradle wrapper jar e sem 'gradle' no PATH: build via Android Studio."
fi

# --- Resultado ---------------------------------------------------------------
echo
if [ "$FAILED" = "0" ]; then
  printf '\033[32m==> Fase 3 validada com sucesso (estrutura).\033[0m\n'
  exit 0
else
  printf '\033[31m==> Fase 3: há falhas obrigatórias acima.\033[0m\n'
  exit 1
fi
