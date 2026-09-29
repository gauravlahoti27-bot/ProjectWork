from datetime import datetime, timezone
from .db import get_db
from .providers import get_provider
from .notifications import notify_available

def now_iso():
    return datetime.now(timezone.utc).isoformat()

def check_all_products(app):
    with app.app_context():
        conn = get_db()
        products = conn.execute(
            "SELECT * FROM products WHERE enabled = 1"
        ).fetchall()

        for product in products:
            result = get_provider(product["provider"]).check(product)
            checked = now_iso()

            conn.execute(
                "UPDATE products SET last_status=?, last_checked_at=? WHERE id=?",
                (result.status, checked, product["id"])
            )
            conn.execute(
                "INSERT INTO availability_history(product_id,status,checked_at,source) VALUES(?,?,?,?)",
                (product["id"], result.status, checked, result.source)
            )

            # Alert only on a transition into AVAILABLE.
            if result.status == "AVAILABLE" and product["last_status"] != "AVAILABLE":
                conn.execute(
                    "UPDATE products SET last_available_at=? WHERE id=?",
                    (checked, product["id"])
                )
                notify_available(product)

        conn.commit()
        conn.close()
