from flask import Blueprint, jsonify, render_template, request
from .db import get_db
from .providers import DemoProvider
from .monitor import check_all_products

bp = Blueprint("main", __name__)

@bp.get("/")
def index():
    return render_template("index.html")

@bp.get("/api/products")
def products():
    conn = get_db()
    rows = conn.execute("SELECT * FROM products ORDER BY id DESC").fetchall()
    conn.close()
    return jsonify([dict(r) for r in rows])

@bp.post("/api/products")
def add_product():
    data = request.get_json(force=True)
    name = str(data.get("name", "")).strip()
    url = str(data.get("product_url", "")).strip()
    provider = str(data.get("provider", "demo")).strip()

    if not name or not url:
        return jsonify({"error": "Name and product URL are required."}), 400

    if provider not in {"demo", "authorized-http"}:
        return jsonify({"error": "Unsupported provider."}), 400

    conn = get_db()
    cur = conn.execute(
        "INSERT INTO products(name,brand,product_url,provider) VALUES(?,?,?,?)",
        (name, data.get("brand", "Hot Wheels"), url, provider)
    )
    conn.commit()
    row = conn.execute("SELECT * FROM products WHERE id=?", (cur.lastrowid,)).fetchone()
    conn.close()
    return jsonify(dict(row)), 201

@bp.delete("/api/products/<int:product_id>")
def delete_product(product_id):
    conn = get_db()
    conn.execute("DELETE FROM products WHERE id=?", (product_id,))
    conn.commit()
    conn.close()
    return jsonify({"ok": True})

@bp.post("/api/products/<int:product_id>/toggle")
def toggle_product(product_id):
    conn = get_db()
    row = conn.execute("SELECT enabled FROM products WHERE id=?", (product_id,)).fetchone()
    if not row:
        conn.close()
        return jsonify({"error": "Not found"}), 404
    enabled = 0 if row["enabled"] else 1
    conn.execute("UPDATE products SET enabled=? WHERE id=?", (enabled, product_id))
    conn.commit()
    conn.close()
    return jsonify({"enabled": bool(enabled)})

@bp.post("/api/products/<int:product_id>/demo-available")
def demo_available(product_id):
    DemoProvider.state[product_id] = "AVAILABLE"
    return jsonify({"ok": True, "status": "AVAILABLE"})

@bp.post("/api/products/<int:product_id>/demo-unavailable")
def demo_unavailable(product_id):
    DemoProvider.state[product_id] = "UNAVAILABLE"
    return jsonify({"ok": True, "status": "UNAVAILABLE"})

@bp.post("/api/check-now")
def check_now():
    from flask import current_app
    check_all_products(current_app._get_current_object())
    return jsonify({"ok": True})
