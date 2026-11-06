#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CORE="$ROOT/aether/aether"
OUT="$ROOT/app/src/main/jniLibs"
API_LEVEL=29

if [[ ! -f "$CORE/Cargo.toml" ]]; then
  repo="https://github.com/patterniha/Aether"
  release_url="$(curl -fsSLI -o /dev/null -w '%{url_effective}' "$repo/releases/latest")"
  tag="${release_url##*/releases/tag/}"
  [[ "$tag" =~ ^v?[0-9][0-9A-Za-z.+-]*$ ]] || { echo "Could not resolve latest Aether source release." >&2; exit 1; }
  source_dir="$ROOT/.build/aether-source"
  rm -rf "$source_dir"
  mkdir -p "$(dirname "$source_dir")"
  git clone --depth 1 --branch "$tag" "$repo.git" "$source_dir"
  CORE="$source_dir/aether"
fi
if [[ -z "${NDK_HOME:-}" || ! -d "$NDK_HOME/toolchains/llvm/prebuilt" ]]; then
  echo "Set NDK_HOME to an installed Android NDK." >&2
  exit 1
fi

HOST_TAG=""
for candidate in linux-x86_64 linux-arm64 darwin-arm64 darwin-x86_64; do
  if [[ -d "$NDK_HOME/toolchains/llvm/prebuilt/$candidate/bin" ]]; then HOST_TAG="$candidate"; break; fi
done
[[ -n "$HOST_TAG" ]] || { echo "Android NDK LLVM toolchain not found." >&2; exit 1; }
TOOLCHAIN="$NDK_HOME/toolchains/llvm/prebuilt/$HOST_TAG/bin"
SYSROOT="$(cd "$TOOLCHAIN/../sysroot" && pwd)"

for abi in arm64-v8a armeabi-v7a x86_64; do
  case "$abi" in
    arm64-v8a) triple=aarch64-linux-android; clang_target="$triple$API_LEVEL" ;;
    armeabi-v7a) triple=armv7-linux-androideabi; clang_target="armv7a-linux-androideabi$API_LEVEL" ;;
    x86_64) triple=x86_64-linux-android; clang_target="$triple$API_LEVEL" ;;
  esac
  under_triple="${triple//-/_}"
  env_triple="${triple^^}"
  env_triple="${env_triple//-/_}"
  rustup target add "$triple"
  env \
    ANDROID_NDK_HOME="$NDK_HOME" ANDROID_NDK_ROOT="$NDK_HOME" \
    "CARGO_TARGET_${env_triple}_LINKER=$TOOLCHAIN/$clang_target-clang" \
    "CARGO_TARGET_${env_triple}_RUSTFLAGS=-C link-arg=-Wl,-z,max-page-size=16384 -C link-arg=-Wl,-z,common-page-size=16384" \
    "CC_${under_triple}=$TOOLCHAIN/clang" "CXX_${under_triple}=$TOOLCHAIN/clang++" \
    "CFLAGS_${under_triple}=--target=$clang_target" "CXXFLAGS_${under_triple}=--target=$clang_target" \
    "AR_${under_triple}=$TOOLCHAIN/llvm-ar" \
    "BINDGEN_EXTRA_CLANG_ARGS_${under_triple}=--target=$clang_target --sysroot=$SYSROOT" \
    cargo build --release --locked --manifest-path "$CORE/Cargo.toml" --target "$triple" --bin aether
  produced="$CORE/target/$triple/release/aether"
  [[ -f "$produced" ]] || { echo "Aether binary missing for $abi." >&2; exit 1; }
  mkdir -p "$OUT/$abi"
  install -m 0755 "$produced" "$OUT/$abi/libaether.so"
done
