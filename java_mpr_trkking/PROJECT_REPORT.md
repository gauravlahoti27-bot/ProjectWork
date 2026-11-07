# TrekMate — Engineering Project Report
**Academic Mini Project / MPR (2nd Year Computer Science & Engineering)**  
**Platform:** Spring Boot (Java 17) &bull; MySQL &bull; HTML5 &bull; CSS3 &bull; JavaScript  
**Repository Working Directory:** `c:\Aryan\java_mpr_trkking`

---

## 1. Executive Summary

**TrekMate** is an end-to-end web-based expedition planning, trip budgeting, virtual wallet management, and host-administered booking platform designed for trekking enthusiasts, group adventurers, and mountain expedition leaders. 

In traditional trekking websites, users can only view fixed packages. If an adventurer wants to customize extreme add-on sports (such as bungee jumping, white water rafting, or paragliding) or essential high-altitude facilities (such as emergency helicopter evacuation, bio-hygiene kits, nutritional ration plans, or porters), cost estimation becomes manual and opaque. Furthermore, adventurers frequently exceed their personal travel budgets without knowing what adjustments would bring the expedition within their financial reach.

**TrekMate** solves these challenges by providing:
1. **Budget-Based Trek Recommendations:** Dynamic matching and scoring algorithm that suggests suitable treks based on budget, group size, and difficulty preferences.
2. **Automatic Trip Cost Calculation:** Instant client-side and server-side calculation aggregating base package fees, per-person adventure sports, and per-group safety facilities.
3. **Virtual Wallet with Over-Budget Handling:** Cashless demo credit wallet with instant deficit detection, suggestions for deselecting optional high-cost activities or switching to cheaper alternative treks, and automated refunds on cancellation/rejection.
4. **Host-Managed Date Selection & Booking:** Dedicated expedition leader interface to schedule departure batches, manage slot capacities, review submitted trekker requests, and lock slots upon approval.

---

## 2. Complete Inventory of Features (30 Blueprints)

### Module 1: User Module
* **Feature 1:** User registration with name, email, password, phone, and role selection (Trekker or Host).
* **Feature 2:** User login and authentication with session persistence and 1-click demo login buttons.
* **Feature 3:** Browse available treks, destinations, base camp locations, and geographical states.
* **Feature 4:** View comprehensive trek details: difficulty level, duration (Days/Nights), peak altitude, fitness level, best season, and estimated base cost per person.
* **Feature 5:** Input user budget, traveling group size, and difficulty preferences.
* **Feature 6:** Receive ranked trek recommendations based on total group budget with compatibility match badges.

### Module 2: Trek Planning Module
* **Feature 7:** Select any trek to launch the interactive custom planner.
* **Feature 8:** View and select available add-on extreme activities (e.g., Bungee Jumping, River Rafting, Paragliding, Zip-lining, Rock Climbing, Acoustic Campfire).
* **Feature 9:** View and select essential expedition facilities (e.g., Emergency Helicopter Evacuation Insurance, Certified Wilderness Guide, High-Nutrition Food Plan, Eco-Hygiene Toilet Kit, Porter Baggage Service, High-Altitude Cold Gear Rental).
* **Feature 10:** Calculate total trip cost automatically in real-time as checkboxes and group size are modified.
* **Feature 11:** Compare customized trip cost against the user's budget with interactive visual indicators.
* **Feature 12:** Automatically suggest cheaper activities to deselect or alternative lower-cost treks if the user exceeds their budget.

### Module 3: Virtual Wallet Module
* **Feature 13:** Display current virtual demo credit balance in real-time across navigation bar and profile.
* **Feature 14:** Automatically allocate initial demo credits upon registration (₹35,000 for new trekkers).
* **Feature 15:** Top-up virtual wallet with demo credits (+₹5,000, +₹10,000, +₹25,000, or custom amounts).
* **Feature 16:** Automatically deduct virtual credits from wallet upon booking request submission.
* **Feature 17:** Display remaining credits or highlight the exact deficit amount required if wallet balance is insufficient.
* **Feature 18:** Maintain an audited transaction ledger recording transaction types (CREDIT / DEBIT), timestamps, reference codes, descriptions, and post-transaction balances.

