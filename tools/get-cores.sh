#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="${1:?android, linux, windows, macos-arm64 or macos-x86_64}"
CACHE="${CORES_CACHE:-$ROOT/build/cores}"

digest() {
  if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1"; else shasum -a 256 "$1"; fi | awk '{print $1}'
}

fetch() {
  local name=$1 asset=$2 repo tag dir base want
  read -r repo tag < <(awk -v n="$name" '$1 == n { print $2, $3 }' "$ROOT/tools/cores.txt")
  [ -n "${tag:-}" ] || { echo "$name is not in tools/cores.txt" >&2; exit 1; }
  dir="$CACHE/$repo/$tag"
  base="https://github.com/$repo/releases/download/$tag"
  mkdir -p "$dir"
  if [ ! -s "$dir/SHA256SUMS" ]; then
    echo "Fetching checksum list for $name ($repo $tag)" >&2
    local sums_tmp="$dir/.SHA256SUMS.part.$$"
    if ! curl --fail --silent --show-error --location --retry 8 --retry-all-errors --retry-delay 5 --retry-max-time 300 \
      --connect-timeout 20 --max-time 120 -o "$sums_tmp" "$base/SHA256SUMS"; then
      rm -f "$sums_tmp"
      echo "Failed to download checksum list for $name ($repo $tag)" >&2
      return 1
    fi
    [ -s "$sums_tmp" ] || { rm -f "$sums_tmp"; echo "Empty checksum list for $name ($repo $tag)" >&2; return 1; }
    mv "$sums_tmp" "$dir/SHA256SUMS"
  fi
  want=$(awk -v a="$asset" '$2 == a || $2 == "*" a { print $1 }' "$dir/SHA256SUMS")
  [[ "$want" =~ ^[[:xdigit:]]{64}$ ]] || { echo "No valid SHA-256 entry for $asset in $repo $tag" >&2; return 1; }
  if [ ! -s "$dir/$asset" ] || [ "$(digest "$dir/$asset")" != "$want" ]; then
    echo "Downloading $name/$asset from $repo ($tag)" >&2
    local asset_tmp="$dir/.$asset.part.$$"
    if ! curl --fail --show-error --location --retry 8 --retry-all-errors --retry-delay 5 --retry-max-time 300 \
      --connect-timeout 20 --max-time "${CORE_DOWNLOAD_MAX_TIME:-1200}" --output "$asset_tmp" "$base/$asset"; then
      rm -f "$asset_tmp"
      echo "Failed to download $asset from $repo $tag; the release server may be temporarily unavailable" >&2
      return 1
    fi
    if [ "$(digest "$asset_tmp")" != "$want" ]; then
      rm -f "$asset_tmp"
      echo "$asset from $repo $tag does not match its checksum" >&2
      return 1
    fi
    mv "$asset_tmp" "$dir/$asset"
  else
    echo "Using cached $name/$asset ($tag)" >&2
  fi
  FETCHED="$dir/$asset"
}

unpack() {
  local zip=$1 dest=$2
  rm -rf "$dest"
  mkdir -p "$dest"
  unzip -q -o "$zip" -d "$dest"
}

tor_tree() {
  unpack "$1" "$2"
  cp "$ROOT/app/src/main/assets/tor/bridges_default.lst" "$2/"
  ( cd "$2" && find . -type f ! -name files.txt | sed 's#^\./##' | sort > files.txt )
}

pick() {
  local from=$1 name=$2 to=$3 src
  src=$(find "$from" -type f -name "$name" | head -1)
  [ -n "$src" ] || { echo "$name is missing from $from" >&2; exit 1; }
  cp "$src" "$to"
}

