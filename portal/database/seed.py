"""Demo data so the portal looks alive on first run."""
from datetime import date, timedelta

from werkzeug.security import generate_password_hash

DEMO_PASSWORD = "password123"

# (name, email, role, branch, year, company, designation, location, skills, mentor, bio)
USERS = [
    ("Admin User", "admin@test.com", "admin", "Computer Engineering", 2015, "Alumni Office", "Portal Administrator", "Mumbai", "management,community", 0, "Keeps the alumni network running smoothly."),
    ("Student Demo", "student@test.com", "student", "Information Technology", 2027, "", "", "Mumbai", "python,react", 0, "Final-year student looking for internships and mentors."),
    ("Alumni Demo", "alumni@test.com", "alumni", "Computer Engineering", 2019, "Infosys", "Senior Engineer", "Pune", "java,spring,cloud", 1, "Happy to guide juniors on backend careers."),
    ("Rahul Sharma", "rahul@alumni.test", "alumni", "Computer Engineering", 2021, "Google", "Software Engineer", "Bangalore", "python,go,distributed systems", 1, "Working on search infrastructure. Ask me about interviews."),
    ("Priya Patel", "priya@alumni.test", "alumni", "Information Technology", 2020, "Microsoft", "Product Manager", "Hyderabad", "product,analytics,sql", 1, "PM for developer tools. Love helping engineers move into product."),
    ("Ananya Rao", "ananya@alumni.test", "alumni", "Electronics", 2018, "Qualcomm", "Hardware Lead", "Bangalore", "vlsi,embedded,c++", 1, "Chip design and embedded systems."),
    ("Vikram Mehta", "vikram@alumni.test", "alumni", "Mechanical", 2016, "Tata Motors", "Design Manager", "Pune", "cad,ev,manufacturing", 0, "EV platform design."),
    ("Sneha Kulkarni", "sneha@alumni.test", "alumni", "Information Technology", 2022, "Amazon", "SDE II", "Mumbai", "java,aws,microservices", 1, "Building services that scale."),
    ("Arjun Nair", "arjun@alumni.test", "alumni", "Computer Engineering", 2017, "Razorpay", "Engineering Manager", "Bangalore", "leadership,fintech,go", 1, "Fintech engineering leader."),
    ("Kavya Iyer", "kavya@alumni.test", "alumni", "Data Science", 2023, "Flipkart", "Data Scientist", "Bangalore", "ml,python,statistics", 1, "Recommendation systems."),
    ("Rohan Desai", "rohan@alumni.test", "alumni", "Civil", 2014, "L&T", "Project Director", "Mumbai", "infrastructure,planning", 0, "Large infrastructure projects."),
    ("Meera Joshi", "meera@alumni.test", "alumni", "Information Technology", 2019, "Zomato", "Frontend Lead", "Gurugram", "react,typescript,design systems", 1, "Frontend architecture and design systems."),
    ("Aditya Singh", "aditya@alumni.test", "alumni", "Computer Engineering", 2024, "TCS", "Systems Engineer", "Mumbai", "java,sql", 0, "Recent graduate."),
]

JOBS = [
    ("Frontend Developer", "Google", "Bangalore", "Full-time", "Build delightful web experiences with React and TypeScript.", 3),
    ("Backend Developer", "Amazon", "Pune", "Full-time", "Design scalable services in Java and AWS.", 7),
    ("Data Analyst", "TCS", "Bangalore", "Full-time", "Turn business data into insights using SQL and Power BI.", 12),
    ("Software Engineering Intern", "Microsoft", "Hyderabad", "Internship", "6-month internship with the developer tools team.", 4),
    ("ML Engineer", "Flipkart", "Bangalore", "Full-time", "Work on ranking and recommendation models.", 9),
    ("Product Design Intern", "Zomato", "Gurugram", "Internship", "Help design our next-generation ordering flow.", 11),
    ("Embedded Engineer", "Qualcomm", "Bangalore", "Full-time", "Firmware for mobile SoCs.", 5),
    ("Remote Python Contractor", "Razorpay", "Remote", "Contract", "Payment-platform automation and tooling.", 8),
]

EVENTS = [
    ("Annual Alumni Meet", "Reconnect with batchmates, meet faculty and enjoy an evening of celebration.", "TSEC Auditorium, Mumbai", 20, "Reunion"),
    ("AI Workshop", "Hands-on workshop on building applications with large language models.", "Seminar Hall", 35, "Workshop"),
    ("Career Fireside Chat", "Alumni from Google, Microsoft and Amazon share how they built their careers.", "Online (Zoom)", 12, "Webinar"),
    ("Startup Founders Panel", "Alumni founders talk fundraising, failure and finding product-market fit.", "Innovation Lab", 50, "Panel"),
    ("Batch of 2016 Reunion", "A decade later! Dinner and campus tour.", "Campus Lawns", -25, "Reunion"),
]

CAMPAIGNS = [
    ("Merit Scholarship Fund", "Fund tuition for meritorious students from low-income families.", 500000, [25000, 10000, 5000, 50000, 1000]),
    ("Innovation Lab Upgrade", "New equipment for the robotics and AI labs.", 300000, [15000, 20000, 5000]),
    ("Alumni Emergency Relief", "Support students facing sudden financial hardship.", 150000, [2000, 5000]),
]


def seed_demo_data(conn):
    pw = generate_password_hash(DEMO_PASSWORD)
    for (name, email, role, branch, year, company, desig, loc, skills, mentor, bio) in USERS:
        conn.execute(
            """INSERT INTO users (full_name,email,password,role,branch,graduation_year,company,
               designation,location,skills,open_to_mentor,bio) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)""",
            (name, email, pw, role, branch, year, company, desig, loc, skills, mentor, bio),
        )
    for title, company, loc, jtype, desc, poster in JOBS:
        conn.execute(
            "INSERT INTO jobs (title,company,location,job_type,description,posted_by) VALUES (?,?,?,?,?,?)",
            (title, company, loc, jtype, desc, poster),
        )
    today = date.today()
    for title, desc, venue, offset, cat in EVENTS:
        conn.execute(
            "INSERT INTO events (title,description,venue,event_date,category,created_by) VALUES (?,?,?,?,?,1)",
            (title, desc, venue, (today + timedelta(days=offset)).isoformat(), cat),
        )
    for title, desc, goal, gifts in CAMPAIGNS:
        cur = conn.execute("INSERT INTO campaigns (title,description,goal_amount) VALUES (?,?,?)", (title, desc, goal))
        for i, amt in enumerate(gifts):
            conn.execute(
                "INSERT INTO donations (campaign_id,user_id,amount) VALUES (?,?,?)",
                (cur.lastrowid, 3 + i % 8, amt),
            )
    conn.executemany(
        "INSERT INTO event_registrations (user_id,event_id) VALUES (?,?)",
        [(2, 1), (3, 1), (4, 1), (5, 2), (3, 2), (2, 3)],
    )
