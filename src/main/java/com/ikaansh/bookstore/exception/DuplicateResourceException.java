package com.ikaansh.bookstore.exception;

public class DuplicateResourceException extends ConflictException {
    public DuplicateResourceException(String message) { super(message); }
}
