# Phase 5 and Phase 6 issues (domain model and data layer)

Paths are relative to `src/main/java/ca/ucalgary/chinookair/` unless they start with `src/`.

## P5-01

**Issue Title:** Implement the User hierarchy, Role, UserFactory and PasswordHasher
**Milestone/Phase:** Phase 5: Domain Model
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P4-05
**Kind:** code
**Rubric check:** FR-13 (roles) and the Factory pattern from the class diagram; security NFR (hashed passwords)

### Context & Objective
The three roles are the base of the whole application: they decide what every screen and controller allows. Building them first as plain objects (no GUI, no database) means everything later can use them and tests need no setup.

### Implementation Guide
1. `business/entity/Role.java`: enum `CUSTOMER`, `AGENT`, `ADMIN`.
2. `business/entity/User.java`: abstract, immutable fields `id` (Long, null before saving), `username` (trimmed, lowercased), `passwordHash`, `role()` abstract.
3. `Customer` adds `name`, `email`, `phone` (phone optional). `FlightAgent` and `Administrator` have no extra fields yet.
4. Validation in constructors: username 3 to 30 characters of letters, digits, `.` and `_`; name not blank; email must match `^[^@\s]+@[^@\s]+\.[^@\s]+$`.
5. `business/entity/UserFactory.java` (Factory Method): `create(Role, ...)` returns the right subclass.
6. `util/PasswordHasher.java`: PBKDF2WithHmacSHA256 from the JDK, 120000 iterations, 16-byte random salt, 256-bit key, stored as `pbkdf2$iterations$saltBase64$hashBase64`. `hash(String)` and `verify(String, String)`. Never throw on a malformed stored hash, return false.
7. SOLID: `User` knows nothing about hashing details, only stores the string (single responsibility).

### Testing Strategy
Create `src/test/java/ca/ucalgary/chinookair/business/entity/UserTest.java` and `util/PasswordHasherTest.java`.
- `hash("admin123")` starts with `pbkdf2$` and does not contain `admin123`.
- `verify("admin123", hash("admin123"))` is true; `verify("Admin123", ...)` is false; `verify("admin123", "garbage")` is false (no exception).
- Hashing the same password twice gives two different strings (salt).
- `new Customer(null, " JSmith ", hash, "Jo Smith", "jo@x.ca", null)` has username `jsmith`.
- Email `a@b` throws `IllegalArgumentException`; `a@b.ca` is accepted.
- Username `ab` (too short) and `a b` throw `IllegalArgumentException`.
- `UserFactory.create(Role.ADMIN, ...)` returns an `Administrator` whose `role()` is `ADMIN`; each of the 3 roles maps to its subclass.

## P5-02

**Issue Title:** Implement Airline, Airport, Route, Aircraft and Promotion entities
**Milestone/Phase:** Phase 5: Domain Model
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P5-01
**Kind:** code
**Rubric check:** FR-1 (airline search), FR-8 (routes and aircraft) and FR-12 (promotion) have their entities

### Context & Objective
These are the reference data of the system. Making them validated immutable records keeps bad data out of the database and makes the DAOs simple.

### Implementation Guide
1. In `business/entity/` create Java records with compact-constructor validation:
   - `Airline(String code, String name)`: code is 2 letters, stored uppercase.
   - `Airport(String code, String city)`: code is 3 letters, stored uppercase.
   - `Route(Long id, Airport origin, Airport destination)`: origin and destination must differ.
   - `Aircraft(Long id, String model, int rows, int seatsPerRow)`: rows 1 to 60, seats per row 1 to 6, with `seatCount()`.
   - `Promotion(Long id, String title, String description)`: title and description not blank.
2. Throw `IllegalArgumentException` with a clear message, for example `"Airport code must be 3 letters"`.
3. Records give equals and hashCode for free, which later tests rely on.

### Testing Strategy
- `new Airport("yyc", "Calgary").code()` is `"YYC"`.
- `new Airport("YY", "Calgary")` throws `IllegalArgumentException` with message containing `3 letters`.
- `new Airline("A", "Air")` throws; `new Airline("ac", "Air Canada").code()` is `"AC"`.
- `Route` with the same airport on both ends throws `IllegalArgumentException`.
- `new Aircraft(null, "A320", 30, 6).seatCount()` is `180`; rows `0` throws; seatsPerRow `7` throws.
- `new Promotion(null, " ", "text")` throws.

