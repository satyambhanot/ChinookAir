package ca.ucalgary.chinookair.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.ucalgary.chinookair.business.entity.Customer;
import java.util.List;
import org.junit.jupiter.api.Test;

class CustomerTableModelTest {

    private static Customer customer(String phone) {
        return new Customer(1L, "jsmith", "pbkdf2$1$AAAA$BBBB", "Jo Smith", "jo@x.ca", phone);
    }

    @Test
    void aCustomerShowsNameEmailPhoneAndUsernameInOrder() {
        CustomerTableModel model = new CustomerTableModel();
        model.setCustomers(List.of(customer("403-555-0100")));
        assertEquals(1, model.getRowCount());
        assertEquals(4, model.getColumnCount());
        assertEquals("Jo Smith", model.getValueAt(0, 0));
        assertEquals("jo@x.ca", model.getValueAt(0, 1));
        assertEquals("403-555-0100", model.getValueAt(0, 2));
        assertEquals("jsmith", model.getValueAt(0, 3));
        assertEquals("Name", model.getColumnName(0));
        assertEquals("Username", model.getColumnName(3));
    }

    @Test
    void aMissingPhoneShowsAsEmptyText() {
        CustomerTableModel model = new CustomerTableModel();
        model.setCustomers(List.of(customer(null)));
        assertEquals("", model.getValueAt(0, 2));
    }

    @Test
    void settingNewCustomersReplacesTheOldRows() {
        CustomerTableModel model = new CustomerTableModel();
        model.setCustomers(List.of(customer(null), customer(null)));
        model.setCustomers(List.of());
        assertEquals(0, model.getRowCount());
    }

    @Test
    void anUnknownColumnIsRejected() {
        CustomerTableModel model = new CustomerTableModel();
        model.setCustomers(List.of(customer(null)));
        assertThrows(IndexOutOfBoundsException.class, () -> model.getValueAt(0, 4));
    }
}
