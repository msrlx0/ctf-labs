# ADB Playbook — Lab 08: ObsidianPay Mobile

Comandos ADB para interagir com o app ObsidianPay no laboratório local.

> Não é necessário dispositivo conectado para a validação do lab — esta é uma
> referência de comandos para uso quando o app estiver instalado.

---

## Pré-requisito

Dispositivo/emulador conectado e `adb devices` listando o alvo.

Variável de conveniência usada nos exemplos:

```bash
PACKAGE="com.obsidianpay.mobile"
# Para debug build: PACKAGE="com.obsidianpay.mobile.debug"
```

---

## install APK

```bash
# install APK no dispositivo/emulador conectado
adb install -r app-debug.apk

# verificar instalação
adb shell pm list packages | grep obsidianpay
```

---

## set proxy

Configure o proxy Burp Suite para interceptar o tráfego do app:

```bash
# definir proxy HTTP (ajuste o IP/porta do Burp)
adb shell settings put global http_proxy 192.168.0.50:8080

# verificar configuração
adb shell settings get global http_proxy
```

---

## clear proxy

```bash
# limpar proxy
adb shell settings put global http_proxy :0
# ou
adb shell settings delete global http_proxy
```

---

## inspect files

Inspecionar o sandbox de dados do app (requer debug build ou root):

```bash
# listar todos os arquivos do sandbox do app
adb shell run-as com.obsidianpay.mobile ls -R /data/data/com.obsidianpay.mobile/

# arquivos em filesDir
adb shell run-as com.obsidianpay.mobile ls -la /data/data/com.obsidianpay.mobile/files/

# cache files
adb shell run-as com.obsidianpay.mobile ls -la /data/data/com.obsidianpay.mobile/cache/
```

---

## inspect shared prefs

```bash
# listar arquivos de SharedPreferences
adb shell run-as com.obsidianpay.mobile ls shared_prefs/

# ler o arquivo de prefs principal do ObsidianPay
adb shell run-as com.obsidianpay.mobile cat shared_prefs/obsidian_prefs.xml
```

---

## inspect sqlite

```bash
# listar tabelas do banco de dados local
adb shell run-as com.obsidianpay.mobile sqlite3 \
  /data/data/com.obsidianpay.mobile/databases/obsidianpay_local.db ".tables"

# ler tabela cached_receipts
adb shell run-as com.obsidianpay.mobile sqlite3 \
  /data/data/com.obsidianpay.mobile/databases/obsidianpay_local.db \
  "SELECT * FROM cached_receipts LIMIT 10;"

# ler eventos de debug
adb shell run-as com.obsidianpay.mobile sqlite3 \
  /data/data/com.obsidianpay.mobile/databases/obsidianpay_local.db \
  "SELECT * FROM debug_events ORDER BY rowid DESC LIMIT 20;"
```

---

## launch deep link

Disparar deep links do scheme `obsidianpay://`:

```bash
# deep link de transferência
adb shell am start -a android.intent.action.VIEW \
  -d "obsidianpay://transfer?toUserId=2001&amount=10&memo=test" \
  com.obsidianpay.mobile

# deep link de suporte (abre WebView)
adb shell am start -a android.intent.action.VIEW \
  -d "obsidianpay://support?topic=mobile&message=hello" \
  com.obsidianpay.mobile

# deep link de recibo
adb shell am start -a android.intent.action.VIEW \
  -d "obsidianpay://receipt?id=1002" \
  com.obsidianpay.mobile
```

---

## start exported activity

```bash
# iniciar a Activity exportada de operações internas
adb shell am start \
  -n com.obsidianpay.mobile/.platform.InternalOpsActivity \
  -a com.obsidianpay.mobile.INTERNAL_OPS \
  --es obsidian.intent.extra.INTERNAL_ROUTE "support/ops"

# com RECEIPT_ID extra
adb shell am start \
  -n com.obsidianpay.mobile/.platform.InternalOpsActivity \
  -a com.obsidianpay.mobile.INTERNAL_OPS \
  --es obsidian.intent.extra.INTERNAL_ROUTE "support/ops" \
  --es obsidian.intent.extra.RECEIPT_ID "1002"
```

---

## send broadcast

```bash
# enviar broadcast de debug
adb shell am broadcast \
  -a com.obsidianpay.mobile.DEBUG_COMMAND \
  --es command sync_marker

# comando set_last_receipt
adb shell am broadcast \
  -a com.obsidianpay.mobile.DEBUG_COMMAND \
  --es command set_last_receipt \
  --es receiptId 1002

# comando write_debug_export
adb shell am broadcast \
  -a com.obsidianpay.mobile.DEBUG_COMMAND \
  --es command write_debug_export \
  --es route "support/ops"

# comando enable_operator_hint
adb shell am broadcast \
  -a com.obsidianpay.mobile.DEBUG_COMMAND \
  --es command enable_operator_hint
```

---

## query content provider

```bash
# consultar notas de suporte
adb shell content query --uri content://com.obsidianpay.mobile.provider.notes/notes

# consultar estado de debug (token_preview mascarado)
adb shell content query --uri content://com.obsidianpay.mobile.provider.notes/debug

# consultar cache de artefatos
adb shell content query --uri content://com.obsidianpay.mobile.provider.notes/cache
```

---

## logcat filters

Filtrar logs do ObsidianPay:

```bash
# todos os logs do pacote
adb logcat --pid=$(adb shell pidof com.obsidianpay.mobile)

# filtrar por tag específica
adb logcat -s ObsidianPay:V

# filtrar por qualquer log relacionado ao app
adb logcat | grep -i obsidian

# capturar em arquivo
adb logcat | grep -i obsidian > obsidian-logcat.txt
```

Para o playbook completo com todos os comandos comentados:
`tools/adb/lab08-adb-playbook.sh`
