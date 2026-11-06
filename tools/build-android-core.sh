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
# Pick the NDK: prefer the one the workflow installed (NDK_HOME), else the newest.
# cronet-go's libcronet.a needs a recent lld; NDK 27 fails with
# "unknown relocation (315) against symbol typeinfo for ...".
NDK_ROOT="${ANDROID_HOME:-/usr/local/lib/android/sdk}/ndk"
if [ -n "${NDK_HOME:-}" ] && [ -d "$NDK_HOME/toolchains/llvm/prebuilt" ]; then
  NEWEST_NDK="$(basename "$NDK_HOME")"
else
  NEWEST_NDK="$(find "$NDK_ROOT" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' 2>/dev/null | sort -V | tail -1)"
fi
[ -n "$NEWEST_NDK" ] || { echo "No Android NDK found under $NDK_ROOT" >&2; exit 1; }
if [ "${NEWEST_NDK%%.*}" -lt 28 ]; then
  echo "NDK $NEWEST_NDK is too old (need >= 28) to link libcronet.a" >&2
  exit 1
fi
export NDK_HOME="$NDK_ROOT/$NEWEST_NDK" ANDROID_NDK_HOME="$NDK_ROOT/$NEWEST_NDK" ANDROID_NDK_ROOT="$NDK_ROOT/$NEWEST_NDK"
# Rewrite every hard-coded NDK version in the upstream scripts: both "/ndk/27.3.13750724"
# and a bare "27.3.13750724" (e.g. NDK_VERSION=...), which the old sed missed.
find "$SOURCE/scripts" -type f -name '*.sh' -exec sed -i -E \
  "s#/ndk/[0-9]+\.[0-9]+\.[0-9]+#/ndk/$NEWEST_NDK#g; s#\b[0-9]{2}\.[0-9]+\.[0-9]{7,8}\b#$NEWEST_NDK#g" {} +
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
if grep -rnE '\b[0-9]{2}\.[0-9]+\.[0-9]{7,8}\b' "$SOURCE/scripts" | grep -v "$NEWEST_NDK"; then
  echo "Hard-coded old NDK version still present in builder scripts" >&2; exit 1
fi
echo ">>> building cores with NDK $NEWEST_NDK"
"$SOURCE/scripts/build-zedcore.sh"
test -s "$SOURCE/out/zedcore.aar"
cp "$SOURCE/out/zedcore.aar" "$DEST"
