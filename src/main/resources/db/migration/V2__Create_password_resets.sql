-- 4. Create the Password Resets table (Free SMTP Token Infrastructure)
CREATE TABLE IF NOT EXISTS password_resets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    INDEX idx_token (token),
    INDEX idx_email (email)
);
