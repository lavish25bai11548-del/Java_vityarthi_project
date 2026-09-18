package com.bank.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable record representing a single banking transaction.
 */
public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String id;
    private final String accountNumber;
    private final TransactionType type;
    private final double amount;
    private final double balanceAfter;
    private final LocalDateTime timestamp;
    private final String description;

    public Transaction(String accountNumber, TransactionType type, double amount, double balanceAfter, String description) {
        this("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
             accountNumber, type, amount, balanceAfter, LocalDateTime.now(), description);
    }

    public Transaction(String id, String accountNumber, TransactionType type, double amount, double balanceAfter, LocalDateTime timestamp, String description) {
        this.id = Objects.requireNonNull(id);
        this.accountNumber = Objects.requireNonNull(accountNumber);
        this.type = Objects.requireNonNull(type);
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = Objects.requireNonNull(timestamp);
        this.description = description == null ? "" : description;
    }

    public String getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Converts transaction to a CSV record.
     */
    public String toCsv() {
        return String.join(",",
                escapeCsv(id),
                escapeCsv(accountNumber),
                escapeCsv(type.name()),
                String.format(java.util.Locale.US, "%.2f", amount),
                String.format(java.util.Locale.US, "%.2f", balanceAfter),
                escapeCsv(timestamp.format(FORMATTER)),
                escapeCsv(description)
        );
    }

    /**
     * Parses a transaction from a CSV line.
     */
    public static Transaction fromCsv(String csvLine) {
        String[] parts = csvLine.split(",", -1);
        if (parts.length < 7) {
            throw new IllegalArgumentException("Invalid transaction CSV line: " + csvLine);
        }
        String id = unescapeCsv(parts[0]);
        String accNum = unescapeCsv(parts[1]);
        TransactionType type = TransactionType.valueOf(unescapeCsv(parts[2]));
        double amount = Double.parseDouble(parts[3]);
        double balanceAfter = Double.parseDouble(parts[4]);
        LocalDateTime timestamp = LocalDateTime.parse(unescapeCsv(parts[5]), FORMATTER);
        String desc = unescapeCsv(parts[6]);

        return new Transaction(id, accNum, type, amount, balanceAfter, timestamp, desc);
    }

    private static String escapeCsv(String val) {
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }

    private static String unescapeCsv(String val) {
        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
            val = val.substring(1, val.length() - 1);
        }
        return val.replace("\"\"", "\"");
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %-12s | Amount: $%,10.2f | Balance: $%,10.2f | %s",
                timestamp.format(FORMATTER), id, type.getDisplayName(), amount, balanceAfter, description);
    }
}
