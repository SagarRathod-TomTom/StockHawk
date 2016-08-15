/**
 * Copyright (C) August 2016
 * The Stock Hawk project
 */

package com.sam_chordas.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.preference.PreferenceManager;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.beans.StockHistory;
import com.sam_chordas.android.stockhawk.data.HistoryColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.exceptions.CountZeroException;
import com.sam_chordas.android.stockhawk.exceptions.NonExistentSymbolException;
import com.sam_chordas.android.stockhawk.rest.Utils;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService {
    private final String INIT_TAG = "init";
    private final String PERIODIC_TAG = "periodic";
    private final String HISTORY_TAG = "history";
    private final String BASE_URL = "https://query.yahooapis.com/v1/public/yql?q=";
    private final String FORMAT = "&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
            + "org%2Falltableswithkeys&callback=";
    private String LOG_TAG = StockTaskService.class.getSimpleName();
    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;
    private SharedPreferences mSharedPreferences;

    public StockTaskService() {
    }

    public StockTaskService(Context context) {
        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    /**
     * Fetches the response from the specified url.
     * @param url The url from which to get response.
     * @return
     * @throws IOException
     */
    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    /**
     * Executes the task requested which can be
     * init         Application initialization task.
     * periodic     To periodically update stock symbol bid price.
     * history      To periodically update the stock history.
     */

    @Override
    public int onRunTask(TaskParams params) {
        Cursor initQueryCursor;
        String tag = params.getTag();

        if (mContext == null) {
            mContext = this;
        }

        if (params.getTag().equals(HISTORY_TAG)) {
            return executeHistoryTask();
        }

        StringBuilder urlStringBuilder = new StringBuilder();
        // Base URL for the Yahoo query
        urlStringBuilder.append(BASE_URL);

        try {
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
                    + "in (", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            notifyResult(Utils.RESULT_FAILURE, mContext.getString(R.string.application_error));
        }

        if (tag.equals(INIT_TAG) || tag.equals(PERIODIC_TAG)) {
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                    null, null);
            if (initQueryCursor.getCount() == 0 || initQueryCursor == null) {
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    urlStringBuilder.append(
                            URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    notifyResult(Utils.RESULT_FAILURE, mContext.getString(R.string.application_error));
                }
            } else if (initQueryCursor != null) {
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    mStoredSymbols.append("\"" +
                            initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")) + "\",");
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    notifyResult(Utils.RESULT_FAILURE, mContext.getString(R.string.application_error));
                }
            }
        } else if (tag.equals("add")) {
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString("symbol");
            try {
                urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                notifyResult(Utils.RESULT_FAILURE, mContext.getString(R.string.application_error));
            }
        }
        // finalize the URL for the API query.
        urlStringBuilder.append(FORMAT);

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (urlStringBuilder != null) {
            urlString = urlStringBuilder.toString();
            try {
                getResponse = fetchData(urlString);
                result = GcmNetworkManager.RESULT_SUCCESS;
                try {
                    ContentValues contentValues = new ContentValues();
                    // update ISCURRENT to 0 (false) so new data is current
                    if (isUpdate) {
                        contentValues.put(QuoteColumns.ISCURRENT, 0);
                        mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                                null, null);
                    }
                    mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                            Utils.quoteJsonToContentVals(getResponse));
                } catch (RemoteException | OperationApplicationException e) {
                    notifyResult(Utils.RESULT_FAILURE, mContext.getString(R.string.application_error));
                    return result;
                } catch (JSONException e) {
                    notifyResult(Utils.RESULT_FAILURE, mContext.getString(R.string.empty_stock_quotes_error));
                    return result;
                }catch (CountZeroException e) {
                    notifyResult(Utils.RESULT_FAILURE, mContext.getString(R.string.stock_symbol_not_present_error));
                    // e.printStackTrace();
                    return result;
                } catch (NonExistentSymbolException e) {
                    notifyResult(Utils.RESULT_FAILURE, mContext.getString(R.string.stock_symbol_not_present_error));
                   // e.printStackTrace();
                    return result;
                }
            }catch (IOException e) {
                notifyResult(Utils.RESULT_FAILURE, mContext.getString(R.string.server_error));
                return result;
            }
        }
        notifyResult(Utils.RESULT_SUCCESS, null);
        return result;
    }

    /**
     * Modifies the default shared preference with result status and appropriate message.
     *
     * @param result
     * @param message
     */
    private void notifyResult(String result, String message) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(mContext.getString(R.string.result), result);
        editor.putString(mContext.getString(R.string.error_message), message);
        editor.commit();
    }

    /**
     * Builds the stock history query.
     *
     * @param symbol
     * @param startDate
     * @param endDate
     * @return
     */

    private String buildQuery(String symbol, String startDate, String endDate) {
        StringBuilder queryBuilder = new StringBuilder();
        String query = "select * from yahoo.finance.historicaldata where";
        queryBuilder.append(query);
        queryBuilder.append(" symbol=\"" + symbol + "\"");
        queryBuilder.append(" and startDate=\"" + startDate + "\"");
        queryBuilder.append(" and endDate=\"" + endDate + "\"");
        return queryBuilder.toString();
    }

    /**
     * Executes the stock history to get stock historical data.
     * @return The status of the task
     */
    private int executeHistoryTask() {
        String query = "select * from yahoo.finance.historicaldata where";

        Cursor cursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                null, null);

        String response;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (cursor != null && cursor.getCount() != 0) {

            StringBuilder stringBuilder;
            String symbol;
            while (cursor.moveToNext()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(BASE_URL);
                symbol = cursor.getString(cursor.getColumnIndex("symbol"));
                String startDate = Utils.getPreviousDayDate();
                String endDate = String.format("%tF", new Date());
                try {
                    stringBuilder.append(URLEncoder.encode(buildQuery(symbol, startDate, endDate), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    notifyResult(Utils.RESULT_FAILURE, mContext.getString(R.string.application_error));
                    return result;
                }

                stringBuilder.append(FORMAT);

                try {

                    response = fetchData(stringBuilder.toString());
                    List<StockHistory> stockHistoryList = Utils.quoteToStockHistory(response);

                    //delete old entry for this symbol from history table and insert a new one
                    String where = HistoryColumns.DATE + "= ?";
                    String selectionArg[] = {Utils.getTenDaysBeforePreviousDate()};
                    int delete = mContext.getContentResolver().delete(QuoteProvider.History.withSymbol(symbol)
                            , where, selectionArg);

                    int i = Utils.bulkInsert(stockHistoryList, symbol, mContext.getContentResolver());

                    if (i == 1)
                        result = GcmNetworkManager.RESULT_SUCCESS;

                } catch (IOException e) {
                    notifyResult(Utils.RESULT_FAILURE, mContext.getString(R.string.server_error));
                    return result;
                } catch (JSONException e) {
                    notifyResult(Utils.RESULT_FAILURE, mContext.getString(R.string.empty_stock_quotes_error));
                    return result;
                }
            }
        }
        return GcmNetworkManager.RESULT_SUCCESS;
    }

}
