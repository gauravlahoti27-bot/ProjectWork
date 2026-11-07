"""SQLite helpers: connection per request, schema creation and demo seeding."""
import os
import sqlite3

from flask import current_app, g

from .schema import SCHEMA
from .seed import seed_demo_data


def get_db():
    if "db" not in g:
        g.db = sqlite3.connect(current_app.config["DATABASE"])
        g.db.row_factory = sqlite3.Row
        g.db.execute("PRAGMA foreign_keys = ON")
    return g.db


def close_db(_exc=None):
    db = g.pop("db", None)
    if db is not None:
        db.close()


def query(sql, args=(), one=False):
    cur = get_db().execute(sql, args)
    rows = cur.fetchall()
    return (rows[0] if rows else None) if one else rows


def execute(sql, args=()):
    db = get_db()
    cur = db.execute(sql, args)
    db.commit()
    return cur


def init_db(app):
    """Create tables (if missing) and seed demo data on first run."""
    path = app.config["DATABASE"]
    os.makedirs(os.path.dirname(path), exist_ok=True)
    conn = sqlite3.connect(path)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    conn.executescript(SCHEMA)
    if conn.execute("SELECT COUNT(*) FROM users").fetchone()[0] == 0:
        seed_demo_data(conn)
    conn.commit()
    conn.close()


def init_app(app):
    app.teardown_appcontext(close_db)
    init_db(app)
