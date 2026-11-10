# ChinookAir - Master Plan

A single-person, milestone-based plan that maps every part of the ENSF 614 term project guideline to small issues. It replaces the earlier [action plan](action-plan.md) and its 50 issues. The issue texts live in [backlog/](backlog/) and are created with [scripts/create_issues.rb](../scripts/create_issues.rb).

Status: in use. The 75 issues exist on GitHub as #56 to #130 (linked in the index at the bottom). The old issues #4 to #53 were closed as superseded, each with a comment naming its replacements.

## 1. How this plan works

- **75 issues, 201 hours.** Every issue is 4 hours or less (the script refuses anything longer).
- **Strict order.** An issue only depends on earlier ones, so there is never a missing prerequisite. The script checks this.
- **One loop per issue:** branch off `dev`, small commits, run the checklist, PR into `dev`, CI green, merge, close.
- **Stack:** Java 17, Swing, SQLite (`sqlite-jdbc`), Maven, JUnit 5, Checkstyle, ArchUnit, GitHub Actions. Details in [CONTRIBUTING.md](../CONTRIBUTING.md).
- **Every issue has the same shape:** title, milestone, context and objective (with the rubric item it satisfies), implementation guide, testing strategy with concrete inputs and expected outputs, and a definition of done.

### Definition of done

Every issue ends with these four boxes (also in the PR template, P0-05):

- [ ] Code linter run and passed.
- [ ] Unit tests written and passing.
- [ ] Adheres to strict OOP and project coding standards.
- [ ] Rubric requirement explicitly verified.

For code issues the first three mean `mvn verify` (Checkstyle 0 violations), `mvn test`, and the layer rules in CONTRIBUTING.md plus `ArchitectureTest`. For document and diagram issues there is nothing to lint or unit test, so the same boxes mean: `mvn verify` still passes, every check in the Testing Strategy is ticked (grep and count commands included), and names match the glossary and class diagram exactly. The fourth box always names the specific guideline requirement.

## 2. Why this order

The brief for this plan says foundation first, then logic, then UI. Two things are different here because of the guideline:

1. **Design comes first, as a graded deliverable.** The design phase is 50 marks and is submitted as a PDF. The class and package diagrams *are* the architecture, and the guideline says classes must trace to the use-case scenarios. So the foundation of this project is requirements, use cases, scenarios and the class diagram (Phases 1 to 4). Code is written to that design.
2. **Entities before the database.** DAOs return entities, so the domain model (Phase 5) comes before the data layer (Phase 6). Both are pure Java with no GUI, which keeps them fast to test.

Then business logic (Phase 7), then the GUI last (Phase 8), then integration and packaging (Phase 9).

## 3. Milestones

| Phase | Goal | Issues | Hours | Done when |
|---|---|---|---|---|
| 0: Setup & Decisions | Decisions recorded, diagram and report tooling ready, PR template | 5 | 6 | Glossary exists, `make pdf` works, guideline checklist done |
| 1: Requirements & Use Cases | FR and NFR lists, use-case table and diagram, introduction | 5 | 11 | Every guideline requirement has an FR; every FR has a use case |
| 2: Scenarios & Domain Model | Three scenarios, candidate classes, first-cut class diagram | 6 | 15 | Every class traces to a scenario |
| 3: Behavioral Diagrams | 4 activity, 4 sequence, 3 state diagrams | 11 | 21 | Names match the class diagram |
| 4: Architecture & Design Report | Pattern refinement, package diagram, traceability, final PDF | 5 | 14 | PDF submitted, tag `design-v1` |
| 5: Domain Model | Entities, State and Strategy patterns, validation | 5 | 15 | All entity tests pass with no database |
| 6: Data Layer | Schema, singleton connection, DAOs, seeder, architecture tests | 10 | 30 | A fresh database seeds itself; `ArchitectureTest` green |
| 7: Business Logic | All controllers with role checks | 10 | 31 | Every use case works without the GUI |
| 8: GUI | Shell, validation helpers, all role screens | 13 | 43 | Every use case reachable from the right dashboard |
| 9: Integration & Submission | End-to-end test, test plan, UML sync, packaging, submit | 5 | 15 | Submission archive verified from scratch, tag `v1.0` |

