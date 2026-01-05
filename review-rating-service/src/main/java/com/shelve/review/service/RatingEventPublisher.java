package com.shelve.review.service;

import com.shelve.review.config.RabbitMQConfig;
import com.shelve.review.event.RatingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishRatingCreated(RatingEvent event) {
        log.info("Publishing rating event: {}", event);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.RATING_EXCHANGE,
                RabbitMQConfig.RATING_ROUTING_KEY,
                event
        );
    }
}
