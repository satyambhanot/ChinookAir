# Phase 2, 3 and 4 issues (design)

## P2-01

**Issue Title:** Write the Book Flight use-case scenario
**Milestone/Phase:** Phase 2: Scenarios & Domain Model
**Labels:** design
**Estimate:** 3h
**Depends on:** P1-03, P0-04
**Kind:** doc
**Rubric check:** Section 7.1 C - detailed scenario with classes single-underlined and operations double-underlined

### Context & Objective
Scenario 1 of 3. Book Flight is the longest end-to-end flow, so it deliberately touches most entities: Customer, Promotion, Flight, Airline, Route, Airport, Seat, Reservation, Payment and the confirmation.

### Implementation Guide
1. In `docs/report/sections/c-scenarios.tex` use one template: use case name, actor, preconditions, trigger, main success scenario (numbered), extensions (numbered 3a, 3b), postconditions.
2. Main flow: the Customer logs in; the system shows the monthly Promotion if it is the first launch of the month; the Customer enters origin, destination, date and optionally an Airline; the system lists Flights with available Seats, times and prices; the Customer selects a Flight; the system shows the seat map; the Customer selects a Seat; the system creates a Reservation (pending payment); the Customer enters Payment details; the system simulates the payment and confirms the Reservation; the system generates the confirmation (screen and `.txt` file).
3. Extensions: no flights found; seat taken meanwhile; card declined (retry or abandon, abandon cancels the reservation); invalid card details.
4. Underline each candidate class (noun) once with `\uline{}` and each candidate operation (verb) twice with `\uuline{}`. Add a one-line legend.

### Testing Strategy
- At least 8 numbered main steps, each one action by one actor.
- At least 3 extensions, each saying where it rejoins the main flow or ends.
- Underlined nouns are all in the candidate-class table (P2-04).
- Legend explaining single and double underline is present.
- Rendered PDF shows both underline styles correctly.

## P2-02

**Issue Title:** Write the Manage Flights use-case scenario
**Milestone/Phase:** Phase 2: Scenarios & Domain Model
**Labels:** design
**Estimate:** 2h
**Depends on:** P2-01
**Kind:** doc
**Rubric check:** Section 7.1 C - second scenario, covering the administrator side and Route, Aircraft, Airline

### Context & Objective
Scenario 2 of 3. It covers what Book Flight does not: Administrator, Route, Aircraft, flight schedule, and removing a flight.

### Implementation Guide
1. Same template as P2-01.
2. Main flow: the Administrator logs in and chooses Manage Flights; chooses Add Flight; picks a Route, an Airline and an Aircraft; enters flight number, departure, arrival and base price; the system validates and creates the Flight and its Seats; the Administrator updates the schedule of an existing Flight; the Administrator removes an outdated Flight.
3. Validation rules inside the flow: flight number is unique; arrival is after departure; the Aircraft is not already on another Flight at that time.
4. Extensions: route does not exist (the Administrator adds the Route first); aircraft does not exist (the Administrator adds the Aircraft first); aircraft busy; the Flight still has active Reservations (removal is blocked).
5. Underline classes once and operations twice.

### Testing Strategy
- At least 7 main steps and 3 extensions.
- Mentions Administrator, Route, Airline, Aircraft, Flight and Seat (checklist).
- Every underlined noun and verb appears in P2-04.

## P2-03

**Issue Title:** Write the Modify Reservation use-case scenario
**Milestone/Phase:** Phase 2: Scenarios & Domain Model
**Labels:** design
**Estimate:** 2h
**Depends on:** P2-01
**Kind:** doc
**Rubric check:** Section 7.1 C - third scenario, covering the Flight Agent, customer profile and payment records

### Context & Objective
Scenario 3 of 3. It covers the Flight Agent role, customer profile maintenance and payment records.

