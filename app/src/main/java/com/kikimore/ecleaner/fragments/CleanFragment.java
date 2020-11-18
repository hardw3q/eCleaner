package com.kikimore.ecleaner.fragments;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.kikimore.ecleaner.MainActivity;
import com.kikimore.ecleaner.R;
import com.kikimore.ecleaner.adapter.AnimatedExpandableListView;
import com.kikimore.ecleaner.adapter.CleanAdapter;
import com.kikimore.ecleaner.asyncTask.ManagerConnect;
import com.kikimore.ecleaner.asyncTask.TaskClean;
import com.kikimore.ecleaner.model.ChildItem;
import com.kikimore.ecleaner.model.GroupItem;
import com.kikimore.ecleaner.model.GroupItemAppManager;
import com.kikimore.ecleaner.utils.Utils;
import com.kikimore.ecleaner.view.RotateLoading;

public class CleanFragment extends BaseFragment {

    private static final String TAG = CleanFragment.class.getName();
    private AnimatedExpandableListView mRecyclerView;
    private TextView mTvTotalCache;
    private TextView mTvType;
    private TextView mTvTotalFound;
    private TextView mTvNoJunk;
    private Button mBtnCleanUp;
    private TextView mTvSuggested;

    private LinearLayout mViewLoading;
    private RotateLoading mRotateloadingApks;
    private RotateLoading mRotateloadingCache;
    private RotateLoading mRotateloadingDownloadFiles;
    private RotateLoading mRotateloadingLargeFiles;
    private RotateLoading mRotateloadingAppCache;
    private RotateLoading mRotateloadingAppData;

    private long mTotalSizeSystemCache;
    private long mTotalSizeFiles;
    private long mTotalSizeApk;
    private long mTotalSizeLargeFiles;
    private static long mTotalSizeAppData;
    private long mTotalSizeResidualFiles;
    private long mTotalAppCache;

    private ArrayList<File> mFileListLarge = new ArrayList<>();

    private ArrayList<GroupItem> mGroupItems = new ArrayList<>();
    private List<GroupItemAppManager> mGroupItems2 = new ArrayList<>();
    private CleanAdapter mAdapter;

    private ScanApkFiles mScanApkFiles;
    private TaskScan mTaskScan;
    private ScanDownLoadFiles mScanDownLoadFiles;
    private ScanLargeFiles mScanLargeFiles;
    private ScanResidualFiles mScanResidualFiles;
    static long totalOnClearLength = 0;

    private boolean mIsFragmentPause;
    View view;
    public static final int EXTDIR_REQUEST_CODE = 1110,MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE=300;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // TODO Auto-generated method stub

