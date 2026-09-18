package com.bank.exception;

/**
 * Thrown when a withdrawal or transfer amount exceeds available funds.
 */
public class InsufficientFundsException extends BankingException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
