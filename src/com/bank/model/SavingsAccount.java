package com.bank.model;

import com.bank.exception.BankingException;
import com.bank.exception.InsufficientFundsException;

import java.time.LocalDateTime;

/**
 * Concrete Savings Account.
 * Demonstrates Inheritance and Polymorphism.
 * Enforces a minimum balance rule and pays annual interest.
 */
public class SavingsAccount extends Account {
    private static final long serialVersionUID = 1L;

    public static final double DEFAULT_MIN_BALANCE = 100.0;
    public static final double DEFAULT_ANNUAL_INTEREST_RATE = 4.5; // 4.5%

    private final double minimumBalance;
    private final double annualInterestRate;

    public SavingsAccount(String accountNumber, Customer customer, String pinHash, double initialDeposit) {
        this(accountNumber, customer, pinHash, initialDeposit, DEFAULT_MIN_BALANCE, DEFAULT_ANNUAL_INTEREST_RATE, LocalDateTime.now());
    }

    public SavingsAccount(String accountNumber, Customer customer, String pinHash, double initialDeposit,
                          double minimumBalance, double annualInterestRate, LocalDateTime createdAt) {
        super(accountNumber, customer, pinHash, initialDeposit, createdAt);
        this.minimumBalance = minimumBalance;
        this.annualInterestRate = annualInterestRate;
    }

    public double getMinimumBalance() {
        return minimumBalance;
    }

    public double getAnnualInterestRate() {
        return annualInterestRate;
    }

    @Override
    public synchronized void withdraw(double amount, String description) throws BankingException {
        if (!active) {
            throw new BankingException("Account is currently inactive or frozen.");
        }
        if (amount <= 0) {
            throw new BankingException("Withdrawal amount must be strictly positive.");
        }
        if (this.balance - amount < minimumBalance) {
            throw new InsufficientFundsException(String.format(
                    "Withdrawal denied. Minimum balance requirement of $%.2f must be maintained. Current balance: $%.2f, Requested: $%.2f",
                    minimumBalance, this.balance, amount
            ));
        }

        this.balance -= amount;
        Transaction tx = new Transaction(
                this.accountNumber,
                TransactionType.WITHDRAWAL,
                amount,
                this.balance,
                description != null ? description : "Cash Withdrawal"
        );
        this.transactions.add(tx);
    }

    /**
     * Calculates and credits monthly interest based on annual interest rate.
     */
    public synchronized double applyMonthlyInterest() {
        if (!active || this.balance <= 0) {
            return 0.0;
        }
        double monthlyRate = (annualInterestRate / 100.0) / 12.0;
        double interest = Math.round(this.balance * monthlyRate * 100.0) / 100.0;
        if (interest > 0) {
            this.balance += interest;
            Transaction tx = new Transaction(
                    this.accountNumber,
                    TransactionType.INTEREST,
                    interest,
                    this.balance,
                    String.format("Monthly Interest Credit (%.2f%% p.a.)", annualInterestRate)
            );
            this.transactions.add(tx);
        }
        return interest;
    }

    @Override
    public String getAccountType() {
        return "SAVINGS";
    }
}
