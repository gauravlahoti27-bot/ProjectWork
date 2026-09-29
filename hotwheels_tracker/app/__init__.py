import os
from flask import Flask
from dotenv import load_dotenv
from .db import init_db
from .scheduler import start_scheduler

load_dotenv()

def create_app():
    app = Flask(__name__)
    app.config["SECRET_KEY"] = os.getenv("FLASK_SECRET_KEY", "dev-secret-change-me")
    app.config["DATABASE_URL"] = os.getenv("DATABASE_URL", "sqlite:///tracker.db")

    init_db(app)

    from .routes import bp
    app.register_blueprint(bp)

    start_scheduler(app)
    return app