### Implementation Guide
1. Same template as P2-01.
2. Main flow: the Flight Agent logs in; searches a Customer by name or email; views and edits the profile; opens the Customer's reservation history; selects a Reservation; chooses Modify; searches another Flight; the system checks seat availability; the agent picks the new Flight and Seat; the system recalculates the price; if the price is the same the Reservation stays confirmed, otherwise the old Payment is refunded and the Reservation goes back to pending payment; the agent reviews the payment records and takes the new payment; the system regenerates the confirmation.
3. Extensions: new flight is full; reservation already cancelled or the flight already departed; new payment declined.
4. Underline classes once and operations twice.

### Testing Strategy
- At least 8 main steps and 3 extensions.
- Mentions Flight Agent, Customer, Reservation, Payment and Seat.
- The price rule matches the Reservation state diagram (P3-11).

## P2-04

**Issue Title:** Extract the candidate class and operation list and check coverage
**Milestone/Phase:** Phase 2: Scenarios & Domain Model
**Labels:** design
**Estimate:** 2h
**Depends on:** P2-02, P2-03
**Kind:** doc
**Rubric check:** Section 7.1 F note - every class in the class diagram traces to a use-case scenario

### Context & Objective
The guideline says marks may be deducted for classes that cannot be traced to the scenarios. This table is the traceability evidence between Section C and Section F.

### Implementation Guide
1. In `docs/report/sections/c-scenarios.tex` add a table of underlined nouns: noun, decision (entity, control, boundary, or rejected), reason, scenario(s).
2. Add a second table of double-underlined verbs: verb, operation name, owner class.
3. Expected result: entities User, Customer, FlightAgent, Administrator, Airline, Airport, Route, Aircraft, Flight, Seat, Reservation, Payment, Promotion; the 11 control classes from P1-03; and the boundary classes listed in P2-06.
4. A class that appears in no scenario must either be added to a scenario or dropped from the design.
5. Judge each noun with the selection characteristics from Module 12: retained information, needed services, multiple attributes, common attributes, common operations, essential requirement. Keep a noun when it meets most of them. A noun with a single attribute becomes an attribute of another class instead.
6. Sort each verb into one of the four operation kinds from Module 12 (changes data, computes, inquires about state, monitors an event) so the operation list has a reason behind it.
7. Add a third table that traces each of the 11 control classes to the scenario step that uses it, so no control class is left without a scenario.

### Testing Strategy
- Every underlined word in the three scenarios appears in a table.
- Every planned class appears in at least one scenario, or has a written reason to stay.
- Rejected nouns (for example "system", "screen") each have a one-line reason.
- Every kept class has a note of which selection characteristics it meets (at least four of the six).
- Each of the 11 control classes names a scenario step or extension that uses it.

## P2-05

**Issue Title:** Draw the first-cut class diagram: entities
**Milestone/Phase:** Phase 2: Scenarios & Domain Model
**Labels:** design
**Estimate:** 3h
**Depends on:** P2-04
**Kind:** doc
**Rubric check:** Section 7.1 F - attributes, relationships, cardinalities, stereotypes

### Context & Objective
First half of Deliverable F. The entity classes are the stable core that the database and controllers are built around.

### Implementation Guide
1. In `docs/diagrams/src/10-class-entities.drawio` draw all entities with `<<entity>>`, typed attributes (`- name: String`) and operations that matter (`+ hasDeparted(now): boolean`). No getters, setters or constructors.
2. Inheritance: User (abstract) is the parent of Customer, FlightAgent and Administrator.
3. Associations with multiplicity on both ends: Airline 1 - 0..* Flight; Route 1 - 0..* Flight; Aircraft 1 - 0..* Flight; Airport 1 - 0..* Route as origin and again as destination (two roles); Flight 1 composes 0..* Seat; Customer 1 - 0..* Reservation; Reservation 0..* - 1 Flight; Reservation 0..1 - 0..1 Seat (the seat is chosen after the reservation is created, and a seat holds at most one active reservation); Reservation 1 - 0..* Payment. Write 0..* instead of a bare * (Module 12).
4. Enumerations shown as `<<enumeration>>`: Role, SeatClass, ReservationStatus, PaymentStatus, PaymentMethodType.
5. Promotion has no associations.

