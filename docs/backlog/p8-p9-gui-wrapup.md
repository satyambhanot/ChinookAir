# Phase 8 and Phase 9 issues (GUI, integration and submission)

Paths are relative to `src/main/java/ca/ucalgary/chinookair/` unless they start with `src/`.

GUI rules for every Phase 8 issue:
- Views are thin: they read input, call one controller, show the result or an error message. No SQL, no business rules, no DAO calls (enforced by `ArchitectureTest`).
- Views are package-private in `presentation` except `MainFrame`.
- Table data comes from a plain `AbstractTableModel` subclass that can be unit tested without opening a window.
- Errors are shown with `ErrorMessages.forException`, never a stack trace.
- Enter submits the default button, Tab order follows reading order, fields have mnemonics.

## P8-01

**Issue Title:** Build the application shell, composition root and navigation
**Milestone/Phase:** Phase 8: GUI
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P7-10
**Kind:** code
**Rubric check:** Section 4 (usability, portability) and assessment 1 - a clean composition root keeps layers separate

### Context & Objective
Replaces the placeholder window with the real frame, and wires DAOs and controllers in exactly one place so no other class creates them.

### Implementation Guide
1. `business/control/Controllers.java`: record holding every controller plus the `SessionContext`.
2. `AppContext.java` in the root package: `create()` opens `DatabaseConnection.getInstance()`, runs `DatabaseSeeder.seedIfEmpty(clock)`, builds `DaoFactory`, `ConfirmationWriter` and all controllers, and returns `Controllers`. `create(String jdbcUrl, Clock clock)` for tests. If the system property `chinookair.today` (`YYYY-MM-DD`) is set the clock is fixed at that date (for manual testing only).
3. `presentation/ViewId.java` enum and `presentation/Navigator.java` interface (`show(ViewId)`); `MainFrame` implements it with a `CardLayout`.
4. `ChinookAir.main`: build `Controllers`, then `SwingUtilities.invokeLater(() -> new MainFrame(controllers).setVisible(true))`. On any startup error show a dialog and exit with code 1.
5. Frame: title `ChinookAir`, 1000x700, centered, minimum 800x600, closing the window closes the database.
6. Use the system look and feel; fall back to Nimbus.

### Testing Strategy
- `AppContextTest` (no Swing): `AppContext.create("jdbc:sqlite::memory:", fixedClock)` returns non-null controllers and `loginController.authenticate("admin", ...)` works (seeded).
- `chinookair.today=2027-02-01` produces a clock whose date is `2027-02-01`.
- `MainFrameSmokeTest` (skipped when headless): construct `MainFrame`, `show(ViewId.LOGIN)`, dispose, no exception.
- Manual: `java -jar target/ChinookAir.jar` opens a centered 1000x700 window titled `ChinookAir`; closing it ends the process.
- `ArchitectureTest` still passes.

## P8-02

**Issue Title:** Add input parsing helpers and the user-facing error messages
**Milestone/Phase:** Phase 8: GUI
**Labels:** implementation
**Estimate:** 2h
**Depends on:** P8-01
**Kind:** code
**Rubric check:** Section 4 (usability, reliability) - clear messages instead of crashes

### Context & Objective
Every form needs the same few things: parse a date or a number, and turn exceptions into one plain sentence. Doing it once avoids copied code and inconsistent wording.

### Implementation Guide
1. `presentation/InputParser.java`: `LocalDate date(String)` expects `yyyy-MM-dd`; `LocalDateTime dateTime(String)` expects `yyyy-MM-dd HH:mm`; `BigDecimal money(String)`; `int positiveInt(String, String label)`. On bad input throw `IllegalArgumentException` with a plain message.
2. `presentation/ErrorMessages.java`: `String forException(Throwable)`: `IllegalArgumentException` and `IllegalStateException` show their own message; `SeatUnavailableException` gives `That seat was just taken. Please pick another.`; `AccessDeniedException` gives `You do not have permission to do that.`; `DataAccessException` gives `The database had a problem. Your data was not changed.`; anything else gives `Something unexpected happened. Please try again.` and is logged with `java.util.logging`.
3. `presentation/Dialogs.java`: `error(Component, Throwable)` and `info(Component, String)`.
4. Business rules stay in the controllers; these helpers only parse text and phrase messages (DRY, single responsibility).

