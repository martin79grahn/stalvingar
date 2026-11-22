#!/bin/sh
# Build dist/Stalvingar-linux-vVERSION.tar.gz (game page + WebKitGTK launcher + installer).
set -e
cd "$(dirname "$0")"
VERSION=${VERSION:-1.0.0}
D=dist/stalvingar-$VERSION
rm -rf "$D" && mkdir -p "$D"
cp game/index.html linux/stalvingar.py linux/install.sh "$D/"
python3 icon.py "$D/stalvingar.png" 256
tar -C dist -czf dist/Stalvingar-linux-v$VERSION.tar.gz stalvingar-$VERSION
rm -rf "$D"
ls -l dist/Stalvingar-linux-v$VERSION.tar.gz