### Testing Strategy
- Number of multiplicity labels equals twice the number of associations.
- No `get`/`set` operations: `grep -i "get\|set"` on the exported text is empty.
- Every class in the diagram is in the P2-04 table.
- Every attribute has a type.

## P2-06

**Issue Title:** Draw the first-cut class diagram: controls and boundaries
**Milestone/Phase:** Phase 2: Scenarios & Domain Model
**Labels:** design
**Estimate:** 3h
**Depends on:** P2-05
**Kind:** doc
**Rubric check:** Assessment 4 - separation of boundary, entity and control classes

### Context & Objective
Second half of Deliverable F and the main evidence for separation of concerns: boundaries call controls, controls coordinate entities, boundaries never touch entities or the database.

### Implementation Guide
1. In `docs/diagrams/src/11-class-control-boundary.drawio` add the 11 `<<control>>` classes (plus the helper `ConfirmationWriter`) with operations taken from the double-underlined verbs, using the names of the controller issues (`authenticate`, not `login`), for example `BookingController: startReservation(customerId, flightId)`, `selectSeat(reservationId, seatNo)`, `cancelPending(reservationId)`.
2. Add the `<<boundary>>` classes: MainFrame, LoginView, RegisterView, CustomerDashboard, AgentDashboard, AdminDashboard, ProfileView, PromotionDialog, FlightSearchView, SeatSelectionView, PaymentView, ConfirmationView, MyReservationsView, CustomerManagementView, ModifyReservationView, PaymentRecordsView, ScheduleView, FlightManagementView, RouteManagementView, AircraftManagementView.
3. Draw dependencies: each boundary uses exactly one primary control; each control uses the entities it coordinates. MainFrame composes the three dashboards and uses `PromotionController`; the dashboards only open other views, so they have no control dependency.
4. DAO interfaces and patterns are added in P4-01, not here.

### Testing Strategy
- Every control class from P1-03 is on the diagram.
- No arrow from a boundary directly to an entity that changes state.
- No boundary has more than two outgoing dependencies to controls.
- Every operation name appears in P2-04's verb table.

## P3-01

**Issue Title:** Draw the Login activity diagram
**Milestone/Phase:** Phase 3: Behavioral Diagrams
**Labels:** design
**Estimate:** 2h
**Depends on:** P2-01
**Kind:** doc
**Rubric check:** Section 7.1 A - activity diagram 1 of 4 (login process)

### Context & Objective
Activity diagram 1 of 4. Shows the login flow including the monthly promotion on startup and role-based routing.

### Implementation Guide
1. In `docs/diagrams/src/20-activity-login.drawio` use two swimlanes: User and System.
2. Flow: initial node; open app; decision "first launch this month?" (yes: show promotion); enter username and password; decision "fields filled?" (no: show error, back to entry); find user; decision "user found and password matches?" (no: show "Invalid username or password", back to entry); decision "role?" with three branches to Customer, Agent and Admin dashboards; final node.
3. Every decision has labeled guards in square brackets.

### Testing Strategy
- Exactly one initial node and at least one final node.
- Every decision node has two or more labeled outgoing guards.
- Every path from the initial node reaches a final node (trace each by hand).
- Matches the steps in P2-01 step 1 and the LoginController design.

## P3-02

**Issue Title:** Draw the Browse and Select Flight activity diagram
**Milestone/Phase:** Phase 3: Behavioral Diagrams
**Labels:** design
**Estimate:** 2h
**Depends on:** P2-01
**Kind:** doc
**Rubric check:** Section 7.1 A - activity diagram 2 of 4 (browsing for and selecting a flight)

### Context & Objective
Activity diagram 2 of 4. Covers FR-1 and FR-2.