### Testing Strategy
- `date("2026-12-01")` equals `LocalDate.of(2026,12,1)`; `date("2026-13-40")` and `date("12/01/2026")` throw containing `yyyy-MM-dd`.
- `dateTime("2026-12-01 08:30")` parses; `"2026-12-01"` throws containing `yyyy-MM-dd HH:mm`.
- `money("199.99")` is `199.99`; `"abc"` throws containing `number like 199.99`; `"-5"` throws containing `greater than 0`.
- `positiveInt("0","Rows")` throws containing `Rows`.
- `ErrorMessages.forException(new SeatUnavailableException(...))` equals the seat sentence; `new RuntimeException("boom")` gives the generic sentence and does not contain `boom`.

## P8-03

**Issue Title:** Build the login view, registration view and role dashboards
**Milestone/Phase:** Phase 8: GUI
**Labels:** implementation
**Estimate:** 4h
**Depends on:** P8-02
**Kind:** code
**Rubric check:** FR-13, FR-6 and the Login activity diagram

### Context & Objective
The first screen a marker sees. It has to route each role to the right place and show the three roles' capabilities clearly.

### Implementation Guide
1. `LoginView`: username field, password field, Log in button (default button), Register link. Calls `LoginController.authenticate`, then `SessionContext.login`, then navigates by role.
2. `RegisterView`: username, password, name, email, phone; calls `CustomerController.register`; success returns to login with a message.
3. `CustomerDashboard`: Search flights, My reservations, My profile, Log out. `AgentDashboard`: Customers, Reservations, Payment records, Schedule, Log out. `AdminDashboard`: Flights, Routes, Aircraft, Schedule, Log out.
4. Log out clears the session and returns to login. The password field is cleared on every failed attempt.
5. Show the logged-in username and role in a header on every dashboard.

### Testing Strategy
Manual, with the demo accounts from the README.
- Empty username, click Log in: inline message `Username is required`, no dialog, no exception.
- `admin` with a wrong password: `Invalid username or password`, password field cleared.
- `customer1` with the right password: Customer dashboard with 4 buttons; header shows `customer1 (Customer)`.
- `agent1`: Agent dashboard; `admin`: Admin dashboard.
- Press Enter in the password field: logs in.
- Register `newuser`, password `pass1234`, name `New User`, email `new@x.ca`: success, then login works.
- Register again with the same email: `Email already registered`.
- Log out returns to login and Back does not show the previous dashboard.
- `ArchitectureTest` passes.

## P8-04

**Issue Title:** Show the monthly promotion dialog on startup
**Milestone/Phase:** Phase 8: GUI
**Labels:** implementation
**Estimate:** 2h
**Depends on:** P8-03, P7-02
**Kind:** code
**Rubric check:** FR-12 / section 3.7 - promotion shown on the first launch of the month, only once

### Context & Objective
Connects the Observer-based `PromotionController` to a dialog so the rule is visible in the real app.

### Implementation Guide
1. `PromotionDialog`: modal dialog with title, description and an OK button.
2. In `MainFrame`, register a `PromotionListener` that opens the dialog with `SwingUtilities.invokeLater`, then call `promotionController.checkOnStartup()` once when the frame is shown.
3. The dialog appears before the login screen is usable.

### Testing Strategy
Manual, using the test clock property.
- Run with a fresh database and `-Dchinookair.today=2027-02-01`: a promotion dialog appears, then login.
- Close the app and run again with the same date: no dialog.
- Run with `-Dchinookair.today=2027-02-20`: no dialog (same month).
- Run with `-Dchinookair.today=2027-03-01`: a dialog appears again.
- Unit: a fake `PromotionListener` registered twice is notified once each (already covered by P7-02).

## P8-05

