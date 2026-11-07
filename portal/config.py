import os
from dotenv import load_dotenv

load_dotenv()

BASE_DIR = os.path.dirname(os.path.abspath(__file__))


class Config:
    SECRET_KEY = os.environ.get("SECRET_KEY", "dev-only-change-me")

    # Vercel's filesystem is read-only except /tmp (data resets there; use a real DB in production)
    DATABASE = os.environ.get(
        "DATABASE_PATH",
        "/tmp/alumni.db" if os.environ.get("VERCEL") else os.path.join(BASE_DIR, "database", "alumni.db"),
    )

    SESSION_COOKIE_HTTPONLY = True
    SESSION_COOKIE_SAMESITE = "Lax"
    SESSION_COOKIE_SECURE = bool(os.environ.get("VERCEL"))

    OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY", "")
    OPENAI_MODEL = os.environ.get("OPENAI_MODEL", "gpt-4o-mini")

    PER_PAGE = 9
