/**
 * Copyright (C) August 2016
 * The Stock Hawk project
 */

package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;


/**
 * Main activity for stock hawk application.
 *
 * @author Sagar Rathod
 * @version 1.0
 */

public class MyStocksActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    private static final int CURSOR_LOADER_ID = 0;
    boolean isConnected;
    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private Intent mServiceIntent;
    private ItemTouchHelper mItemTouchHelper;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;
    private Cursor mCursor;
    private RecyclerView mRecyclerView;
    private TextView mEmptyTextView;

    private SharedPreferences mSharedPreference;

    /**
     * Initializes the application UI with list of stock quotes and their bid price.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        isConnected = Utils.isNetworkAvailable(mContext);
        setContentView(R.layout.activity_my_stocks);

        mSharedPreference = PreferenceManager.getDefaultSharedPreferences(this);

        initializeService(savedInstanceState);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mEmptyTextView = (TextView) findViewById(R.id.empty_text_view);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mCursorAdapter = new QuoteCursorAdapter(this, null);
        mRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {

                        TextView stockSymbol = (TextView) v.findViewById(R.id.stock_symbol);
                        if (stockSymbol != null) {
                            String symbol = stockSymbol.getText().toString();
                            Intent intent = new Intent(MyStocksActivity.this, StockHistoryActivity.class);
                            intent.putExtra(getString(R.string.symbol_extra), symbol);
                            startActivity(intent);
                        }
                    }
                }));

        mRecyclerView.setAdapter(mCursorAdapter);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.attachToRecyclerView(mRecyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected) {
                    new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                            .content(R.string.content_test)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {

                                    if(input.toString().trim().equals("")){
                                        Toast toast =
                                                Toast.makeText(MyStocksActivity.this, R.string.incorrect_symbol_entered,
                                                        Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                        toast.show();
                                        return;
                                    }

                                    // On FAB click, receive user input. Make sure the stock doesn't already exist
                                    // in the DB and proceed accordingly
                                    Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                            new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                            new String[]{input.toString()}, null);
                                    if (c.getCount() != 0) {
                                        Toast toast =
                                                Toast.makeText(MyStocksActivity.this, R.string.stock_present_error,
                                                        Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                        toast.show();
                                        return;
                                    } else {
                                        // Add the stock to DB
                                        mServiceIntent.putExtra("tag", "add");
                                        mServiceIntent.putExtra("symbol", input.toString());
                                        startService(mServiceIntent);
                                    }
                                }
                            })
                            .show();
                } else {
                    networkToast(getString(R.string.network_error));
                }
            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        mTitle = getTitle();
        if (isConnected) {
            long period = 3600L;
            long flex = 10L;
            String periodicTag = "periodic";

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask1 = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            // Schedule task with tag "peri;;odic." This ensure that only the stocks present in the DB
            // are updated.

            GcmNetworkManager gcmNetworkManager = GcmNetworkManager.getInstance(this);
            gcmNetworkManager.schedule(periodicTask1);


            period = 60 * 24;
            flex = 10L;
            periodicTag = "history";

            PeriodicTask periodicTask2 = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            gcmNetworkManager.schedule(periodicTask2);

        }
    }

    /**
     * Executes the {@link StockIntentService} for application initialization task
     *
     * @param savedInstanceState
     */
    private void initializeService(Bundle savedInstanceState) {
        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        mServiceIntent = new Intent(this, StockIntentService.class);
        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra("tag", "init");
            if (isConnected) {
                startService(mServiceIntent);
            } else {
                networkToast(getString(R.string.stocks_out_of_date_error));
            }
        }
    }


    /**
     * Restarts the loader and register the listener for shared preference
     */
    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
        mSharedPreference.registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Unregisters the listener for shared preference
     */
    @Override
    protected void onPause() {
        mSharedPreference.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    /**
     * Shows the toast with appropriate message
     *
     * @param message
     */
    public void networkToast(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    /**
     * Inflates the menu for stock hawk.
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        restoreActionBar();
        return true;
    }

    /**
     * Handles the menu item selection event.
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Creates the cursor loader for list of stock quotes.
     * @param id
     * @param args
     * @return
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    /**
     *  Displays the list of stock quotes or particular error message in particular case.
     * @param loader
     * @param data
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if(data != null && data.getCount() == 0){

            if (!isConnected) {
                showErrorMessage(getString(R.string.network_error));
            }
            else
            if(data.getCount() == 0){

                if(getString(R.string.result).equals(Utils.RESULT_FAILURE)){
                    showErrorMessage(getString(R.string.error_message));
                }
                else
                  showErrorMessage(getString(R.string.empty_stock_quotes_error));
            }

        }else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mEmptyTextView.setVisibility(View.GONE);
        }

        mCursorAdapter.swapCursor(data);
        mCursor = data;
        Intent intent = new Intent(Utils.STOCK_APPWIDGET_UPDATE);
        sendBroadcast(intent);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    /**
     * Handles the changes happened to the default shared preference.
     *
     * @param sharedPreferences
     * @param key
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        String value = sharedPreferences.getString(key, null);
        Log.d("Preference changed:",key + ":" + value);
        if (key.equals(getString(R.string.network_state))) {

            value = sharedPreferences.getString(key, Utils.NETWORK_STATE_DISCONNECTED);

            if (value.equals(Utils.NETWORK_STATE_CONNECTED))
                isConnected = true;
                initializeService(null);
                mEmptyTextView.setVisibility(View.GONE);
        }
        else if(key.equals(getString(R.string.result))){

            value = sharedPreferences.getString(key, Utils.RESULT_FAILURE);

            if(value.equals(Utils.RESULT_FAILURE)){
                networkToast(sharedPreferences.getString("error_message",Utils.RESULT_FAILURE));
            }

        }

    }

    /**
     * Updates the empty view with appropriate message.
     * @param message
     */
    private void showErrorMessage(String message) {
        mRecyclerView.setVisibility(View.GONE);
        mEmptyTextView.setVisibility(View.VISIBLE);
        mEmptyTextView.setText(message);
    }

}
