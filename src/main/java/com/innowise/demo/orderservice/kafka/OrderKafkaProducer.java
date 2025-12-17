package com.innowise.demo.orderservice.kafka;

import com.innowise.demo.orderservice.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Service
@RequiredArgsConstructor
public class OrderKafkaProducer {

    private static final Logger logger = LoggerFactory.getLogger(OrderKafkaProducer.class);
    private static final String TOPIC = "create-order";

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void sendOrderEvent(OrderEvent event) {
        logger.info("=== KAFKA PRODUCER: Attempting to send OrderEvent ===");
        logger.info("Topic: {}", TOPIC);
        logger.info("Event: {}", event);
        logger.info("KafkaTemplate is null: {}", kafkaTemplate == null);

        try {
            if (kafkaTemplate == null) {
                logger.error("KafkaTemplate is null!");
                return;
            }

            ListenableFuture<SendResult<String, OrderEvent>> future =
                    (ListenableFuture<SendResult<String, OrderEvent>>) kafkaTemplate.send(TOPIC, event);

            future.addCallback(
                    result -> {
                        logger.info("=== KAFKA SUCCESS ===");
                        logger.info("Sent message=[{}] with offset=[{}]",
                                event, result.getRecordMetadata().offset());
                        logger.info("Partition: {}", result.getRecordMetadata().partition());
                    },
                    ex -> {
                        logger.error("=== KAFKA ERROR ===");
                        logger.error("Unable to send message=[{}] due to: {}",
                                event, ex.getMessage(), ex);
                    }
            );

            logger.info("Message sent (async) to Kafka");
        } catch (Exception e) {
            logger.error("Exception while sending to Kafka: {}", e.getMessage(), e);
        }
    }
}
