package com.bank.repository;

import com.bank.model.Account;
import com.bank.model.CheckingAccount;
import com.bank.model.Customer;
import com.bank.model.SavingsAccount;
import com.bank.model.Transaction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * File-based implementation of AccountRepository using human-readable CSV files.
 * Provides persistent storage without requiring an external database server or driver.
 */
public class FileAccountRepository implements AccountRepository {
    private final String dataDir;
    private final String accountsFile;
    private final String transactionsFile;

    private final Map<String, Account> accountMap = new ConcurrentHashMap<>();
    private final Map<String, List<Transaction>> transactionMap = new ConcurrentHashMap<>();
    private final AtomicInteger accountSequence = new AtomicInteger(1000);

    public FileAccountRepository() {
        this("data");
    }

    public FileAccountRepository(String dataDir) {
        this.dataDir = dataDir;
        this.accountsFile = dataDir + File.separator + "accounts.csv";
        this.transactionsFile = dataDir + File.separator + "transactions.csv";
        initializeStorage();
        loadAll();
    }

    private void initializeStorage() {
        try {
            Path path = Paths.get(dataDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not create data directory: " + e.getMessage());
        }
    }

    @Override
    public synchronized void save(Account account) {
        accountMap.put(account.getAccountNumber(), account);
        persistAll();
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        if (accountNumber == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(accountMap.get(accountNumber.trim().toUpperCase()));
    }

    @Override
    public List<Account> findAll() {
        return new ArrayList<>(accountMap.values());
    }

    @Override
    public synchronized void saveTransaction(Transaction transaction) {
        transactionMap.computeIfAbsent(transaction.getAccountNumber(), k -> new ArrayList<>()).add(transaction);
        appendTransactionToFile(transaction);
    }

    @Override
    public List<Transaction> findTransactionsByAccount(String accountNumber) {
        List<Transaction> list = transactionMap.get(accountNumber);
        if (list == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(list);
    }

    @Override
    public synchronized String generateNextAccountNumber() {
        return "ACC-" + accountSequence.incrementAndGet();
    }

    @Override
    public synchronized void persistAll() {
        // Persist Accounts
        File file = new File(accountsFile);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.write("accountType,accountNumber,pinHash,balance,createdAt,active,customerId,customerName,customerEmail,customerPhone,extra1,extra2");
            writer.newLine();

            for (Account acc : accountMap.values()) {
                String line = serializeAccount(acc);
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving accounts to file: " + e.getMessage());
        }
    }

    private synchronized void appendTransactionToFile(Transaction tx) {
        File file = new File(transactionsFile);
        boolean exists = file.exists() && file.length() > 0;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (!exists) {
                writer.write("id,accountNumber,type,amount,balanceAfter,timestamp,description");
                writer.newLine();
            }
            writer.write(tx.toCsv());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error appending transaction to file: " + e.getMessage());
        }
    }

    private void loadAll() {
        loadTransactions();
        loadAccounts();
    }

    private void loadTransactions() {
        File file = new File(transactionsFile);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine(); // Header
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    Transaction tx = Transaction.fromCsv(line);
                    transactionMap.computeIfAbsent(tx.getAccountNumber(), k -> new ArrayList<>()).add(tx);
                } catch (Exception ex) {
                    System.err.println("Skipping malformed transaction line: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Error reading transactions file: " + e.getMessage());
        }
    }

    private void loadAccounts() {
        File file = new File(accountsFile);
        if (!file.exists()) {
            return;
        }

        int maxSeq = 1000;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine(); // Header
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    Account acc = deserializeAccount(line);
                    if (acc != null) {
                        accountMap.put(acc.getAccountNumber(), acc);
                        // Populate transactions loaded earlier
                        List<Transaction> txs = transactionMap.get(acc.getAccountNumber());
                        if (txs != null) {
                            for (Transaction tx : txs) {
                                acc.addTransaction(tx);
                            }
                        }
                        // Update sequence number
                        String numStr = acc.getAccountNumber().replace("ACC-", "");
                        try {
                            int seq = Integer.parseInt(numStr);
                            if (seq > maxSeq) {
                                maxSeq = seq;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                } catch (Exception ex) {
                    System.err.println("Skipping malformed account line: " + line + " (" + ex.getMessage() + ")");
                }
            }
            accountSequence.set(maxSeq);
        } catch (IOException e) {
            System.err.println("Warning: Error reading accounts file: " + e.getMessage());
        }
    }

    private String serializeAccount(Account acc) {
        Customer c = acc.getCustomer();
        String extra1 = "";
        String extra2 = "";

        if (acc instanceof SavingsAccount sa) {
            extra1 = String.valueOf(sa.getMinimumBalance());
            extra2 = String.valueOf(sa.getAnnualInterestRate());
        } else if (acc instanceof CheckingAccount ca) {
            extra1 = String.valueOf(ca.getOverdraftLimit());
            extra2 = String.valueOf(ca.getOverdraftFee());
        }

        return String.join(",",
                escape(acc.getAccountType()),
                escape(acc.getAccountNumber()),
                escape(acc.getPinHash()),
                String.format(java.util.Locale.US, "%.2f", acc.getBalance()),
                escape(acc.getCreatedAt().format(Account.DATE_FORMAT)),
                String.valueOf(acc.isActive()),
                escape(c.getCustomerId()),
                escape(c.getFullName()),
                escape(c.getEmail()),
                escape(c.getPhone()),
                escape(extra1),
                escape(extra2)
        );
    }

    private Account deserializeAccount(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 12) {
            return null;
        }
        String type = unescape(parts[0]);
        String accNum = unescape(parts[1]);
        String pinHash = unescape(parts[2]);
        double balance = Double.parseDouble(parts[3]);
        LocalDateTime createdAt = LocalDateTime.parse(unescape(parts[4]), Account.DATE_FORMAT);
        boolean active = Boolean.parseBoolean(parts[5]);

        String custId = unescape(parts[6]);
        String name = unescape(parts[7]);
        String email = unescape(parts[8]);
        String phone = unescape(parts[9]);
        Customer customer = new Customer(custId, name, email, phone);

        Account account;
        if ("SAVINGS".equalsIgnoreCase(type)) {
            double minBal = Double.parseDouble(unescape(parts[10]));
            double interestRate = Double.parseDouble(unescape(parts[11]));
            account = new SavingsAccount(accNum, customer, pinHash, 0, minBal, interestRate, createdAt);
        } else {
            double overdraftLimit = Double.parseDouble(unescape(parts[10]));
            double overdraftFee = Double.parseDouble(unescape(parts[11]));
            account = new CheckingAccount(accNum, customer, pinHash, 0, overdraftLimit, overdraftFee, createdAt);
        }

        // Reflection or direct package access to restore exact balance & active status without triggering new initial transactions
        try {
            var balanceField = Account.class.getDeclaredField("balance");
            balanceField.setAccessible(true);
            balanceField.set(account, balance);

            var activeField = Account.class.getDeclaredField("active");
            activeField.setAccessible(true);
            activeField.set(account, active);
        } catch (Exception e) {
            throw new RuntimeException("Failed to restore account state", e);
        }

        return account;
    }

    private static String escape(String val) {
        if (val == null) val = "";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }

    private static String unescape(String val) {
        if (val == null) return "";
        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
            val = val.substring(1, val.length() - 1);
        }
        return val.replace("\"\"", "\"");
    }
}
