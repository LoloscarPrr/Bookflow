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
download_verified "$SHERPA_URL" "$SHERPA_DOWNLOAD" "$SHERPA_SHA256"
cp -- "$SHERPA_DOWNLOAD" "$AAR_DIR/sherpa-onnx.aar"

prepare_model() {
    local model_name="$1"
    local model_file="$2"
    local model_sha256="$3"
    local model_archive="${model_name}.tar.bz2"
    local model_url="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/${model_archive}"
    local model_download="$DOWNLOAD_DIR/$model_archive"
    local model_dir="$ASSETS_DIR/$model_name"
    local model_marker="$model_dir/.bookflow-model-sha256"

    download_verified "$model_url" "$model_download" "$model_sha256"
    if [[ ! -f "$model_marker" ]] || [[ "$(tr -d '\r\n' < "$model_marker")" != "$model_sha256" ]]; then
        case "$model_dir" in
            "$PROJECT_DIR"/app/src/main/assets/*) ;;
            *)
                printf 'Refusing unsafe model destination: %s\n' "$model_dir" >&2
                exit 1
                ;;
        esac

        local temp_dir
        temp_dir="$(mktemp -d)"
        tar --no-same-owner -xjf "$model_download" -C "$temp_dir"
        rm -rf -- "$model_dir"
        mv -- "$temp_dir/$model_name" "$model_dir"
        printf '%s\n' "$model_sha256" > "$model_marker"
        rm -rf -- "$temp_dir"
    fi

    test -s "$model_dir/$model_file"
    test -s "$model_dir/tokens.txt"
    test -d "$model_dir/espeak-ng-data"
}

prepare_model \
    "vits-piper-es_ES-miro-high" \
    "es_ES-miro-high.onnx" \
    "15c7593c9e6eed29b48df983708672091c6ce3556485f351c2eb917cf3e5c2db"

prepare_model \
    "vits-piper-es_AR-daniela-high" \
    "es_AR-daniela-high.onnx" \
    "71cbf6b7f646ab74f3c51336151abef41e6c54467ac929ffdb19ed706a07dd7b"

test -s "$AAR_DIR/sherpa-onnx.aar"

printf 'Two high-quality offline Spanish voices are ready.\n'
