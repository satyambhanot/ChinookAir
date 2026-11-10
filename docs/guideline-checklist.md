# Guideline checklist

Every requirement, deliverable and assessment criterion in the project
guideline, with the issues that cover it. "stated" means the guideline says
it; "silent: our rule" means it leaves the point open and we picked a rule
(see "Decisions" in [action-plan.md](action-plan.md)). Issue IDs are listed
in [master-plan.md](master-plan.md).

## What the system does (section 2)

| Section | Requirement | Covered by | Basis |
|---|---|---|---|
| 2 | Standalone, single-user, role-based GUI; not web or client-server | P8-01, P8-03, P9-04 | stated |
| 2 | Object-oriented, domain-driven design | P2-04, P2-05, P5-01 to P5-05 | stated |
| 2 | View available flights and flight details | P7-03, P8-05 | stated |
| 2 | Create, cancel or modify reservations | P7-04, P7-07, P8-06, P8-08, P8-10 | stated |
| 2 | View booking history and flight schedules | P7-07, P8-08, P8-11 | stated |
| 2 | Manage flight information (add, update, delete) | P7-09, P8-12 | stated |
| 2 | Maintain customer information and payment records | P7-06, P7-08, P8-09, P8-11 | stated |
| 2.1 | Customer: search flights, create and cancel reservations, view reservations | P7-03, P7-04, P7-07, P8-05, P8-08 | stated |
| 2.1 | Flight agent: manage customer data, modify reservations, view schedules | P7-07, P7-08, P8-09, P8-10, P8-11 | stated |
| 2.1 | Administrator: add and remove flights, manage routes and aircraft, update schedules | P7-09, P7-10, P8-12, P8-13 | stated |
| 2.1 | Login with a role (needed for the Login activity diagram) | P7-01, P8-03, P3-01 | stated |

## Functional requirements (section 3)

| Section | Requirement | Covered by | Basis |
|---|---|---|---|
| 3 | Extra requirements are allowed (seat selection, login, registration) | P1-01 | stated |
| 3.1 | Search flights by origin, destination, date and airline | P5-03, P6-04, P7-03, P8-05 | stated |
| 3.1 | Display available seats, flight times and prices | P5-03, P6-04, P8-05, P8-06 | stated |
| 3.2 | Create new bookings (we call them reservations) | P5-04, P7-04, P8-06 | stated |
| 3.2 | Cancel or modify existing reservations | P7-07, P8-08, P8-10 | stated |
| 3.2 | Generate booking confirmations | P7-05, P8-07 | stated |
| 3.3 | Create customer profiles | P7-08, P8-03, P8-09 | stated |
| 3.3 | View and edit customer details | P7-08, P8-09 | stated |
| 3.4 | Add flights and update existing flight details (administrator only) | P7-01, P7-09, P8-12 | stated |
| 3.4 | Remove outdated flights | P7-09, P8-12 | stated |
| 3.5 | Record payment information; processing is simulated | P5-05, P6-07, P7-06, P8-07 | stated |
| 3.6 | Local or embedded database (SQLite) holds flight and reservation data | P6-01 to P6-10 | stated |
| 3.7 | Promotion picked at random when the app is first opened in a month | P5-02, P6-08, P7-02 | stated |
| 3.7 | Promotion shown only once that month | P6-08, P7-02, P8-04 | stated |

## Non-functional requirements and architecture (sections 4 and 5)

| Section | Requirement | Covered by | Basis |
|---|---|---|---|
| 4 | Usability: simple GUI in Java or C++ | P8-02 to P8-13 | stated |
| 4 | Portability: standalone desktop application | P6-02, P9-04 | stated |
| 4 | Maintainability: modular, layered design | P0-05, P6-09 | stated |
| 4 | Scalability: room for online booking or client-server later | P4-01, P6-03 | stated |
| 4 | Our own: reliability, reusability, security | P1-02, P5-01, P5-05, P6-01 | stated |
| 5 | Presentation layer: GUI and input handling | P8-01 to P8-13 | stated |
| 5 | Business logic layer: domain classes and operations | P5-01 to P5-05, P7-01 to P7-10 | stated |
| 5 | Data layer: database and file access | P6-01 to P6-10, P7-05 | stated |

