package com.javation.coloringbook.Exception;

public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(String message) {
        super(message);
    }

    public BookNotFoundException(Long id) {
        super("Livro não encontrado com ID: " + id);
    }
}
