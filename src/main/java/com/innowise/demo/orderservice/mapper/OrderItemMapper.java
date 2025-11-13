package com.innowise.demo.orderservice.mapper;

import com.innowise.demo.orderservice.dto.OrderItemDto;
import com.innowise.demo.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ItemMapper.class})
public interface OrderItemMapper {
    @Mapping(target = "order", ignore = true)
    OrderItem toEntity(OrderItemDto dto);

    OrderItemDto toDto(OrderItem entity);
}