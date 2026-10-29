package com.ikaansh.bookstore.exception;

/** Maps to HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
