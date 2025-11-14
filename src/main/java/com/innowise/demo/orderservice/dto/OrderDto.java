package com.innowise.demo.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Long id;

    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotBlank(message = "User email cannot be blank")
    private String userEmail;

    @NotBlank(message = "Status cannot be blank")
    private String status;

    private LocalDateTime creationDate;

    private List<OrderItemDto> orderItems;

    private Object userInfo;
}