### Implementation Guide
1. In `docs/diagrams/src/21-activity-browse-flight.drawio` use User and System swimlanes.
2. Flow: enter origin, destination, date, optional airline; decision "origin different from destination and date not in the past?" (no: show message); query flights; decision "any results?" (no: show "No flights found", change criteria); show table with times, price and seats left; user selects a flight; show flight details; decision "seats available?" (no: back to results); continue to seat selection.

### Testing Strategy
- Both validation guards (same origin and destination, date in the past) are present.
- The "no results" loop returns to the criteria step.
- Every decision has labeled guards and every path ends.

## P3-03

**Issue Title:** Draw the Book Flight activity diagram
**Milestone/Phase:** Phase 3: Behavioral Diagrams
**Labels:** design
**Estimate:** 2h
**Depends on:** P3-02
**Kind:** doc
**Rubric check:** Section 7.1 A - activity diagram 3 of 4 (booking a flight)

### Context & Objective
Activity diagram 3 of 4. Uses a fork and join to show that screen confirmation and file creation happen together.

### Implementation Guide
1. In `docs/diagrams/src/22-activity-book-flight.drawio` use User and System swimlanes.
2. Flow: show seat map; user selects seat; decision "seat still available?" (no: show message and refresh map); create reservation in pending payment; hand over to the payment activity; decision "payment approved?" (no: retry or cancel reservation and release seat); confirm reservation; fork: show confirmation on screen and write confirmation `.txt` file; join; final node.
3. Show the payment step as a call-behavior node that refers to the payment diagram.

### Testing Strategy
- One fork and one matching join.
- "Seat taken" and "payment declined" paths exist and end correctly.
- The reservation statuses mentioned match P3-11.

## P3-04

**Issue Title:** Draw the Make Payment activity diagram
**Milestone/Phase:** Phase 3: Behavioral Diagrams
**Labels:** design
**Estimate:** 2h
**Depends on:** P3-03
**Kind:** doc
**Rubric check:** Section 7.1 A - activity diagram 4 of 4 (making a payment)

### Context & Objective
Activity diagram 4 of 4. Covers FR-9 with the simulated gateway.

### Implementation Guide
1. In `docs/diagrams/src/23-activity-payment.drawio` use User and System swimlanes.
2. Flow: choose credit or debit; enter name, card number, expiry; decision "valid input?" (card number 16 digits, expiry not in the past, name not empty; no: show errors); simulate payment; decision "approved?" (card ending 0000, or debit over 5000.00, is declined); approved: record payment with masked card and confirm reservation; declined: record declined payment and offer retry or cancel.

### Testing Strategy
- Both decline rules are written on the diagram or in a note.
- Only the last 4 digits are mentioned as stored.
- Matches the payment state diagram (P3-10).

## P3-05

**Issue Title:** Draw the Find Available Flights sequence diagram
**Milestone/Phase:** Phase 3: Behavioral Diagrams
**Labels:** design
**Estimate:** 2h
**Depends on:** P2-06
**Kind:** doc
**Rubric check:** Section 7.1 D - sequence diagram 1 of 4, with FlightSearchController as the control class

### Context & Objective
Sequence diagram 1 of 4. The control class must exist in the class diagram.

### Implementation Guide
1. In `docs/diagrams/src/30-sequence-find-flights.drawio` lifelines: Customer, FlightSearchView, FlightSearchController, FlightDao, SeatDao, Flight.
2. Messages: enter criteria; `search(criteria)`; `validate(criteria)`; `FlightDao.search(origin, destination, date, airline)`; return flights; per flight `Flight.hasDeparted(now)` and `SeatDao.countAvailable(flightId)`; return offers; show a message or the table. The names follow the data layer issues.
3. Add an `alt` fragment: invalid criteria returns an error message; and an `alt` for empty result.
4. Use activation bars and return arrows.

