#!/usr/bin/env bash
set -euo pipefail

target="${1:?usage: package-aether.sh linux-64|linux-arm64|windows-64|macos-64|macos-x86_64|macos-arm64}"
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
repo="https://github.com/patterniha/Aether"
release_url="$(curl -fsSLI -o /dev/null -w '%{url_effective}' "$repo/releases/latest")"
tag="${release_url##*/releases/tag/}"
[[ "$tag" =~ ^v?[0-9][0-9A-Za-z.+-]*$ ]] || { echo "Could not resolve latest Aether release." >&2; exit 1; }
case "$target" in
  linux-64) asset=aether-linux-x86_64-musl.tar.gz; os=linux; binary=aether ;;
  linux-arm64) asset=aether-linux-aarch64-musl.tar.gz; os=linux; binary=aether ;;
  windows-64) asset=aether-windows-x86_64.zip; os=windows; binary=aether.exe ;;
  macos-64|macos-x86_64) asset=aether-macos-x86_64.tar.gz; os=macos; binary=aether ;;
  macos-arm64) asset=aether-macos-arm64.tar.gz; os=macos; binary=aether ;;
  *) echo "Unsupported Aether target: $target" >&2; exit 2 ;;
esac
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
base="$repo/releases/download/$tag"
curl -fsSL "$base/$asset" -o "$tmp/$asset"
curl -fsSL "$base/$asset.sha256" -o "$tmp/$asset.sha256"
expected="$(awk 'NR==1 {print tolower($1)}' "$tmp/$asset.sha256")"
actual="$(sha256sum "$tmp/$asset" | awk '{print $1}')"
[[ "$expected" =~ ^[0-9a-f]{64}$ && "$expected" == "$actual" ]] || { echo "Aether release checksum failed." >&2; exit 1; }

mkdir -p "$tmp/unpack"
python3 - "$tmp/$asset" "$tmp/unpack" <<'PY'
import os, pathlib, shutil, stat, sys, tarfile, zipfile
archive, destination = sys.argv[1:]
root = pathlib.Path(destination).resolve()
if archive.endswith('.zip'):
    with zipfile.ZipFile(archive) as source:
        for item in source.infolist():
            path = pathlib.PurePosixPath(item.filename.replace('\\', '/'))
            if path.is_absolute() or '..' in path.parts:
                raise SystemExit('Unsafe path in Aether release archive')
            target = (root / pathlib.Path(*path.parts)).resolve()
            if root not in target.parents and root != target:
                raise SystemExit('Unsafe path in Aether release archive')
            mode = item.external_attr >> 16
            if stat.S_ISLNK(mode):
                raise SystemExit('Links are not accepted in Aether release archives')
            if item.is_dir():
                target.mkdir(parents=True, exist_ok=True)
            else:
                target.parent.mkdir(parents=True, exist_ok=True)
                with source.open(item) as src, target.open('wb') as dst:
                    shutil.copyfileobj(src, dst)
                if mode & 0o111:
                    target.chmod(0o755)
else:
    with tarfile.open(archive, 'r:gz') as source:
        for item in source.getmembers():
            path = pathlib.PurePosixPath(item.name)
            if path.is_absolute() or '..' in path.parts or not (item.isfile() or item.isdir()):
                raise SystemExit('Unsafe entry in Aether release archive')
            target = (root / pathlib.Path(*path.parts)).resolve()
            if root not in target.parents and root != target:
                raise SystemExit('Unsafe entry in Aether release archive')
            if item.isdir():
                target.mkdir(parents=True, exist_ok=True)
            else:
                target.parent.mkdir(parents=True, exist_ok=True)
                stream = source.extractfile(item)
                if stream is None:
                    raise SystemExit('Unreadable file in Aether release archive')
                with stream, target.open('wb') as dst:
                    shutil.copyfileobj(stream, dst)
                target.chmod(0o755 if item.mode & 0o111 else 0o644)
PY
source="$(find "$tmp/unpack" -type f -name "$binary" | head -n1)"
[[ -f "$source" ]] || { echo "Aether executable missing from release archive." >&2; exit 1; }
dest="$root/desktop/src/main/resources/bin/$os"
mkdir -p "$dest"
install -m 0755 "$source" "$dest/$binary"
licenses="$(find "$tmp/unpack" -type d -name LICENSES | head -n1)"
if [[ -n "$licenses" ]]; then
  rm -rf "$dest/aether-LICENSES"
  cp -a "$licenses" "$dest/aether-LICENSES"
fi
license="$(find "$tmp/unpack" -maxdepth 4 -type f \( -iname 'AGPL-3.0*' -o -iname 'LICENSE*' \) | head -n1)"
if [[ -n "$license" ]]; then install -m 0644 "$license" "$dest/AETHER-LICENSE.txt"; fi
echo "Packaged Aether $tag for $target at $dest/$binary"
