# ⛰️ TrekMate — Smart Trek Planning & Virtual Wallet System

**2nd Year Engineering Mini Project (MPR)**  
Built strictly with **Java (Spring Boot), JavaScript, HTML5, CSS3, and MySQL**.

---

## 🌟 The Four Main Highlights
1. **Budget-Based Trek Recommendations:** Automatically matches and ranks expeditions according to user budget, group size, and difficulty preferences.
2. **Automatic Trip Cost Calculation:** Real-time client-side and server-side calculation formula aggregating base package costs, per-person adventure add-on sports (bungee jumping, river rafting, paragliding), and per-group safety facilities.
3. **Virtual Wallet with Over-Budget Handling:** Cashless demo credit wallet with instant deficit detection, advice on deselecting high-cost activities or choosing cheaper alternative treks, and automated refunds on cancellation/rejection.
4. **Host-Managed Date Selection and Booking:** Dedicated Expedition Leader interface to schedule departures, configure slot capacities, review incoming requests, and lock participant capacity upon approval.

---

## 🚀 Quick Start (1-Click Run)

### 1. Launch the Server
Simply double-click:
```cmd
run.bat
```
*(or run `mvn spring-boot:run` in terminal)*

The server will start on port 8080:
```
http://localhost:8080
```

### 2. Demo User Credentials (Pre-loaded)
| Role | Email | Password | Features / Permissions |
| :--- | :--- | :--- | :--- |
| **Trekker** | `trekker@example.com` | `pass123` | Pre-loaded with **₹45,000.00** demo credits, custom planning, booking, and wallet management. |
| **Host / Leader** | `host@example.com` | `admin123` | Add/Edit treks, manage activities catalog & prices, schedule departure dates & capacities, approve/reject bookings. |

---

## 🗄️ Database Setup (MySQL)

TrekMate is built with a resilient dual-mode database engine:
1. **Local MySQL Server (Primary):**
   - Import `trekmate_mysql.sql` into MySQL Workbench or phpMyAdmin.
   - Default connection configured in `src/main/resources/application.properties`:
     - URL: `jdbc:mysql://localhost:3306/trekmate_db`
     - Username: `root`
     - Password: `root`
2. **Automatic Zero-Crash Fallback:**
   - If MySQL is not currently running when presenting or testing, the application's `DataSourceConfig` automatically initializes an in-memory database with MySQL dialect so TrekMate can run immediately without crashes!

---

## 📄 Project Report
A complete academic project report covering all 30 features, architecture, mathematical formulas, and ER schema is documented in:
👉 [`PROJECT_REPORT.md`](file:///c:/Aryan/java_mpr_trkking/PROJECT_REPORT.md)