## P5-03

**Issue Title:** Implement Flight, Seat, SeatLayout and PricingPolicy
**Milestone/Phase:** Phase 5: Domain Model
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P5-02
**Kind:** code
**Rubric check:** FR-2 (seats and prices) and FR-14 (seat selection)

### Context & Objective
Flight and Seat carry the rules customers see every day: what a flight costs, which seats exist, whether it has left. Keeping them as plain logic means they are tested without a database.

### Implementation Guide
1. `business/entity/SeatClass.java`: enum `ECONOMY`, `BUSINESS`.
2. `business/entity/Seat.java`: record `Seat(String number, SeatClass seatClass, boolean available)`.
3. `business/entity/Flight.java`: fields `id`, `flightNumber` (regex `^[A-Z]{2}\d{1,4}$`, stored uppercase), `route`, `airline`, `aircraft`, `departure`, `arrival` (`LocalDateTime`), `basePrice` (`BigDecimal`, greater than 0, scale 2). Methods `hasDeparted(Clock)` and `overlaps(LocalDateTime start, LocalDateTime end)`.
4. `business/entity/SeatLayout.java`: `generate(Aircraft)` returns every seat named like `12A` (row, then letter A to F). The first two rows (or all rows if fewer) are `BUSINESS`.
5. `business/entity/PricingPolicy.java`: `priceFor(BigDecimal base, SeatClass)`: economy is the base, business is base times 1.5, rounded `HALF_UP` to 2 decimals.
6. Pass a `Clock` in instead of calling `now()` so tests are repeatable (dependency inversion).

### Testing Strategy
- Flight with arrival before departure throws `IllegalArgumentException`; flight number `ac123` is stored as `AC123`; `A1` and `AC12345` throw.
- Base price `0` throws.
- `hasDeparted` with a fixed clock: departure `2026-12-01T08:00`, clock `2026-12-01T07:59` is false, `08:00` is true.
- `overlaps`: a flight 08:00-10:00 overlaps 09:30-11:00 (true) and does not overlap 10:00-12:00 (false, boundaries touch).
- `SeatLayout.generate` for a 3 rows by 4 seats aircraft gives 12 seats `1A` to `3D`, 8 `BUSINESS` (rows 1 and 2) and 4 `ECONOMY`, all available.
- `PricingPolicy.priceFor(new BigDecimal("200.00"), BUSINESS)` is `300.00`; `199.99` business is `299.99` (299.985 rounds half up); economy `199.99` stays `199.99`.

## P5-04

**Issue Title:** Implement Reservation with the State pattern
**Milestone/Phase:** Phase 5: Domain Model
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P5-03
**Kind:** code
**Rubric check:** Section 7.1 E (reservation state diagram) and FR-3/FR-4; State pattern from the class diagram

### Context & Objective
The reservation lifecycle is where illegal actions are easiest to introduce, for example paying for a cancelled reservation. Encoding the state diagram as classes makes illegal transitions impossible.

### Implementation Guide
1. `business/entity/ReservationStatus.java`: `CREATED`, `PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`, `COMPLETED`.
2. `business/entity/ReservationState.java` (package-private interface): `assignSeat`, `confirmPayment`, `changeFlight`, `cancel`, `complete`. One package-private class per state implements it; unsupported actions throw `IllegalStateException("Cannot <action> a <status> reservation")`.
3. `business/entity/Reservation.java`: fields `id`, `confirmationNumber`, `customer`, `flight`, `seatNo`, `price`, `createdAt`, and the current state. The public methods delegate to the state object. Methods: `assignSeat(String seatNo, BigDecimal price)`, `confirmPayment()`, `changeFlight(Flight f, String seatNo, BigDecimal newPrice)`, `cancel()`, `complete(Clock)`.
4. Rules from the state diagram: `changeFlight` in `CONFIRMED` stays `CONFIRMED` when the new price equals the old one, otherwise goes to `PENDING_PAYMENT`.
5. `SeatUnavailableException` (runtime) in `business/entity/` for later use.
6. SOLID: open for new states without editing existing ones (open/closed).

