package com.zzzmode.appopsx;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.UserHandle;
import android.app.ActivityThread;
import java.util.Map;
import java.util.HashMap;
import com.android.internal.app.IAppOpsService;
import com.zzzmode.appopsx.common.OpsCommands;
import com.zzzmode.appopsx.common.OpsResult;
import com.zzzmode.appopsx.common.FLog;
import com.zzzmode.appopsx.common.OpEntry;
import com.zzzmode.appopsx.common.OtherOp;
import com.zzzmode.appopsx.common.PackageOps;
import com.zzzmode.appopsx.common.ReflectUtils;
import java.util.ArrayList;
import java.util.List;
import android.os.ServiceManager;
import android.Manifest;

/**
 * Created by zl on 2016/11/13.
 */

class LocalServerManager {

  private static final String TAG = "LocalServerManager";

  private static LocalServerManager sLocalServerManager;

  private OpsxManager.Config mConfig;

  static LocalServerManager getInstance(OpsxManager.Config config) {
    if (sLocalServerManager == null) {
      synchronized (LocalServerManager.class) {
        if (sLocalServerManager == null) {
          sLocalServerManager = new LocalServerManager(config);
        }
      }
    }
    return sLocalServerManager;
  }

  private LocalServerManager(OpsxManager.Config config) {
    mConfig = config;
  }

  void updateConfig(OpsxManager.Config config) {
    if (config != null) {
      mConfig = config;
    }
  }

  OpsxManager.Config getConfig() {
    return mConfig;
  }

  void start() {}

  public boolean isRunning() {
    return true;
  }

  public void stop() {
  }

  public OpsResult exec(OpsCommands.Builder builder) throws Exception {
    return handleCommand(builder);
  }

  private OpsResult handleCommand(OpsCommands.Builder builder) throws Exception {
    String s = builder.getAction();
    if (OpsCommands.ACTION_GET.equals(s)) {
      return runGet(builder);
    } else if (OpsCommands.ACTION_SET.equals(s)) {
      return runSet(builder);
    } else if (OpsCommands.ACTION_RESET.equals(s)) {
      return runReset(builder);
    } else if (OpsCommands.ACTION_GET_FOR_OPS.equals(s)) {
      return runGetForOps(builder);
    }
    return new OpsResult(null, null);
  }

