package ca.ucalgary.chinookair.data;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

/** For tests: a DAO that behaves like the real one except that one method fails, to prove a change is undone. */
public final class FailingDao {

    private FailingDao() {
    }

    /** @return the real DAO, but every call of the named method throws a {@link DataAccessException} */
    public static <T> T failingOn(Class<T> type, T real, String method) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, called, args) -> {
            if (called.getName().equals(method)) {
                throw new DataAccessException("Injected failure in " + method);
            }
            try {
                return called.invoke(real, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }));
    }
}
