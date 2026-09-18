package com.bank.exception;

/**
 * Thrown when a checking account withdrawal exceeds allowed overdraft limit.
 */
public class OverdraftExceededException extends BankingException {
    public OverdraftExceededException(String message) {
        super(message);
    }
}
