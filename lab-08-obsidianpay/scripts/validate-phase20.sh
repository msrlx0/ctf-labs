#!/usr/bin/env bash
#
# validate-phase20.sh — valida a Fase 20 do Lab 08 (ObsidianPay Mobile):
# estabilização de runtime + build Android real + Stage 03 solucionável.
#
# A Fase 20 corrige 6 problemas que só aparecem em build/execução real:
#   1. clash de assinatura JVM em InsecureSessionStore.kt (quebra a compilação);
#   2. Stage 03 sem caminho real de entrega da flag (checkpoint backend);
#   3. WebView com base URL fixa (não funciona em celular físico);
#   4. crash da tela "Configuração" (scroll vertical aninhado infinito);
#   5. RootDetector que não detecta root básico real (ex.: /system_ext/bin/su);
#   6. /health exibindo "0.2.0-phase2" apesar de já haver 9 stages.
#
# Este script valida:
#   A. Compilação real (quando o Android SDK está disponível) — build é a
#      validação PRINCIPAL do clash JVM; sem SDK, emite AVISO claro.
#   B. Clash JVM — ausência de propriedades Kotlin que colidem com getters.
#   C. Stage 03 — checkpoint dinâmico (auth, payload incompleto/incorreto/correto,
#      flag só no sucesso, submit, 200 pontos, idempotência).
#   D. WebView — usa a base URL efetiva (sem base fixa independente).
#   E. Configuração — sem scroll vertical aninhado conhecido (+ teste manual).
#   F. RootDetector — caminhos comuns (/system_ext/bin/su, /data/adb/magisk).
#   G. /health — versão != 0.2.0-phase2, 9 stages, HTTP 200.
#   H. Regressões — roda validate-phase18.sh e validate-phase19.sh.
#   I. Anti-leak — sem flags em docs públicos; flag 03 só no backend.
#   J. Escopo — labs 1..7 intactos.
#
# Sai com exit 1 se qualquer verificação obrigatória falhar.
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$LAB_DIR"

BASE_URL="http://127.0.0.1:8102"

APP_DIR="android-app"
SRC="$APP_DIR/app/src/main/java/com/obsidianpay/mobile"
STORE="$SRC/storage/InsecureSessionStore.kt"
WEBVIEW="$SRC/ui/WebViewSupportScreen.kt"
MAINACT="$SRC/MainActivity.kt"
ROOTDET="$SRC/environment/RootDetector.kt"
PROVIDER="$SRC/platform/ObsidianNotesProvider.kt"

STAGE03_FLAG="FLAG{obsidianpay_exported_components_03}"

pass() { printf '  \033[32m[PASS]\033[0m %s\n' "$1"; }
warn() { printf '  \033[33m[WARN]\033[0m %s\n' "$1"; }
info() { printf '\033[36m[*]\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m[FAIL]\033[0m %s\n' "$1" >&2; FAILED=1; }

FAILED=0
BUILD_VERIFIED=0

need_file() { if [ -f "$1" ]; then pass "arquivo: $1"; else fail "ausente: $1"; fi; }

need_grep_file() {
  if [ -f "$2" ] && grep -qF "$1" "$2"; then pass "$3"; else fail "$3 (esperado '$1' em $2)"; fi
}

need_grep_re_file() {
  if [ -f "$2" ] && grep -Eq "$1" "$2"; then pass "$3"; else fail "$3 (esperado /$1/ em $2)"; fi
}

reject_grep_re_file() {
  if [ -f "$2" ] && grep -Eq "$1" "$2"; then
    fail "$3 (padrão /$1/ encontrado em $2)"
  else
    pass "$3"
  fi
}

