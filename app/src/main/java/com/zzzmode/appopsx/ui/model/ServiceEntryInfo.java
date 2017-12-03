package com.zzzmode.appopsx.ui.model;

/**
 * Created by linusyang on 12/1/17.
 */

public class ServiceEntryInfo {
    public String packageName;
    public String serviceName;
    public boolean serviceEnabled;
    public boolean isBroadcast;

    public ServiceEntryInfo(String packageName, String serviceName, boolean enabled, boolean isBroadcast) {
        this.packageName = packageName;
        this.serviceName = serviceName;
        this.serviceEnabled = enabled;
        this.isBroadcast = isBroadcast;
    }

    public void changeStatus() {
        this.serviceEnabled = !this.serviceEnabled;
    }

    @Override
    public String toString() {
        return "ServiceEntryInfo{" +
                ", packageName='" + packageName + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", serviceEnabled='" + serviceEnabled + '\'' +
                ", isBroadcast='" + isBroadcast + '\'' +
                '}';
    }
}
