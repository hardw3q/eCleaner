package com.kikimore.ecleaner.model;

import android.content.pm.ApplicationInfo;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class GroupItem implements Parcelable {
    public static final int TYPE_FILE = 0;
    public static final int TYPE_CACHE = 1;
    public static final int TYPE_APP_DATA = 5;
    public static final int TYPE_APP_CACHE = 6;
    public static final int TYPE_RESIDUAL_FILES = 8;

    private String title;
    private long total;
    private boolean isCheck;
    private int type;
    private List<ApplicationInfo> Appitems = new ArrayList<>();
    private List<ChildItem> items = new ArrayList<>();

    public GroupItem() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public boolean isCheck() {
        return isCheck;
    }

    public void setIsCheck(boolean isCheck) {
        this.isCheck = isCheck;
    }

    public List<ChildItem> getItems() {
        return items;
    }

    public void setItems(List<ChildItem> items) {
        this.items = items;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<ApplicationInfo> getAppItems() {
        return Appitems;
    }


    public void setAppItems(List<ApplicationInfo> Appitems) {
        this.Appitems = Appitems;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