### Testing Strategy
Parameterized test in `ReservationTest`, one row per transition:
- `CREATED` + `assignSeat("12A", 200.00)` gives `PENDING_PAYMENT`.
- `PENDING_PAYMENT` + `confirmPayment()` gives `CONFIRMED`.
- `CONFIRMED` + `cancel()` gives `CANCELLED`; `CREATED` + `cancel()` and `PENDING_PAYMENT` + `cancel()` also give `CANCELLED`.
- `CONFIRMED` + `changeFlight(f2, "3C", 200.00)` when the price is 200.00 stays `CONFIRMED`; with price 250.00 goes to `PENDING_PAYMENT`.
- `CANCELLED` + `confirmPayment()` throws `IllegalStateException`; `COMPLETED` + `cancel()` throws; `PENDING_PAYMENT` + `changeFlight(...)` throws.
- `complete(clock)` before departure throws `IllegalStateException`; after departure gives `COMPLETED`.
- After `assignSeat`, `seatNo()` is `12A` and `price()` is `200.00`.

## P5-05

**Issue Title:** Implement Payment, PaymentDetails and the PaymentMethod strategies
**Milestone/Phase:** Phase 5: Domain Model
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P5-03
**Kind:** code
**Rubric check:** FR-9 (simulated payment, payment records), payment state diagram, Strategy pattern

### Context & Objective
Payments are simulated, but the rules for approving, declining and storing them still need to be correct and testable, and each payment method is a swappable strategy.

### Implementation Guide
1. `business/entity/PaymentStatus.java`: `PENDING`, `APPROVED`, `DECLINED`, `REFUNDED`. `PaymentMethodType`: `CREDIT`, `DEBIT`.
2. `business/entity/PaymentDetails.java`: record with `cardholderName`, `cardNumber`, `expiry` (`MM/yy`). `validate(Clock)` strips spaces, requires exactly 16 digits, expiry not before the current month, name not blank.
3. `business/entity/PaymentMethod.java` (Strategy interface): `AuthorizationResult authorize(PaymentDetails, BigDecimal amount)`. Record `AuthorizationResult(boolean approved, String message)`.
4. `CreditCardPayment` declines a card whose last four digits are `0000`. `DebitCardPayment` does the same and also declines amounts over 5000.00.
5. `business/entity/PaymentMethods.java`: factory `forType(PaymentMethodType)`.
6. `business/entity/Payment.java`: `id`, `reservationId`, `amount`, `type`, `status`, `maskedCard` (`**** **** **** 1111`), `createdAt`, `message`. Methods `approve()`, `decline(String reason)`, `refund()` with legal transitions only.

### Testing Strategy
- Credit card `4111 1111 1111 1111`, amount 250.00 gives approved; masked card is `**** **** **** 1111`.
- `4111111111110000` gives declined with message `Card declined`.
- 15 digits throws `IllegalArgumentException` containing `16 digits`; expiry `01/20` throws containing `expired`; blank name throws containing `name`. These exact messages are what the payment screen shows later.
- Debit with amount 6000.00 is declined; debit with 5000.00 is approved.
- `Payment`: `PENDING` + `approve()` gives `APPROVED`; `APPROVED` + `refund()` gives `REFUNDED`; `DECLINED` + `refund()` throws `IllegalStateException`; `PENDING` + `refund()` throws.
- The full card number never appears in `Payment.toString()`.

## P6-01

**Issue Title:** Write the SQLite schema with constraints that prevent double booking
**Milestone/Phase:** Phase 6: Data Layer
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P5-05
**Kind:** code
**Rubric check:** Section 3.6 (SQLite storage) and the reliability NFR (no double booking)

### Context & Objective
The database is the last line of defence against bad data. Putting the rules into the schema (keys, checks, a partial unique index) means the application cannot corrupt data even if a bug slips through.

