#!/bin/bash -e

cd "$(dirname "$0")"
./gradlew clean build

cd package
TARGET=../appopsx.zip
APK=../app/build/outputs/apk/app-release.apk
TOOLDIR="$ANDROID_HOME/build-tools/26.0.2"
DEST=system/priv-app/AppOpsX/AppOpsX.apk

rm -rf system
mkdir -p "$(dirname $DEST)"
"$TOOLDIR/zipalign" -f 4 "$APK" "$DEST"
"$TOOLDIR/apksigner" sign --ks ../travis.keystore \
  --ks-key-alias travis \
  --ks-pass pass:travis \
  --key-pass pass:travis \
  "$DEST"

rm -f $TARGET
zip -9 -r $TARGET . -X -x \*.DS_Store
echo 'Done!'
