import os

class Config:

    SECRET_KEY = "hackathon123"

    DATABASE = os.path.join(
        os.path.dirname(__file__),
        "database",
        "alumni.db"
    )