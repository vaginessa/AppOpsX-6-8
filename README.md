AppOpsX with IFW support
---------

### What is AppOpsX?

AppOpsX is an Android app for individually managing permissions, which can overwrite permission settings of the system. For example, you can disable an *EVIL* app that sniffs your IMEI information by disabling its Phone permission in AppOpsX, even if you allowed Phone permission in the system Settings app. This is quite useful when such *EVIL* apps refuse to work without permissions.

For more details, please visit the introduction of the [original project](https://github.com/8enet/AppOpsX).

### Features of this modified version:

1. No need of adb or root when running (but needs root or 3rd-party recovery for installation).
2. Support disabling services, broadcasts and activities using Intent Firewall (through `/data/system/ifw/ifw.xml`, **require Android 6.0 and above**).

### Downloads:

Visit the [release](https://github.com/linusyang92/AppOpsX/releases) page.

### How to install: 

* For **first installation**: 
    * Use 3rd party recovery: Download and flash the installer zip (`appopsx-installer.zip`) by 3rd party recovery (e.g. TWRP).
    * Use root permission: Download and install the apk file. Open the app and tap the "setting" icon on the right top corner. Tap "Install as system app" option in "Others" section. Select "Install", confirm and reboot your device.
* For **future updates**: Just install the apk file normally like any other apps. You can also flash the installer to update.

### How to uninstall: 
* Use 3rd party recovery: Download and flash the `uninstaller.zip` in recovery.
* Use root permission: Tap "Install as system app" option, select "Uninstall" and confirm. Reboot your device and uninstall the app normally.

### How to backup or restore settings of Intent Firewall: 
You can use import/export IFW options in settings page of the app.

*Caveat: The backup for IFW settings is automatically saved at `/sdcard/Android/data/com.zzzmode.appopsx.sys/files/backup/ifw_backup.xml`. But this backup file will be removed if you uninstall the app or clear its data. If you allow storage permission, another backup will be automatically saved at `/sdcard/Android/ifw_backup.xml`.*

### License
Same as the original project.