### Implementation Guide
1. Create `src/main/resources/db/schema.sql`.
2. Tables: `airline(code PK, name)`, `airport(code PK, city)`, `aircraft(id, model, rows, seats_per_row)`, `route(id, origin_code, destination_code, UNIQUE pair, CHECK origin <> destination)`, `flight(id, flight_number UNIQUE, route_id, airline_code, aircraft_id, departure, arrival, base_price_cents CHECK > 0, archived INTEGER NOT NULL DEFAULT 0 CHECK IN (0,1))`, `flight_seat(flight_id, seat_no, seat_class, PK both)`, `app_user(id, username UNIQUE COLLATE NOCASE, password_hash, role CHECK)`, `customer(user_id PK, name, email UNIQUE COLLATE NOCASE, phone)`, `reservation(id, confirmation_number UNIQUE, customer_id, flight_id, seat_no NULL, price_cents NULL, status CHECK, created_at)`, `payment(id, reservation_id, amount_cents CHECK > 0, method CHECK, status CHECK, masked_card, message, created_at)`, `promotion(id, title, description)`, `app_state(key PK, value)`.
3. Money is stored as integer cents, never floating point. Dates as ISO-8601 text.
4. Foreign keys everywhere, including `reservation(flight_id, seat_no)` referencing `flight_seat`.
5. Partial unique index: `CREATE UNIQUE INDEX one_active_reservation_per_seat ON reservation(flight_id, seat_no) WHERE seat_no IS NOT NULL AND status IN ('PENDING_PAYMENT','CONFIRMED');`
6. Seat availability is derived from active reservations, not stored (single source of truth).

### Testing Strategy
`SchemaTest` runs the script on `jdbc:sqlite::memory:` with `PRAGMA foreign_keys=ON`:
- Every table above exists in `sqlite_master`.
- Insert two reservations for the same flight and seat `1A`, both `CONFIRMED`: the second fails with a constraint error.
- Insert a `CANCELLED` reservation for `1A` after a cancelled one: succeeds.
- Reservation with a flight id that does not exist fails (foreign key).
- Reservation seat `99Z` that is not in `flight_seat` fails.
- Status `BOGUS` is rejected by the CHECK.
- Two flights with number `AC123` fail on UNIQUE.

## P6-02

**Issue Title:** Implement the DatabaseConnection singleton, initializer and test helper
**Milestone/Phase:** Phase 6: Data Layer
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P6-01
**Kind:** code
**Rubric check:** Section 3.6 and the Singleton pattern; portability (database file created on first run)

### Context & Objective
One place owns the connection and creates the database file on first launch, so the app works from any folder on any machine and tests can run against memory.

### Implementation Guide
1. `data/DatabaseConnection.java`: Singleton with `getInstance()`, `connection()`, `close()`. Default file `~/.chinookair/chinookair.db`, overridable with the system property `chinookair.db`. Create the directory if missing.
2. `data/DatabaseInitializer.java`: turn on `PRAGMA foreign_keys=ON`; if `PRAGMA user_version` is `0` run `/db/schema.sql` and set `user_version` to `1`.
3. `data/DataAccessException.java`: unchecked wrapper around `SQLException`.
4. Private constructor takes a JDBC URL, and a package-private static `forUrl(String)` lets tests build instances without touching the singleton.
5. Test helper `src/test/java/ca/ucalgary/chinookair/data/TestDatabase.java` with `inMemory()` returning an initialized connection.
6. DAOs will receive a `Connection` in their constructor (dependency inversion), so only the composition root calls `getInstance()`.

### Testing Strategy
- `getInstance()` called twice returns the same object.
- With `@TempDir` and `-Dchinookair.db=<tmp>/x.db`: the directory and file are created and `airline` table exists.
- Insert a row, close and reopen: the row is still there and the schema is not applied twice (`user_version` is `1`).
- `PRAGMA foreign_keys` returns `1`.
- `TestDatabase.inMemory()` returns a connection with all tables.

## P6-03

**Issue Title:** Implement the reference-data DAOs (Airline, Airport, Aircraft, Route)
**Milestone/Phase:** Phase 6: Data Layer
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P6-02, P5-02
**Kind:** code
**Rubric check:** Section 5 (data layer) and the DAO pattern; FR-8 storage

### Context & Objective
First DAOs, built small. They also set the pattern every later DAO follows: a public interface, a package-private `Sqlite...Dao` class, and a shared helper for the JDBC boilerplate.

