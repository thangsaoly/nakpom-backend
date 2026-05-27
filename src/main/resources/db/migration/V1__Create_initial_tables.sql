-- Create the database
CREATE DATABASE IF NOT EXISTS nakpom_db;
USE nakpom_db;

-- 1. Create the Users table
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email)
);

-- 2. Create the Families table (To hold automated spaces)
CREATE TABLE IF NOT EXISTS families (
    family_id INT AUTO_INCREMENT PRIMARY KEY,
    family_name VARCHAR(100) NOT NULL,
    invite_code VARCHAR(10) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_invite_code (invite_code)
);

-- 3. Create the Membership Link table
CREATE TABLE IF NOT EXISTS family_memberships (
    membership_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    family_id INT NOT NULL,
    role VARCHAR(20) DEFAULT 'member',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (family_id) REFERENCES families(family_id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_family (user_id, family_id),
    INDEX idx_user_id (user_id),
    INDEX idx_family_id (family_id)
);
