#!/usr/bin/env bash
# Downloads the official Stockfish release and puts it where Maven bundles it into the jar.
# Run before `mvn package` (the exe is ~100 MB, so it is not committed).
set -euo pipefail

VERSION="sf_19"
ASSET="stockfish-windows-x86-64-universal"
DEST="$(cd "$(dirname "$0")/.." && pwd)/src/main/resources/engines"

mkdir -p "$DEST/windows"
tmp="$(mktemp -d)"
curl -fsSL "https://github.com/official-stockfish/Stockfish/releases/download/${VERSION}/${ASSET}.zip" -o "$tmp/sf.zip"
unzip -q "$tmp/sf.zip" -d "$tmp"
cp "$(find "$tmp" -name "${ASSET}.exe" | head -1)" "$DEST/windows/stockfish.exe"
cp "$(find "$tmp" -name Copying.txt | head -1)" "$DEST/STOCKFISH-COPYING.txt"
rm -rf "$tmp"
echo "Stockfish ${VERSION} ready in $DEST"