### Implementation Guide
1. Public interfaces in `data/`: `AirlineDao` (`findAll`, `findByCode`, `save`), `AirportDao` (same), `AircraftDao` (`findAll`, `findById`, `save`), `RouteDao` (`findAll`, `findById`, `findByAirports`, `save`).
2. Package-private implementations `SqliteAirlineDao`, `SqliteAirportDao`, `SqliteAircraftDao`, `SqliteRouteDao`, each taking a `Connection`.
3. Package-private `JdbcSupport` helper for prepared statements and SQLException wrapping, so no DAO repeats it (DRY).
4. `save` returns the entity with its generated id when it has one.
5. Always use `PreparedStatement` with parameters, never string concatenation.
6. Test helper `TestFixtures` in `src/test/java/.../data/` with `airline("AC")`, `airport("YYC")`, `aircraft()`, `route("YYC","YVR")` shortcuts.

### Testing Strategy
Each DAO test uses `TestDatabase.inMemory()`.
- Save `Airline("AC","Air Canada")`; `findByCode("AC")` returns an equal record; `findByCode("ZZ")` is empty.
- Saving `AC` twice throws `DataAccessException`.
- `AirportDao.findAll()` after saving `YVR` then `YYC` returns `YVR, YYC` sorted by code.
- Save an aircraft: returned id is greater than 0, `findById` returns the same model and size.
- Save route `YYC` to `YVR`: `findByAirports("YYC","YVR")` is present; `findByAirports("YVR","YYC")` is empty.
- Route with an airport code that is not saved throws `DataAccessException`.

## P6-04

**Issue Title:** Implement FlightDao and SeatDao with the flight search query
**Milestone/Phase:** Phase 6: Data Layer
**Labels:** implementation
**Estimate:** 4h
**Depends on:** P6-03, P5-03
**Kind:** code
**Rubric check:** FR-1 (search by origin, destination, date, airline), FR-2 (seats left, prices) and FR-7 storage

### Context & Objective
Flight search is the most-used query in the system and must be exactly right on date boundaries and optional filters.

### Implementation Guide
1. `FlightDao`: `save`, `update`, `delete`, `archive(long flightId)`, `findById`, `existsByFlightNumber`, `search(String origin, String destination, LocalDate date, String airlineCodeOrNull)`, `findDepartingBetween(LocalDateTime from, LocalDateTime to)`, `findByAircraft(long aircraftId)`.
2. `search` matches flights whose departure is on that calendar day (`>= date 00:00` and `< next day 00:00`), ordered by departure, with the airline filter only when not null. Archived flights are never returned by `search` or `findDepartingBetween`.
3. `SeatDao`: `createSeats(flightId, List<Seat>)`, `findByFlight(flightId)` returning seats with `available` computed by left-joining active reservations (`PENDING_PAYMENT` or `CONFIRMED`), `countAvailable(flightId)`.
4. Rebuild a full `Flight` (route, airports, airline, aircraft) from joins in one query.
5. Dates are compared as ISO strings, which sort correctly.

### Testing Strategy
Fixture: three `YYC` to `YVR` flights on `2026-12-01` at 08:00 (AC), 12:00 (WS) and 18:00 (AC); one `YYC` to `YEG` on the same day; one `YYC` to `YVR` on `2026-12-02`.
- `search("YYC","YVR","2026-12-01", null)` returns 3 flights in order 08:00, 12:00, 18:00.
- With airline `"AC"` returns 2; with `"PD"` returns 0.
- `search("YYC","YVR","2026-12-03", null)` is empty.
- A flight departing `2026-12-01T23:59` is found on `12-01` and not on `12-02`; one at `00:00` on `12-02` is found on `12-02` only.
- `createSeats` for a 3x4 aircraft then `countAvailable` is `12`; after inserting a `CONFIRMED` reservation on `2A` it is `11` and `findByFlight` shows `2A` unavailable; after setting it `CANCELLED` it is `12` again.
- `delete` on a flight that has a reservation throws `DataAccessException`.
- `archive(flightId)` hides the flight: it no longer appears in `search` or `findDepartingBetween`, but `findById` still returns it and its reservations are untouched.
- `existsByFlightNumber("AC123")` is true only after saving it.

## P6-05

