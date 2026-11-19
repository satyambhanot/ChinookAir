package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Payment;
import ca.ucalgary.chinookair.business.entity.PaymentMethodType;
import ca.ucalgary.chinookair.business.entity.PaymentStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

class SqlitePaymentDao implements PaymentDao {

    private static final String SELECT = "SELECT p.id, p.reservation_id, p.amount_cents, p.method, p.status, "
            + "p.masked_card, p.message, p.created_at FROM payment p";

    private final Connection connection;

    SqlitePaymentDao(Connection connection) {
        this.connection = connection;
    }

    private static Payment read(ResultSet row) throws SQLException {
        return Payment.restore(row.getLong("id"), row.getLong("reservation_id"),
                BigDecimal.valueOf(row.getLong("amount_cents"), 2), PaymentMethodType.valueOf(row.getString("method")),
                PaymentStatus.valueOf(row.getString("status")), row.getString("masked_card"),
                DbDates.parse(row.getString("created_at")), row.getString("message"));
    }

    @Override
    public Payment save(Payment payment) {
        if (payment.id() != null) {
            throw new IllegalArgumentException("This payment is already saved");
        }
        long id = JdbcSupport.insert(connection, "INSERT INTO payment (reservation_id, amount_cents, method, status, "
                + "masked_card, message, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)", st -> {
                    st.setLong(1, payment.reservationId());
                    // whole cents: 249.99 is stored as 24999, so nothing is lost to rounding
                    st.setLong(2, payment.amount().movePointRight(2).longValueExact());
                    st.setString(3, payment.type().name());
                    st.setString(4, payment.status().name());
                    st.setString(5, payment.maskedCard());
                    st.setString(6, payment.message());
                    st.setString(7, DbDates.format(payment.createdAt()));
                });
        payment.assignId(id);
        return payment;
    }

    @Override
    public void update(Payment payment) {
        if (payment.id() == null) {
            throw new IllegalArgumentException("This payment is not saved yet");
        }
        int changed = JdbcSupport.update(connection, "UPDATE payment SET status = ?, message = ? WHERE id = ?", st -> {
            st.setString(1, payment.status().name());
            st.setString(2, payment.message());
            st.setLong(3, payment.id());
        });
        if (changed != 1) {
            throw new DataAccessException("There is no payment with id " + payment.id());
        }
    }

    @Override
    public Optional<Payment> findById(long paymentId) {
        return JdbcSupport.queryOne(connection, SELECT + " WHERE p.id = ?", st -> st.setLong(1, paymentId),
                SqlitePaymentDao::read);
    }

    @Override
    public List<Payment> findByReservation(long reservationId) {
        return JdbcSupport.query(connection, SELECT + " WHERE p.reservation_id = ? ORDER BY p.created_at, p.id",
                st -> st.setLong(1, reservationId), SqlitePaymentDao::read);
    }

    @Override
    public List<Payment> findByCustomer(long customerId) {
        return JdbcSupport.query(connection, SELECT + " JOIN reservation r ON r.id = p.reservation_id "
                + "WHERE r.customer_id = ? ORDER BY p.created_at DESC, p.id DESC", st -> st.setLong(1, customerId),
                SqlitePaymentDao::read);
    }
}
