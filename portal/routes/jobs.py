from flask import Blueprint, abort, current_app, flash, g, redirect, render_template, request, url_for

from database import execute, query
from utils import int_arg, like, login_required, paginate, roles_required

jobs_bp = Blueprint("jobs", __name__)
JOB_TYPES = ["Full-time", "Internship", "Contract", "Part-time"]


@jobs_bp.route("/jobs")
@login_required
def jobs_page():
    q = request.args.get("q", "").strip()
    jtype = request.args.get("type", "").strip()
    location = request.args.get("location", "").strip()
    where, args = ["1=1"], []
    if q:
        where.append("(j.title LIKE ? ESCAPE '\\' OR j.company LIKE ? ESCAPE '\\' OR j.description LIKE ? ESCAPE '\\')")
        args += [like(q)] * 3
    if jtype in JOB_TYPES:
        where.append("j.job_type = ?"); args.append(jtype)
    if location:
        where.append("j.location LIKE ? ESCAPE '\\'"); args.append(like(location))
    clause = " AND ".join(where)
    total = query(f"SELECT COUNT(*) c FROM jobs j WHERE {clause}", args, one=True)["c"]
    pg = paginate(total, int_arg("page"), current_app.config["PER_PAGE"])
    jobs = query(
        f"""SELECT j.*, u.full_name poster_name FROM jobs j LEFT JOIN users u ON u.id = j.posted_by
            WHERE {clause} ORDER BY j.created_at DESC, j.id DESC LIMIT ? OFFSET ?""",
        args + [current_app.config["PER_PAGE"], pg["offset"]])
    return render_template("jobs.html", jobs=jobs, pg=pg, job_types=JOB_TYPES,
                           f={"q": q, "type": jtype, "location": location})


@jobs_bp.route("/jobs/new", methods=["POST"])
@roles_required("alumni", "admin")
def create_job():
    f = request.form
    title, company = f.get("title", "").strip(), f.get("company", "").strip()
    if not title or not company:
        flash("Job title and company are required.", "error")
        return redirect(url_for("jobs.jobs_page"))
    link = f.get("apply_link", "").strip()
    if link and not link.startswith(("http://", "https://")):
        link = "https://" + link
    jtype = f.get("job_type") if f.get("job_type") in JOB_TYPES else "Full-time"
    execute(
        "INSERT INTO jobs (title,company,location,job_type,description,apply_link,posted_by) VALUES (?,?,?,?,?,?,?)",
        (title, company, f.get("location", "").strip(), jtype, f.get("description", "").strip(), link, g.user["id"]))
    flash("Job posted — thanks for helping the community!", "success")
    return redirect(url_for("jobs.jobs_page"))


@jobs_bp.route("/jobs/<int:job_id>/delete", methods=["POST"])
@login_required
def delete_job(job_id):
    job = query("SELECT * FROM jobs WHERE id = ?", (job_id,), one=True)
    if not job:
        abort(404)
    if g.user["role"] != "admin" and job["posted_by"] != g.user["id"]:
        abort(403)
    execute("DELETE FROM jobs WHERE id = ?", (job_id,))
    flash("Job removed.", "info")
    return redirect(url_for("jobs.jobs_page"))
