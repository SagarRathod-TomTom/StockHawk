/**
 * Copyright (C) August 2016
 * The Stock Hawk project
 */

package com.sam_chordas.android.stockhawk.app_widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockHawkRemoteViewsService;

import com.sam_chordas.android.stockhawk.ui.StockHistoryActivity;

/**
 * Handles updates to Stock Hawk app widget.
 *
 * @author Sagar Rathod
 * @version 1.0
 */

public class StockHawkAppWidget extends AppWidgetProvider {

    /**
     * Update callback for app widget.
     *
     * @param context          The application context
     * @param appWidgetManager The application widget manager
     * @param appWidgetIds     An array of app widget Ids
     */

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int i = 0; i < appWidgetIds.length; i++) {

            Intent intent = new Intent(context, StockHawkRemoteViewsService.class);

            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.stock_widget_layout);

            views.setRemoteAdapter(R.id.widget_list_view, intent);
            views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_text_view);

            Intent templateIntent = new Intent(context, StockHistoryActivity.class);
            templateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    templateIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            views.setPendingIntentTemplate(R.id.widget_list_view, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetIds[i], views);

        }
    }

    /**
     * Updates the app widgets when an stock hawk is updated with latest data.
     *
     * @param context The application context
     * @param intent  The intent.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (intent.getAction().equals(Utils.STOCK_APPWIDGET_UPDATE)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context, this.getClass());

            int appWidgetIds[] = appWidgetManager.getAppWidgetIds(componentName);

            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view);

        }


    }
}
