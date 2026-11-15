package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BreadcrumbTest {

    @BeforeAll
    static void install() {
        Theme.install();
    }

    @Test
    void everyPartIsShownAndOnlyTheEarlierLinksAreClickable() {
        List<String> clicked = new ArrayList<>();
        Breadcrumb crumbs = new Breadcrumb();
        crumbs.show(List.of(Breadcrumb.Crumb.link("Customers", () -> clicked.add("Customers")),
                Breadcrumb.Crumb.text("Casey"), Breadcrumb.Crumb.link("Modify", () -> clicked.add("Modify"))));
        assertEquals(List.of("Customers", "Casey", "Modify"), crumbs.texts());
        assertEquals(1, crumbs.links().size(), "the last part is the page you are on, so it is not a link");
        crumbs.links().get(0).doClick(0);
        assertEquals(List.of("Customers"), clicked);
    }

    @Test
    void anEmptyPathHidesTheBar() {
        Breadcrumb crumbs = new Breadcrumb();
        crumbs.show(List.of(Breadcrumb.Crumb.link("A", () -> { }), Breadcrumb.Crumb.text("B")));
        assertTrue(crumbs.isVisible());
        crumbs.show(List.of());
        assertFalse(crumbs.isVisible());
        assertEquals(0, crumbs.links().size());
    }

    @Test
    void showingAnotherPathReplacesTheFirst() {
        Breadcrumb crumbs = new Breadcrumb();
        crumbs.show(List.of(Breadcrumb.Crumb.link("A", () -> { }), Breadcrumb.Crumb.text("B")));
        crumbs.show(List.of(Breadcrumb.Crumb.text("C")));
        assertEquals(List.of("C"), crumbs.texts());
        assertEquals(0, crumbs.links().size());
    }

    @Test
    void theBarHasANameForScreenReaders() {
        assertEquals("You are here", new Breadcrumb().getAccessibleContext().getAccessibleName());
    }
}
