package dev.sbr.location.mapping;

import dev.sbr.location.model.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.MappingTarget;

/**
 * Mapper to map all fields on an input {@link Location} onto a target {@link Location}.
 */
@Mapper(componentModel = ComponentModel.JAKARTA_CDI)
public interface LocationFullUpdateMapper {
	/**
	 * Maps all fields except <code>id</code> from {@code input} onto {@code target}.
	 * @param input The input {@link Location}
	 * @param target The target {@link Location}
	 */
	@Mapping(target = "id", ignore = true)
	void mapFullUpdate(Location input, @MappingTarget Location target);
}
