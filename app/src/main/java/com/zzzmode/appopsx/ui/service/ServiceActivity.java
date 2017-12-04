package com.zzzmode.appopsx.ui.service;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.zzzmode.appopsx.R;
import com.zzzmode.appopsx.ui.BaseActivity;
import com.zzzmode.appopsx.ui.analytics.AEvent;
import com.zzzmode.appopsx.ui.analytics.ATracker;
import com.zzzmode.appopsx.ui.core.Helper;
import com.zzzmode.appopsx.ui.core.LangHelper;
import com.zzzmode.appopsx.ui.model.AppInfo;
import com.zzzmode.appopsx.ui.model.ServiceEntryInfo;
import com.zzzmode.appopsx.ui.widget.CommonDivderDecorator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by linusyang on 12/1/17.
 */

public class ServiceActivity extends BaseActivity implements IServiceView, ServiceAdapter.IServiceCopy {
    private static final String TAG = "ServiceActivity";

    public static final String EXTRA_APP = "extra.app";
    public static final String EXTRA_APP_PKGNAME = "pkgName";
    public static final String EXTRA_APP_NAME = "appName";


    private ProgressBar mProgressBar;
    private TextView tvError;
    private ServicePresenter mPresenter;
    private ServiceAdapter adapter;

    private String pkgName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opsx);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        AppInfo appInfo = handleIntent(getIntent());
        if(appInfo == null){
            finish();
            return;
        }

        if(TextUtils.isEmpty(appInfo.appName)){
            loadAppinfo(appInfo.packageName);
        }else {
            setTitle(appInfo.appName);
        }


        tvError = (TextView) findViewById(R.id.tv_error);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.addItemDecoration(new CommonDivderDecorator(getApplicationContext()));

        adapter = new ServiceAdapter();
        adapter.setCopier(this);
        recyclerView.setAdapter(adapter);

        adapter.setListener(new ServiceAdapter.OnSwitchItemClickListener() {
            @Override
            public void onSwitch(ServiceEntryInfo info, boolean v) {
                mPresenter.switchMode(info, v);
            }
        });

        pkgName = appInfo.packageName;
        mPresenter = new ServicePresenter(this, appInfo, getApplicationContext());
        mPresenter.setUp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (!sp.getBoolean("ifw_enabled", true)) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.ifw_disabled_hint), Toast.LENGTH_LONG).show();
        }
    }


    private AppInfo handleIntent(Intent intent){
        AppInfo appInfo = intent.getParcelableExtra(EXTRA_APP);
        if(appInfo == null){
            //find from extra
            String pkgName = intent.getStringExtra(EXTRA_APP_PKGNAME);
            if(TextUtils.isEmpty(pkgName) && intent.getData() != null){
                pkgName = intent.getData().getQueryParameter("id");
            }
            if(!TextUtils.isEmpty(pkgName)){
                appInfo = new AppInfo();
                appInfo.packageName = pkgName;
            }

        }
        return appInfo;
    }

    private void loadAppinfo(String pkgName){
        Helper.getAppInfo(getApplicationContext(),pkgName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<AppInfo>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onSuccess(@NonNull AppInfo appInfo) {
                        setTitle(appInfo.appName);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPresenter != null) {
            mPresenter.destory();
        }
    }

    private int getBlockTypeIndex() {
        String nowType = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getString("key_ifw_block_type", "service");
        List<String> typeList = Arrays.asList(getResources().getStringArray(R.array.ifw_block_type));
        int defSelected = typeList.indexOf(nowType);
        if (defSelected < 0) {
            defSelected = 0;
        }
        return defSelected;
    }

    private void showBlockTypeDialog() {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_show_broadcast);

        final int[] selected = new int[1];
        final int lastSelected = getBlockTypeIndex();

        builder.setSingleChoiceItems(R.array.ifw_block_type_display, lastSelected,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selected[0] = which;
                    }
                });

        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (selected[0] != lastSelected) {
                    String blockType = getResources().getStringArray(R.array.ifw_block_type)[selected[0]];
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
                            .putString("key_ifw_block_type", blockType).apply();
                    ActivityCompat.invalidateOptionsMenu(ServiceActivity.this);
                    mPresenter.load();
                }
            }
        });
        builder.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_show_full_name:
                showHidePerms();
                return true;
            case R.id.action_service_enable_all:
                changeAll(true);
                break;
            case R.id.action_service_disable_all:
                changeAll(false);
                break;
            case R.id.action_service_app_info:
                startAppinfo();
                break;
            case R.id.action_show_broadcast:
                showBlockTypeDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        if (!mPresenter.isLoadSuccess()) {
            return false;
        }

        getMenuInflater().inflate(R.menu.service_menu, menu);

        MenuItem menuShowFullname = menu.findItem(R.id.action_show_full_name);

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        final Map<MenuItem, String> menus = new HashMap<>();
        menus.put(menuShowFullname, "key_show_full_name");

        MenuItem.OnMenuItemClickListener itemClickListener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String s = menus.get(item);
                if (s != null) {
                    item.setChecked(!item.isChecked());
                    sp.edit().putBoolean(s, item.isChecked()).apply();
                    ActivityCompat.invalidateOptionsMenu(ServiceActivity.this);
                    mPresenter.load();
                }
                return true;
            }
        };

        Set<Map.Entry<MenuItem, String>> entries = menus.entrySet();
        for (Map.Entry<MenuItem, String> entry : entries) {
            entry.getKey().setChecked(sp.getBoolean(entry.getValue(), false));
            entry.getKey().setOnMenuItemClickListener(itemClickListener);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuBlockType = menu.findItem(R.id.action_show_broadcast);
        menuBlockType.setTitle(getString(R.string.menu_block_type,
                getResources().getStringArray(R.array.ifw_block_type_display)[getBlockTypeIndex()]));
        return true;
    }

    private void showHidePerms() {

    }

    private void changeAll(boolean enabled) {
        final List<ServiceEntryInfo> datas = adapter.getDatas();
        if (datas != null) {
            for (ServiceEntryInfo data : datas) {
                data.serviceEnabled = enabled;
                adapter.updateItem(data);
            }
            mPresenter.setModes(datas);
        }
    }


    @Override
    public void showProgress(boolean show) {
        tvError.setVisibility(View.GONE);
        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);

        ActivityCompat.invalidateOptionsMenu(ServiceActivity.this);
    }

    @Override
    public void showError(CharSequence text) {
        mProgressBar.setVisibility(View.GONE);
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(text);

        ActivityCompat.invalidateOptionsMenu(ServiceActivity.this);
    }

    @Override
    public void showServices(List<ServiceEntryInfo> opEntryInfos) {
        final SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        adapter.setShowConfig(sp.getBoolean("key_show_full_name", false),
                sp.getBoolean("ifw_enabled", true));
        adapter.setDatas(opEntryInfos);
        adapter.notifyDataSetChanged();

        ActivityCompat.invalidateOptionsMenu(ServiceActivity.this);
    }

    @Override
    public void updateItem(ServiceEntryInfo info) {
        adapter.updateItem(info);

        //Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
    }


    private void startAppinfo(){
        Intent intent=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package",pkgName,null));
        startActivity(intent);
    }

    @Override
    public void copyToPasteboard(String serviceName) {
        Context ctx = getApplicationContext();
        ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Service", serviceName);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(ctx, ctx.getString(R.string.copied_hint), Toast.LENGTH_SHORT).show();
    }
}
