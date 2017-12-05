package com.zzzmode.appopsx.ui.service;

import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.zzzmode.appopsx.R;
import com.zzzmode.appopsx.ui.model.ServiceEntryInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zl on 2016/11/18.
 */
class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ViewHolder> implements
    View.OnClickListener, CompoundButton.OnCheckedChangeListener, View.OnLongClickListener {

  private List<ServiceEntryInfo> datas = new ArrayList<>();
  private IServiceCopy copier;

  private OnSwitchItemClickListener listener;

  private boolean showFullName;
  private boolean switchEnabled;
  private int HIGHLIGHT_COLOR = Color.BLUE;
  private int DISABLED_COLOR = Color.RED;

  void setShowConfig(boolean showFullName, boolean enabled) {
    this.showFullName = showFullName;
    this.switchEnabled = enabled;
  }

  void setDatas(List<ServiceEntryInfo> datas) {
    this.datas = datas;
  }

  List<ServiceEntryInfo> getDatas() {
    return datas;
  }

  void updateItem(ServiceEntryInfo info) {
    if (datas != null && info != null) {
      int i = datas.indexOf(info);
      if (i != -1 && i < datas.size()) {
        notifyItemChanged(i);
      }
    }
  }

  public void setListener(OnSwitchItemClickListener listener) {
    this.listener = listener;
  }

  public void setCopier(IServiceCopy copier) {
    this.copier = copier;
  }


  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new ViewHolder(LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_permission_item, parent, false));
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    ServiceEntryInfo opEntryInfo = datas.get(position);
//    holder.itemView.setOnClickListener(this);
    holder.itemView.setOnLongClickListener(this);
    holder.itemView.setTag(holder);

    holder.switchCompat.setTag(opEntryInfo);
    holder.summary.setVisibility(View.GONE);
    holder.lastTime.setVisibility(View.GONE);
    holder.title.setTypeface(null, Typeface.NORMAL);
    int defaultColor = copier.getDefaultColor();
    holder.title.setTextColor(defaultColor);

    if (opEntryInfo != null) {
      if (opEntryInfo.serviceName != null) {
        String[] shortNames = opEntryInfo.serviceName.split("\\.");
        holder.title.setText(shortNames[shortNames.length - 1]);
        switch (opEntryInfo.isRunning) {
          case DISABLED:
            holder.title.setTextColor(DISABLED_COLOR);
            break;
          case NOT_RUNNING:
            break;
          case RUNNING:
            holder.title.setTextColor(HIGHLIGHT_COLOR);
            holder.title.setTypeface(null, Typeface.BOLD);
            break;
          case PERSISTENT:
            holder.title.setTextColor(HIGHLIGHT_COLOR);
            holder.title.setTypeface(null, Typeface.ITALIC);
            break;
          case FOREGROUND:
            holder.title.setTextColor(HIGHLIGHT_COLOR);
            holder.title.setTypeface(null, Typeface.BOLD_ITALIC);
            break;
        }
        if (showFullName) {
          holder.summary.setVisibility(View.VISIBLE);
          holder.summary.setText(opEntryInfo.serviceName);
        } else {
          holder.summary.setVisibility(View.GONE);
        }
      }

      holder.switchCompat.setOnCheckedChangeListener(null);
      holder.switchCompat.setChecked(opEntryInfo.serviceEnabled);
      holder.switchCompat.setOnCheckedChangeListener(this);
      holder.switchCompat.setEnabled(switchEnabled);
    }
  }

  @Override
  public int getItemCount() {
    return datas.size();
  }

  @Override
  public void onClick(View v) {
    if (v.getTag() instanceof ViewHolder) {
      ((ViewHolder) v.getTag()).switchCompat.toggle();
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    if (buttonView.getTag() instanceof ServiceEntryInfo && listener != null) {
      listener.onSwitch(((ServiceEntryInfo) buttonView.getTag()), isChecked);
    }
  }

  @Override
  public boolean onLongClick(View v) {
    if (v.getTag() instanceof ViewHolder) {
      ServiceEntryInfo info = (ServiceEntryInfo) ((ViewHolder) v.getTag()).switchCompat.getTag();
      String toCopy = info.packageName + '/' + info.serviceName;
      copier.copyToPasteboard(toCopy);
    }
    return true;
  }

  static class ViewHolder extends RecyclerView.ViewHolder {

    ImageView icon;
    TextView title;
    TextView summary;
    TextView lastTime;
    SwitchCompat switchCompat;

    ViewHolder(View itemView) {
      super(itemView);
      icon = (ImageView) itemView.findViewById(R.id.img_group);
      title = (TextView) itemView.findViewById(android.R.id.title);
      summary = (TextView) itemView.findViewById(android.R.id.summary);
      lastTime = (TextView) itemView.findViewById(R.id.last_time);
      switchCompat = (SwitchCompat) itemView.findViewById(R.id.switch_compat);

    }
  }

  public interface OnSwitchItemClickListener {

    void onSwitch(ServiceEntryInfo info, boolean v);
  }

  public interface IServiceCopy {
    void copyToPasteboard(String serviceName);
    int getDefaultColor();
  }
}