  static int getPackageUid(String packageName, int userId) {
    int uid = -1;
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        uid = ActivityThread.getPackageManager().getPackageUid(packageName,PackageManager.MATCH_UNINSTALLED_PACKAGES,userId);
      } else {
        uid = ActivityThread.getPackageManager().getPackageUid(packageName, userId);
      }
    } catch (Throwable e) {
      FLog.log(e);
    }

    if (uid == -1) {
      try {
        ApplicationInfo applicationInfo = ActivityThread.getPackageManager()
                .getApplicationInfo(packageName, 0, userId);
        List<Class> paramsType = new ArrayList<>(2);
        paramsType.add(int.class);
        paramsType.add(int.class);
        List<Object> params = new ArrayList<>(2);
        params.add(userId);
        params.add(applicationInfo.uid);
        uid = (int) ReflectUtils.invokMethod(UserHandle.class, "getUid", paramsType, params);
      } catch (Throwable e) {
        FLog.log(e);
      }
    }

    return uid;
  }

  private static Map<String, Integer> sRuntimePermToOp = null;

  static int permissionToCode(String permission) {
    if (sRuntimePermToOp == null) {
      sRuntimePermToOp = new HashMap<>();
      Object sOpPerms = ReflectUtils.getFieldValue(AppOpsManager.class, "sOpPerms");
      Object sOpToSwitch = ReflectUtils.getFieldValue(AppOpsManager.class, "sOpToSwitch");

      if (sOpPerms instanceof String[] && sOpToSwitch instanceof int[]) {
        String[] opPerms = (String[]) sOpPerms;
        int[] opToSwitch = (int[]) sOpToSwitch;

        if (opPerms.length == opToSwitch.length) {
          for (int i = 0; i < opToSwitch.length; i++) {
            if (opPerms[i] != null) {
              sRuntimePermToOp.put(opPerms[i], opToSwitch[i]);
            }
          }
        }
      }
    }
    Integer code = sRuntimePermToOp.get(permission);
    if (code != null) {
      return code;
    }
    return -1;
  }

  private OpsResult runGet(OpsCommands.Builder getBuilder) throws Exception {

    FLog.log("runGet sdk:" + Build.VERSION.SDK_INT);

    final IAppOpsService appOpsService = IAppOpsService.Stub.asInterface(
            ServiceManager.getService(Context.APP_OPS_SERVICE));
    String packageName = getBuilder.getPackageName();

    int uid = getPackageUid(packageName, getBuilder.getUserHandleId());

    List opsForPackage = appOpsService.getOpsForPackage(uid, packageName, null);
    List<PackageOps> packageOpses = new ArrayList<>();
    if (opsForPackage != null) {
      for (Object o : opsForPackage) {
        PackageOps packageOps = ReflectUtils.opsConvert(o);
        addSupport(appOpsService, packageOps, getBuilder.getUserHandleId());
        packageOpses.add(packageOps);
      }
    } else {
      PackageOps packageOps = new PackageOps(packageName, uid, new ArrayList<OpEntry>());
      addSupport(appOpsService, packageOps, getBuilder.getUserHandleId());
      packageOpses.add(packageOps);
    }

    return sendOpResult(new OpsResult(packageOpses, null));

  }

  private void addSupport(IAppOpsService appOpsService, PackageOps ops, int userHandleId) {
    try {
      PackageInfo packageInfo = ActivityThread.getPackageManager()
              .getPackageInfo(ops.getPackageName(), PackageManager.GET_PERMISSIONS, userHandleId);
      if (packageInfo != null && packageInfo.requestedPermissions != null) {
        for (String permission : packageInfo.requestedPermissions) {
          int code = permissionToCode(permission);

          if (code <= 0) {
            //correct OP_WIFI_SCAN code.
            if (Manifest.permission.ACCESS_WIFI_STATE.equals(permission)) {
              code = OtherOp.getWifiScanOp();
            }
          }

          if (code > 0 && !ops.hasOp(code)) {
            int mode = appOpsService.checkOperation(code, ops.getUid(), ops.getPackageName());
            if (mode != AppOpsManager.MODE_ERRORED) {
              //
              ops.getOps().add(new OpEntry(code, mode, 0, 0, 0, 0, null));
            }
          }
        }
      }

    } catch (Throwable e) {
      FLog.log(e);
    }
  }

  private OpsResult runSet(OpsCommands.Builder builder) throws Exception {

    final int uid = getPackageUid(builder.getPackageName(), builder.getUserHandleId());
    if (OtherOp.isOtherOp(builder.getOpInt())) {
    } else {
      final IAppOpsService appOpsService = IAppOpsService.Stub.asInterface(
              ServiceManager.getService(Context.APP_OPS_SERVICE));
      appOpsService
              .setMode(builder.getOpInt(), uid, builder.getPackageName(), builder.getModeInt());
    }

    return sendOpResult(new OpsResult(null, null));

  }

  private OpsResult runReset(OpsCommands.Builder builder) throws Exception {
    final IAppOpsService appOpsService = IAppOpsService.Stub.asInterface(
            ServiceManager.getService(Context.APP_OPS_SERVICE));

    appOpsService.resetAllModes(builder.getUserHandleId(), builder.getPackageName());
    return sendOpResult(new OpsResult(null, null));
  }

  private OpsResult runGetForOps(OpsCommands.Builder builder) throws Exception {

    final IAppOpsService appOpsService = IAppOpsService.Stub.asInterface(
            ServiceManager.getService(Context.APP_OPS_SERVICE));

    List opsForPackage = appOpsService.getPackagesForOps(builder.getOps());
    List<PackageOps> packageOpses = new ArrayList<>();

    if (opsForPackage != null) {
      for (Object o : opsForPackage) {
        PackageOps packageOps = ReflectUtils.opsConvert(o);
        addSupport(appOpsService, packageOps, builder.getUserHandleId());
        packageOpses.add(packageOps);
      }

      FLog.log("runGetForOps ---- "+packageOpses.size());
    }

    return sendOpResult(new OpsResult(packageOpses, null));

  }

  private OpsResult sendOpResult(OpsResult result) {
    return result;
  }


  public static void closeBgServer() {}
}
