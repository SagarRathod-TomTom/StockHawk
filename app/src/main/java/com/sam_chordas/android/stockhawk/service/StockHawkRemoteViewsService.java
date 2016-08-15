/**
 * Copyright (C) August 2016
 * The Stock Hawk project
 */

package com.sam_chordas.android.stockhawk.service;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Remote view service for {@link StockHawkRemoteViewsFactory}
 *
 * @author Sagar Rathod
 * @version 1.0
 */

public class StockHawkRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StockHawkRemoteViewsFactory(getApplicationContext());
    }
}
