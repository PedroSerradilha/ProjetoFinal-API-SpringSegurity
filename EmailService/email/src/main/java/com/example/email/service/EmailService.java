package com.example.email.service; // Ajuste para o seu pacote se necessário

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.email.dto.EmailRecordDto;
import com.example.email.entity.EmailModel;
import com.example.email.entity.StatusEmail;
import com.example.email.repository.EmailRepository;

@Service
public class EmailService {

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private JavaMailSender emailSender; // Ferramenta do Spring para disparar o e-mail real

    // Pega o seu e-mail do Gmail configurado nas propriedades para usar como remetente
    @Value("${spring.mail.username}")
    private String emailFrom;

    @Transactional
    public void sendEmail(EmailRecordDto emailRecordDto) {
        // Criação do objeto usando o construtor tradicional, sem depender do Lombok
        EmailModel emailModel = new EmailModel(
                emailRecordDto.userId(),
                this.emailFrom,
                emailRecordDto.emailTo(),
                emailRecordDto.subject(),
                emailRecordDto.text(),
                LocalDateTime.now(),
                null
        );

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailModel.getEmailFrom());
            message.setTo(emailModel.getEmailTo());
            message.setSubject(emailModel.getSubject());
            message.setText(emailModel.getText());

            emailSender.send(message);

            emailModel.setStatus(StatusEmail.SENT);
        } catch (MailException e) {
            emailModel.setStatus(StatusEmail.ERROR);
            System.err.println("Falha ao disparar e-mail real: " + e.getMessage());
        } finally {
            emailRepository.save(emailModel);
        }
    }
}