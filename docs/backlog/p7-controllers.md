# Phase 7 issues (business logic: controllers)

Paths are relative to `src/main/java/ca/ucalgary/chinookair/` unless they start with `src/`.
Controllers get their DAOs, a `java.time.Clock` and other helpers through the constructor. No controller imports `javax.swing` or `java.sql`. Tests use `TestDatabase.inMemory()` and a fixed `Clock` (`2026-12-01T09:00`).

## P7-01

**Issue Title:** Implement LoginController, SessionContext and the role check
**Milestone/Phase:** Phase 7: Business Logic
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P6-10
**Kind:** code
**Rubric check:** FR-13 (login with three roles) and section 3.4 (administrator-only operations)

### Context & Objective
Every later controller needs to know who is acting and whether they are allowed. Building login and the permission check once, here, keeps that logic out of the screens.

### Implementation Guide
1. `business/control/LoginController.java`: constructor takes `UserDao`. `User authenticate(String username, String password)`.
2. Rules: trim the username; blank username or password throws `IllegalArgumentException("Username is required")` or `("Password is required")`; unknown user and wrong password both throw `AuthenticationException("Invalid username or password")` (same message so nobody can tell which usernames exist).
3. `business/control/SessionContext.java`: holds the current user for one running app (an instance passed around, not a static global). `login(User)`, `logout()`, `currentUser()`.
4. `business/control/Authorizer.java`: `static void require(User actor, Role... allowed)` throws `AccessDeniedException`. Reused by every controller.
5. Exceptions `AuthenticationException` and `AccessDeniedException` (runtime) in `business/control/`.

### Testing Strategy
Fixture: users `customer1`, `agent1`, `admin` saved with known passwords.
- `authenticate("admin","admin123")` returns an `Administrator`; `authenticate(" ADMIN ","admin123")` also works (trim, case-insensitive).
- `authenticate("admin","wrong")` and `authenticate("nobody","x")` both throw `AuthenticationException` with the identical message.
- `authenticate("","x")` throws `IllegalArgumentException` containing `Username is required`; `authenticate("admin","")` contains `Password is required`.
- `Authorizer.require(customer1, Role.ADMIN)` throws `AccessDeniedException`; `require(admin, Role.ADMIN)` passes; `require(agent1, Role.AGENT, Role.ADMIN)` passes.
- `SessionContext`: `currentUser()` is empty before login, the user after login, empty after `logout()`.

## P7-02

**Issue Title:** Implement PromotionController with the Observer pattern
**Milestone/Phase:** Phase 7: Business Logic
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P7-01
**Kind:** code
**Rubric check:** FR-12 / section 3.7 - random promotion on the first launch of a month, shown only once that month

### Context & Objective
The promotion rule is easy to get subtly wrong (showing it twice, or never in a new month). Injecting the clock and the random choice makes the rule fully testable.

### Implementation Guide
1. `business/control/PromotionListener.java`: functional interface `void onPromotion(Promotion)`.
2. `business/control/PromotionController.java`: constructor takes `PromotionDao`, `AppStateDao`, `Clock`, `IntUnaryOperator chooseIndex` (production passes `new SecureRandom()::nextInt`).
3. `addListener(listener)`; `checkOnStartup()`: compute `YearMonth.now(clock)`; if `app_state.last_promo_month` equals it, do nothing; otherwise load all promotions, pick `promotions.get(chooseIndex.applyAsInt(size))`, save the month, notify listeners.
4. If there are no promotions, notify nobody and do not save the month.
5. The month is saved when the promotion is picked, so a crash after showing it cannot cause a second showing.

### Testing Strategy
Three promotions saved; `chooseIndex = bound -> 1` (always the second).
- Clock `2026-12-01`: first call notifies the listener once with promotion #2; `last_promo_month` is `2026-12`.
- Second call the same day: no notification.
- Clock `2026-12-15`: no notification.
- Clock `2027-01-02`: notifies again; stored month is `2027-01`.
- Zero promotions: no notification and no month stored.
- Two listeners both receive the promotion; zero listeners does not throw.

