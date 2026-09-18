package com.bank;

import com.bank.exception.BankingException;
import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidPinException;
import com.bank.exception.OverdraftExceededException;
import com.bank.model.Account;
import com.bank.model.CheckingAccount;
import com.bank.model.SavingsAccount;
import com.bank.model.Transaction;
import com.bank.repository.FileAccountRepository;
import com.bank.service.BankService;

import java.io.File;
import java.util.List;

/**
 * Automated Verification and Test Suite for the Banking Simulation System.
 * Can be run to guarantee system correctness without requiring third-party testing frameworks.
 */
public class TestRunner {
    private static int testsPassed = 0;
    private static int testsTotal = 0;

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("                 BANKING SYSTEM AUTOMATED TEST & VERIFICATION                   ");
        System.out.println("================================================================================");

        // Use a temporary test directory for isolation
        String testDataDir = "data_test";
        cleanDir(new File(testDataDir));

        try {
            FileAccountRepository repo = new FileAccountRepository(testDataDir);
            BankService service = new BankService(repo);

            testAccountCreation(service);
            testDeposit(service);
            testSavingsMinimumBalance(service);
            testCheckingOverdraft(service);
            testCheckingOverdraftLimitExceeded(service);
            testAtomicTransfer(service);
            testTransferRollbackOnInvalidTarget(service);
            testPinAuthentication(service);
            testInterestAccrual(service);
            testPersistence(testDataDir);

            System.out.println("================================================================================");
            System.out.printf("RESULTS: %d / %d tests passed successfully!\n", testsPassed, testsTotal);
            if (testsPassed == testsTotal) {
                System.out.println(">>> ALL TESTS PASSED! System is fully verified and stable. <<<");
            } else {
                System.err.println(">>> SOME TESTS FAILED! Check output above. <<<");
            }
            System.out.println("================================================================================");
        } finally {
            cleanDir(new File(testDataDir));
        }
    }

    private static void assertTrue(String testName, boolean condition, String details) {
        testsTotal++;
        if (condition) {
            testsPassed++;
            System.out.println(" [PASS] " + testName);
        } else {
            System.err.println(" [FAIL] " + testName + " - " + details);
        }
    }

    private static void testAccountCreation(BankService service) {
        try {
            SavingsAccount sa = service.openSavingsAccount("Alice Smith", "alice@test.com", "555-01", "1234", 250.0);
            CheckingAccount ca = service.openCheckingAccount("Bob Jones", "bob@test.com", "555-02", "5678", 500.0);

            assertTrue("Savings account creation", sa != null && sa.getBalance() == 250.0, "Expected balance 250.0");
            assertTrue("Checking account creation", ca != null && ca.getBalance() == 500.0, "Expected balance 500.0");
        } catch (Exception e) {
            assertTrue("Account creation failed with exception: " + e.getMessage(), false, e.getMessage());
        }
    }

    private static void testDeposit(BankService service) {
        try {
            SavingsAccount sa = service.openSavingsAccount("Charlie", "c@test.com", "555-03", "1111", 200.0);
            service.deposit(sa.getAccountNumber(), 150.0, "Bonus");
            Account refreshed = service.getAccount(sa.getAccountNumber());
            assertTrue("Deposit increases balance", Math.abs(refreshed.getBalance() - 350.0) < 0.001, "Expected 350.0, got " + refreshed.getBalance());
        } catch (Exception e) {
            assertTrue("Deposit exception", false, e.getMessage());
        }
    }

    private static void testSavingsMinimumBalance(BankService service) {
        try {
            SavingsAccount sa = service.openSavingsAccount("Diana", "d@test.com", "555-04", "2222", 150.0);
            // Minimum balance is $100. Withdrawing $60 would drop balance to $90 (< $100) -> should fail
            try {
                service.withdraw(sa.getAccountNumber(), "2222", 60.0, "Try withdraw below min");
                assertTrue("Savings min balance enforcement", false, "Should have thrown InsufficientFundsException");
            } catch (InsufficientFundsException e) {
                assertTrue("Savings min balance enforcement", true, "");
            }
        } catch (Exception e) {
            assertTrue("Savings test exception", false, e.getMessage());
        }
    }

    private static void testCheckingOverdraft(BankService service) {
        try {
            CheckingAccount ca = service.openCheckingAccount("Evan", "e@test.com", "555-05", "3333", 100.0);
            // Overdraft limit is $500, fee is $15.
            // Balance = 100. Withdraw 150 -> balance drops to -50.
            // Crossing into overdraft incurs $15 fee -> balance becomes -65.
            service.withdraw(ca.getAccountNumber(), "3333", 150.0, "Overdraft withdraw");
            Account refreshed = service.getAccount(ca.getAccountNumber());
            assertTrue("Checking overdraft with fee", Math.abs(refreshed.getBalance() - (-65.0)) < 0.001, "Expected -65.0, got " + refreshed.getBalance());
        } catch (Exception e) {
            assertTrue("Checking overdraft exception", false, e.getMessage());
        }
    }

    private static void testCheckingOverdraftLimitExceeded(BankService service) {
        try {
            CheckingAccount ca = service.openCheckingAccount("Fiona", "f@test.com", "555-06", "4444", 50.0);
            // Balance = 50. Overdraft limit = 500. Total available = 550.
            // Withdrawing 600 should exceed overdraft limit!
            try {
                service.withdraw(ca.getAccountNumber(), "4444", 600.0, "Excessive withdraw");
                assertTrue("Checking overdraft cap exceeded", false, "Should have thrown OverdraftExceededException");
            } catch (OverdraftExceededException e) {
                assertTrue("Checking overdraft cap exceeded", true, "");
            }
        } catch (Exception e) {
            assertTrue("Overdraft limit exception", false, e.getMessage());
        }
    }

    private static void testAtomicTransfer(BankService service) {
        try {
            SavingsAccount sender = service.openSavingsAccount("George", "g@test.com", "555-07", "7777", 1000.0);
            CheckingAccount receiver = service.openCheckingAccount("Hannah", "h@test.com", "555-08", "8888", 200.0);

            service.transfer(sender.getAccountNumber(), "7777", receiver.getAccountNumber(), 400.0, "Gift");

            Account senderPost = service.getAccount(sender.getAccountNumber());
            Account receiverPost = service.getAccount(receiver.getAccountNumber());

            boolean senderOk = Math.abs(senderPost.getBalance() - 600.0) < 0.001;
            boolean receiverOk = Math.abs(receiverPost.getBalance() - 600.0) < 0.001;

            assertTrue("Atomic transfer updates both accounts", senderOk && receiverOk,
                    "Sender: " + senderPost.getBalance() + ", Receiver: " + receiverPost.getBalance());
        } catch (Exception e) {
            assertTrue("Atomic transfer exception", false, e.getMessage());
        }
    }

    private static void testTransferRollbackOnInvalidTarget(BankService service) {
        try {
            SavingsAccount sender = service.openSavingsAccount("Ian", "i@test.com", "555-09", "9999", 500.0);
            try {
                service.transfer(sender.getAccountNumber(), "9999", "ACC-NONEXISTENT", 200.0, "Fail transfer");
                assertTrue("Transfer rollback check", false, "Should have failed on invalid account");
            } catch (BankingException e) {
                Account senderAfter = service.getAccount(sender.getAccountNumber());
                assertTrue("Transfer rollback restores sender balance", Math.abs(senderAfter.getBalance() - 500.0) < 0.001,
                        "Expected balance 500.0, got " + senderAfter.getBalance());
            }
        } catch (Exception e) {
            assertTrue("Transfer rollback exception", false, e.getMessage());
        }
    }

    private static void testPinAuthentication(BankService service) {
        try {
            SavingsAccount sa = service.openSavingsAccount("Julia", "j@test.com", "555-10", "1234", 300.0);
            try {
                service.authenticate(sa.getAccountNumber(), "9999");
                assertTrue("Invalid PIN rejection", false, "Should have thrown InvalidPinException");
            } catch (InvalidPinException e) {
                assertTrue("Invalid PIN rejection", true, "");
            }
        } catch (Exception e) {
            assertTrue("PIN authentication exception", false, e.getMessage());
        }
    }

    private static void testInterestAccrual(BankService service) {
        try {
            SavingsAccount sa = service.openSavingsAccount("Kevin", "k@test.com", "555-11", "5555", 10000.0);
            // 4.5% annual on 10,000 is 450/year => 37.50/month
            double interest = sa.applyMonthlyInterest();
            assertTrue("Interest accrual calculation", Math.abs(interest - 37.50) < 0.01, "Expected 37.50, got " + interest);
        } catch (Exception e) {
            assertTrue("Interest calculation exception", false, e.getMessage());
        }
    }

    private static void testPersistence(String testDir) {
        try {
            // First instance
            FileAccountRepository repo1 = new FileAccountRepository(testDir);
            BankService service1 = new BankService(repo1);
            SavingsAccount sa = service1.openSavingsAccount("Laura", "l@test.com", "555-12", "6666", 750.0);
            service1.deposit(sa.getAccountNumber(), 250.0, "Add funds");

            // Re-instantiate repository from same directory to test reload from disk
            FileAccountRepository repo2 = new FileAccountRepository(testDir);
            BankService service2 = new BankService(repo2);
            Account reloaded = service2.getAccount(sa.getAccountNumber());

            boolean balancePersisted = Math.abs(reloaded.getBalance() - 1000.0) < 0.001;
            List<Transaction> txs = reloaded.getTransactions();
            boolean txsPersisted = txs.size() == 2; // Initial + Deposit

            assertTrue("Persistence of account balance and transactions", balancePersisted && txsPersisted,
                    "Balance: " + reloaded.getBalance() + ", Tx Count: " + txs.size());
        } catch (Exception e) {
            assertTrue("Persistence test exception", false, e.getMessage());
        }
    }

    private static void cleanDir(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            dir.delete();
        }
    }
}
