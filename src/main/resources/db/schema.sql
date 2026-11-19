-- ChinookAir database schema (SQLite).
-- Money is whole cents, never floating point. Dates and times are ISO-8601 text,
-- like 2026-12-01T08:00:00, which also sorts correctly as text.
-- The rules live here as keys and checks, so bad data is refused even if a bug slips through.

CREATE TABLE airline (
    code TEXT PRIMARY KEY CHECK (length(code) = 2),
    name TEXT NOT NULL CHECK (length(trim(name)) > 0)
);

CREATE TABLE airport (
    code TEXT PRIMARY KEY CHECK (length(code) = 3),
    city TEXT NOT NULL CHECK (length(trim(city)) > 0)
);

CREATE TABLE aircraft (
    id INTEGER PRIMARY KEY,
    model TEXT NOT NULL CHECK (length(trim(model)) > 0),
    rows INTEGER NOT NULL CHECK (rows BETWEEN 1 AND 60),
    seats_per_row INTEGER NOT NULL CHECK (seats_per_row BETWEEN 1 AND 6)
);

CREATE TABLE route (
    id INTEGER PRIMARY KEY,
    origin_code TEXT NOT NULL REFERENCES airport (code),
    destination_code TEXT NOT NULL REFERENCES airport (code),
    UNIQUE (origin_code, destination_code),
    CHECK (origin_code <> destination_code)
);

CREATE TABLE flight (
    id INTEGER PRIMARY KEY,
    flight_number TEXT NOT NULL UNIQUE,
    route_id INTEGER NOT NULL REFERENCES route (id),
    airline_code TEXT NOT NULL REFERENCES airline (code),
    aircraft_id INTEGER NOT NULL REFERENCES aircraft (id),
    departure TEXT NOT NULL,
    arrival TEXT NOT NULL,
    base_price_cents INTEGER NOT NULL CHECK (base_price_cents > 0),
    archived INTEGER NOT NULL DEFAULT 0 CHECK (archived IN (0, 1)),
    CHECK (arrival > departure)
);

-- The seats of a flight. Whether a seat is free is not stored here: it follows
-- from the active reservations, so there is only one place that knows.
CREATE TABLE flight_seat (
    flight_id INTEGER NOT NULL REFERENCES flight (id) ON DELETE CASCADE,
    seat_no TEXT NOT NULL,
    seat_class TEXT NOT NULL CHECK (seat_class IN ('ECONOMY', 'BUSINESS')),
    PRIMARY KEY (flight_id, seat_no)
);

CREATE TABLE app_user (
    id INTEGER PRIMARY KEY,
    username TEXT NOT NULL UNIQUE COLLATE NOCASE,
    password_hash TEXT NOT NULL CHECK (length(password_hash) > 0),
    role TEXT NOT NULL CHECK (role IN ('CUSTOMER', 'AGENT', 'ADMIN'))
);

CREATE TABLE customer (
    user_id INTEGER PRIMARY KEY REFERENCES app_user (id) ON DELETE CASCADE,
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    email TEXT NOT NULL UNIQUE COLLATE NOCASE,
    phone TEXT
);

CREATE TABLE reservation (
    id INTEGER PRIMARY KEY,
    confirmation_number TEXT NOT NULL UNIQUE,
    customer_id INTEGER NOT NULL REFERENCES customer (user_id),
    flight_id INTEGER NOT NULL REFERENCES flight (id),
    seat_no TEXT,
    price_cents INTEGER CHECK (price_cents IS NULL OR price_cents > 0),
    status TEXT NOT NULL CHECK (status IN ('CREATED', 'PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),
    created_at TEXT NOT NULL,
    -- a seat and its price come together
    CHECK ((seat_no IS NULL) = (price_cents IS NULL)),
    FOREIGN KEY (flight_id, seat_no) REFERENCES flight_seat (flight_id, seat_no)
);

-- The last line of defence against double booking: one active reservation per seat.
CREATE UNIQUE INDEX one_active_reservation_per_seat
    ON reservation (flight_id, seat_no)
    WHERE seat_no IS NOT NULL AND status IN ('PENDING_PAYMENT', 'CONFIRMED');

CREATE TABLE payment (
    id INTEGER PRIMARY KEY,
    reservation_id INTEGER NOT NULL REFERENCES reservation (id),
    amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
    method TEXT NOT NULL CHECK (method IN ('CREDIT', 'DEBIT')),
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'DECLINED', 'REFUNDED')),
    -- only the masked card is ever stored, like **** **** **** 1111
    masked_card TEXT NOT NULL CHECK (masked_card GLOB '[*][*][*][*] [*][*][*][*] [*][*][*][*] [0-9][0-9][0-9][0-9]'),
    message TEXT,
    created_at TEXT NOT NULL
);

CREATE TABLE promotion (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL CHECK (length(trim(title)) > 0),
    description TEXT NOT NULL CHECK (length(trim(description)) > 0)
);

CREATE TABLE app_state (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

CREATE INDEX reservation_by_customer ON reservation (customer_id, created_at);
CREATE INDEX reservation_by_flight ON reservation (flight_id);
CREATE INDEX payment_by_reservation ON payment (reservation_id);
CREATE INDEX flight_by_departure ON flight (departure);
