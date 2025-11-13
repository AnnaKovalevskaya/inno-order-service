package com.innowise.demo.orderservice.controller;

import com.innowise.demo.orderservice.dto.OrderDto;
import com.innowise.demo.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody OrderDto orderDto) {
        OrderDto created = orderService.createOrder(orderDto);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getOrdersByIds(@RequestParam List<Long> ids) {
        List<OrderDto> orders = orderService.getOrdersByIds(ids);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/statuses")
    public ResponseEntity<List<OrderDto>> getOrdersByStatuses(@RequestParam List<String> statuses) {
        List<OrderDto> orders = orderService.getOrdersByStatuses(statuses);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDto> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderDto orderDto) {
        try {
            OrderDto updated = orderService.updateOrder(id, orderDto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderById(@PathVariable Long id) {
        orderService.deleteOrderById(id);
        return ResponseEntity.noContent().build();
    }
}
