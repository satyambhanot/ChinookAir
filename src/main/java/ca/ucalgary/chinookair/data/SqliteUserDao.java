package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Customer;
import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;
import ca.ucalgary.chinookair.business.entity.UserFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

class SqliteUserDao implements UserDao {

    // the profile columns are empty for an agent or an administrator
    static final String SELECT = "SELECT u.id, u.username, u.password_hash, u.role, c.name, c.email, c.phone "
            + "FROM app_user u LEFT JOIN customer c ON c.user_id = u.id";

    private final Connection connection;

    SqliteUserDao(Connection connection) {
        this.connection = connection;
    }

    /** Builds the subclass of User that goes with the role of the row. */
    static User read(ResultSet row) throws SQLException {
        return UserFactory.create(Role.valueOf(row.getString("role")), row.getLong("id"),
                row.getString("username"), row.getString("password_hash"), row.getString("name"),
                row.getString("email"), row.getString("phone"));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        // the username column compares without case
        return JdbcSupport.queryOne(connection, SELECT + " WHERE u.username = ?",
                st -> st.setString(1, username.trim()), SqliteUserDao::read);
    }

    @Override
    public Optional<User> findById(long userId) {
        return JdbcSupport.queryOne(connection, SELECT + " WHERE u.id = ?", st -> st.setLong(1, userId),
                SqliteUserDao::read);
    }

    @Override
    public User save(User user) {
        if (user.id() != null) {
            throw new IllegalArgumentException("This user is already saved");
        }
        if (user instanceof Customer) {
            return new SqliteCustomerDao(connection).save((Customer) user);
        }
        long id = SqliteCustomerDao.insertUser(connection, user);
        return UserFactory.create(user.role(), id, user.username(), user.passwordHash(), null, null, null);
    }
}
