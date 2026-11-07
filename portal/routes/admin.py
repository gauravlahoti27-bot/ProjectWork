from flask import Blueprint, flash, g, redirect, render_template, request, url_for

from database import execute, query
from utils import roles_required

admin_bp = Blueprint("admin", __name__, url_prefix="/admin")


@admin_bp.route("/")
@roles_required("admin")
def panel():
    counts = {t: query(f"SELECT COUNT(*) c FROM {t}", one=True)["c"]
              for t in ("users", "jobs", "events", "event_registrations", "donations")}
    users = query("SELECT * FROM users ORDER BY created_at DESC, id DESC")
    campaigns = query("""SELECT c.*, COALESCE(SUM(d.amount),0) raised FROM campaigns c
                         LEFT JOIN donations d ON d.campaign_id=c.id GROUP BY c.id""")
    return render_template("admin.html", counts=counts, users=users, campaigns=campaigns)


@admin_bp.route("/users/<int:user_id>/role", methods=["POST"])
@roles_required("admin")
def set_role(user_id):
    role = request.form.get("role")
    if user_id == g.user["id"]:
        flash("You can't change your own role.", "error")
    elif role in ("student", "alumni", "admin"):
        execute("UPDATE users SET role=? WHERE id=?", (role, user_id))
        flash("Role updated.", "success")
    return redirect(url_for("admin.panel"))


@admin_bp.route("/users/<int:user_id>/delete", methods=["POST"])
@roles_required("admin")
def delete_user(user_id):
    if user_id == g.user["id"]:
        flash("You can't delete your own account.", "error")
    else:
        execute("DELETE FROM users WHERE id=?", (user_id,))
        flash("User deleted.", "info")
    return redirect(url_for("admin.panel"))


@admin_bp.route("/campaigns", methods=["POST"])
@roles_required("admin")
def create_campaign():
    title = request.form.get("title", "").strip()
    try:
        goal = float(request.form.get("goal_amount", ""))
    except ValueError:
        goal = 0
    if not title or goal <= 0:
        flash("Campaign needs a title and a positive goal.", "error")
    else:
        execute("INSERT INTO campaigns (title,description,goal_amount) VALUES (?,?,?)",
                (title, request.form.get("description", "").strip(), goal))
        flash("Campaign created.", "success")
    return redirect(url_for("admin.panel"))


@admin_bp.route("/campaigns/<int:cid>/delete", methods=["POST"])
@roles_required("admin")
def delete_campaign(cid):
    execute("DELETE FROM campaigns WHERE id=?", (cid,))
    flash("Campaign deleted.", "info")
    return redirect(url_for("admin.panel"))
