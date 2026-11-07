from flask import Blueprint, abort, flash, g, redirect, render_template, request, url_for

from database import execute, query
from utils import int_arg, like, login_required, paginate
from flask import current_app

users_bp = Blueprint("users", __name__)

FIELDS = ("full_name", "branch", "company", "designation", "location", "phone", "bio", "skills", "linkedin")


@users_bp.route("/dashboard")
@login_required
def dashboard():
    uid = g.user["id"]
    stats = {
        "alumni": query("SELECT COUNT(*) c FROM users WHERE role='alumni'", one=True)["c"],
        "jobs": query("SELECT COUNT(*) c FROM jobs", one=True)["c"],
        "events": query("SELECT COUNT(*) c FROM events WHERE event_date >= date('now')", one=True)["c"],
        "raised": query("SELECT COALESCE(SUM(amount),0) s FROM donations", one=True)["s"],
    }
    upcoming = query(
        """SELECT e.*, (SELECT COUNT(*) FROM event_registrations r WHERE r.event_id=e.id) attendees,
           EXISTS(SELECT 1 FROM event_registrations r WHERE r.event_id=e.id AND r.user_id=?) registered
           FROM events e WHERE event_date >= date('now') ORDER BY event_date LIMIT 3""", (uid,))
    jobs = query("SELECT * FROM jobs ORDER BY created_at DESC, id DESC LIMIT 4")
    # Suggested connections: alumni sharing skills/branch, open to mentoring first
    me_skills = {s.strip().lower() for s in (g.user["skills"] or "").split(",") if s.strip()}
    pool = query("SELECT * FROM users WHERE id != ? AND role != 'admin'", (uid,))
    scored = []
    for u in pool:
        sk = {s.strip().lower() for s in (u["skills"] or "").split(",") if s.strip()}
        score = len(me_skills & sk) * 3 + (2 if u["branch"] and u["branch"] == g.user["branch"] else 0) + (1 if u["open_to_mentor"] else 0)
        scored.append((score, u))
    suggestions = [u for s, u in sorted(scored, key=lambda x: -x[0])[:4]]
    fields = [g.user[f] for f in ("branch", "company", "designation", "location", "bio", "skills", "linkedin")]
    completeness = int(100 * sum(1 for v in fields if v) / len(fields))
    return render_template("dashboard.html", stats=stats, upcoming=upcoming, jobs=jobs,
                           suggestions=suggestions, completeness=completeness)


@users_bp.route("/directory")
@users_bp.route("/alumni")
@login_required
def directory():
    q = request.args.get("q", "").strip()
    year = request.args.get("year", "").strip()
    branch = request.args.get("branch", "").strip()
    location = request.args.get("location", "").strip()
    mentors = request.args.get("mentors") == "1"

    where, args = ["role != 'admin'"], []
    if q:
        where.append("(full_name LIKE ? ESCAPE '\\' OR company LIKE ? ESCAPE '\\' OR designation LIKE ? ESCAPE '\\' OR skills LIKE ? ESCAPE '\\')")
        args += [like(q)] * 4
    if year.isdigit():
        where.append("graduation_year = ?"); args.append(int(year))
    if branch:
        where.append("branch = ?"); args.append(branch)
    if location:
        where.append("location LIKE ? ESCAPE '\\'"); args.append(like(location))
    if mentors:
        where.append("open_to_mentor = 1")
    clause = " AND ".join(where)

    total = query(f"SELECT COUNT(*) c FROM users WHERE {clause}", args, one=True)["c"]
    pg = paginate(total, int_arg("page"), current_app.config["PER_PAGE"])
    people = query(f"SELECT * FROM users WHERE {clause} ORDER BY graduation_year DESC, full_name LIMIT ? OFFSET ?",
                   args + [current_app.config["PER_PAGE"], pg["offset"]])
    years = [r[0] for r in query("SELECT DISTINCT graduation_year FROM users WHERE graduation_year IS NOT NULL AND role!='admin' ORDER BY 1 DESC")]
    branches = [r[0] for r in query("SELECT DISTINCT branch FROM users WHERE branch != '' ORDER BY 1")]
    return render_template("directory.html", people=people, pg=pg, years=years, branches=branches,
                           f={"q": q, "year": year, "branch": branch, "location": location, "mentors": mentors})


@users_bp.route("/alumni/<int:user_id>")
@login_required
def alumni_detail(user_id):
    person = query("SELECT * FROM users WHERE id = ?", (user_id,), one=True)
    if not person:
        abort(404)
    jobs = query("SELECT * FROM jobs WHERE posted_by = ? ORDER BY created_at DESC", (user_id,))
    return render_template("alumni_detail.html", person=person, jobs=jobs)


@users_bp.route("/profile", methods=["GET", "POST"])
@login_required
def profile():
    if request.method == "POST":
        f = request.form
        name = f.get("full_name", "").strip()
        year = f.get("graduation_year", "").strip()
        if len(name) < 2:
            flash("Name is required.", "error")
        elif year and (not year.isdigit() or not 1950 <= int(year) <= 2040):
            flash("Enter a valid graduation year.", "error")
        else:
            linkedin = f.get("linkedin", "").strip()
            if linkedin and not linkedin.startswith(("http://", "https://")):
                linkedin = "https://" + linkedin
            vals = {k: f.get(k, "").strip() for k in FIELDS}
            vals["linkedin"] = linkedin
            vals["skills"] = ",".join(s.strip() for s in vals["skills"].split(",") if s.strip())
            execute(
                f"UPDATE users SET {', '.join(k + '=?' for k in FIELDS)}, graduation_year=?, open_to_mentor=? WHERE id=?",
                [vals[k] for k in FIELDS] + [int(year) if year else None, 1 if f.get("open_to_mentor") else 0, g.user["id"]],
            )
            flash("Profile updated.", "success")
            return redirect(url_for("users.profile"))
    return render_template("profile.html")
