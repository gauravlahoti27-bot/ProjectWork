from flask import Blueprint, flash, g, redirect, render_template, request, url_for

from database import execute, query
from utils import login_required

donations_bp = Blueprint("donations", __name__)


@donations_bp.route("/donations")
@login_required
def donation():
    campaigns = query(
        """SELECT c.*, COALESCE(SUM(d.amount),0) raised, COUNT(d.id) donors
           FROM campaigns c LEFT JOIN donations d ON d.campaign_id = c.id GROUP BY c.id ORDER BY c.id""")
    recent = query(
        """SELECT d.*, u.full_name, c.title campaign FROM donations d
           LEFT JOIN users u ON u.id = d.user_id JOIN campaigns c ON c.id = d.campaign_id
           ORDER BY d.created_at DESC, d.id DESC LIMIT 6""")
    mine = query("SELECT COALESCE(SUM(amount),0) s FROM donations WHERE user_id = ?", (g.user["id"],), one=True)["s"]
    return render_template("donations.html", campaigns=campaigns, recent=recent, mine=mine)


@donations_bp.route("/donate", methods=["POST"])
@login_required
def donate():
    try:
        amount = float(request.form.get("amount", ""))
        campaign_id = int(request.form.get("campaign_id", ""))
    except ValueError:
        flash("Please enter a valid amount.", "error")
        return redirect(url_for("donations.donation"))
    campaign = query("SELECT * FROM campaigns WHERE id = ?", (campaign_id,), one=True)
    if not campaign or not (1 <= amount <= 10_000_000):
        flash("Enter an amount between ₹1 and ₹1,00,00,000 for an existing campaign.", "error")
        return redirect(url_for("donations.donation"))
    # NOTE: demo flow — records the pledge. Plug in Razorpay/Stripe here for real payments.
    execute("INSERT INTO donations (campaign_id,user_id,amount,message) VALUES (?,?,?,?)",
            (campaign_id, g.user["id"], amount, request.form.get("message", "").strip()[:200]))
    flash(f"Thank you! Your pledge of ₹{amount:,.0f} to “{campaign['title']}” was recorded.", "success")
    return redirect(url_for("donations.donation"))
