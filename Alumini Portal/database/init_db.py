from flask_sqlalchemy import SQLAlchemy

db = SQLAlchemy()
EOF

cat > database/models.py << 'EOF'
"""
SQLAlchemy models for Alumni Nexus AI.

Tables:
  users        - students, alumni, and admins (single table, role column)
  events       - reunions, webinars, meetups
  event_rsvps  - association table: which users RSVP'd to which event
  jobs         - job / internship postings shared by alumni
  campaigns    - donation campaigns (scholarships, funds, etc.)
  donations    - individual donation records against a campaign
  chat_messages- AI Career Assistant conversation history per user
"""
from datetime import datetime
from flask_login import UserMixin
from werkzeug.security import generate_password_hash, check_password_hash

from database import db

import random

AVATAR_GRADIENTS = [
    "linear-gradient(120deg,#6C63FF,#8B5CF6)",
    "linear-gradient(120deg,#00E5FF,#6C63FF)",
    "linear-gradient(120deg,#8B5CF6,#00E5FF)",
    "linear-gradient(120deg,#34D399,#00E5FF)",
    "linear-gradient(120deg,#F59E0B,#8B5CF6)",
]

# ---------------------------------------------------------------
# Association table: many-to-many between users and events (RSVPs)
# ---------------------------------------------------------------
event_rsvps = db.Table(
    "event_rsvps",
    db.Column("user_id", db.Integer, db.ForeignKey("users.id"), primary_key=True),
    db.Column("event_id", db.Integer, db.ForeignKey("events.id"), primary_key=True),
    db.Column("created_at", db.DateTime, default=datetime.utcnow),
)


class User(db.Model, UserMixin):
    __tablename__ = "users"

    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(120), nullable=False)
    email = db.Column(db.String(160), unique=True, nullable=False, index=True)
    password_hash = db.Column(db.String(255), nullable=False)
    role = db.Column(db.String(20), default="alumni")  # 'alumni' | 'admin'

    grad_year = db.Column(db.Integer)
    branch = db.Column(db.String(120), default="")
    company = db.Column(db.String(120), default="")
    job_title = db.Column(db.String(120), default="")
    location = db.Column(db.String(120), default="")
    bio = db.Column(db.Text, default="")
    linkedin = db.Column(db.String(255), default="")
    skills = db.Column(db.String(255), default="")   # comma-separated, used for AI matching
    goals = db.Column(db.String(255), default="")    # comma-separated, used for AI matching
    open_to_mentor = db.Column(db.Boolean, default=False)

    avatar_gradient = db.Column(db.String(80), default=AVATAR_GRADIENTS[0])
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    events = db.relationship("Event", secondary=event_rsvps, back_populates="attendees")
    jobs_posted = db.relationship("Job", backref="poster", lazy=True)
    donations = db.relationship("Donation", backref="donor", lazy=True)
    chat_messages = db.relationship("ChatMessage", backref="user", lazy=True)

    def set_password(self, raw_password):
        self.password_hash = generate_password_hash(raw_password)

    def check_password(self, raw_password):
        return check_password_hash(self.password_hash, raw_password)

    def initials(self):
        parts = self.name.split()
        return "".join(p[0] for p in parts[:2]).upper()

    def skills_list(self):
        return [s.strip() for s in (self.skills or "").split(",") if s.strip()]

    def to_public_dict(self):
        return {
            "id": self.id,
            "name": self.name,
            "role": self.role,
            "gradYear": self.grad_year,
            "branch": self.branch,
            "company": self.company,
            "jobTitle": self.job_title,
            "location": self.location,
            "bio": self.bio,
            "linkedin": self.linkedin,
            "skills": self.skills_list(),
            "initials": self.initials(),
            "avatarGradient": self.avatar_gradient,
        }

    @staticmethod
    def random_gradient():
        return random.choice(AVATAR_GRADIENTS)


class Event(db.Model):
    __tablename__ = "events"

    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(160), nullable=False)
    description = db.Column(db.Text, default="")
    date = db.Column(db.String(20), nullable=False)  # ISO date string, e.g. 2026-12-20
    location = db.Column(db.String(160), default="")
    created_by = db.Column(db.Integer, db.ForeignKey("users.id"))
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    attendees = db.relationship("User", secondary=event_rsvps, back_populates="events")

    def attendee_count(self):
        return len(self.attendees)


class Job(db.Model):
    __tablename__ = "jobs"

    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(160), nullable=False)
    company = db.Column(db.String(120), nullable=False)
    location = db.Column(db.String(120), default="")
    description = db.Column(db.Text, default="")
    link = db.Column(db.String(255), default="")
    posted_by = db.Column(db.Integer, db.ForeignKey("users.id"))
    created_at = db.Column(db.DateTime, default=datetime.utcnow)


class Campaign(db.Model):
    __tablename__ = "campaigns"

    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(160), nullable=False)
    description = db.Column(db.Text, default="")
    goal_amount = db.Column(db.Float, default=0)
    raised_amount = db.Column(db.Float, default=0)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    donations = db.relationship("Donation", backref="campaign", lazy=True)

    def progress_pct(self):
        if not self.goal_amount:
            return 0
        return min(100, round((self.raised_amount / self.goal_amount) * 100))


class Donation(db.Model):
    __tablename__ = "donations"

    id = db.Column(db.Integer, primary_key=True)
    campaign_id = db.Column(db.Integer, db.ForeignKey("campaigns.id"), nullable=False)
    donor_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
    amount = db.Column(db.Float, nullable=False)
    message = db.Column(db.String(255), default="")
    created_at = db.Column(db.DateTime, default=datetime.utcnow)


class ChatMessage(db.Model):
    __tablename__ = "chat_messages"

    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
    role = db.Column(db.String(10), nullable=False)  # 'user' | 'assistant'
    content = db.Column(db.Text, nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)



