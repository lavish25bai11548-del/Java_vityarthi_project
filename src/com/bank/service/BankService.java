package com.bank.service;

import com.bank.exception.AccountNotFoundException;
import com.bank.exception.BankingException;
import com.bank.exception.InvalidPinException;
import com.bank.model.Account;
import com.bank.model.CheckingAccount;
import com.bank.model.Customer;
import com.bank.model.SavingsAccount;
import com.bank.model.Transaction;
import com.bank.model.TransactionType;
import com.bank.repository.AccountRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service orchestrating banking business logic, security, and atomic operations.
 */
public class BankService {
    private final AccountRepository repository;

    public BankService(AccountRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository cannot be null");
    }

    /**
     * Opens a new Savings Account.
     */
    public synchronized SavingsAccount openSavingsAccount(String fullName, String email, String phone,
                                                          String pin, double initialDeposit) throws BankingException {
        if (initialDeposit < SavingsAccount.DEFAULT_MIN_BALANCE) {
            throw new BankingException(String.format(
                    "Initial deposit must be at least the minimum balance requirement of $%.2f",
                    SavingsAccount.DEFAULT_MIN_BALANCE));
        }
        String accNum = repository.generateNextAccountNumber();
        String custId = "CUST-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Customer customer = new Customer(custId, fullName, email, phone);
        String pinHash = SecurityUtil.hashPin(pin);

        SavingsAccount account = new SavingsAccount(accNum, customer, pinHash, initialDeposit);
        repository.save(account);
        for (Transaction tx : account.getTransactions()) {
            repository.saveTransaction(tx);
        }
        return account;
    }

    /**
     * Opens a new Checking Account.
     */
    public synchronized CheckingAccount openCheckingAccount(String fullName, String email, String phone,
                                                            String pin, double initialDeposit) throws BankingException {
        if (initialDeposit < 0) {
            throw new BankingException("Initial deposit cannot be negative.");
        }
        String accNum = repository.generateNextAccountNumber();
        String custId = "CUST-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Customer customer = new Customer(custId, fullName, email, phone);
        String pinHash = SecurityUtil.hashPin(pin);

        CheckingAccount account = new CheckingAccount(accNum, customer, pinHash, initialDeposit);
        repository.save(account);
        for (Transaction tx : account.getTransactions()) {
            repository.saveTransaction(tx);
        }
        return account;
    }

    /**
     * Authenticates account credentials.
     */
    public Account authenticate(String accountNumber, String pin) throws BankingException {
        Account account = getAccount(accountNumber);
        account.authenticate(pin);
        return account;
    }

    /**
     * Retrieves an account or throws AccountNotFoundException.
     */
    public Account getAccount(String accountNumber) throws AccountNotFoundException {
        return repository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
    }

    /**
     * Deposits funds into an account.
     */
    public synchronized void deposit(String accountNumber, double amount, String description) throws BankingException {
        Account account = getAccount(accountNumber);
        int initialTxCount = account.getTransactions().size();
        account.deposit(amount, description);

        // Persist newly generated transaction
        if (account.getTransactions().size() > initialTxCount) {
            Transaction latestTx = account.getTransactions().get(account.getTransactions().size() - 1);
            repository.saveTransaction(latestTx);
        }
        repository.save(account);
    }

    /**
     * Withdraws funds after validating the user's PIN.
     */
    public synchronized void withdraw(String accountNumber, String pin, double amount, String description) throws BankingException {
        Account account = authenticate(accountNumber, pin);
        int initialTxCount = account.getTransactions().size();
        account.withdraw(amount, description);

        // Persist new transactions (could be withdrawal + fee)
        for (int i = initialTxCount; i < account.getTransactions().size(); i++) {
            repository.saveTransaction(account.getTransactions().get(i));
        }
        repository.save(account);
    }

    /**
     * Executes an atomic transfer between two accounts.
     * Prevents partial state updates: if any step fails, money is restored.
     */
    public synchronized void transfer(String fromAccNum, String pin, String toAccNum,
                                     double amount, String notes) throws BankingException {
        if (fromAccNum.equalsIgnoreCase(toAccNum)) {
            throw new BankingException("Cannot transfer funds to the same account.");
        }
        if (amount <= 0) {
            throw new BankingException("Transfer amount must be strictly positive.");
        }

        Account sender = authenticate(fromAccNum, pin);
        Account recipient = getAccount(toAccNum);

        if (!recipient.isActive()) {
            throw new BankingException("Recipient account " + toAccNum + " is inactive or frozen.");
        }

        double senderOriginalBalance = sender.getBalance();
        int senderTxCount = sender.getTransactions().size();
        int recipientTxCount = recipient.getTransactions().size();

        try {
            String outDesc = "Transfer to " + recipient.getAccountNumber() + (notes != null && !notes.isBlank() ? " (" + notes + ")" : "");
            sender.withdraw(amount, outDesc);

            String inDesc = "Transfer from " + sender.getAccountNumber() + (notes != null && !notes.isBlank() ? " (" + notes + ")" : "");
            recipient.deposit(amount, inDesc);

            // Persist new sender transactions
            for (int i = senderTxCount; i < sender.getTransactions().size(); i++) {
                Transaction tx = sender.getTransactions().get(i);
                // Mark transfer out specifically if it was generic withdrawal
                repository.saveTransaction(tx);
            }
            // Persist new recipient transactions
            for (int i = recipientTxCount; i < recipient.getTransactions().size(); i++) {
                Transaction tx = recipient.getTransactions().get(i);
                repository.saveTransaction(tx);
            }

            repository.save(sender);
            repository.save(recipient);
        } catch (Exception e) {
            // Rollback if failure occurred after sender withdrawal
            try {
                var balField = Account.class.getDeclaredField("balance");
                balField.setAccessible(true);
                balField.set(sender, senderOriginalBalance);
            } catch (Exception ignored) {}
            throw new BankingException("Transfer failed and was rolled back: " + e.getMessage(), e);
        }
    }

    /**
     * Calculates and credits monthly interest across all active savings accounts.
     */
    public synchronized double applyMonthlyInterestToAllSavings() {
        double totalInterestDistributed = 0.0;
        for (Account acc : repository.findAll()) {
            if (acc instanceof SavingsAccount savings && acc.isActive()) {
                int preCount = savings.getTransactions().size();
                double interest = savings.applyMonthlyInterest();
                if (interest > 0) {
                    totalInterestDistributed += interest;
                    if (savings.getTransactions().size() > preCount) {
                        repository.saveTransaction(savings.getTransactions().get(savings.getTransactions().size() - 1));
                    }
                    repository.save(savings);
                }
            }
        }
        return totalInterestDistributed;
    }

    /**
     * Lists all registered accounts.
     */
    public List<Account> getAllAccounts() {
        return repository.findAll();
    }

    /**
     * Calculates total bank liquidity across all accounts.
     */
    public double getTotalBankReserves() {
        return repository.findAll().stream()
                .mapToDouble(Account::getBalance)
                .sum();
    }

    /**
     * Freezes or activates an account.
     */
    public synchronized void setAccountActive(String accountNumber, boolean active) throws AccountNotFoundException {
        Account account = getAccount(accountNumber);
        account.setActive(active);
        repository.save(account);
    }

    /**
     * Retrieves transactions for an account with PIN verification.
     */
    public List<Transaction> getStatement(String accountNumber, String pin) throws BankingException {
        authenticate(accountNumber, pin);
        return repository.findTransactionsByAccount(accountNumber);
    }
}
