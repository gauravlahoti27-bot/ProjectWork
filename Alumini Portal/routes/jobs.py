from flask import Blueprint, render_template

jobs_bp = Blueprint("jobs", __name__)

jobs = [

{
"title":"Frontend Developer",
"company":"Google",
"location":"Mumbai"
},

{
"title":"Backend Developer",
"company":"Amazon",
"location":"Pune"
},

{
"title":"Data Analyst",
"company":"TCS",
"location":"Bangalore"
}

]


@jobs_bp.route("/jobs")
def jobs_page():

    return render_template(
        "jobs.html",
        jobs=jobs
    )