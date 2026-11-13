package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.User;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

class SqliteCustomerDao implements CustomerDao {

    private static final String SELECT = SqliteUserDao.SELECT + " WHERE u.role = 'CUSTOMER'";

    private final Connection connection;

    SqliteCustomerDao(Connection connection) {
        this.connection = connection;
    }

    private static Customer asCustomer(User user) {
        return (Customer) user;
    }

    /** Inserts the login row of any user and returns its id. */
    static long insertUser(Connection connection, User user) {
        try {
            return JdbcSupport.insert(connection,
                    "INSERT INTO app_user (username, password_hash, role) VALUES (?, ?, ?)", st -> {
                        st.setString(1, user.username());
                        st.setString(2, user.passwordHash());
                        st.setString(3, user.role().name());
                    });
        } catch (DataAccessException e) {
            throw clearer(e, user);
        }
    }

    /** Puts a plain "already used" sentence in place of the raw constraint message of SQLite. */
    private static DataAccessException clearer(DataAccessException e, User user) {
        String cause = e.getCause() == null ? "" : String.valueOf(e.getCause().getMessage());
        if (cause.contains("app_user.username")) {
            return new DataAccessException("The username '" + user.username() + "' is already taken");
        }
        if (cause.contains("customer.email")) {
            return new DataAccessException("The email '" + asCustomer(user).email() + "' is already used");
        }
        return e;
    }

    @Override
    public Customer save(Customer customer) {
        if (customer.id() != null) {
            throw new IllegalArgumentException("This customer is already saved");
        }
        long id = JdbcSupport.transaction(connection, () -> {
            long userId = insertUser(connection, customer);
            try {
                JdbcSupport.update(connection, "INSERT INTO customer (user_id, name, email, phone) VALUES (?, ?, ?, ?)",
                        st -> {
                            st.setLong(1, userId);
                            st.setString(2, customer.name());
                            st.setString(3, customer.email());
                            st.setString(4, customer.phone());
                        });
            } catch (DataAccessException e) {
                throw clearer(e, customer);
            }
            return userId;
        });
        return new Customer(id, customer.username(), customer.passwordHash(), customer.name(), customer.email(),
                customer.phone());
    }

    @Override
    public void update(Customer customer) {
        if (customer.id() == null) {
            throw new IllegalArgumentException("This customer is not saved yet");
        }
        int changed;
        try {
            changed = JdbcSupport.update(connection, "UPDATE customer SET name = ?, email = ?, phone = ? "
                    + "WHERE user_id = ?", st -> {
                        st.setString(1, customer.name());
                        st.setString(2, customer.email());
                        st.setString(3, customer.phone());
                        st.setLong(4, customer.id());
                    });
        } catch (DataAccessException e) {
            throw clearer(e, customer);
        }
        if (changed != 1) {
            throw new DataAccessException("There is no customer with id " + customer.id());
        }
    }

    @Override
    public Optional<Customer> findById(long customerId) {
        return JdbcSupport.queryOne(connection, SELECT + " AND u.id = ?", st -> st.setLong(1, customerId),
                row -> asCustomer(SqliteUserDao.read(row)));
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        // the email column compares without case
        return JdbcSupport.queryOne(connection, SELECT + " AND c.email = ?", st -> st.setString(1, email.trim()),
                row -> asCustomer(SqliteUserDao.read(row)));
    }

    @Override
    public List<Customer> search(String text) {
        String needle = text == null ? "" : text.trim();
        // a % or _ typed by the user is text to find, not a wildcard
        String pattern = "%" + needle.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return JdbcSupport.query(connection, SELECT + " AND (c.name LIKE ? ESCAPE '\\' OR c.email LIKE ? ESCAPE '\\') "
                + "ORDER BY c.name, u.id", st -> {
                    st.setString(1, pattern);
                    st.setString(2, pattern);
                }, row -> asCustomer(SqliteUserDao.read(row)));
    }
}
