from datetime import date

from flask import Blueprint, abort, flash, g, redirect, render_template, request, url_for

from database import execute, query
from utils import login_required, roles_required

events_bp = Blueprint("events", __name__)
CATEGORIES = ["Reunion", "Workshop", "Webinar", "Panel", "Meetup", "Networking"]


@events_bp.route("/events")
@login_required
def event_page():
    show = request.args.get("show", "upcoming")
    op, order = (">=", "ASC") if show != "past" else ("<", "DESC")
    events = query(
        f"""SELECT e.*, (SELECT COUNT(*) FROM event_registrations r WHERE r.event_id = e.id) attendees,
            EXISTS(SELECT 1 FROM event_registrations r WHERE r.event_id = e.id AND r.user_id = ?) registered
            FROM events e WHERE e.event_date {op} ? ORDER BY e.event_date {order}""",
        (g.user["id"], date.today().isoformat()))
    return render_template("events.html", events=events, show=show, categories=CATEGORIES)


@events_bp.route("/events/new", methods=["POST"])
@roles_required("alumni", "admin")
def create_event():
    f = request.form
    title, ev_date = f.get("title", "").strip(), f.get("event_date", "").strip()
    try:
        date.fromisoformat(ev_date)
    except ValueError:
        ev_date = ""
    if not title or not ev_date:
        flash("Event title and a valid date are required.", "error")
        return redirect(url_for("events.event_page"))
    cat = f.get("category") if f.get("category") in CATEGORIES else "Meetup"
    execute("INSERT INTO events (title,description,venue,event_date,category,created_by) VALUES (?,?,?,?,?,?)",
            (title, f.get("description", "").strip(), f.get("venue", "").strip(), ev_date, cat, g.user["id"]))
    flash("Event created.", "success")
    return redirect(url_for("events.event_page"))


@events_bp.route("/events/<int:event_id>/register", methods=["POST"])
@login_required
def toggle_registration(event_id):
    event = query("SELECT * FROM events WHERE id = ?", (event_id,), one=True)
    if not event:
        abort(404)
    existing = query("SELECT 1 FROM event_registrations WHERE user_id=? AND event_id=?", (g.user["id"], event_id), one=True)
    if existing:
        execute("DELETE FROM event_registrations WHERE user_id=? AND event_id=?", (g.user["id"], event_id))
        flash(f"Registration cancelled for “{event['title']}”.", "info")
    elif event["event_date"] < date.today().isoformat():
        flash("This event has already taken place.", "error")
    else:
        execute("INSERT INTO event_registrations (user_id,event_id) VALUES (?,?)", (g.user["id"], event_id))
        flash(f"You're registered for “{event['title']}”!", "success")
    return redirect(request.referrer or url_for("events.event_page"))


@events_bp.route("/events/<int:event_id>/delete", methods=["POST"])
@login_required
def delete_event(event_id):
    event = query("SELECT * FROM events WHERE id = ?", (event_id,), one=True)
    if not event:
        abort(404)
    if g.user["role"] != "admin" and event["created_by"] != g.user["id"]:
        abort(403)
    execute("DELETE FROM events WHERE id = ?", (event_id,))
    flash("Event deleted.", "info")
    return redirect(url_for("events.event_page"))
