package com.javation.coloringbook.exceptions;

public class BusinessRuleException extends RuntimeException{
    public BusinessRuleException() { super("Algum valor não segue as normas aceitas"); }

    public BusinessRuleException(String message) { super(message); }
}
