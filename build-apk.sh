#!/bin/sh
# Build out/Stalvingar-vVERSION.apk (the game in a fullscreen landscape WebView).
# Needs the Android SDK (build-tools 35, platform 35), a JDK, Python 3 with Pillow, and the release key:
#   ~/.android/stalvingar-release.keystore, alias stalvingar, password in ~/.android/stalvingar-release.pass
# Bump VERSION and CODE for every release so it installs as an update.
set -e
cd "$(dirname "$0")"
VERSION=${VERSION:-1.0.0}
CODE=${CODE:-1}
SDK=${ANDROID_HOME:-$HOME/Android/Sdk}
BT=$SDK/build-tools/35.0.0
JAR=$SDK/platforms/android-35/android.jar
KEYSTORE=${KEYSTORE:-$HOME/.android/stalvingar-release.keystore}
KEY_PASS=${KEY_PASS:-$(cat "$HOME/.android/stalvingar-release.pass")}
export KEY_PASS
if [ -z "$JAVAC" ] && ! command -v javac >/dev/null; then
  JAVAC="java -m jdk.compiler/com.sun.tools.javac.Main"; JFLAGS=${JFLAGS:--source 11 -target 11 -Xlint:-options}
fi
JAVAC=${JAVAC:-javac}
JFLAGS=${JFLAGS:---release 11}

B=out/android
rm -rf $B && mkdir -p $B/assets $B/classes $B/res
cp game/index.html $B/assets/
python3 icon.py --android $B/res
$BT/aapt2 compile --dir android/res -o $B/res-values.zip
$BT/aapt2 compile --dir $B/res -o $B/res-icon.zip
$BT/aapt2 link -o $B/unsigned.apk -I $JAR --manifest android/AndroidManifest.xml -A $B/assets \
  --min-sdk-version 24 --target-sdk-version 35 --version-code $CODE --version-name $VERSION \
  $B/res-values.zip $B/res-icon.zip
$JAVAC $JFLAGS -cp $JAR -d $B/classes $(find android/src -name '*.java')
$BT/d8 --release --lib $JAR --min-api 24 --output $B $(find $B/classes -name '*.class')
(cd $B && zip -q unsigned.apk classes.dex)
$BT/zipalign -f -p 4 $B/unsigned.apk $B/aligned.apk
$BT/apksigner sign --ks "$KEYSTORE" --ks-key-alias stalvingar --ks-pass env:KEY_PASS --out out/Stalvingar-v$VERSION.apk $B/aligned.apk
rm -f out/Stalvingar-v$VERSION.apk.idsig
ls -l out/Stalvingar-v$VERSION.apk
