"""Shared helpers: auth decorators, CSRF protection, pagination, template filters."""
import math
import secrets
from datetime import date, datetime
from functools import wraps

from flask import abort, flash, g, redirect, request, session, url_for

from database import query


def load_user():
    uid = session.get("user_id")
    g.user = query("SELECT * FROM users WHERE id = ?", (uid,), one=True) if uid else None
    if uid and g.user is None:
        session.clear()


def login_required(view):
    @wraps(view)
    def wrapped(*a, **kw):
        if g.user is None:
            flash("Please log in to continue.", "info")
            return redirect(url_for("auth.login", next=request.path))
        return view(*a, **kw)
    return wrapped


def roles_required(*roles):
    def deco(view):
        @wraps(view)
        @login_required
        def wrapped(*a, **kw):
            if g.user["role"] not in roles:
                abort(403)
            return view(*a, **kw)
        return wrapped
    return deco


# ---- CSRF -----------------------------------------------------------------
def csrf_token():
    if "_csrf" not in session:
        session["_csrf"] = secrets.token_hex(16)
    return session["_csrf"]


def check_csrf():
    if request.method == "POST":
        sent = request.form.get("_csrf") or request.headers.get("X-CSRF-Token")
        if not sent or not secrets.compare_digest(sent, session.get("_csrf", "")):
            abort(400, "Invalid or missing CSRF token. Refresh the page and try again.")


# ---- pagination -----------------------------------------------------------
def paginate(total, page, per_page):
    pages = max(1, math.ceil(total / per_page))
    page = min(max(page, 1), pages)
    return {"page": page, "pages": pages, "total": total, "offset": (page - 1) * per_page,
            "has_prev": page > 1, "has_next": page < pages}


def int_arg(name, default=1):
    try:
        return int(request.args.get(name, default))
    except (TypeError, ValueError):
        return default


# ---- misc -----------------------------------------------------------------
def initials(name):
    return "".join(p[0] for p in (name or "?").split()[:2]).upper()


def pretty_date(value):
    try:
        return datetime.strptime(value[:10], "%Y-%m-%d").strftime("%d %b %Y")
    except (TypeError, ValueError):
        return value or ""


def days_until(value):
    try:
        return (datetime.strptime(value[:10], "%Y-%m-%d").date() - date.today()).days
    except (TypeError, ValueError):
        return 0


def money(value):
    return "₹{:,.0f}".format(value or 0)


def skills_list(value):
    return [s.strip() for s in (value or "").split(",") if s.strip()]


def like(term):
    """Escape LIKE wildcards so user input is matched literally (use with ESCAPE '\\')."""
    return "%" + term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"


def update_args(**changes):
    """Current query args with changes applied (for pagination links)."""
    args = request.args.to_dict()
    args.update({k: v for k, v in changes.items()})
    return {k: v for k, v in args.items() if v not in ("", None)}
