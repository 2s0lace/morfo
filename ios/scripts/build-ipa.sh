#!/bin/zsh
# Builds an unsigned Morfo.ipa for sideloading (Sideloadly, AltStore…), which re-sign it with the
# installer's own Apple ID. Output: ios/build/Morfo.ipa
set -euo pipefail

cd "$(dirname "$0")/.."
OUT=build/ipa
rm -rf "$OUT"

xcodebuild \
  -project Morfo.xcodeproj -scheme Morfo -configuration Release \
  -destination 'generic/platform=iOS' -derivedDataPath build \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY="" \
  build | grep -E "error:|BUILD" || true

APP=build/Build/Products/Release-iphoneos/Morfo.app
[ -d "$APP" ] || { echo "Build failed: $APP not found" >&2; exit 1; }

mkdir -p "$OUT/Payload"
cp -R "$APP" "$OUT/Payload/"
(cd "$OUT" && zip -qry ../Morfo.ipa Payload)
rm -rf "$OUT"
echo "Created $(pwd)/build/Morfo.ipa ($(du -h build/Morfo.ipa | cut -f1))"
