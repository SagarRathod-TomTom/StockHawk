/**
 * Copyright (C) August 2016
 * The Stock Hawk project
 */

package com.sam_chordas.android.stockhawk.data;

import android.net.Uri;

import net.simonvt.schematic.annotation.ContentProvider;
import net.simonvt.schematic.annotation.ContentUri;
import net.simonvt.schematic.annotation.InexactContentUri;
import net.simonvt.schematic.annotation.TableEndpoint;

/**
 * Stock Hawk Content Provider to manage stock hawk database.
 *
 * @author Sagar Rathod
 * @version 1.0
 */

@ContentProvider(authority = QuoteProvider.AUTHORITY, database = QuoteDatabase.class)
public class QuoteProvider {
    public static final String AUTHORITY = "com.sam_chordas.android.stockhawk.data.QuoteProvider";

    static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * Builds the URIs with base content uri.
     *
     * @param paths
     * @return
     */
    private static Uri buildUri(String... paths) {
        Uri.Builder builder = BASE_CONTENT_URI.buildUpon();
        for (String path : paths) {
            builder.appendPath(path);
        }
        return builder.build();
    }

    interface Path {
        String QUOTES = "quotes";
        String HISTORY = "history";
    }

    /**
     * Stock Quote table
     */
    @TableEndpoint(table = QuoteDatabase.QUOTES)
    public static class Quotes {
        @ContentUri(
                path = Path.QUOTES,
                type = "vnd.android.cursor.dir/quote"
        )
        public static final Uri CONTENT_URI = buildUri(Path.QUOTES);

        @InexactContentUri(
                name = "QUOTE_ID",
                path = Path.QUOTES + "/*",
                type = "vnd.android.cursor.item/quote",
                whereColumn = QuoteColumns.SYMBOL,
                pathSegment = 1
        )
        /**
         *  Builds uri to find specific symbol from quotes table.
         */
        public static Uri withSymbol(String symbol) {
            return buildUri(Path.QUOTES, symbol);
        }
    }

    /**
     * Stock History table
     */
    @TableEndpoint(table = QuoteDatabase.HISTORY)
    public static class History {

        @ContentUri(path = Path.HISTORY,
                type = "vnd.android.cursor.dir/history")
        public static final Uri CONTENT_URI = buildUri(Path.HISTORY);

        @InexactContentUri(
                name = "HISTORY_SYMBOL",
                path = Path.HISTORY + "/*",
                type = "vnd.android.cursor.item/history",
                whereColumn = HistoryColumns.SYMBOL,
                pathSegment = 1
        )

        /**
         *  Builds uri to find specific symbol history from stock history table.
         */
        public static final Uri withSymbol(String symbol) {
            return buildUri(Path.HISTORY, symbol);
        }

    }

}