**Issue Title:** Build the flight search view and results table
**Milestone/Phase:** Phase 8: GUI
**Labels:** implementation
**Estimate:** 4h
**Depends on:** P8-03, P7-03
**Kind:** code
**Rubric check:** FR-1 / FR-2 - search by origin, destination, date and airline; display seats, times and prices

### Context & Objective
The most used screen for customers and agents.

### Implementation Guide
1. `FlightSearchView`: origin and destination `JComboBox` of airports, date field (default today, `yyyy-MM-dd`), airline combo (`Any` plus airlines), Search button, Select button, Back.
2. `FlightTableModel` (AbstractTableModel) with columns Flight, Airline, Route, Departs, Arrives, Seats left, From. Dates formatted `yyyy-MM-dd HH:mm`, price `$199.99`.
3. Search button: `InputParser.date`, `FlightSearchController.search`, fill the table. Empty result shows `No flights found. Try another date or airline.`
4. Select moves to `SeatSelectionView` (P8-06) with the chosen `FlightOffer`.
5. Load airports and airlines through the controller, not a DAO.

### Testing Strategy
- `FlightTableModelTest`: for one offer (`AC123`, `YYC -> YVR`, `2026-12-01T12:00`, 150 seats, from 200.00) `getValueAt` returns `AC123`, `Air Canada`, `YYC → YVR`, `2026-12-01 12:00`, `150`, `$200.00`; `getRowCount` is the number of offers.
- Manual: YYC to YYC shows `Origin and destination must be different` and leaves the table unchanged.
- Date `2026-13-40` shows `Date must be in the form yyyy-MM-dd`.
- YYC to YVR on a seeded date shows at least one row; airline `AC` shows only AC rows; a date with no flights shows the empty message.
- Select with no row selected is disabled.

## P8-06

**Issue Title:** Build the seat selection view
**Milestone/Phase:** Phase 8: GUI
**Labels:** implementation
**Estimate:** 4h
**Depends on:** P8-05, P7-04
**Kind:** code
**Rubric check:** FR-14 / FR-3 - choose a seat and create the reservation

### Context & Objective
Shows the seat map, creates the reservation and handles the case where someone takes the seat first.

### Implementation Guide
1. `SeatSelectionView`: a grid of toggle buttons built from `BookingController.availableSeats`. Business rows are highlighted, unavailable seats are disabled and grey, a legend explains the colors.
2. `SeatGridModel`: pure class mapping a `List<Seat>` to rows and columns (row number, letters) so the layout logic is testable.
3. On opening: `startReservation(currentUser, customer, flightId)`. Selecting a seat shows `Seat 1A (Business) - $300.00`.
4. Continue calls `selectSeat` and moves to `PaymentView`. On `SeatUnavailableException` show the message, refresh the grid and stay on the screen.
5. Back calls `cancelPending` so no orphan reservation is left.

### Testing Strategy
- `SeatGridModelTest` for a 3x4 aircraft: 3 rows, 4 columns, cell (row 2, column 1) is `2B`; seat `2B` unavailable is reported as disabled.
- Manual: open a flight, pick `1A`: label shows the business price; pick `3C`: economy price.
- Race: with the seat screen open, run `sqlite3 ~/.chinookair/chinookair.db` and insert a `CONFIRMED` reservation for seat `1A` of that flight, then click Continue on `1A`: message `That seat was just taken. Please pick another.`, and `1A` is now grey.
- Back out: the reservation shows `CANCELLED` and no seat stays blocked.

## P8-07

**Issue Title:** Build the payment and confirmation views
**Milestone/Phase:** Phase 8: GUI
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P8-06, P7-06
**Kind:** code
**Rubric check:** FR-9 / section 3.5 and FR-5 (booking confirmation) - simulated payment, confirmation shown and saved

### Context & Objective
Completes the booking flow: pay, then see and keep the confirmation.

