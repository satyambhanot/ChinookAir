# Phase 0 and Phase 1 issues

Format and conventions are explained in [../master-plan.md](../master-plan.md).

## P0-01

**Issue Title:** Record scope decisions and glossary
**Milestone/Phase:** Phase 0: Setup & Decisions
**Labels:** setup
**Estimate:** 1h
**Depends on:** none
**Kind:** doc
**Rubric check:** Assessment 6 - one vocabulary is used everywhere, so use cases trace to classes

### Context & Objective
The guideline leaves a few things open: the promotion rule, what happens to reservations when a flight is removed, cancel rules, and "booking" vs "reservation" vs "ticket". Settling them now means the diagrams, tests and code all agree. Covers section 3.7 (promotion rule) and assessment criterion 6 (traceability).

### Implementation Guide
1. Open the Decisions table at the bottom of `docs/action-plan.md` and replace every open item with the defaults below.
2. Create `docs/glossary.md` with one sentence each for: Reservation, Flight, Route, Airline, Airport, Aircraft, Seat, Payment, Promotion, Customer, Flight Agent, Administrator, Confirmation number.
3. Add a "Guideline basis" column: say which guideline section states the decision, or that the guideline is silent and the rule is ours.
4. Link the glossary from `docs/README.md`.

Defaults to record:
- The entity is **Reservation**. "Book" is only a verb (Book Flight, `BookingController`, `BookingView`).
- One passenger per reservation: the customer who owns it.
- Promotion: picked on the first app launch of a calendar month, remembered as `YYYY-MM` in the database, shown once.
- Cancel: allowed until the flight departs, full refund.
- Removing a flight that has active reservations is blocked with an error.
- Seat classes: Economy (x1.0) and Business (x1.5). Business is the first two rows.
- Simulated payment: a card whose last four digits are `0000` is declined, everything else is approved.
- Passwords are stored hashed. Only the last 4 card digits are stored.

### Testing Strategy
- `grep -rniE "\b(ticket|booking)\b" docs src` returns only "Book Flight", `BookingController` and `BookingView`. Any other hit is a bug.
- `grep -n TBD docs/action-plan.md` returns nothing.
- Every glossary term appears at least once in another file under `docs/`.

## P0-02

**Issue Title:** Check the plan against the guideline line by line
**Milestone/Phase:** Phase 0: Setup & Decisions
**Labels:** setup
**Estimate:** 1h
**Depends on:** P0-01
**Kind:** doc
**Rubric check:** Sections 6 and 7 - every requirement, deliverable and assessment criterion in the guideline maps to an issue

### Context & Objective
The guideline is the only source of truth, so nothing is sent to the instructor. This issue proves nothing in it was missed: every requirement, deliverable and assessment criterion gets a row that says where it is covered. Where the guideline is silent (due dates, page limit, report format, cancel rules) the row says which rule we use, so silence never blocks work.

### Implementation Guide
1. Create `docs/guideline-checklist.md` with a table: Section, Requirement (short), Covered by (issue IDs and the deliverable), Basis (`stated` or `silent: our rule`).
2. Add one row for each bullet in sections 2.1, 3.1 to 3.7, 4 and 5, each deliverable A to G in 7.1, the two items in 7.2, and each of the seven assessment criteria in section 6 (label those rows `Criterion 1` to `Criterion 7`).
3. End with a "Silent in the guideline" list and the rule used for each: due dates, page limit and report format, whether an ER diagram is required (it is not listed, so it is optional), which compiled file to submit (a JAR, section 7.2), whether a demo is required (none listed).
4. Check the Decisions table in `docs/action-plan.md` still agrees with the checklist, and link the checklist from `docs/README.md`.

### Testing Strategy
- `grep -c "^| Criterion" docs/guideline-checklist.md` returns 7.
- Every issue ID in the file exists: `grep -oE "P[0-9]-[0-9]{2}" docs/guideline-checklist.md | sort -u` lists only IDs that appear in `docs/master-plan.md`.
- No row has an empty "Covered by" cell and `grep -n TBD docs/guideline-checklist.md` returns nothing.
- Each of deliverables A to G (7.1) appears in at least one row.

## P0-03

