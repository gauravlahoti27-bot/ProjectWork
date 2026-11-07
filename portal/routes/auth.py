import re

from flask import Blueprint, flash, g, redirect, render_template, request, session, url_for
from werkzeug.security import check_password_hash, generate_password_hash

from database import execute, query

auth_bp = Blueprint("auth", __name__)
EMAIL_RE = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")


def _safe_next(target):
    return target if target and target.startswith("/") and not target.startswith("//") else None


@auth_bp.route("/login", methods=["GET", "POST"])
def login():
    if g.user:
        return redirect(url_for("users.dashboard"))
    if request.method == "POST":
        email = request.form.get("email", "").strip().lower()
        password = request.form.get("password", "")
        user = query("SELECT * FROM users WHERE email = ?", (email,), one=True)
        if user and check_password_hash(user["password"], password):
            session.clear()
            session["user_id"] = user["id"]
            flash(f"Welcome back, {user['full_name'].split()[0]}!", "success")
            return redirect(_safe_next(request.args.get("next")) or url_for("users.dashboard"))
        flash("Invalid email or password.", "error")
    return render_template("login.html")


@auth_bp.route("/register", methods=["GET", "POST"])
def register():
    if g.user:
        return redirect(url_for("users.dashboard"))
    if request.method == "POST":
        f = request.form
        name, email = f.get("full_name", "").strip(), f.get("email", "").strip().lower()
        password, role = f.get("password", ""), f.get("role", "alumni")
        year = f.get("graduation_year", "").strip()
        errors = []
        if len(name) < 2:
            errors.append("Please enter your full name.")
        if not EMAIL_RE.match(email):
            errors.append("Please enter a valid email address.")
        if len(password) < 8:
            errors.append("Password must be at least 8 characters.")
        if password != f.get("confirm", ""):
            errors.append("Passwords do not match.")
        if role not in ("student", "alumni"):
            role = "alumni"
        if not year.isdigit() or not (1950 <= int(year) <= 2040):
            errors.append("Enter a valid graduation year.")
        if not errors and query("SELECT 1 FROM users WHERE email = ?", (email,), one=True):
            errors.append("An account with this email already exists.")
        if errors:
            for e in errors:
                flash(e, "error")
            return render_template("register.html", form=f), 400
        cur = execute(
            "INSERT INTO users (full_name,email,password,role,branch,graduation_year) VALUES (?,?,?,?,?,?)",
            (name, email, generate_password_hash(password), role, f.get("branch", "").strip(), int(year)),
        )
        session.clear()
        session["user_id"] = cur.lastrowid
        flash("Account created! Complete your profile to appear in the directory.", "success")
        return redirect(url_for("users.profile"))
    return render_template("register.html", form={})


@auth_bp.route("/logout", methods=["POST"])
def logout():
    session.clear()
    flash("You have been logged out.", "info")
    return redirect(url_for("home"))