### Module 4: Booking Module
* **Feature 19:** View upcoming available departure batches scheduled for each trek.
* **Feature 20:** Select preferred departure date with live capacity indicator (remaining available slots).
* **Feature 21:** Submit customized booking requests with activity line items and medical/dietary notes.
* **Feature 22:** Receive booking confirmation with unique reference code (e.g., `TRK-261023-XXXX`).
* **Feature 23:** Track real-time booking status (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`).
* **Feature 24:** View separated upcoming and previous bookings with option to cancel and trigger an instant 100% wallet refund.

### Module 5: Host / Leader Module
* **Feature 25:** Dedicated Host/Expedition Leader login with dashboard analytics.
* **Feature 26:** Add new trek packages with full parameters (title, location, base price, altitude, difficulty, duration).
* **Feature 27:** Edit existing trek details or deactivate expeditions.
* **Feature 28:** Manage adventure activities and facilities catalog (update prices, toggle active/inactive status).
* **Feature 29:** Manage departure dates and participant slot capacities for any trek.
* **Feature 30:** Review participant booking requests: approve (locks batch slots) or reject (records reason and triggers automated instant wallet refund).

### Module 6: Database & Backend Architecture
* **Feature 31:** Relational MySQL database schema with normalized entities (`users`, `treks`, `trek_dates`, `activities`, `bookings`, `booking_items`, `wallet_transactions`).
* **Feature 32:** Java 17 and Object-Oriented Programming (OOP) for all domain modeling and business logic.
* **Feature 33:** Spring Boot 3 with Spring Data JPA for data access and transactional boundaries.
* **Feature 34:** Clean MVC architecture separating Controllers, Services, Repositories, Models, and DTOs.
* **Feature 35:** Responsive frontend constructed strictly with standard HTML5, CSS3, and JavaScript.
* **Feature 36:** Rigorous server-side and client-side input validation, error handling, and capacity safeguards.

---

## 3. Deep Dive into the Four Main Highlights

### Highlight 1: Budget-Based Trek Recommendations
The recommendation engine analyzes the user's total budget \(B\), traveling group size \(G\), and preferred difficulty. 
For every active trek in the database, the total baseline cost for the group is:
\[
C_{\text{base\_group}} = P_{\text{base}} \times G
\]
A realistic essentials buffer (nutrition plan + emergency medical kit) is added to estimate realistic expedition expenditure:
\[
C_{\text{est\_total}} = C_{\text{base\_group}} + (P_{\text{essentials}} \times G)
\]
Treks are evaluated against the user budget:
- If \(C_{\text{base\_group}} \le B\): Ranked as **Within Budget**. Treks utilizing 70% to 95% of the budget are tagged **"Best Value"** (Match Score: 95%). Treks using under 70% are tagged **"Budget Friendly"** (Score: 85%).
- If \(C_{\text{base\_group}} > B\): Evaluated for proximity to the budget. If within 20% overdraft, tagged **"Slight Stretch"** (Score: 65%), allowing the user to consider minor budget adjustments.

### Highlight 2: Automatic Trip Cost Calculation
The pricing calculation implements an object-oriented strategy distinguishing between individual (per-person) and team (flat-group) line items:
\[
\text{Total Cost} = (P_{\text{base}} \times G) + \sum_{i \in \text{Activities}} (\text{Price}_i \times M_i) + \sum_{j \in \text{Facilities}} (\text{Price}_j \times M_j)
\]
Where multiplier \(M_k = G\) if the pricing type is `PER_PERSON`, and \(M_k = 1\) if `PER_GROUP`.

On the client side, vanilla JavaScript event listeners on group size spinners, budget inputs, and activity checkboxes send asynchronous requests to `/api/calculate-cost`, dynamically refreshing the breakdown card and budget comparison bar without page reloads.

### Highlight 3: Virtual Wallet with Over-Budget Handling
To simulate real-world fintech transactions in an academic setting:
- New users receive ₹35,000 in virtual credits upon signup.
- When customized cost exceeds user budget:
  1. The system detects the deficit: \(\text{Deficit} = \text{Total Cost} - B\).
  2. The algorithm scans selected activities in reverse order of cost, proposing specific actions (e.g., *"Deselect Bungee Jumping to save ₹3,500 per person"*).
  3. The system queries other active treks where \((P_{\text{alt\_base}} \times G) \le B\) and displays alternative cheaper treks.
- During booking submission, the wallet is checked. If credits are sufficient, a `DEBIT` transaction is logged and funds are locked. If the host rejects the booking or the user cancels, a `CREDIT` refund transaction is automatically generated.

### Highlight 4: Host-Managed Date Selection and Booking
Expedition batches are modeled with finite capacity:
- A `TrekDate` record stores `total_slots`, `booked_slots`, and `status`.
- Users can only book batches with available capacity \(\ge G\).
- Once a booking is submitted, status is `PENDING`.
- When the Host clicks **Approve**, `booked_slots` increments by \(G\). If `booked_slots == total_slots`, the batch automatically switches to `FULL`.
- If a booking is cancelled, `booked_slots` decrements by \(G\) and status reverts to `OPEN`.

---

## 4. Technology Stack & Language Compliance

In strict adherence to project constraints, **only Java, JavaScript, HTML, CSS, and MySQL** have been used:
- **Backend Application Logic:** Java 17 (OOP, Encapsulation, Polymorphism, Spring MVC, Spring Data JPA, Hibernate).
- **Web APIs & Framework:** Spring Boot 3.2.5.
- **Database Engine:** MySQL 8.x (`trekmate_db`).
- **Web Frontend:** HTML5 (semantic layouts, Thymeleaf templating).
- **User Interface Styling:** Pure CSS3 (modern flexbox, CSS grid, variables, responsive breakpoints).
- **Client-Side Interactivity:** Pure Vanilla JavaScript (Fetch API, DOM manipulation, real-time math calculations).

---

## 5. Relational Database Schema (MySQL)

```
       +--------------------+
       |       users        |
       +--------------------+
       | id (PK)            |<-----------------------+
       | full_name          |                        |
       | email (UNIQUE)     |                        |
       | password           |                        |
       | role (USER/HOST)   |                        |
       | wallet_balance     |                        |
       +--------------------+                        |
                 | 1                                 | 1
                 |                                   |
                 | N                                 | N
       +--------------------+              +--------------------+
       | wallet_transactions|              |      bookings      |
       +--------------------+              +--------------------+
       | id (PK)            |              | id (PK)            |
       | user_id (FK)       |              | booking_code (UQ)  |
       | transaction_type   |              | user_id (FK)       |
       | amount             |              | trek_id (FK)-------+--> [treks]
       | balance_after      |              | trek_date_id (FK)--+--> [trek_dates]
       | reference_code     |              | group_size         |
       +--------------------+              | base_cost          |
                                           | total_cost         |
                                           | status             |
                                           +--------------------+
                                                     | 1
                                                     |
                                                     | N
                                           +--------------------+
                                           |   booking_items    |
                                           +--------------------+
                                           | id (PK)            |
                                           | booking_id (FK)    |
                                           | activity_id (FK)---+--> [activities]
                                           | unit_price         |
                                           | quantity           |
                                           | subtotal           |
                                           +--------------------+
```

### Table Definitions:
1. **`users`**: Stores user authentication credentials, contact details, account role (`USER` or `HOST`), and current virtual wallet balance.
2. **`treks`**: Stores mountain expeditions, destination names, difficulty levels, duration, base price, altitude, and itinerary.
3. **`trek_dates`**: Stores batch departures, start/end dates, total capacity, and booked slots for each trek.
4. **`activities`**: Catalog of add-on adventure activities and support facilities with pricing model (`PER_PERSON` vs `PER_GROUP`).
5. **`bookings`**: Stores submitted expedition reservations, group sizes, calculated costs, special medical/dietary notes, and host approval status.
6. **`booking_items`**: Line items representing each specific activity or facility chosen in a booking with quantity and subtotal.
7. **`wallet_transactions`**: Complete audit trail of virtual wallet credits, debits, top-ups, and cancellation refunds.

---

## 6. Object-Oriented Programming (OOP) Architecture

The application demonstrates core software engineering design patterns:
1. **Encapsulation:** Entities (`User`, `Trek`, `Booking`, `Activity`, `WalletTransaction`) encapsulate internal state with private fields, validation constraints, and business helper methods (e.g., `hasCapacityFor()`, `isPerPerson()`, `isCredit()`).
2. **Layered Separation of Concerns:**
   - **Model Layer:** Domain entities mapped to relational database tables via JPA annotations.
   - **Repository Layer:** Spring Data JPA interfaces providing declarative CRUD and JPQL query methods.
   - **Service Layer:** Transactional business logic orchestrating wallet debits/credits, recommendations, and capacity management.
   - **Controller Layer:** Handles HTTP routing, model binding, and REST endpoint serialization.
   - **DTO Layer:** Decoupled data transfer objects (`CostCalculationRequest`, `CostCalculationResponse`, `RecommendationRequest`, `BookingRequestDto`).
3. **Defensive Programming & Fail-Safe Database Resilience:**
   - The custom `DataSourceConfig` attempts connection to the primary MySQL server (`localhost:3306/trekmate_db`). If the host machine does not have MySQL running during a presentation, it automatically falls back to an embedded in-memory H2 database with MySQL dialect, ensuring the project never crashes unexpectedly during academic evaluation.

---

## 7. How to Run & Test the Application

### Prerequisites:
- Java Development Kit (JDK 17) & Maven (bundled in `C:\Aryan\tools`)
- MySQL Server (optional, runs automatically with MySQL or seamless embedded fallback)

### Launch Instructions:
1. Double click **`run.bat`** in the project root (`c:\Aryan\java_mpr_trkking\run.bat`), or run in terminal:
   ```cmd
   mvn spring-boot:run
   ```
2. Open your web browser and navigate to:
   ```
   http://localhost:8080
   ```

### Default Demo Credentials:
- **Trekker Account:**
  - Email: `trekker@example.com`
  - Password: `pass123`
  - Pre-loaded Virtual Wallet: **₹45,000.00**
- **Host / Expedition Leader Account:**
  - Email: `host@example.com`
  - Password: `admin123`
  - Role: `HOST` (Access to Host Dashboard, Add Treks, Manage Dates, Approve/Reject Bookings)

---

## 8. Verification & Test Walkthrough

1. **Test Budget Recommendations:**
   - Click "Budget Recommender" in the navbar.
   - Enter Budget: ₹30,000, Group Size: 2, Difficulty: Easy.
   - Observe ranked results with match percentages, remaining budget calculation, and "Best Value" badges.
2. **Test Custom Trek Planning & Live Costing:**
   - Select "Kedarkantha Winter Snow Summit" and click "Plan & Customize".
   - Toggle checkboxes for "White Water River Rafting" and "Emergency Heli-Evac Insurance".
   - Notice the instant real-time cost recalculation without page reload.
   - Enter a budget lower than the total (e.g., ₹10,000); observe the red "Budget Exceeded" alert with suggestions of cheaper activities to deselect and alternative treks.
3. **Test Cashless Booking via Virtual Wallet:**
   - Select an available batch date and click "Confirm & Submit Booking".
   - Credits are deducted from your virtual wallet and held in pending state.
   - Review your booking receipt on the confirmation page.
4. **Test Host Approval & Capacity Locking:**
   - Log out and log in as Host (`host@example.com` / `admin123`).
   - Open "Participant Booking Approvals".
   - Click "Approve" on the pending booking.
   - Notice slots are reserved and locked on the trek date.
5. **Test Rejection & Instant Wallet Refund:**
   - Submit a new booking as trekker.
   - In Host portal, click "Reject".
   - Check the trekker's Virtual Wallet: the full amount is immediately credited back with a refund transaction entry!

---
*Prepared by Aryan Sharma — Department of Computer Engineering*