## Design phase deliverables (section 7.1)

| Section | Requirement | Covered by | Basis |
|---|---|---|---|
| 7.1 A | System introduction and descriptions of the major processes | P1-05 | stated |
| 7.1 A | Activity diagram 1: login | P3-01 | stated |
| 7.1 A | Activity diagram 2: browsing for and selecting a flight | P3-02 | stated |
| 7.1 A | Activity diagram 3: booking a flight | P3-03 | stated |
| 7.1 A | Activity diagram 4: making a payment | P3-04 | stated |
| 7.1 B | Use-case diagram | P1-03, P1-04 | stated |
| 7.1 C | Detailed scenarios for three use cases | P2-01, P2-02, P2-03 | stated |
| 7.1 C | Candidate classes single-underlined, operations double-underlined | P0-04, P2-04 | stated |
| 7.1 D | Four sequence diagrams, each with a control class | P3-05, P3-06, P3-07, P3-08 | stated |
| 7.1 E | State diagram: the overall system | P3-09 | stated |
| 7.1 E | State diagram: the payment process | P3-10 | stated |
| 7.1 E | State diagram: a reservation | P3-11 | stated |
| 7.1 F | Class diagram with classes, relationships, attributes, behaviours, cardinalities | P2-05, P2-06 | stated |
| 7.1 F | Stereotypes entity, control and boundary | P2-05, P2-06 | stated |
| 7.1 F | Every class traceable to a scenario | P2-04, P4-03 | stated |
| 7.1 F | Refined for reuse and scale: inheritance, realization, aggregation, composition, patterns | P4-01 | stated |
| 7.1 G | Package diagram of the three layers, with public classes marked | P4-02 | stated |
| 7.1 | The whole design as one PDF | P4-05 | stated |

## Implementation phase (section 7.2)

| Section | Requirement | Covered by | Basis |
|---|---|---|---|
| 7.2 | Java GUI application with SQLite storage | P5-01 to P8-13 | stated |
| 7.2 | A compiled file (JAR or executable) | P9-04, P9-05 | stated |
| 7.2 | A ZIP of all source files | P9-05 | stated |

## Assessment criteria (section 6)

| Section | Requirement | Covered by | Basis |
|---|---|---|---|
| Criterion 1 | Separation of concerns across components (entities, controllers, views) | P2-06, P4-02, P6-09 | stated |
| Criterion 2 | Modularity, cohesion and low coupling | P4-01, P6-09, P8-01 | stated |
| Criterion 3 | Usability, reusability, maintainability and reliability | P1-02, P9-04 | stated |
| Criterion 4 | Boundary, entity and control separation across levels | P2-06, P6-09 | stated |
| Criterion 5 | UML is clear and matches the final application | P0-03, P4-04, P9-03 | stated |
| Criterion 6 | Use-case coverage and traceability to the class diagram | P1-03, P1-04, P2-04, P4-03, P9-02 | stated |
| Criterion 7 | Quality of scenarios and sequence diagrams | P2-01, P2-02, P2-03, P3-05 to P3-08 | stated |

## Silent in the guideline

These are not in the guideline, so none of them is required. The rule is what
we do instead.

| Topic | Rule we use |
|---|---|
| Due dates | None given. Work to the calendar in the master plan, design phase first. |
| Page limit and report format | Only "PDF" is stated (7.1). Keep it short and clear; build it from `docs/report` with `make pdf`. |
| ER diagram | Not listed, so optional. The class diagram carries the data model. |
| Which compiled file | A JAR (7.2 allows "JAR or executable"). |
| Demo | Not listed, so not required. P9-02 is our own test run. |
| Updated design PDF with the implementation | Not listed. Keep it in `docs/`; the submission holds only what 7.2 names. |
| Group work | The guideline talks about groups but does not require one. We are solo. |
| Languages | Java or C++ for the GUI (section 4). We use Java. |
| Passengers, cancel limits, seat classes, card-decline rule, stored card digits, password hashing | See "Decisions" in [action-plan.md](action-plan.md). |
