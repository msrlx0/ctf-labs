#!/usr/bin/env bash
#
# lab08-adb-playbook.sh
# [ObsidianPay Lab] — Fase 13: ADB command playbook
#
# Playbook de comandos ADB para testes manuais do Lab 08 ObsidianPay Mobile.
# Este script é DIDÁTICO e comentado — não executa tudo automaticamente.
# Execute os blocos relevantes de acordo com a fase/tarefa em curso.
#
# Pré-requisito: adb disponível no PATH e dispositivo/emulador conectado.
# Ajuste APK_PATH e PROXY_HOST/PROXY_PORT conforme o seu ambiente.
#
# AVISO: Use somente no laboratório local autorizado.
#        Não execute estes comandos contra dispositivos ou apps de terceiros.
#
# ============================================================================

# Pacote alvo do lab
PACKAGE="com.obsidianpay.mobile"
# Para debug build, use:
# PACKAGE="com.obsidianpay.mobile.debug"

# Caminho do APK (ajuste para o seu ambiente)
APK_PATH="./android-app/app/build/outputs/apk/debug/app-debug.apk"

# IP e porta do proxy Burp (ajuste para o IP da sua máquina)
PROXY_HOST="192.168.0.50"
PROXY_PORT="8080"

echo "[ObsidianPay Lab] ADB Playbook — Lab 08"
echo "PACKAGE = $PACKAGE"
echo ""

# ============================================================================
# SEÇÃO 1: INSTALAR O APK
# ============================================================================
section_install() {
  echo "--- [install APK] ---"
  # Ajuste APK_PATH acima para o caminho real do APK
  # adb install -r "$APK_PATH"
  echo "(descomente a linha acima e ajuste APK_PATH)"
}

# ============================================================================
# SEÇÃO 2: CONFIGURAR PROXY BURP
# ============================================================================
section_set_proxy() {
  echo "--- [set proxy] ---"
  # Define o proxy HTTP para interceptação com Burp Suite
  # adb shell settings put global http_proxy "$PROXY_HOST:$PROXY_PORT"
  echo "(descomente e ajuste PROXY_HOST/PROXY_PORT)"
}

section_clear_proxy() {
  echo "--- [clear proxy] ---"
  # Remove a configuração de proxy
  # adb shell settings put global http_proxy :0
  echo "(descomente para limpar o proxy)"
}

# ============================================================================
# SEÇÃO 3: INSPECIONAR ARQUIVOS DO APP
# ============================================================================
section_inspect_files() {
  echo "--- [inspect files] ---"
  # Listar todos os arquivos do sandbox (requer debug build sem root)
  adb shell run-as "$PACKAGE" ls -R /data/data/"$PACKAGE"/ 2>/dev/null || \
    echo "(precisa de debug build ou dispositivo com root)"

  # Inspecionar diretórios específicos
  echo ""
  echo "-- files/ --"
  adb shell run-as "$PACKAGE" ls -la files/ 2>/dev/null || true

  echo ""
  echo "-- cache/ --"
  adb shell run-as "$PACKAGE" ls -la cache/ 2>/dev/null || true
}

# ============================================================================
# SEÇÃO 4: INSPECIONAR SHARED PREFERENCES
# ============================================================================
section_inspect_shared_prefs() {
  echo "--- [inspect shared prefs] ---"
  # Listar arquivos de SharedPreferences
  adb shell run-as "$PACKAGE" ls shared_prefs/ 2>/dev/null || \
    echo "(precisa de debug build)"

  echo ""
  echo "-- obsidian_prefs.xml --"
  # Ler o arquivo de prefs (ajuste o nome se necessário)
  adb shell run-as "$PACKAGE" cat shared_prefs/obsidian_prefs.xml 2>/dev/null || \
    echo "(ajuste o nome do arquivo de prefs)"
}

# ============================================================================
# SEÇÃO 5: INSPECIONAR SQLITE
# ============================================================================
section_inspect_sqlite() {
  echo "--- [inspect sqlite] ---"
  DB_PATH="/data/data/$PACKAGE/databases/obsidianpay_local.db"

  echo "-- tabelas --"
  adb shell run-as "$PACKAGE" sqlite3 "$DB_PATH" ".tables" 2>/dev/null || \
    echo "(sqlite3 pode nao estar disponivel no dispositivo)"

  echo ""
  echo "-- cached_receipts (primeiros 5) --"
  adb shell run-as "$PACKAGE" sqlite3 "$DB_PATH" \
    "SELECT id, receipt_id, substr(raw_json,1,100) FROM cached_receipts LIMIT 5;" 2>/dev/null || true

  echo ""
  echo "-- debug_events (ultimos 10) --"
  adb shell run-as "$PACKAGE" sqlite3 "$DB_PATH" \
    "SELECT event_type, timestamp, details FROM debug_events ORDER BY rowid DESC LIMIT 10;" 2>/dev/null || true
}

