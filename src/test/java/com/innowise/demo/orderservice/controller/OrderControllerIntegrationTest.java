package com.innowise.demo.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.demo.orderservice.dto.OrderDto;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.annotation.DirtiesContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "user.service.url=http://nonexistent-host:9999"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createOrder_ShouldReturnCreated() throws Exception {
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1L);
        orderDto.setUserEmail("test@example.com");
        orderDto.setStatus("PENDING");

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void getOrderById_ShouldReturnNotFound_WhenOrderNotExists() throws Exception {
        mockMvc.perform(get("/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrdersByIds_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/orders").param("ids", "1,2,3"))
                .andExpect(status().isOk());
    }

    @Test
    void getOrdersByIds_ShouldReturnEmptyList_WhenNoOrders() throws Exception {
        mockMvc.perform(get("/orders").param("ids", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getOrdersByStatuses_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/orders/statuses").param("statuses", "PENDING"))
                .andExpect(status().isOk());
    }

    @Test
    void getOrdersByStatuses_ShouldReturnEmptyList_WhenNoMatchingStatus() throws Exception {
        mockMvc.perform(get("/orders/statuses").param("statuses", "NON_EXISTENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void updateOrder_ShouldReturnNotFound_WhenOrderNotExists() throws Exception {
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1L);
        orderDto.setUserEmail("test@example.com");
        orderDto.setStatus("COMPLETED");

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

    @Test
    void createAndGetOrder_ShouldWork() throws Exception {
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(100L);
        orderDto.setUserEmail("unique100@example.com");
        orderDto.setStatus("PENDING");

        String response = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @Transactional
    void createUpdateAndDeleteOrder_ShouldWork() throws Exception {
        OrderDto createDto = new OrderDto();
        createDto.setUserId(200L);
        createDto.setUserEmail("update200@example.com");
        createDto.setStatus("PENDING");

        String response = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(response).get("id").asLong();

        OrderDto updateDto = new OrderDto();
        updateDto.setUserId(200L);
        updateDto.setUserEmail("updated200@example.com");
        updateDto.setStatus("COMPLETED");

        mockMvc.perform(put("/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(delete("/orders/{id}", orderId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void simpleUpdateTest() throws Exception {
        OrderDto createDto = new OrderDto();
        createDto.setUserId(300L);
        createDto.setUserEmail("simple300@example.com");
        createDto.setStatus("PENDING");

        String response = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(response).get("id").asLong();

        OrderDto updateDto = new OrderDto();
        updateDto.setUserId(300L);
        updateDto.setUserEmail("simple_updated300@example.com");
        updateDto.setStatus("COMPLETED");

        mockMvc.perform(put("/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void createOrder_WithDifferentData_ShouldWork() throws Exception {
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(400L);
        orderDto.setUserEmail("another400@example.com");
        orderDto.setStatus("COMPLETED");

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}