reject_grep_file() {
  if [ -f "$2" ] && grep -qF "$1" "$2"; then
    fail "$3 (string '$1' encontrada em $2)"
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

# JSON helper: lê stdin (JSON) e imprime o valor de um caminho com pontos.
jget() {
  node -e 'let d="";process.stdin.on("data",c=>d+=c).on("end",()=>{try{let o=JSON.parse(d);for(const p of process.argv[1].split("."))o=(o==null?undefined:o[p]);console.log(o==null?"":o)}catch(e){console.log("")}})' "$1"
}

http_code() {
  # $1 = url ; demais = args extras do curl
  local url="$1"; shift
  curl -s -o /dev/null -w '%{http_code}' "$@" "$url"
}

info "Diretório do lab: $LAB_DIR"

# --- A. Compilação real (build é a validação principal do clash JVM) ---------
info "A. Compilação Android real (assembleDebug)..."
SDK_DIR=""
if [ -n "${ANDROID_SDK_ROOT:-}" ]; then SDK_DIR="$ANDROID_SDK_ROOT";
elif [ -n "${ANDROID_HOME:-}" ]; then SDK_DIR="$ANDROID_HOME";
elif [ -f "$APP_DIR/local.properties" ] && grep -q '^sdk.dir=' "$APP_DIR/local.properties"; then
  SDK_DIR="$(sed -n 's/^sdk.dir=//p' "$APP_DIR/local.properties" | head -1)"
fi

if [ -n "$SDK_DIR" ] && [ -d "$SDK_DIR" ]; then
  info "Android SDK encontrado em: $SDK_DIR — executando build real."
  if ( cd "$APP_DIR" && ./gradlew --no-daemon clean :app:assembleDebug ); then
    APK="$APP_DIR/app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK" ]; then
      pass "BUILD SUCCESSFUL — APK gerado ($(du -h "$APK" | cut -f1)): $APK"
      BUILD_VERIFIED=1
    else
      fail "Build retornou sucesso mas o APK não foi encontrado em $APK"
    fi
  else
    fail "assembleDebug FALHOU com o SDK disponível (corrija a compilação)."
  fi
else
  warn "Android SDK não disponível neste host — build real NÃO executado."
  warn "A Fase 20 NÃO está totalmente aprovada sem evidência externa de 'BUILD SUCCESSFUL'."
  warn "Execute em ambiente com SDK: (cd $APP_DIR && ./gradlew --no-daemon clean :app:assembleDebug)"
fi

# --- B. Clash JVM (estrutural; o build real é a prova definitiva) ------------
info "B. Verificando ausência de clash JVM em InsecureSessionStore.kt..."
need_file "$STORE"
# Os getters de sessão devem existir como MÉTODOS explícitos.
need_grep_re_file 'fun getToken\(\)'    "$STORE" "getToken() existe como método"
need_grep_re_file 'fun getUsername\(\)' "$STORE" "getUsername() existe como método"
need_grep_re_file 'fun getRole\(\)'     "$STORE" "getRole() existe como método"
# E NÃO devem existir propriedades Kotlin val token/username/role, que gerariam
# getters JVM com a MESMA assinatura (platform declaration clash). Esta checagem
# por grep é secundária — a compilação real (seção A) é a validação principal.
reject_grep_re_file '^[[:space:]]*val[[:space:]]+token([[:space:]:]|$)'    "$STORE" "sem propriedade 'val token' (evita clash)"
reject_grep_re_file '^[[:space:]]*val[[:space:]]+username([[:space:]:]|$)' "$STORE" "sem propriedade 'val username' (evita clash)"
reject_grep_re_file '^[[:space:]]*val[[:space:]]+role([[:space:]:]|$)'     "$STORE" "sem propriedade 'val role' (evita clash)"
# Nenhum call site deve usar a propriedade removida (store.token/.username/.role).
if grep -rqnE '\bstore\.(token|username|role)\b' "$SRC" 2>/dev/null; then
  fail "ainda há call sites usando store.token/.username/.role (use getToken()/getUsername()/getRole())"
else
  pass "nenhum call site usa store.token/.username/.role"
fi

