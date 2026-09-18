package com.bank.exception;

/**
 * Base custom exception for all banking domain errors.
 */
public class BankingException extends Exception {
    public BankingException(String message) {
        super(message);
    }

    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}
