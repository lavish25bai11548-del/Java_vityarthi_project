package com.bank.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a registered bank customer.
 */
public class Customer implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String customerId;
    private String fullName;
    private String email;
    private String phone;

    public Customer(String customerId, String fullName, String email, String phone) {
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.fullName = Objects.requireNonNull(fullName, "Full name cannot be null");
        this.email = email == null ? "" : email;
        this.phone = phone == null ? "" : phone;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        return String.format("%s (%s | %s | %s)", fullName, customerId, email, phone);
    }
}
