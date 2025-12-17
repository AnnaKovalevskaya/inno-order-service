package com.innowise.demo.orderservice.service;

import com.innowise.demo.orderservice.dto.OrderDto;
import com.innowise.demo.orderservice.entity.Order;
import com.innowise.demo.orderservice.kafka.OrderKafkaProducer;
import com.innowise.demo.orderservice.mapper.OrderMapper;
import com.innowise.demo.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private OrderKafkaProducer orderKafkaProducer;

    @InjectMocks
    private OrderService orderService;

    private void setupWebClientMock() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Object.class)).thenReturn(Mono.just(Map.of("name", "Test User")));
    }

    @Test
    void createOrder_ShouldReturnOrderDto() {
        setupWebClientMock();
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1L);
        orderDto.setUserEmail("test@example.com");
        orderDto.setStatus("PENDING");

        Order order = new Order();
        order.setUserId(1L);

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setUserId(1L);

        when(orderMapper.toEntity(orderDto)).thenReturn(order);
        when(orderRepository.save(order)).thenReturn(savedOrder);
        when(orderMapper.toDto(savedOrder)).thenReturn(orderDto);

        OrderDto result = orderService.createOrder(orderDto);

        assertNotNull(result);
        verify(orderRepository).save(order);
        verify(webClient).get();
        verify(orderKafkaProducer).sendOrderEvent(any());
    }

    @Test
    void getOrderById_ShouldReturnOrderDto_WhenOrderExists() {
        setupWebClientMock();
        Long orderId = 1L;
        Order order = new Order();
        OrderDto orderDto = new OrderDto();
        orderDto.setUserEmail("test@example.com");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toDto(order)).thenReturn(orderDto);

        Optional<OrderDto> result = orderService.getOrderById(orderId);

        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getUserEmail());
        verify(orderRepository).findById(orderId);
        verify(webClient).get();
    }

    @Test
    void getOrderById_ShouldReturnEmpty_WhenOrderNotExists() {
        Long orderId = 999L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        Optional<OrderDto> result = orderService.getOrderById(orderId);

        assertTrue(result.isEmpty());
        verify(orderRepository).findById(orderId);
        verifyNoInteractions(webClient);
    }

    @Test
    void getOrdersByIds_ShouldReturnListOfOrderDto() {
        setupWebClientMock();
        List<Long> ids = List.of(1L, 2L);
        Order order1 = new Order();
        Order order2 = new Order();

        OrderDto orderDto1 = new OrderDto();
        orderDto1.setUserEmail("test1@example.com");
        OrderDto orderDto2 = new OrderDto();
        orderDto2.setUserEmail("test2@example.com");

        when(orderRepository.findAllById(ids)).thenReturn(List.of(order1, order2));
        when(orderMapper.toDto(order1)).thenReturn(orderDto1);
        when(orderMapper.toDto(order2)).thenReturn(orderDto2);

        List<OrderDto> result = orderService.getOrdersByIds(ids);

        assertEquals(2, result.size());
        assertEquals("test1@example.com", result.get(0).getUserEmail());
        assertEquals("test2@example.com", result.get(1).getUserEmail());
        verify(orderRepository).findAllById(ids);
        verify(webClient, times(2)).get();
    }

    @Test
    void getOrdersByStatuses_ShouldReturnListOfOrderDto() {
        setupWebClientMock();
        List<String> statuses = List.of("PENDING");
        Order order = new Order();
        OrderDto orderDto = new OrderDto();
        orderDto.setUserEmail("test@example.com");

        when(orderRepository.findByStatuses(statuses)).thenReturn(List.of(order));
        when(orderMapper.toDto(order)).thenReturn(orderDto);

        List<OrderDto> result = orderService.getOrdersByStatuses(statuses);

        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getUserEmail());
        verify(orderRepository).findByStatuses(statuses);
        verify(webClient).get();
    }

    @Test
    void updateOrder_ShouldReturnUpdatedOrderDto_WhenOrderExists() {
        setupWebClientMock();
        Long orderId = 1L;
        OrderDto orderDto = new OrderDto();
        orderDto.setUserEmail("updated@example.com");
        orderDto.setStatus("COMPLETED");

        Order order = new Order();
        Order updatedOrder = new Order();

        when(orderRepository.existsById(orderId)).thenReturn(true);
        when(orderMapper.toEntity(orderDto)).thenReturn(order);
        when(orderRepository.save(order)).thenReturn(updatedOrder);
        when(orderMapper.toDto(updatedOrder)).thenReturn(orderDto);

        OrderDto result = orderService.updateOrder(orderId, orderDto);

        assertNotNull(result);
        assertEquals("updated@example.com", result.getUserEmail());
        verify(orderRepository).save(order);
        verify(webClient).get();
    }

    @Test
    void updateOrder_ShouldThrowException_WhenOrderNotFound() {
        Long orderId = 999L;
        OrderDto orderDto = new OrderDto();

        when(orderRepository.existsById(orderId)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> orderService.updateOrder(orderId, orderDto));
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(webClient);
    }

    @Test
    void deleteOrderById_ShouldCallRepositoryDelete() {
        Long orderId = 1L;

        orderService.deleteOrderById(orderId);

        verify(orderRepository).deleteById(orderId);
        verifyNoInteractions(webClient);
    }

    @Test
    void deleteOrderById_ShouldNotThrow_WhenOrderNotExists() {
        Long orderId = 999L;

        assertDoesNotThrow(() -> orderService.deleteOrderById(orderId));
        verify(orderRepository).deleteById(orderId);
        verifyNoInteractions(webClient);
    }

    @Test
    void createOrder_ShouldCallWebClientWithCorrectEmail() {
        setupWebClientMock();
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1L);
        orderDto.setUserEmail("test@example.com");
        orderDto.setStatus("PENDING");

        Order order = new Order();
        order.setUserId(1L);

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setUserId(1L);

        when(orderMapper.toEntity(orderDto)).thenReturn(order);
        when(orderRepository.save(order)).thenReturn(savedOrder);
        when(orderMapper.toDto(savedOrder)).thenReturn(orderDto);

        orderService.createOrder(orderDto);

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri("/users?email={email}", "test@example.com");
        verify(orderKafkaProducer).sendOrderEvent(any());
    }
}