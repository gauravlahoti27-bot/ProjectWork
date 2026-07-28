from flask import Flask, render_template
from config import Config

# Import Blueprints
from routes.auth import auth_bp
from routes.users import users_bp
from routes.jobs import jobs_bp
from routes.events import events_bp
from routes.donations import donations_bp
from routes.chatbot import chatbot_bp


app = Flask(__name__)

app.config.from_object(Config)


# Register Blueprints
app.register_blueprint(auth_bp)
app.register_blueprint(users_bp)
app.register_blueprint(jobs_bp)
app.register_blueprint(events_bp)
app.register_blueprint(donations_bp)
app.register_blueprint(chatbot_bp)



@app.route("/")
def home():
    return render_template("index.html")



if __name__ == "__main__":
    app.run(debug=True)