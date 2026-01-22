package com.javation.coloringbook.exceptions;

public class DuplicateUserException extends RuntimeException{

    public DuplicateUserException() { super("Usuário já está cadastrado"); }

    public DuplicateUserException(String message) { super(message); }
}