### Testing Strategy
- Every lifeline name matches a class in the class diagram.
- Every message to a controller or DAO exists as an operation in the class diagram (check by listing them).
- At least two `alt` branches with guards.

## P3-06

**Issue Title:** Draw the Book Flight sequence diagram
**Milestone/Phase:** Phase 3: Behavioral Diagrams
**Labels:** design
**Estimate:** 2h
**Depends on:** P2-06
**Kind:** doc
**Rubric check:** Section 7.1 D - sequence diagram 2 of 4, with BookingController as the control class

### Context & Objective
Sequence diagram 2 of 4. Shows seat selection and the creation of a reservation, and references the payment diagram.

### Implementation Guide
1. In `docs/diagrams/src/31-sequence-book-flight.drawio` lifelines: Customer, SeatSelectionView, BookingController, SeatDao, ReservationDao, Reservation.
2. Messages: `startReservation(customerId, flightId)` creates a Reservation (`<<create>>`, state CREATED) and calls `ReservationDao.save`; `availableSeats(flightId)` calls `SeatDao.findByFlight`; the customer picks a seat; `selectSeat(reservationId, seatNo)` calls `ReservationDao.findById`, then `assignSeat(seatNo, price)` on Reservation (state PENDING_PAYMENT), then `ReservationDao.update(reservation)`. Show the state names in notes next to the Reservation lifeline.
3. `alt` fragment: seat already taken raises SeatUnavailableException and the view refreshes the seat map.
4. End with an `sd` reference frame pointing to the Make Payment diagram.

### Testing Strategy
- Lifelines and operations match the class diagram exactly.
- The reservation state names match the state diagram (P3-11).
- The reference frame to the payment diagram is present.

## P3-07

**Issue Title:** Draw the Make Payment sequence diagram
**Milestone/Phase:** Phase 3: Behavioral Diagrams
**Labels:** design
**Estimate:** 2h
**Depends on:** P2-06
**Kind:** doc
**Rubric check:** Section 7.1 D - sequence diagram 3 of 4, with PaymentController as the control class

### Context & Objective
Sequence diagram 3 of 4. Shows the Strategy pattern for payment methods and the state change of the reservation.

### Implementation Guide
1. In `docs/diagrams/src/32-sequence-payment.drawio` lifelines: Customer, PaymentView, PaymentController, ReservationDao, PaymentMethod (credit card), Payment, PaymentDao, Reservation, ConfirmationWriter.
2. Messages: `pay(reservationId, type, details)`; `ReservationDao.findById`; `validate(details)` as a self-call; `authorize(details, amount)` on the strategy chosen by `PaymentMethods.forType`; result; create the Payment (state PENDING); on approval `approve()`, `PaymentDao.save`, `confirmPayment()` on Reservation, `ReservationDao.update` and `write(reservation, payment)`. Show the Payment and Reservation state names in notes.
3. `alt`: approved vs declined. Declined calls `decline(reason)`, saves the payment as DECLINED and leaves the reservation in PENDING_PAYMENT.

### Testing Strategy
- Names match the class diagram (including the Strategy interface from P4-01).
- Both branches are present and end with a return to the view.
- Operations on `Reservation` match the state diagram transitions.

## P3-08

**Issue Title:** Draw the Manage Flights sequence diagram
**Milestone/Phase:** Phase 3: Behavioral Diagrams
**Labels:** design
**Estimate:** 2h
**Depends on:** P2-06
**Kind:** doc
**Rubric check:** Section 7.1 D - sequence diagram 4 of 4, with FlightManagementController as the control class

### Context & Objective
Sequence diagram 4 of 4, covering the administrator side.

