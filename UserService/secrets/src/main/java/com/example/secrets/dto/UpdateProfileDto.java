package com.example.secrets.dto;

public class UpdateProfileDto {
    private String name;
    private String role; // Receberá do HTML "ROLE_CUSTOMER" ou "ROLE_ADMINISTRATOR"

    public UpdateProfileDto() {}

    public UpdateProfileDto(String name, String role) {
        this.name = name;
        this.role = role;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}