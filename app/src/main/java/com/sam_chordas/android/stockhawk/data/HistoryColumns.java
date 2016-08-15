/**
 * Copyright (C) August 2016
 * The Stock Hawk project
 */

package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

/**
 * List of columns for StockHistory table
 *
 * @author Sagar Rathod
 * @version 1.0
 */
public class HistoryColumns {

    @DataType(DataType.Type.INTEGER) @PrimaryKey
    @AutoIncrement
    public static final String _ID = "_id";

    @DataType(DataType.Type.TEXT)
    public static final String SYMBOL = "symbol";

    @DataType(DataType.Type.TEXT) @NotNull
    public static final String DATE = "date";

    @DataType(DataType.Type.REAL) @NotNull
    public static final String LOW_PRICE = "low_price";

    @DataType(DataType.Type.REAL) @NotNull
    public static final String HIGH_PRICE = "high_price";
}