## P7-03

**Issue Title:** Implement FlightSearchController
**Milestone/Phase:** Phase 7: Business Logic
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P6-04
**Kind:** code
**Rubric check:** FR-1 and FR-2 (search by origin, destination, date, airline; show seats, times, prices)

### Context & Objective
Turns raw search input into validated criteria and returns offers ready to display: flight, seats left and the lowest price.

### Implementation Guide
1. `business/control/SearchCriteria.java`: record `(String origin, String destination, LocalDate date, String airlineCode)`; airline may be null for "any".
2. `business/control/FlightOffer.java`: record `(Flight flight, int seatsLeft, BigDecimal fromPrice)`.
3. `FlightSearchController(FlightDao, SeatDao, Clock)` with `List<FlightOffer> search(SearchCriteria)` and `FlightOffer details(long flightId)`.
4. Validation: origin and destination required, 3 letters, normalized to uppercase; origin must differ from destination; the date may not be before today. Flights that already departed today are left out.
5. `fromPrice` is the economy price from `PricingPolicy`.

### Testing Strategy
Clock `2026-12-01T09:00`. Flights `YYC` to `YVR` at 08:00, 12:00, 18:00 on the 1st (base 200.00), one on the 2nd.
- Search `YYC`, `YVR`, `2026-12-01`: returns 12:00 and 18:00 only (08:00 already departed), in order.
- Lowercase `yyc`, `yvr` gives the same result.
- Same origin and destination throws `IllegalArgumentException` containing `must be different`.
- Date `2026-11-30` throws containing `cannot be in the past`; today is accepted.
- Airline filter `AC` returns only AC flights.
- `seatsLeft` equals `SeatDao.countAvailable`; after one seat is reserved it drops by 1.
- `fromPrice` for a base of 200.00 is `200.00`.
- Origin `YY` throws containing `3 letters`; `null` origin throws.

## P7-04

**Issue Title:** Implement BookingController with seat selection and pricing
**Milestone/Phase:** Phase 7: Business Logic
**Labels:** implementation
**Estimate:** 4h
**Depends on:** P7-03, P5-04, P6-06
**Kind:** code
**Rubric check:** FR-3, FR-14 (create reservation, seat selection) and the reliability NFR (no double booking)

### Context & Objective
This is the heart of the Book Flight use case. The seat race (two people picking the same seat) must always end with exactly one winner and a clear message for the other.

### Implementation Guide
1. `business/control/ConfirmationNumberGenerator.java`: interface `String next()`; default implementation produces `CA` plus 6 random uppercase letters or digits.
2. `BookingController(FlightDao, SeatDao, ReservationDao, ConfirmationNumberGenerator, Clock)`:
   - `Reservation startReservation(User actor, Customer customer, long flightId)`: a customer may only book for themselves, an agent for anyone, an administrator not at all (`Authorizer`). Flight must not have departed. Creates a `CREATED` reservation with a fresh confirmation number (retry up to 5 times on collision).
   - `List<Seat> availableSeats(long flightId)`.
   - `Reservation selectSeat(long reservationId, String seatNo)`: seat must exist on the flight, price from `PricingPolicy`, calls `assignSeat`, saves. A `SeatUnavailableException` from the DAO bubbles up unchanged.
   - `void cancelPending(long reservationId)`: only for `CREATED` or `PENDING_PAYMENT`.
3. No UI wording in this class; the view translates exceptions into messages.

### Testing Strategy
Flight base price 200.00, 3x4 aircraft.
- `selectSeat(r, "1A")` (business) gives price `300.00`; `"3C"` (economy) gives `200.00`; status is `PENDING_PAYMENT`.
- Customer B selecting `1A` after customer A has it throws `SeatUnavailableException`; A's reservation is unchanged.
- `"99Z"` throws `IllegalArgumentException` containing `does not exist on this flight`.
- Starting a reservation on a flight whose clock is after departure throws `IllegalStateException` containing `already departed`.
- Customer A starting a reservation for customer B throws `AccessDeniedException`; an agent can; an administrator cannot.
- `cancelPending` on a `PENDING_PAYMENT` reservation makes it `CANCELLED` and the seat available again; on a `CONFIRMED` one throws `IllegalStateException`.
- 1000 generated confirmation numbers all match `^CA[A-Z0-9]{6}$` and are unique.

