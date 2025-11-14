package com.innowise.demo.orderservice.mapper;

import com.innowise.demo.orderservice.dto.OrderDto;
import com.innowise.demo.orderservice.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {
    @Mapping(target = "userEmail", ignore = true)
    @Mapping(target = "userInfo", ignore = true)
    OrderDto toDto(Order entity);

    Order toEntity(OrderDto dto);
}