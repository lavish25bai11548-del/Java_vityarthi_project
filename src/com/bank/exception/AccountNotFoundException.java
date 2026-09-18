package com.bank.exception;

/**
 * Thrown when an account cannot be located by account number.
 */
public class AccountNotFoundException extends BankingException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