### Implementation Guide
1. In `docs/diagrams/src/33-sequence-manage-flights.drawio` lifelines: Administrator, FlightManagementView, FlightManagementController, RouteDao, AirlineDao, AircraftDao, FlightDao, SeatDao, ReservationDao.
2. Messages for add: `addFlight(details)`; role check `Authorizer.require(actor, ADMIN)`; `RouteDao.findById`, `AirlineDao.findByCode`, `AircraftDao.findById`; `FlightDao.existsByFlightNumber` and `FlightDao.findByAircraft`; `validate(details)` (unique number, arrival after departure, aircraft free); `FlightDao.save(flight)`; `SeatDao.createSeats(flightId, seats)`. Returns of the plain lookups may be left out.
3. `alt` for a rejected role, `alt` for validation failure; `opt` fragment for removing a flight that calls `ReservationDao.countActiveByFlight`, blocks it when reservations are active, and otherwise calls `delete` or `archive`.

### Testing Strategy
- The role check appears (a customer calling this is rejected, in its own `alt` branch).
- Both the validation `alt` and the removal `opt` are present.
- Names match the class diagram.

## P3-09

**Issue Title:** Draw the whole-system state diagram
**Milestone/Phase:** Phase 3: Behavioral Diagrams
**Labels:** design
**Estimate:** 2h
**Depends on:** P3-01
**Kind:** doc
**Rubric check:** Section 7.1 E - state diagram 1 of 3 (overall system)

### Context & Objective
State diagram 1 of 3. Shows the lifecycle of the application as a whole.

### Implementation Guide
1. In `docs/diagrams/src/40-state-system.drawio` states: Starting (entry: open database), CheckingPromotion, ShowingPromotion, LoggedOut, Authenticating, a composite LoggedIn state with sub-states CustomerSession, AgentSession and AdminSession, Closing, and Closed.
2. Transitions with events and guards: `appLaunched`, `databaseReady`, `databaseFailed`, `startupChecked [new month]`, `startupChecked [same month]`, `promotionClosed`, `loginSubmitted`, `loginAccepted [role = customer]` (and the same for flight agent and administrator), `loginRejected [credentials invalid]`, `logout`, `exit`, `databaseClosed`, `messageShown`. `logout` and `exit` leave the composite LoggedIn state, so they apply to all three session states.
3. Add an Error state for a database failure at startup that leads to Closed after a message.

### Testing Strategy
- Every transition has an event label, and guards where branching.
- Initial and final pseudostates are present.
- No state without an outgoing transition except the final one (the three session states leave through the transitions of their composite LoggedIn state).

## P3-10

**Issue Title:** Draw the payment state diagram
**Milestone/Phase:** Phase 3: Behavioral Diagrams
**Labels:** design
**Estimate:** 1.5h
**Depends on:** P3-04
**Kind:** doc
**Rubric check:** Section 7.1 E - state diagram 2 of 3 (payment process)

### Context & Objective
State diagram 2 of 3 for a single Payment record.

### Implementation Guide
1. In `docs/diagrams/src/41-state-payment.drawio` states: Pending, Approved, Declined, Refunded.
2. Transitions, written `event [guard] / action`: initial to Pending on `create`; Pending to Approved on `authorize [accepted] / approve()`; Pending to Declined on `authorize [rejected] / decline(reason)`; Approved to Refunded on `refund()`.
3. Declined and Refunded end the life of the record: each has only a completion arrow to a final node and no event (a retry creates a new Payment record). Add a note saying so.
4. Add entry actions, for example Approved: `record masked card`.

### Testing Strategy
- Only the three state-to-state transitions above exist (no Declined to Approved).
- Matches the `PaymentStatus` enumeration in the class diagram.
- Consistent with the payment activity diagram.

## P3-11

**Issue Title:** Draw the reservation state diagram
**Milestone/Phase:** Phase 3: Behavioral Diagrams
**Labels:** design
**Estimate:** 1.5h
**Depends on:** P3-03
**Kind:** doc
**Rubric check:** Section 7.1 E - state diagram 3 of 3 (reservation object)

### Context & Objective
State diagram 3 of 3 for a Reservation object. It later maps one-to-one onto the State pattern classes.

