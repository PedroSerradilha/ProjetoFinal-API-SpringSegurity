package com.example.email.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "emails")
public class EmailModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID emailId;
    
    private UUID userId;
    private String emailFrom;
    private String emailTo;
    private String subject;
    
    @Column(columnDefinition = "TEXT")
    private String text;
    
    private LocalDateTime sendDateEmail;
    
    @Enumerated(EnumType.STRING)
    private StatusEmail status;

    // Construtor Padrão (Vazio)
    public EmailModel() {}

    // Construtor Completo
    public EmailModel(UUID userId, String emailFrom, String emailTo, String subject, String text, LocalDateTime sendDateEmail, StatusEmail status) {
        this.userId = userId;
        this.emailFrom = emailFrom;
        this.emailTo = emailTo;
        this.subject = subject;
        this.text = text;
        this.sendDateEmail = sendDateEmail;
        this.status = status;
    }

    // Getters e Setters manuais (para garantir o build sem depender de plugins)
    public UUID getEmailId() { return emailId; }
    public void setEmailId(UUID emailId) { this.emailId = emailId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getEmailFrom() { return emailFrom; }
    public void setEmailFrom(String emailFrom) { this.emailFrom = emailFrom; }
    public String getEmailTo() { return emailTo; }
    public void setEmailTo(String emailTo) { this.emailTo = emailTo; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public LocalDateTime getSendDateEmail() { return sendDateEmail; }
    public void setSendDateEmail(LocalDateTime sendDateEmail) { this.sendDateEmail = sendDateEmail; }
    public StatusEmail getStatus() { return status; }
    public void setStatus(StatusEmail status) { this.status = status; }
}