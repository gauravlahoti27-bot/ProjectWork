"""
models.py

Contains SQL queries for creating database tables.
No database operations are performed here.
"""

# -----------------------------
# Users Table
# -----------------------------
USER_TABLE = """
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT NOT NULL,
    branch TEXT,
    graduation_year INTEGER,
    company TEXT,
    designation TEXT,
    location TEXT,
    phone TEXT,
    bio TEXT
);
"""

# -----------------------------
# Jobs Table
# -----------------------------
JOB_TABLE = """
CREATE TABLE IF NOT EXISTS jobs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    company TEXT NOT NULL,
    location TEXT,
    description TEXT,
    posted_by INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
"""

# -----------------------------
# Events Table
# -----------------------------
EVENT_TABLE = """
CREATE TABLE IF NOT EXISTS events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    description TEXT,
    venue TEXT,
    event_date TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
"""

# -----------------------------
# Donations Table
# -----------------------------
DONATION_TABLE = """
CREATE TABLE IF NOT EXISTS donations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    donor_name TEXT,
    amount REAL,
    payment_status TEXT
);
"""