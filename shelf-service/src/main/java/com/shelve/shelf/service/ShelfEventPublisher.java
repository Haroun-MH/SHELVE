package com.shelve.shelf.service;

import com.shelve.shelf.config.RabbitMQConfig;
import com.shelve.shelf.event.ShelfEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShelfEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishShelfEvent(ShelfEvent event) {
        log.info("Publishing shelf event: {}", event);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SHELF_EXCHANGE,
                RabbitMQConfig.SHELF_ROUTING_KEY,
                event
        );
    }
}
