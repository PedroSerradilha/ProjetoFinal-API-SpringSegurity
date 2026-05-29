package com.example.secrets.dto;

import com.example.secrets.enums.RoleName;

public record CreateUserDto(
    String email,
    String password,
    RoleName role
) {}