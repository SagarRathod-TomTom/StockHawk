/**
 * Copyright (C) August 2016
 * The Stock Hawk project
 */

package com.sam_chordas.android.stockhawk.exceptions;

/**
 * Exception class to handle empty response from Yahoo apis.
 *
 * @author Sagar Rathod
 * @version 1.0
 */
public class CountZeroException extends Exception {

   public CountZeroException(){
        super();
    }

    @Override
    public String toString() {
        return "CountZeroException: Empty Results.";
    }
}
