package com.example.elderlink;

public class Caregiver {
    private String name;
    private String email;
    private String phone;
    private boolean isCurrentUser;

    public Caregiver() {

    }

    public Caregiver(String name, String email, String phone, boolean isCurrentUser) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.isCurrentUser = isCurrentUser;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean hasPhone() {
        return phone != null && !phone.trim().isEmpty();
    }

    public boolean isCurrentUser() {
        return isCurrentUser;
    }
}