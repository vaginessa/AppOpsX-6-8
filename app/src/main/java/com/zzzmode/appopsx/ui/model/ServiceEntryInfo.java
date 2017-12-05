package com.zzzmode.appopsx.ui.model;

/**
 * Created by linusyang on 12/1/17.
 */

public class ServiceEntryInfo {
    public String packageName;
    public String serviceName;
    public boolean serviceEnabled;
    public String tag;
    public RunningStatus isRunning;

    public enum RunningStatus { DISABLED, NOT_RUNNING, RUNNING, PERSISTENT, FOREGROUND }

    public ServiceEntryInfo(String packageName, String serviceName, boolean enabled, String isBroadcast, RunningStatus isRunning) {
        this.packageName = packageName;
        this.serviceName = serviceName;
        this.serviceEnabled = enabled;
        this.tag = isBroadcast;
        this.isRunning = isRunning;
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
                ", tag='" + tag + '\'' +
                '}';
    }
}
