from flask import Blueprint, render_template

events_bp = Blueprint("events", __name__)

events = [

{
"title":"Annual Alumni Meet",
"date":"20 July 2026",
"venue":"TSEC Auditorium"
},

{
"title":"AI Workshop",
"date":"25 July 2026",
"venue":"Seminar Hall"
}

]


@events_bp.route("/events")
def event_page():

    return render_template(
        "events.html",
        events=events
    )