package banking_system_console;
/*
 * Author: Samruddha Belsare
 * Updated: 07-Oct-2025
 */

import java.sql.*;
import java.sql.Date;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BankingSystemSQLComplete {

    // ═══════════════════════════════════════════════════════════════
    //                        DATABASE CONFIGURATION
    // ═══════════════════════════════════════════════════════════════
    private static final String DB_URL = "jdbc:mysql://localhost:3306/banking_system";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "bntyipo43505408^%^*%%^&^&fghhfhh5y56877"; // CHANGE THIS!

    // ═══════════════════════════════════════════════════════════════
    //                        SYSTEM CONSTANTS
    // ═══════════════════════════════════════════════════════════════
    private static final Scanner sc = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");
    private static final DateTimeFormatter SIMPLE_DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    // Business Rules
    private static final int MAX_PIN_ATTEMPTS = 3;
    private static final double DEFAULT_SAVINGS_LIMIT = 20000.0;
    private static final double DEFAULT_CHECKING_LIMIT = 15000.0;
    private static final double DEFAULT_BUSINESS_LIMIT = 50000.0;
    private static final double SAVINGS_INTEREST_RATE = 0.04;
    private static final double CHECKING_INTEREST_RATE = 0.02;
    private static final double BUSINESS_INTEREST_RATE = 0.03;

    // ═══════════════════════════════════════════════════════════════
    //                        UTILITY CLASSES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Database connection utility
     */
    private static class DBUtil {
        static Connection getConnection() throws SQLException {
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        }

        static void closeQuietly(AutoCloseable... resources) {
            for (AutoCloseable resource : resources) {
                try {
                    if (resource != null) resource.close();
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Security utilities for PIN hashing and validation
     */
    private static class SecurityUtil {
        static String hashPin(String pin) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(pin.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : hash) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                // Fallback to simple hash
                return String.valueOf(pin.hashCode());
            }
        }

        static boolean validatePin(String pin) {
            return pin != null && pin.matches("\\d{4}");
        }

        static boolean validateAccountType(String type) {
            return type != null && (type.equalsIgnoreCase("Savings") ||
                    type.equalsIgnoreCase("Checking") ||
                    type.equalsIgnoreCase("Business") ||
                    type.equalsIgnoreCase("Fixed"));
        }
    }

    /**
     * Input validation utilities
     */
    private static class InputUtil {
        static double getDouble(String prompt) {
            while (true) {
                try {
                    System.out.print(prompt);
                    return Double.parseDouble(sc.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number. Please try again.");
                }
            }
        }

        static int getInt(String prompt) {
            while (true) {
                try {
                    System.out.print(prompt);
                    return Integer.parseInt(sc.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number. Please try again.");
                }
            }
        }

        static String getString(String prompt) {
            System.out.print(prompt);
            return sc.nextLine().trim();
        }

        static String getPin(String prompt) {
            while (true) {
                String pin = getString(prompt);
                if (SecurityUtil.validatePin(pin)) {
                    return pin;
                }
                System.out.println("PIN must be exactly 4 digits. Please try again.");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //                        DATA CLASSES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Account data class
     */
    static class Account {
        private int accountNo;
        private String holderName;
        private String accountType;
        private BigDecimal balance;
        private boolean active;
        private String pinHash;
        private LocalDate lastInterestDate;
        private int failedLoginAttempts;
        private boolean locked;
        private BigDecimal dailyLimit;
        private LocalDate lastDailyReset;
        private LocalDateTime createdDate;

        // Constructors
        public Account() {}

        public Account(int accountNo, String holderName, String accountType,
                       BigDecimal balance, boolean active, String pinHash,
                       LocalDate lastInterestDate, int failedLoginAttempts,
                       boolean locked, BigDecimal dailyLimit, LocalDate lastDailyReset) {
            this.accountNo = accountNo;
            this.holderName = holderName;
            this.accountType = accountType;
            this.balance = balance;
            this.active = active;
            this.pinHash = pinHash;
            this.lastInterestDate = lastInterestDate;
            this.failedLoginAttempts = failedLoginAttempts;
            this.locked = locked;
            this.dailyLimit = dailyLimit;
            this.lastDailyReset = lastDailyReset;
        }

        // Getters and Setters
        public int getAccountNo() { return accountNo; }
        public void setAccountNo(int accountNo) { this.accountNo = accountNo; }

        public String getHolderName() { return holderName; }
        public void setHolderName(String holderName) { this.holderName = holderName; }

        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }

        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal balance) { this.balance = balance; }

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }

        public String getPinHash() { return pinHash; }
        public void setPinHash(String pinHash) { this.pinHash = pinHash; }

        public LocalDate getLastInterestDate() { return lastInterestDate; }
        public void setLastInterestDate(LocalDate lastInterestDate) { this.lastInterestDate = lastInterestDate; }

        public int getFailedLoginAttempts() { return failedLoginAttempts; }
        public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

        public boolean isLocked() { return locked; }
        public void setLocked(boolean locked) { this.locked = locked; }

        public BigDecimal getDailyLimit() { return dailyLimit; }
        public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }

        public LocalDate getLastDailyReset() { return lastDailyReset; }
        public void setLastDailyReset(LocalDate lastDailyReset) { this.lastDailyReset = lastDailyReset; }

        public LocalDateTime getCreatedDate() { return createdDate; }
        public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    }

    /**
     * Transaction data class
     */
    static class Transaction {
        private int id;
        private int accountNo;
        private String type;
        private BigDecimal amount;
        private String memo;
        private LocalDateTime txnTime;
        private Integer reverseOf;

        public Transaction() {}

        public Transaction(int id, int accountNo, String type, BigDecimal amount,
                           String memo, LocalDateTime txnTime, Integer reverseOf) {
            this.id = id;
            this.accountNo = accountNo;
            this.type = type;
            this.amount = amount;
            this.memo = memo;
            this.txnTime = txnTime;
            this.reverseOf = reverseOf;
        }

        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getAccountNo() { return accountNo; }
        public void setAccountNo(int accountNo) { this.accountNo = accountNo; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getMemo() { return memo; }
        public void setMemo(String memo) { this.memo = memo; }

        public LocalDateTime getTxnTime() { return txnTime; }
        public void setTxnTime(LocalDateTime txnTime) { this.txnTime = txnTime; }

        public Integer getReverseOf() { return reverseOf; }
        public void setReverseOf(Integer reverseOf) { this.reverseOf = reverseOf; }
    }

    /**
     * Scheduled Transaction data class
     */
    static class ScheduledTransaction {
        private int id;
        private int accountNo;
        private String type;
        private BigDecimal amount;
        private String memo;
        private LocalDate scheduleDate;
        private boolean executed;

        public ScheduledTransaction() {}

        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getAccountNo() { return accountNo; }
        public void setAccountNo(int accountNo) { this.accountNo = accountNo; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getMemo() { return memo; }
        public void setMemo(String memo) { this.memo = memo; }

        public LocalDate getScheduleDate() { return scheduleDate; }
        public void setScheduleDate(LocalDate scheduleDate) { this.scheduleDate = scheduleDate; }

        public boolean isExecuted() { return executed; }
        public void setExecuted(boolean executed) { this.executed = executed; }
    }

    // ═══════════════════════════════════════════════════════════════
    //                        AUDIT LOGGER
    // ═══════════════════════════════════════════════════════════════

    /**
     * Comprehensive audit logging system
     */
    static class AuditLogger {

        void log(String eventType, String description) {
            log(eventType, description, null, null);
        }

        void log(String eventType, String description, Integer userId, Integer accountNo) {
            String sql = "INSERT INTO audit_log (event_time, user_id, account_no, event_text) VALUES (NOW(), ?, ?, ?)";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                if (userId != null) {
                    ps.setInt(1, userId);
                } else {
                    ps.setNull(1, Types.INTEGER);
                }

                if (accountNo != null) {
                    ps.setInt(2, accountNo);
                } else {
                    ps.setNull(2, Types.INTEGER);
                }

                ps.setString(3, eventType + " | " + description);
                ps.executeUpdate();

            } catch (SQLException e) {
                System.err.println("Failed to log audit event: " + e.getMessage());
            }
        }

        void viewAuditLog(int limit) {
            String sql = "SELECT event_time, event_text FROM audit_log ORDER BY event_time DESC LIMIT ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, limit);
                ResultSet rs = ps.executeQuery();

                System.out.println("═══════════════════════════════════════");
                System.out.println("           AUDIT LOG (Last " + limit + ")");
                System.out.println("═══════════════════════════════════════");
                System.out.printf("%-20s %s%n", "Time", "Event");
                System.out.println("─────────────────────────────────────────");

                while (rs.next()) {
                    System.out.printf("%-20s %s%n",
                            rs.getTimestamp("event_time").toLocalDateTime().format(DATE_FMT),
                            rs.getString("event_text"));
                }

            } catch (SQLException e) {
                System.out.println("Error viewing audit log: " + e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //                        ACCOUNT MANAGER
    // ═══════════════════════════════════════════════════════════════

    /**
     * Comprehensive account management system
     */
    static class AccountManager {

        private final AuditLogger auditLogger;

        public AccountManager(AuditLogger auditLogger) {
            this.auditLogger = auditLogger;
        }

        /**
         * Create new account
         */
        public Account createAccount(String holderName, String accountType, double initialDeposit, String pin) {
            if (!SecurityUtil.validateAccountType(accountType)) {
                System.out.println("Invalid account type. Must be Savings, Checking, Business, or Fixed.");
                return null;
            }

            if (!SecurityUtil.validatePin(pin)) {
                System.out.println("Invalid PIN. Must be exactly 4 digits.");
                return null;
            }

            if (initialDeposit < 0) {
                System.out.println("Initial deposit cannot be negative.");
                return null;
            }

            String sql = """
                INSERT INTO accounts (holder_name, account_type, balance, active, pin_hash, 
                                    last_interest_date, failed_login_attempts, locked, 
                                    daily_limit, last_daily_reset)
                VALUES (?, ?, ?, ?, ?, CURDATE(), ?, ?, ?, CURDATE())
                """;

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, holderName);
                ps.setString(2, accountType);
                ps.setBigDecimal(3, BigDecimal.valueOf(initialDeposit));
                ps.setBoolean(4, true);
                ps.setString(5, SecurityUtil.hashPin(pin));
                ps.setInt(6, 0);
                ps.setBoolean(7, false);

                // Set daily limit based on account type
                double dailyLimit = switch (accountType.toLowerCase()) {
                    case "savings" -> DEFAULT_SAVINGS_LIMIT;
                    case "checking" -> DEFAULT_CHECKING_LIMIT;
                    case "business" -> DEFAULT_BUSINESS_LIMIT;
                    default -> DEFAULT_SAVINGS_LIMIT;
                };
                ps.setBigDecimal(8, BigDecimal.valueOf(dailyLimit));

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    ResultSet generatedKeys = ps.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        int accountNo = generatedKeys.getInt(1);

                        // Log initial deposit transaction
                        if (initialDeposit > 0) {
                            addTransaction(accountNo, "INITIAL_DEPOSIT", BigDecimal.valueOf(initialDeposit),
                                    "Account opening deposit", null);
                        }

                        auditLogger.log("ACCOUNT_CREATED",
                                "Account " + accountNo + " created for " + holderName, null, accountNo);

                        return findAccount(accountNo);
                    }
                }

            } catch (SQLException e) {
                System.out.println("Error creating account: " + e.getMessage());
                auditLogger.log("ACCOUNT_CREATE_FAILED", "Failed to create account for " + holderName);
            }

            return null;
        }

        /**
         * Find account by account number
         */
        public Account findAccount(int accountNo) {
            String sql = "SELECT * FROM accounts WHERE account_no = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, accountNo);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    return mapResultSetToAccount(rs);
                }

            } catch (SQLException e) {
                System.out.println("Error finding account: " + e.getMessage());
            }

            return null;
        }

        /**
         * Verify PIN for login
         */
        public boolean verifyPin(int accountNo, String pin) {
            Account account = findAccount(accountNo);
            if (account == null || !account.isActive() || account.isLocked()) {
                return false;
            }

            return account.getPinHash().equals(SecurityUtil.hashPin(pin));
        }

        /**
         * Handle failed login attempt
         */
        public boolean handleFailedLogin(int accountNo) {
            String sql = "UPDATE accounts SET failed_login_attempts = failed_login_attempts + 1 WHERE account_no = ?";
            String lockSql = "UPDATE accounts SET locked = TRUE WHERE account_no = ? AND failed_login_attempts >= ?";

            try (Connection conn = DBUtil.getConnection()) {
                conn.setAutoCommit(false);

                try (PreparedStatement ps1 = conn.prepareStatement(sql);
                     PreparedStatement ps2 = conn.prepareStatement(lockSql)) {

                    ps1.setInt(1, accountNo);
                    ps1.executeUpdate();

                    ps2.setInt(1, accountNo);
                    ps2.setInt(2, MAX_PIN_ATTEMPTS);
                    int lockedRows = ps2.executeUpdate();

                    conn.commit();

                    auditLogger.log("LOGIN_FAILED", "Failed login attempt for account " + accountNo, null, accountNo);

                    if (lockedRows > 0) {
                        auditLogger.log("ACCOUNT_LOCKED", "Account " + accountNo + " locked due to multiple failed attempts", null, accountNo);
                        return true; // Account was locked
                    }

                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }

            } catch (SQLException e) {
                System.out.println("Error handling failed login: " + e.getMessage());
            }

            return false;
        }

        /**
         * Reset failed login attempts
         */
        public void resetFailedLoginAttempts(int accountNo) {
            String sql = "UPDATE accounts SET failed_login_attempts = 0 WHERE account_no = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, accountNo);
                ps.executeUpdate();

            } catch (SQLException e) {
                System.out.println("Error resetting failed login attempts: " + e.getMessage());
            }
        }

        /**
         * Deposit money to account
         */
        public boolean deposit(int accountNo, double amount) {
            if (amount <= 0) {
                System.out.println("Deposit amount must be positive.");
                return false;
            }

            Account account = findAccount(accountNo);
            if (account == null || !account.isActive() || account.isLocked()) {
                System.out.println("Account not found, inactive, or locked.");
                return false;
            }

            String sql = "UPDATE accounts SET balance = balance + ? WHERE account_no = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setBigDecimal(1, BigDecimal.valueOf(amount));
                ps.setInt(2, accountNo);

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    addTransaction(accountNo, "DEPOSIT", BigDecimal.valueOf(amount),
                            "Cash deposit", null);

                    auditLogger.log("DEPOSIT",
                            "₹" + amount + " deposited to account " + accountNo, null, accountNo);

                    return true;
                }

            } catch (SQLException e) {
                System.out.println("Error processing deposit: " + e.getMessage());
                auditLogger.log("DEPOSIT_FAILED",
                        "Failed to deposit ₹" + amount + " to account " + accountNo, null, accountNo);
            }

            return false;
        }

        /**
         * Withdraw money from account
         */
        public boolean withdraw(int accountNo, double amount) {
            if (amount <= 0) {
                System.out.println("Withdrawal amount must be positive.");
                return false;
            }

            Account account = findAccount(accountNo);
            if (account == null || !account.isActive() || account.isLocked()) {
                System.out.println("Account not found, inactive, or locked.");
                return false;
            }

            if (account.getBalance().compareTo(BigDecimal.valueOf(amount)) < 0) {
                System.out.println("Insufficient balance.");
                return false;
            }

            // Check daily limit
            if (!checkDailyLimit(accountNo, amount)) {
                System.out.println("Daily withdrawal limit exceeded.");
                return false;
            }
            /*
             * Author: Samruddha Belsare
             * Updated: 07-Oct-2025
             */
            String sql = "UPDATE accounts SET balance = balance - ? WHERE account_no = ?";
            try (Connection conn = DBUtil.getConnection()) {
                conn.setAutoCommit(false);

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setBigDecimal(1, BigDecimal.valueOf(amount));
                    ps.setInt(2, accountNo);

                    int rowsAffected = ps.executeUpdate();
                    if (rowsAffected > 0) {
                        addTransaction(conn, accountNo, "WITHDRAWAL", BigDecimal.valueOf(amount),
                                "Cash withdrawal", null);

                        conn.commit();

                        auditLogger.log("WITHDRAWAL",
                                "₹" + amount + " withdrawn from account " + accountNo, null, accountNo);

                        return true;
                    }

                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }

            } catch (SQLException e) {
                System.out.println("Error processing withdrawal: " + e.getMessage());
                auditLogger.log("WITHDRAWAL_FAILED",
                        "Failed to withdraw ₹" + amount + " from account " + accountNo, null, accountNo);
            }

            return false;
        }

        /**
         * Transfer money between accounts
         */
        public boolean transfer(int fromAccount, int toAccount, double amount) {
            if (amount <= 0) {
                System.out.println("Transfer amount must be positive.");
                return false;
            }

            if (fromAccount == toAccount) {
                System.out.println("Cannot transfer to the same account.");
                return false;
            }

            Account fromAcc = findAccount(fromAccount);
            Account toAcc = findAccount(toAccount);

            if (fromAcc == null || !fromAcc.isActive() || fromAcc.isLocked()) {
                System.out.println("Source account not found, inactive, or locked.");
                return false;
            }

            if (toAcc == null || !toAcc.isActive()) {
                System.out.println("Destination account not found or inactive.");
                return false;
            }

            if (fromAcc.getBalance().compareTo(BigDecimal.valueOf(amount)) < 0) {
                System.out.println("Insufficient balance in source account.");
                return false;
            }

            // Check daily limit for source account
            if (!checkDailyLimit(fromAccount, amount)) {
                System.out.println("Daily transfer limit exceeded.");
                return false;
            }

            String debitSql = "UPDATE accounts SET balance = balance - ? WHERE account_no = ?";
            String creditSql = "UPDATE accounts SET balance = balance + ? WHERE account_no = ?";

            try (Connection conn = DBUtil.getConnection()) {
                conn.setAutoCommit(false);

                try (PreparedStatement debitPs = conn.prepareStatement(debitSql);
                     PreparedStatement creditPs = conn.prepareStatement(creditSql)) {

                    // Debit from source account
                    debitPs.setBigDecimal(1, BigDecimal.valueOf(amount));
                    debitPs.setInt(2, fromAccount);
                    debitPs.executeUpdate();

                    // Credit to destination account
                    creditPs.setBigDecimal(1, BigDecimal.valueOf(amount));
                    creditPs.setInt(2, toAccount);
                    creditPs.executeUpdate();

                    // Add transactions
                    addTransaction(conn, fromAccount, "TRANSFER_OUT", BigDecimal.valueOf(amount),
                            "Transfer to account " + toAccount, null);
                    addTransaction(conn, toAccount, "TRANSFER_IN", BigDecimal.valueOf(amount),
                            "Transfer from account " + fromAccount, null);

                    conn.commit();

                    auditLogger.log("TRANSFER",
                            "₹" + amount + " transferred from " + fromAccount + " to " + toAccount,
                            null, fromAccount);

                    return true;

                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }

            } catch (SQLException e) {
                System.out.println("Error processing transfer: " + e.getMessage());
                auditLogger.log("TRANSFER_FAILED",
                        "Failed to transfer ₹" + amount + " from " + fromAccount + " to " + toAccount,
                        null, fromAccount);
            }

            return false;
        }

        /**
         * Apply interest if due
         */
        public void applyInterestIfDue(int accountNo) {
            Account account = findAccount(accountNo);
            if (account == null || !account.isActive()) {
                return;
            }

            LocalDate today = LocalDate.now();
            LocalDate lastInterestDate = account.getLastInterestDate();

            if (lastInterestDate == null || lastInterestDate.isBefore(today)) {
                double interestRate = switch (account.getAccountType().toLowerCase()) {
                    case "savings" -> SAVINGS_INTEREST_RATE;
                    case "checking" -> CHECKING_INTEREST_RATE;
                    case "business" -> BUSINESS_INTEREST_RATE;
                    default -> SAVINGS_INTEREST_RATE;
                };

                // Calculate monthly interest (annual rate / 12)
                BigDecimal monthlyRate = BigDecimal.valueOf(interestRate / 12.0);
                BigDecimal interest = account.getBalance().multiply(monthlyRate)
                        .setScale(2, RoundingMode.HALF_UP);

                if (interest.compareTo(BigDecimal.ZERO) > 0) {
                    String updateSql = """
                        UPDATE accounts 
                        SET balance = balance + ?, last_interest_date = CURDATE() 
                        WHERE account_no = ?
                        """;

                    try (Connection conn = DBUtil.getConnection();
                         PreparedStatement ps = conn.prepareStatement(updateSql)) {

                        ps.setBigDecimal(1, interest);
                        ps.setInt(2, accountNo);
                        ps.executeUpdate();

                        addTransaction(accountNo, "INTEREST", interest,
                                "Monthly interest credit", null);

                        auditLogger.log("INTEREST_APPLIED",
                                "₹" + interest + " interest applied to account " + accountNo, null, accountNo);

                        System.out.println("Interest of ₹" + interest + " has been credited to your account.");

                    } catch (SQLException e) {
                        System.out.println("Error applying interest: " + e.getMessage());
                    }
                }
            }
        }

        /**
         * Check daily withdrawal/transfer limit
         */
        private boolean checkDailyLimit(int accountNo, double amount) {
            Account account = findAccount(accountNo);
            if (account == null) return false;

            LocalDate today = LocalDate.now();

            // Reset daily limit if it's a new day
            if (!today.equals(account.getLastDailyReset())) {
                String resetSql = "UPDATE accounts SET last_daily_reset = CURDATE() WHERE account_no = ?";
                try (Connection conn = DBUtil.getConnection();
                     PreparedStatement ps = conn.prepareStatement(resetSql)) {
                    ps.setInt(1, accountNo);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    System.out.println("Error resetting daily limit: " + e.getMessage());
                }
            }

            // Get today's withdrawal/transfer total
            String sql = """
                SELECT COALESCE(SUM(amount), 0) as daily_total 
                FROM transactions 
                WHERE account_no = ? AND (type = 'WITHDRAWAL' OR type = 'TRANSFER_OUT') 
                AND DATE(txn_time) = CURDATE()
                """;

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, accountNo);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    BigDecimal dailyTotal = rs.getBigDecimal("daily_total");
                    BigDecimal newTotal = dailyTotal.add(BigDecimal.valueOf(amount));

                    return newTotal.compareTo(account.getDailyLimit()) <= 0;
                }

            } catch (SQLException e) {
                System.out.println("Error checking daily limit: " + e.getMessage());
            }

            return false;
        }

        /**
         * Add transaction record
         */
        private void addTransaction(int accountNo, String type, BigDecimal amount,
                                    String memo, Integer reverseOf) {
            try (Connection conn = DBUtil.getConnection()) {
                addTransaction(conn, accountNo, type, amount, memo, reverseOf);
            } catch (SQLException e) {
                System.out.println("Error adding transaction: " + e.getMessage());
            }
        }

        private void addTransaction(Connection conn, int accountNo, String type, BigDecimal amount,
                                    String memo, Integer reverseOf) throws SQLException {
            String sql = """
                INSERT INTO transactions (account_no, type, amount, memo, txn_time, reverse_of)
                VALUES (?, ?, ?, ?, NOW(), ?)
                """;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, accountNo);
                ps.setString(2, type);
                ps.setBigDecimal(3, amount);
                ps.setString(4, memo);
                if (reverseOf != null) {
                    ps.setInt(5, reverseOf);
                } else {
                    ps.setNull(5, Types.INTEGER);
                }
                ps.executeUpdate();
            }
        }

        /**
         * Show mini statement
         */
        public void showMiniStatement(int accountNo) {
            String sql = """
                SELECT type, amount, memo, txn_time 
                FROM transactions 
                WHERE account_no = ? 
                ORDER BY txn_time DESC 
                LIMIT 10
                """;

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, accountNo);
                ResultSet rs = ps.executeQuery();

                Account account = findAccount(accountNo);
                if (account != null) {
                    System.out.println("═══════════════════════════════════════");
                    System.out.println("           MINI STATEMENT");
                    System.out.println("Account: " + accountNo + " | " + account.getHolderName());
                    System.out.println("Current Balance: ₹" + account.getBalance());
                    System.out.println("═══════════════════════════════════════");
                    System.out.printf("%-15s %-12s %-25s %s%n", "Type", "Amount", "Description", "Date/Time");
                    System.out.println("─────────────────────────────────────────");

                    boolean hasTransactions = false;
                    while (rs.next()) {
                        hasTransactions = true;
                        System.out.printf("%-15s ₹%-11.2f %-25s %s%n",
                                rs.getString("type"),
                                rs.getBigDecimal("amount").doubleValue(),
                                rs.getString("memo"),
                                rs.getTimestamp("txn_time").toLocalDateTime().format(DATE_FMT)
                        );
                    }

                    if (!hasTransactions) {
                        System.out.println("No transactions found.");
                    }

                    System.out.println("═══════════════════════════════════════");
                }

            } catch (SQLException e) {
                System.out.println("Error retrieving mini statement: " + e.getMessage());
            }
        }

        /**
         * Show detailed transaction history
         */
        public void showTransactionHistory(int accountNo, int limit) {
            String sql = """
                SELECT t.type, t.amount, t.memo, t.txn_time, t.reverse_of
                FROM transactions t
                WHERE t.account_no = ? 
                ORDER BY t.txn_time DESC 
                LIMIT ?
                """;

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, accountNo);
                ps.setInt(2, limit);
                ResultSet rs = ps.executeQuery();

                Account account = findAccount(accountNo);
                if (account != null) {
                    System.out.println("═══════════════════════════════════════");
                    System.out.println("        TRANSACTION HISTORY");
                    System.out.println("Account: " + accountNo + " | " + account.getHolderName());
                    System.out.println("Current Balance: ₹" + account.getBalance());
                    System.out.println("═══════════════════════════════════════");
                    System.out.printf("%-15s %-12s %-30s %-20s %s%n",
                            "Type", "Amount", "Description", "Date/Time", "Notes");
                    System.out.println("────────────────────────────────────────────────────────────────────────────");

                    boolean hasTransactions = false;
                    while (rs.next()) {
                        hasTransactions = true;
                        String notes = "";
                        Integer reverseOf = rs.getObject("reverse_of", Integer.class);
                        if (reverseOf != null) {
                            notes = "(Reversal of #" + reverseOf + ")";
                        }

                        System.out.printf("%-15s ₹%-11.2f %-30s %-20s %s%n",
                                rs.getString("type"),
                                rs.getBigDecimal("amount").doubleValue(),
                                rs.getString("memo"),
                                rs.getTimestamp("txn_time").toLocalDateTime().format(DATE_FMT),
                                notes
                        );
                    }

                    if (!hasTransactions) {
                        System.out.println("No transactions found.");
                    }

                    System.out.println("════════════════════════════════════════════════════════════════════════════");
                }

            } catch (SQLException e) {
                System.out.println("Error retrieving transaction history: " + e.getMessage());
            }
        }

        /**
         * Get account summary
         */
        public void showAccountSummary(int accountNo) {
            Account account = findAccount(accountNo);
            if (account == null) {
                System.out.println("Account not found.");
                return;
            }

            // Get transaction counts and totals
            String sql = """
                SELECT
                    COUNT(*) as total_transactions,
                    COALESCE(SUM(CASE WHEN type = 'DEPOSIT' OR type = 'TRANSFER_IN' OR type = 'INTEREST' THEN amount ELSE 0 END), 0) as total_credits,
                    COALESCE(SUM(CASE WHEN type = 'WITHDRAWAL' OR type = 'TRANSFER_OUT' THEN amount ELSE 0 END), 0) as total_debits
                FROM transactions
                WHERE account_no = ?
                """;

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, accountNo);
                ResultSet rs = ps.executeQuery();

                System.out.println("═══════════════════════════════════════");
                System.out.println("         ACCOUNT SUMMARY");
                System.out.println("═══════════════════════════════════════");
                System.out.println("Account Number    : " + account.getAccountNo());
                System.out.println("Holder Name       : " + account.getHolderName());
                System.out.println("Account Type      : " + account.getAccountType());
                System.out.println("Current Balance   : ₹" + account.getBalance());
                System.out.println("Account Status    : " + (account.isActive() ? "Active" : "Inactive"));
                System.out.println("Lock Status       : " + (account.isLocked() ? "Locked" : "Unlocked"));
                System.out.println("Daily Limit       : ₹" + account.getDailyLimit());
                System.out.println("Last Interest Date: " +
                        (account.getLastInterestDate() != null ? account.getLastInterestDate().format(SIMPLE_DATE_FMT) : "Never"));

                if (rs.next()) {
                    System.out.println("─────────────────────────────────────────");
                    System.out.println("TRANSACTION SUMMARY:");
                    System.out.println("Total Transactions: " + rs.getInt("total_transactions"));
                    System.out.println("Total Credits     : ₹" + rs.getBigDecimal("total_credits"));
                    System.out.println("Total Debits      : ₹" + rs.getBigDecimal("total_debits"));
                }

                System.out.println("═══════════════════════════════════════");

            } catch (SQLException e) {
                System.out.println("Error retrieving account summary: " + e.getMessage());
            }
        }

        /**
         * Map ResultSet to Account object
         */
        private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
            Account account = new Account();
            account.setAccountNo(rs.getInt("account_no"));
            account.setHolderName(rs.getString("holder_name"));
            account.setAccountType(rs.getString("account_type"));
            account.setBalance(rs.getBigDecimal("balance"));
            account.setActive(rs.getBoolean("active"));
            account.setPinHash(rs.getString("pin_hash"));

            Date lastInterestDate = rs.getDate("last_interest_date");
            if (lastInterestDate != null) {
                account.setLastInterestDate(lastInterestDate.toLocalDate());
            }

            account.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
            account.setLocked(rs.getBoolean("locked"));
            account.setDailyLimit(rs.getBigDecimal("daily_limit"));

            Date lastDailyReset = rs.getDate("last_daily_reset");
            if (lastDailyReset != null) {
                account.setLastDailyReset(lastDailyReset.toLocalDate());
            }

            return account;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //                     SCHEDULED MANAGER
    // ═══════════════════════════════════════════════════════════════

    /**
     * Manages scheduled transactions
     */
    static class ScheduledManager {

        private final AccountManager accountManager;
        private final AuditLogger auditLogger;

        public ScheduledManager(AccountManager accountManager, AuditLogger auditLogger) {
            this.accountManager = accountManager;
            this.auditLogger = auditLogger;
        }

        /**
         * Schedule a transaction
         */
        public boolean scheduleTransaction(int accountNo, String type, double amount,
                                           String memo, LocalDate scheduleDate) {
            if (!isValidTransactionType(type)) {
                System.out.println("Invalid transaction type for scheduling.");
                return false;
            }

            String sql = """
                INSERT INTO scheduled_transactions (account_no, type, amount, memo, schedule_date, executed)
                VALUES (?, ?, ?, ?, ?, FALSE)
                """;

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, accountNo);
                ps.setString(2, type);
                ps.setBigDecimal(3, BigDecimal.valueOf(amount));
                ps.setString(4, memo);
                ps.setDate(5, Date.valueOf(scheduleDate));

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    auditLogger.log("TRANSACTION_SCHEDULED",
                            type + " of ₹" + amount + " scheduled for " + scheduleDate +
                                    " for account " + accountNo, null, accountNo);
                    return true;
                }

            } catch (SQLException e) {
                System.out.println("Error scheduling transaction: " + e.getMessage());
            }

            return false;
        }

        /**
         * Apply due scheduled transactions for all accounts
         */
        public void applyDueScheduledTransactions() {
            String sql = """
                SELECT id, account_no, type, amount, memo, schedule_date
                FROM scheduled_transactions 
                WHERE executed = FALSE AND schedule_date <= CURDATE()
                ORDER BY schedule_date
                """;

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    int id = rs.getInt("id");
                    int accountNo = rs.getInt("account_no");
                    String type = rs.getString("type");
                    BigDecimal amount = rs.getBigDecimal("amount");
                    String memo = rs.getString("memo");
                    LocalDate scheduleDate = rs.getDate("schedule_date").toLocalDate();

                    if (executeScheduledTransaction(id, accountNo, type, amount.doubleValue(),
                            memo, scheduleDate)) {
                        markScheduledTransactionExecuted(id);
                    }
                }

            } catch (SQLException e) {
                System.out.println("Error applying scheduled transactions: " + e.getMessage());
            }
        }

        /**
         * Apply due scheduled transactions for specific account
         */
        public void applyDueScheduledTransactionsForAccount(int accountNo) {
            String sql = """
                SELECT id, type, amount, memo, schedule_date
                FROM scheduled_transactions 
                WHERE account_no = ? AND executed = FALSE AND schedule_date <= CURDATE()
                ORDER BY schedule_date
                """;

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, accountNo);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String type = rs.getString("type");
                    BigDecimal amount = rs.getBigDecimal("amount");
                    String memo = rs.getString("memo");
                    LocalDate scheduleDate = rs.getDate("schedule_date").toLocalDate();

                    if (executeScheduledTransaction(id, accountNo, type, amount.doubleValue(),
                            memo, scheduleDate)) {
                        markScheduledTransactionExecuted(id);
                        System.out.println("Scheduled " + type.toLowerCase() + " of ₹" + amount +
                                " has been processed.");
                    }
                }

            } catch (SQLException e) {
                System.out.println("Error applying scheduled transactions for account: " + e.getMessage());
            }
        }

        /**
         * Execute a scheduled transaction
         */
        private boolean executeScheduledTransaction(int scheduledId, int accountNo, String type,
                                                    double amount, String memo, LocalDate scheduleDate) {
            boolean success = false;

            switch (type.toUpperCase()) {
                case "DEPOSIT":
                    success = accountManager.deposit(accountNo, amount);
                    break;
                case "WITHDRAWAL":
                    success = accountManager.withdraw(accountNo, amount);
                    break;
                default:
                    System.out.println("Unsupported scheduled transaction type: " + type);
                    return false;
            }

            if (success) {
                auditLogger.log("SCHEDULED_TRANSACTION_EXECUTED",
                        "Scheduled " + type + " of ₹" + amount + " executed for account " + accountNo,
                        null, accountNo);
            } else {
                auditLogger.log("SCHEDULED_TRANSACTION_FAILED",
                        "Scheduled " + type + " of ₹" + amount + " failed for account " + accountNo,
                        null, accountNo);
            }

            return success;
        }

        /**
         * Mark scheduled transaction as executed
         */
        private void markScheduledTransactionExecuted(int scheduledId) {
            String sql = "UPDATE scheduled_transactions SET executed = TRUE WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, scheduledId);
                ps.executeUpdate();

            } catch (SQLException e) {
                System.out.println("Error marking scheduled transaction as executed: " + e.getMessage());
            }
        }

        /**
         * View pending scheduled transactions for account
         */
        public void viewPendingScheduledTransactions(int accountNo) {
            String sql = """
                SELECT id, type, amount, memo, schedule_date
                FROM scheduled_transactions 
                WHERE account_no = ? AND executed = FALSE
                ORDER BY schedule_date
                """;

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, accountNo);
                ResultSet rs = ps.executeQuery();

                System.out.println("═══════════════════════════════════════");
                System.out.println("      PENDING SCHEDULED TRANSACTIONS");
                System.out.println("═══════════════════════════════════════");
                System.out.printf("%-5s %-12s %-12s %-25s %s%n", "ID", "Type", "Amount", "Description", "Date");
                System.out.println("─────────────────────────────────────────────────────────────────");

                boolean hasPending = false;
                while (rs.next()) {
                    hasPending = true;
                    System.out.printf("%-5d %-12s ₹%-11.2f %-25s %s%n",
                            rs.getInt("id"),
                            rs.getString("type"),
                            rs.getBigDecimal("amount").doubleValue(),
                            rs.getString("memo"),
                            rs.getDate("schedule_date").toLocalDate().format(SIMPLE_DATE_FMT)
                    );
                }

                if (!hasPending) {
                    System.out.println("No pending scheduled transactions.");
                }

                System.out.println("═════════════════════════════════════════════════════════════════");

            } catch (SQLException e) {
                System.out.println("Error viewing pending scheduled transactions: " + e.getMessage());
            }
        }

        /**
         * Cancel a scheduled transaction
         */
        public boolean cancelScheduledTransaction(int scheduledId, int accountNo) {
            String sql = "DELETE FROM scheduled_transactions WHERE id = ? AND account_no = ? AND executed = FALSE";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, scheduledId);
                ps.setInt(2, accountNo);

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    auditLogger.log("SCHEDULED_TRANSACTION_CANCELLED",
                            "Scheduled transaction #" + scheduledId + " cancelled for account " + accountNo,
                            null, accountNo);
                    return true;
                }

            } catch (SQLException e) {
                System.out.println("Error cancelling scheduled transaction: " + e.getMessage());
            }

            return false;
        }

        private boolean isValidTransactionType(String type) {
            return type != null && (type.equalsIgnoreCase("DEPOSIT") || type.equalsIgnoreCase("WITHDRAWAL"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //                        ADMIN MANAGER
    // ═══════════════════════════════════════════════════════════════
    /*
     * Author: Samruddha Belsare
     * Updated: 07-Oct-2025
     */
    /**
     * Administrative functions
     */
    static class AdminManager {

        private final AccountManager accountManager;
        private final AuditLogger auditLogger;

        public AdminManager(AccountManager accountManager, AuditLogger auditLogger) {
            this.accountManager = accountManager;
            this.auditLogger = auditLogger;
        }

        /**
         * Admin login flow
         */
        public void adminLoginFlow() {
            System.out.println("═══════════════════════════════════════");
            System.out.println("           ADMIN LOGIN");
            System.out.println("═══════════════════════════════════════");

            String username = InputUtil.getString("Admin Username: ");
            String password = InputUtil.getString("Admin Password: ");

            if (authenticateAdmin(username, password)) {
                System.out.println("Admin login successful!");
                auditLogger.log("ADMIN_LOGIN_SUCCESS", "Admin " + username + " logged in");
                adminMenu();
            } else {
                System.out.println("Invalid admin credentials.");
                auditLogger.log("ADMIN_LOGIN_FAILED", "Failed admin login attempt for " + username);
            }
        }

        /**
         * Admin menu
         */
        private void adminMenu() {
            while (true) {
                System.out.println("\n═══════════════════════════════════════");
                System.out.println("             ADMIN PANEL");
                System.out.println("═══════════════════════════════════════");
                System.out.println("1. View All Accounts");
                System.out.println("2. Lock/Unlock Account");
                System.out.println("3. View Account Details");
                System.out.println("4. System Statistics");
                System.out.println("5. View Audit Log");
                System.out.println("6. Generate Reports");
                System.out.println("7. Manage Daily Limits");
                System.out.println("8. Force Apply Interest");
                System.out.println("9. Logout");

                int choice = InputUtil.getInt("Choose option: ");

                switch (choice) {
                    case 1 -> viewAllAccounts();
                    case 2 -> manageAccountLock();
                    case 3 -> viewAccountDetails();
                    case 4 -> showSystemStatistics();
                    case 5 -> auditLogger.viewAuditLog(50);
                    case 6 -> generateReports();
                    case 7 -> manageDailyLimits();
                    case 8 -> forceApplyInterest();
                    case 9 -> {
                        auditLogger.log("ADMIN_LOGOUT", "Admin logged out");
                        return;
                    }
                    default -> System.out.println("Invalid option.");
                }
            }
        }

        /**
         * Authenticate admin user
         */
        private boolean authenticateAdmin(String username, String password) {
            String sql = "SELECT password_hash FROM users WHERE username = ? AND role = 'ADMIN'";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    // Simple hash comparison (in production, use proper password hashing)
                    return storedHash.equals(SecurityUtil.hashPin(password));
                }

            } catch (SQLException e) {
                System.out.println("Error authenticating admin: " + e.getMessage());
            }

            return false;
        }

        /**
         * View all accounts
         */
        private void viewAllAccounts() {
            String sql = """
                SELECT account_no, holder_name, account_type, balance, active, locked, 
                       failed_login_attempts, daily_limit
                FROM accounts 
                ORDER BY account_no
                """;

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ResultSet rs = ps.executeQuery();

                System.out.println("═══════════════════════════════════════════════════════════════════════════");
                System.out.println("                              ALL ACCOUNTS");
                System.out.println("═══════════════════════════════════════════════════════════════════════════");
                System.out.printf("%-8s %-20s %-10s %-12s %-8s %-8s %-8s %-12s%n",
                        "Acc No", "Name", "Type", "Balance", "Active", "Locked", "Attempts", "Daily Limit");
                System.out.println("───────────────────────────────────────────────────────────────────────────");

                while (rs.next()) {
                    System.out.printf("%-8d %-20s %-10s ₹%-11.2f %-8s %-8s %-8d ₹%-11.2f%n",
                            rs.getInt("account_no"),
                            rs.getString("holder_name"),
                            rs.getString("account_type"),
                            rs.getBigDecimal("balance").doubleValue(),
                            rs.getBoolean("active") ? "Yes" : "No",
                            rs.getBoolean("locked") ? "Yes" : "No",
                            rs.getInt("failed_login_attempts"),
                            rs.getBigDecimal("daily_limit").doubleValue()
                    );
                }

                System.out.println("═══════════════════════════════════════════════════════════════════════════");

            } catch (SQLException e) {
                System.out.println("Error viewing accounts: " + e.getMessage());
            }
        }

        /**
         * Manage account lock/unlock
         */
        private void manageAccountLock() {
            int accountNo = InputUtil.getInt("Enter account number: ");
            Account account = accountManager.findAccount(accountNo);

            if (account == null) {
                System.out.println("Account not found.");
                return;
            }

            System.out.println("Account " + accountNo + " (" + account.getHolderName() + ")");
            System.out.println("Current status: " + (account.isLocked() ? "Locked" : "Unlocked"));

            String action = InputUtil.getString("Enter action (lock/unlock): ");

            boolean newLockStatus;
            if ("lock".equalsIgnoreCase(action)) {
                newLockStatus = true;
            } else if ("unlock".equalsIgnoreCase(action)) {
                newLockStatus = false;
            } else {
                System.out.println("Invalid action.");
                return;
            }

            String sql = "UPDATE accounts SET locked = ?, failed_login_attempts = 0 WHERE account_no = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setBoolean(1, newLockStatus);
                ps.setInt(2, accountNo);

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    String actionText = newLockStatus ? "locked" : "unlocked";
                    System.out.println("Account " + accountNo + " has been " + actionText + ".");
                    auditLogger.log("ACCOUNT_" + actionText.toUpperCase(),
                            "Account " + accountNo + " " + actionText + " by admin", null, accountNo);
                }

            } catch (SQLException e) {
                System.out.println("Error updating account lock status: " + e.getMessage());
            }
        }

        /**
         * View detailed account information
         */
        private void viewAccountDetails() {
            int accountNo = InputUtil.getInt("Enter account number: ");
            accountManager.showAccountSummary(accountNo);
        }

        /**
         * Show system statistics
         */
        private void showSystemStatistics() {
            String sql = """
                SELECT 
                    COUNT(*) as total_accounts,
                    COUNT(CASE WHEN active = TRUE THEN 1 END) as active_accounts,
                    COUNT(CASE WHEN locked = TRUE THEN 1 END) as locked_accounts,
                    COALESCE(SUM(balance), 0) as total_balance,
                    COUNT(CASE WHEN account_type = 'Savings' THEN 1 END) as savings_accounts,
                    COUNT(CASE WHEN account_type = 'Checking' THEN 1 END) as checking_accounts,
                    COUNT(CASE WHEN account_type = 'Business' THEN 1 END) as business_accounts
                FROM accounts
                """;

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    System.out.println("═══════════════════════════════════════");
                    System.out.println("         SYSTEM STATISTICS");
                    System.out.println("═══════════════════════════════════════");
                    System.out.println("Total Accounts     : " + rs.getInt("total_accounts"));
                    System.out.println("Active Accounts    : " + rs.getInt("active_accounts"));
                    System.out.println("Locked Accounts    : " + rs.getInt("locked_accounts"));
                    System.out.println("Total Balance      : ₹" + rs.getBigDecimal("total_balance"));
                    System.out.println();
                    System.out.println("Account Types:");
                    System.out.println("  Savings          : " + rs.getInt("savings_accounts"));
                    System.out.println("  Checking         : " + rs.getInt("checking_accounts"));
                    System.out.println("  Business         : " + rs.getInt("business_accounts"));
                    System.out.println("═══════════════════════════════════════");
                }

                // Transaction statistics
                String txnSql = """
                    SELECT 
                        COUNT(*) as total_transactions,
                        COUNT(CASE WHEN type = 'DEPOSIT' THEN 1 END) as deposits,
                        COUNT(CASE WHEN type = 'WITHDRAWAL' THEN 1 END) as withdrawals,
                        COUNT(CASE WHEN type LIKE 'TRANSFER%' THEN 1 END) as transfers
                    FROM transactions
                    WHERE DATE(txn_time) = CURDATE()
                    """;

                try (PreparedStatement txnPs = conn.prepareStatement(txnSql)) {
                    ResultSet txnRs = txnPs.executeQuery();

                    if (txnRs.next()) {
                        System.out.println("TODAY'S TRANSACTIONS:");
                        System.out.println("  Total            : " + txnRs.getInt("total_transactions"));
                        System.out.println("  Deposits         : " + txnRs.getInt("deposits"));
                        System.out.println("  Withdrawals      : " + txnRs.getInt("withdrawals"));
                        System.out.println("  Transfers        : " + txnRs.getInt("transfers"));
                        System.out.println("═══════════════════════════════════════");
                    }
                }

            } catch (SQLException e) {
                System.out.println("Error retrieving system statistics: " + e.getMessage());
            }
        }

        /**
         * Generate reports
         */
        private void generateReports() {
            System.out.println("═══════════════════════════════════════");
            System.out.println("           REPORT GENERATION");
            System.out.println("═══════════════════════════════════════");
            System.out.println("1. Daily Transaction Report");
            System.out.println("2. Weekly Transaction Report");
            System.out.println("3. Account Balance Report");
            System.out.println("4. Dormant Accounts Report");

            int choice = InputUtil.getInt("Choose report: ");

            switch (choice) {
                case 1 -> generateDailyTransactionReport();
                case 2 -> generateWeeklyTransactionReport();
                case 3 -> generateAccountBalanceReport();
                case 4 -> generateDormantAccountsReport();
                default -> System.out.println("Invalid option.");
            }
        }

        private void generateDailyTransactionReport() {
            String sql = """
                SELECT 
                    type, 
                    COUNT(*) as count, 
                    COALESCE(SUM(amount), 0) as total
                FROM transactions 
                WHERE DATE(txn_time) = CURDATE()
                GROUP BY type
                ORDER BY type
                """;

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ResultSet rs = ps.executeQuery();

                System.out.println("═══════════════════════════════════════");
                System.out.println("      DAILY TRANSACTION REPORT");
                System.out.println("      Date: " + LocalDate.now().format(SIMPLE_DATE_FMT));
                System.out.println("═══════════════════════════════════════");
                System.out.printf("%-15s %-10s %s%n", "Type", "Count", "Total Amount");
                System.out.println("─────────────────────────────────────────");

                while (rs.next()) {
                    System.out.printf("%-15s %-10d ₹%.2f%n",
                            rs.getString("type"),
                            rs.getInt("count"),
                            rs.getBigDecimal("total").doubleValue()
                    );
                }

                System.out.println("═══════════════════════════════════════");

            } catch (SQLException e) {
                System.out.println("Error generating daily transaction report: " + e.getMessage());
            }
        }

        private void generateWeeklyTransactionReport() {
            // Implementation for weekly report
            System.out.println("Weekly transaction report - Feature coming soon!");
        }

        private void generateAccountBalanceReport() {
            String sql = """
                SELECT 
                    account_type,
                    COUNT(*) as count,
                    COALESCE(AVG(balance), 0) as avg_balance,
                    COALESCE(SUM(balance), 0) as total_balance,
                    COALESCE(MIN(balance), 0) as min_balance,
                    COALESCE(MAX(balance), 0) as max_balance
                FROM accounts
                WHERE active = TRUE
                GROUP BY account_type
                ORDER BY account_type
                """;

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ResultSet rs = ps.executeQuery();

                System.out.println("═══════════════════════════════════════════════════════════════════");
                System.out.println("                        ACCOUNT BALANCE REPORT");
                System.out.println("═══════════════════════════════════════════════════════════════════");
                System.out.printf("%-10s %-8s %-12s %-12s %-12s %s%n",
                        "Type", "Count", "Avg Balance", "Total", "Min", "Max");
                System.out.println("───────────────────────────────────────────────────────────────────");

                while (rs.next()) {
                    System.out.printf("%-10s %-8d ₹%-11.2f ₹%-11.2f ₹%-11.2f ₹%.2f%n",
                            rs.getString("account_type"),
                            rs.getInt("count"),
                            rs.getBigDecimal("avg_balance").doubleValue(),
                            rs.getBigDecimal("total_balance").doubleValue(),
                            rs.getBigDecimal("min_balance").doubleValue(),
                            rs.getBigDecimal("max_balance").doubleValue()
                    );
                }

                System.out.println("═══════════════════════════════════════════════════════════════════");

            } catch (SQLException e) {
                System.out.println("Error generating balance report: " + e.getMessage());
            }
        }

        private void generateDormantAccountsReport() {
            String sql = """
                SELECT DISTINCT a.account_no, a.holder_name, a.account_type, a.balance,
                       COALESCE(MAX(t.txn_time), 'Never') as last_transaction
                FROM accounts a
                LEFT JOIN transactions t ON a.account_no = t.account_no
                WHERE a.active = TRUE
                GROUP BY a.account_no, a.holder_name, a.account_type, a.balance
                HAVING last_transaction < DATE_SUB(NOW(), INTERVAL 90 DAY) OR last_transaction = 'Never'
                ORDER BY last_transaction
                """;

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ResultSet rs = ps.executeQuery();

                System.out.println("═══════════════════════════════════════════════════════════════════");
                System.out.println("              DORMANT ACCOUNTS REPORT");
                System.out.println("              (No activity for 90+ days)");
                System.out.println("═══════════════════════════════════════════════════════════════════");
                System.out.printf("%-8s %-20s %-10s %-12s %s%n",
                        "Acc No", "Name", "Type", "Balance", "Last Transaction");
                System.out.println("───────────────────────────────────────────────────────────────────");

                boolean hasDormant = false;
                while (rs.next()) {
                    hasDormant = true;
                    Object lastTxn = rs.getObject("last_transaction");
                    String lastTxnStr = lastTxn instanceof Timestamp ?
                            ((Timestamp) lastTxn).toLocalDateTime().format(SIMPLE_DATE_FMT) :
                            lastTxn.toString();

                    System.out.printf("%-8d %-20s %-10s ₹%-11.2f %s%n",
                            rs.getInt("account_no"),
                            rs.getString("holder_name"),
                            rs.getString("account_type"),
                            rs.getBigDecimal("balance").doubleValue(),
                            lastTxnStr
                    );
                }

                if (!hasDormant) {
                    System.out.println("No dormant accounts found.");
                }

                System.out.println("═══════════════════════════════════════════════════════════════════");

            } catch (SQLException e) {
                System.out.println("Error generating dormant accounts report: " + e.getMessage());
            }
        }

        /**
         * Manage daily limits
         */
        private void manageDailyLimits() {
            int accountNo = InputUtil.getInt("Enter account number: ");
            Account account = accountManager.findAccount(accountNo);

            if (account == null) {
                System.out.println("Account not found.");
                return;
            }

            System.out.println("Account: " + accountNo + " (" + account.getHolderName() + ")");
            System.out.println("Current daily limit: ₹" + account.getDailyLimit());

            double newLimit = InputUtil.getDouble("Enter new daily limit: ");

            String sql = "UPDATE accounts SET daily_limit = ? WHERE account_no = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setBigDecimal(1, BigDecimal.valueOf(newLimit));
                ps.setInt(2, accountNo);

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("Daily limit updated successfully.");
                    auditLogger.log("DAILY_LIMIT_CHANGED",
                            "Daily limit for account " + accountNo + " changed to ₹" + newLimit,
                            null, accountNo);
                }

            } catch (SQLException e) {
                System.out.println("Error updating daily limit: " + e.getMessage());
            }
        }

        /**
         * Force apply interest to all accounts
         */
        private void forceApplyInterest() {
            String confirmation = InputUtil.getString("Are you sure you want to apply interest to all accounts? (yes/no): ");

            if (!"yes".equalsIgnoreCase(confirmation)) {
                System.out.println("Operation cancelled.");
                return;
            }

            String sql = "SELECT account_no FROM accounts WHERE active = TRUE";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ResultSet rs = ps.executeQuery();
                int processed = 0;

                while (rs.next()) {
                    int accountNo = rs.getInt("account_no");
                    accountManager.applyInterestIfDue(accountNo);
                    processed++;
                }

                System.out.println("Interest processing completed for " + processed + " accounts.");
                auditLogger.log("FORCE_INTEREST_APPLICATION",
                        "Admin forced interest application for " + processed + " accounts");

            } catch (SQLException e) {
                System.out.println("Error applying interest: " + e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //                        MAIN MENU SYSTEM
    // ═══════════════════════════════════════════════════════════════

    /**
     * Main menu controller
     */
    static class MainMenuController {

        private final AccountManager accountManager;
        private final ScheduledManager scheduledManager;
        private final AdminManager adminManager;
        private final AuditLogger auditLogger;

        public MainMenuController() {
            this.auditLogger = new AuditLogger();
            this.accountManager = new AccountManager(auditLogger);
            this.scheduledManager = new ScheduledManager(accountManager, auditLogger);
            this.adminManager = new AdminManager(accountManager, auditLogger);
        }
        /*
         * Author: Samruddha Belsare
         * Updated: 07-Oct-2025
         */
        /**
         * Main menu loop
         */
        public void start() {
            System.out.println("═══════════════════════════════════════");
            System.out.println("    ADVANCED BANKING SYSTEM v2.0");
            System.out.println("═══════════════════════════════════════");

            // Apply any due scheduled transactions at startup
            scheduledManager.applyDueScheduledTransactions();
            auditLogger.log("SYSTEM_STARTUP", "Banking system started");

            while (true) {
                showMainMenu();
                int choice = InputUtil.getInt("Choose option: ");

                switch (choice) {
                    case 1 -> createAccountFlow();
                    case 2 -> userLoginFlow();
                    case 3 -> adminManager.adminLoginFlow();
                    case 4 -> {
                        System.out.println("Thank you for using Advanced Banking System!");
                        auditLogger.log("SYSTEM_SHUTDOWN", "Banking system shutdown");
                        return;
                    }
                    default -> System.out.println("Invalid option. Please try again.");
                }
            }
        }

        private void showMainMenu() {
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("             MAIN MENU");
            System.out.println("═══════════════════════════════════════");
            System.out.println("1. Create Account");
            System.out.println("2. User Login");
            System.out.println("3. Admin Panel");
            System.out.println("4. Exit");
            System.out.println("═══════════════════════════════════════");
        }

        /**
         * Account creation flow
         */
        private void createAccountFlow() {
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("          CREATE NEW ACCOUNT");
            System.out.println("═══════════════════════════════════════");

            String holderName = InputUtil.getString("Full Name: ");

            System.out.println("Account Types:");
            System.out.println("1. Savings (Interest: 4%, Limit: ₹20,000)");
            System.out.println("2. Checking (Interest: 2%, Limit: ₹15,000)");
            System.out.println("3. Business (Interest: 3%, Limit: ₹50,000)");
            System.out.println("4. Fixed (Interest: 4%, Limit: ₹20,000)");

            int typeChoice = InputUtil.getInt("Choose account type: ");
            String accountType = switch (typeChoice) {
                case 1 -> "Savings";
                case 2 -> "Checking";
                case 3 -> "Business";
                case 4 -> "Fixed";
                default -> {
                    System.out.println("Invalid choice. Defaulting to Savings.");
                    yield "Savings";
                }
            };

            double initialDeposit = InputUtil.getDouble("Initial deposit amount: ");
            String pin = InputUtil.getPin("Set 4-digit PIN: ");
            String confirmPin = InputUtil.getPin("Confirm PIN: ");

            if (!pin.equals(confirmPin)) {
                System.out.println("PINs do not match. Account creation cancelled.");
                return;
            }

            Account newAccount = accountManager.createAccount(holderName, accountType, initialDeposit, pin);

            if (newAccount != null) {
                System.out.println("\n✓ Account created successfully!");
                System.out.println("═══════════════════════════════════════");
                System.out.println("Account Number: " + newAccount.getAccountNo());
                System.out.println("Holder Name   : " + newAccount.getHolderName());
                System.out.println("Account Type  : " + newAccount.getAccountType());
                System.out.println("Initial Balance: ₹" + newAccount.getBalance());
                System.out.println("Daily Limit   : ₹" + newAccount.getDailyLimit());
                System.out.println("═══════════════════════════════════════");
                System.out.println("Please note down your account number for future reference.");
            } else {
                System.out.println("✗ Account creation failed. Please try again.");
            }
        }

        /**
         * User login flow
         */
        private void userLoginFlow() {
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("             USER LOGIN");
            System.out.println("═══════════════════════════════════════");

            int accountNo = InputUtil.getInt("Account Number: ");
            String pin = InputUtil.getPin("PIN: ");

            Account account = accountManager.findAccount(accountNo);
            if (account == null) {
                System.out.println("Account not found.");
                return;
            }

            if (!account.isActive()) {
                System.out.println("Account is inactive. Please contact admin.");
                return;
            }

            if (account.isLocked()) {
                System.out.println("Account is locked due to multiple failed login attempts. Please contact admin.");
                return;
            }

            if (accountManager.verifyPin(accountNo, pin)) {
                System.out.println("\n✓ Login successful!");
                System.out.println("Welcome, " + account.getHolderName() + "!");

                accountManager.resetFailedLoginAttempts(accountNo);
                accountManager.applyInterestIfDue(accountNo);
                scheduledManager.applyDueScheduledTransactionsForAccount(accountNo);

                auditLogger.log("USER_LOGIN_SUCCESS", "User logged in", null, accountNo);

                userSession(accountNo);

            } else {
                boolean locked = accountManager.handleFailedLogin(accountNo);
                if (locked) {
                    System.out.println("✗ Invalid PIN. Account has been locked due to multiple failed attempts.");
                } else {
                    System.out.println("✗ Invalid PIN. Please try again.");
                }
            }
        }

        /**
         * User session menu
         */
        private void userSession(int accountNo) {
            while (true) {
                showUserMenu(accountNo);
                int choice = InputUtil.getInt("Choose option: ");

                switch (choice) {
                    case 1 -> showAccountBalance(accountNo);
                    case 2 -> processDeposit(accountNo);
                    case 3 -> processWithdrawal(accountNo);
                    case 4 -> processTransfer(accountNo);
                    case 5 -> accountManager.showMiniStatement(accountNo);
                    case 6 -> showTransactionHistory(accountNo);
                    case 7 -> accountManager.showAccountSummary(accountNo);
                    case 8 -> manageScheduledTransactions(accountNo);
                    case 9 -> {
                        auditLogger.log("USER_LOGOUT", "User logged out", null, accountNo);
                        System.out.println("Logged out successfully. Thank you!");
                        return;
                    }
                    default -> System.out.println("Invalid option. Please try again.");
                }
            }
        }

        private void showUserMenu(int accountNo) {
            Account account = accountManager.findAccount(accountNo);
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("            USER DASHBOARD");
            System.out.println("Account: " + accountNo + " | " +
                    (account != null ? account.getHolderName() : "Unknown"));
            System.out.println("═══════════════════════════════════════");
            System.out.println("1. Check Balance");
            System.out.println("2. Deposit Money");
            System.out.println("3. Withdraw Money");
            System.out.println("4. Transfer Money");
            System.out.println("5. Mini Statement");
            System.out.println("6. Transaction History");
            System.out.println("7. Account Summary");
            System.out.println("8. Scheduled Transactions");
            System.out.println("9. Logout");
            System.out.println("═══════════════════════════════════════");
        }

        private void showAccountBalance(int accountNo) {
            Account account = accountManager.findAccount(accountNo);
            if (account != null) {
                System.out.println("\n═══════════════════════════════════════");
                System.out.println("           ACCOUNT BALANCE");
                System.out.println("═══════════════════════════════════════");
                System.out.println("Current Balance: ₹" + account.getBalance());
                System.out.println("Account Type   : " + account.getAccountType());
                System.out.println("Daily Limit    : ₹" + account.getDailyLimit());
                System.out.println("═══════════════════════════════════════");
            }
        }

        private void processDeposit(int accountNo) {
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("            DEPOSIT MONEY");
            System.out.println("═══════════════════════════════════════");

            double amount = InputUtil.getDouble("Enter deposit amount: ₹");

            if (amount <= 0) {
                System.out.println("Invalid amount. Deposit cancelled.");
                return;
            }

            String confirmation = InputUtil.getString("Confirm deposit of ₹" + amount + "? (yes/no): ");

            if ("yes".equalsIgnoreCase(confirmation)) {
                if (accountManager.deposit(accountNo, amount)) {
                    System.out.println("✓ Deposit successful!");
                    showAccountBalance(accountNo);
                } else {
                    System.out.println("✗ Deposit failed. Please try again.");
                }
            } else {
                System.out.println("Deposit cancelled.");
            }
        }

        private void processWithdrawal(int accountNo) {
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("           WITHDRAW MONEY");
            System.out.println("═══════════════════════════════════════");

            showAccountBalance(accountNo);

            double amount = InputUtil.getDouble("Enter withdrawal amount: ₹");

            if (amount <= 0) {
                System.out.println("Invalid amount. Withdrawal cancelled.");
                return;
            }

            String confirmation = InputUtil.getString("Confirm withdrawal of ₹" + amount + "? (yes/no): ");

            if ("yes".equalsIgnoreCase(confirmation)) {
                if (accountManager.withdraw(accountNo, amount)) {
                    System.out.println("✓ Withdrawal successful!");
                    showAccountBalance(accountNo);
                } else {
                    System.out.println("✗ Withdrawal failed. Please check your balance and daily limit.");
                }
            } else {
                System.out.println("Withdrawal cancelled.");
            }
        }

        private void processTransfer(int accountNo) {
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("           TRANSFER MONEY");
            System.out.println("═══════════════════════════════════════");

            showAccountBalance(accountNo);

            int toAccount = InputUtil.getInt("Enter destination account number: ");

            if (toAccount == accountNo) {
                System.out.println("Cannot transfer to the same account.");
                return;
            }

            // Verify destination account exists
            Account destAccount = accountManager.findAccount(toAccount);
            if (destAccount == null) {
                System.out.println("Destination account not found.");
                return;
            }

            System.out.println("Destination: " + destAccount.getHolderName() +
                    " (Account: " + toAccount + ")");

            double amount = InputUtil.getDouble("Enter transfer amount: ₹");

            if (amount <= 0) {
                System.out.println("Invalid amount. Transfer cancelled.");
                return;
            }

            String confirmation = InputUtil.getString(
                    "Confirm transfer of ₹" + amount + " to " + destAccount.getHolderName() +
                            " (Account: " + toAccount + ")? (yes/no): ");

            if ("yes".equalsIgnoreCase(confirmation)) {
                if (accountManager.transfer(accountNo, toAccount, amount)) {
                    System.out.println("✓ Transfer successful!");
                    showAccountBalance(accountNo);
                } else {
                    System.out.println("✗ Transfer failed. Please check your balance and daily limit.");
                }
            } else {
                System.out.println("Transfer cancelled.");
            }
        }

        private void showTransactionHistory(int accountNo) {
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("        TRANSACTION HISTORY");
            System.out.println("═══════════════════════════════════════");
            System.out.println("1. Last 10 transactions");
            System.out.println("2. Last 25 transactions");
            System.out.println("3. Last 50 transactions");

            int choice = InputUtil.getInt("Choose option: ");
            int limit = switch (choice) {
                case 1 -> 10;
                case 2 -> 25;
                case 3 -> 50;
                default -> 10;
            };
            /*
             * Author: Samruddha Belsare
             * Updated: 07-Oct-2025
             */
            accountManager.showTransactionHistory(accountNo, limit);
        }

        private void manageScheduledTransactions(int accountNo) {
            while (true) {
                System.out.println("\n═══════════════════════════════════════");
                System.out.println("       SCHEDULED TRANSACTIONS");
                System.out.println("═══════════════════════════════════════");
                System.out.println("1. View Pending Scheduled Transactions");
                System.out.println("2. Schedule New Transaction");
                System.out.println("3. Cancel Scheduled Transaction");
                System.out.println("4. Back to Main Menu");

                int choice = InputUtil.getInt("Choose option: ");

                switch (choice) {
                    case 1 -> scheduledManager.viewPendingScheduledTransactions(accountNo);
                    case 2 -> scheduleNewTransaction(accountNo);
                    case 3 -> cancelScheduledTransaction(accountNo);
                    case 4 -> { return; }
                    default -> System.out.println("Invalid option.");
                }
            }
        }

        private void scheduleNewTransaction(int accountNo) {
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("       SCHEDULE NEW TRANSACTION");
            System.out.println("═══════════════════════════════════════");
            System.out.println("1. Schedule Deposit");
            System.out.println("2. Schedule Withdrawal");

            int type = InputUtil.getInt("Choose transaction type: ");
            String txnType;

            switch (type) {
                case 1 -> txnType = "DEPOSIT";
                case 2 -> txnType = "WITHDRAWAL";
                default -> {
                    System.out.println("Invalid type.");
                    return;
                }
            }

            double amount = InputUtil.getDouble("Enter amount: ₹");
            String memo = InputUtil.getString("Enter description: ");

            System.out.print("Enter schedule date (YYYY-MM-DD): ");
            String dateStr = sc.nextLine().trim();

            try {
                LocalDate scheduleDate = LocalDate.parse(dateStr);

                if (scheduleDate.isBefore(LocalDate.now())) {
                    System.out.println("Schedule date cannot be in the past.");
                    return;
                }

                if (scheduledManager.scheduleTransaction(accountNo, txnType, amount, memo, scheduleDate)) {
                    System.out.println("✓ Transaction scheduled successfully!");
                } else {
                    System.out.println("✗ Failed to schedule transaction.");
                }

            } catch (Exception e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            }
        }

        private void cancelScheduledTransaction(int accountNo) {
            scheduledManager.viewPendingScheduledTransactions(accountNo);

            int scheduledId = InputUtil.getInt("Enter ID of transaction to cancel (0 to go back): ");

            if (scheduledId == 0) {
                return;
            }

            String confirmation = InputUtil.getString("Are you sure you want to cancel this scheduled transaction? (yes/no): ");

            if ("yes".equalsIgnoreCase(confirmation)) {
                if (scheduledManager.cancelScheduledTransaction(scheduledId, accountNo)) {
                    System.out.println("✓ Scheduled transaction cancelled successfully!");
                } else {
                    System.out.println("✗ Failed to cancel scheduled transaction.");
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //                           MAIN METHOD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Application entry point
     */
    public static void main(String[] args) {
        try {
            // Test database connection
            try (Connection conn = DBUtil.getConnection()) {
                System.out.println("✓ Database connection successful!");
            }

            // Start the application
            MainMenuController controller = new MainMenuController();
            controller.start();

        } catch (SQLException e) {
            System.err.println("✗ Database connection failed!");
            System.err.println("Error: " + e.getMessage());
            System.err.println("\nPlease check:");
            System.err.println("1. MySQL server is running");
            System.err.println("2. Database 'banking_system' exists");
            System.err.println("3. Database credentials are correct");
            System.err.println("4. MySQL Connector/J is in classpath");

        } catch (Exception e) {
            System.err.println("✗ Application startup failed!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
/*
 * Author: Samruddha Belsare
 * Updated: 07-Oct-2025
 */