## P7-05

**Issue Title:** Implement ConfirmationWriter (booking confirmation text file)
**Milestone/Phase:** Phase 7: Business Logic
**Labels:** implementation
**Estimate:** 2h
**Depends on:** P5-05
**Kind:** code
**Rubric check:** Section 3.2 (generate booking confirmations) and section 5 (file-access functionality in the data layer)

### Context & Objective
The guideline mentions file access in the data layer and confirmations in booking management. A small confirmation file satisfies both and is easy to check.

### Implementation Guide
1. `data/ConfirmationWriter.java` (public): constructor takes a directory `Path` (default `~/.chinookair/confirmations`).
2. `Path write(Reservation, Payment)` creates the directory if needed and writes `<confirmationNumber>.txt` in UTF-8 with lines: Confirmation, Passenger, Flight, Route, Departure (`yyyy-MM-dd HH:mm`), Seat and class, Amount paid, Card (masked), Payment status.
3. Writing again for the same confirmation number overwrites (used after a modification).
4. Failures throw `ConfirmationWriteException` (runtime). The payment flow catches it, so a payment that succeeded is never rolled back because a file could not be written.

### Testing Strategy
Use `@TempDir`.
- `write` creates `CA12AB34.txt`; the content contains `Confirmation: CA12AB34`, `Seat: 1A (BUSINESS)`, `Departure: 2026-12-01 08:00`, `Amount paid: 300.00`, `Card: **** **** **** 1111`.
- The full 16-digit card number does not appear in the file.
- A second `write` for the same reservation overwrites and the folder still has one file.
- A directory path that is actually a file throws `ConfirmationWriteException`.

## P7-06

**Issue Title:** Implement PaymentController with simulated authorization and refunds
**Milestone/Phase:** Phase 7: Business Logic
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P7-04, P7-05
**Kind:** code
**Rubric check:** FR-9 (simulated payment, payment records) and the Make Payment use case

### Context & Objective
Connects the Strategy classes from Phase 5 to a real reservation: approving a payment confirms the reservation, declining leaves it pending.

### Implementation Guide
1. `business/control/PaymentResult.java`: record `(Payment payment, Reservation reservation, Path confirmationFile, String message)`.
2. `PaymentController(ReservationDao, PaymentDao, ConfirmationWriter, Clock)`:
   - `PaymentResult pay(User actor, long reservationId, PaymentMethodType type, PaymentDetails details)`: the reservation must be `PENDING_PAYMENT` and belong to the actor (agents may pay for anyone). Amount is the reservation price. Validate the details, pick the strategy with `PaymentMethods.forType`, authorize, save the `Payment` as `APPROVED` or `DECLINED`.
   - On approval: `reservation.confirmPayment()`, update, write the confirmation (a write failure adds a warning to the message but does not undo the payment).
   - On decline: reservation stays `PENDING_PAYMENT` so the customer can retry.
   - `List<Payment> recordsForCustomer(User actor, long customerId)`: the customer themself or an agent.
3. Store only the masked card.

### Testing Strategy
Reservation pending at 300.00.
- Credit `4111 1111 1111 1111`, `Jo Smith`, `12/30`: payment `APPROVED` for `300.00`, reservation `CONFIRMED`, confirmation file exists.
- Card `4111111111110000`: payment `DECLINED`, reservation still `PENDING_PAYMENT`, no file.
- A decline followed by a good card: two payment rows (`DECLINED`, `APPROVED`) and reservation `CONFIRMED`.
- Debit with 6000.00 reservation is declined.
- Paying a `CONFIRMED` reservation throws `IllegalStateException`.
- Customer paying another customer's reservation throws `AccessDeniedException`; an agent can.
- Invalid details (15 digits) throw `IllegalArgumentException` and no payment row is saved.
- The stored payment has `maskedCard` ending `1111` and no full number.

