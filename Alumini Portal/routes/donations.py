from flask import Blueprint, render_template, request

donations_bp = Blueprint("donations", __name__)


@donations_bp.route("/donations")
def donation():

    return render_template("donation.html")


@donations_bp.route("/donate", methods=["POST"])
def donate():

    amount = request.form["amount"]

    return f"Thank You! Donation of ₹{amount} received."