from flask import Flask, g, render_template

from config import Config
import database
import utils
from routes.admin import admin_bp
from routes.auth import auth_bp
from routes.chatbot import chatbot_bp
from routes.donations import donations_bp
from routes.events import events_bp
from routes.jobs import jobs_bp
from routes.users import users_bp


def create_app():
    app = Flask(__name__, template_folder="templates", static_folder="static")
    app.config.from_object(Config)
    database.init_app(app)

    for bp in (auth_bp, users_bp, jobs_bp, events_bp, donations_bp, chatbot_bp, admin_bp):
        app.register_blueprint(bp)

    app.before_request(utils.load_user)
    app.before_request(utils.check_csrf)

    app.jinja_env.filters.update(
        pretty_date=utils.pretty_date, money=utils.money, skills=utils.skills_list, initials=utils.initials)
    app.jinja_env.globals.update(csrf_token=utils.csrf_token, days_until=utils.days_until,
                                 update_args=utils.update_args)

    @app.route("/")
    def home():
        return render_template("index.html")

    @app.errorhandler(400)
    @app.errorhandler(403)
    @app.errorhandler(404)
    def http_error(err):
        return render_template("error.html", err=err), err.code

    @app.after_request
    def security_headers(resp):
        resp.headers.setdefault("X-Content-Type-Options", "nosniff")
        resp.headers.setdefault("X-Frame-Options", "DENY")
        return resp

    return app


app = create_app()

if __name__ == "__main__":
    app.run(debug=True)
