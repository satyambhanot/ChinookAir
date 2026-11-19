package ca.ucalgary.chinookair.business.entity;

/** The answer of the simulated card authorization. */
public record AuthorizationResult(boolean approved, String message) {
}
