package com.bank;

import com.bank.exception.BankingException;
import com.bank.model.Account;
import com.bank.model.CheckingAccount;
import com.bank.model.SavingsAccount;
import com.bank.model.Transaction;
import com.bank.repository.FileAccountRepository;
import com.bank.service.BankService;

import java.util.List;
import java.util.Scanner;

/**
 * Main application entry point for the Banking & Transaction Simulation System.
 * Features an interactive, robust console interface designed for academic demonstrations.
 */
public class Main {
    private static final String ADMIN_PASSCODE = "admin123";
    private static final BankService bankService = new BankService(new FileAccountRepository("data"));
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        printBanner();

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = promptInt("Select an option (1-5): ", 1, 5);
            System.out.println();

            switch (choice) {
                case 1 -> handleOpenAccount();
                case 2 -> handleCustomerPortal();
                case 3 -> handleAdminPortal();
                case 4 -> handleSeedDemoData();
                case 5 -> {
                    System.out.println("Thank you for using the Banking Simulation System. Goodbye!");
                    running = false;
                }
            }
            if (running) {
                System.out.println("\nPress [ENTER] to return to the main menu...");
                scanner.nextLine();
            }
        }
    }

    private static void printBanner() {
        System.out.println("================================================================================");
        System.out.println("               BANKING & TRANSACTION SIMULATION SYSTEM (JAVA)                  ");
        System.out.println("                  Object-Oriented Academic Project Suite                        ");
        System.out.println("================================================================================");
    }

    private static void printMainMenu() {
        System.out.println("\n============================= MAIN MENU =============================");
        System.out.println(" [1] Open New Account (Savings / Checking)");
        System.out.println(" [2] Customer Login & Transactions (Deposit, Withdraw, Transfer, Statement)");
        System.out.println(" [3] Bank Manager / Admin Portal (Auditing, Reserves, Interest)");
        System.out.println(" [4] Seed Quick Demo Accounts (For Viva / Demo Presentation)");
        System.out.println(" [5] Exit System");
        System.out.println("=====================================================================");
    }

    // =========================================================================
    // 1. OPEN ACCOUNT
    // =========================================================================
    private static void handleOpenAccount() {
        System.out.println("------------------------- OPEN NEW ACCOUNT -------------------------");
        System.out.println("Select Account Type:");
        System.out.println("  1. Savings Account (4.5% annual interest, Min Balance: $100.00)");
        System.out.println("  2. Checking Account ($500.00 Overdraft facility, $15.00 Overdraft Fee)");
        int typeChoice = promptInt("Enter choice (1-2): ", 1, 2);

        String name = promptNonEmptyString("Full Customer Name: ");
        String email = promptString("Email Address: ");
        String phone = promptString("Phone Number: ");
        String pin = promptPin("Set 4-Digit Security PIN: ");

        try {
            if (typeChoice == 1) {
                double deposit = promptDouble("Initial Deposit Amount ($ min 100.00): ", 100.0, 1_000_000.0);
                SavingsAccount sa = bankService.openSavingsAccount(name, email, phone, pin, deposit);
                System.out.println("\n SUCCESS! Savings Account opened successfully.");
                System.out.println(" Account Number : " + sa.getAccountNumber());
                System.out.println(" Account Holder : " + sa.getCustomer().getFullName());
                System.out.printf(" Opening Balance: $%,.2f\n", sa.getBalance());
                System.out.println(" Please memorize your Account Number and PIN.");
            } else {
                double deposit = promptDouble("Initial Deposit Amount ($ min 0.00): ", 0.0, 1_000_000.0);
                CheckingAccount ca = bankService.openCheckingAccount(name, email, phone, pin, deposit);
                System.out.println("\n SUCCESS! Checking Account opened successfully.");
                System.out.println(" Account Number  : " + ca.getAccountNumber());
                System.out.println(" Account Holder  : " + ca.getCustomer().getFullName());
                System.out.printf(" Opening Balance : $%,.2f\n", ca.getBalance());
                System.out.printf(" Overdraft Limit : $%,.2f\n", ca.getOverdraftLimit());
                System.out.println(" Please memorize your Account Number and PIN.");
            }
        } catch (BankingException e) {
            System.err.println("\n Account Opening Error: " + e.getMessage());
        }
    }

    // =========================================================================
    // 2. CUSTOMER PORTAL
    // =========================================================================
    private static void handleCustomerPortal() {
        System.out.println("------------------------- CUSTOMER LOGIN -------------------------");
        String accNum = promptNonEmptyString("Enter Account Number (e.g., ACC-1001): ").toUpperCase();
        String pin = promptPin("Enter 4-Digit PIN: ");

        Account account;
        try {
            account = bankService.authenticate(accNum, pin);
        } catch (BankingException e) {
            System.err.println("\n Login Failed: " + e.getMessage());
            return;
        }

        System.out.println("\n Welcome back, " + account.getCustomer().getFullName() + "!");

        boolean inPortal = true;
        while (inPortal) {
            System.out.println("\n----------------- ACCOUNT OPERATIONS (" + account.getAccountNumber() + ") -----------------");
            System.out.println(" [1] Check Balance & Account Details");
            System.out.println(" [2] Deposit Cash");
            System.out.println(" [3] Withdraw Cash");
            System.out.println(" [4] Transfer Funds to Another Account");
            System.out.println(" [5] View Transaction Statement");
            System.out.println(" [6] Logout & Return to Main Menu");
            System.out.println("------------------------------------------------------------------");
            int choice = promptInt("Select action (1-6): ", 1, 6);

            switch (choice) {
                case 1 -> {
                    System.out.println("\n--- Account Overview ---");
                    System.out.println("Type            : " + account.getAccountType());
                    System.out.println("Account Number  : " + account.getAccountNumber());
                    System.out.println("Holder Name     : " + account.getCustomer().getFullName());
                    System.out.printf("Current Balance : $%,.2f\n", account.getBalance());
                    if (account instanceof SavingsAccount sa) {
                        System.out.printf("Interest Rate   : %.2f%% p.a.\n", sa.getAnnualInterestRate());
                        System.out.printf("Min Balance     : $%,.2f\n", sa.getMinimumBalance());
                    } else if (account instanceof CheckingAccount ca) {
                        System.out.printf("Overdraft Limit : $%,.2f\n", ca.getOverdraftLimit());
                        System.out.printf("Available Funds : $%,.2f\n", ca.getAvailableFunds());
                    }
                    System.out.println("Account Status  : " + (account.isActive() ? "ACTIVE" : "FROZEN"));
                }
                case 2 -> {
                    double amount = promptDouble("Enter amount to deposit: $", 0.01, 1_000_000.0);
                    String desc = promptString("Optional notes (or press Enter): ");
                    try {
                        bankService.deposit(account.getAccountNumber(), amount, desc.isBlank() ? "ATM Deposit" : desc);
                        System.out.printf("\n Deposit of $%,.2f successful. New Balance: $%,.2f\n", amount, account.getBalance());
                    } catch (BankingException e) {
                        System.err.println("\n Deposit Failed: " + e.getMessage());
                    }
                }
                case 3 -> {
                    double amount = promptDouble("Enter amount to withdraw: $", 0.01, 1_000_000.0);
                    String desc = promptString("Optional notes (or press Enter): ");
                    try {
                        bankService.withdraw(account.getAccountNumber(), pin, amount, desc.isBlank() ? "ATM Withdrawal" : desc);
                        System.out.printf("\n Withdrawal of $%,.2f successful. New Balance: $%,.2f\n", amount, account.getBalance());
                    } catch (BankingException e) {
                        System.err.println("\n Withdrawal Failed: " + e.getMessage());
                    }
                }
                case 4 -> {
                    String targetAcc = promptNonEmptyString("Recipient Account Number: ").toUpperCase();
                    double amount = promptDouble("Transfer Amount: $", 0.01, 1_000_000.0);
                    String notes = promptString("Transfer Memo/Notes: ");
                    try {
                        bankService.transfer(account.getAccountNumber(), pin, targetAcc, amount, notes);
                        System.out.printf("\n Successfully transferred $%,.2f to %s!\n", amount, targetAcc);
                        System.out.printf(" Your New Balance: $%,.2f\n", account.getBalance());
                    } catch (BankingException e) {
                        System.err.println("\n Transfer Failed: " + e.getMessage());
                    }
                }
                case 5 -> {
                    System.out.println("\n======================= TRANSACTION STATEMENT =======================");
                    System.out.println("Account: " + account.getAccountNumber() + " | Holder: " + account.getCustomer().getFullName());
                    System.out.println("---------------------------------------------------------------------");
                    List<Transaction> txs = account.getTransactions();
                    if (txs.isEmpty()) {
                        System.out.println("No transactions found for this account.");
                    } else {
                        System.out.printf("%-19s | %-12s | %-12s | %10s | %10s | %s\n",
                                "Timestamp", "Txn ID", "Type", "Amount", "Balance", "Description");
                        System.out.println("---------------------------------------------------------------------------------------------------");
                        for (Transaction tx : txs) {
                            System.out.printf("%-19s | %-12s | %-12s | $%9.2f | $%9.2f | %s\n",
                                    tx.getTimestamp().format(Transaction.FORMATTER),
                                    tx.getId(),
                                    tx.getType().getDisplayName(),
                                    tx.getAmount(),
                                    tx.getBalanceAfter(),
                                    tx.getDescription());
                        }
                    }
                    System.out.println("=====================================================================");
                }
                case 6 -> {
                    System.out.println("Logging out...");
                    inPortal = false;
                }
            }
        }
    }

    // =========================================================================
    // 3. ADMIN / MANAGER PORTAL
    // =========================================================================
    private static void handleAdminPortal() {
        System.out.println("------------------------- MANAGER AUTHENTICATION -------------------------");
        String pass = promptString("Enter Manager Admin Passcode: ");
        if (!ADMIN_PASSCODE.equals(pass)) {
            System.err.println(" Access Denied: Invalid passcode.");
            return;
        }

        boolean inAdmin = true;
        while (inAdmin) {
            System.out.println("\n======================== BANK MANAGER PORTAL ========================");
            System.out.println(" [1] View All Accounts Ledger");
            System.out.println(" [2] Bank Total Liquidity & Reserve Summary");
            System.out.println(" [3] Run Monthly Interest Accrual (Savings Accounts)");
            System.out.println(" [4] Freeze / Unfreeze Customer Account");
            System.out.println(" [5] Return to Main Menu");
            System.out.println("=====================================================================");
            int choice = promptInt("Select Manager Action (1-5): ", 1, 5);

            switch (choice) {
                case 1 -> {
                    List<Account> list = bankService.getAllAccounts();
                    System.out.println("\n------------------------- ALL REGISTERED ACCOUNTS -------------------------");
                    if (list.isEmpty()) {
                        System.out.println("No accounts currently on file.");
                    } else {
                        System.out.printf("%-10s | %-9s | %-20s | %11s | %-8s | %s\n",
                                "Acc Number", "Type", "Customer Name", "Balance", "Status", "Email");
                        System.out.println("----------------------------------------------------------------------------------");
                        for (Account a : list) {
                            System.out.printf("%-10s | %-9s | %-20s | $%10.2f | %-8s | %s\n",
                                    a.getAccountNumber(),
                                    a.getAccountType(),
                                    a.getCustomer().getFullName(),
                                    a.getBalance(),
                                    a.isActive() ? "ACTIVE" : "FROZEN",
                                    a.getCustomer().getEmail());
                        }
                    }
                }
                case 2 -> {
                    List<Account> list = bankService.getAllAccounts();
                    double total = bankService.getTotalBankReserves();
                    long activeCount = list.stream().filter(Account::isActive).count();
                    long frozenCount = list.size() - activeCount;

                    System.out.println("\n------------------- BANK LIQUIDITY REPORT -------------------");
                    System.out.printf("Total Accounts Registered : %d (Active: %d, Frozen: %d)\n", list.size(), activeCount, frozenCount);
                    System.out.printf("Total Net Bank Reserves   : $%,.2f\n", total);
                    System.out.println("-------------------------------------------------------------");
                }
                case 3 -> {
                    System.out.println("\nExecuting end-of-month interest calculation for all active savings accounts...");
                    double credited = bankService.applyMonthlyInterestToAllSavings();
                    System.out.printf(" Completed! Total interest distributed across savings accounts: $%,.2f\n", credited);
                }
                case 4 -> {
                    String target = promptNonEmptyString("Enter Account Number to Toggle Status: ").toUpperCase();
                    try {
                        Account acc = bankService.getAccount(target);
                        boolean current = acc.isActive();
                        bankService.setAccountActive(target, !current);
                        System.out.printf(" Account %s status changed to: %s\n", target, (!current ? "ACTIVE" : "FROZEN"));
                    } catch (BankingException e) {
                        System.err.println(" Error: " + e.getMessage());
                    }
                }
                case 5 -> inAdmin = false;
            }
        }
    }

    // =========================================================================
    // 4. QUICK DEMO SEEDING
    // =========================================================================
    private static void handleSeedDemoData() {
        System.out.println("\nSeeding demo accounts for viva presentation...");
        try {
            SavingsAccount sa = bankService.openSavingsAccount(
                    "Alice Johnson", "alice@example.com", "+1-555-0101", "1234", 1500.00
            );
            CheckingAccount ca = bankService.openCheckingAccount(
                    "Bob Smith", "bob@example.com", "+1-555-0202", "4321", 800.00
            );

            // Execute sample transactions
            bankService.deposit(sa.getAccountNumber(), 500.00, "Salary Bonus Credit");
            bankService.transfer(sa.getAccountNumber(), "1234", ca.getAccountNumber(), 300.00, "Dinner split");

            System.out.println(" Demo accounts seeded successfully!");
            System.out.println("  1. Savings Account  : " + sa.getAccountNumber() + " (PIN: 1234, Holder: Alice Johnson)");
            System.out.println("  2. Checking Account : " + ca.getAccountNumber() + " (PIN: 4321, Holder: Bob Smith)");
            System.out.println("  Manager Passcode    : admin123");
        } catch (BankingException e) {
            System.out.println(" Note: " + e.getMessage());
        }
    }

    // =========================================================================
    // HELPER INPUT UTILITIES
    // =========================================================================
    private static int promptInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(input);
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.printf(" Please enter a number between %d and %d.\n", min, max);
            } catch (NumberFormatException e) {
                System.out.println(" Invalid input. Please enter a valid integer.");
            }
        }
    }

    private static double promptDouble(String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                double val = Double.parseDouble(input);
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.printf(" Amount must be between $%.2f and $%.2f.\n", min, max);
            } catch (NumberFormatException e) {
                System.out.println(" Invalid input. Please enter a valid numeric dollar amount.");
            }
        }
    }

    private static String promptString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static String promptNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println(" Input cannot be blank.");
        }
    }

    private static String promptPin(String prompt) {
        while (true) {
            System.out.print(prompt);
            String pin = scanner.nextLine().trim();
            if (pin.matches("\\d{4}")) {
                return pin;
            }
            System.out.println(" PIN must consist of exactly 4 digits (e.g., 1234).");
        }
    }
}
