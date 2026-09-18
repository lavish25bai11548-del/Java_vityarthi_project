package com.bank.repository;

import com.bank.model.Account;
import com.bank.model.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Data access abstraction for accounts and transaction ledgers.
 */
public interface AccountRepository {
    void save(Account account);
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findAll();
    void saveTransaction(Transaction transaction);
    List<Transaction> findTransactionsByAccount(String accountNumber);
    String generateNextAccountNumber();
    void persistAll();
}
