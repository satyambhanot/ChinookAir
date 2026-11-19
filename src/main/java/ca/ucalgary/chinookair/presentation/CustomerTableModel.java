package ca.ucalgary.chinookair.presentation;

import ca.ucalgary.chinookair.business.entity.Customer;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/** The rows of the customer list: one customer per row, read only. */
class CustomerTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"Name", "Email", "Phone", "Username"};

    private final transient List<Customer> customers = new ArrayList<>();

    /** Replaces all rows. */
    void setCustomers(List<Customer> newCustomers) {
        customers.clear();
        customers.addAll(newCustomers);
        fireTableDataChanged();
    }

    Customer customerAt(int row) {
        return customers.get(row);
    }

    @Override
    public int getRowCount() {
        return customers.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int row, int column) {
        Customer customer = customers.get(row);
        switch (column) {
            case 0:
                return customer.name();
            case 1:
                return customer.email();
            case 2:
                // the phone is optional
                return customer.phone() == null ? "" : customer.phone();
            case 3:
                return customer.username();
            default:
                throw new IndexOutOfBoundsException("No column " + column);
        }
    }
}
