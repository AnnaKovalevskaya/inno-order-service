package com.innowise.demo.orderservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.demo.orderservice.dto.PaymentEvent;
import com.innowise.demo.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderKafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderKafkaConsumer.class);

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "create-payment", groupId = "order-group")
    public void handleCreatePayment(String message) {
        logger.info("=== KAFKA CONSUMER: Received payment event ===");
        logger.info("Message: {}", message);

        try {
            PaymentEvent paymentEvent = objectMapper.readValue(message, PaymentEvent.class);

            logger.info("Parsed payment event - orderId: {}, status: {}",
                    paymentEvent.getOrderId(), paymentEvent.getStatus());

            orderService.updateOrderStatus(paymentEvent.getOrderId(), paymentEvent.getStatus());

            logger.info("Successfully updated order {} status to {}",
                    paymentEvent.getOrderId(), paymentEvent.getStatus());

        } catch (Exception e) {
            logger.error("Failed to process payment message: {}", message, e);
        }
    }
}