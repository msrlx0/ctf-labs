#!/usr/bin/env bash
#
# package-android-apk.sh — empacota o APK debug do Lab 08 (ObsidianPay Mobile)
# como o artefato candidato a QA usado pela Fase 22A.
#
# Faz EXATAMENTE o que o workflow do GitHub Actions faz, mas a partir de um build
# local já existente:
#   1. localiza o APK debug real (app/build/outputs/apk/debug/app-debug.apk);
#   2. falha se o APK não existir (não dispara build sozinho);
#   3. copia/renomeia para ObsidianPay-Lab08-v1.0.0-rc2.apk em um diretório de
#      distribuição local IGNORADO pelo Git (android-app/dist/);
#   4. gera o arquivo .sha256;
#   5. imprime caminhos finais, tamanho e checksum.
#
# Este script NÃO adiciona o APK ao Git, NÃO commita, NÃO cria tags, NÃO faz push
# e NÃO publica nada. Funciona a partir de qualquer diretório atual e não usa
# caminhos absolutos de home de usuário.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

APP_DIR="$LAB_DIR/android-app"
SRC_APK="$APP_DIR/app/build/outputs/apk/debug/app-debug.apk"
DIST_DIR="$APP_DIR/dist"
ARTIFACT_APK="ObsidianPay-Lab08-v1.0.0-rc2.apk"

err() { printf '\033[31m[ERRO]\033[0m %s\n' "$1" >&2; }
info() { printf '\033[36m[*]\033[0m %s\n' "$1"; }
ok()   { printf '\033[32m[OK]\033[0m %s\n' "$1"; }

info "Lab dir:    $LAB_DIR"
info "APK debug:  $SRC_APK"

if [ ! -f "$SRC_APK" ]; then
  err "APK debug não encontrado em: $SRC_APK"
  err "Gere o build primeiro (requer Android SDK):"
  err "  (cd \"$APP_DIR\" && ./gradlew --no-daemon clean :app:assembleDebug)"
  exit 1
fi

mkdir -p "$DIST_DIR"
DEST_APK="$DIST_DIR/$ARTIFACT_APK"
DEST_SHA="$DEST_APK.sha256"

cp "$SRC_APK" "$DEST_APK"

# Gera o .sha256 com o nome de arquivo relativo (sem caminho), para conferência
# previsível em qualquer máquina.
if command -v sha256sum >/dev/null 2>&1; then
  ( cd "$DIST_DIR" && sha256sum "$ARTIFACT_APK" > "$(basename "$DEST_SHA")" )
elif command -v shasum >/dev/null 2>&1; then
  ( cd "$DIST_DIR" && shasum -a 256 "$ARTIFACT_APK" > "$(basename "$DEST_SHA")" )
else
  err "Nenhuma ferramenta de SHA256 disponível (sha256sum/shasum)."
  exit 1
fi

# Tamanho (portável: prefere du -h, com fallback para bytes).
APK_SIZE="$(du -h "$DEST_APK" 2>/dev/null | cut -f1)"
[ -n "$APK_SIZE" ] || APK_SIZE="$(wc -c < "$DEST_APK") bytes"
APK_SHA="$(cut -d' ' -f1 "$DEST_SHA")"

echo ""
ok "Artefato candidato a QA empacotado (NÃO publicado):"
echo "  APK:     $DEST_APK"
echo "  SHA256:  $DEST_SHA"
echo "  Tamanho: $APK_SIZE"
echo "  Hash:    $APK_SHA"
echo ""
info "Diretório de distribuição ($DIST_DIR) é ignorado pelo Git — nada foi commitado."
