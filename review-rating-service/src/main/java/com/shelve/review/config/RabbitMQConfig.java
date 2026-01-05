package com.shelve.review.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String RATING_EXCHANGE = "rating.exchange";
    public static final String RATING_QUEUE = "rating.queue";
    public static final String RATING_ROUTING_KEY = "rating.created";

    @Bean
    public Exchange ratingExchange() {
        return ExchangeBuilder.topicExchange(RATING_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue ratingQueue() {
        return QueueBuilder.durable(RATING_QUEUE).build();
    }

    @Bean
    public Binding ratingBinding(Queue ratingQueue, Exchange ratingExchange) {
        return BindingBuilder.bind(ratingQueue)
                .to(ratingExchange)
                .with(RATING_ROUTING_KEY)
                .noargs();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
