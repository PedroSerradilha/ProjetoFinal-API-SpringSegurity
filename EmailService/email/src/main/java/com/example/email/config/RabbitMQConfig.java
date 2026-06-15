package com.example.email.config; 

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Busca o nome da fila (default.email) definido no seu application.properties
    @Value("${broker.queue.email.name}")
    private String queue;

    // Declara a fila para o Spring saber onde se conectar
    @Bean
    public Queue queue() {
        return new Queue(queue, true); // true = fila durável
    }

    // Configura o conversor que transforma o JSON que chega da fila em um objeto Java (DTO)
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}