package com.javation.coloringbook.Exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(Long id) {
        super("Usuário não encontrado com ID: " + id);
    }
}
