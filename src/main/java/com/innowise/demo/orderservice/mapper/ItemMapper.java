package com.innowise.demo.orderservice.mapper;

import com.innowise.demo.orderservice.dto.ItemDto;
import com.innowise.demo.orderservice.entity.Item;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemMapper {
    ItemDto toDto(Item entity);
    Item toEntity(ItemDto dto);
}
