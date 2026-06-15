package com.example.email.repository; 

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.email.entity.EmailModel;

@Repository
public interface EmailRepository extends JpaRepository<EmailModel, UUID> {
}