## P7-07

**Issue Title:** Implement ReservationController (history, cancel, modify)
**Milestone/Phase:** Phase 7: Business Logic
**Labels:** implementation
**Estimate:** 4h
**Depends on:** P7-06
**Kind:** code
**Rubric check:** FR-4 (cancel or modify reservations) and FR-10 (booking history)

### Context & Objective
Cancel and modify touch the reservation, the seat and the payment together and must never leave them inconsistent.

### Implementation Guide
1. `ReservationController(ReservationDao, PaymentDao, FlightDao, SeatDao, Clock)`.
2. `List<Reservation> history(User actor, long customerId)`: the customer themself or an agent. Before returning, mark `CONFIRMED` reservations whose flight departed as `COMPLETED` via `complete(clock)`.
3. `Reservation cancel(User actor, long reservationId)`: customer for their own, agent for anyone. Flight must not have departed. Cancels the reservation (seat frees automatically) and refunds every `APPROVED` payment (`APPROVED` to `REFUNDED`).
4. `Reservation modify(User actor, long reservationId, long newFlightId, String newSeatNo)`: agent only. Reservation must be `CONFIRMED`, the new flight in the future, the seat available. Do the reservation update first (the seat unique index will reject a taken seat before anything else changed), then if the price differs refund the old payment so the state goes to `PENDING_PAYMENT`.

### Testing Strategy
- Cancel a `CONFIRMED` reservation paid 300.00: status `CANCELLED`, payment `REFUNDED`, seat `1A` available again.
- Cancel a `PENDING_PAYMENT` reservation with no payment: `CANCELLED`, nothing to refund.
- Cancel after departure throws `IllegalStateException` containing `already departed`.
- Customer cancelling someone else's reservation throws `AccessDeniedException`.
- Modify from a 200.00 flight to another 200.00 flight seat `3C`: still `CONFIRMED`, payment unchanged.
- Modify to a 250.00 flight: `PENDING_PAYMENT`, old payment `REFUNDED`.
- Modify to a seat that is taken throws `SeatUnavailableException`; reservation and payment are unchanged.
- A customer calling `modify` throws `AccessDeniedException`.
- `history` returns newest first, and a reservation whose flight departed yesterday is `COMPLETED`.

## P7-08

**Issue Title:** Implement CustomerController (register, view, edit, search)
**Milestone/Phase:** Phase 7: Business Logic
**Labels:** implementation
**Estimate:** 2h
**Depends on:** P7-01, P6-05
**Kind:** code
**Rubric check:** FR-6 / section 3.3 (create profiles, view and edit customer details)

### Context & Objective
Customer data is entered by customers and agents, so validation and duplicate checks need to live in one place with messages a person can act on.

### Implementation Guide
1. `CustomerController(CustomerDao, UserDao)`.
2. `Customer register(String username, String password, String name, String email, String phone)`: password at least 8 characters with at least one digit; hashes with `PasswordHasher`; pre-checks duplicates to give friendly messages (the database constraint is the backstop).
3. `Customer view(User actor, long customerId)` (self or agent), `Customer update(User actor, long customerId, String name, String email, String phone)` (self or agent), `List<Customer> search(User actor, String text)` (agent only).

### Testing Strategy
- `register("jsmith","pass1234","Jo Smith","jo@x.ca","")` succeeds and the stored hash is not the plaintext.
- Password `short1` throws `IllegalArgumentException` containing `at least 8 characters`; `longpassword` (no digit) throws containing `digit`.
- Duplicate email `JO@X.CA` throws containing `Email already registered`; duplicate username throws containing `Username already taken`.
- A customer viewing another customer throws `AccessDeniedException`; an agent can.
- `update` changes the phone and the name; an invalid email throws.
- `search` as a customer throws `AccessDeniedException`; as an agent `search("smi")` returns Jo Smith.

## P7-09

**Issue Title:** Implement FlightManagementController (add, update, remove flights)
**Milestone/Phase:** Phase 7: Business Logic
**Labels:** implementation
**Estimate:** 4h
**Depends on:** P7-01, P6-04
**Kind:** code
**Rubric check:** FR-7 / section 3.4 - administrator-only add, update and remove of flights

