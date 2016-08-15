/**
 * Copyright (C) August 2016
 * The Stock Hawk project
 */
package com.sam_chordas.android.stockhawk.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;

/**
 * Listens for network changes in the device.
 *
 * @author Sagar Rathod
 * @version 1.0
 */

public class NetworkStateReceiver extends BroadcastReceiver {

    /**
     * Notifies the stock hawk application for network availability.
     *
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);

        SharedPreferences.Editor editor = sharedPreferences.edit();

        if(Utils.isNetworkAvailable(context)){
            editor.putString(context.getString(R.string.network_state),
                    Utils.NETWORK_STATE_CONNECTED);
        }else{
            editor.putString(context.getString(R.string.network_state),
                    Utils.NETWORK_STATE_DISCONNECTED);
        }

        editor.commit();

    }
}