**Issue Title:** Implement UserDao and CustomerDao with transactional customer creation
**Milestone/Phase:** Phase 6: Data Layer
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P6-03, P5-01
**Kind:** code
**Rubric check:** FR-6 (customer profiles) and FR-13 (login with roles)

### Context & Objective
Creating a customer touches two tables, so it must be all-or-nothing. Case-insensitive usernames and emails prevent duplicate accounts that differ only in capitals.

### Implementation Guide
1. `UserDao`: `findByUsername` (case-insensitive), `findById`, `save(User)`. Returns the right subclass by role using `UserFactory`.
2. `CustomerDao`: `save`, `update`, `findById`, `findByEmail`, `search(String text)` matching name or email, case-insensitive.
3. `SqliteCustomerDao.save` inserts `app_user` then `customer` inside one transaction (`setAutoCommit(false)`, commit, rollback on error, restore autocommit).
4. Duplicate username or email throws `DataAccessException` with a clear message and leaves no partial rows.

### Testing Strategy
- Save customer `jsmith` / `j@x.ca`; `findByUsername("JSMITH")` returns a `Customer` with the same email.
- Saving a second customer with email `J@X.CA` throws `DataAccessException`, and the `app_user` row count did not change (rollback).
- Saving a second customer with username `jsmith` throws.
- `search("smi")` finds Jo Smith; `search("zzz")` is empty; `search("J@X")` finds by email.
- `update` phone to `403-555-0100`, read it back.
- Save an admin through `UserDao`: `findByUsername("admin")` returns an `Administrator` with role `ADMIN`.

## P6-06

**Issue Title:** Implement ReservationDao with double-booking protection
**Milestone/Phase:** Phase 6: Data Layer
**Labels:** implementation
**Estimate:** 4h
**Depends on:** P6-04, P6-05, P5-04
**Kind:** code
**Rubric check:** FR-3, FR-4, FR-10 (reservation history) and the reliability NFR

### Context & Objective
Reservations are the core record of the system and the place where two people could try to take the same seat.

### Implementation Guide
1. `ReservationDao`: `save`, `update`, `findById`, `findByConfirmationNumber`, `findByCustomer(customerId)` newest first, `findByFlight(flightId)`, `countActiveByFlight(flightId)`.
2. `SqliteReservationDao` takes `CustomerDao` and `FlightDao` to rebuild full objects (single place for hydration).
3. A unique-index violation on the active-seat index is caught and rethrown as `SeatUnavailableException`; any other violation stays a `DataAccessException`.
4. Rebuilding the state: the status string maps back to the right state object through a static factory on `Reservation`.

### Testing Strategy
- Save a reservation on seat `1A` as `PENDING_PAYMENT`: id is assigned and `findById` returns the same seat, price and status.
- A second customer reserving `1A` on the same flight throws `SeatUnavailableException`.
- After the first one is updated to `CANCELLED`, reserving `1A` succeeds.
- `findByCustomer` returns the customer's two reservations newest first.
- `findByConfirmationNumber("CA12AB34")` returns the reservation; duplicate confirmation number throws `DataAccessException`.
- `countActiveByFlight` ignores `CANCELLED` and `COMPLETED` ones.

## P6-07

**Issue Title:** Implement PaymentDao
**Milestone/Phase:** Phase 6: Data Layer
**Labels:** implementation
**Estimate:** 2h
**Depends on:** P6-06, P5-05
**Kind:** code
**Rubric check:** FR-9 (record payment information and keep payment records)

### Context & Objective
Payment records are shown to agents and used for refunds, so amounts must round-trip exactly.

### Implementation Guide
1. `PaymentDao`: `save`, `update`, `findById`, `findByReservation(reservationId)`, `findByCustomer(customerId)` newest first.
2. Store `amount_cents` as `amount.movePointRight(2)` and read back with `BigDecimal.valueOf(cents, 2)` to avoid rounding errors.
3. Only the masked card string is ever written.

### Testing Strategy
- Save a payment of `249.99`; read back equals `249.99` exactly.
- Save, then `update` status from `PENDING` to `APPROVED`: persisted.
- `findByReservation` returns the declined attempt and the approved one in order.
- `findByCustomer` returns payments across two reservations for the same customer only.
- Inspecting the row shows `masked_card` like `**** **** **** 1111` and no 16-digit value anywhere.

