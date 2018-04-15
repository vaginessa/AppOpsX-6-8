package com.zzzmode.appopsx.ui.main;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.zzzmode.appopsx.R;
import com.zzzmode.appopsx.ui.analytics.AEvent;
import com.zzzmode.appopsx.ui.analytics.ATracker;
import com.zzzmode.appopsx.ui.core.SpHelper;
import com.zzzmode.appopsx.ui.model.AppInfo;
import com.zzzmode.appopsx.ui.permission.AppPermissionActivity;
import com.zzzmode.appopsx.ui.service.ServiceActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zl on 2016/11/18.
 */

class MainListAdapter extends RecyclerView.Adapter<AppItemViewHolder> implements
    View.OnClickListener, View.OnLongClickListener {

  protected List<AppInfo> appInfos = new ArrayList<>();
  private int HIGHLIGHT_COLOR = 0xff1976d2;
  private int DISABLED_COLOR = 0xfff50057;
  private int DEFAULT_COLOR = 0xFF212121;
  private int DEFAULT_COLOR_NIGHT = 0xf2ffffff;
  public boolean isNightMode = false;

  void addItem(AppInfo info) {
    appInfos.add(info);
    notifyItemInserted(appInfos.size() - 1);
  }

  void showItems(List<AppInfo> infos) {
    appInfos.clear();
    if (infos != null) {
      appInfos.addAll(infos);
    }
    notifyDataSetChanged();
  }

  List<AppInfo> getAppInfos() {
    return appInfos;
  }

  @Override
  public AppItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new AppItemViewHolder(
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_main_app, parent, false));
  }

  @Override
  public void onBindViewHolder(AppItemViewHolder holder, int position) {
    AppInfo appInfo = appInfos.get(position);
    holder.bindData(appInfo);

    holder.tvName.setText(processText(appInfo.appName));
    if (appInfo.hasRunningServices) {
      holder.tvName.setTextColor(HIGHLIGHT_COLOR);
      holder.tvName.setTypeface(null, Typeface.BOLD);
    } else if (appInfo.isDisabled) {
      holder.tvName.setTextColor(DISABLED_COLOR);
      holder.tvName.setTypeface(null, Typeface.NORMAL);
    } else {
      holder.tvName.setTypeface(null, Typeface.NORMAL);
      holder.tvName.setTextColor(isNightMode ? DEFAULT_COLOR_NIGHT : DEFAULT_COLOR);
    }
    holder.itemView.setTag(appInfo);
    holder.itemView.setOnClickListener(this);
    holder.itemView.setOnLongClickListener(this);
  }

  protected CharSequence processText(String name) {
    return name;
  }

  private Intent getNextIntent(View v, boolean ifw) {
    Intent intent;
    if (ifw) {
      intent = new Intent(v.getContext(), ServiceActivity.class);
      intent.putExtra(ServiceActivity.EXTRA_APP, ((AppInfo) v.getTag()));
    } else {
      intent = new Intent(v.getContext(), AppPermissionActivity.class);
      intent.putExtra(AppPermissionActivity.EXTRA_APP, ((AppInfo) v.getTag()));
    }
    return intent;
  }

  private boolean ifwUseShortClick(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("ifw_short_click", false);
  }

  @Override
  public int getItemCount() {
    return appInfos.size();
  }

  @Override
  public void onClick(View v) {
    if (v.getTag() instanceof AppInfo) {
      boolean ifwShortClick = ifwUseShortClick(v.getContext().getApplicationContext());
      Intent intent = getNextIntent(v, ifwShortClick);
      v.getContext().startActivity(intent);
    }
  }


  protected String getAEventId() {
    return AEvent.C_APP;
  }

  @Override
  public boolean onLongClick(View v) {
    if (v.getTag() instanceof AppInfo) {
      boolean ifwShortClick = ifwUseShortClick(v.getContext().getApplicationContext());
      Intent intent = getNextIntent(v, !ifwShortClick);
      v.getContext().startActivity(intent);
    }
    return true;
  }
}
