package ca.ucalgary.chinookair.data;

import ca.ucalgary.chinookair.business.entity.StorageException;
import java.sql.SQLException;

/** Something went wrong while talking to the database. It wraps the checked SQLException. */
public class DataAccessException extends StorageException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, SQLException cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
