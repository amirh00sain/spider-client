#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/cores/zedsecure-cores-zedcore-r6"
ARCHIVE="$ROOT/build/cores/zedsecure-cores-zedcore-r6.zip"
SOURCE=""
DEST="${1:?output AAR path is required}"
GOBIN="$ROOT/build/cores/go-bin"

mkdir -p "$OUT" "$GOBIN" "$(dirname "$DEST")"
if ! find "$OUT" -mindepth 3 -maxdepth 3 -type f -path '*/scripts/build-zedcore.sh' -print -quit | grep -q .; then
  curl --fail --silent --show-error --location --retry 5 --retry-all-errors --retry-delay 3 \
    --connect-timeout 20 --max-time 120 -o "$ARCHIVE.part" \
    'https://codeload.github.com/CluvexStudio/ZedSecure-Cores/zip/refs/tags/zedcore-r6'
  mv "$ARCHIVE.part" "$ARCHIVE"
  unzip -q -o "$ARCHIVE" -d "$OUT"
fi

SOURCE="$(find "$OUT" -mindepth 3 -maxdepth 3 -type f -path '*/scripts/build-zedcore.sh' -print -quit | sed 's#/scripts/build-zedcore.sh$##')"
[ -n "$SOURCE" ] || { echo "ZedSecure core builder is missing from $OUT" >&2; exit 1; }

export GOBIN PATH="$GOBIN:$PATH" SPIDER_ROOT="$ROOT"
go install golang.org/x/mobile/cmd/gomobile@v0.0.0-20260908204917-8b95e45f8d3e
go install golang.org/x/mobile/cmd/gobind@v0.0.0-20260908204917-8b95e45f8d3e
"$SOURCE/scripts/fetch.sh"
(
  cd "$SOURCE/vendor/AndroidLibXrayLite"
  export GOTOOLCHAIN=go1.27.3
  go get -tool golang.org/x/mobile/cmd/gobind@v0.0.0-20260908204917-8b95e45f8d3e
  go mod download golang.org/x/mobile@v0.0.0-20260908204917-8b95e45f8d3e
)
if ! grep -q 'github.com/armanlife/arman-vpn-client/core/mobile' "$SOURCE/scripts/build-zedcore.sh"; then
  patch --directory="$SOURCE" -p1 < "$ROOT/tools/zedcore-arman.patch"
fi
grep -q 'github.com/armanlife/arman-vpn-client/core/mobile' "$SOURCE/scripts/build-zedcore.sh"
"$SOURCE/scripts/build-zedcore.sh"
test -s "$SOURCE/out/zedcore.aar"
cp "$SOURCE/out/zedcore.aar" "$DEST"