### Implementation Guide
1. In `docs/diagrams/src/42-state-reservation.drawio` states: Created, PendingPayment, Confirmed, Cancelled, Completed.
2. Transitions: Created to PendingPayment on `assignSeat`; PendingPayment to Confirmed on `confirmPayment`; Confirmed to Completed on `complete [flight departed]`; Created, PendingPayment and Confirmed to Cancelled on `cancel`; Confirmed to Confirmed on `changeFlight [price unchanged]`; Confirmed to PendingPayment on `changeFlight [price differs]` (the old payment is refunded).
3. Cancelled and Completed end the life of the reservation: each has only a completion arrow to a final node. Name the states exactly like the `ReservationStatus` values (`CREATED`, `PENDING_PAYMENT`, ...), because the sequence diagrams use those names in their state notes. Add entry actions (confirmation number, seat held, confirmation file, seat freed).

### Testing Strategy
- Every transition above exists with its event and guard.
- No transition leaves Cancelled or Completed except to a final node.
- State names equal the `ReservationStatus` enumeration values.

## P4-01

**Issue Title:** Refine the class diagram with design patterns and DAO interfaces
**Milestone/Phase:** Phase 4: Architecture & Design Report
**Labels:** design
**Estimate:** 4h
**Depends on:** P2-06, P3-11
**Kind:** doc
**Rubric check:** Section 7.1 F (final stage) and assessment 2 and 3 - inheritance, realization, composition, polymorphism and design patterns

### Context & Objective
The guideline asks for a final refinement step that improves reusability, scalability, maintainability and reliability using patterns. This turns the first-cut diagram into the one that goes in the report and that the code is written to.

### Implementation Guide
1. Draw two diagrams, because one would hold about 65 classes: `docs/diagrams/src/12-class-refined.drawio` for the business layer (control classes, entities and the State, Strategy, Factory and Observer patterns) and `docs/diagrams/src/13-class-data.drawio` for the data layer (Singleton, DAO interfaces with their SQLite classes, `DaoFactory`, `AppContext`). Together they add:
   - Singleton: `DatabaseConnection`.
   - DAO: one interface per entity (`FlightDao`, `SeatDao`, `ReservationDao`, `PaymentDao`, `UserDao`, `CustomerDao`, `AirlineDao`, `AirportDao`, `AircraftDao`, `RouteDao`, `PromotionDao`, `AppStateDao`) realized by `Sqlite...Dao` classes, and a `DaoFactory`.
   - Strategy: `PaymentMethod` interface with `CreditCardPayment` and `DebitCardPayment`.
   - State: `ReservationState` interface with one class per state.
   - Factory: `UserFactory` creating the right `User` subclass.
   - Observer: `PromotionListener` registered with `PromotionController`.
2. Use realization arrows for interfaces and composition for Flight and Seat.
3. Next to the diagrams write one sentence per pattern in `f-class.tex`: the problem it solves and where. Add a table of which DAO interfaces each control class uses.

### Testing Strategy
- All six patterns are present and each one has its justification sentence.
- Every DAO interface has exactly one realizing class (12 interfaces, 12 SQLite classes).
- Every class from P2-04 is still present in the business-layer diagram (the 13 entities, the 12 control classes, plus `Authorizer` and `SessionContext` used by the sequence diagrams), and the new classes are explained in the text.
- The diagram is consistent with the four sequence diagrams (same operation names), so every DAO operation they use (for example `FlightDao.search` and `SeatDao.countAvailable`) is listed on its DAO interface.

## P4-02

**Issue Title:** Draw the package diagram with public and package-private classes
**Milestone/Phase:** Phase 4: Architecture & Design Report
**Labels:** design
**Estimate:** 2h
**Depends on:** P4-01
**Kind:** doc
**Rubric check:** Section 7.1 G - package diagram showing the three layers, their classes and which are public

### Context & Objective
Deliverable G. The diagram must show how the three layers interact and which classes are public, and it must match the real Java packages.