### Implementation Guide
1. `PaymentView`: method combo (Credit, Debit), name on card, card number, expiry `MM/yy`, amount (read only), Pay and Back buttons.
2. Pay calls `PaymentController.pay`. Approved: go to `ConfirmationView`. Declined: show the message and stay for a retry.
3. `ConfirmationView`: confirmation number, passenger, flight, route, departure, seat, amount, and `Saved to: <path>`; a button back to the dashboard.
4. The card number field is a `JPasswordField`-style masked input or cleared after each attempt; never log card numbers.

### Testing Strategy
Manual, amount 300.00.
- `Jo Smith`, `4111 1111 1111 1111`, `12/30`: approved, confirmation screen shows a number like `CA7K2P9X` and a file path; the file exists and contains the same number.
- `4111 1111 1111 0000`: stays on the payment screen with `Card declined`; then a good card succeeds.
- Card `1234`: message contains `16 digits`; expiry `01/20`: message contains `expired`; empty name: message contains `name`.
- Debit with an amount over 5000.00 is declined.
- After success, Back does not allow paying the same reservation again.

## P8-08

**Issue Title:** Build the My Reservations view with cancel and resume payment
**Milestone/Phase:** Phase 8: GUI
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P8-07, P7-07
**Kind:** code
**Rubric check:** FR-4 and FR-10 - cancel reservations and view booking history

### Context & Objective
Lets customers see everything they booked and cancel it.

### Implementation Guide
1. `ReservationTableModel` with columns Confirmation, Flight, Route, Departs, Seat, Status, Price.
2. `MyReservationsView`: loads `ReservationController.history`. Buttons: Cancel (enabled for `PENDING_PAYMENT` and `CONFIRMED` on a future flight), Pay now (enabled for `PENDING_PAYMENT`, opens `PaymentView`).
3. Cancel asks for confirmation: `Cancel reservation CA7K2P9X? You will be refunded $300.00.` After confirming, refresh the table.
4. The same view takes a `Customer` so agents can reuse it for a chosen customer in P8-10.

### Testing Strategy
- `ReservationTableModelTest`: one confirmed reservation gives `CA7K2P9X`, `AC123`, `YYC → YVR`, `2026-12-01 12:00`, `1A`, `CONFIRMED`, `$300.00`.
- Manual: a new customer sees an empty table with a short message.
- After booking, the table shows one `CONFIRMED` row; select it, Cancel, confirm: row becomes `CANCELLED`, seat is free again on the seat screen, payment is `REFUNDED`.
- Cancel is disabled for a `CANCELLED` row and for a past flight.
- Choosing No in the confirmation leaves the reservation unchanged.

## P8-09

**Issue Title:** Build customer management (agent) and the customer profile view
**Milestone/Phase:** Phase 8: GUI
**Labels:** implementation
**Estimate:** 4h
**Depends on:** P8-03, P7-08
**Kind:** code
**Rubric check:** FR-6 / section 3.3 and section 2.1 - agents manage customer data, customers edit their own

### Context & Objective
Covers the Manage Customer Profile use case for both roles.

### Implementation Guide
1. `CustomerTableModel` (Name, Email, Phone, Username).
2. `CustomerManagementView` (agent): search box and table; form with name, email, phone; New, Save, Reservations buttons. New opens an empty form that calls `register` (agent enters a starting password); Save calls `update`; Reservations opens `MyReservationsView` for the selected customer.
3. `ProfileView` (customer): same form, pre-filled with the current user and allowed to edit only their own data.
4. Messages come from the controllers (`Email already registered`, and so on).

### Testing Strategy
- `CustomerTableModelTest`: a customer `Jo Smith`, `jo@x.ca`, `403-555-0100`, `jsmith` gives those values in column order.
- Manual as agent: search `smi` finds Jo Smith; search `zzz` shows an empty table.
- Edit Jo's phone to `403-555-0199`, Save, search again: new phone shown.
- Change the email to an existing one: `Email already registered`, nothing saved.
- Enter email `a@b`: message about a valid email.
- As a customer, My profile shows only their own data and saving works; there is no search box.

## P8-10

