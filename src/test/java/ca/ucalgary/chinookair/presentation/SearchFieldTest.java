package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchFieldTest {

    @Test
    void itHasAPlaceholderAMagnifierAndAClearButton() {
        SearchField field = new SearchField("Search customers", 20);
        assertEquals("Search customers", field.getClientProperty("JTextField.placeholderText"));
        assertNotNull(field.getClientProperty("JTextField.leadingIcon"));
        assertEquals(Boolean.TRUE, field.getClientProperty("JTextField.showClearButton"));
        assertEquals("Search customers", field.getAccessibleContext().getAccessibleName());
    }

    @Test
    void theListenerHearsEveryChange() {
        SearchField field = new SearchField("Search", 10);
        List<String> heard = new ArrayList<>();
        field.onChange(heard::add);
        field.setText("ca");
        field.setText("cas");
        field.setText("");
        // setText replaces the text, so each call is a removal and an insert; the last words are what count
        assertEquals("cas", heard.get(heard.size() - 2));
        assertEquals("", heard.get(heard.size() - 1));
    }
}
