#!/bin/bash -e
cd "$(dirname "$0")"
cd package
TARGET=../appopsx.zip
rm -rf system
mkdir -p system/priv-app/AppOpsX
mv ../app/app-release.apk system/priv-app/AppOpsX/AppOpsX.apk
rm -f $TARGET
zip -9 -r $TARGET . -X -x \*.DS_Store
echo 'Done!'
