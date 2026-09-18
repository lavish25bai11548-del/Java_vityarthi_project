package com.bank.model;

import com.bank.exception.BankingException;
import com.bank.exception.InvalidPinException;
import com.bank.service.SecurityUtil;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base class representing a bank account.
 * Demonstrates Object-Oriented Principles: Abstraction, Encapsulation, and Polymorphism.
 */
public abstract class Account implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected final String accountNumber;
    protected final Customer customer;
    protected String pinHash;
    protected double balance;
    protected final LocalDateTime createdAt;
    protected boolean active;
    protected final List<Transaction> transactions;

    public Account(String accountNumber, Customer customer, String pinHash, double initialDeposit, LocalDateTime createdAt) {
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number cannot be null");
        this.customer = Objects.requireNonNull(customer, "Customer cannot be null");
        this.pinHash = Objects.requireNonNull(pinHash, "PIN hash cannot be null");
        this.balance = 0.0;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.active = true;
        this.transactions = new ArrayList<>();

        if (initialDeposit > 0) {
            this.balance = initialDeposit;
            this.transactions.add(new Transaction(
                    this.accountNumber,
                    TransactionType.DEPOSIT,
                    initialDeposit,
                    this.balance,
                    "Initial Account Opening Deposit"
            ));
        }
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public double getBalance() {
        return balance;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public void addTransaction(Transaction tx) {
        if (tx != null) {
            this.transactions.add(tx);
        }
    }

    public boolean authenticate(String pin) throws InvalidPinException {
        if (!active) {
            throw new InvalidPinException("Account is currently inactive/frozen.");
        }
        if (!SecurityUtil.verifyPin(pin, pinHash)) {
            throw new InvalidPinException("Invalid PIN entered for account: " + accountNumber);
        }
        return true;
    }

    /**
     * Deposits funds into the account.
     * Common to all account types.
     */
    public synchronized void deposit(double amount, String description) throws BankingException {
        if (!active) {
            throw new BankingException("Cannot deposit into an inactive account.");
        }
        if (amount <= 0) {
            throw new BankingException("Deposit amount must be strictly positive.");
        }
        this.balance += amount;
        Transaction tx = new Transaction(
                this.accountNumber,
                TransactionType.DEPOSIT,
                amount,
                this.balance,
                description != null ? description : "Cash Deposit"
        );
        this.transactions.add(tx);
    }

    /**
     * Abstract method for withdrawing funds.
     * Concrete account types define their own overdraft / minimum balance rules.
     */
    public abstract void withdraw(double amount, String description) throws BankingException;

    /**
     * Account classification name.
     */
    public abstract String getAccountType();

    /**
     * Summarized account details.
     */
    @Override
    public String toString() {
        return String.format("[%s] #%s | Holder: %s | Balance: $%,10.2f | Status: %s",
                getAccountType(), accountNumber, customer.getFullName(), balance, active ? "ACTIVE" : "FROZEN");
    }
}
