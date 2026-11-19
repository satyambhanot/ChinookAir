package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Seat;
import ca.ucalgary.chinookair.business.entity.SeatClass;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

class SqliteSeatDao implements SeatDao {

    // a seat is taken while a pending or confirmed reservation holds it
    private static final String ACTIVE_RESERVATION = "SELECT 1 FROM reservation r WHERE r.flight_id = fs.flight_id "
            + "AND r.seat_no = fs.seat_no AND r.status IN ('PENDING_PAYMENT', 'CONFIRMED')";

    private final Connection connection;

    SqliteSeatDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void createSeats(long flightId, List<Seat> seats) {
        List<JdbcSupport.Binder> rows = new ArrayList<>();
        for (Seat seat : seats) {
            rows.add(st -> {
                st.setLong(1, flightId);
                st.setString(2, seat.number());
                st.setString(3, seat.seatClass().name());
            });
        }
        JdbcSupport.batch(connection, "INSERT INTO flight_seat (flight_id, seat_no, seat_class) VALUES (?, ?, ?)",
                rows);
    }

    @Override
    public void replaceSeats(long flightId, List<Seat> seats) {
        // the delete and the inserts are one transaction, so a refusal leaves the old seats in place
        JdbcSupport.transaction(connection, () -> {
            JdbcSupport.update(connection, "DELETE FROM flight_seat WHERE flight_id = ?",
                    st -> st.setLong(1, flightId));
            createSeats(flightId, seats);
            return null;
        });
    }

    @Override
    public List<Seat> findByFlight(long flightId) {
        // "1A" is shorter than "10A", so sorting by length and then text gives row order
        return JdbcSupport.query(connection, "SELECT fs.seat_no, fs.seat_class, NOT EXISTS (" + ACTIVE_RESERVATION
                + ") AS available FROM flight_seat fs WHERE fs.flight_id = ? ORDER BY length(fs.seat_no), fs.seat_no",
                st -> st.setLong(1, flightId),
                row -> new Seat(row.getString("seat_no"), SeatClass.valueOf(row.getString("seat_class")),
                        row.getInt("available") == 1));
    }

    @Override
    public int countAvailable(long flightId) {
        return JdbcSupport.queryOne(connection, "SELECT COUNT(*) FROM flight_seat fs WHERE fs.flight_id = ? "
                + "AND NOT EXISTS (" + ACTIVE_RESERVATION + ")", st -> st.setLong(1, flightId),
                row -> row.getInt(1)).orElse(0);
    }
}
