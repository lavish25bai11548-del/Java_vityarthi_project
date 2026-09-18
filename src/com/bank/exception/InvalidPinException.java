package com.bank.exception;

/**
 * Thrown when PIN authentication fails.
 */
public class InvalidPinException extends BankingException {
    public InvalidPinException(String message) {
        super(message);
    }
}
