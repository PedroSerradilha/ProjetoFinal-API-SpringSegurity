package com.example.secrets.dto;

import java.util.List;

public class UserProfileDto {
    private String name;
    private String email;
    private List<String> roles;

    public UserProfileDto() {}

    public UserProfileDto(String name, String email, List<String> roles) {
        this.name = name;
        this.email = email;
        this.roles = roles;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}