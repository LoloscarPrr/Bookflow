#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DOWNLOAD_DIR="$PROJECT_DIR/.offline-tts-downloads"
AAR_DIR="$PROJECT_DIR/app/libs"
ASSETS_DIR="$PROJECT_DIR/app/src/main/assets"

SHERPA_VERSION="1.13.6"
SHERPA_FILE="sherpa-onnx-static-link-onnxruntime-${SHERPA_VERSION}.aar"
SHERPA_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/v${SHERPA_VERSION}/${SHERPA_FILE}"
SHERPA_SHA256="01e87037afca2ed49085062aace5c012e60321e8e23e3a72b6d9ac02c843f66c"

MODEL_NAME="vits-piper-es_ES-sharvard-medium"
MODEL_ARCHIVE="${MODEL_NAME}.tar.bz2"
MODEL_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/${MODEL_ARCHIVE}"
MODEL_SHA256="b30a7a83df0518f0ee1c7039506648cade99f1f9b498fc49ed2ced2e2536bb5a"

mkdir -p "$DOWNLOAD_DIR" "$AAR_DIR" "$ASSETS_DIR"

download_verified() {
    local url="$1"
    local destination="$2"
    local expected_sha="$3"

    if [[ -f "$destination" ]] && printf '%s  %s\n' "$expected_sha" "$destination" | sha256sum --check --status; then
        return
    fi

    local partial="${destination}.part"
    rm -f -- "$partial"
    curl --fail --location --retry 3 --silent --show-error "$url" --output "$partial"
    printf '%s  %s\n' "$expected_sha" "$partial" | sha256sum --check
    mv -- "$partial" "$destination"
}

SHERPA_DOWNLOAD="$DOWNLOAD_DIR/$SHERPA_FILE"
MODEL_DOWNLOAD="$DOWNLOAD_DIR/$MODEL_ARCHIVE"

download_verified "$SHERPA_URL" "$SHERPA_DOWNLOAD" "$SHERPA_SHA256"
download_verified "$MODEL_URL" "$MODEL_DOWNLOAD" "$MODEL_SHA256"

cp -- "$SHERPA_DOWNLOAD" "$AAR_DIR/sherpa-onnx.aar"

MODEL_DIR="$ASSETS_DIR/$MODEL_NAME"
MODEL_MARKER="$MODEL_DIR/.bookflow-model-sha256"
if [[ ! -f "$MODEL_MARKER" ]] || [[ "$(tr -d '\r\n' < "$MODEL_MARKER")" != "$MODEL_SHA256" ]]; then
    case "$MODEL_DIR" in
        "$PROJECT_DIR"/app/src/main/assets/*) ;;
        *)
            printf 'Refusing unsafe model destination: %s\n' "$MODEL_DIR" >&2
            exit 1
            ;;
    esac

    TEMP_DIR="$(mktemp -d)"
    trap 'rm -rf -- "$TEMP_DIR"' EXIT
    tar --no-same-owner -xjf "$MODEL_DOWNLOAD" -C "$TEMP_DIR"
    rm -rf -- "$MODEL_DIR"
    mv -- "$TEMP_DIR/$MODEL_NAME" "$MODEL_DIR"
    printf '%s\n' "$MODEL_SHA256" > "$MODEL_MARKER"
fi

test -s "$AAR_DIR/sherpa-onnx.aar"
test -s "$MODEL_DIR/es_ES-sharvard-medium.onnx"
test -s "$MODEL_DIR/tokens.txt"
test -d "$MODEL_DIR/espeak-ng-data"

printf 'Offline neural narration dependencies are ready.\n'