# --- D. WebView usa a base URL efetiva (estrutural) --------------------------
info "D. Verificando WebView (base URL efetiva, sem host fixo)..."
need_file "$WEBVIEW"
need_grep_re_file 'apiClient: ApiClient' "$WEBVIEW" "WebView recebe o ApiClient"
need_grep_file 'apiClient.getBaseUrl()'  "$WEBVIEW" "WebView usa a base URL efetiva (apiClient.getBaseUrl())"
need_grep_file 'NetworkSecurityProfile.joinUrl' "$WEBVIEW" "WebView normaliza/junta a URL (joinUrl)"
reject_grep_re_file 'Uri\.parse\(Constants\.DEFAULT_BASE_URL' "$WEBVIEW" "WebView NÃO monta URL a partir de Constants.DEFAULT_BASE_URL fixo"
# A regra "override ?: default" tem uma fonte única e o override é recuperado.
need_grep_file 'fun effectiveBaseUrl'    "$SRC/network/NetworkSecurityProfile.kt" "fonte única da base URL efetiva (effectiveBaseUrl)"
need_grep_file 'effectiveBaseUrl(store.getApiBaseUrlOverride())' "$MAINACT" "override recuperado no startup (effectiveBaseUrl + getApiBaseUrlOverride)"
need_grep_re_file 'onReceivedError' "$WEBVIEW" "WebView trata erro de carregamento (onReceivedError)"

# --- E. Configuração sem scroll vertical aninhado ----------------------------
info "E. Verificando ausência de scroll vertical aninhado (crash 'Configuração')..."
need_file "$MAINACT"
# ResponseBox (usado por todas as telas dentro de um Column.verticalScroll) NÃO
# pode possuir seu próprio verticalScroll — isso causa o crash de altura infinita.
reject_grep_re_file 'verticalScroll\(rememberScrollState\(\)\)' "$MAINACT" "ResponseBox/MainActivity sem verticalScroll próprio"
reject_grep_re_file '^import androidx\.compose\.foundation\.verticalScroll' "$MAINACT" "MainActivity não importa verticalScroll (não há scroll aninhado)"
warn "TESTE MANUAL OBRIGATÓRIO: abrir Home > 'Configuração', rolar até o fim, editar/salvar API Host, "
warn "  reabrir e confirmar persistência, em telas de tamanhos diferentes, sem crash (grep não é suficiente)."

# --- F. RootDetector detecta root básico visível -----------------------------
info "F. Verificando caminhos de root comuns no RootDetector.kt..."
need_file "$ROOTDET"
ROOT_PATHS=(
  "/system/bin/su"
  "/system/xbin/su"
  "/system_ext/bin/su"
  "/sbin/su"
  "/su/bin/su"
  "/vendor/bin/su"
  "/data/local/bin/su"
  "/data/local/xbin/su"
  "/data/adb/magisk"
  "/data/adb/modules"
)
for p in "${ROOT_PATHS[@]}"; do
  need_grep_file "$p" "$ROOTDET" "RootDetector verifica $p"
done
# Continua contornável (Frida/patch/ocultação) — apenas sinais, sem bloqueio.
need_grep_re_file 'bypass|Frida|patch'  "$ROOTDET" "RootDetector documenta que continua contornável"

# --- C+G. Testes dinâmicos (Stage 03 + /health) ------------------------------
info "C/G. Testes dinâmicos (backend): Stage 03 checkpoint + /health..."

DC=""
if docker compose version >/dev/null 2>&1; then DC="docker compose";
elif command -v docker-compose >/dev/null 2>&1; then DC="docker-compose"; fi

BACKEND_MODE=""
BACKEND_PID=""
start_backend() {
  if [ -n "$DC" ] && docker ps >/dev/null 2>&1; then
    if $DC up --build -d >/dev/null 2>&1; then BACKEND_MODE="docker"; return 0; fi
  fi
  if command -v node >/dev/null 2>&1; then
    if [ ! -d "api/node_modules" ]; then ( cd api && npm install --no-audit --no-fund >/dev/null 2>&1 ) || true; fi
    if [ -d "api/node_modules" ]; then
      ( cd api && PORT=8102 node src/server.js >/tmp/obsidian-phase20-api.log 2>&1 & echo $! >/tmp/obsidian-phase20-api.pid )
      BACKEND_PID="$(cat /tmp/obsidian-phase20-api.pid 2>/dev/null || true)"
      BACKEND_MODE="node"
      return 0
    fi
  fi
  return 1
}
stop_backend() {
  if [ "$BACKEND_MODE" = "docker" ]; then $DC down >/dev/null 2>&1 || true; fi
  if [ "$BACKEND_MODE" = "node" ] && [ -n "$BACKEND_PID" ]; then kill "$BACKEND_PID" >/dev/null 2>&1 || true; fi
}
trap stop_backend EXIT INT TERM

