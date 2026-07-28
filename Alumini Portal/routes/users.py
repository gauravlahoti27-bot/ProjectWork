from flask import Blueprint, render_template

users_bp = Blueprint("users", __name__)

dummy_alumni = [
    {
        "name":"Rahul Sharma",
        "company":"Google",
        "batch":"2021"
    },
    {
        "name":"Priya Patel",
        "company":"Microsoft",
        "batch":"2020"
    }
]


@users_bp.route("/dashboard")
def dashboard():
    return render_template("dashboard.html")

@users_bp.route("/alumni")
def alumni():
    return render_template("alumni.html")


@users_bp.route("/directory")
def directory():
    return render_template("alumni.html")

@users_bp.route("/profile")
def profile():
    return render_template("profile.html")