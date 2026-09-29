import os
import requests

def send_telegram(message):
    token = os.getenv("TELEGRAM_BOT_TOKEN")
    chat_id = os.getenv("TELEGRAM_CHAT_ID")
    if not token or not chat_id:
        return False

    url = f"https://api.telegram.org/bot{token}/sendMessage"
    try:
        response = requests.post(url, json={
            "chat_id": chat_id,
            "text": message,
            "disable_web_page_preview": False
        }, timeout=10)
        return response.ok
    except Exception:
        return False

def notify_available(product):
    message = (
        f"🚨 Hot Wheels availability alert\n\n"
        f"{product['name']} is AVAILABLE.\n"
        f"Open Blinkit: {product['product_url']}"
    )
    return send_telegram(message)
