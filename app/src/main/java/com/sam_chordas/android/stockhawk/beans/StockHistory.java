/**
 * Copyright (C) August 2016
 * The Stock Hawk project
 */
package com.sam_chordas.android.stockhawk.beans;

/**
 * Bean class for Stock History object to encapsulate stock data.
 *
 * @author Sagar Rathod
 * @version 1.0
 */

public class StockHistory {
    private float mLow;
    private float mHigh;
    private String mDate;

    public String getDate() {
        return mDate;
    }

    public void setDate(String mDate) {
        this.mDate = mDate;
    }

    public float getHigh() {
        return mHigh;
    }

    public void setHigh(float mHigh) {
        this.mHigh = mHigh;
    }

    public float getLow() {
        return mLow;
    }

    public void setLow(float mLow) {
        this.mLow = mLow;
    }

    @Override
    public String toString() {
        return "StockHistory:[" + mLow + ", " + mHigh + ", " + mDate + "]";
    }
}
