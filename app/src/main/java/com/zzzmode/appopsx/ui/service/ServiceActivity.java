package com.zzzmode.appopsx.ui.service;

import android.app.SearchManager;
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
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.zzzmode.appopsx.R;
import com.zzzmode.appopsx.ui.BaseActivity;
import com.zzzmode.appopsx.ui.core.Helper;
import com.zzzmode.appopsx.ui.model.AppInfo;
import com.zzzmode.appopsx.ui.model.ServiceEntryInfo;
import com.zzzmode.appopsx.ui.widget.CommonDivderDecorator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by linusyang on 12/1/17.
 */

public class ServiceActivity extends BaseActivity implements
        IServiceView, ServiceAdapter.IServiceCopy, SearchView.OnQueryTextListener,
        ServiceAdapter.OnSwitchItemClickListener {
    private static final String TAG = "ServiceActivity";

    public static final String EXTRA_APP = "extra.app";
    public static final String EXTRA_APP_PKGNAME = "pkgName";
    public static final String EXTRA_APP_NAME = "appName";
    public static final String KEY_BLOCK_TYPE = "key_ifw_block_type";
    public static final String DEFAULT_BLOCK_TYPE = "service";
    public static final String KEY_FULL_NAME = "key_show_full_name";
    public static final String KEY_IFW_ENABLED = "ifw_enabled";


    private ProgressBar mProgressBar;
    private TextView tvError;
    private ServicePresenter mPresenter;
    private ServiceAdapter adapter;
    private ServiceAdapter activeAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private String pkgName;
    private View containerApp, containerSearch;
    private SearchHandler mSearchHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opsx);

        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }

        containerApp = findViewById(R.id.container_app);
        containerSearch = findViewById(R.id.container_search);
        mSearchHandler = new SearchHandler();
        mSearchHandler.setCopier(this);
        mSearchHandler.setListener(this);
        mSearchHandler.initView(containerSearch);
        setAdapterConfig(mSearchHandler.getAdapter());

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
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefreshlayout);
        mSwipeRefreshLayout.setRefreshing(false);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        mSwipeRefreshLayout.setEnabled(true);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.addItemDecoration(new CommonDivderDecorator(getApplicationContext()));

        adapter = new ServiceAdapter();
        adapter.setCopier(this);
        recyclerView.setAdapter(adapter);

        adapter.setListener(this);
        activeAdapter = adapter;

        pkgName = appInfo.packageName;
        mPresenter = new ServicePresenter(this, appInfo, getApplicationContext());
        mPresenter.setUp();

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mPresenter.load();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!getConfig(KEY_IFW_ENABLED, true)) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.ifw_disabled_hint), Toast.LENGTH_LONG).show();
        }
    }

    private boolean getConfig(String key, boolean defValue) {
        final SharedPreferences sp =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return sp.getBoolean(key, defValue);
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

    public static int getBlockTypeIndex(Context context) {
        String nowType = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_BLOCK_TYPE, DEFAULT_BLOCK_TYPE);
        List<String> typeList = Arrays.asList(context.getResources().getStringArray(R.array.ifw_block_type));
        int defSelected = typeList.indexOf(nowType);
        if (defSelected < 0) {
            defSelected = 0;
        }
        return defSelected;
    }

    public static String getBlockTypeString(Context context) {
        return context.getResources()
                .getStringArray(R.array.ifw_block_type_display)[getBlockTypeIndex(context)];
    }

    private void showBlockTypeDialog() {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_show_broadcast);

        final int lastSelected = getBlockTypeIndex(getApplicationContext());
        builder.setSingleChoiceItems(R.array.ifw_block_type_display, lastSelected,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (which != lastSelected) {
                            String blockType = getResources().getStringArray(R.array.ifw_block_type)[which];
                            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
                                    .putString(KEY_BLOCK_TYPE, blockType).apply();
                            ActivityCompat.invalidateOptionsMenu(ServiceActivity.this);
                            mPresenter.load();
                        }
                    }
                });
        builder.setNegativeButton(android.R.string.cancel, null);
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
        final MenuItem searchMenu = menu.findItem(R.id.action_search);
        final MenuItem settingsMenu = menu.findItem(R.id.action_show_broadcast);
        final MenuItem premsMenu = menu.findItem(R.id.action_show_full_name);
        final MenuItem infoMenu = menu.findItem(R.id.action_service_app_info);

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        menuShowFullname.setChecked(getConfig(KEY_FULL_NAME, false));
        menuShowFullname.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                item.setChecked(!item.isChecked());
                sp.edit().putBoolean(KEY_FULL_NAME, item.isChecked()).apply();
                ActivityCompat.invalidateOptionsMenu(ServiceActivity.this);
                refreshAdapter(null);
                setAdapterConfig(mSearchHandler.getAdapter());
                return true;
            }
        });

        searchMenu
                .setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        containerApp.setVisibility(View.GONE);
                        containerSearch.setVisibility(View.VISIBLE);

                        settingsMenu.setVisible(false);
                        premsMenu.setVisible(false);
                        infoMenu.setVisible(false);
                        activeAdapter = mSearchHandler.getAdapter();

                        ActivityCompat.invalidateOptionsMenu(ServiceActivity.this);
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        containerApp.setVisibility(View.VISIBLE);
                        containerSearch.setVisibility(View.GONE);

                        settingsMenu.setVisible(true);
                        premsMenu.setVisible(true);
                        infoMenu.setVisible(true);
                        activeAdapter = adapter;
                        refreshAdapter(null);

                        ActivityCompat.invalidateOptionsMenu(ServiceActivity.this);
                        return true;
                    }
                });

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) searchMenu.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuBlockType = menu.findItem(R.id.action_show_broadcast);
        menuBlockType.setTitle(getString(R.string.menu_block_type,
                getBlockTypeString(getApplicationContext())));
        return true;
    }

    private void showHidePerms() {

    }

    private void changeAll(boolean enabled) {
        if (!getConfig(KEY_IFW_ENABLED, true)) {
            return;
        }
        final List<ServiceEntryInfo> datas = activeAdapter.getDatas();
        if (datas != null) {
            for (ServiceEntryInfo data : datas) {
                data.serviceEnabled = enabled;
            }
            activeAdapter.notifyDataSetChanged();
            mPresenter.setModes(datas);
        }
    }

    private void setAdapterConfig(ServiceAdapter adapter) {
        if (adapter == null) {
            return;
        }
        adapter.setShowConfig(getConfig(KEY_FULL_NAME, false),
                getConfig(KEY_IFW_ENABLED, true));
    }

    private void refreshAdapter(List<ServiceEntryInfo> infos) {
        if (adapter == null) {
            return;
        }
        if (infos != null) {
            adapter.setDatas(infos);
            mSearchHandler.setBaseData(infos);
        }
        setAdapterConfig(adapter);
        adapter.notifyDataSetChanged();
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
        mSwipeRefreshLayout.setRefreshing(false);
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(text);
        refreshAdapter(Collections.<ServiceEntryInfo>emptyList());
        ActivityCompat.invalidateOptionsMenu(ServiceActivity.this);
    }

    @Override
    public void showServices(List<ServiceEntryInfo> opEntryInfos) {
        mSwipeRefreshLayout.setRefreshing(false);
        refreshAdapter(opEntryInfos);
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
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Service", serviceName);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(ctx, ctx.getString(R.string.copied_hint), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mSearchHandler.handleWord(newText);
        return true;
    }

    @Override
    public void onSwitch(ServiceEntryInfo info, boolean v) {
        mPresenter.switchMode(info, v);
    }
}
