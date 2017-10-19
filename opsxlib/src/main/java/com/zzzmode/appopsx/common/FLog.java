package com.zzzmode.appopsx.common;

import com.zzzmode.appopsx.BuildConfig;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by zl on 2017/2/26.
 */
public class FLog {
  public static void log(String log) {
    if (BuildConfig.DEBUG)
      Log.d("appopsx", "Flog --> " + log);
  }

  public static void log(Throwable e) {
    log(Log.getStackTraceString(e));
  }
}

