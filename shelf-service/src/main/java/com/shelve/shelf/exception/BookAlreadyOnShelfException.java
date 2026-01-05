package com.shelve.shelf.exception;

public class BookAlreadyOnShelfException extends RuntimeException {
    public BookAlreadyOnShelfException(String message) {
        super(message);
    }
}
