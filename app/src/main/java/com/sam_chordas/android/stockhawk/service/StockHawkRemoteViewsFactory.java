/**
 * Copyright (C) August 2016
 * The Stock Hawk project
 */

package com.sam_chordas.android.stockhawk.service;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

/**
 * Works as a adapter class for collection widget.
 *
 * @author Sagar Rathod
 * @version 1.0
 */

public class StockHawkRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private Cursor mCursor;

    /**
     * Constructs an instance of StockHawkRemoteViewsFactory.
     */
    public StockHawkRemoteViewsFactory(Context context){
        this.mContext = context;
    }

    /**
     * Initializes the cursor
     */
    @Override
    public void onCreate() {

        mCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);

    }

    /**
     * Applies the data set changes.
     */
    @Override
    public void onDataSetChanged() {
        onCreate();
    }

    /**
     * Closes the cursor.
     */
    @Override
    public void onDestroy() {
        if(mCursor != null)
            mCursor.close();
    }

    /**
     * Returns the total number of rows in cursor.
     * @return
     */
    @Override
    public int getCount() {
        if(mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    /**
     * Returns the remote view for cursor view to collection widget.
     *
     * @param index
     * @return
     */
    @Override
    public RemoteViews getViewAt(int index) {

        mCursor.moveToPosition(index);

        int symbolIndex = mCursor.getColumnIndex(QuoteColumns.SYMBOL);
        int bidPriceIndex = mCursor.getColumnIndex(QuoteColumns.BIDPRICE);
        int changeIndex = mCursor.getColumnIndex(QuoteColumns.CHANGE);
        int percentChangeIndex = mCursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE);
        int isUpIndex = mCursor.getColumnIndex(QuoteColumns.ISUP);

        String symbol = mCursor.getString(symbolIndex);
        String bidPrice = mCursor.getString(bidPriceIndex);
        String change = mCursor.getString(changeIndex);
        String percentChange = mCursor.getString(percentChangeIndex);
        int isUp = mCursor.getInt(isUpIndex);

        RemoteViews views = new RemoteViews(mContext.getPackageName(),
                R.layout.list_item_quote);

        views.setTextViewText(R.id.stock_symbol, symbol);
        views.setTextViewText(R.id.bid_price, bidPrice);

        if (Utils.showPercent) {
            views.setTextViewText(R.id.change,percentChange);
        } else {
            views.setTextViewText(R.id.change,change);
        }

        Intent fillInIntent  = new Intent();
        fillInIntent.putExtra("symbol",symbol);

        views.setOnClickFillInIntent(R.id.linear_layout, fillInIntent);

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    /**
     * Returns the number of view types
     * @return
     */
    @Override
    public int getViewTypeCount() {
        return 1;
    }

    /**
     * Returns the _id of associated with row(i) in cursor.
     * @param i
     * @return
     */
    @Override
    public long getItemId(int i) {
        if(mCursor != null){
            return mCursor.getLong( mCursor.getColumnIndex(QuoteColumns._ID));
        }
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}
