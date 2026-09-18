package com.bank.model;

import com.bank.exception.BankingException;
import com.bank.exception.OverdraftExceededException;

import java.time.LocalDateTime;

/**
 * Concrete Checking Account.
 * Demonstrates Inheritance and Polymorphism.
 * Permits overdraft transactions up to a designated credit threshold.
 */
public class CheckingAccount extends Account {
    private static final long serialVersionUID = 1L;

    public static final double DEFAULT_OVERDRAFT_LIMIT = 500.0;
    public static final double DEFAULT_OVERDRAFT_FEE = 15.0;

    private final double overdraftLimit;
    private final double overdraftFee;

    public CheckingAccount(String accountNumber, Customer customer, String pinHash, double initialDeposit) {
        this(accountNumber, customer, pinHash, initialDeposit, DEFAULT_OVERDRAFT_LIMIT, DEFAULT_OVERDRAFT_FEE, LocalDateTime.now());
    }

    public CheckingAccount(String accountNumber, Customer customer, String pinHash, double initialDeposit,
                           double overdraftLimit, double overdraftFee, LocalDateTime createdAt) {
        super(accountNumber, customer, pinHash, initialDeposit, createdAt);
        this.overdraftLimit = overdraftLimit;
        this.overdraftFee = overdraftFee;
    }

    public double getOverdraftLimit() {
        return overdraftLimit;
    }

    public double getOverdraftFee() {
        return overdraftFee;
    }

    public double getAvailableFunds() {
        return this.balance + this.overdraftLimit;
    }

    @Override
    public synchronized void withdraw(double amount, String description) throws BankingException {
        if (!active) {
            throw new BankingException("Account is currently inactive or frozen.");
        }
        if (amount <= 0) {
            throw new BankingException("Withdrawal amount must be strictly positive.");
        }

        double available = this.balance + this.overdraftLimit;
        if (amount > available) {
            throw new OverdraftExceededException(String.format(
                    "Withdrawal denied. Exceeds total available credit of $%.2f (Balance: $%.2f + Overdraft Limit: $%.2f). Requested: $%.2f",
                    available, this.balance, this.overdraftLimit, amount
            ));
        }

        boolean willIncurOverdraftFee = (this.balance >= 0 && (this.balance - amount) < 0);

        this.balance -= amount;
        Transaction tx = new Transaction(
                this.accountNumber,
                TransactionType.WITHDRAWAL,
                amount,
                this.balance,
                description != null ? description : "Checking Withdrawal"
        );
        this.transactions.add(tx);

        // Apply overdraft charge if crossing from non-negative to negative
        if (willIncurOverdraftFee && overdraftFee > 0) {
            this.balance -= overdraftFee;
            Transaction feeTx = new Transaction(
                    this.accountNumber,
                    TransactionType.FEE,
                    overdraftFee,
                    this.balance,
                    "Overdraft Protection Service Fee"
            );
            this.transactions.add(feeTx);
        }
    }

    @Override
    public String getAccountType() {
        return "CHECKING";
    }
}
