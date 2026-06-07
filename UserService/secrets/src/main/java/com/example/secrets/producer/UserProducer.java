package com.example.secrets.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.secrets.dto.EmailDto;

@Component
public class UserProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${broker.queue.email.name}")
    private String routingKey;

    public void publishEmailMessage(EmailDto emailDto) {
        // Envia o JSON do e-mail direto para a fila do CloudAMQP
        rabbitTemplate.convertAndSend(routingKey, emailDto);
    }
}