# ============================================================================
# SEÇÃO 6: DEEP LINKS
# ============================================================================
section_deep_links() {
  echo "--- [launch deep link] ---"

  echo "-- deep link: transfer --"
  adb shell am start -a android.intent.action.VIEW \
    -d "obsidianpay://transfer?toUserId=2001&amount=10&memo=test" \
    "$PACKAGE"

  sleep 1

  echo "-- deep link: support (abre WebView) --"
  adb shell am start -a android.intent.action.VIEW \
    -d "obsidianpay://support?topic=mobile&message=hello" \
    "$PACKAGE"

  sleep 1

  echo "-- deep link: receipt --"
  adb shell am start -a android.intent.action.VIEW \
    -d "obsidianpay://receipt?id=1002" \
    "$PACKAGE"
}

# ============================================================================
# SEÇÃO 7: EXPORTED ACTIVITY
# ============================================================================
section_exported_activity() {
  echo "--- [start exported activity] ---"

  echo "-- InternalOpsActivity: operações internas --"
  adb shell am start \
    -n "$PACKAGE/.platform.InternalOpsActivity" \
    -a com.obsidianpay.mobile.INTERNAL_OPS \
    --es obsidian.intent.extra.INTERNAL_ROUTE "support/ops"

  sleep 1

  echo "-- InternalOpsActivity: com RECEIPT_ID --"
  adb shell am start \
    -n "$PACKAGE/.platform.InternalOpsActivity" \
    -a com.obsidianpay.mobile.INTERNAL_OPS \
    --es obsidian.intent.extra.INTERNAL_ROUTE "support/ops" \
    --es obsidian.intent.extra.RECEIPT_ID "1002"
}

# ============================================================================
# SEÇÃO 8: BROADCAST RECEIVER
# ============================================================================
section_broadcast() {
  echo "--- [send broadcast] ---"

  echo "-- comando: sync_marker --"
  adb shell am broadcast \
    -a com.obsidianpay.mobile.DEBUG_COMMAND \
    --es command sync_marker

  sleep 1

  echo "-- comando: set_last_receipt --"
  adb shell am broadcast \
    -a com.obsidianpay.mobile.DEBUG_COMMAND \
    --es command set_last_receipt \
    --es receiptId 1002

  sleep 1

  echo "-- comando: write_debug_export --"
  adb shell am broadcast \
    -a com.obsidianpay.mobile.DEBUG_COMMAND \
    --es command write_debug_export \
    --es route "support/ops"

  sleep 1

  echo "-- comando: enable_operator_hint --"
  adb shell am broadcast \
    -a com.obsidianpay.mobile.DEBUG_COMMAND \
    --es command enable_operator_hint
}

# ============================================================================
# SEÇÃO 9: CONTENT PROVIDER
# ============================================================================
section_content_provider() {
  echo "--- [query content provider] ---"

  echo "-- /notes: notas de suporte --"
  adb shell content query --uri content://com.obsidianpay.mobile.provider.notes/notes

  echo ""
  echo "-- /debug: estado de debug (token_preview mascarado) --"
  adb shell content query --uri content://com.obsidianpay.mobile.provider.notes/debug

  echo ""
  echo "-- /cache: artefatos locais --"
  adb shell content query --uri content://com.obsidianpay.mobile.provider.notes/cache
}

# ============================================================================
# SEÇÃO 10: LOGCAT
# ============================================================================
section_logcat() {
  echo "--- [logcat filters] ---"

  # Filtrar logs por PID do app (requer app rodando)
  APP_PID=$(adb shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r')
  if [ -n "$APP_PID" ]; then
    echo "PID do app: $APP_PID"
    echo "Iniciando logcat filtrado... (Ctrl+C para parar)"
    adb logcat --pid="$APP_PID"
  else
    echo "App nao esta rodando. Filtrando por string 'obsidian':"
    echo "Iniciando logcat... (Ctrl+C para parar)"
    # adb logcat | grep -i obsidian
    echo "(descomente a linha acima para iniciar o logcat)"
  fi
}

# ============================================================================
# MAIN — execute a seção desejada
# ============================================================================
# Descomente as seções que deseja executar:
# section_install
# section_set_proxy
# section_inspect_files
# section_inspect_shared_prefs
# section_inspect_sqlite
# section_deep_links
# section_exported_activity
# section_broadcast
# section_content_provider
# section_logcat

echo ""
echo "Playbook carregado. Descomente as seções desejadas no final do script."
echo "Ajuste PACKAGE, APK_PATH e PROXY_HOST/PROXY_PORT conforme o seu ambiente."
echo ""
echo "Dica: para executar uma seção específica direto:"
echo "  source $0 && section_content_provider"
