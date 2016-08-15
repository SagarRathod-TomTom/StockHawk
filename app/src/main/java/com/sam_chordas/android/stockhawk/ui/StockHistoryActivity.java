/**
 * Copyright (C) August 2016
 * The Stock Hawk project
 */
package com.sam_chordas.android.stockhawk.ui;


import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.beans.StockHistory;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Shows stock history over a time for stock hawk application.
 *
 * @author Sagar Rathod
 * @version 1.0
 */

public class StockHistoryActivity extends AppCompatActivity {

    private OkHttpClient mOkHttpClient = new OkHttpClient();
    private String mBaseUrl = "https://query.yahooapis.com/v1/public/yql?q=";
    private String mFormat = "&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=";
    private String mSymbol;
    private String mStartDate = Utils.getTenDaysBeforePreviousDate();
    private LineChart mLowLineChart;
    private LineChart mHighLineChart;
    private String mEndDate = Utils.getPreviousDayDate();
    private ContentResolver mContentResolver;

    /**
     * Initializes a chart view.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_history);
        mLowLineChart = (LineChart) findViewById(R.id.low_line_chart);
        mHighLineChart = (LineChart) findViewById(R.id.high_line_chart);

        mContentResolver = getContentResolver();

        Intent intent = getIntent();
        if (intent != null) {
            mSymbol = intent.getStringExtra(getString(R.string.symbol_extra));
            new StockHistoryAsyncTask().execute();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.stock_history) + ": " + mSymbol);
        }

    }

    /**
     * Builds a sql query for YAHOO Apis to fetch historical data.
     * @return
     */
    private String buildQuery() {
        StringBuilder queryBuilder = new StringBuilder();
        String query = "select * from yahoo.finance.historicaldata where";
        queryBuilder.append(query);
        queryBuilder.append(" symbol=\"" + mSymbol + "\"");
        queryBuilder.append(" and startDate=\"" + mStartDate + "\"");
        queryBuilder.append(" and endDate=\"" + mEndDate + "\"");
        return queryBuilder.toString();
    }

    /**
     * Loads the stock history in background thread.
     *
     */
    class StockHistoryAsyncTask extends AsyncTask<Void, Void, List<StockHistory>> {

        ProgressDialog progressDialog;

        /**
         * Displays a progress dialog
         */
        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog
                    .show(StockHistoryActivity.this, getString(R.string.please_wait),
                            getString(R.string.loading));
        }

        /**
         * Checks whether stock history already exist in the database.
         * @return
         */
        private List<StockHistory> getStockHistoryIfExist() {
            Cursor cursor = null;
            String projection[] = null, selection = null, selectionArgs[] = null, sortOrder = null;

            cursor = mContentResolver.query(
                    QuoteProvider.History.withSymbol(mSymbol),
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder);

            return buildFromCursor(cursor);
        }

        /**
         * Prepares a list of stock history object from cursor.
         * @param cursor
         * @return
         */

        List<StockHistory> buildFromCursor(Cursor cursor) {

            List<StockHistory> stockHistoryList = null;

            if (cursor != null && cursor.getCount()!= 0) {
                Log.d("Cursor","count:"+cursor.getColumnCount());
                stockHistoryList = new ArrayList<StockHistory>();
                while (cursor.moveToNext()) {
                    StockHistory stockHistory = new StockHistory();
                    stockHistory.setDate(cursor.getString(2));
                    stockHistory.setLow(cursor.getFloat(3));
                    stockHistory.setHigh(cursor.getFloat(4));
                    stockHistoryList.add(stockHistory);
                }
            }

            return stockHistoryList;
        }

        /**
         * Fetches the stock historical data from yahoo apis.
         * @param params
         * @return List of stock history. Returns null in case of empty response.
         */
        @Override
        protected List<StockHistory> doInBackground(Void... params) {

            List<StockHistory> stockHistoryList = getStockHistoryIfExist();

            if(stockHistoryList != null) {
                Log.d("Stock History","Stock History does exit in the database.");
                return stockHistoryList;
            }

            Log.d("Stock History","Stock History does not exit in the database.");

            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(mBaseUrl);
            try {
                urlBuilder.append(URLEncoder.encode(buildQuery(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                return null;
            }

            urlBuilder.append(mFormat);

            Request request = new Request.Builder()
                    .url(urlBuilder.toString())
                    .build();
            Response response = null;
            try {
                response = mOkHttpClient.newCall(request).execute();
                String data = response.body().string();
                stockHistoryList = Utils.quoteToStockHistory(data);

                Utils.bulkInsert(stockHistoryList, mSymbol, mContentResolver);

                return stockHistoryList;

            } catch (IOException e) {
                return null;
            } catch (JSONException e) {
                return null;
            }
        }

        /**
         * Displays the line chart.
         *
         * @param stockHistoryList
         */
        @Override
        protected void onPostExecute(List<StockHistory> stockHistoryList) {

            if (stockHistoryList != null) {

                ArrayList<Entry> lowEntries = new ArrayList<Entry>();
                ArrayList<Entry> highEntries = new ArrayList<Entry>();
                ArrayList<String> dateLabels = new ArrayList<String>();

                LineDataSet lowLineSet, highLineSet;

                int value;
                int lowMax = 0, highMax = 0;
                String dateLabel = null;
                int index = 0;
                for (StockHistory stockHistory : stockHistoryList) {
                    value = (int) stockHistory.getLow();
                    if (value > lowMax)
                        lowMax = value;

                    value = (int) stockHistory.getHigh();
                    if (value > highMax)
                        highMax = value;

                    try {
                        dateLabel = Utils.formatLabel(stockHistory.getDate());
                        lowEntries.add(new Entry(stockHistory.getLow(), index));
                        highEntries.add(new Entry(stockHistory.getHigh(), index++));
                        dateLabels.add(dateLabel);
                    } catch (ParseException e) {
                    }
                }

                lowLineSet = new LineDataSet(lowEntries, "Stock Bid Price");
                highLineSet = new LineDataSet(highEntries, "Stock Bid Price");

                lowLineSet.setDrawFilled(true);
                highLineSet.setDrawFilled(true);

                lowLineSet.setColors(ColorTemplate.COLORFUL_COLORS);
                highLineSet.setColors(ColorTemplate.COLORFUL_COLORS);

                LineData lowLineData = new LineData(dateLabels, lowLineSet);
                LineData highLineData = new LineData(dateLabels, highLineSet);
                mLowLineChart.setDescription(getString(R.string.stock_low_tag_line));
                mHighLineChart.setDescription(getString(R.string.stock_high_tag_line));

                updateLineChartView(mLowLineChart, lowMax, lowLineData);
                updateLineChartView(mHighLineChart, highMax, highLineData);

            }
            progressDialog.dismiss();
        }

        /**
         * Updates the line chart view.
         *
         * @param lineChart
         * @param max
         * @param lineData
         */
        private void updateLineChartView(LineChart lineChart, int max, LineData lineData) {
            lineChart.setData(lineData);
            lineChart.notifyDataSetChanged();
            lineChart.invalidate();

        }

    }

}