**Issue Title:** Set up the diagram workflow and style rules
**Milestone/Phase:** Phase 0: Setup & Decisions
**Labels:** setup
**Estimate:** 1h
**Depends on:** none
**Kind:** doc
**Rubric check:** Assessment 5 - diagrams are clear, consistent and exportable

### Context & Objective
Every deliverable from section A to G is a diagram. A fixed folder layout and style rules keep them consistent and keep exports readable in the PDF.

### Implementation Guide
1. Create `docs/diagrams/src/` for draw.io sources and `docs/diagrams/export/` for PNG/PDF exports (replace the `.gitkeep`).
2. Add `docs/diagrams/README.md` with: file naming (`NN-short-name.drawio`, for example `20-activity-login.drawio`; the tens digit is the diagram kind), stereotype format (`<<entity>>`, lowercase), font and size, multiplicity written as `1`, `0..1`, `*`, `1..*`, and export settings (PNG, 2x scale, white background, border 10px).
3. Make one `00-template.drawio` containing a class box, a lifeline and a swimlane so the style stays the same across files.
4. Install the draw.io desktop app, or use the web version, and confirm export works.

### Testing Strategy
- Export `00-template.drawio` to PNG: width is at least 1600 px and text is legible when the image is scaled to A4 page width.
- Open the `.drawio` file in a fresh browser session: it loads without a warning.
- `git ls-files docs/diagrams` lists both folders.

## P0-04

**Issue Title:** Create the design report skeleton with double-underline support
**Milestone/Phase:** Phase 0: Setup & Decisions
**Labels:** setup
**Estimate:** 2h
**Depends on:** none
**Kind:** doc
**Rubric check:** Section 7.1 C - single underline for candidate classes, double underline for candidate operations

### Context & Objective
Section C requires single and double underlines in the scenarios, which Google Docs cannot do. This sets up a report whose structure follows sections A to G exactly, so nothing is forgotten at the end.

### Implementation Guide
1. Create `docs/report/main.tex` (LaTeX, `article`, 11pt, 2.5 cm margins) with the `ulem` package: `\uline{...}` for a single underline and `\uuline{...}` for a double underline.
2. One file per section in `docs/report/sections/`: `a-introduction.tex`, `b-use-case-diagram.tex`, `c-scenarios.tex`, `d-sequence.tex`, `e-state.tex`, `f-class.tex`, `g-package.tex`, plus `requirements.tex` and `traceability.tex`.
3. Add a `Makefile` with `make pdf` (runs `latexmk -pdf`) and `make clean`. Ignore LaTeX build files in `.gitignore`.
4. Add a title page (course, group number, project name, names) and a table of contents.
5. If LaTeX is not installed, use Word and keep the same section order. The underline test below still applies.

### Testing Strategy
- `make pdf` produces `docs/report/main.pdf` with no errors.
- A test sentence `\uline{Reservation} is created by \uuline{cancel}` renders a single underline under "Reservation" and a double underline under "cancel" (check the exported page image).
- The table of contents lists A to G in order.

## P0-05

**Issue Title:** Add a pull request template with the pre-commit checklist
**Milestone/Phase:** Phase 0: Setup & Decisions
**Labels:** setup
**Estimate:** 1h
**Depends on:** none
**Kind:** doc
**Rubric check:** Assessment 3 - maintainability and reliability are enforced by a repeatable process

### Context & Objective
Even working alone, every change should go through the same checklist before it reaches `dev`. A PR template makes the checklist impossible to forget and gives the course a visible quality process.

### Implementation Guide
1. Create `.github/pull_request_template.md` with sections: What changed, Why, Closes #, and the four checkbox items (linter, tests, OOP and standards, rubric requirement).
2. Mention in `CONTRIBUTING.md` that every PR into `dev` uses it.
3. Keep PRs one issue each.

### Testing Strategy
- GitHub reads PR templates only from the default branch (`main`), and `main` stays untouched until the final merge (P9-05). So until then, copy the template into each PR description by hand, and check the pre-fill at the end: a draft PR from a throwaway branch opens with the template filled in.
- `Closes #<n>` in the description links the issue (visible in the PR sidebar).

## P1-01