Design side: 32 issues, 67 hours. Implementation side: 43 issues, 134 hours.

Rough calendar (deadlines are not in the guideline, see P0-02): at 20 hours a week this is about 10 weeks, at 30 hours a week about 7. The design deliverable (Phases 0 to 4) is a third of the work, about 67 hours.

## 4. Rubric coverage map

### Deliverables

| Guideline item | Issues |
|---|---|
| A. Introduction, process descriptions, 4 activity diagrams | P1-05, P3-01, P3-02, P3-03, P3-04 |
| B. Use-case diagram | P1-03, P1-04 |
| C. Three scenarios with underlines | P0-04, P2-01, P2-02, P2-03, P2-04 |
| D. Four sequence diagrams (each with a control class) | P3-05, P3-06, P3-07, P3-08 |
| E. Three state diagrams | P3-09, P3-10, P3-11 |
| F. Class diagram (stereotypes, patterns, refined) | P2-05, P2-06, P4-01 |
| G. Package diagram (public classes marked) | P4-02 |
| 7.2 Java app, SQLite, JAR plus source ZIP | Phases 5 to 9, P9-04, P9-05 |

### Functional requirements (section 3)

| Requirement | Issues |
|---|---|
| 3.1 Search and viewing | P5-03, P6-04, P7-03, P8-05 |
| 3.2 Booking management and confirmation | P5-04, P7-04, P7-05, P7-07, P8-06, P8-08 |
| 3.3 Customer management | P7-08, P8-09 |
| 3.4 Flight management (admin only) | P7-09, P7-10, P8-12, P8-13 |
| 3.5 Payment simulation | P5-05, P7-06, P8-07 |
| 3.6 Database connectivity | P6-01 to P6-10 |
| 3.7 Monthly promotion | P6-08, P7-02, P8-04 |

### Non-functional requirements (section 4)

| Requirement | Issues |
|---|---|
| Usability | P8-02 to P8-13 (messages, validation, keyboard use) |
| Portability | P6-02 (database created on first run), P9-04 (macOS, Windows, Linux CI) |
| Maintainability | P0-05, P6-09 (layer rules enforced by tests), Checkstyle in CI |
| Scalability | P4-01, P6-03 (DAO interfaces, controllers free of Swing) |
| Others (reusability, reliability, security) | P1-02, P6-01 (no double booking), P5-01, P5-05 (hashed passwords, masked cards) |

### Assessment criteria (section 6)

| Criterion | Issues |
|---|---|
| 1. Separation of concerns across components | P2-06, P4-02, P6-09 |
| 2. Modularity, cohesion, low coupling | P4-01, P6-09, P8-01 |
| 3. Non-functional requirements | P1-02, P9-04 |
| 4. Boundary, entity and control separation | P2-06, P6-09 |
| 5. UML clear and matches the final app | P0-03, P4-04, P9-03 |
| 6. Use-case coverage and traceability | P1-03, P1-04, P2-04, P4-03, P9-02 |
| 7. Quality of scenarios and sequence diagrams | P2-01, P2-02, P2-03, P3-05 to P3-08 |

## 5. Rules the plan fixes (so tests and diagrams agree)

These come from P0-01 and are repeated in the code issues. Change them in one place first if a decision changes.

