from flask import Blueprint, render_template, request, redirect, url_for

auth_bp = Blueprint("auth", __name__)

users = {
    "student@test.com": {"password": "123", "role": "Student"},
    "alumni@test.com": {"password": "123", "role": "Alumni"},
    "admin@test.com": {"password": "123", "role": "Admin"}
}

@auth_bp.route("/login", methods=["GET", "POST"])
def login():

    if request.method == "POST":

        email = request.form["email"]
        password = request.form["password"]

        if email in users and users[email]["password"] == password:
            return redirect(url_for("users.dashboard"))

        return "Invalid Email or Password"

    return render_template("login.html")


@auth_bp.route("/logout")
def logout():
    return redirect(url_for("auth.login"))