## P6-08

**Issue Title:** Implement PromotionDao and AppStateDao
**Milestone/Phase:** Phase 6: Data Layer
**Labels:** implementation
**Estimate:** 2h
**Depends on:** P6-02, P5-02
**Kind:** code
**Rubric check:** FR-12 (monthly promotion remembered so it is shown once a month)

### Context & Objective
The "shown only once that month" rule needs a small piece of persistent state. A generic key/value table keeps it simple.

### Implementation Guide
1. `PromotionDao`: `findAll`, `findById`, `save`.
2. `AppStateDao`: `Optional<String> get(String key)` and `void put(String key, String value)` as an upsert (`INSERT ... ON CONFLICT(key) DO UPDATE`).
3. Both are public interfaces with package-private SQLite implementations.

### Testing Strategy
- `get("last_promo_month")` on an empty database is empty.
- `put("last_promo_month","2026-12")` then `get` returns `2026-12`; `put` again with `2027-01` overwrites (still one row).
- `PromotionDao.findAll()` returns the saved promotions in insertion order; `findById(999)` is empty.

## P6-09

**Issue Title:** Add DaoFactory and the ArchUnit layer-rule tests
**Milestone/Phase:** Phase 6: Data Layer
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P6-08
**Kind:** code
**Rubric check:** Assessment 1 and 2 - separation of concerns and low coupling, enforced by tests

### Context & Objective
The assessment is mostly about architecture. This issue turns "views never touch SQL" from a promise into a test that fails the build, and gives the data layer one public entry point.

### Implementation Guide
1. `data/DaoFactory.java` (public): `create(Connection)` and methods returning the DAO interfaces (`flightDao()`, `reservationDao()`, ...). Implementations stay package-private.
2. Add `com.tngtech.archunit:archunit-junit5:1.3.0` (test scope) to `pom.xml`.
3. `src/test/java/ca/ucalgary/chinookair/ArchitectureTest.java` with rules:
   - `presentation` must not depend on `data`.
   - `business.entity` must not depend on `javax.swing`, `java.sql`, `presentation`, `data` or `business.control`.
   - `business.control` must not depend on `javax.swing`, `java.sql`, `presentation` or classes named `Sqlite*`.
   - No package cycles.
   - `Sqlite*Dao` classes are not public; `*Dao` interfaces are public.

### Testing Strategy
- All five rules pass on the current code.
- Prove the test works: temporarily add `import javax.swing.JLabel;` to an entity class, run `mvn test`, see `ArchitectureTest` fail, then revert.
- `DaoFactory.create(TestDatabase.inMemory()).flightDao()` returns a working DAO.

## P6-10

**Issue Title:** Implement the idempotent DatabaseSeeder with demo data
**Milestone/Phase:** Phase 6: Data Layer
**Labels:** implementation
**Estimate:** 3h
**Depends on:** P6-09, P5-01
**Kind:** code
**Rubric check:** Section 3.6 and section 7.2 - the app works on first launch with realistic data

### Context & Objective
A marker opens the jar for the first time and expects flights to search, users to log in as, and a promotion to show.

### Implementation Guide
1. `data/DatabaseSeeder.java` (public): `seedIfEmpty(Clock)`; does nothing when the `airline` table already has rows.
2. Seed: airlines AC, WS, PD; airports YYC, YVR, YYZ, YEG, YUL, YOW; three aircraft (for example 30x6, 28x6, 20x4); routes in both directions for five airport pairs; at least 40 flights over the next 60 days **relative to the clock**, each with its seats; 12 promotions; users `customer1`, `agent1` and `admin` with known demo passwords, hashed by `PasswordHasher`.
3. Make sure no aircraft is on two overlapping flights.
4. Document the demo accounts in the README.

### Testing Strategy
- After `seedIfEmpty`: 3 airlines, 6 airports, at least 40 flights, 12 promotions, 3 users.
- Calling it a second time changes no counts.
- Every flight departs after the clock used for seeding.
- For every flight the number of `flight_seat` rows equals `aircraft.seatCount()`.
- No two flights on the same aircraft overlap (loop over all pairs).
- `admin` logs in with its documented password (`PasswordHasher.verify`).
