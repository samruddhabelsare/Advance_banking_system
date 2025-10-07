package banking_system_console;

import java.io.*;
import java.nio.file.*;
import java.text.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/*
 Owner  : Samruddha Belsare
 Github : https://github.com/samruddhabelsare

 File-based, PIN-protected advanced console banking system.
*/

public class BankingSystemAdvanced {
    // data folder paths
    private static final String DATA_DIR = "data";
    private static final String ACCOUNTS_FILE = DATA_DIR + "/accounts.txt";
    private static final String TRANSACTIONS_DIR = DATA_DIR + "/transactions";
    private static final String SCHEDULED_FILE = DATA_DIR + "/scheduled.txt";
    private static final String AUDIT_LOG = DATA_DIR + "/audit.log";
    private static final String RECEIPTS_DIR = DATA_DIR + "/receipts";
    private static final String BACKUP_DIR = DATA_DIR + "/backup";

    private static final Scanner sc = new Scanner(System.in);
    private static AccountManager accountManager;
    private static AdminManager adminManager;
    private static ScheduledManager scheduledManager;
    private static AuditLogger audit;

    public static void main(String[] args) {
        try {
            initStorage();
            audit = new AuditLogger(AUDIT_LOG);
            accountManager = new AccountManager(ACCOUNTS_FILE, TRANSACTIONS_DIR);
            adminManager = new AdminManager(accountManager, audit);
            scheduledManager = new ScheduledManager(SCHEDULED_FILE, accountManager, audit);

            // run any due scheduled transactions at startup
            scheduledManager.applyDueScheduledTransactions();

            mainMenu();
        } catch (Exception e) {
            System.out.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void initStorage() throws IOException {
        // Create required directories if they don't exist
        Files.createDirectories(Paths.get(DATA_DIR));
        Files.createDirectories(Paths.get(TRANSACTIONS_DIR));
        Files.createDirectories(Paths.get(RECEIPTS_DIR));
        Files.createDirectories(Paths.get(BACKUP_DIR));

        // Safely create main data files only if missing
        Path accountsPath = Paths.get(ACCOUNTS_FILE);
        if (!Files.exists(accountsPath)) {
            Files.createFile(accountsPath);
        }

        Path scheduledPath = Paths.get(SCHEDULED_FILE);
        if (!Files.exists(scheduledPath)) {
            Files.createFile(scheduledPath);
        }

        Path auditPath = Paths.get(AUDIT_LOG);
        if (!Files.exists(auditPath)) {
            Files.createFile(auditPath);
        }
    }


    private static void mainMenu() {
        while (true) {
            System.out.println("\n=== Banking System ===");
            System.out.println("1. User Login");
            System.out.println("2. Create Account");
            System.out.println("3. Admin Login");
            System.out.println("4. Backup Data");
            System.out.println("5. Restore Data");
            System.out.println("6. Exit");
            System.out.print("Choose: ");
            int ch = readInt(1, 6);
            switch (ch) {
                case 1:
                    userLogin();
                    break;
                case 2:
                    createAccountFlow();
                    break;
                case 3:
                    adminManager.adminLoginFlow();
                    break;
                case 4:
                    backupData();
                    break;
                case 5:
                    restoreData();
                    break;
                case 6:
                    System.out.println("Exiting. Goodbye.");
                    audit.log("SYSTEM_EXIT", "System exited by user.");
                    return;
                default:
                    break;
            }
        }
    }

    // ---------------------------
    // User flows
    // ---------------------------
    private static void userLogin() {
        System.out.print("Enter account number: ");
        int accNo = readInt();
        Account acc = accountManager.findAccount(accNo);
        if (acc == null) {
            System.out.println("Account not found or inactive.");
            return;
        }
        if (acc.isLocked()) {
            System.out.println("Account is locked due to failed login attempts. Contact admin.");
            return;
        }
        System.out.print("Enter 4-digit PIN: ");
        String pin = sc.next().trim();
        if (!accountManager.verifyPin(accNo, pin)) {
            System.out.println("Invalid PIN.");
            audit.log("LOGIN_FAIL", "Account " + accNo + " failed login attempt.");
            if (accountManager.registerFailedLogin(accNo)) {
                System.out.println("Too many failed attempts. Account locked.");
                audit.log("ACCOUNT_LOCK", "Account " + accNo + " locked due to failed attempts.");
            }
            return;
        }
        // success, reset failed attempts
        accountManager.resetFailedLogin(accNo);
        System.out.println("Login successful. Applying pending interest and scheduled transactions...");
        accountManager.applyInterestIfDue(accNo); // automatic interest
        scheduledManager.applyDueScheduledTransactionsForAccount(accNo);
        audit.log("LOGIN_SUCCESS", "Account " + accNo + " login.");

        // session
        userSession(accNo);
    }

    private static void userSession(int accNo) {
        Account acc = accountManager.findAccount(accNo);
        long sessionStart = System.currentTimeMillis();
        long lastActivity = sessionStart;
        final long SESSION_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes inactivity

        boolean active = true;
        while (active) {
            if (System.currentTimeMillis() - lastActivity > SESSION_TIMEOUT_MS) {
                System.out.println("Session timed out due to inactivity.");
                audit.log("SESSION_TIMEOUT", "Account " + accNo + " session timed out.");
                return;
            }

            System.out.println("\n--- User Menu (Acc: " + accNo + ") ---");
            System.out.println("1. View Balance / Apply Interest");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Mini Statement (last 5)");
            System.out.println("6. Full Transaction History (filters)");
            System.out.println("7. Schedule Transaction");
            System.out.println("8. Reverse Last Transaction");
            System.out.println("9. Change PIN");
            System.out.println("10. Logout");
            System.out.print("Choose: ");
            int ch = readInt(1, 10);
            lastActivity = System.currentTimeMillis();
            switch (ch) {
                case 1:
                    accountManager.applyInterestIfDue(accNo);
                    acc = accountManager.findAccount(accNo); // reload
                    System.out.println("Account No: " + acc.getAccountNo());
                    System.out.println("Name      : " + acc.getHolderName());
                    System.out.println("Type      : " + acc.getType());
                    System.out.println("Balance   : " + formatCurrency(acc.getBalance()));
                    accountManager.checkAlerts(acc);
                    audit.log("VIEW_BALANCE", "Account " + accNo + " viewed balance.");
                    break;
                case 2:
                    depositFlow(accNo);
                    break;
                case 3:
                    withdrawFlow(accNo);
                    break;
                case 4:
                    transferFlow(accNo);
                    break;
                case 5:
                    accountManager.printMiniStatement(accNo, 5);
                    break;
                case 6:
                    transactionHistoryFlow(accNo);
                    break;
                case 7:
                    scheduleTransactionFlow(accNo);
                    break;
                case 8:
                    accountManager.reverseLastTransaction(accNo);
                    break;
                case 9:
                    changePinFlow(accNo);
                    break;
                case 10:
                    System.out.println("Logging out.");
                    audit.log("LOGOUT", "Account " + accNo + " logged out.");
                    active = false;
                    break;
                default:
                    break;
            }
        }
    }

    // ---------------------------
    // Actions
    // ---------------------------
    private static void createAccountFlow() {
        System.out.println("\n--- Create New Account ---");
        sc.nextLine(); // flush newline
        System.out.print("Holder Name: ");
        String name = readNonEmptyLine();
        System.out.print("Account Type (Savings/Checking/Fixed/Business): ");
        String type = readAccountType();
        System.out.print("Initial Deposit: ");
        double initial = readDoubleMin(0);
        System.out.print("Set 4-digit PIN for account: ");
        String pin = readPin();

        Account acc = accountManager.createAccount(name, initial, type, pin);
        System.out.println("Account created with Account No: " + acc.getAccountNo());
        audit.log("ACCOUNT_CREATE", "Account " + acc.getAccountNo() + " created by user action.");
    }

    private static void depositFlow(int accNo) {
        System.out.print("Enter amount to deposit: ");
        double amt = readDoubleMin(0.01);
        if (accountManager.deposit(accNo, amt)) {
            System.out.println("Deposited " + formatCurrency(amt));
            accountManager.printBalance(accNo);
            accountManager.saveTransactionReceipt(accNo, "DEPOSIT", amt, "Deposit to account");
            audit.log("DEPOSIT", "Acc " + accNo + " deposit " + amt);
        } else {
            System.out.println("Deposit failed.");
        }
    }

    private static void withdrawFlow(int accNo) {
        Account acc = accountManager.findAccount(accNo);
        System.out.print("Enter amount to withdraw: ");
        double amt = readDoubleMin(0.01);
        if (accountManager.isOverDailyLimit(accNo, amt)) {
            System.out.println("Amount exceeds daily withdrawal limit.");
            return;
        }
        System.out.print("Enter PIN to confirm: ");
        String pin = sc.next().trim();
        if (!accountManager.verifyPin(accNo, pin)) {
            System.out.println("Invalid PIN.");
            audit.log("WITHDRAW_PIN_FAIL", "Acc " + accNo + " withdraw PIN fail.");
            return;
        }
        if (accountManager.withdraw(accNo, amt)) {
            System.out.println("Withdrawn " + formatCurrency(amt));
            accountManager.printBalance(accNo);
            accountManager.saveTransactionReceipt(accNo, "WITHDRAW", amt, "Withdrawal from account");
            audit.log("WITHDRAW", "Acc " + accNo + " withdrawal " + amt);
        } else {
            System.out.println("Insufficient balance or invalid amount.");
        }
    }

    private static void transferFlow(int fromAccNo) {
        System.out.print("Enter destination account number: ");
        int toAccNo = readInt();
        if (fromAccNo == toAccNo) {
            System.out.println("Cannot transfer to same account.");
            return;
        }
        Account dest = accountManager.findAccount(toAccNo);
        if (dest == null) {
            System.out.println("Destination account not found.");
            return;
        }
        System.out.print("Enter amount to transfer: ");
        double amt = readDoubleMin(0.01);
        if (accountManager.isOverDailyLimit(fromAccNo, amt)) {
            System.out.println("Amount exceeds daily transfer/withdrawal limit.");
            return;
        }
        System.out.print("Enter PIN to confirm: ");
        String pin = sc.next().trim();
        if (!accountManager.verifyPin(fromAccNo, pin)) {
            System.out.println("Invalid PIN.");
            return;
        }
        if (accountManager.transfer(fromAccNo, toAccNo, amt)) {
            System.out.println("Transferred " + formatCurrency(amt) + " to account " + toAccNo);
            accountManager.saveTransactionReceipt(fromAccNo, "TRANSFER_OUT", amt, "Transfer to " + toAccNo);
            accountManager.saveTransactionReceipt(toAccNo, "TRANSFER_IN", amt, "Transfer from " + fromAccNo);
            audit.log("TRANSFER", "Acc " + fromAccNo + " -> " + toAccNo + " : " + amt);
        } else {
            System.out.println("Transfer failed.");
        }
    }

    private static void transactionHistoryFlow(int accNo) {
        System.out.println("\nTransaction History Filters:");
        System.out.println("1. All");
        System.out.println("2. Date range");
        System.out.println("3. Type (DEPOSIT/WITHDRAW/TRANSFER)");
        System.out.println("4. Amount range");
        System.out.print("Choose: ");
        int ch = readInt(1, 4);
        switch (ch) {
            case 1:
                accountManager.printTransactionHistory(accNo, null, null, null, null);
                break;
            case 2:
                System.out.print("From (YYYY-MM-DD): ");
                LocalDate from = readDate();
                System.out.print("To   (YYYY-MM-DD): ");
                LocalDate to = readDate();
                accountManager.printTransactionHistory(accNo, from, to, null, null);
                break;
            case 3:
                System.out.print("Type (DEPOSIT/WITHDRAW/TRANSFER_IN/TRANSFER_OUT): ");
                String type = sc.next().trim().toUpperCase();
                accountManager.printTransactionHistory(accNo, null, null, type, null);
                break;
            case 4:
                System.out.print("Min amount: ");
                double min = readDoubleMin(0);
                System.out.print("Max amount: ");
                double max = readDoubleMin(min);
                accountManager.printTransactionHistory(accNo, null, null, null, new double[]{min, max});
                break;
            default:
                break;
        }
    }

    private static void scheduleTransactionFlow(int accNo) {
        System.out.println("\nSchedule a transaction:");
        System.out.println("1. Scheduled Transfer");
        System.out.println("2. Scheduled Withdrawal");
        System.out.print("Choose: ");
        int ch = readInt(1, 2);
        System.out.print("Enter amount: ");
        double amt = readDoubleMin(0.01);
        System.out.print("Enter date (YYYY-MM-DD): ");
        LocalDate date = readDate();
        String memo = "";
        int targetAcc = -1;
        if (ch == 1) {
            System.out.print("Destination account for transfer: ");
            targetAcc = readInt();
            if (accountManager.findAccount(targetAcc) == null) {
                System.out.println("Destination account not found.");
                return;
            }
            memo = "Scheduled transfer to " + targetAcc;
        } else {
            memo = "Scheduled withdrawal";
        }
        ScheduledTransaction s = new ScheduledTransaction(accNo, targetAcc, amt, date, ch == 1 ? "TRANSFER" : "WITHDRAW", memo);
        scheduledManager.addScheduledTransaction(s);
        System.out.println("Transaction scheduled on " + date.toString());
        audit.log("SCHEDULE_ADD", "Scheduled transaction for acc " + accNo + " on " + date);
    }

    private static void changePinFlow(int accNo) {
        System.out.print("Enter current PIN: ");
        String cur = sc.next().trim();
        if (!accountManager.verifyPin(accNo, cur)) {
            System.out.println("Invalid current PIN.");
            return;
        }
        System.out.print("Enter new 4-digit PIN: ");
        String np = readPin();
        accountManager.changePin(accNo, np);
        System.out.println("PIN changed.");
        audit.log("PIN_CHANGE", "Account " + accNo + " changed PIN.");
    }

    // ---------------------------
    // Admin helpers
    // ---------------------------
    private static void backupData() {
        try {
            String ts = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
            Path backup = Paths.get(BACKUP_DIR, "backup_" + ts);
            Files.createDirectories(backup);
            // copy files
            copyFile(Paths.get(ACCOUNTS_FILE), backup.resolve("accounts.txt"));
            copyDirectory(Paths.get(TRANSACTIONS_DIR), backup.resolve("transactions"));
            copyFile(Paths.get(SCHEDULED_FILE), backup.resolve("scheduled.txt"));
            copyFile(Paths.get(AUDIT_LOG), backup.resolve("audit.log"));
            System.out.println("Backup created at " + backup.toString());
            audit.log("BACKUP", "Backup created " + backup.toString());
        } catch (Exception e) {
            System.out.println("Backup failed: " + e.getMessage());
        }
    }

    private static void restoreData() {
        System.out.println("Restoration will overwrite current data. Continue? (yes/no)");
        String ans = sc.next().trim().toLowerCase();
        if (!ans.equals("yes")) {
            System.out.println("Restore cancelled.");
            return;
        }
        // list backups
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get(BACKUP_DIR))) {
            List<Path> backups = new ArrayList<>();
            for (Path p : ds) backups.add(p);
            if (backups.isEmpty()) {
                System.out.println("No backups available.");
                return;
            }
            System.out.println("Available backups:");
            for (int i = 0; i < backups.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, backups.get(i).getFileName());
            }
            System.out.print("Choose backup to restore: ");
            int idx = readInt(1, backups.size());
            Path chosen = backups.get(idx - 1);
            // copy back
            copyFile(chosen.resolve("accounts.txt"), Paths.get(ACCOUNTS_FILE));
            copyDirectory(chosen.resolve("transactions"), Paths.get(TRANSACTIONS_DIR));
            copyFile(chosen.resolve("scheduled.txt"), Paths.get(SCHEDULED_FILE));
            copyFile(chosen.resolve("audit.log"), Paths.get(AUDIT_LOG));
            System.out.println("Restore complete. Please restart the program.");
            audit.log("RESTORE", "Data restored from " + chosen.toString());
            System.exit(0);
        } catch (Exception e) {
            System.out.println("Restore failed: " + e.getMessage());
        }
    }

    // ---------------------------
    // Utilities & Readers
    // ---------------------------
    private static int readInt() {
        while (true) {
            try {
                return Integer.parseInt(sc.next().trim());
            } catch (Exception e) {
                System.out.print("Invalid number, try again: ");
            }
        }
    }

    private static int readInt(int min, int max) {
        while (true) {
            int v = readInt();
            if (v >= min && v <= max) return v;
            System.out.print("Enter number between " + min + " and " + max + ": ");
        }
    }

    private static double readDoubleMin(double min) {
        while (true) {
            try {
                double d = Double.parseDouble(sc.next().trim());
                if (d >= min) return d;
            } catch (Exception e) {}
            System.out.print("Enter a number >= " + min + ": ");
        }
    }

    private static String readNonEmptyLine() {
        while (true) {
            String s = sc.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.print("Cannot be empty. Try again: ");
        }
    }

    private static String readAccountType() {
        while (true) {
            String t = sc.next().trim();
            if (t.equalsIgnoreCase("Savings") || t.equalsIgnoreCase("Checking") ||
                    t.equalsIgnoreCase("Fixed") || t.equalsIgnoreCase("Business")) {
                return capitalize(t);
            }
            System.out.print("Type must be Savings/Checking/Fixed/Business: ");
        }
    }

    private static String readPin() {
        while (true) {
            String p = sc.next().trim();
            if (p.matches("\\d{4}")) return p;
            System.out.print("PIN must be exactly 4 digits. Try again: ");
        }
    }

    private static LocalDate readDate() {
        while (true) {
            String s = sc.next().trim();
            try {
                return LocalDate.parse(s);
            } catch (Exception e) {
                System.out.print("Invalid date format. Use YYYY-MM-DD: ");
            }
        }
    }

    private static String formatCurrency(double v) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.getDefault());
        return nf.format(v);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0,1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private static void copyFile(Path src, Path dst) throws IOException {
        if (!Files.exists(src)) return;
        Files.createDirectories(dst.getParent());
        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void copyDirectory(Path src, Path dst) throws IOException {
        if (!Files.exists(src)) return;
        Files.createDirectories(dst);
        Files.walk(src).forEach(p -> {
            try {
                Path rel = src.relativize(p);
                Path targ = dst.resolve(rel);
                if (Files.isDirectory(p)) {
                    Files.createDirectories(targ);
                } else {
                    Files.copy(p, targ, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {}
        });
    }

    // ---------------------------
    // AccountManager - main logic & persistence
    // ---------------------------
    static class AccountManager {
        private final String accountsFile;
        private final String txDir;
        private Map<Integer, Account> accounts = new HashMap<>();
        private int nextAccountNo = 1001;
        private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        public AccountManager(String accountsFile, String txDir) throws IOException {
            this.accountsFile = accountsFile;
            this.txDir = txDir;
            loadAccounts();
        }

        private synchronized void loadAccounts() throws IOException {
            accounts.clear();
            Path p = Paths.get(accountsFile);
            if (!Files.exists(p)) {
                Files.createFile(p);
                return;
            }
            List<String> lines = Files.readAllLines(p);
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                // format: accNo|name|type|balance|active|pin|lastInterest|failedAttempts|locked|dailyLimit|lastDailyReset
                String[] parts = line.split("\\|", -1);
                int accNo = Integer.parseInt(parts[0]);
                String name = parts[1];
                String type = parts[2];
                double bal = Double.parseDouble(parts[3]);
                boolean active = Boolean.parseBoolean(parts[4]);
                String pinHash = parts[5];
                LocalDate lastInterest = parts[6].isEmpty() ? LocalDate.now() : LocalDate.parse(parts[6]);
                int failedAttempts = Integer.parseInt(parts[7]);
                boolean locked = Boolean.parseBoolean(parts[8]);
                double dailyLimit = Double.parseDouble(parts[9]);
                LocalDate lastDailyReset = parts[10].isEmpty() ? LocalDate.now() : LocalDate.parse(parts[10]);

                Account acc = new Account(accNo, name, type, bal, active, pinHash, lastInterest, failedAttempts, locked, dailyLimit, lastDailyReset);
                accounts.put(accNo, acc);
                nextAccountNo = Math.max(nextAccountNo, accNo + 1);
            }
        }

        private synchronized void persistAccounts() {
            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(accountsFile))) {
                for (Account a : accounts.values()) {
                    // accNo|name|type|balance|active|pinHash|lastInterest|failedAttempts|locked|dailyLimit|lastDailyReset
                    String line = String.join("|",
                            String.valueOf(a.getAccountNo()),
                            a.getHolderName(),
                            a.getType(),
                            String.valueOf(a.getBalance()),
                            String.valueOf(a.isActive()),
                            a.getPinHash(),
                            a.getLastInterestDate().toString(),
                            String.valueOf(a.getFailedLoginAttempts()),
                            String.valueOf(a.isLocked()),
                            String.valueOf(a.getDailyLimit()),
                            a.getLastDailyReset().toString()
                    );
                    bw.write(line);
                    bw.newLine();
                }
            } catch (Exception e) {
                System.out.println("Failed to save accounts: " + e.getMessage());
            }
        }

        public synchronized Account createAccount(String name, double initialDeposit, String type, String plainPin) {
            int accNo = nextAccountNo++;
            String pinHash = hashPin(plainPin);
            double dailyLimit = type.equalsIgnoreCase("Business") ? 50000.0 : 20000.0;
            Account a = new Account(accNo, name, type, initialDeposit, true, pinHash, LocalDate.now(), 0, false, dailyLimit, LocalDate.now());
            accounts.put(accNo, a);
            persistAccounts();
            // initial transaction record
            writeTransaction(accNo, new TransactionEntry("OPEN", initialDeposit, "Account opening", LocalDateTime.now(), 0));
            return a;
        }

        public synchronized Account findAccount(int accNo) {
            Account a = accounts.get(accNo);
            if (a == null || !a.isActive()) return null;
            return a;
        }

        public synchronized boolean deposit(int accNo, double amount) {
            Account a = accounts.get(accNo);
            if (a == null || !a.isActive()) return false;
            a.setBalance(a.getBalance() + amount);
            persistAccounts();
            writeTransaction(accNo, new TransactionEntry("DEPOSIT", amount, "Deposit", LocalDateTime.now(), nextTxId()));
            return true;
        }

        public synchronized boolean withdraw(int accNo, double amount) {
            Account a = accounts.get(accNo);
            if (a == null || !a.isActive()) return false;
            // reset daily limit if date changed
            if (!a.getLastDailyReset().equals(LocalDate.now())) {
                a.resetDailyWithdrawals();
                a.setLastDailyReset(LocalDate.now());
            }
            if (amount <= 0 || amount > a.getBalance()) return false;
            if (a.getDailyWithdrawals() + amount > a.getDailyLimit()) return false;
            a.setBalance(a.getBalance() - amount);
            a.addDailyWithdrawals(amount);
            persistAccounts();
            writeTransaction(accNo, new TransactionEntry("WITHDRAW", amount, "Withdrawal", LocalDateTime.now(), nextTxId()));
            return true;
        }

        public synchronized boolean transfer(int fromAcc, int toAcc, double amount) {
            Account aFrom = accounts.get(fromAcc);
            Account aTo = accounts.get(toAcc);
            if (aFrom == null || !aFrom.isActive()) return false;
            if (aTo == null || !aTo.isActive()) return false;
            // daily reset
            if (!aFrom.getLastDailyReset().equals(LocalDate.now())) {
                aFrom.resetDailyWithdrawals();
                aFrom.setLastDailyReset(LocalDate.now());
            }
            if (aFrom.getDailyWithdrawals() + amount > aFrom.getDailyLimit()) return false;
            if (amount <= 0 || amount > aFrom.getBalance()) return false;
            aFrom.setBalance(aFrom.getBalance() - amount);
            aTo.setBalance(aTo.getBalance() + amount);
            aFrom.addDailyWithdrawals(amount);
            persistAccounts();
            writeTransaction(fromAcc, new TransactionEntry("TRANSFER_OUT", amount, "Transfer to " + toAcc, LocalDateTime.now(), nextTxId()));
            writeTransaction(toAcc, new TransactionEntry("TRANSFER_IN", amount, "Transfer from " + fromAcc, LocalDateTime.now(), nextTxId()));
            return true;
        }

        public synchronized boolean verifyPin(int accNo, String plainPin) {
            Account a = accounts.get(accNo);
            if (a == null) return false;
            return a.getPinHash().equals(hashPin(plainPin));
        }

        public synchronized void changePin(int accNo, String newPin) {
            Account a = accounts.get(accNo);
            if (a == null) return;
            a.setPinHash(hashPin(newPin));
            persistAccounts();
        }

        public synchronized boolean registerFailedLogin(int accNo) {
            Account a = accounts.get(accNo);
            if (a == null) return false;
            a.incrementFailedLogin();
            if (a.getFailedLoginAttempts() >= 3) {
                a.setLocked(true);
            }
            persistAccounts();
            return a.isLocked();
        }

        public synchronized void resetFailedLogin(int accNo) {
            Account a = accounts.get(accNo);
            if (a == null) return;
            a.resetFailedLoginAttempts();
            persistAccounts();
        }

        private String hashPin(String p) {
            // simple hash (not secure) - in real systems use bcrypt/argon2
            return Integer.toString(p.hashCode());
        }

        // transaction persistence & reading
        private synchronized void writeTransaction(int accNo, TransactionEntry t) {
            try {
                Files.createDirectories(Paths.get(txDir));
                Path txFile = Paths.get(txDir, "tx_" + accNo + ".txt");
                try (BufferedWriter bw = Files.newBufferedWriter(txFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    // txId|type|amount|memo|timestamp|reverseOf
                    String line = String.join("|",
                            String.valueOf(t.id),
                            t.type,
                            String.valueOf(t.amount),
                            t.memo.replace("|", " "),
                            t.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            t.reverseOf == 0 ? "" : String.valueOf(t.reverseOf)
                    );
                    bw.write(line);
                    bw.newLine();
                }
            } catch (Exception e) {
                System.out.println("Write tx failed: " + e.getMessage());
            }
        }

        private synchronized List<TransactionEntry> readTransactions(int accNo) {
            List<TransactionEntry> list = new ArrayList<>();
            Path txFile = Paths.get(txDir, "tx_" + accNo + ".txt");
            if (!Files.exists(txFile)) return list;
            try {
                List<String> lines = Files.readAllLines(txFile);
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    String[] p = line.split("\\|", -1);
                    int id = Integer.parseInt(p[0]);
                    String type = p[1];
                    double amt = Double.parseDouble(p[2]);
                    String memo = p[3];
                    LocalDateTime ts = LocalDateTime.parse(p[4], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    int reverseOf = p[5].isEmpty() ? 0 : Integer.parseInt(p[5]);
                    list.add(new TransactionEntry(id, type, amt, memo, ts, reverseOf));
                }
            } catch (Exception e) {
                System.out.println("Read tx failed: " + e.getMessage());
            }
            return list;
        }

        public synchronized void printMiniStatement(int accNo, int lastN) {
            List<TransactionEntry> list = readTransactions(accNo);
            if (list.isEmpty()) {
                System.out.println("No transactions.");
                return;
            }
            System.out.println("Last " + lastN + " transactions:");
            int start = Math.max(0, list.size() - lastN);
            for (int i = list.size() - 1; i >= start; i--) {
                System.out.println(list.get(i));
            }
        }

        public synchronized void printTransactionHistory(int accNo, LocalDate from, LocalDate to, String type, double[] amountRange) {
            List<TransactionEntry> list = readTransactions(accNo);
            if (list.isEmpty()) {
                System.out.println("No transactions.");
                return;
            }
            System.out.println("Transaction History:");
            for (TransactionEntry t : list) {
                LocalDate d = t.timestamp.toLocalDate();
                if (from != null && d.isBefore(from)) continue;
                if (to != null && d.isAfter(to)) continue;
                if (type != null && !t.type.equalsIgnoreCase(type)) continue;
                if (amountRange != null && (t.amount < amountRange[0] || t.amount > amountRange[1])) continue;
                System.out.println(t);
            }
        }

        // transaction reversal: reverse last transaction for account if possible
        public synchronized void reverseLastTransaction(int accNo) {
            List<TransactionEntry> list = readTransactions(accNo);
            if (list.isEmpty()) {
                System.out.println("No transactions to reverse.");
                return;
            }
            TransactionEntry last = list.get(list.size() - 1);
            if (last.type.equals("OPEN")) {
                System.out.println("Cannot reverse opening transaction.");
                return;
            }
            // check if already reversed
            if (last.reverseOf != 0) {
                System.out.println("Last transaction is already a reversal.");
                return;
            }
            // check age - allow reversal only within 1 day
            if (Duration.between(last.timestamp, LocalDateTime.now()).toHours() > 24) {
                System.out.println("Cannot reverse transaction older than 24 hours.");
                return;
            }
            Account acc = accounts.get(accNo);
            if (acc == null) return;
            // perform reverse
            String revType = "REVERSAL_OF_" + last.type;
            double amt = last.amount;
            if (last.type.equals("DEPOSIT") || last.type.equals("TRANSFER_IN")) {
                // subtract
                if (acc.getBalance() < amt) {
                    System.out.println("Insufficient balance to reverse.");
                    return;
                }
                acc.setBalance(acc.getBalance() - amt);
            } else if (last.type.equals("WITHDRAW") || last.type.equals("TRANSFER_OUT")) {
                // add back
                acc.setBalance(acc.getBalance() + amt);
            } else {
                System.out.println("Unsupported reversal type.");
                return;
            }
            persistAccounts();
            int newTxId = nextTxId();
            writeTransaction(accNo, new TransactionEntry(newTxId, "REVERSAL", -amt, "Reversal of tx " + last.id, LocalDateTime.now(), last.id));
            System.out.println("Transaction reversed. New balance: " + formatCurrency(acc.getBalance()));
            audit.log("REVERSAL", "Account " + accNo + " reversed tx " + last.id);
        }

        // interest calculation - simple annual rate depending on type, apply proportional to days since lastInterest
        public synchronized void applyInterestIfDue(int accNo) {
            Account a = accounts.get(accNo);
            if (a == null) return;
            LocalDate last = a.getLastInterestDate();
            LocalDate now = LocalDate.now();
            long days = ChronoUnit.DAYS.between(last, now);
            if (days <= 0) return;
            double annualRate = getAnnualRate(a.getType());
            // simple interest for days
            double interest = a.getBalance() * (annualRate / 100.0) * (days / 365.0);
            if (interest > 0.0) {
                a.setBalance(a.getBalance() + interest);
                a.setLastInterestDate(now);
                persistAccounts();
                writeTransaction(accNo, new TransactionEntry(nextTxId(), "INTEREST", interest, "Interest for " + days + " days", LocalDateTime.now(), 0));
                System.out.println("Interest applied: " + formatCurrency(interest));
                audit.log("INTEREST", "Applied " + interest + " to acc " + accNo);
            } else {
                a.setLastInterestDate(now);
                persistAccounts();
            }
        }

        private double getAnnualRate(String type) {
            switch (type.toLowerCase()) {
                case "savings": return 4.0;
                case "checking": return 1.0;
                case "fixed": return 6.5;
                case "business": return 2.5;
                default: return 1.0;
            }
        }

        public synchronized void printBalance(int accNo) {
            Account a = accounts.get(accNo);
            if (a == null) return;
            System.out.println("Current balance: " + formatCurrency(a.getBalance()));
        }

        public synchronized void saveTransactionReceipt(int accNo, String type, double amt, String memo) {
            try {
                String ts = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
                Path r = Paths.get(RECEIPTS_DIR, "receipt_" + accNo + "_" + ts + ".txt");
                try (BufferedWriter bw = Files.newBufferedWriter(r)) {
                    bw.write("Receipt - Account: " + accNo);
                    bw.newLine();
                    bw.write("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    bw.newLine();
                    bw.write("Type: " + type);
                    bw.newLine();
                    bw.write("Amount: " + formatCurrency(amt));
                    bw.newLine();
                    bw.write("Memo: " + memo);
                    bw.newLine();
                }
            } catch (Exception e) {
                System.out.println("Receipt save failed: " + e.getMessage());
            }
        }

        public synchronized boolean isOverDailyLimit(int accNo, double amount) {
            Account a = accounts.get(accNo);
            if (a == null) return true;
            if (!a.getLastDailyReset().equals(LocalDate.now())) {
                a.resetDailyWithdrawals();
                a.setLastDailyReset(LocalDate.now());
                persistAccounts();
            }
            return (a.getDailyWithdrawals() + amount) > a.getDailyLimit();
        }

        public synchronized void checkAlerts(Account acc) {
            if (acc.getBalance() < 100.0) {
                System.out.println("ALERT: Low balance.");
            }
            if (acc.getBalance() > 500000) {
                System.out.println("ALERT: Large balance - consider contacting bank.");
            }
        }

        // admin actions
        public synchronized List<Account> getAllAccounts() {
            return new ArrayList<>(accounts.values());
        }

        public synchronized void setAccountActive(int accNo, boolean active) {
            Account a = accounts.get(accNo);
            if (a == null) return;
            a.setActive(active);
            persistAccounts();
            audit.log("ADMIN_ACCOUNT_STATUS", "Acc " + accNo + " active=" + active);
        }

        public synchronized void setAccountLock(int accNo, boolean locked) {
            Account a = accounts.get(accNo);
            if (a == null) return;
            a.setLocked(locked);
            persistAccounts();
            audit.log("ADMIN_LOCK", "Acc " + accNo + " locked=" + locked);
        }

        public synchronized void updateAccountBalance(int accNo, double newBalance) {
            Account a = accounts.get(accNo);
            if (a == null) return;
            a.setBalance(newBalance);
            persistAccounts();
            audit.log("ADMIN_ADJUST_BALANCE", "Acc " + accNo + " balance set to " + newBalance);
        }

        // next tx id generation (simple)
        private synchronized int nextTxId() {
            int max = 0;
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get(txDir), "tx_*.txt")) {
                for (Path p : ds) {
                    List<String> lines = Files.readAllLines(p);
                    for (String l : lines) {
                        if (l.trim().isEmpty()) continue;
                        String[] parts = l.split("\\|", -1);
                        int id = Integer.parseInt(parts[0]);
                        max = Math.max(max, id);
                    }
                }
            } catch (Exception e) {}
            return max + 1;
        }
    }

    // ---------------------------
    // Account entity
    // ---------------------------
    static class Account {
        private final int accountNo;
        private String holderName;
        private String type;
        private double balance;
        private boolean active;
        private String pinHash;
        private LocalDate lastInterestDate;
        private int failedLoginAttempts;
        private boolean locked;
        private double dailyLimit;
        private double dailyWithdrawals;
        private LocalDate lastDailyReset;

        public Account(int accountNo, String holderName, String type, double balance, boolean active, String pinHash, LocalDate lastInterestDate, int failedLoginAttempts, boolean locked, double dailyLimit, LocalDate lastDailyReset) {
            this.accountNo = accountNo;
            this.holderName = holderName;
            this.type = type;
            this.balance = balance;
            this.active = active;
            this.pinHash = pinHash;
            this.lastInterestDate = lastInterestDate;
            this.failedLoginAttempts = failedLoginAttempts;
            this.locked = locked;
            this.dailyLimit = dailyLimit;
            this.dailyWithdrawals = 0.0;
            this.lastDailyReset = lastDailyReset;
        }

        // getters & setters
        public int getAccountNo() { return accountNo; }
        public String getHolderName() { return holderName; }
        public String getType() { return type; }
        public double getBalance() { return balance; }
        public boolean isActive() { return active; }
        public String getPinHash() { return pinHash; }
        public LocalDate getLastInterestDate() { return lastInterestDate; }
        public int getFailedLoginAttempts() { return failedLoginAttempts; }
        public boolean isLocked() { return locked; }
        public double getDailyLimit() { return dailyLimit; }
        public LocalDate getLastDailyReset() { return lastDailyReset; }
        public double getDailyWithdrawals() { return dailyWithdrawals; }

        public void setBalance(double b) { this.balance = b; }
        public void setActive(boolean v) { this.active = v; }
        public void setPinHash(String h) { this.pinHash = h; }
        public void setLastInterestDate(LocalDate d) { this.lastInterestDate = d; }
        public void incrementFailedLogin() { this.failedLoginAttempts++; }
        public void resetFailedLoginAttempts() { this.failedLoginAttempts = 0; }
        public void setLocked(boolean v) { this.locked = v; }
        public void setDailyLimit(double v) { this.dailyLimit = v; }
        public void setLastDailyReset(LocalDate d) { this.lastDailyReset = d; }
        public void addDailyWithdrawals(double amt) { this.dailyWithdrawals += amt; }
        public void resetDailyWithdrawals() { this.dailyWithdrawals = 0.0; }
    }

    // ---------------------------
    // Transaction entry
    // ---------------------------
    static class TransactionEntry {
        int id;
        String type;
        double amount;
        String memo;
        LocalDateTime timestamp;
        int reverseOf;

        public TransactionEntry(int id, String type, double amount, String memo, LocalDateTime timestamp, int reverseOf) {
            this.id = id;
            this.type = type;
            this.amount = amount;
            this.memo = memo;
            this.timestamp = timestamp;
            this.reverseOf = reverseOf;
        }

        public TransactionEntry(String type, double amount, String memo, LocalDateTime ts, int reverseOf) {
            this.id = 0;
            this.type = type;
            this.amount = amount;
            this.memo = memo;
            this.timestamp = ts;
            this.reverseOf = reverseOf;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s %.2f - %s (txId:%d)", timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), type, amount, memo, id);
        }
    }

    // ---------------------------
    // Scheduled transactions manager
    // ---------------------------
    static class ScheduledManager {
        private final String file;
        private final AccountManager accountManager;
        private final AuditLogger audit;

        public ScheduledManager(String file, AccountManager accountManager, AuditLogger audit) {
            this.file = file;
            this.accountManager = accountManager;
            this.audit = audit;
            try {
                Path p = Paths.get(file);
                if (!Files.exists(p)) Files.createFile(p);
            } catch (IOException e) {}
        }

        public synchronized void addScheduledTransaction(ScheduledTransaction s) {
            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(file), StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                // from|to|amount|date|type|memo
                String line = String.join("|",
                        String.valueOf(s.fromAcc),
                        String.valueOf(s.toAcc),
                        String.valueOf(s.amount),
                        s.date.toString(),
                        s.type,
                        s.memo.replace("|", " ")
                );
                bw.write(line);
                bw.newLine();
            } catch (Exception e) {
                System.out.println("Add scheduled failed: " + e.getMessage());
            }
        }

        public synchronized void applyDueScheduledTransactions() {
            List<ScheduledTransaction> all = readAllScheduled();
            LocalDate today = LocalDate.now();
            List<ScheduledTransaction> remain = new ArrayList<>();
            for (ScheduledTransaction s : all) {
                if (!s.date.isAfter(today)) {
                    applyScheduled(s);
                } else {
                    remain.add(s);
                }
            }
            // overwrite with remaining
            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(file))) {
                for (ScheduledTransaction r : remain) {
                    String line = String.join("|",
                            String.valueOf(r.fromAcc),
                            String.valueOf(r.toAcc),
                            String.valueOf(r.amount),
                            r.date.toString(),
                            r.type,
                            r.memo.replace("|", " ")
                    );
                    bw.write(line); bw.newLine();
                }
            } catch (Exception e) {}
        }

        public synchronized void applyDueScheduledTransactionsForAccount(int accNo) {
            List<ScheduledTransaction> all = readAllScheduled();
            LocalDate today = LocalDate.now();
            List<ScheduledTransaction> remain = new ArrayList<>();
            for (ScheduledTransaction s : all) {
                if (!s.date.isAfter(today) && s.fromAcc == accNo) {
                    applyScheduled(s);
                } else {
                    remain.add(s);
                }
            }
            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(file))) {
                for (ScheduledTransaction r : remain) {
                    String line = String.join("|",
                            String.valueOf(r.fromAcc),
                            String.valueOf(r.toAcc),
                            String.valueOf(r.amount),
                            r.date.toString(),
                            r.type,
                            r.memo.replace("|", " ")
                    );
                    bw.write(line); bw.newLine();
                }
            } catch (Exception e) {}
        }

        private void applyScheduled(ScheduledTransaction s) {
            boolean ok = false;
            if (s.type.equals("TRANSFER")) {
                ok = accountManager.transfer(s.fromAcc, s.toAcc, s.amount);
            } else if (s.type.equals("WITHDRAW")) {
                ok = accountManager.withdraw(s.fromAcc, s.amount);
            }
            if (ok) {
                audit.log("SCHEDULE_EXECUTED", "Scheduled tx executed for acc " + s.fromAcc + " amt " + s.amount + " type " + s.type);
            } else {
                audit.log("SCHEDULE_FAILED", "Scheduled tx failed for acc " + s.fromAcc + " amt " + s.amount + " type " + s.type);
            }
        }

        private List<ScheduledTransaction> readAllScheduled() {
            List<ScheduledTransaction> list = new ArrayList<>();
            Path p = Paths.get(file);
            if (!Files.exists(p)) return list;
            try {
                List<String> lines = Files.readAllLines(p);
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split("\\|", -1);
                    int from = Integer.parseInt(parts[0]);
                    int to = Integer.parseInt(parts[1]);
                    double amt = Double.parseDouble(parts[2]);
                    LocalDate d = LocalDate.parse(parts[3]);
                    String type = parts[4];
                    String memo = parts[5];
                    list.add(new ScheduledTransaction(from, to, amt, d, type, memo));
                }
            } catch (Exception e) {}
            return list;
        }
    }

    static class ScheduledTransaction {
        int fromAcc;
        int toAcc; // -1 if not used
        double amount;
        LocalDate date;
        String type; // TRANSFER or WITHDRAW
        String memo;

        public ScheduledTransaction(int fromAcc, int toAcc, double amount, LocalDate date, String type, String memo) {
            this.fromAcc = fromAcc;
            this.toAcc = toAcc;
            this.amount = amount;
            this.date = date;
            this.type = type;
            this.memo = memo;
        }
    }

    // ---------------------------
    // Admin manager
    // ---------------------------
    static class AdminManager {
        private final AccountManager accountManager;
        private final AuditLogger audit;
        private final String adminUser = "admin";
        private final String adminPassHash; // simple hash
        private final Scanner sc = new Scanner(System.in);

        public AdminManager(AccountManager am, AuditLogger audit) {
            this.accountManager = am;
            this.audit = audit;
            this.adminPassHash = Integer.toString("admin123".hashCode()); // default password admin123
        }

        public void adminLoginFlow() {
            System.out.print("Admin username: ");
            String u = sc.next().trim();
            System.out.print("Admin password: ");
            String p = sc.next().trim();
            if (!u.equals(adminUser) || !Integer.toString(p.hashCode()).equals(adminPassHash)) {
                System.out.println("Invalid admin credentials.");
                audit.log("ADMIN_LOGIN_FAIL", "Admin login failed.");
                return;
            }
            audit.log("ADMIN_LOGIN", "Admin logged in.");
            adminSession();
        }

        private void adminSession() {
            boolean keep = true;
            while (keep) {
                System.out.println("\n--- Admin Panel ---");
                System.out.println("1. View all accounts");
                System.out.println("2. Search account");
                System.out.println("3. Freeze/Unfreeze account");
                System.out.println("4. Adjust balance");
                System.out.println("5. View audit log");
                System.out.println("6. Export account statements (CSV)");
                System.out.println("7. Generate statistics");
                System.out.println("8. Unlock account");
                System.out.println("9. Logout");
                System.out.print("Choose: ");
                int ch = readInt(1, 9);
                switch (ch) {
                    case 1:
                        viewAllAccounts();
                        break;
                    case 2:
                        searchAccountFlow();
                        break;
                    case 3:
                        freezeAccountFlow();
                        break;
                    case 4:
                        adjustBalanceFlow();
                        break;
                    case 5:
                        viewAuditLog();
                        break;
                    case 6:
                        exportStatements();
                        break;
                    case 7:
                        showStatistics();
                        break;
                    case 8:
                        unlockAccountFlow();
                        break;
                    case 9:
                        System.out.println("Admin logout.");
                        audit.log("ADMIN_LOGOUT", "Admin logged out.");
                        keep = false;
                        break;
                }
            }
        }

        private void viewAllAccounts() {
            List<Account> all = accountManager.getAllAccounts();
            System.out.println("AccNo\tName\tType\tBalance\tActive\tLocked");
            for (Account a : all) {
                System.out.printf("%d\t%s\t%s\t%.2f\t%s\t%s%n",
                        a.getAccountNo(), a.getHolderName(), a.getType(),
                        a.getBalance(), a.isActive(), a.isLocked());
            }
            audit.log("ADMIN_VIEW_ALL", "Viewed all accounts.");
        }

        private void searchAccountFlow() {
            System.out.println("Search by: 1) Account Number 2) Name 3) Type");
            System.out.print("Choose: ");
            int ch = readInt(1, 3);
            if (ch == 1) {
                System.out.print("Enter account number: ");
                Account a = accountManager.findAccount(readInt());
                if (a == null) System.out.println("Not found.");
                else printAccountDetails(a);
            } else if (ch == 2) {
                System.out.print("Enter partial/full name: ");
                String q = sc.next().trim().toLowerCase();
                boolean found = false;
                for (Account a : accountManager.getAllAccounts()) {
                    if (a.getHolderName().toLowerCase().contains(q)) {
                        printAccountDetails(a);
                        found = true;
                    }
                }
                if (!found) System.out.println("No accounts found.");
            } else {
                System.out.print("Enter type (Savings/Checking/Fixed/Business): ");
                String t = sc.next().trim();
                boolean found = false;
                for (Account a : accountManager.getAllAccounts()) {
                    if (a.getType().equalsIgnoreCase(t)) {
                        printAccountDetails(a);
                        found = true;
                    }
                }
                if (!found) System.out.println("No accounts found.");
            }
        }

        private void freezeAccountFlow() {
            System.out.print("Enter account number: ");
            int accNo = readInt();
            Account a = accountManager.findAccount(accNo);
            if (a == null) {
                System.out.println("Account not found.");
                return;
            }
            System.out.println("1. Freeze  2. Unfreeze");
            int ch = readInt(1, 2);
            accountManager.setAccountActive(accNo, ch == 2); // freeze => active=false, unfreeze => active=true
            System.out.println("Done.");
        }

        private void adjustBalanceFlow() {
            System.out.print("Enter account number: ");
            int accNo = readInt();
            Account a = accountManager.findAccount(accNo);
            if (a == null) {
                System.out.println("Account not found.");
                return;
            }
            System.out.print("Enter new balance: ");
            double b = readDoubleMin(0);
            accountManager.updateAccountBalance(accNo, b);
            System.out.println("Balance updated.");
        }

        private void viewAuditLog() {
            Path p = Paths.get(AUDIT_LOG);
            if (!Files.exists(p)) {
                System.out.println("No logs.");
                return;
            }
            try {
                List<String> lines = Files.readAllLines(p);
                for (String l : lines) System.out.println(l);
            } catch (Exception e) {
                System.out.println("Could not read audit log.");
            }
        }

        private void exportStatements() {
            System.out.print("Enter account number: ");
            int accNo = readInt();
            List<TransactionEntry> tx = accountManager.readTransactions(accNo);
            if (tx.isEmpty()) {
                System.out.println("No transactions.");
                return;
            }
            Path out = Paths.get(DATA_DIR, "statement_" + accNo + ".csv");
            try (BufferedWriter bw = Files.newBufferedWriter(out)) {
                bw.write("txId,type,amount,memo,timestamp,reverseOf"); bw.newLine();
                for (TransactionEntry t : tx) {
                    bw.write(String.join(",", String.valueOf(t.id), t.type, String.valueOf(t.amount), t.memo.replace(",", " "), t.timestamp.toString(), String.valueOf(t.reverseOf)));
                    bw.newLine();
                }
                System.out.println("Statement exported to " + out.toString());
                audit.log("ADMIN_EXPORT", "Exported statement for " + accNo);
            } catch (Exception e) {
                System.out.println("Export failed: " + e.getMessage());
            }
        }

        private void showStatistics() {
            List<Account> all = accountManager.getAllAccounts();
            double totalDeposits = 0;
            double totalBalance = 0;
            int active = 0;
            for (Account a : all) {
                totalBalance += a.getBalance();
                if (a.isActive()) active++;
            }
            System.out.println("Total accounts: " + all.size());
            System.out.println("Active accounts: " + active);
            System.out.println("Total balance across accounts: " + totalBalance);
            audit.log("ADMIN_STATS", "Viewed system statistics.");
        }

        private void unlockAccountFlow() {
            System.out.print("Enter account number to unlock: ");
            int accNo = readInt();
            accountManager.resetFailedLogin(accNo);
            accountManager.setAccountLock(accNo, false);
            System.out.println("Account unlocked and failed attempts reset.");
        }

        private void printAccountDetails(Account a) {
            System.out.println("AccNo: " + a.getAccountNo());
            System.out.println("Name : " + a.getHolderName());
            System.out.println("Type : " + a.getType());
            System.out.println("Bal  : " + a.getBalance());
            System.out.println("Active: " + a.isActive());
            System.out.println("Locked: " + a.isLocked());
        }
    }

    // ---------------------------
    // Simple audit logger
    // ---------------------------
    static class AuditLogger {
        private final Path file;
        public AuditLogger(String file) throws IOException {
            this.file = Paths.get(file);
            if (!Files.exists(this.file)) Files.createFile(this.file);
        }
        public synchronized void log(String event, String msg) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            try (BufferedWriter bw = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                bw.write(ts + " | " + event + " | " + msg);
                bw.newLine();
            } catch (Exception e) {}
        }
    }
}