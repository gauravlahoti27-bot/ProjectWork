import sqlite3
from pathlib import Path
from flask import current_app

SCHEMA = '''
CREATE TABLE IF NOT EXISTS products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    brand TEXT DEFAULT 'Hot Wheels',
    product_url TEXT NOT NULL,
    provider TEXT NOT NULL DEFAULT 'demo',
    target_quantity INTEGER NOT NULL DEFAULT 1,
    enabled INTEGER NOT NULL DEFAULT 1,
    last_status TEXT NOT NULL DEFAULT 'UNKNOWN',
    last_checked_at TEXT,
    last_available_at TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS availability_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER NOT NULL,
    status TEXT NOT NULL,
    checked_at TEXT NOT NULL,
    source TEXT,
    FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
);
'''

def db_path():
    url = current_app.config["DATABASE_URL"]
    if url.startswith("sqlite:///"):
        return Path(url.replace("sqlite:///", "", 1))
    return Path("tracker.db")

def get_db():
    conn = sqlite3.connect(db_path())
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn

def init_db(app):
    with app.app_context():
        conn = get_db()
        conn.executescript(SCHEMA)
        if conn.execute("SELECT COUNT(*) FROM products").fetchone()[0] == 0:
            conn.execute(
                "INSERT INTO products(name,brand,product_url,provider) VALUES(?,?,?,?)",
                ("Hot Wheels Porsche 911 Carrera RS 2.7 Die Cast Car", "Hot Wheels",
                 "https://blinkit.com/prn/x/prid/1404276", "demo")
            )
        conn.commit()
        conn.close()
