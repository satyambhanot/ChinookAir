package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.Promotion;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

class SqlitePromotionDao implements PromotionDao {

    private static final String SELECT = "SELECT id, title, description FROM promotion";

    private final Connection connection;

    SqlitePromotionDao(Connection connection) {
        this.connection = connection;
    }

    private static Promotion read(ResultSet row) throws SQLException {
        return new Promotion(row.getLong("id"), row.getString("title"), row.getString("description"));
    }

    @Override
    public List<Promotion> findAll() {
        return JdbcSupport.query(connection, SELECT + " ORDER BY id", JdbcSupport.NO_PARAMETERS,
                SqlitePromotionDao::read);
    }

    @Override
    public Optional<Promotion> findById(long promotionId) {
        return JdbcSupport.queryOne(connection, SELECT + " WHERE id = ?", st -> st.setLong(1, promotionId),
                SqlitePromotionDao::read);
    }

    @Override
    public Promotion save(Promotion promotion) {
        if (promotion.id() != null) {
            throw new IllegalArgumentException("This promotion is already saved");
        }
        long id = JdbcSupport.insert(connection, "INSERT INTO promotion (title, description) VALUES (?, ?)", st -> {
            st.setString(1, promotion.title());
            st.setString(2, promotion.description());
        });
        return new Promotion(id, promotion.title(), promotion.description());
    }
}
