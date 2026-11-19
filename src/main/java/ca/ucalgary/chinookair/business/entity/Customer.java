package ca.ucalgary.chinookair.business.entity;

import java.util.regex.Pattern;

/** A person who reserves seats. The system keeps a name, an email and an optional phone. */
public class Customer extends User {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final String name;
    private final String email;
    private final String phone;

    /** The phone is optional: null or blank means none. */
    public Customer(Long id, String username, String passwordHash, String name, String email, String phone) {
        super(id, username, passwordHash);
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Email must look like name@domain.ca");
        }
        this.name = name.trim();
        this.email = email.trim();
        this.phone = phone == null || phone.isBlank() ? null : phone.trim();
    }

    @Override
    public Role role() {
        return Role.CUSTOMER;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    /** @return the phone number, or null if the customer gave none */
    public String phone() {
        return phone;
    }
}
