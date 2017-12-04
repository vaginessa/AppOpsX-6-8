package com.zzzmode.appopsx.ui.service;

import android.content.Context;
import android.preference.PreferenceManager;

import com.zzzmode.appopsx.R;
import com.zzzmode.appopsx.ui.core.AppOpsx;
import com.zzzmode.appopsx.ui.core.Helper;
import com.zzzmode.appopsx.ui.model.AppInfo;
import com.zzzmode.appopsx.ui.model.ServiceEntryInfo;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.ResourceObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by zl on 2017/5/1.
 */

class ServicePresenter {

    private static final String TAG = "ServicePresenter";

    private IServiceView mView;
    private Context context;
    private AppInfo appInfo;

    private Observable<List<ServiceEntryInfo>> observable;

    private boolean loadSuccess = false;

    ServicePresenter(IServiceView mView, AppInfo appInfo, Context context) {
        this.mView = mView;
        this.context = context;
        this.appInfo = appInfo;
    }

    void setUp() {
        mView.showProgress(!AppOpsx.getInstance(context).isRunning());
        load();
    }

    void load() {
    observable = Helper.getAppServices(context, appInfo.packageName,
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(ServiceActivity.KEY_BLOCK_TYPE,
                               ServiceActivity.DEFAULT_BLOCK_TYPE));

        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ResourceObserver<List<ServiceEntryInfo>>() {

                    @Override
                    protected void onStart() {
                        super.onStart();
                    }

                    @Override
                    public void onNext(List<ServiceEntryInfo> opEntryInfos) {

                        if (opEntryInfos != null && !opEntryInfos.isEmpty()) {
                            mView.showProgress(false);
                            mView.showServices(opEntryInfos);
                        } else {
                            mView.showError(context.getString(R.string.no_services,
                                    ServiceActivity.getBlockTypeString(context).toLowerCase(Locale.US)));
                        }
                        loadSuccess = true;
                    }

                    @Override
                    public void onError(Throwable e) {
                        mView.showError(getHandleError(e));
                        loadSuccess = false;
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private String getHandleError(Throwable e) {
        String msg = "";
        String errorMsg = e.getMessage();
        if (e instanceof IOException) {
            if (errorMsg.contains("error=13")) {
                msg = context.getString(R.string.error_no_su);
            }
        } else if (e instanceof RuntimeException) {
            if (errorMsg.contains("RootAccess denied")) {
                msg = context.getString(R.string.error_su_timeout);
            } else if (errorMsg.contains("connect fail")) {
                msg = context.getString(R.string.error_connect_fail);
            }
        }
        return context.getString(R.string.error_msg, msg, errorMsg);
    }

    void switchMode(ServiceEntryInfo info, boolean v) {
        info.serviceEnabled = v;
        setMode(info);
    }

    void setMode(final ServiceEntryInfo info) {
        Helper.setService(context, appInfo.packageName, info)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ResourceObserver<Boolean>() {
                    @Override
                    public void onNext(Boolean value) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        mView.updateItem(info);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    void setModes(final List<ServiceEntryInfo> infos) {
        Helper.setBatchService(context, appInfo.packageName, infos)
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

    void destory() {
        try {
            if (observable != null) {
                observable.unsubscribeOn(Schedulers.io());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean isLoadSuccess() {
        return loadSuccess;
    }
}
