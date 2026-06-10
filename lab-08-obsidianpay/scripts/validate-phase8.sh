#!/usr/bin/env bash
#
# validate-phase8.sh — valida a Fase 8 do Lab 08 (hardcoded secrets / weak crypto
# / weak request signing / device-trust / trilha de reverse engineering).
#
# - Verifica scripts de fases anteriores (phase1..phase7).
# - Verifica os arquivos novos do pacote security/ e a tela DeviceTrustScreen.
# - Grepa o código por constantes/métodos/headers/eventos da fase.
# - Confere o backend (device-trust, reverse-hint, assinatura fraca).
# - Reforça as checagens de typos de bridge/WebView (regex com limites).
# - Guards de docs públicos e das novas classes (sem FLAG{ / sem credenciais).
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
SECURITY="$SRC/security"
DEVICE_TRUST_SCREEN="$SRC/ui/DeviceTrustScreen.kt"
BRIDGE="$SRC/webview/ObsidianSupportBridge.kt"
WEBVIEW_SCREEN="$SRC/ui/WebViewSupportScreen.kt"
SERVER="api/src/server.js"
DATA="api/src/data.js"

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
  if grep -rEq "$1" "$2" 2>/dev/null; then
    fail "$3 (typo /$1/ encontrado em $2)"
  else
    pass "$3"
  fi
}

info "Diretório do lab: $LAB_DIR"

# --- Scripts anteriores ------------------------------------------------------
info "Conferindo scripts de fases anteriores..."
for n in 1 2 3 4 5 6 7; do need_file "scripts/validate-phase$n.sh"; done

# --- Arquivos novos ----------------------------------------------------------
info "Conferindo arquivos da Fase 8..."
need_file "$SECURITY/HardcodedSecrets.kt"
need_file "$SECURITY/WeakCrypto.kt"
need_file "$SECURITY/LegacyRequestSigner.kt"
need_file "$DEVICE_TRUST_SCREEN"

# --- HardcodedSecrets --------------------------------------------------------
info "Conferindo HardcodedSecrets..."
need_grep_tree "INTERNAL_CLIENT_PART" "$SECURITY" "constantes INTERNAL_CLIENT_PART"
need_grep_tree "LEGACY_SIGNING_SALT" "$SECURITY" "constantes LEGACY_SIGNING_SALT"
need_grep_tree "getInternalClientId" "$SECURITY" "método getInternalClientId"
need_grep_tree "getLegacySigningSalt" "$SECURITY" "método getLegacySigningSalt"
need_grep_tree "getEncodedOperatorHint" "$SECURITY" "método getEncodedOperatorHint"
need_grep_tree "getHiddenRoutes" "$SECURITY" "método getHiddenRoutes"

# --- WeakCrypto --------------------------------------------------------------
info "Conferindo WeakCrypto..."
need_grep_tree "base64Encode" "$SECURITY" "método base64Encode"
need_grep_tree "base64Decode" "$SECURITY" "método base64Decode"
need_grep_tree "weakXor" "$SECURITY" "método weakXor"
if grep -rqE "sha1Hex|md5Hex" "$SECURITY" 2>/dev/null; then
  pass "hash fraco (sha1Hex/md5Hex)"
else
  fail "hash fraco ausente (esperado sha1Hex ou md5Hex em $SECURITY)"
fi

# --- LegacyRequestSigner + headers -------------------------------------------
info "Conferindo LegacyRequestSigner e headers..."
need_grep_tree "X-Obsidian-Client" "$SRC" "header X-Obsidian-Client"
need_grep_tree "X-Obsidian-Device" "$SRC" "header X-Obsidian-Device"
need_grep_tree "X-Obsidian-Timestamp" "$SRC" "header X-Obsidian-Timestamp"
need_grep_tree "X-Obsidian-Signature" "$SRC" "header X-Obsidian-Signature"

# --- Typos de LegacyRequestSigner / WeakCrypto (quebrariam o build) -----------
# Estas checagens usam palavra inteira (não substring): 'LegacyRequestSigne'
# nunca pode casar com o correto 'LegacyRequestSigner'. Usamos python3 (com
# fallback para awk) para inspecionar linha a linha de forma exata.
info "Conferindo typos de LegacyRequestSigner / WeakCrypto (palavra inteira)..."
LRS_FILE="$SECURITY/LegacyRequestSigner.kt"
SIGNER_CHECK=""
if command -v python3 >/dev/null 2>&1; then SIGNER_CHECK=python3; elif command -v awk >/dev/null 2>&1; then SIGNER_CHECK=awk; fi

