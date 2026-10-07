-- Fast Cash Sri Lanka D1 Database Schema

CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    reference TEXT NOT NULL UNIQUE,
    type TEXT NOT NULL, -- DEPOSIT or WITHDRAWAL
    status TEXT NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, REJECTED
    amount INTEGER NOT NULL,
    playerId TEXT NOT NULL,
    paymentMethod TEXT NOT NULL DEFAULT 'Bank transfer',
    securityCode TEXT,
    bank TEXT,
    accountNumber TEXT,
    contactNumber TEXT,
    receiptKey TEXT,
    rejectionReason TEXT,
    timestamp INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS agent_accounts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    label TEXT NOT NULL,
    accountNumber TEXT NOT NULL,
    method TEXT NOT NULL,
    isActive INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS notifications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    isRead INTEGER NOT NULL DEFAULT 0,
    createdAt INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS app_settings (
    id INTEGER PRIMARY KEY DEFAULT 1,
    whatsappNumber TEXT NOT NULL DEFAULT '+94765865387',
    minTransaction INTEGER NOT NULL DEFAULT 1000,
    maxTransaction INTEGER NOT NULL DEFAULT 500000,
    promoCode TEXT NOT NULL DEFAULT 'VGSL'
);

-- Seed initial agent accounts
INSERT OR IGNORE INTO agent_accounts (id, label, accountNumber, method, isActive) VALUES
(1, 'Bank of Ceylon (BOC) - Fast Cash LK', '8340192841', 'Bank transfer', 1),
(2, 'People''s Bank - Walasmulla Operations', '2049102941', 'Bank transfer', 1),
(3, 'Sampath Bank - Fast Cash Corporate', '1004819204', 'iPay / Bank', 1);

-- Seed app settings
INSERT OR IGNORE INTO app_settings (id, whatsappNumber, minTransaction, maxTransaction, promoCode) VALUES
(1, '+94765865387', 1000, 500000, 'VGSL');
