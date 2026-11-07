# Alumni Nexus AI — Alumni Portal

Flask + SQLite portal with a dark, premium UI.

## Run
```bash
python -m venv venv && source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt
python app.py                                      # http://127.0.0.1:5000
```
The database (`database/alumni.db`) is created and seeded with demo data on first run.
Delete the file to reset it.

## Demo logins (password `password123`)
`student@test.com` · `alumni@test.com` · `admin@test.com`

## Features
- Auth: register/login/logout, hashed passwords, sessions, CSRF protection, role-based access (student / alumni / admin)
- Dashboard: stats, profile completeness, upcoming events, latest jobs, suggested connections
- Alumni directory: search, batch/branch/city filters, mentor filter, pagination, public profile pages
- Profile: edit all details, skills, LinkedIn, "open to mentor"
- Jobs: search/filter/pagination, alumni post jobs, owners/admin delete
- Events: upcoming/past, create, register/cancel (event_registrations table), attendee counts
- Give Back: campaigns with progress bars, pledges, recent supporters (demo – no real payments)
- AI assistant: data-aware chatbot; set `OPENAI_API_KEY` (+ `pip install openai`) for LLM answers
- Admin: manage roles/users, create/delete campaigns

## Structure
```
app.py  config.py  utils.py
database/  schema.py seed.py __init__.py   (SQL tables, demo data, helpers)
routes/    auth users jobs events donations chatbot admin
templates/ base + one template per page
static/    css/ js/
```
Env vars: `SECRET_KEY`, `DATABASE_PATH`, `OPENAI_API_KEY` (see `.env.example`).
Production: `gunicorn app:app`. On Vercel the SQLite file lives in /tmp and resets — use Postgres for real deployments.
