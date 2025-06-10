package dev.sbr.customer.mapping;

import dev.sbr.customer.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, nullValuePropertyMappingStrategy = IGNORE)
public interface CustomerPartialUpdateMapper {
    void mapPartialUpdate(Customer input, @MappingTarget Customer target);
}