| Topic | Rule |
|---|---|
| Word for a booking | Reservation. "Book" is only a verb. |
| Passengers | One per reservation, the customer who owns it |
| Promotion | Picked on the first launch of a calendar month, stored as `YYYY-MM`, shown once |
| Cancel | Allowed until departure, full refund |
| Remove a flight | Blocked while active reservations exist; archived if it only has old reservations; deleted if it has none |
| Seats | Economy x1.0, Business x1.5 (first two rows), numbered like `12A` |
| Simulated payment | Card ending `0000` declined; debit over 5000.00 declined; only last 4 digits stored |
| Modify a reservation | Same price keeps it confirmed; a different price refunds the old payment and goes back to pending payment |
| Reservation states | `CREATED`, `PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`, `COMPLETED` |
| Payment states | `PENDING`, `APPROVED`, `DECLINED`, `REFUNDED` |
| Passwords | PBKDF2 hash, never stored in plain text |
| Seat availability | Derived from active reservations; a partial unique index makes double booking impossible |

## 6. Risks and what to do about them

| Risk | Mitigation |
|---|---|
| The class diagram changes while coding | P9-03 syncs the UML at the end; keep a list of changes as they happen |
| Date handling bugs in SQLite (stored as text) | Dates are ISO-8601 strings, all tests use a fixed `Clock`, boundary tests in P6-04 |
| Swing threading mistakes | Views are thin, all DB work is fast and called from event handlers; no background threads needed |
| Double underline impossible in the report tool | P0-04 tests it on day one |
| Jar fails on the grader's machine | P9-04 self-test and a three-OS CI matrix |
| A decision turns out to conflict with the guideline | Rules table above is the single place to update, then the affected issues |
| Scope creep | Anything not in the guideline or this plan goes in a new issue, not into the current one |

## 7. Moving from the old issues

Issues #4 to #53 were written for a team and have been closed as superseded. The 75 new issues were created from `docs/backlog/` with `scripts/create_issues.rb`, which checks the backlog first and can be re-run safely (it skips issues that already exist). The old action plan stays in the repo for history.

If an issue text needs to change, edit the backlog file and the GitHub issue together so the two never disagree.

## 8. Issue index

#### Phase 0: Setup & Decisions

