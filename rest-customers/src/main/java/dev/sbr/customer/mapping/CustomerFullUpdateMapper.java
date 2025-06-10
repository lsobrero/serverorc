package dev.sbr.customer.mapping;

import dev.sbr.customer.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface CustomerFullUpdateMapper {
    @Mapping(target = "id", ignore = true)
    void mapFullUpdate(Customer input, @MappingTarget Customer target);
}
