package com.javation.coloringbook.exceptions;

public class JsonMalformedException extends RuntimeException{

    public JsonMalformedException() { super("Não foram passados todos os campos necessários"); }

    public JsonMalformedException(String message) { super(message); }
}
