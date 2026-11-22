#!/bin/sh
# Build dist/Stalvingar-linux-vVERSION.tar.gz (game page + WebKitGTK launcher stalvingar.sh + menu installer "install").
set -e
cd "$(dirname "$0")"
VERSION=${VERSION:-1.0.0}
# update check against the Release Store (~/gh-appstore); the repo is private, so it needs the store key
STORE_URL=${STORE_URL:-http://192.168.0.97:8787 http://100.85.197.108:8787}
STORE_KEY=${STORE_KEY:-$(python3 -c 'import json,os;print(json.load(open(os.path.expanduser("~/.local/share/gh-appstore/config.json")))["access_key"])' 2>/dev/null || true)}
UPDATE_JSON=$(printf '{"store": "%s", "repo": "martin79grahn/stalvingar", "key": "%s"}' "$STORE_URL" "$STORE_KEY")
D=dist/stalvingar-$VERSION
rm -rf "$D" && mkdir -p "$D"
cp game/index.html linux/install "$D/"
sed "s/^VERSION = .*/VERSION = \"$VERSION\"  # set by build-linux.sh/" linux/stalvingar.py > "$D/stalvingar.py"
printf '#!/bin/sh\nexec python3 "$(dirname "$(readlink -f "$0")")/stalvingar.py" "$@"\n' > "$D/stalvingar.sh"
chmod 755 "$D/stalvingar.py" "$D/stalvingar.sh" "$D/install"
[ -n "$STORE_KEY" ] && (umask 077; echo "$UPDATE_JSON" > "$D/update.json")
python3 icon.py "$D/stalvingar.png" 256
tar -C dist -czf dist/Stalvingar-linux-v$VERSION.tar.gz stalvingar-$VERSION
rm -rf "$D"
ls -l dist/Stalvingar-linux-v$VERSION.tar.gz
