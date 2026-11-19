package ca.ucalgary.chinookair.business.entity;

/**
 * The three kinds of user. The role decides which screens and which
 * controller operations a user may use.
 */
public enum Role {
    CUSTOMER,
    AGENT,
    ADMIN
}
