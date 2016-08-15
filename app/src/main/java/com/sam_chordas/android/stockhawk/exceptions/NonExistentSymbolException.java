/**
 * Copyright (C) August 2016
 * The Stock Hawk project
 */

package com.sam_chordas.android.stockhawk.exceptions;

/**
 * Exception class to handle non-existent stock quote.
 *
 * @author Sagar Rathod
 * @version 1.0
 */

public class NonExistentSymbolException extends Exception {

    public NonExistentSymbolException(){
        super();
    }

    @Override
    public String toString() {
        return "NonExistentSymbolException: Symbol does not exist.";
    }
}
