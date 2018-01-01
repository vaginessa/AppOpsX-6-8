package com.zzzmode.appopsx.ui.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.ResourceObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by zl on 2017/1/16.
 */

public class BootReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
      Helper.syncNoBack(context.getApplicationContext())
              .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
              .subscribe(new ResourceObserver<Boolean>() {
                  @Override
                  public void onNext(Boolean value) {
                  }

                  @Override
                  public void onError(Throwable e) {
                  }

                  @Override
                  public void onComplete() {
                  }
              });
  }
}