### Implementation Guide
1. In `docs/diagrams/src/50-package.drawio` draw packages `presentation`, `business.control`, `business.entity`, `data` and `util`, with dependency arrows: presentation to control; control to entity and to data (DAO interfaces); data to entity.
2. List the classes inside each package. Mark visibility: `+` public (controllers, entities, DAO interfaces, `DaoFactory`, `DatabaseSeeder`, `ConfirmationWriter`, `MainFrame`), `~` package-private (the `Sqlite...Dao` classes, the view classes used only inside `presentation`, the Reservation state classes).
3. Add a short note explaining that nothing in `presentation` imports from `data`.

### Testing Strategy
- Package names equal the real packages under `src/main/java/ca/ucalgary/chinookair`.
- No dependency arrow goes from `entity` to `data` or from `control` to `presentation`.
- Each public class listed also exists in the class diagram.

## P4-03

**Issue Title:** Build the traceability matrix
**Milestone/Phase:** Phase 4: Architecture & Design Report
**Labels:** design
**Estimate:** 2h
**Depends on:** P4-01
**Kind:** doc
**Rubric check:** Assessment 6 - use-case coverage and traceability from use cases to the class diagram

### Context & Objective
One table that proves the chain: requirement, use case, scenario, diagram, control class, entities. It is the quickest way for a marker to see traceability.

### Implementation Guide
1. In `docs/report/sections/traceability.tex` build a table with columns FR, UC, scenario or diagram, control class, entities touched.
2. Fill one row per UC from P1-03. For use cases without a scenario say "covered by activity diagram" or similar.
3. Add a second small table: each class in the class diagram and the scenarios that mention it.

### Testing Strategy
- All 14 FRs appear at least once.
- All 17 UCs appear exactly once.
- Every class in the class diagram appears in the second table with at least one scenario.

## P4-04

**Issue Title:** Run a consistency review across all diagrams and text
**Milestone/Phase:** Phase 4: Architecture & Design Report
**Labels:** design
**Estimate:** 3h
**Depends on:** P4-02, P4-03
**Kind:** doc
**Rubric check:** Assessment 5 and 7 - clarity and accuracy of the UML and quality of scenarios and sequence diagrams

### Context & Objective
Most lost marks in a document like this come from small mismatches between diagrams. This is one deliberate pass to find them.

### Implementation Guide
1. Build a checklist in the issue: every class name, operation name, state name and multiplicity is compared between the use-case table, scenarios, class diagram, sequence diagrams and state diagrams.
2. Read each sequence diagram aloud against the class diagram.
3. Fix mismatches in the sources and re-export.
4. Print the report to PDF and review it at 100% zoom.

### Testing Strategy
- Zero mismatches on the checklist.
- `grep -rniE "\b(ticket|booking)\b" docs/report` shows only the allowed terms.
- Every exported diagram is readable when the PDF is viewed at 100%.

## P4-05

**Issue Title:** Assemble and export the design PDF, then tag design-v1
**Milestone/Phase:** Phase 4: Architecture & Design Report
**Labels:** design
**Estimate:** 3h
**Depends on:** P4-04
**Kind:** doc
**Rubric check:** Section 7.1 - the design document is submitted as a PDF containing requirements and artifacts A to G

### Context & Objective
Closes the 50-mark design phase.

### Implementation Guide
1. Fill the title page, include all exported images, and run `make pdf`.
2. Check the page order: requirements, A (introduction and four activity diagrams), B, C, D, E, F, G, traceability.
3. Open a PR from `dev` into `main` with the final report and tag `design-v1` after merging.
4. Submit the PDF.

### Testing Strategy
- The PDF has all sections A to G plus requirements, in that order.
- Count of figures: 4 activity, 1 use case, 4 sequence, 3 state, class diagrams, 1 package.
- Page number and table of contents are correct and no figure is cut off.
