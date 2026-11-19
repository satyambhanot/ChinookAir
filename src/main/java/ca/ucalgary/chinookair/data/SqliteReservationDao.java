package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Flight;
import ca.ucalgary.chinookair.business.entity.Reservation;
import ca.ucalgary.chinookair.business.entity.ReservationStatus;
import ca.ucalgary.chinookair.business.entity.SeatUnavailableException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class SqliteReservationDao implements ReservationDao {

    private static final String SELECT = "SELECT id, confirmation_number, customer_id, flight_id, seat_no, "
            + "price_cents, status, created_at FROM reservation";

    private final Connection connection;
    private final CustomerDao customers;
    private final FlightDao flights;

    SqliteReservationDao(Connection connection, CustomerDao customers, FlightDao flights) {
        this.connection = connection;
        this.customers = customers;
        this.flights = flights;
    }

    /** The columns of one row, before the customer and the flight are loaded. */
    private static final class Row {
        private final long id;
        private final String confirmationNumber;
        private final long customerId;
        private final long flightId;
        private final String seatNo;
        private final BigDecimal price;
        private final ReservationStatus status;
        private final String createdAt;

        Row(ResultSet row) throws SQLException {
            id = row.getLong("id");
            confirmationNumber = row.getString("confirmation_number");
            customerId = row.getLong("customer_id");
            flightId = row.getLong("flight_id");
            seatNo = row.getString("seat_no");
            long cents = row.getLong("price_cents");
            price = row.wasNull() ? null : BigDecimal.valueOf(cents, 2);
            status = ReservationStatus.valueOf(row.getString("status"));
            createdAt = row.getString("created_at");
        }
    }

    // ---- the one place where rows become reservations

    private List<Reservation> load(String sql, JdbcSupport.Binder binder) {
        List<Row> rows = JdbcSupport.query(connection, sql, binder, Row::new);
        // the same customer or flight often comes up many times in one list
        Map<Long, Customer> customerCache = new HashMap<>();
        Map<Long, Flight> flightCache = new HashMap<>();
        List<Reservation> result = new ArrayList<>();
        for (Row row : rows) {
            Customer customer = customerCache.computeIfAbsent(row.customerId, id -> customers.findById(id)
                    .orElseThrow(() -> new DataAccessException("Reservation " + row.id + " has no customer")));
            Flight flight = flightCache.computeIfAbsent(row.flightId, id -> flights.findById(id)
                    .orElseThrow(() -> new DataAccessException("Reservation " + row.id + " has no flight")));
            result.add(Reservation.restore(row.id, row.confirmationNumber, customer, flight, row.seatNo, row.price,
                    DbDates.parse(row.createdAt), row.status));
        }
        return result;
    }

    private static Long cents(BigDecimal price) {
        return price == null ? null : price.movePointRight(2).longValueExact();
    }

    /** A clash on the active-seat index means the seat is taken; anything else is a plain database problem. */
    private static RuntimeException translate(DataAccessException e, Reservation reservation) {
        String cause = e.getCause() == null ? "" : String.valueOf(e.getCause().getMessage());
        if (cause.contains("reservation.flight_id") && cause.contains("reservation.seat_no")) {
            return new SeatUnavailableException("Seat " + reservation.seatNo() + " was just taken", e);
        }
        if (cause.contains("reservation.confirmation_number")) {
            return new DataAccessException("The confirmation number " + reservation.confirmationNumber()
                    + " is already used");
        }
        return e;
    }

    private static void requireSaved(Reservation reservation) {
        if (reservation.customer().id() == null || reservation.flight().id() == null) {
            throw new IllegalArgumentException("Save the customer and the flight before the reservation");
        }
    }

    // ---- writing

    @Override
    public Reservation save(Reservation reservation) {
        if (reservation.id() != null) {
            throw new IllegalArgumentException("This reservation is already saved");
        }
        requireSaved(reservation);
        try {
            long id = JdbcSupport.insert(connection, "INSERT INTO reservation (confirmation_number, customer_id, "
                    + "flight_id, seat_no, price_cents, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)", st -> {
                        st.setString(1, reservation.confirmationNumber());
                        st.setLong(2, reservation.customer().id());
                        st.setLong(3, reservation.flight().id());
                        st.setString(4, reservation.seatNo());
                        st.setObject(5, cents(reservation.price()));
                        st.setString(6, reservation.status().name());
                        st.setString(7, DbDates.format(reservation.createdAt()));
                    });
            reservation.assignId(id);
            return reservation;
        } catch (DataAccessException e) {
            throw translate(e, reservation);
        }
    }

    @Override
    public void update(Reservation reservation) {
        if (reservation.id() == null) {
            throw new IllegalArgumentException("This reservation is not saved yet");
        }
        requireSaved(reservation);
        int changed;
        try {
            changed = JdbcSupport.update(connection, "UPDATE reservation SET flight_id = ?, seat_no = ?, "
                    + "price_cents = ?, status = ? WHERE id = ?", st -> {
                        st.setLong(1, reservation.flight().id());
                        st.setString(2, reservation.seatNo());
                        st.setObject(3, cents(reservation.price()));
                        st.setString(4, reservation.status().name());
                        st.setLong(5, reservation.id());
                    });
        } catch (DataAccessException e) {
            throw translate(e, reservation);
        }
        if (changed != 1) {
            throw new DataAccessException("There is no reservation with id " + reservation.id());
        }
    }

    // ---- reading

    @Override
    public Optional<Reservation> findById(long reservationId) {
        return load(SELECT + " WHERE id = ?", st -> st.setLong(1, reservationId)).stream().findFirst();
    }

    @Override
    public Optional<Reservation> findByConfirmationNumber(String confirmationNumber) {
        if (confirmationNumber == null) {
            return Optional.empty();
        }
        return load(SELECT + " WHERE confirmation_number = ?",
                st -> st.setString(1, confirmationNumber.trim().toUpperCase())).stream().findFirst();
    }

    @Override
    public List<Reservation> findByCustomer(long customerId) {
        return load(SELECT + " WHERE customer_id = ? ORDER BY created_at DESC, id DESC",
                st -> st.setLong(1, customerId));
    }

    @Override
    public List<Reservation> findByFlight(long flightId) {
        return load(SELECT + " WHERE flight_id = ? ORDER BY created_at, id", st -> st.setLong(1, flightId));
    }

    @Override
    public int countActiveByFlight(long flightId) {
        return JdbcSupport.queryOne(connection, "SELECT COUNT(*) FROM reservation WHERE flight_id = ? "
                + "AND status IN ('CREATED', 'PENDING_PAYMENT', 'CONFIRMED')", st -> st.setLong(1, flightId),
                row -> row.getInt(1)).orElse(0);
    }
}
