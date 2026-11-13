package ca.ucalgary.chinookair.business.control;

import java.time.LocalDate;

/**
 * What the customer typed on the search screen. The controller checks and tidies it,
 * so this record only carries the values. The airline is null or blank for "any airline".
 */
public record SearchCriteria(String origin, String destination, LocalDate date, String airlineCode) {
}
