package com.kikimore.ecleaner.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.kikimore.ecleaner.model.BatterySaver;
import com.kikimore.ecleaner.utils.PreferenceUtil;
import com.kikimore.ecleaner.utils.Utils;

import java.util.List;

public class BatteryPlanStopReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        PreferenceUtil preferenceUtil = new PreferenceUtil();
        boolean isEnbale = PreferenceUtil.getBoolean(context, PreferenceUtil.BATTERY_PLAN, false);
        if (!isEnbale) {
            return;
        }
        int position = PreferenceUtil.getInt(context, PreferenceUtil.PEDIOD_OUTSIDE_INDEX);
        List<BatterySaver> batterySavers = preferenceUtil.getListBatterySaver(context);
        Utils.setBatterySaverSelected(context, batterySavers.get(position));
    }
}
