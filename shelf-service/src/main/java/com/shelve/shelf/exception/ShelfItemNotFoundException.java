package com.shelve.shelf.exception;

public class ShelfItemNotFoundException extends RuntimeException {
    public ShelfItemNotFoundException(String message) {
        super(message);
    }
}
