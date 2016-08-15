/**
 * Copyright (C) August 2016
 * The Stock Hawk project
 */

package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

/**
 * Stock Hawk database.
 *
 * @author Sagar Rathod
 * @version 1.0
 *
 */
@Database(version = QuoteDatabase.VERSION)
public class QuoteDatabase {
    public static final int VERSION = 7;

    @Table(QuoteColumns.class)
    public static final String QUOTES = "quotes";

    @Table(HistoryColumns.class)
    public static final String HISTORY = "history";

    private QuoteDatabase() {
    }
}