**Issue Title:** Write the functional requirements list (FR-1 to FR-14)
**Milestone/Phase:** Phase 1: Requirements & Use Cases
**Labels:** design
**Estimate:** 2h
**Depends on:** P0-01
**Kind:** doc
**Rubric check:** Section 3 and 2.1 - every guideline requirement has an ID that later artifacts can trace to

### Context & Objective
The design document has to start with a clear description of the requirements, and everything later (use cases, scenarios, tests) is traced to these IDs.

### Implementation Guide
1. Create `docs/report/sections/requirements.tex` with a table: ID, requirement, guideline section, source (guideline or ours).
2. Copy each bullet from guideline sections 2, 2.1 and 3 into its own row:
   - FR-1 search by origin, destination, date and airline; FR-2 show seats, times and prices; FR-3 create reservation; FR-4 cancel or modify; FR-5 booking confirmation (screen and `.txt` file); FR-6 customer profiles (create, view, edit); FR-7 add, update and remove flights (admin only); FR-8 manage routes, aircraft and schedules; FR-9 record simulated payments and payment records; FR-10 reservation history and flight schedules; FR-11 local SQLite storage; FR-12 monthly promotion.
3. Add ours, marked "ours": FR-13 login with three roles, FR-14 seat selection.
4. Use `@DisplayName("FR-x ...")` on tests later, so IDs must never be renumbered.

### Testing Strategy
- Walk the guideline sections 2, 2.1 and 3 bullet by bullet: each one maps to at least one FR (keep the mapping in the issue comments).
- IDs are unique and consecutive: `grep -o "FR-[0-9]*" requirements.tex | sort -u | wc -l` is 14.
- No row without a source.

## P1-02

**Issue Title:** Write the non-functional requirements with measurable evidence
**Milestone/Phase:** Phase 1: Requirements & Use Cases
**Labels:** design
**Estimate:** 2h
**Depends on:** P1-01
**Kind:** doc
**Rubric check:** Assessment 3 - usability, reusability, maintainability and reliability are all treated

### Context & Objective
Section 4 lists usability, portability, maintainability and scalability, and assessment criterion 3 adds reusability and reliability. Each one needs a measurable statement and a pointer to where the design or tests prove it.

### Implementation Guide
1. Add a table to `requirements.tex`: NFR, measurable criterion, how the design meets it, evidence (issue or test).
2. Fill in:
   - Usability: every task reachable in at most 4 clicks from the dashboard; clear error messages.
   - Portability: `java -jar` runs on macOS and Windows (CI matrix, P9-04).
   - Maintainability: three layers, 0 Checkstyle violations, ArchUnit layer tests (P6-09).
   - Scalability: DAO interfaces allow replacing SQLite; controllers have no Swing code.
   - Reusability: controllers and entities usable without the GUI (tested without Swing).
   - Reliability: double booking impossible (database constraint plus test, P6-01 and P7-04).
   - Security: hashed passwords, masked card numbers.
3. Keep each criterion testable, not "good" or "fast".

### Testing Strategy
- Each of the six named qualities (usability, portability, maintainability, scalability, reusability, reliability) appears in the table.
- Every row has a concrete evidence pointer that exists as an issue ID.
- No criterion contains vague words (`grep -niE "good|fast|easy" requirements.tex` returns nothing).

## P1-03

**Issue Title:** Build the use-case table (UC-01 to UC-17)
**Milestone/Phase:** Phase 1: Requirements & Use Cases
**Labels:** design
**Estimate:** 2h
**Depends on:** P1-01
**Kind:** doc
**Rubric check:** Assessment 6 - use-case coverage and a control class for every use case

### Context & Objective
Section D requires each sequence-diagram use case to have a control class in the class diagram. Deciding the control class for every use case now keeps the use-case diagram, scenarios, class diagram and code aligned.

