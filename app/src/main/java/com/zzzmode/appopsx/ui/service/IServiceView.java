package com.zzzmode.appopsx.ui.service;

import com.zzzmode.appopsx.ui.model.ServiceEntryInfo;

import java.util.List;

/**
 * Created by linusyang on 12/1/17.
 */

public interface IServiceView {
    void showProgress(boolean show);

    void showError(CharSequence text);

    void showServices(List<ServiceEntryInfo> opServiceInfos);

    void updateItem(ServiceEntryInfo info);
}