**Issue Title:** Build the agent modify-reservation view
**Milestone/Phase:** Phase 8: GUI
**Labels:** implementation
**Estimate:** 4h
**Depends on:** P8-09, P8-06, P7-07
**Kind:** code
**Rubric check:** FR-4 / Modify Reservation scenario - the agent changes a reservation to another flight or seat

### Context & Objective
Implements the third scenario end to end in the UI.

### Implementation Guide
1. `ModifyReservationView`: reservation table for the selected customer (reuse `ReservationTableModel`), a Modify button.
2. Modify opens `FlightSearchView` in pick mode (Select returns the offer through a callback instead of starting a booking), then `SeatSelectionView` in pick mode (returns the seat without creating a reservation).
3. Confirm calls `ReservationController.modify`. If the result is `PENDING_PAYMENT`, tell the agent the old payment was refunded and open `PaymentView` for the new amount.
4. Show clear messages for each exception.

### Testing Strategy
Manual with a confirmed reservation of 200.00 on seat `3C`.
- Modify to another flight at 200.00, seat `4A`: reservation stays `CONFIRMED`, new flight and seat shown, old payment unchanged.
- Modify to a 250.00 flight: message about the refund, `PaymentView` opens for 250.00; paying makes it `CONFIRMED`; the old payment shows `REFUNDED` in the payment records.
- Pick a taken seat: `That seat was just taken. Please pick another.`
- A `CANCELLED` reservation cannot be selected for Modify (button disabled).
- Log in as a customer: no way to reach this screen.

## P8-11

**Issue Title:** Build the payment records and schedule views
**Milestone/Phase:** Phase 8: GUI
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P8-09, P7-10
**Kind:** code
**Rubric check:** Section 2.1 / FR-9 / FR-10 - agent views payment records and flight schedules

### Context & Objective
Read-only views that complete the agent's capabilities.

### Implementation Guide
1. `PaymentTableModel` (Date, Reservation, Amount, Method, Status, Card).
2. `PaymentRecordsView`: choose a customer (search, then select), show `PaymentController.recordsForCustomer`.
3. `ScheduleView`: from and to date fields, table of flights from `ScheduleController.schedule` (Flight, Route, Departs, Arrives, Aircraft). Make it reusable so the admin version in P8-13 adds a Reschedule button.

### Testing Strategy
- `PaymentTableModelTest`: payment `300.00`, `CREDIT`, `APPROVED`, masked `**** **** **** 1111` gives those strings and `$300.00`.
- Manual: customer with one approved and one declined payment shows two rows in order.
- Schedule `2026-12-01` to `2026-12-03` shows flights ordered by departure; a range with no flights shows an empty table and message.
- Invalid date shows `Date must be in the form yyyy-MM-dd`.

## P8-12

**Issue Title:** Build the administrator flight management view
**Milestone/Phase:** Phase 8: GUI
**Labels:** implementation
**Estimate:** 4h
**Depends on:** P8-11, P7-09
**Kind:** code
**Rubric check:** FR-7 / section 3.4 / Manage Flights scenario - add, update and remove flights (administrator only)

### Context & Objective
Implements the second scenario end to end in the UI.

### Implementation Guide
1. `FlightManagementView`: table of flights for a date range; form with flight number, route combo, airline combo, aircraft combo, departure and arrival (`yyyy-MM-dd HH:mm`) and base price; Add, Update and Remove buttons.
2. Selecting a row fills the form. Remove asks for confirmation.
3. Calls `FlightManagementController`; shows the controller messages as they are.

### Testing Strategy
Manual as `admin`.
- Add `AC9001` on a seeded route, a free aircraft, a future departure and arrival, price `199.99`: success and it shows in the table; a customer search for that date finds it.
- Add `AC9001` again: `Flight number already exists`.
- Arrival before departure: message about arrival; departure in the past: `must be in the future`.
- Same aircraft overlapping another flight: `Aircraft is already assigned to a flight at that time`.
- Remove a flight that has a confirmed reservation: `Flight has active reservations`; remove `AC9001` (no reservations): disappears.
- Log in as customer or agent: the screen is not reachable.

## P8-13