if start_backend; then
  pass "backend iniciado (modo: $BACKEND_MODE)"
  HEALTH=""
  for _ in $(seq 1 30); do
    if HEALTH="$(curl -fsS "$BASE_URL/health" 2>/dev/null)"; then break; fi
    sleep 1
  done

  if [ -n "$HEALTH" ]; then
    # G. /health
    [ "$(http_code "$BASE_URL/health")" = "200" ] && pass "/health HTTP 200" || fail "/health não respondeu 200"
    [ "$(echo "$HEALTH" | jget status)" = "ok" ] && pass "/health status=ok" || fail "/health status inesperado"
    HV="$(echo "$HEALTH" | jget version)"
    [ "$HV" != "0.2.0-phase2" ] && [ -n "$HV" ] && pass "/health version != 0.2.0-phase2 (=$HV)" || fail "/health ainda exibe versão antiga (=$HV)"
    [ "$(echo "$HEALTH" | jget challengeStages)" = "9" ] && pass "/health challengeStages=9" || fail "/health challengeStages != 9"

    # C. Stage 03 checkpoint
    TOKEN="$(curl -fsS -X POST "$BASE_URL/api/mobile/login" -H 'Content-Type: application/json' \
      -d '{"username":"guest","password":"guest123"}' | jget token)"
    if [ -n "$TOKEN" ]; then
      pass "login guest OK"
      CP="$BASE_URL/api/mobile/challenge/checkpoint/exported-components"
      GOOD='{"activityProof":"act:internal-ops:af83c1","receiverProof":"rcv:debug-command:7b21de","providerProof":"prv:notes-consolidated:5c90af"}'

      # 1. sem autenticação => 401
      [ "$(http_code "$CP" -X POST -H 'Content-Type: application/json' -d "$GOOD")" = "401" ] \
        && pass "Stage03: sem auth => 401" || fail "Stage03: sem auth deveria ser 401"

      # 2. payload vazio => 400
      [ "$(http_code "$CP" -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{}')" = "400" ] \
        && pass "Stage03: payload vazio => 400" || fail "Stage03: payload vazio deveria ser 400"

      # 3. apenas uma prova => 400
      [ "$(http_code "$CP" -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"activityProof":"act:internal-ops:af83c1"}')" = "400" ] \
        && pass "Stage03: uma prova só => 400" || fail "Stage03: uma prova só deveria ser 400"

      # 4. provas erradas => 403 e sem flag
      WRONG="$(curl -s -X POST "$CP" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"activityProof":"x","receiverProof":"y","providerProof":"z"}')"
      [ "$(http_code "$CP" -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"activityProof":"x","receiverProof":"y","providerProof":"z"}')" = "403" ] \
        && pass "Stage03: provas erradas => 403" || fail "Stage03: provas erradas deveria ser 403"
      case "$WRONG" in *"FLAG{"*) fail "Stage03: provas erradas VAZARAM flag";; *) pass "Stage03: provas erradas não vazam flag";; esac

      # 5. três provas corretas => flag 03
      OKR="$(curl -s -X POST "$CP" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "$GOOD")"
      F3="$(echo "$OKR" | jget exportedComponentsCheckpoint.flag)"
      [ "$F3" = "$STAGE03_FLAG" ] && pass "Stage03: provas corretas => flag 03" || fail "Stage03: provas corretas não retornaram a flag 03 (=$F3)"

      # 6. submit da flag 03 => accepted true
      if [ -n "$F3" ]; then
        SUB="$(curl -s -X POST "$BASE_URL/api/mobile/challenge/submit" -H "Authorization: Bearer $TOKEN" \
          -H 'Content-Type: application/json' -d "{\"stageId\":\"stage-03-exported-components\",\"flag\":\"$F3\",\"evidence\":\"exported-components\"}")"
        [ "$(echo "$SUB" | jget accepted)" = "true" ] && pass "Stage03: submit accepted=true" || fail "Stage03: submit não aceito"
        # 7. 200 pontos
        [ "$(echo "$SUB" | jget pointsAwarded)" = "200" ] && pass "Stage03: 200 pontos" || fail "Stage03: pontuação != 200"
        # 8. idempotência
        DUP="$(curl -s -X POST "$BASE_URL/api/mobile/challenge/submit" -H "Authorization: Bearer $TOKEN" \
          -H 'Content-Type: application/json' -d "{\"stageId\":\"stage-03-exported-components\",\"flag\":\"$F3\"}")"
        [ "$(echo "$DUP" | jget pointsAwarded)" = "0" ] && pass "Stage03: resubmit idempotente (0 pontos)" || fail "Stage03: resubmit duplicou pontos"
      fi
    else
      fail "login guest falhou — não foi possível testar o Stage 03"
    fi
  else
    fail "/health não respondeu a tempo"
  fi
  stop_backend
  BACKEND_MODE=""
