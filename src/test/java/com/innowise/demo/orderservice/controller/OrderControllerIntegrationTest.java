package com.innowise.demo.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.demo.orderservice.config.TestConfig;
import com.innowise.demo.orderservice.dto.OrderDto;
import com.innowise.demo.orderservice.dto.OrderItemDto;
import com.innowise.demo.orderservice.dto.ItemDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createOrder_ShouldReturnCreated() throws Exception {
        OrderDto orderDto = createValidOrderDto(1L, "test@example.com", "PENDING");

        String responseContent = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andDo(result -> {
                    System.out.println("=== DEBUG INFO ===");
                    System.out.println("Request Body: " + objectMapper.writeValueAsString(orderDto));
                    System.out.println("Response Status: " + result.getResponse().getStatus());
                    System.out.println("Response Body: " + result.getResponse().getContentAsString());
                    System.out.println("=== END DEBUG ===");
                })
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        System.out.println("Success! Order created: " + responseContent);
    }

    @Test
    void getOrderById_ShouldReturnNotFound_WhenOrderNotExists() throws Exception {
        mockMvc.perform(get("/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrdersByIds_ShouldReturnOk() throws Exception {
        OrderDto orderDto = createValidOrderDto(1L, "test1@example.com", "PENDING");

        String response = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/orders")
                        .param("ids", orderId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getOrdersByStatuses_ShouldReturnOk() throws Exception {
        OrderDto orderDto = createValidOrderDto(1L, "test@example.com", "PENDING");

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/orders/statuses")
                        .param("statuses", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void updateOrder_ShouldReturnNotFound_WhenOrderNotExists() throws Exception {
        OrderDto orderDto = createValidOrderDto(1L, "test@example.com", "COMPLETED");

        mockMvc.perform(put("/orders/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOrderById_ShouldReturnNoContent_EvenWhenOrderNotExists() throws Exception {
        mockMvc.perform(delete("/orders/999"))
                .andExpect(status().isNoContent());
    }

    private OrderDto createValidOrderDto(Long userId, String email, String status) {
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(userId);
        orderDto.setUserEmail(email);
        orderDto.setStatus(status);

        List<OrderItemDto> orderItems = new ArrayList<>();

        ItemDto itemDto = new ItemDto();
        itemDto.setName("Test Item " + System.currentTimeMillis());
        itemDto.setPrice(BigDecimal.valueOf(99.99));

        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setItem(itemDto);
        orderItemDto.setQuantity(2);

        orderItems.add(orderItemDto);
        orderDto.setOrderItems(orderItems);

        return orderDto;
    }
}