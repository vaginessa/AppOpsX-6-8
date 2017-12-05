package com.zzzmode.appopsx.ui.service;

import android.graphics.Color;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator;
import com.zzzmode.appopsx.R;
import com.zzzmode.appopsx.ui.model.ServiceEntryInfo;
import com.zzzmode.appopsx.ui.widget.CommonDivderDecorator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.ResourceObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by zl on 2017/1/23.
 */
class SearchHandler {

  private static final String TAG = "SearchHandler";

  private List<ServiceEntryInfo> mBaseData;

  private RecyclerView recyclerView;
  private SearchResultAdapter mAdapter;
  private ServiceAdapter.IServiceCopy copier;
  private ServiceAdapter.OnSwitchItemClickListener listener;

  void setBaseData(List<ServiceEntryInfo> baseData) {
    this.mBaseData = baseData;
  }

  ServiceAdapter getAdapter() {
    return mAdapter;
  }

  void initView(View container) {
    this.recyclerView = (RecyclerView) container.findViewById(R.id.search_result_recyclerView);
    recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
    recyclerView.addItemDecoration(new CommonDivderDecorator(recyclerView.getContext()));
    recyclerView.setItemAnimator(new RefactoredDefaultItemAnimator());
    mAdapter = new SearchResultAdapter();
    mAdapter.setCopier(copier);
    mAdapter.setListener(listener);
    recyclerView.setAdapter(mAdapter);
  }

  void handleWord(final String text) {
    if (TextUtils.isEmpty(text)) {
      mAdapter.setDatas(Collections.<ServiceEntryInfo>emptyList());
      mAdapter.notifyDataSetChanged();
      return;
    }

    search(text)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread()).subscribe(new ResourceObserver<List<ServiceEntryInfo>>() {


      @Override
      protected void onStart() {
        super.onStart();
      }

      @Override
      public void onNext(List<ServiceEntryInfo> value) {

        mAdapter.kw = text;
        mAdapter.setDatas(value);
        mAdapter.notifyDataSetChanged();
        if (!value.isEmpty()) {
          recyclerView.scrollToPosition(0);
        }
      }

      @Override
      public void onError(Throwable e) {

      }

      @Override
      public void onComplete() {

      }
    });


  }


  private Observable<List<ServiceEntryInfo>> search(final String key) {
    return Observable.create(new ObservableOnSubscribe<List<ServiceEntryInfo>>() {
      @Override
      public void subscribe(ObservableEmitter<List<ServiceEntryInfo>> e) throws Exception {
        Pattern p = Pattern.compile(".*(?i)(" + key + ").*");
        List<ServiceEntryInfo> result = new ArrayList<>();
        for (ServiceEntryInfo info : mBaseData) {
          String candidate = info.serviceName;
          if (!mAdapter.showFullName) {
            String[] s = candidate.split("\\.");
            candidate = s[s.length - 1];
          }
          if (p.matcher(candidate).matches()) {
            result.add(info);
          }
        }
        e.onNext(result);
      }
    });
  }

  public void setCopier(ServiceAdapter.IServiceCopy copier) {
    this.copier = copier;
  }

  public void setListener(ServiceAdapter.OnSwitchItemClickListener listener) {
    this.listener = listener;
  }

  private static class SearchResultAdapter extends ServiceAdapter {

    String kw;

    private int color = Color.parseColor("#FF4081");

    @Override
    protected CharSequence processText(String name) {
      return resultHighlight(kw, name, color);
    }

    @Override
    protected boolean textShouldBeProcessed() {
      return true;
    }

    private CharSequence resultHighlight(String key, String text, int color) {
      String phantom = text.toLowerCase();
      String k = key != null ? key.toLowerCase() : null;

      if (k != null && phantom.contains(k)) {
        int st = 0;
        List<Integer> pos = new ArrayList<>(3);
        while ((st = phantom.indexOf(k, st)) != -1) {
          pos.add(st);
          st += key.length();
        }
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(text);
        if (!pos.isEmpty()) {
          for (Integer idx : pos) {
            stringBuilder.setSpan(new ForegroundColorSpan(color), idx, idx + key.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
          }
        }
        return stringBuilder;
      }
      return text;
    }
  }
}