else
  warn "Backend indisponível (sem Docker e sem node/deps) — testes dinâmicos do Stage 03 e /health PULADOS."
  warn "Execute com Docker ('docker compose up') ou node ('cd api && npm install && node src/server.js') para validá-los."
fi

# --- I. Anti-leak: flag 03 só no backend -------------------------------------
info "I. Anti-leak: flag 03 não vaza para APK/docs públicos..."
# Flag 03 deve existir SOMENTE no backend (flags.js).
need_grep_file "$STAGE03_FLAG" "api/src/flags.js" "flag 03 presente em api/src/flags.js (backend)"
reject_grep_tree "$STAGE03_FLAG" "$APP_DIR" "flag 03 NÃO está no app Android (APK)"
PUBLIC_DOCS=(
  "README.md"
  "STUDENT-GUIDE.md"
  "docs/ARCHITECTURE.md"
  "docs/PHASES.md"
  "docs/VULNERABILITY-ROADMAP.md"
  "docs/CHALLENGE-SCORING.md"
  "docs/FINAL-QA.md"
  "docs/ANDROID-BUILD-CHECKLIST.md"
)
for doc in "${PUBLIC_DOCS[@]}"; do
  if [ -e "$doc" ]; then reject_grep_tree "FLAG{" "$doc" "sem FLAG{ em $doc"; else warn "doc ausente (pulando): $doc"; fi
done

# --- H. Regressões (Fases 18 e 19) -------------------------------------------
info "H. Executando scripts/validate-phase18.sh..."
if [ -f "scripts/validate-phase18.sh" ]; then
  if bash scripts/validate-phase18.sh; then pass "validate-phase18.sh passou"; else fail "validate-phase18.sh falhou"; fi
else
  fail "scripts/validate-phase18.sh ausente"
fi

info "H. Executando scripts/validate-phase19.sh..."
if [ -f "scripts/validate-phase19.sh" ]; then
  if bash scripts/validate-phase19.sh; then pass "validate-phase19.sh passou"; else fail "validate-phase19.sh falhou"; fi
else
  fail "scripts/validate-phase19.sh ausente"
fi

# --- J. Nenhum lab 1..7 alterado ---------------------------------------------
info "J. Verificando que labs 1..7 não foram alterados..."
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

# --- Resultado ---------------------------------------------------------------
echo ""
if [ "$BUILD_VERIFIED" -eq 0 ]; then
  warn "Build Android real NÃO foi verificado neste host (SDK ausente)."
  warn "Conclua a Fase 20 com evidência de 'BUILD SUCCESSFUL' em ambiente com SDK (ver docs/ANDROID-BUILD-CHECKLIST.md)."
fi
if [ "$FAILED" -eq 0 ]; then
  printf '\033[32m[OK] Fase 20 validada (checagens estruturais/dinâmicas).\033[0m\n'
  exit 0
else
  printf '\033[31m[FAIL] Fase 20: uma ou mais verificações falharam.\033[0m\n' >&2
  exit 1
fi
