package com.kikimore.ecleaner;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.ads.control.AdmobHelp;
import com.kikimore.ecleaner.fragments.BaseFragment;
import com.kikimore.ecleaner.fragments.BatterySavingPlanFragment;
import com.kikimore.ecleaner.fragments.BoostResultFragment;
import com.kikimore.ecleaner.fragments.CleanResultFragment;
import com.kikimore.ecleaner.fragments.HomeFragment;
import com.kikimore.ecleaner.fragments.SettingFragment;
import com.kikimore.ecleaner.utils.KeyboardUtil;
import com.kikimore.ecleaner.utils.Utils;

public class MainActivity extends AppCompatActivity implements BaseFragment.OnBaseFragmentListener {

    /**
     * HeaderBarType define types of header
     */

    public static final int WRITE_PERMISSION_REQUEST = 5000;
    public enum HeaderBarType {
        TYPE_HOME, TYPE_CLEAN, TYPE_CLEAN_UP, TYPE_BATTERY_PLAN
    }

    private Menu mMenu;

    private Fragment mCurrentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        writePermission();
        if (null != getSupportActionBar()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.logo);
            getSupportActionBar().setElevation(0);
        }
        AdmobHelp.getInstance().loadBanner(this);

    }
    public void writePermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(getApplicationContext())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, WRITE_PERMISSION_REQUEST);
                startActivity(new Intent(MainActivity.this,PermissionSettingActivity.class));

            }
        }
    }
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        // Check which request we're responding to
//        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
//            fragment.onActivityResult(requestCode, resultCode, data);
//        }
//        if(requestCode==WRITE_PERMISSION_REQUEST){
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                if (Settings.System.canWrite(MainActivity.this)){
//
//
//                }
//            }
//
//        }
//
//
//
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMenu = menu;
        replaceFragment(new HomeFragment(), false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            View view = getCurrentFocus();
            if (view != null) {
                KeyboardUtil.hideSoftKeyboard(MainActivity.this);
            }
            if (!(mCurrentFragment instanceof HomeFragment)) {
                onBackPressed();
            }
        } else if (id == R.id.action_settings) {
            replaceFragment(new SettingFragment(), false);
        } else if (id == R.id.action_rate) {
            Utils.rateApp(MainActivity.this);
        } 
//        else if (id == R.id.action_remove_ads) {
//            Utils.removeAds(MainActivity.this);
//        } 
        else if (id == R.id.action_alarm) {
            replaceFragment(new BatterySavingPlanFragment(), false);
        }

        return super.onOptionsItemSelected(item);
    }

    public void replaceFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        if (addToBackStack) {
            transaction.setCustomAnimations(R.anim.open_main, R.anim.close_next);
        } else {
            transaction.setCustomAnimations(R.anim.open_next, R.anim.close_main,
                    R.anim.open_main, R.anim.close_next);
        }
        if (addToBackStack) {
            transaction.addToBackStack(null);
        } else {
            transaction.addToBackStack(fragment.toString());
        }
        if (fragment.getTag() == null) {
            transaction.replace(R.id.contentFrame, fragment, fragment.toString());
        } else {
            transaction.replace(R.id.contentFrame, fragment, fragment.getTag());
        }
        transaction.commit();
        mCurrentFragment = fragment;
    }

    @Override
    public void onBackPressed() {
        if (mCurrentFragment instanceof CleanResultFragment
                || mCurrentFragment instanceof BoostResultFragment) {
            return;
        }
        onBack();
    }

    public void onBack() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() != 1) {
            FragmentManager.BackStackEntry backStackEntry = fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 2);
            mCurrentFragment = getSupportFragmentManager().findFragmentByTag(backStackEntry.getName());
            super.onBackPressed();
        } else {
            finish();
        }
    }

    @Override
    public void setTitleHeader(String title) {
        if (null != getSupportActionBar()) {
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public void setTypeHeader(HeaderBarType type) {
        switch (type) {
            case TYPE_CLEAN:
                if (null != getSupportActionBar() && null != mMenu) {
                    getSupportActionBar().show();
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
                   // mMenu.findItem(R.id.action_remove_ads).setVisible(false);
                    mMenu.findItem(R.id.action_rate).setVisible(false);
                    mMenu.findItem(R.id.action_settings).setVisible(false);
                    mMenu.findItem(R.id.action_alarm).setVisible(false);
                }
                break;
            case TYPE_CLEAN_UP:
                if (null != getSupportActionBar() && null != mMenu) {
                    getSupportActionBar().hide();
                }
                break;
            case TYPE_BATTERY_PLAN:
                if (null != getSupportActionBar() && null != mMenu) {
                    getSupportActionBar().show();
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
                  //  mMenu.findItem(R.id.action_remove_ads).setVisible(false);
                    mMenu.findItem(R.id.action_rate).setVisible(false);
                    mMenu.findItem(R.id.action_settings).setVisible(false);
                    mMenu.findItem(R.id.action_alarm).setVisible(true);
                }
                break;
            default:
                if (null != getSupportActionBar() && null != mMenu) {
                    getSupportActionBar().show();
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setHomeAsUpIndicator(R.drawable.logo);
                 //   mMenu.findItem(R.id.action_remove_ads).setVisible(true);
                    mMenu.findItem(R.id.action_rate).setVisible(true);
                    mMenu.findItem(R.id.action_settings).setVisible(true);
                    mMenu.findItem(R.id.action_alarm).setVisible(false);
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case HomeFragment.WRITE_EXTERNAL_STORAGE_REQUEST: {
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
            }
        }
    }

}
