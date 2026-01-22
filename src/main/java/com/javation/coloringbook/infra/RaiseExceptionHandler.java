package com.javation.coloringbook.infra;

import com.javation.coloringbook.exceptions.BusinessRuleException;
import com.javation.coloringbook.exceptions.DuplicateUserException;
import com.javation.coloringbook.exceptions.JsonMalformedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RaiseExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(DuplicateUserException.class)
    private ResponseEntity<String> duplicateUserHandler(DuplicateUserException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ex.getMessage());
    }

    @ExceptionHandler(JsonMalformedException.class)
    private ResponseEntity<String> jsonMalformedHandler(JsonMalformedException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    private ResponseEntity<String> businessRuleHandler(BusinessRuleException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ex.getMessage());
    }

}