if [ -z "$SIGNER_CHECK" ]; then
  fail "nem python3 nem awk disponíveis para validar typos por palavra inteira"
elif [ "$SIGNER_CHECK" = "python3" ]; then
  python3 - "$SRC" "$LRS_FILE" "$DEVICE_TRUST_SCREEN" <<'PY'
import os, re, sys
src, lrs, screen = sys.argv[1], sys.argv[2], sys.argv[3]
G='\033[32m'; R='\033[31m'; Z='\033[0m'
ok=True
def pas(m): print(f"  {G}[PASS]{Z} {m}")
def bad(m):
    global ok; ok=False; print(f"  {R}[FAIL]{Z} {m}", file=sys.stderr)

def walk(root):
    if os.path.isfile(root):
        yield root; return
    for d,_,fs in os.walk(root):
        for f in fs: yield os.path.join(d,f)

def read(p):
    try:
        with open(p, encoding='utf-8', errors='replace') as fh: return fh.read()
    except Exception: return ""

# Typos negativos: (rótulo, regex). \b garante palavra inteira.
NEG = [
    ("object LegacyRequestSigne {",  r"object\s+LegacyRequestSigne\s*\{"),
    ("LegacyRequestSigne (sem r)",   r"\bLegacyRequestSigne\b(?!r)"),
    ("WeakCryptosha1Hex",            r"\bWeakCryptosha1Hex\b"),
    ("WeakCryptomd5Hex",             r"\bWeakCryptomd5Hex\b"),
    ("WeakCrypto sha1Hex (sem ponto)", r"\bWeakCrypto[ \t]+sha1Hex\b"),
    ("WeakCrypto md5Hex (sem ponto)",  r"\bWeakCrypto[ \t]+md5Hex\b"),
    ("securityLegacyRequestSigner",  r"\bsecurityLegacyRequestSigner\b"),
]
hits = {label: [] for label,_ in NEG}
for path in walk(src):
    if not path.endswith('.kt'): continue
    for i,line in enumerate(read(path).splitlines(),1):
        for label,pat in NEG:
            if re.search(pat,line): hits[label].append(f"{path}:{i}")
for label,_ in NEG:
    if hits[label]: bad(f"typo '{label}' encontrado: {', '.join(hits[label])}")
    else: pas(f"sem typo '{label}'")

# Positivos exatos.
lrs = read(lrs)
if re.search(r"^\s*object\s+LegacyRequestSigner\s*\{", lrs, re.M):
    pas("declaração exata 'object LegacyRequestSigner {'")
else: bad("ausente 'object LegacyRequestSigner {' em LegacyRequestSigner.kt")
if "WeakCrypto.sha1Hex(base)" in lrs:
    pas("chamada exata 'WeakCrypto.sha1Hex(base)'")
else: bad("ausente 'WeakCrypto.sha1Hex(base)' em LegacyRequestSigner.kt")
if "import com.obsidianpay.mobile.security.LegacyRequestSigner" in read(screen):
    pas("DeviceTrustScreen importa LegacyRequestSigner")
else: bad("DeviceTrustScreen não importa com.obsidianpay.mobile.security.LegacyRequestSigner")
sys.exit(0 if ok else 1)
PY
  [ $? -ne 0 ] && FAILED=1
