#!/bin/bash

set -e

cd "$(dirname "$0")"
[[ "$1" == "debug" ]] && ./gradlew build || ./gradlew clean build

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

cd ../uninstaller
rm -f ../uninstaller.zip
zip -9 -r ../uninstaller.zip . -X -x \*.DS_Store
cd ..

COMMIT=$(git rev-parse --short HEAD)
echo "AppOpsX Release (build $(date +"%y%m%d")-$COMMIT)" > checksum
echo "" >> checksum
echo "MD5 Checksum:" >> checksum
if hash openssl 2>/dev/null; then
    MD5="openssl md5 -r"
else
    MD5="md5sum"
fi
for f in *.zip package/$DEST; do
    chksum=($($MD5 $f))
    echo "  ${f##*/}:" >> checksum
    echo "    $chksum" >> checksum
done
if hash perl 2>/dev/null; then
    perl -pe 's/\r\n|\n|\r/\r\n/g' checksum > checksum.txt
else
    sed -e 's/$/\r/' checksum > checksum.txt
fi
rm -f checksum

echo 'Done!'
