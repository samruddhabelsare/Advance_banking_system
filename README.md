Advance Banking System

A robust Java console-based banking application with full MySQL database integration.

Overview
Advance Banking System is a feature-rich, fully modular banking solution written in Java, designed for educational, prototyping, and small-scale production purposes. It backs all data using a MySQL relational database and provides modern security, transaction management, scheduled operations, and administrative control—entirely through a console interface.

Features
Account Management: Create, view, and manage accounts (Savings, Checking, Business, Fixed)
Secure Authentication: PIN-protected login, automatic account lockout after failed attempts
Transaction Handling: Deposit, withdrawal, transfer with balance/limit/validation
Interest Application: Applies monthly interest based on account type
Scheduled Transactions: Schedule future deposits/withdrawals, auto-execution on due date
Transaction History: View mini statements and full transaction records
Admin Panel: Lock/unlock accounts, view system stats, generate reports, audit log, change limits
Audit Logging: Tracks all operations for compliance and review
Reporting: Daily/weekly transaction reports, dormant account detection, balance summaries
Data Integrity: Full JDBC transaction handling for multistep operations

Technologies
Programming Language: Java (JDK 11+ recommended)
Database: MySQL (8.0+ recommended, tested on 5.7+)
Persistence: JDBC (MySQL Connector/J required)
Security: SHA-256 PIN/password hashing

Setup
Clone the Repository

git clone 

Create and Configure MySQL Database

Database name: banking_system
Run provided SQL table creation scripts (accounts, transactions, users, etc.)
Add MySQL Connector/J to Your Project
Download the JAR

For Maven:

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>9.4.0</version>
</dependency>

Configure Database Connection

Edit DB_USER and DB_PASS constants in the source file
Insert Admin User(s) into the users Table

sql

INSERT INTO users (username, full_name, password_hash, role) VALUES
('admin', 'System Administrator', SHA2('admin123', 256), 'ADMIN');
Compile and Run



javac BankingSystemSQLComplete.java
java BankingSystemSQLComplete


Quick Start
Create your first user account and admin user
Use menu options to make transactions, view account details, and test scheduled features
Access the admin panel for advanced controls and reporting


 SQL Table Creation 

 
Run each querry one by one to create tables required for program



CREATE TABLE accounts (
  account_no INT PRIMARY KEY AUTO_INCREMENT,
  holder_name VARCHAR(100) NOT NULL,
  account_type VARCHAR(50) NOT NULL,
  balance DOUBLE NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  pin_hash VARCHAR(255),
  last_interest_date DATE,
  failed_login_attempts INT DEFAULT 0,
  locked BOOLEAN NOT NULL DEFAULT FALSE,
  daily_limit DOUBLE DEFAULT 0,
  last_daily_reset DATE
);

CREATE TABLE transactions (
  id INT PRIMARY KEY AUTO_INCREMENT,
  account_no INT NOT NULL,
  type VARCHAR(50) NOT NULL,
  amount DOUBLE NOT NULL,
  memo VARCHAR(255),
  txn_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  reverse_of INT,
  FOREIGN KEY (account_no) REFERENCES accounts(account_no)
);

CREATE TABLE users (
  user_id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  full_name VARCHAR(100),
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('USER','ADMIN') DEFAULT 'USER'
);

CREATE TABLE scheduled_transactions (
  id INT PRIMARY KEY AUTO_INCREMENT,
  account_no INT NOT NULL,
  type VARCHAR(50) NOT NULL,
  amount DOUBLE NOT NULL,
  memo VARCHAR(255),
  schedule_date DATE,
  executed BOOLEAN DEFAULT FALSE,
  FOREIGN KEY (account_no) REFERENCES accounts(account_no)
);

CREATE TABLE audit_log (
  id INT PRIMARY KEY AUTO_INCREMENT,
  event_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  user_id INT,
  account_no INT,
  event_text VARCHAR(255),
  FOREIGN KEY (user_id) REFERENCES users(user_id),
  FOREIGN KEY (account_no) REFERENCES accounts(account_no)
);



Author :
Samruddha Belsare ,


License :
MIT License