        view = inflater.inflate(R.layout.fragment_clean, container, false);
        intView();
        return view;
    }
    public void intView(){
        mViewLoading = (LinearLayout) view.findViewById(R.id.viewLoading);

        mRotateloadingApks = (RotateLoading) view.findViewById(R.id.rotateloadingApks);
        mRotateloadingCache = (RotateLoading) view.findViewById(R.id.rotateloadingCache);
        mRotateloadingDownloadFiles = (RotateLoading) view.findViewById(R.id.rotateloadingDownload);
        mRotateloadingLargeFiles = (RotateLoading) view.findViewById(R.id.rotateloadingLargeFiles);
        mRotateloadingAppCache = view.findViewById(R.id.rotateloadingAppCache);
        mRotateloadingAppData = view.findViewById(R.id.rotateloadingAppData);

        mRecyclerView = (AnimatedExpandableListView) view.findViewById(R.id.recyclerView);
        mTvTotalCache = (TextView) view.findViewById(R.id.tvTotalCache);
        mTvType = (TextView) view.findViewById(R.id.tvType);
        mTvTotalFound = (TextView) view.findViewById(R.id.tvTotalFound);
        mTvNoJunk = (TextView) view.findViewById(R.id.tvNoJunk);
        mBtnCleanUp = (Button) view.findViewById(R.id.btnCleanUp);
        mBtnCleanUp.setVisibility(View.GONE);
        mBtnCleanUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cleanUp();
            }
        });

        mTotalSizeSystemCache = 0;
        mTotalSizeFiles = 0;
        mTotalSizeApk = 0;
        mTotalSizeLargeFiles = 0;
        mTotalSizeAppData = 0;
        mTotalAppCache = 0;
        mTotalSizeResidualFiles = 0;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());


        initAdapter();
        if (mGroupItems.size() == 0) {
            mTvTotalFound.setText(String.format(getString(R.string.total_found),
                    getString(R.string.calculating)));
            Utils.setTextFromSize(0, mTvTotalCache, mTvType);
            mViewLoading.setVisibility(View.VISIBLE);
            startImageLoading();
            getFilesFromDirApkOld();
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }


    public String getJunkValue(){
        getFilesFromDirApkOld();

        return Utils.formatSize( mTotalSizeSystemCache +
                mTotalSizeFiles +
                mTotalSizeApk +
                mTotalSizeLargeFiles +
                mTotalSizeAppData +
                mTotalAppCache +
                mTotalSizeResidualFiles);
    }


    public void cleanUp() {


        for (int i = 0; i < mGroupItems.size() + 1; i++) {
            if (i == mGroupItems.size()) {
                replaceFragment(new CleanResultFragment(), false);
                return;
            }


            GroupItem groupItem = mGroupItems.get(i);
            if (groupItem.getType() == GroupItem.TYPE_FILE || groupItem.getType() == GroupItem.TYPE_RESIDUAL_FILES
                    || groupItem.getType() == GroupItem.TYPE_APP_DATA) {
                for (ChildItem childItem : groupItem.getItems()) {
                    if (childItem.isCheck()) {
                        File file = new File(childItem.getPath());
                        if(file.isDirectory()){
                            totalOnClearLength += getFileSize(file);
                        }else{
                            totalOnClearLength += file.length();
                        }
                        file.delete();
                        if (file.exists()) {
                            try {
                                file.getCanonicalFile().delete();
                                if (file.exists()) {
                                    getActivity().deleteFile(file.getName());
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "error delete file " + e);
                            }
                        }
                    }
                }
            } else {
                if (groupItem.isCheck()) {
                    totalOnClearLength += groupItem.getTotal();
                    new TaskClean(getActivity(), new TaskClean.OnTaskCleanListener() {
                        @Override
                        public void onCleanCompleted(boolean result) {
                            Log.i(TAG, "===--onCleanCompleted-->" + result);
                        }
                    }).execute();
                }
            }
        }
    }
    public static long getTotalOnClearLength(){
        return totalOnClearLength;
    }

    private void startImageLoading() {
        mRotateloadingApks.start();
        mRotateloadingCache.start();
        mRotateloadingDownloadFiles.start();
        mRotateloadingLargeFiles.start();
        mRotateloadingAppData.start();
        mRotateloadingAppCache.start();
    }
    private boolean getPerms(){
        ActivityCompat.requestPermissions(getActivity(),
                new String[] {Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CLEAR_APP_CACHE,
                        Manifest.permission.DELETE_CACHE_FILES,
                        Manifest.permission.SYSTEM_ALERT_WINDOW},
                CleanFragment.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        int permissionReadStatus = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionWriteStatus = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permissionReadStatus == PackageManager.PERMISSION_GRANTED && permissionWriteStatus == PackageManager.PERMISSION_GRANTED){
            return true;
        }else {
            return false;
        }
    }
    private void initAdapter() {

        mAdapter = new CleanAdapter(getActivity(), mGroupItems, new CleanAdapter.OnGroupClickListener() {
            @Override
            public void onGroupClick(int groupPosition) {
                if (mRecyclerView.isGroupExpanded(groupPosition)) {
                    mRecyclerView.collapseGroupWithAnimation(groupPosition);
                } else {
                    mRecyclerView.expandGroupWithAnimation(groupPosition);
                }
            }

            @Override
            public void onSelectItemHeader(int position, boolean isCheck) {
                changeCleanFileHeader(position, isCheck);
            }

            @Override
            public void onSelectItem(int groupPosition, int childPosition, boolean isCheck) {
                changeCleanFileItem(groupPosition, childPosition, isCheck);
            }
        });
        mRecyclerView.setAdapter(mAdapter);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 123:
                replaceFragment(new CleanFragment(), false);
                break;
        }
    }
    private void changeCleanFileHeader(int position, boolean isCheck) {
        long total = mGroupItems.get(position).getTotal();
        if (isCheck) {
            mTotalSizeSystemCache = mTotalSizeSystemCache + total;
        } else {
            mTotalSizeSystemCache = mTotalSizeSystemCache - total;
        }
        Utils.setTextFromSize(mTotalSizeSystemCache, mTvTotalCache, mTvType);
        mGroupItems.get(position).setIsCheck(isCheck);
        for (ChildItem childItem : mGroupItems.get(position).getItems()) {
            childItem.setIsCheck(isCheck);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void loadApplicationData() {
        final GroupItem groupItemApp = new GroupItem();
        groupItemApp.setTitle(getString(R.string.application_data));
        groupItemApp.setType(GroupItem.TYPE_APP_DATA);
        ManagerConnect managerConnect = new ManagerConnect();
        final List<ChildItem> childItems = new ArrayList<>();
        managerConnect.getListManager(getActivity(), new ManagerConnect.OnManagerConnectListener() {
            @Override
            public void OnResultManager(final List<ApplicationInfo> result) {
                Log.i(TAG, "====---->>" + result.size());
                if (result.size() != 0) {
                    List<ApplicationInfo> applicationInfosUser = new ArrayList<>();
                    for (ApplicationInfo applicationInfo : result) {
                        if (!applicationInfo.packageName.equals(getActivity().getPackageName())
                                && Utils.isUserApp(applicationInfo)) {
                            File appData = new File("/storage/emulated/0/Android/data/"
                                    + applicationInfo.packageName + "/files");
                            if(appData.length() != 0) {
                                //long length = FileUtils.sizeOfDirectory(appData);
                                long length = getFileSize(appData);
                                //CleanFragment.mTotalSizeAppData += length;
                                if(length != 0) {
                                    ChildItem childItem = new ChildItem(applicationInfo.packageName,
                                            applicationInfo.packageName,
                                            applicationInfo.loadIcon(getActivity().getPackageManager()),
                                            length, GroupItem.TYPE_APP_DATA,
                                            appData.getPath(),
                                            false);
                                    groupItemApp.setIsCheck(false);
                                    mTotalSizeAppData += length;
                                    groupItemApp.setTotal(mTotalSizeAppData);
                                    applicationInfosUser.add(applicationInfo);
                                    childItems.add(childItem);
                                }
                            }
                        }
                    }
                    groupItemApp.setItems(childItems);
                    mGroupItems.add(groupItemApp);
                    mTvTotalFound.setText(String.format(getString(R.string.total_found),
                            Utils.formatSize(mTotalSizeSystemCache + mTotalSizeFiles + mTotalSizeApk + mTotalSizeLargeFiles
                                    + mTotalSizeAppData)));
                    loadApplicationCache();
                }
            }
        });
        mRotateloadingAppData.stop();
    }

    private void loadApplicationCache() {
        final GroupItem groupItemApp = new GroupItem();
        groupItemApp.setTitle(getString(R.string.application_cache));
        groupItemApp.setType(GroupItem.TYPE_APP_CACHE);
        ManagerConnect managerConnect = new ManagerConnect();
        final List<ChildItem> childItems = new ArrayList<>();
        managerConnect.getListManager(getActivity(), new ManagerConnect.OnManagerConnectListener() {
            @Override
            public void OnResultManager(final List<ApplicationInfo> result) {
                Log.i(TAG, "====---->>" + result.size());
                if (result.size() != 0) {
                    List<ApplicationInfo> applicationInfosUser = new ArrayList<>();
                    for (ApplicationInfo applicationInfo : result) {
                        if (!applicationInfo.packageName.equals(getActivity().getPackageName())
                                && Utils.isUserApp(applicationInfo)) {
                            File appCache = new File("/storage/emulated/0/Android/data/"
                                    + applicationInfo.packageName + "/cache");
                            if(appCache.length() != 0) {
                                //long length = FileUtils.sizeOfDirectory(appData);
                                long length = getFileSize(appCache);
                                //CleanFragment.mTotalSizeAppData += length;
                                if(length != 0) {
                                    ChildItem childItem = new ChildItem(applicationInfo.packageName,
                                            applicationInfo.packageName,
                                            applicationInfo.loadIcon(getActivity().getPackageManager()),
                                            length, GroupItem.TYPE_APP_CACHE,
                                            appCache.getPath(),
                                            false);
                                    groupItemApp.setIsCheck(false);
                                    mTotalAppCache += length;
                                    groupItemApp.setTotal(mTotalAppCache);
                                    applicationInfosUser.add(applicationInfo);
                                    childItems.add(childItem);
                                }
                            }
                        }
                    }
                    groupItemApp.setItems(childItems);
                    mGroupItems.add(groupItemApp);
                    mTvTotalFound.setText(String.format(getString(R.string.total_found),
                            Utils.formatSize(mTotalSizeSystemCache + mTotalSizeFiles + mTotalSizeApk + mTotalSizeLargeFiles
                                    + mTotalSizeAppData)));
                    loadResidualFiles();
                }
            }
        });
        //mRotateloadingAppCache.stop();
    }
    private void loadResidualFiles(){
        if (mScanResidualFiles != null
                && mScanResidualFiles.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }
        mScanResidualFiles = new ScanResidualFiles(new OnScanResidualFilesListener(){

            @Override
            public void onScanCompleted(List<File> result) {
                if (result != null && result.size() > 0) {
                    GroupItem groupItem = new GroupItem();
                    groupItem.setTitle(getString(R.string.residual_files));
                    groupItem.setIsCheck(false);
                    groupItem.setType(GroupItem.TYPE_RESIDUAL_FILES);
                    List<ChildItem> childItems = new ArrayList<>();
                    for (File currentFile : result) {
                        ChildItem childItem = new ChildItem(currentFile.getName(),
                                "Пустая Папка", ContextCompat.getDrawable(getActivity(),
                                R.drawable.ic_android_white_24dp),
                                currentFile.length(), GroupItem.TYPE_RESIDUAL_FILES,
                                currentFile.getPath(), false);
                        childItems.add(childItem);
                        mTotalSizeResidualFiles += currentFile.length();
                        groupItem.setTotal(mTotalSizeResidualFiles);
                        groupItem.setItems(childItems);
                    }
                    mGroupItems.add(groupItem);
                }
                updateAdapter();
            }
        });

        mScanResidualFiles.execute();
    }

    private void changeCleanFileItem(int groupPosition, int childPosition, boolean isCheck) {
        long total = mGroupItems.get(groupPosition).getItems().get(childPosition).getCacheSize();
        if (isCheck) {
            mTotalSizeSystemCache = mTotalSizeSystemCache + total;
        } else {
            mTotalSizeSystemCache = mTotalSizeSystemCache - total;
        }
        Utils.setTextFromSize(mTotalSizeSystemCache, mTvTotalCache, mTvType);
        mGroupItems.get(groupPosition).getItems().get(childPosition).setIsCheck(isCheck);
        boolean isCheckItem = false;
        for (ChildItem childItem : mGroupItems.get(groupPosition).getItems()) {
            isCheckItem = childItem.isCheck();
            if (!isCheckItem) {
                break;
            }
        }
        if (isCheckItem) {
            mGroupItems.get(groupPosition).setIsCheck(true);
        } else {
            mGroupItems.get(groupPosition).setIsCheck(false);
        }
        mAdapter.notifyDataSetChanged();
    }

    public void getFilesFromDirApkOld() {
        if (mScanApkFiles != null
                && mScanApkFiles.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }
        mScanApkFiles = new ScanApkFiles(new OnScanApkFilesListener() {
            @Override
            public void onScanCompleted(List<File> result) {
                if (mIsFragmentPause) {
                    return;
                }
                if (result != null && result.size() > 0) {
                    GroupItem groupItem = new GroupItem();
                    groupItem.setTitle(getString(R.string.obsolete_apk));
                    groupItem.setIsCheck(false);
                    groupItem.setType(ChildItem.TYPE_APKS);
                    List<ChildItem> childItems = new ArrayList<>();
                    for (File currentFile : result) {
                        ChildItem childItem = new ChildItem(currentFile.getName(),
                                currentFile.getName(), ContextCompat.getDrawable(getActivity(),
                                R.drawable.ic_android_white_24dp),
                                currentFile.length(), ChildItem.TYPE_APKS,
                                currentFile.getPath(), false);
                        childItems.add(childItem);
                        mTotalSizeApk += currentFile.length();
                        groupItem.setTotal(mTotalSizeApk);
                        groupItem.setItems(childItems);
                    }
                    mGroupItems.add(groupItem);
                    mTvTotalFound.setText(String.format(getString(R.string.total_found),
                            Utils.formatSize(mTotalSizeApk)));
                }
                if(mRotateloadingApks != null) {
                    mRotateloadingApks.stop();
                }
                getCacheFile();
            }
        });
        mScanApkFiles.execute();
    }


    private void getCacheFile() {
        if (mTaskScan != null
                && mTaskScan.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }
        mTaskScan = new TaskScan(new OnActionListener() {
            @Override
            public void onScanCompleted(long totalSize, List<ChildItem> result) {
                if (mIsFragmentPause) {
                    return;
                }
                mTotalSizeSystemCache = totalSize;
                Utils.setTextFromSize(totalSize, mTvTotalCache, mTvType);
                if (result.size() != 0) {
                    GroupItem groupItem = new GroupItem();
                    groupItem.setTitle(getString(R.string.system_cache));
                    groupItem.setTotal(mTotalSizeSystemCache);
                    groupItem.setIsCheck(true);
                    groupItem.setType(GroupItem.TYPE_CACHE);
                    groupItem.setItems(result);
                    mGroupItems.add(groupItem);
                    mTvTotalFound.setText(String.format(getString(R.string.total_found),
                            Utils.formatSize(mTotalSizeApk + mTotalSizeSystemCache)));
                    Utils.setTextFromSize(mTotalSizeSystemCache, mTvTotalCache, mTvType);
                }
                mRotateloadingCache.stop();
                getFilesFromDirFileDownload();
            }
        });
        mTaskScan.execute();
    }

    public void getFilesFromDirFileDownload() {
        if (mScanDownLoadFiles != null
                && mScanDownLoadFiles.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }
        mScanDownLoadFiles = new ScanDownLoadFiles(new OnScanDownloadFilesListener() {
            @Override
            public void onScanCompleted(File[] result) {
                if (mIsFragmentPause) {
                    return;
                }
                if (result != null && result.length > 0) {
                    GroupItem groupItem = new GroupItem();
                    groupItem.setTitle(getString(R.string.downloader_files));
                    groupItem.setIsCheck(false);
                    groupItem.setType(GroupItem.TYPE_FILE);
                    List<ChildItem> childItems = new ArrayList<>();
                    for (File currentFile : result) {
                        mTotalSizeFiles += currentFile.length();
                        ChildItem childItem = new ChildItem(currentFile.getName(),
                                currentFile.getName(), ContextCompat.getDrawable(getActivity(),
                                R.drawable.ic_android_white_24dp),
                                currentFile.length(), ChildItem.TYPE_DOWNLOAD_FILE,
                                currentFile.getPath(), false);
                        childItems.add(childItem);
                        groupItem.setTotal(mTotalSizeFiles);
                        groupItem.setItems(childItems);
                    }
                    mGroupItems.add(groupItem);
                }
                mRotateloadingDownloadFiles.stop();
                getLargeFile();
            }
        });
        mScanDownLoadFiles.execute();
    }

    private void getLargeFile() {
        if (mScanLargeFiles != null
                && mScanLargeFiles.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }
        mScanLargeFiles = new ScanLargeFiles(new OnScanLargeFilesListener() {
            @Override
            public void onScanCompleted(List<File> result) {
                if (mIsFragmentPause) {
                    return;
                }
                if (result.size() != 0) {
                    GroupItem groupItem = new GroupItem();
                    groupItem.setTitle(getString(R.string.large_files));
                    groupItem.setTotal(mTotalSizeLargeFiles);
                    groupItem.setIsCheck(false);
                    groupItem.setType(GroupItem.TYPE_FILE);
                    List<ChildItem> childItems = new ArrayList<>();
                    for (File currentFile : result) {
                        ChildItem childItem = new ChildItem(currentFile.getName(),
                                currentFile.getName(), ContextCompat.getDrawable(getActivity(),
                                R.drawable.ic_android_white_24dp),
                                currentFile.length(), ChildItem.TYPE_LARGE_FILES,
                                currentFile.getPath(), false);
                        childItems.add(childItem);
                        groupItem.setItems(childItems);
                    }
                    mGroupItems.add(groupItem);
                }
                mRotateloadingLargeFiles.stop();
                loadApplicationData();
            }
        });
        mScanLargeFiles.execute();
    }

    private void updateAdapter() {
        if (mGroupItems.size() != 0) {
            //for (int i = 0; i < mGroupItems.size(); i++) {
            //    if (mRecyclerView.isGroupExpanded(i)) {
            //        mRecyclerView.collapseGroupWithAnimation(i);
            //    } else {
            //        mRecyclerView.expandGroupWithAnimation(i);
            //    }
            //}
            mRecyclerView.setVisibility(View.VISIBLE);
            mTvNoJunk.setVisibility(View.GONE);
            mBtnCleanUp.setVisibility(View.VISIBLE);
            mAdapter.notifyDataSetChanged();
        } else {
            mRecyclerView.setVisibility(View.GONE);
            mBtnCleanUp.setVisibility(View.GONE);
            mTvNoJunk.setVisibility(View.VISIBLE);
        }
        mViewLoading.setVisibility(View.GONE);
        mTvTotalFound.setText(String.format(getString(R.string.total_found),
                Utils.formatSize(mTotalSizeSystemCache + mTotalSizeFiles + mTotalSizeApk + mTotalSizeLargeFiles +
                        mTotalAppCache + mTotalSizeAppData + mTotalSizeResidualFiles)));
    }

    public ArrayList<File> getfile(File dir) {
        File listFile[] = dir.listFiles();
        if (listFile != null && listFile.length > 0) {
            for (File aListFile : listFile) {
                if (aListFile.isDirectory() && !aListFile.getName().equals(Environment.DIRECTORY_DOWNLOADS)) {
                    getfile(aListFile);
                } else {
                    long fileSizeInBytes = aListFile.length();
                    long fileSizeInKB = fileSizeInBytes / 1024;
                    long fileSizeInMB = fileSizeInKB / 1024;
                    if (fileSizeInMB >= 10 && !aListFile.getName().endsWith(".apk")) {
                        mTotalSizeLargeFiles += aListFile.length();
                        mFileListLarge.add(aListFile);
                    }
                }
            }
        }
        return mFileListLarge;
    }

    @Override
    public void onPause() {
        super.onPause();
        mIsFragmentPause = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsFragmentPause = false;
        setHeader(getString(R.string.app_name), MainActivity.HeaderBarType.TYPE_HOME);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mGroupItems.clear();
        if (mScanApkFiles != null
                && mScanApkFiles.getStatus() == AsyncTask.Status.RUNNING) {
            mScanApkFiles.cancel(true);
        }
        if (mTaskScan != null
                && mTaskScan.getStatus() == AsyncTask.Status.RUNNING) {
            mTaskScan.cancel(true);
        }
        if (mScanDownLoadFiles != null
                && mScanDownLoadFiles.getStatus() == AsyncTask.Status.RUNNING) {
            mScanDownLoadFiles.cancel(true);
        }
        if (mScanLargeFiles != null
                && mScanLargeFiles.getStatus() == AsyncTask.Status.RUNNING) {
            mScanLargeFiles.cancel(true);
        }

        if (mScanResidualFiles != null
                && mScanResidualFiles.getStatus() == AsyncTask.Status.RUNNING) {
            mScanResidualFiles.cancel(true);
        }
    }

    private class TaskScan extends AsyncTask<Void, Integer, List<ChildItem>> {

        private Method mGetPackageSizeInfoMethod;
        private OnActionListener mOnActionListener;
        private long mTotalSize;

        public TaskScan(OnActionListener onActionListener) {
            mOnActionListener = onActionListener;
            try {
                mGetPackageSizeInfoMethod = getActivity().getPackageManager().getClass().getMethod(
                        "getPackageSizeInfo", String.class, IPackageStatsObserver.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }catch(NullPointerException e){
                e.printStackTrace();
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected List<ChildItem> doInBackground(Void... params) {
            final List<ApplicationInfo> packages = getActivity().getPackageManager().getInstalledApplications(
                    PackageManager.GET_META_DATA);
            final CountDownLatch countDownLatch = new CountDownLatch(packages.size());
            final List<ChildItem> apps = new ArrayList<>();
            try {
                for (ApplicationInfo pkg : packages) {
                    mGetPackageSizeInfoMethod.invoke(getActivity().getPackageManager(), pkg.packageName,
                            new IPackageStatsObserver.Stub() {

                                @Override
                                public void onGetStatsCompleted(PackageStats pStats,
                                                                boolean succeeded)
                                        throws RemoteException {
                                    synchronized (apps) {
                                        addPackage(apps, pStats);
                                    }
                                    synchronized (countDownLatch) {
                                        countDownLatch.countDown();
                                    }
                                }
                            }
                    );
                }

                countDownLatch.await();
            } catch (InvocationTargetException | InterruptedException | IllegalAccessException e) {
                e.printStackTrace();
            }

            return new ArrayList<>(apps);
        }

        @Override
        protected void onPostExecute(List<ChildItem> result) {
            if (mOnActionListener != null) {
                mOnActionListener.onScanCompleted(mTotalSize, result);
            }
        }

        private void addPackage(List<ChildItem> apps, PackageStats pStats) {
            long cacheSize = pStats.cacheSize;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                cacheSize += pStats.externalCacheSize;
            }
            try {
                PackageManager packageManager = getActivity().getPackageManager();
                ApplicationInfo info = packageManager.getApplicationInfo(pStats.packageName,
                        PackageManager.GET_META_DATA);
                if (cacheSize > 1024 * 12) {
                    mTotalSize += cacheSize;
                    apps.add(new ChildItem(pStats.packageName,
                            packageManager.getApplicationLabel(info).toString(),
                            packageManager.getApplicationIcon(pStats.packageName),
                            cacheSize, ChildItem.TYPE_CACHE, null, true));
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public interface OnActionListener {
        void onScanCompleted(long totalSize, List<ChildItem> result);
    }

    private class ScanLargeFiles extends AsyncTask<Void, Integer, List<File>> {

        private OnScanLargeFilesListener mOnScanLargeFilesListener;

        public ScanLargeFiles(OnScanLargeFilesListener onActionListener) {
            mOnScanLargeFilesListener = onActionListener;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected List<File> doInBackground(Void... params) {
            File root = new File(Environment.getExternalStorageDirectory()
                    .getAbsolutePath());
            return getfile(root);
        }

        @Override
        protected void onPostExecute(List<File> result) {
            if (mOnScanLargeFilesListener != null) {
                mOnScanLargeFilesListener.onScanCompleted(result);
            }
        }
    }


    public interface OnScanLargeFilesListener {
        void onScanCompleted(List<File> result);
    }

    private class ScanApkFiles extends AsyncTask<Void, Integer, List<File>> {

        private OnScanApkFilesListener mOnScanLargeFilesListener;

        public ScanApkFiles(OnScanApkFilesListener onActionListener) {
            mOnScanLargeFilesListener = onActionListener;
        }

        @Override
        protected void onPreExecute() {
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected List<File> doInBackground(Void... params) {
            List<File> filesResult = new ArrayList<>();
            File downloadDir = new File(String.valueOf(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS)));
            File storageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
            File docDir = new File(String.valueOf(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS)));
            for(File downFile : downloadDir.listFiles()){
                if (downFile.getName().endsWith(".apk")) {
                    filesResult.add(downFile);
                }
            }
            for(File storeFile : storageDir.listFiles()){
                if (storeFile.getName().endsWith(".apk")) {
                    filesResult.add(storeFile);
                }
            }
            //List<File> files = Arrays.asList(Environment.getExternalStoragePublicDirectory(
            //       Environment.DIRECTORY_DOWNLOADS).listFiles());


            return filesResult;
        }

        @Override
        protected void onPostExecute(List<File> result) {
            if (mOnScanLargeFilesListener != null) {
                mOnScanLargeFilesListener.onScanCompleted(result);
            }
        }
    }

    public interface OnScanApkFilesListener {
        void onScanCompleted(List<File> result);
    }

    private class ScanDownLoadFiles extends AsyncTask<Void, Integer, File[]> {

        private OnScanDownloadFilesListener mOnScanLargeFilesListener;

        public ScanDownLoadFiles(OnScanDownloadFilesListener onActionListener) {
            mOnScanLargeFilesListener = onActionListener;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected File[] doInBackground(Void... params) {
            File downloadDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
            return downloadDir.listFiles();
        }

        @Override
        protected void onPostExecute(File[] result) {
            if (mOnScanLargeFilesListener != null) {
                mOnScanLargeFilesListener.onScanCompleted(result);
            }
        }
    }

    public interface OnScanDownloadFilesListener {
        void onScanCompleted(File[] result);
    }


    public class ScanResidualFiles extends AsyncTask<Void, Integer, List<File>>{
        private OnScanResidualFilesListener mOnScanResidualFilesListener;

        public ScanResidualFiles(OnScanResidualFilesListener onActionListener){
            mOnScanResidualFilesListener = onActionListener;
        }

        @Override
        protected List<File> doInBackground(Void... voids) {

            List<File> filesResult = new ArrayList<>();
            File datadir = new File("/storage/emulated/0/Android/data/");
            File files[] = datadir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if(file.isDirectory()) {
                        if (getFileSize(file) == 0) {
                            filesResult.add(file);
                        }
                    }
                }
            }
            return filesResult;
        }
        @Override
        protected void onPostExecute(List<File> result) {
            if (mOnScanResidualFilesListener != null) {
                mOnScanResidualFilesListener.onScanCompleted(result);
            }
        }
    }
    private interface OnScanResidualFilesListener{
        void onScanCompleted(List<File> result);
    }
    public static long getFileSize(final File file) {
        if (file == null || !file.exists())
            return 0;
        if (!file.isDirectory())
            return file.length();
        final List<File> dirs = new LinkedList<>();
        dirs.add(file);
        long result = 0;
        while (!dirs.isEmpty()) {
            final File dir = dirs.remove(0);
            if (!dir.exists())
                continue;
            final File[] listFiles = dir.listFiles();
            if (listFiles == null || listFiles.length == 0)
                continue;
            for (final File child : listFiles) {
                result += child.length();
                if (child.isDirectory())
                    dirs.add(child);
            }
        }
        return result;
    }
}