### Implementation Guide
1. In `docs/report/sections/b-use-case-diagram.tex` add a table: ID, use case, actor, FR, control class, goal.
2. Rows: UC-01 Login (all) `LoginController`; UC-02 Register Customer `CustomerController`; UC-03 Manage Customer Profile `CustomerController`; UC-04 Find Available Flights `FlightSearchController`; UC-05 View Flight Details `FlightSearchController`; UC-06 Book Flight `BookingController`; UC-07 Make Payment `PaymentController`; UC-08 View Payment Records `PaymentController`; UC-09 Cancel Reservation `ReservationController`; UC-10 Modify Reservation `ReservationController`; UC-11 View Reservation History `ReservationController`; UC-12 View Flight Schedules `ScheduleController`; UC-13 Update Schedules `ScheduleController`; UC-14 Manage Flights `FlightManagementController`; UC-15 Manage Routes `RouteController`; UC-16 Manage Aircraft `AircraftController`; UC-17 View Monthly Promotion `PromotionController`.
3. Actors: Customer, Flight Agent, Administrator, plus an abstract User used for UC-01.
4. Write each goal as a result of value to the actor. UC-04 reads: see the available flights with their seats, times and prices. The control class keeps the name `FlightSearchController`; only the use case is renamed.

### Testing Strategy
- Every FR from P1-01 appears in at least one row.
- Every use case has exactly one control class, and there are 11 distinct control classes in total.
- No two use cases share an ID or name.

## P1-04

**Issue Title:** Draw the use-case diagram
**Milestone/Phase:** Phase 1: Requirements & Use Cases
**Labels:** design
**Estimate:** 3h
**Depends on:** P1-03, P0-03
**Kind:** doc
**Rubric check:** Section 7.1 B - use-case diagram covers all requirements

### Context & Objective
Deliverable B. The diagram must cover every requirement and will be the first thing a marker checks classes against.

### Implementation Guide
1. In `docs/diagrams/src/01-use-case.drawio` draw a system boundary "ChinookAir" with the actors outside it.
2. Add generalization: Customer, Flight Agent and Administrator all inherit from User, and User is linked to Login.
3. Draw all 17 use cases. Add relationships: Book Flight `<<include>>` Select Seat and `<<include>>` Make Payment; View Flight Details `<<extend>>` Find Available Flights; Modify Reservation `<<include>>` Find Available Flights.
4. Connect each actor only to the use cases it owns (Customer: search, book, pay, cancel, history, promotion; Agent: customer management, modify, payment records, schedules, plus booking and cancelling for a customer; Administrator: flights, routes, aircraft, schedule updates).
5. Follow the notation from the course slides (Module 12): stick-figure actors, labelled ovals, a rectangle for the system boundary, a plain line from an actor to each use case. `<<include>>` points from the base use case to the shared one, `<<extend>>` from the extension to the base use case; both are dashed arrows with an open arrowhead. Details are in `docs/diagrams/README.md`.
6. Export to `docs/diagrams/export/01-use-case.png` and include it in section B.

### Testing Strategy
- The diagram shows 17 use cases (plus Select Seat as an included one), matching the table in P1-03.
- Every use case has at least one actor or is reached through include/extend.
- Every FR appears in a use case that is on the diagram.
- Every use case name is short and names a result of value to an actor (Module 12: "Search" alone is a weak use case, so UC-04 is named Find Available Flights and its goal says it returns the available flights with seats, times and prices).
- Exported image is readable at A4 page width.

## P1-05

**Issue Title:** Write the system introduction and process descriptions
**Milestone/Phase:** Phase 1: Requirements & Use Cases
**Labels:** design
**Estimate:** 2h
**Depends on:** P1-03
**Kind:** doc
**Rubric check:** Section 7.1 A - introduction plus descriptions of searching, booking and paying

### Context & Objective
Deliverable A asks for a short system introduction and descriptions of several major or relatively complex processes (searching and selecting a flight, booking, paying). The four activity diagrams are added in Phase 3.

### Implementation Guide
1. In `docs/report/sections/a-introduction.tex` write: purpose of ChinookAir, the three roles, scope and out of scope (no web, no real payments), and the three-layer architecture in two sentences.
2. Add three process descriptions of at most 200 words each: Search and select a flight, Book a flight, Make a payment. Each states the actor, trigger, key steps and outcome, and refers to its UC ID.
3. Use only glossary terms (Reservation, not ticket).

### Testing Strategy
- The three processes named in the guideline are all present.
- Each description names an actor, a trigger and an outcome (check with a checklist).
- Word count per process is at most 200 (`wc -w` on the extracted paragraph).
- `grep -niE "\b(ticket)\b"` on the section returns nothing.
