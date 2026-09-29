import os
from apscheduler.schedulers.background import BackgroundScheduler
from .monitor import check_all_products

_scheduler = None

def start_scheduler(app):
    global _scheduler
    if _scheduler is not None:
        return

    seconds = int(os.getenv("CHECK_INTERVAL_SECONDS", "60"))
    _scheduler = BackgroundScheduler(daemon=True)
    _scheduler.add_job(
        lambda: check_all_products(app),
        "interval",
        seconds=max(15, seconds),
        id="availability-monitor",
        replace_existing=True,
    )
    _scheduler.start()