else
  # Fallback awk (palavra inteira via [^A-Za-z0-9_] nas bordas).
  awk -v RS='\n' '
    /object[ \t]+LegacyRequestSigne[ \t]*\{/ { n1=1 }
    /(^|[^A-Za-z0-9_])WeakCryptosha1Hex([^A-Za-z0-9_]|$)/ { n2=1 }
    /(^|[^A-Za-z0-9_])WeakCryptomd5Hex([^A-Za-z0-9_]|$)/ { n3=1 }
    /(^|[^A-Za-z0-9_])WeakCrypto[ \t]+sha1Hex([^A-Za-z0-9_]|$)/ { n4=1 }
    /(^|[^A-Za-z0-9_])WeakCrypto[ \t]+md5Hex([^A-Za-z0-9_]|$)/ { n5=1 }
    /(^|[^A-Za-z0-9_])securityLegacyRequestSigner([^A-Za-z0-9_]|$)/ { n6=1 }
    END { if (n1||n2||n3||n4||n5||n6) exit 1 }
  ' $(find "$SRC" -name '*.kt') \
    && pass "sem typos de LegacyRequestSigner/WeakCrypto (awk)" \
    || fail "typo de LegacyRequestSigner/WeakCrypto encontrado (awk)"
  need_grep_file "object LegacyRequestSigner {" "$LRS_FILE" "declaração 'object LegacyRequestSigner {'"
  need_grep_file "WeakCrypto.sha1Hex(base)" "$LRS_FILE" "chamada 'WeakCrypto.sha1Hex(base)'"
  need_grep_file "import com.obsidianpay.mobile.security.LegacyRequestSigner" "$DEVICE_TRUST_SCREEN" \
    "DeviceTrustScreen importa LegacyRequestSigner"
fi

# --- DeviceTrustScreen + eventos ---------------------------------------------
info "Conferindo DeviceTrustScreen e eventos..."
need_grep_tree "Device Trust" "$APP/app/src/main" "UI: Device Trust"
need_grep_tree "device_trust_check_started" "$SRC" "evento device_trust_check_started"
need_grep_tree "weak_signature_generated" "$SRC" "evento weak_signature_generated"
need_grep_tree "device_trust_response_cached" "$SRC" "evento device_trust_response_cached"
need_grep_tree "encoded_hint_decoded" "$SRC" "evento encoded_hint_decoded"

# --- Backend -----------------------------------------------------------------
info "Conferindo backend device-trust..."
need_grep_file "/api/mobile/internal/device-trust" "$SERVER" "backend tem rota device-trust"
need_grep_file "/api/mobile/internal/reverse-hint" "$SERVER" "backend tem rota reverse-hint"
need_grep_file "x-obsidian-signature" "$SERVER" "backend lê X-Obsidian-Signature"
need_grep_file "trusted-legacy" "$SERVER" "backend retorna trusted-legacy"
need_grep_file "legacy-attestation" "$SERVER" "backend retorna legacy-attestation"
if grep -qE "createHash\('sha1'\)|createHash\(\"sha1\"\)|createHash\('md5'\)" "$SERVER"; then
  pass "backend usa hash fraco (sha1/md5) para a assinatura"
else
  fail "backend não usa hash fraco esperado para a assinatura"
fi
need_grep_file "enableLegacyDeviceTrust" "$DATA" "config tem enableLegacyDeviceTrust"
need_grep_file "internalDeviceTrustPath" "$DATA" "config tem internalDeviceTrustPath"
need_grep_file "internalReverseHintPath" "$DATA" "config tem internalReverseHintPath"

# --- Reforço dos typos de bridge/WebView (Fase 6) ----------------------------
info "Reforçando checagem de typos de bridge/WebView..."
reject_grep_re_tree 'fun[[:space:]]+getSessionSummar\(' "$SRC" "sem 'fun getSessionSummar()' (typo)"
reject_grep_re_tree 'getSessionSummar([^y]|$)' "$SRC" "sem typo 'getSessionSummar' (esperado getSessionSummary)"
reject_grep_re_tree '@JavascriptInterfac([^e]|$)' "$SRC" "sem typo '@JavascriptInterfac' (esperado @JavascriptInterface)"
reject_grep_re_tree 'webVieClient' "$SRC" "sem typo 'webVieClient' (esperado webViewClient)"

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
info "Conferindo guards nas classes da Fase 8..."
for f in "$SECURITY/HardcodedSecrets.kt" "$SECURITY/WeakCrypto.kt" \
         "$SECURITY/LegacyRequestSigner.kt" "$DEVICE_TRUST_SCREEN"; do
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
  printf '\033[32m==> Fase 8 validada com sucesso (estrutura).\033[0m\n'
  exit 0
else
  printf '\033[31m==> Fase 8: há falhas obrigatórias acima.\033[0m\n'
  exit 1
fi
