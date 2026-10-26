# ChinookAir - Action Plan

Our step-by-step plan for the ENSF 614 term project. Every step points back
to the part of the guideline it covers, so nothing gets missed.

Each numbered task has its own GitHub issue, linked next to it. Tick it
off here when the issue is closed.

**Marks:** Design phase 50 + Implementation phase 50.

**Golden rule:** requirements -> use cases -> scenarios -> classes, in that
order. The class diagram has to trace back to the scenarios, and we lose
marks for any class that can't be traced.

---

## Phase 0 - Setup and decisions

- [x] **0.1 Repo, Maven build, layered packages, Checkstyle, CI**
- [ ] **0.2 Confirm open questions with the instructor** ([#4](https://github.com/satyambhanot/ChinookAir/issues/4))
  - Deadlines for both phases, group size, page limit for the report
  - Do we resubmit an updated design PDF with the implementation? (Criterion 5
    marks the UML against the *final* app)
  - Promotion rule: once per month for the app, or once per customer?
  - Done when: answers are written into this file under "Decisions"
- [ ] **0.3 Pick the tools** ([#5](https://github.com/satyambhanot/ChinookAir/issues/5))
  - UML: draw.io (files saved in `docs/diagrams/`, exported as PNG)
  - Report: Word or LaTeX. **Not Google Docs** - it can't do the double
    underline the scenarios need (LaTeX: `ulem` package, `\uline` / `\uuline`)
- [ ] **0.4 Lock in key decisions and the glossary** ([#6](https://github.com/satyambhanot/ChinookAir/issues/6)) (see "Decisions" below)

## Phase 1 - Requirements (design doc: requirements section)

- [ ] **1.1 Functional requirements list** ([#7](https://github.com/satyambhanot/ChinookAir/issues/7)) with IDs
  - FR-1 Search flights by origin, destination, date and airline (3.1)
  - FR-2 Show available seats, times and prices (3.1)
  - FR-3 Create a reservation (3.2)
  - FR-4 Cancel or modify a reservation (3.2)
  - FR-5 Generate a booking confirmation, also saved to a .txt file (3.2)
  - FR-6 Create, view and edit customer profiles (3.3)
  - FR-7 Add, update and remove flights - admin only (3.4)
  - FR-8 Manage routes, aircraft and schedules - admin (2.1)
  - FR-9 Record simulated payments and keep payment records (3.5, 2)
  - FR-10 View reservation history and flight schedules (2)
  - FR-11 Store all data in SQLite (3.6)
  - FR-12 Monthly promotion, picked at random, shown once a month (3.7)
  - FR-13 Login with roles: customer, flight agent, admin (our addition)
  - FR-14 Seat selection (our addition)
- [ ] **1.2 Non-functional requirements** ([#8](https://github.com/satyambhanot/ChinookAir/issues/8))
  - From the guideline: usability, portability, maintainability, scalability
  - From the marking criteria: **reusability** and **reliability**
  - Ours: security (hashed passwords), data integrity (no double booking)
  - For each one, write *how* the design meets it, not just the name
- [ ] **1.3 Assumptions and glossary** ([#9](https://github.com/satyambhanot/ChinookAir/issues/9)) written into the report

## Phase 2 - Use cases (Section B)

- [ ] **2.1 Use-case table** ([#10](https://github.com/satyambhanot/ChinookAir/issues/10)) mapping each use case to actor, FR and control class

  | Use case | Actors | FR | Control class |
  |---|---|---|---|
  | Login / Logout | all | FR-13 | `LoginController` |
  | Register Customer | Customer, Agent | FR-6 | `CustomerController` |
  | Manage Customer Profile | Agent (Customer: own profile) | FR-6 | `CustomerController` |
  | Search Flights | Customer, Agent | FR-1, FR-2 | `FlightSearchController` |
  | View Flight Details | Customer, Agent | FR-2 | `FlightSearchController` |
  | Book Flight (includes Select Seat, Make Payment) | Customer, Agent | FR-3, FR-14 | `BookingController` |
  | Make Payment | Customer, Agent | FR-9 | `PaymentController` |
  | View Payment Records | Agent | FR-9 | `PaymentController` |
  | Cancel Reservation | Customer, Agent | FR-4 | `ReservationController` |
  | Modify Reservation | Agent | FR-4 | `ReservationController` |
  | View Reservation History | Customer, Agent | FR-10 | `ReservationController` |
  | View Flight Schedules | Agent | FR-10 | `ScheduleController` |
  | Update Schedules | Admin | FR-8 | `ScheduleController` |
  | Manage Flights (add / update / remove) | Admin | FR-7 | `FlightManagementController` |
  | Manage Routes | Admin | FR-8 | `RouteController` |
  | Manage Aircraft | Admin | FR-8 | `AircraftController` |
  | View Monthly Promotion | Customer | FR-12 | `PromotionController` |

- [ ] **2.2 Use-case diagram** ([#11](https://github.com/satyambhanot/ChinookAir/issues/11))
  - Abstract `User` actor with Customer, Flight Agent and Admin under it
  - `<<include>>`: Book Flight -> Select Seat, Make Payment
  - `<<extend>>`: e.g. View Flight Details -> Search Flights
  - Every FR from 1.1 shows up in at least one use case

## Phase 3 - Use-case scenarios (Section C)

Three scenarios, picked so that between them they mention **every** class
we plan to have. Single underline = candidate class, double underline =
candidate operation. Each one has actors, preconditions, main flow,
alternate flows and postconditions.

- [ ] **3.1 Book Flight** ([#12](https://github.com/satyambhanot/ChinookAir/issues/12)) (customer) - end to end: log in, see the monthly
  promotion, search by airline/date, view details, pick a seat, pay, get a
  confirmation saved to a file. Alternates: seat taken, payment declined.
- [ ] **3.2 Manage Flights** ([#13](https://github.com/satyambhanot/ChinookAir/issues/13)) (admin) - add a flight on a route with an
  airline and aircraft, update its schedule, remove an outdated flight.
  Alternates: route doesn't exist yet so the admin adds it, aircraft
  already in use, flight still has reservations.
- [ ] **3.3 Modify Reservation** ([#14](https://github.com/satyambhanot/ChinookAir/issues/14)) (agent) - find the customer, update their
  profile, change their reservation to another flight, check payment
  records, refund the difference. Alternate: new flight full.
- [ ] **3.4 Candidate class and operation list** ([#15](https://github.com/satyambhanot/ChinookAir/issues/15)) pulled from the underlines.
  Done when: every class planned for the class diagram is in this list.

## Phase 4 - Intro and activity diagrams (Section A)

- [ ] **4.1 System intro** ([#16](https://github.com/satyambhanot/ChinookAir/issues/16)) (purpose, users, scope) and short descriptions
  of the main processes: search and select, book, pay
- [ ] **4.2 Activity diagrams** ([#17](https://github.com/satyambhanot/ChinookAir/issues/17)) - use swimlanes (User / System), decisions
  with guards, start and end nodes
  - [ ] Login
  - [ ] Browsing for and selecting a flight
  - [ ] Booking a flight
  - [ ] Making a payment

## Phase 5 - Class diagram (Section F)

- [ ] **5.1 First-cut class diagram** ([#18](https://github.com/satyambhanot/ChinookAir/issues/18)) from the 3.4 list
  - `<<entity>>`: User (abstract), Customer, FlightAgent, Admin, Airline,
    Airport, Route, Aircraft, Flight, Seat, Reservation, Payment, Promotion
  - `<<control>>`: the controllers from the 2.1 table
  - `<<boundary>>`: one view per screen (LoginView, FlightSearchView, ...)
  - Attributes and operations with types, visibility, multiplicity on both
    ends of every association
  - Leave out getters, setters, constructors and library classes
- [ ] **5.2 Refined class diagram** ([#19](https://github.com/satyambhanot/ChinookAir/issues/19)) - the version that goes in the report
  - Patterns, each with one line on *why*:
    - Singleton - `DatabaseConnection`
    - DAO - one interface per entity, SQLite class behind it
    - Strategy - `PaymentMethod` (credit card, debit, ...)
    - State - Reservation lifecycle (matches the state diagram)
    - Factory - creating the right `User` subclass at login
    - Observer - promotion notification
  - Use inheritance, realization, aggregation and composition where they
    actually fit (e.g. Flight *composes* Seats)
  - Done when: every class traces to a scenario and every use case has a
    control class

## Phase 6 - Sequence diagrams (Section D)

Each one goes boundary -> control -> entity / DAO, and the control class
must exist in the class diagram.

- [ ] **6.1 Search Flights** ([#20](https://github.com/satyambhanot/ChinookAir/issues/20)) - `FlightSearchController`
- [ ] **6.2 Book Flight** ([#21](https://github.com/satyambhanot/ChinookAir/issues/21)) - `BookingController`
- [ ] **6.3 Make Payment** ([#22](https://github.com/satyambhanot/ChinookAir/issues/22)) - `PaymentController`
- [ ] **6.4 Manage Flights** ([#23](https://github.com/satyambhanot/ChinookAir/issues/23)) - `FlightManagementController`

## Phase 7 - State diagrams (Section E)

- [ ] **7.1 Whole system** ([#24](https://github.com/satyambhanot/ChinookAir/issues/24)) - Starting -> Promotion check -> Login ->
  Customer / Agent / Admin session -> Logged out -> Closed
- [ ] **7.2 Payment** ([#25](https://github.com/satyambhanot/ChinookAir/issues/25)) - Pending -> Processing -> Approved / Declined;
  Approved -> Refunded
- [ ] **7.3 Reservation** ([#26](https://github.com/satyambhanot/ChinookAir/issues/26)) - Created -> Pending payment -> Confirmed ->
  Modified / Cancelled -> Completed (after the flight departs)

## Phase 8 - Package diagram (Section G)

- [ ] **8.1 Package diagram** ([#27](https://github.com/satyambhanot/ChinookAir/issues/27))
  - Presentation -> Business (control, entity) -> Data, with dependency arrows
  - List the classes in each package
  - Mark public (`+`) vs package-private (`~`): DAO interfaces public,
    SQLite DAO classes package-private
  - Must match the real Java packages in `src/`

## Phase 9 - Finish and submit the design document

- [ ] **9.1 Traceability matrix** ([#28](https://github.com/satyambhanot/ChinookAir/issues/28)): FR -> use case -> scenario ->
  sequence diagram -> control class -> entities
- [ ] **9.2 Consistency review** ([#29](https://github.com/satyambhanot/ChinookAir/issues/29)) - same names everywhere (Reservation,
  not booking/ticket), multiplicities make sense, no orphan classes
- [ ] **9.3 Put the report together** ([#30](https://github.com/satyambhanot/ChinookAir/issues/30)) - title page with names, table of
  contents, sections in guideline order (A-G), every diagram readable
- [ ] **9.4 Export PDF and submit** ([#31](https://github.com/satyambhanot/ChinookAir/issues/31)). Merge `dev` into `main`, tag `design-v1`

---

## Phase 10 - Data layer

- [ ] **10.1 Schema and seed data** ([#32](https://github.com/satyambhanot/ChinookAir/issues/32)) - `schema.sql`, plus airlines,
  airports, aircraft, sample flights and one login per role
- [ ] **10.2 `DatabaseConnection`** ([#33](https://github.com/satyambhanot/ChinookAir/issues/33)) (Singleton) - creates and seeds the DB
  on first run, kept in the user's home folder so it works from anywhere
- [ ] **10.3 DAOs** ([#34](https://github.com/satyambhanot/ChinookAir/issues/34)) - interface + SQLite class per entity, with tests
  against an in-memory DB

## Phase 11 - Entities

- [ ] **11.1 Entity classes** ([#35](https://github.com/satyambhanot/ChinookAir/issues/35)) matching the class diagram, with rules
  (seat availability, price, what can be cancelled), plus tests
- [ ] **11.2 Reservation State pattern** ([#36](https://github.com/satyambhanot/ChinookAir/issues/36)) and **Payment Strategy pattern**,
  plus tests

## Phase 12 - Controllers

- [ ] **12.1 `LoginController`** ([#37](https://github.com/satyambhanot/ChinookAir/issues/37)) - hashed passwords, User Factory
- [ ] **12.2 `FlightSearchController`** ([#38](https://github.com/satyambhanot/ChinookAir/issues/38)) - filter by origin, destination,
  date, airline
- [ ] **12.3 `BookingController`** ([#39](https://github.com/satyambhanot/ChinookAir/issues/39)) - no double booking, confirmation
  number, confirmation .txt file (this is our file access)
- [ ] **12.4 `PaymentController`** ([#40](https://github.com/satyambhanot/ChinookAir/issues/40)) - simulated payment, records, refunds;
  store masked card numbers only
- [ ] **12.5 `ReservationController`** ([#41](https://github.com/satyambhanot/ChinookAir/issues/41)) - cancel, modify, history
- [ ] **12.6 `CustomerController`** ([#42](https://github.com/satyambhanot/ChinookAir/issues/42)) - register, view, edit
- [ ] **12.7 `FlightManagementController`, `RouteController`,
  `AircraftController`, `ScheduleController`** ([#43](https://github.com/satyambhanot/ChinookAir/issues/43)) - what happens to
  reservations on a removed flight
- [ ] **12.8 `PromotionController`** ([#44](https://github.com/satyambhanot/ChinookAir/issues/44)) - random pick, once a month,
  remembered in the DB. Clock and random generator passed in so it can be
  tested

## Phase 13 - GUI (Swing)

- [ ] **13.1 Login screen** ([#45](https://github.com/satyambhanot/ChinookAir/issues/45)) and one dashboard per role
- [ ] **13.2 Customer screens** ([#46](https://github.com/satyambhanot/ChinookAir/issues/46)) - search, results, details, seat pick,
  payment, confirmation, my reservations, promotion popup
- [ ] **13.3 Agent screens** ([#47](https://github.com/satyambhanot/ChinookAir/issues/47)) - customers, modify reservation, payment
  records, schedules
- [ ] **13.4 Admin screens** ([#48](https://github.com/satyambhanot/ChinookAir/issues/48)) - flights, routes, aircraft, schedules
- [ ] **13.5 Validation and error messages** ([#49](https://github.com/satyambhanot/ChinookAir/issues/49)) on every form

## Phase 14 - Wrap up and submit

- [ ] **14.1 End-to-end test** ([#50](https://github.com/satyambhanot/ChinookAir/issues/50)) of every use case in the 2.1 table, per role
- [ ] **14.2 Update the UML** ([#51](https://github.com/satyambhanot/ChinookAir/issues/51)) so it matches the final code, and update the
  report PDF
- [ ] **14.3 Packaging** ([#52](https://github.com/satyambhanot/ChinookAir/issues/52)) - fat jar, silence the SQLite native-access warning
  on newer JDKs, test on a Windows machine with a clean DB
- [ ] **14.4 Submit** ([#53](https://github.com/satyambhanot/ChinookAir/issues/53)) - ZIP with `ChinookAir.jar` + a ZIP of the `.java`
  source (and the updated PDF if allowed). Merge `dev` into `main`, tag `v1.0`

---

## Marking checklist

| Criterion from the guideline | Where we cover it |
|---|---|
| 1. Separation of concerns (entities, controllers, views) | Packages + layer rules in CONTRIBUTING, 5.2, 8.1 |
| 2. Modularity, cohesion, low coupling | DAO interfaces, one controller per use case, 5.2 |
| 3. Usability, reusability, maintainability, reliability | 1.2, 13.5, tests, Checkstyle + CI |
| 4. Boundary / entity / control split | Stereotypes in 5.2, packages in 8.1 |
| 5. UML matches the final app | 14.2 |
| 6. Use-case coverage + traceability to class diagram | 2.1, 3.4, 9.1 |
| 7. Quality of scenarios and sequence diagrams | Phase 3, Phase 6 |

## Decisions

Fill these in as we agree on them.

| Topic | Decision |
|---|---|
| Word for a booking | **Reservation** (not booking or ticket) |
| Passengers per reservation | One (keeps it simple) |
| Promotion | Picked on the first launch in a new month, shown once that month (confirm in 0.2) |
| Removing a flight with reservations | TBD - block it, or cancel and refund |
| Cancel rules | TBD - e.g. allowed until departure |
| Passwords | Stored hashed, never plain text |
| Card numbers | Only the last 4 digits stored |
