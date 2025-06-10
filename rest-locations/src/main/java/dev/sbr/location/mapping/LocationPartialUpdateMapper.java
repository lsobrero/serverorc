package dev.sbr.location.mapping;

import dev.sbr.location.model.Location;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.MappingTarget;

import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

/**
 * Mapper to map <code><strong>non-null</strong></code> fields on an input {@link Location} onto a target {@link Location}.
 */
@Mapper(componentModel = ComponentModel.JAKARTA_CDI, nullValuePropertyMappingStrategy = IGNORE)
public interface LocationPartialUpdateMapper {
	/**
	 * Maps all <code><strong>non-null</strong></code> fields from {@code input} onto {@code target}.
	 * @param input The input {@link Location}
	 * @param target The target {@link Location}
	 */
	void mapPartialUpdate(Location input, @MappingTarget Location target);
}