| ID | Issue | Hours | Depends on |
|---|---|---|---|
| P0-01 ([#56](https://github.com/satyambhanot/ChinookAir/issues/56)) | Record scope decisions and glossary | 1 | - |
| P0-02 ([#57](https://github.com/satyambhanot/ChinookAir/issues/57)) | Check the plan against the guideline line by line | 1 | P0-01 |
| P0-03 ([#58](https://github.com/satyambhanot/ChinookAir/issues/58)) | Set up the diagram workflow and style rules | 1 | - |
| P0-04 ([#59](https://github.com/satyambhanot/ChinookAir/issues/59)) | Create the design report skeleton with double-underline support | 2 | - |
| P0-05 ([#60](https://github.com/satyambhanot/ChinookAir/issues/60)) | Add a pull request template with the pre-commit checklist | 1 | - |

#### Phase 1: Requirements & Use Cases

| ID | Issue | Hours | Depends on |
|---|---|---|---|
| P1-01 ([#61](https://github.com/satyambhanot/ChinookAir/issues/61)) | Write the functional requirements list (FR-1 to FR-14) | 2 | P0-01 |
| P1-02 ([#62](https://github.com/satyambhanot/ChinookAir/issues/62)) | Write the non-functional requirements with measurable evidence | 2 | P1-01 |
| P1-03 ([#63](https://github.com/satyambhanot/ChinookAir/issues/63)) | Build the use-case table (UC-01 to UC-17) | 2 | P1-01 |
| P1-04 ([#64](https://github.com/satyambhanot/ChinookAir/issues/64)) | Draw the use-case diagram | 3 | P1-03, P0-03 |
| P1-05 ([#65](https://github.com/satyambhanot/ChinookAir/issues/65)) | Write the system introduction and process descriptions | 2 | P1-03 |

#### Phase 2: Scenarios & Domain Model

| ID | Issue | Hours | Depends on |
|---|---|---|---|
| P2-01 ([#66](https://github.com/satyambhanot/ChinookAir/issues/66)) | Write the Book Flight use-case scenario | 3 | P1-03, P0-04 |
| P2-02 ([#67](https://github.com/satyambhanot/ChinookAir/issues/67)) | Write the Manage Flights use-case scenario | 2 | P2-01 |
| P2-03 ([#68](https://github.com/satyambhanot/ChinookAir/issues/68)) | Write the Modify Reservation use-case scenario | 2 | P2-01 |
| P2-04 ([#69](https://github.com/satyambhanot/ChinookAir/issues/69)) | Extract the candidate class and operation list and check coverage | 2 | P2-02, P2-03 |
| P2-05 ([#70](https://github.com/satyambhanot/ChinookAir/issues/70)) | Draw the first-cut class diagram: entities | 3 | P2-04 |
| P2-06 ([#71](https://github.com/satyambhanot/ChinookAir/issues/71)) | Draw the first-cut class diagram: controls and boundaries | 3 | P2-05 |

#### Phase 3: Behavioral Diagrams

| ID | Issue | Hours | Depends on |
|---|---|---|---|
| P3-01 ([#72](https://github.com/satyambhanot/ChinookAir/issues/72)) | Draw the Login activity diagram | 2 | P2-01 |
| P3-02 ([#73](https://github.com/satyambhanot/ChinookAir/issues/73)) | Draw the Browse and Select Flight activity diagram | 2 | P2-01 |
| P3-03 ([#74](https://github.com/satyambhanot/ChinookAir/issues/74)) | Draw the Book Flight activity diagram | 2 | P3-02 |
| P3-04 ([#75](https://github.com/satyambhanot/ChinookAir/issues/75)) | Draw the Make Payment activity diagram | 2 | P3-03 |
| P3-05 ([#76](https://github.com/satyambhanot/ChinookAir/issues/76)) | Draw the Search Flights sequence diagram | 2 | P2-06 |
| P3-06 ([#77](https://github.com/satyambhanot/ChinookAir/issues/77)) | Draw the Book Flight sequence diagram | 2 | P2-06 |
| P3-07 ([#78](https://github.com/satyambhanot/ChinookAir/issues/78)) | Draw the Make Payment sequence diagram | 2 | P2-06 |
| P3-08 ([#79](https://github.com/satyambhanot/ChinookAir/issues/79)) | Draw the Manage Flights sequence diagram | 2 | P2-06 |
| P3-09 ([#80](https://github.com/satyambhanot/ChinookAir/issues/80)) | Draw the whole-system state diagram | 2 | P3-01 |
| P3-10 ([#81](https://github.com/satyambhanot/ChinookAir/issues/81)) | Draw the payment state diagram | 1.5 | P3-04 |
| P3-11 ([#82](https://github.com/satyambhanot/ChinookAir/issues/82)) | Draw the reservation state diagram | 1.5 | P3-03 |

#### Phase 4: Architecture & Design Report

| ID | Issue | Hours | Depends on |
|---|---|---|---|
| P4-01 ([#83](https://github.com/satyambhanot/ChinookAir/issues/83)) | Refine the class diagram with design patterns and DAO interfaces | 4 | P2-06, P3-11 |
| P4-02 ([#84](https://github.com/satyambhanot/ChinookAir/issues/84)) | Draw the package diagram with public and package-private classes | 2 | P4-01 |
| P4-03 ([#85](https://github.com/satyambhanot/ChinookAir/issues/85)) | Build the traceability matrix | 2 | P4-01 |
| P4-04 ([#86](https://github.com/satyambhanot/ChinookAir/issues/86)) | Run a consistency review across all diagrams and text | 3 | P4-02, P4-03 |
| P4-05 ([#87](https://github.com/satyambhanot/ChinookAir/issues/87)) | Assemble and export the design PDF, then tag design-v1 | 3 | P4-04 |

#### Phase 5: Domain Model

| ID | Issue | Hours | Depends on |
|---|---|---|---|
| P5-01 ([#88](https://github.com/satyambhanot/ChinookAir/issues/88)) | Implement the User hierarchy, Role, UserFactory and PasswordHasher | 3 | P4-05 |
| P5-02 ([#89](https://github.com/satyambhanot/ChinookAir/issues/89)) | Implement Airline, Airport, Route, Aircraft and Promotion entities | 3 | P5-01 |
| P5-03 ([#90](https://github.com/satyambhanot/ChinookAir/issues/90)) | Implement Flight, Seat, SeatLayout and PricingPolicy | 3 | P5-02 |
| P5-04 ([#91](https://github.com/satyambhanot/ChinookAir/issues/91)) | Implement Reservation with the State pattern | 3 | P5-03 |
| P5-05 ([#92](https://github.com/satyambhanot/ChinookAir/issues/92)) | Implement Payment, PaymentDetails and the PaymentMethod strategies | 3 | P5-03 |

#### Phase 6: Data Layer

| ID | Issue | Hours | Depends on |
|---|---|---|---|
| P6-01 ([#93](https://github.com/satyambhanot/ChinookAir/issues/93)) | Write the SQLite schema with constraints that prevent double booking | 3 | P5-05 |
| P6-02 ([#94](https://github.com/satyambhanot/ChinookAir/issues/94)) | Implement the DatabaseConnection singleton, initializer and test helper | 3 | P6-01 |
| P6-03 ([#95](https://github.com/satyambhanot/ChinookAir/issues/95)) | Implement the reference-data DAOs (Airline, Airport, Aircraft, Route) | 3 | P6-02, P5-02 |
| P6-04 ([#96](https://github.com/satyambhanot/ChinookAir/issues/96)) | Implement FlightDao and SeatDao with the flight search query | 4 | P6-03, P5-03 |
| P6-05 ([#97](https://github.com/satyambhanot/ChinookAir/issues/97)) | Implement UserDao and CustomerDao with transactional customer creation | 3 | P6-03, P5-01 |
| P6-06 ([#98](https://github.com/satyambhanot/ChinookAir/issues/98)) | Implement ReservationDao with double-booking protection | 4 | P6-04, P6-05, P5-04 |
| P6-07 ([#99](https://github.com/satyambhanot/ChinookAir/issues/99)) | Implement PaymentDao | 2 | P6-06, P5-05 |
| P6-08 ([#100](https://github.com/satyambhanot/ChinookAir/issues/100)) | Implement PromotionDao and AppStateDao | 2 | P6-02, P5-02 |
| P6-09 ([#101](https://github.com/satyambhanot/ChinookAir/issues/101)) | Add DaoFactory and the ArchUnit layer-rule tests | 3 | P6-08 |
| P6-10 ([#102](https://github.com/satyambhanot/ChinookAir/issues/102)) | Implement the idempotent DatabaseSeeder with demo data | 3 | P6-09, P5-01 |

#### Phase 7: Business Logic

| ID | Issue | Hours | Depends on |
|---|---|---|---|
| P7-01 ([#103](https://github.com/satyambhanot/ChinookAir/issues/103)) | Implement LoginController, SessionContext and the role check | 3 | P6-10 |
| P7-02 ([#104](https://github.com/satyambhanot/ChinookAir/issues/104)) | Implement PromotionController with the Observer pattern | 3 | P7-01 |
| P7-03 ([#105](https://github.com/satyambhanot/ChinookAir/issues/105)) | Implement FlightSearchController | 3 | P6-04 |
| P7-04 ([#106](https://github.com/satyambhanot/ChinookAir/issues/106)) | Implement BookingController with seat selection and pricing | 4 | P7-03, P5-04, P6-06 |
| P7-05 ([#107](https://github.com/satyambhanot/ChinookAir/issues/107)) | Implement ConfirmationWriter (booking confirmation text file) | 2 | P5-05 |
| P7-06 ([#108](https://github.com/satyambhanot/ChinookAir/issues/108)) | Implement PaymentController with simulated authorization and refunds | 3 | P7-04, P7-05 |
| P7-07 ([#109](https://github.com/satyambhanot/ChinookAir/issues/109)) | Implement ReservationController (history, cancel, modify) | 4 | P7-06 |
| P7-08 ([#110](https://github.com/satyambhanot/ChinookAir/issues/110)) | Implement CustomerController (register, view, edit, search) | 2 | P7-01, P6-05 |
| P7-09 ([#111](https://github.com/satyambhanot/ChinookAir/issues/111)) | Implement FlightManagementController (add, update, remove flights) | 4 | P7-01, P6-04 |
| P7-10 ([#112](https://github.com/satyambhanot/ChinookAir/issues/112)) | Implement RouteController, AircraftController and ScheduleController | 3 | P7-09 |

#### Phase 8: GUI

| ID | Issue | Hours | Depends on |
|---|---|---|---|
| P8-01 ([#113](https://github.com/satyambhanot/ChinookAir/issues/113)) | Build the application shell, composition root and navigation | 3 | P7-10 |
| P8-02 ([#114](https://github.com/satyambhanot/ChinookAir/issues/114)) | Add input parsing helpers and the user-facing error messages | 2 | P8-01 |
| P8-03 ([#115](https://github.com/satyambhanot/ChinookAir/issues/115)) | Build the login view, registration view and role dashboards | 4 | P8-02 |
| P8-04 ([#116](https://github.com/satyambhanot/ChinookAir/issues/116)) | Show the monthly promotion dialog on startup | 2 | P8-03, P7-02 |
| P8-05 ([#117](https://github.com/satyambhanot/ChinookAir/issues/117)) | Build the flight search view and results table | 4 | P8-03, P7-03 |
| P8-06 ([#118](https://github.com/satyambhanot/ChinookAir/issues/118)) | Build the seat selection view | 4 | P8-05, P7-04 |
| P8-07 ([#119](https://github.com/satyambhanot/ChinookAir/issues/119)) | Build the payment and confirmation views | 3 | P8-06, P7-06 |
| P8-08 ([#120](https://github.com/satyambhanot/ChinookAir/issues/120)) | Build the My Reservations view with cancel and resume payment | 3 | P8-07, P7-07 |
| P8-09 ([#121](https://github.com/satyambhanot/ChinookAir/issues/121)) | Build customer management (agent) and the customer profile view | 4 | P8-03, P7-08 |
| P8-10 ([#122](https://github.com/satyambhanot/ChinookAir/issues/122)) | Build the agent modify-reservation view | 4 | P8-09, P8-06, P7-07 |
| P8-11 ([#123](https://github.com/satyambhanot/ChinookAir/issues/123)) | Build the payment records and schedule views | 3 | P8-09, P7-10 |
| P8-12 ([#124](https://github.com/satyambhanot/ChinookAir/issues/124)) | Build the administrator flight management view | 4 | P8-11, P7-09 |
| P8-13 ([#125](https://github.com/satyambhanot/ChinookAir/issues/125)) | Build the administrator route, aircraft and reschedule views | 3 | P8-12 |

#### Phase 9: Integration & Submission

| ID | Issue | Hours | Depends on |
|---|---|---|---|
| P9-01 ([#126](https://github.com/satyambhanot/ChinookAir/issues/126)) | Write the automated end-to-end flow test | 3 | P8-13 |
| P9-02 ([#127](https://github.com/satyambhanot/ChinookAir/issues/127)) | Write and run the manual test plan for every use case | 4 | P9-01 |
| P9-03 ([#128](https://github.com/satyambhanot/ChinookAir/issues/128)) | Update the UML and report so they match the final code | 3 | P9-02 |
| P9-04 ([#129](https://github.com/satyambhanot/ChinookAir/issues/129)) | Package the fat jar, add a self-test, and run CI on macOS, Windows and Linux | 3 | P9-01 |
| P9-05 ([#130](https://github.com/satyambhanot/ChinookAir/issues/130)) | Build the submission archive, verify it from scratch, and tag v1.0 | 2 | P9-03, P9-04 |
