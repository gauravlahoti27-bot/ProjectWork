from flask import Blueprint, render_template, request, jsonify

chatbot_bp = Blueprint("chatbot", __name__)

@chatbot_bp.route("/chatbot")
def chatbot():
    return render_template("chatbot.html")


@chatbot_bp.route("/ask", methods=["POST"])
def ask():

    user_message = request.json.get("message")

    # Dummy response for now
    answer = "Hello! I am your Alumni AI Assistant."

    return jsonify({
        "reply": answer
    })