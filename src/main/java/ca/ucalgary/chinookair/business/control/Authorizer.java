package ca.ucalgary.chinookair.business.control;

import ca.ucalgary.chinookair.business.entity.Role;
import ca.ucalgary.chinookair.business.entity.User;

/** The role check that every controller uses before it does anything for a user. */
public final class Authorizer {

    private Authorizer() {
    }

    /**
     * Passes silently if the actor has one of the allowed roles.
     *
     * @throws AccessDeniedException if nobody is acting, or the role is not allowed
     */
    public static void require(User actor, Role... allowed) {
        if (actor == null) {
            throw new AccessDeniedException("Please log in first");
        }
        for (Role role : allowed) {
            if (actor.role() == role) {
                return;
            }
        }
        throw new AccessDeniedException("You do not have permission to do this");
    }
}