**Issue Title:** Build the administrator route, aircraft and reschedule views
**Milestone/Phase:** Phase 8: GUI
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P8-12
**Kind:** code
**Rubric check:** Section 2.1 / FR-8 - administrator manages routes, aircraft and schedules

### Context & Objective
Finishes the administrator role.

### Implementation Guide
1. `RouteManagementView`: two airport combos, Add route, table of routes.
2. `AircraftManagementView`: model, rows, seats per row, Add aircraft, table showing the computed seat count.
3. Admin `ScheduleView` adds a Reschedule button (new departure and arrival fields) calling `ScheduleController.reschedule`.

### Testing Strategy
Manual as `admin`.
- Add route `YEG` to `YOW`: appears in the table; again: `Route already exists`.
- Add aircraft `E190`, rows `25`, seats per row `4`: table shows seat count `100`; rows `0` shows a message containing `Rows`.
- Reschedule a flight to a free slot: schedule table updates; to a clashing slot: `Aircraft is already assigned to a flight at that time`.
- Rescheduling a flight in the past is rejected.

## P9-01

**Issue Title:** Write the automated end-to-end flow test
**Milestone/Phase:** Phase 9: Integration & Submission
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P8-13
**Kind:** code
**Rubric check:** Assessment 6 - every main use case works together, not just on its own

### Context & Objective
One test that walks the real controllers against a seeded database. It catches wiring mistakes that unit tests cannot.

### Implementation Guide
1. `src/test/java/ca/ucalgary/chinookair/EndToEndFlowTest.java` using `AppContext.create("jdbc:sqlite::memory:", fixedClock)` and `@TempDir` for confirmations.
2. Steps: register a customer; log in; search a seeded route and date; start a reservation; select seat; pay with a declined card then a good card; check the confirmation file; check history shows `CONFIRMED`; cancel; check the payment is `REFUNDED` and the seat is free; as admin add a flight and check the customer can find it; as agent search the customer and modify a second reservation to a pricier flight; check `PENDING_PAYMENT`.
3. Use the same assertions as the controller tests so failures point to a clear step.

### Testing Strategy
- The scenario above runs green from a clean in-memory database in under 5 seconds.
- Run it twice in a row: no shared state, same result.
- Break one thing on purpose (for example comment out the refund) and confirm the test fails at the right step, then revert.

## P9-02

**Issue Title:** Write and run the manual test plan for every use case
**Milestone/Phase:** Phase 9: Integration & Submission
**Labels:** implementation
**Estimate:** 4h
**Depends on:** P9-01
**Kind:** doc
**Rubric check:** Assessment 6 - use-case coverage is demonstrated in the running application

### Context & Objective
Walks every use case in the real app, once per role, and records the result. Bugs found become separate issues.

### Implementation Guide
1. Create `docs/test-plan.md` with a table: test ID, use case (UC-01 to UC-17), role, steps, clicks from the dashboard, expected result, result (pass or fail), date.
2. At least one case per use case, and the main alternative flows from the scenarios (seat taken, declined card, blocked flight removal).
3. Run it on a fresh database (`rm -rf ~/.chinookair`) with the built jar. Open an issue labelled `bug` for each failure and fix it.
4. Re-run the failed cases after fixing.

### Testing Strategy
- Every UC from the use-case table appears in at least one test row.
- No use case needs more than 4 clicks from its role's dashboard (the usability requirement in P1-02).
- All rows are `pass` at the end; the failing ones are linked to closed bug issues.
- The plan can be followed by someone who did not write it (read it once as a stranger).

## P9-03

**Issue Title:** Update the UML and report so they match the final code
**Milestone/Phase:** Phase 9: Integration & Submission
**Labels:** design, implementation
**Estimate:** 3h
**Depends on:** P9-02
**Kind:** doc
**Rubric check:** Assessment 5 - the UML represents the final application accurately

### Context & Objective
The assessment compares the diagrams with what was actually built. Anything that changed during coding must be reflected in the diagrams.

