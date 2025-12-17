package com.innowise.demo.orderservice.service;

import com.innowise.demo.orderservice.dto.OrderDto;
import com.innowise.demo.orderservice.dto.OrderEvent;
import com.innowise.demo.orderservice.entity.Order;
import com.innowise.demo.orderservice.entity.OrderItem;
import com.innowise.demo.orderservice.mapper.OrderMapper;
import com.innowise.demo.orderservice.repository.OrderRepository;
import com.innowise.demo.orderservice.repository.ItemRepository;
import com.innowise.demo.orderservice.kafka.OrderKafkaProducer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private WebClient webClient;

    @Autowired
    private OrderKafkaProducer orderKafkaProducer;

    public OrderDto createOrder(OrderDto orderDto) {
        logger.info("Creating order for user: {}", orderDto.getUserEmail());
        logger.info("Order DTO received: userId={}, status={}, itemsCount={}",
                orderDto.getUserId(),
                orderDto.getStatus(),
                orderDto.getOrderItems() != null ? orderDto.getOrderItems().size() : 0);

        if (orderDto.getOrderItems() == null || orderDto.getOrderItems().isEmpty()) {
            logger.warn("Order items is null or empty");
        } else {
            for (int i = 0; i < orderDto.getOrderItems().size(); i++) {
                var item = orderDto.getOrderItems().get(i);
                logger.info("Item {}: id={}, name={}, price={}, quantity={}",
                        i,
                        item.getItem() != null ? item.getItem().getId() : "null",
                        item.getItem() != null ? item.getItem().getName() : "null",
                        item.getItem() != null ? item.getItem().getPrice() : "null",
                        item.getQuantity());
            }
        }
        Order order = orderMapper.toEntity(orderDto);
        if (order.getCreationDate() == null) {
            order.setCreationDate(LocalDateTime.now());
        }
        if (order.getOrderItems() != null) {
            for (OrderItem orderItem : order.getOrderItems()) {
                orderItem.setOrder(order);
            }
        }
        Order savedOrder = orderRepository.save(order);
        logger.info("Saved order with ID: {}", savedOrder.getId());

        sendOrderEventToKafka(savedOrder);

        OrderDto result = orderMapper.toDto(savedOrder);
        result.setUserEmail(orderDto.getUserEmail());
        result.setUserInfo(getUserInfo(orderDto.getUserEmail()));
        logger.info("Returning DTO: {}", result);
        return result;
    }

    private void sendOrderEventToKafka(Order order) {
        logger.info("=== SEND ORDER EVENT TO KAFKA ===");
        logger.info("Order ID: {}", order.getId());
        logger.info("User ID: {}", order.getUserId());

        try {
            OrderEvent orderEvent = new OrderEvent();
            orderEvent.setOrderId(order.getId().toString());
            orderEvent.setUserId(order.getUserId().toString());
            orderEvent.setTimestamp(order.getCreationDate());

            Double totalAmount = calculateOrderAmount(order);
            orderEvent.setAmount(totalAmount);

            logger.info("Created OrderEvent: {}", orderEvent);
            logger.info("Calling orderKafkaProducer...");

            orderKafkaProducer.sendOrderEvent(orderEvent);

            logger.info("=== KAFKA EVENT SENT SUCCESSFULLY ===");
        } catch (Exception e) {
            logger.error("=== FAILED TO SEND KAFKA EVENT ===");
            logger.error("Error: {}", e.getMessage(), e);
        }
    }

    private Double calculateOrderAmount(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        for (OrderItem orderItem : order.getOrderItems()) {
            if (orderItem.getItem() != null && orderItem.getItem().getPrice() != null) {
                total += orderItem.getItem().getPrice().doubleValue() * orderItem.getQuantity();
            }
        }
        return total;
    }

    public Optional<OrderDto> getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(orderMapper::toDto)
                .map(dto -> {
                    dto.setUserInfo(getUserInfo(dto.getUserEmail()));
                    return dto;
                });
    }

    public List<OrderDto> getOrdersByIds(List<Long> ids) {
        return orderRepository.findAllById(ids).stream()
                .map(orderMapper::toDto)
                .peek(dto -> dto.setUserInfo(getUserInfo(dto.getUserEmail())))
                .collect(Collectors.toList());
    }

    public List<OrderDto> getOrdersByStatuses(List<String> statuses) {
        return orderRepository.findByStatuses(statuses).stream()
                .map(orderMapper::toDto)
                .peek(dto -> dto.setUserInfo(getUserInfo(dto.getUserEmail())))
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDto updateOrder(Long id, OrderDto orderDto) {
        logger.info("Updating order with ID: {}", id);

        if (orderRepository.existsById(id)) {
            if (orderDto.getOrderItems() != null) {
                for (var orderItemDto : orderDto.getOrderItems()) {
                    if (orderItemDto.getItem() == null || !itemRepository.existsById(orderItemDto.getItem().getId())) {
                        throw new RuntimeException("Item not found: " + (orderItemDto.getItem() != null ? orderItemDto.getItem().getId() : "null"));
                    }
                }
            }
            orderDto.setId(id);
            Order order = orderMapper.toEntity(orderDto);
            if (order.getCreationDate() == null) {
                order.setCreationDate(LocalDateTime.now());
            }
            if (order.getOrderItems() != null) {
                for (OrderItem orderItem : order.getOrderItems()) {
                    orderItem.setOrder(order);
                }
            }
            Order savedOrder = orderRepository.save(order);
            OrderDto result = orderMapper.toDto(savedOrder);
            result.setUserEmail(orderDto.getUserEmail());
            result.setUserInfo(getUserInfo(orderDto.getUserEmail()));
            return result;
        }
        throw new RuntimeException("Order not found with id: " + id);
    }

    @Transactional
    public void updateOrderStatus(String orderId, String status) {
        logger.info("Updating order status: orderId={}, status={}", orderId, status);

        try {
            Long id = Long.parseLong(orderId);  // Конвертируем String в Long

            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            order.setStatus(status);
            orderRepository.save(order);

            logger.info("Order {} status updated to {}", orderId, status);
        } catch (NumberFormatException e) {
            logger.error("Invalid orderId format: {}", orderId);
        }
    }

    @Transactional
    public void deleteOrderById(Long id) {
        orderRepository.deleteById(id);
    }

    private Object getUserInfo(String email) {
        if (email == null || email.isBlank()) {
            logger.warn("Email is null or blank, returning fallback");
            return Map.of("error", "Email not provided", "email", "unknown");
        }
        try {
            return webClient.get()
                    .uri("/users?email={email}", email)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
        } catch (Exception e) {
            logger.warn("User Service unavailable for email: {}, returning fallback", email, e);
            return Map.of("error", "User Service unavailable", "email", email);
        }
    }
}