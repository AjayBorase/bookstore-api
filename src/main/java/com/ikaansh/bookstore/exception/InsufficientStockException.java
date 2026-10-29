package com.ikaansh.bookstore.exception;

public class InsufficientStockException extends ConflictException {
    public InsufficientStockException(String title, int requested, int available) {
        super("Insufficient stock for '" + title + "': requested " + requested + ", available " + available);
    }
}