### Context & Objective
The administrator controls what customers can book, so validation matters: no duplicate flight numbers, no aircraft in two places at once, no breaking flights that people already booked.

### Implementation Guide
1. `business/control/FlightDetails.java`: record `(String flightNumber, long routeId, String airlineCode, long aircraftId, LocalDateTime departure, LocalDateTime arrival, BigDecimal basePrice)`.
2. `FlightManagementController(FlightDao, SeatDao, RouteDao, AirlineDao, AircraftDao, ReservationDao, Clock)`; every method starts with `Authorizer.require(actor, Role.ADMIN)`.
3. `Flight addFlight(User actor, FlightDetails d)`: flight number unique; route, airline and aircraft exist; arrival after departure; departure in the future; aircraft has no overlapping flight (`findByAircraft` plus `Flight.overlaps`). Saves the flight, then `SeatLayout.generate(aircraft)` into `createSeats`.
4. `Flight updateFlight(User actor, long flightId, FlightDetails d)`: same checks (ignoring itself); changing route or aircraft is blocked when active reservations exist.
5. `void removeFlight(User actor, long flightId)`: blocked if there are active reservations; if the flight has no reservation rows at all it is deleted, otherwise it is archived (hidden from search and schedules).
6. Archiving uses `FlightDao.archive` and the `archived` column from P6-01 and P6-04.

### Testing Strategy
- Admin adds `AC123` on `YYC`-`YVR` with a 3x4 aircraft: flight saved and 12 seats created.
- A customer calling `addFlight` throws `AccessDeniedException`.
- Second flight with number `AC123` throws containing `Flight number already exists`.
- Arrival equal to or before departure throws; departure in the past throws containing `must be in the future`.
- The same aircraft on 08:00-10:00 and another flight 09:30-11:00 throws containing `Aircraft is already assigned`; 10:00-12:00 is accepted.
- Unknown route id throws containing `Route not found`.
- `removeFlight` on a flight with a `CONFIRMED` reservation throws containing `active reservations`; with only a `CANCELLED` reservation it is archived and no longer appears in a search; with none it is deleted.
- `updateFlight` changing base price succeeds; changing the aircraft with an active reservation throws.

## P7-10

**Issue Title:** Implement RouteController, AircraftController and ScheduleController
**Milestone/Phase:** Phase 7: Business Logic
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P7-09
**Kind:** code
**Rubric check:** Section 2.1 (administrator manages routes, aircraft and schedules; agent views schedules) and FR-8, FR-10

### Context & Objective
Completes the administrator and agent controllers listed in the use-case table.

### Implementation Guide
1. `RouteController(RouteDao, AirportDao)`: `Route addRoute(User actor, String originCode, String destinationCode)` (admin only; both airports must exist; the pair must not already exist) and `List<Route> list()`.
2. `AircraftController(AircraftDao)`: `Aircraft addAircraft(User actor, String model, int rows, int seatsPerRow)` (admin only) and `List<Aircraft> list()`.
3. `ScheduleController(FlightDao, Clock)`: `List<Flight> schedule(User actor, LocalDate from, LocalDate to)` (agent or admin) ordered by departure and without archived flights; `Flight reschedule(User actor, long flightId, LocalDateTime departure, LocalDateTime arrival)` (admin only) with the same aircraft-overlap rule and not allowed after departure.

### Testing Strategy
- `addRoute(admin, "YYC", "YVR")` succeeds; again throws containing `Route already exists`; `"YYC","XXX"` throws containing `Airport not found`.
- A customer calling `addRoute` or `addAircraft` throws `AccessDeniedException`.
- `addAircraft(admin, "A320", 30, 6)` gives `seatCount()` of 180; rows `0` throws.
- `schedule(agent, 2026-12-01, 2026-12-03)` returns flights in that range ordered by departure; a customer calling it throws.
- `reschedule` to a time that overlaps another flight on the same aircraft throws; moving it to a free slot works; rescheduling a departed flight throws.
