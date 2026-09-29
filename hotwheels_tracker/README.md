# Hot Wheels Drop Tracker

A Flask + SQLite watchlist dashboard for tracking a collectible product drop.

## Important Blinkit integration note

This project deliberately does NOT scrape Blinkit, reverse-engineer private Blinkit APIs, automate a Blinkit account, add items to a Blinkit cart, or place orders automatically.

Blinkit's current Terms prohibit data mining, robots/similar extraction tools, bypassing access/security measures, and bot-posted activity. Therefore the production-safe design is:

1. Store the Blinkit product URL.
2. Monitor availability only through an authorized/public data source or a source you are permitted to use.
3. When availability changes to `AVAILABLE`, notify the user immediately.
4. The user clicks `Buy on Blinkit` and completes checkout manually.

The code includes:
- A manual/demo availability provider so the project works immediately.
- A generic authorized HTTP provider adapter that can be connected to an API/feed you are explicitly allowed to use.
- SQLite watchlist/history.
- Browser notifications.
- Optional Telegram notifications.
- A responsive dashboard.

## Run

### Windows

```powershell
py -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
python run.py
```

Open http://127.0.0.1:5000

## Notification testing

The dashboard includes a **Test notification** button. It tests the notification delivery path without pretending that the Blinkit product is in stock. For safety, the backend endpoint is disabled by default. Temporarily set `ENABLE_TEST_NOTIFICATION=true` in the deployment environment, test it, then set it back to `false`.

The test reports whether browser notifications and Telegram delivery are working. Browser notifications require permission in the browser. Telegram requires both `TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID`.

## Demo

1. Add a product.
2. Set provider to `demo`.
3. Click `Make Available` on the dashboard.
4. The tracker records the availability transition and triggers notifications.

## Production integration

Use an official Blinkit API/feed or written permission if Blinkit provides one to you. Put that integration inside `app/providers.py`. Do not put account passwords, payment details, session cookies, or private API tokens into the frontend.

## Share the website with others

### Easiest: Render
1. Put this project in a GitHub repository.
2. Create a Web Service on Render.
3. Build command: `pip install -r requirements.txt`
4. Start command: `gunicorn run:app` (add `gunicorn` to requirements.txt).
5. Render gives you a public `onrender.com` URL.

For a persistent production database, use PostgreSQL rather than SQLite.

### Railway
Push the repo to GitHub, create a Railway service from the repo, install dependencies, and run `python run.py` for testing or a production WSGI server for deployment.

### Important
Do not put Blinkit passwords, OTPs, payment details, session cookies, or private account tokens in this project.
