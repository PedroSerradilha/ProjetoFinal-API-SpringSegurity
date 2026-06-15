package com.example.email.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.example.email.dto.EmailRecordDto;
import com.example.email.service.EmailService;

@Component
public class EmailConsumer {

    @Autowired
    private EmailService emailService;

    @RabbitListener(queues = "default.email")
    public void listen(@Payload EmailRecordDto emailDto) {
        System.out.println("📬 Nova mensagem recebida da fila RabbitMQ para: " + emailDto.emailTo());
        
        // Repassa os dados para o seu serviço de e-mail processar e enviar via SMTP
        emailService.sendEmail(emailDto);
    }
}