### Implementation Guide
1. List every class under `src/main/java`; compare with the class diagram and package diagram. Add, rename or remove boxes.
2. Check each operation named in a sequence diagram exists in code with the same name.
3. Re-export diagrams and rebuild the PDF (`make pdf`) with an "updated for implementation" note on the title page.
4. Keep the updated PDF in `docs/`. The guideline (section 7.2) lists only the compiled file and the source ZIP, so do not add anything else to the submission archive.

### Testing Strategy
- `find src/main/java -name "*.java" | wc -l` is explained by the class diagram (public classes appear; helpers listed in a footnote).
- Every `public` class in `business.control`, `business.entity` and the `data` interfaces is on the diagram.
- Every message in the four sequence diagrams matches a real method (`grep -rn "<methodName>" src/main/java` finds it).

## P9-04

**Issue Title:** Package the fat jar, add a self-test, and run CI on macOS, Windows and Linux
**Milestone/Phase:** Phase 9: Integration & Submission
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P9-01
**Kind:** code
**Rubric check:** Section 4 (portability) and section 7.2 (a compiled JAR that runs)

### Context & Objective
The grader runs the jar on a machine we have not seen. This proves it works on all three operating systems and creates its own database.

### Implementation Guide
1. In `ChinookAir.main` handle the argument `--selftest`: use a temporary database file, run the seeder, log in as `admin`, search a seeded route, print `SELFTEST OK` and exit `0`; any failure prints the reason and exits `1`. No window is opened.
2. In `pom.xml` set the manifest entry `Enable-Native-Access: ALL-UNNAMED` through the shade plugin to remove the SQLite native-access warning on newer JDKs.
3. Change `.github/workflows/build.yml` to a matrix of `ubuntu-latest`, `windows-latest` and `macos-latest`; after `mvn -B verify` run `java -jar target/ChinookAir.jar --selftest`.
4. Keep the jar upload on one platform only.

### Testing Strategy
- Locally: `java -jar target/ChinookAir.jar --selftest` prints `SELFTEST OK` and exits `0` (`echo $?`).
- Run it from another working directory (`cd /tmp && java -jar <path>`): same result.
- Delete `~/.chinookair` and start the GUI: the folder and database are created and the login works.
- CI shows three green jobs. Open the Windows job log and confirm the self-test line is there.
- No native-access warning appears on a recent JDK.

## P9-05

**Issue Title:** Build the submission archive, verify it from scratch, and tag v1.0
**Milestone/Phase:** Phase 9: Integration & Submission
**Labels:** implementation
**Estimate:** 2h
**Depends on:** P9-03, P9-04
**Kind:** code
**Rubric check:** Section 7.2 - ZIP containing the compiled JAR and a ZIP of all source files

### Context & Objective
The last check before submitting: build exactly what will be uploaded and test it as a stranger would.

### Implementation Guide
1. Add `scripts/package-submission.sh`: runs `mvn -B clean verify`, creates `ChinookAir-source.zip` with `git archive` (`src`, `pom.xml`, `checkstyle.xml`, `README.md`, `docs/report` PDF if allowed), and zips `ChinookAir.jar` and the source zip into `ChinookAir-submission.zip`.
2. Add a short `RUN.txt` inside: Java version needed, `java -jar ChinookAir.jar`, demo accounts.
3. Open the PR from `dev` into `main`, merge it once CI is green and tag `v1.0`.
4. Submit the archive.

### Testing Strategy
- Unzip `ChinookAir-submission.zip` into an empty temporary folder: it contains the jar, the source zip and `RUN.txt`.
- `java -jar ChinookAir.jar --selftest` from that folder prints `SELFTEST OK`.
- Unzip the source zip into another empty folder and run `mvn -q package`: builds without the Git history.
- Every `.java` file under `src/main/java` is inside the source zip (compare counts).
- Tag `v1.0` exists on `main` (`git tag -l v1.0`).
- The PR template is now on `main`: `gh api graphql -f query='{repository(owner:"satyambhanot",name:"ChinookAir"){pullRequestTemplates{filename}}}'` lists `pull_request_template.md`.