case "$TARGET" in
  android)
    jni="$ROOT/app/src/main/jniLibs"
    mkdir -p "$jni"
    for abi in arm64-v8a armeabi-v7a x86_64; do
      mkdir -p "$jni/$abi"
      for core in tor snispoof hev; do
        fetch "$core" "$core-android-$abi.zip"
        unzip -q -o "$FETCHED" -d "$jni/$abi"
      done
    done
    fetch zeptun zeptun-android-jniLibs.zip
    unpack "$FETCHED" "$CACHE/zeptun-android"
    cp -r "$CACHE/zeptun-android/jniLibs/." "$jni/"
    find "$jni" -name '*.so' -exec chmod +x {} +
    ;;
  linux)
    bin="$ROOT/desktop/src/main/resources/bin/linux"
    mkdir -p "$bin"
    fetch xray xray-linux; cp "$FETCHED" "$bin/xray"
    fetch xray libcronet.so; cp "$FETCHED" "$bin/libcronet.so"
    fetch hev hev-linux; cp "$FETCHED" "$bin/hev-socks5-tunnel"
    fetch dns-tunnels zeddns-linux; cp "$FETCHED" "$bin/zeddns"
    fetch dns-tunnels masterdns-linux; cp "$FETCHED" "$bin/masterdns"
    fetch psiphon psiphon-linux; cp "$FETCHED" "$bin/psiphon"
    fetch tor tor-linux.zip; tor_tree "$FETCHED" "$bin/tor"
    chmod +x "$bin/xray" "$bin/hev-socks5-tunnel" "$bin/zeddns" "$bin/masterdns" "$bin/psiphon"
    ;;
  windows)
    bin="$ROOT/desktop/src/main/resources/bin/windows"
    mkdir -p "$bin"
    fetch xray xray-windows.exe; cp "$FETCHED" "$bin/xray.exe"
    fetch xray libcronet.dll; cp "$FETCHED" "$bin/libcronet.dll"
    fetch zeptun zeptun-windows-x86_64.zip; unpack "$FETCHED" "$CACHE/zeptun-windows"
    for f in zeptun.exe wintun.dll wintun-LICENSE.txt; do pick "$CACHE/zeptun-windows" "$f" "$bin/$f"; done
    fetch hev hev-windows.zip; unpack "$FETCHED" "$CACHE/hev-windows"
    for f in hev-socks5-tunnel.exe msys-2.0.dll; do pick "$CACHE/hev-windows" "$f" "$bin/$f"; done
    fetch dns-tunnels zeddns-windows.exe; cp "$FETCHED" "$bin/zeddns.exe"
    fetch dns-tunnels masterdns-windows.exe; cp "$FETCHED" "$bin/masterdns.exe"
    fetch psiphon psiphon-windows.exe; cp "$FETCHED" "$bin/psiphon.exe"
    fetch openconnect openconnect-windows.zip; unpack "$FETCHED" "$bin/openconnect"
    fetch tor tor-windows.zip; tor_tree "$FETCHED" "$bin/tor"
    ;;
  macos-arm64|macos-x86_64)
    arch=${TARGET#macos-}
    bin="$ROOT/desktop/src/main/resources/bin/macos"
    mkdir -p "$bin"
    fetch xray "xray-macos-$arch"; cp "$FETCHED" "$bin/xray"
    fetch zeptun zeptun-darwin-universal; cp "$FETCHED" "$bin/zeptun"
    fetch dns-tunnels "zeddns-macos-$arch"; cp "$FETCHED" "$bin/zeddns"
    fetch dns-tunnels "masterdns-macos-$arch"; cp "$FETCHED" "$bin/masterdns"
    fetch psiphon "psiphon-macos-$arch"; cp "$FETCHED" "$bin/psiphon"
    fetch hev "hev-macos-$arch"; cp "$FETCHED" "$bin/hev-socks5-tunnel"
    fetch tor "tor-macos-$arch.zip"; tor_tree "$FETCHED" "$bin/tor"
    chmod +x "$bin/xray" "$bin/zeptun" "$bin/zeddns" "$bin/masterdns" "$bin/psiphon" "$bin/hev-socks5-tunnel"
    ;;
  *)
    echo "unknown target $TARGET" >&2
    exit 1
    ;;
esac
echo "cores for $TARGET are in place"
