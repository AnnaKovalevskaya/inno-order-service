package com.innowise.demo.orderservice.service;

import com.innowise.demo.orderservice.dto.OrderDto;
import com.innowise.demo.orderservice.entity.Order;
import com.innowise.demo.orderservice.entity.OrderItem;
import com.innowise.demo.orderservice.mapper.OrderMapper;
import com.innowise.demo.orderservice.repository.OrderRepository;
import com.innowise.demo.orderservice.repository.ItemRepository;
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

    public OrderDto createOrder(OrderDto orderDto) {
        logger.info("Creating order for user: {}", orderDto.getUserEmail());
        if (orderDto.getOrderItems() != null) {
            for (var orderItemDto : orderDto.getOrderItems()) {
                if (orderItemDto.getItem() == null || !itemRepository.existsById(orderItemDto.getItem().getId())) {
                    throw new RuntimeException("Item not found: " + (orderItemDto.getItem() != null ? orderItemDto.getItem().getId() : "null"));
                }
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
        OrderDto result = orderMapper.toDto(savedOrder);
        result.setUserEmail(orderDto.getUserEmail());
        result.setUserInfo(getUserInfo(orderDto.getUserEmail()));
        logger.info("Returning DTO: {}", result);
        return result;
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