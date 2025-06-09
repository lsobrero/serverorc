package dev.sbr.location.service;

import dev.sbr.location.mapping.LocationFullUpdateMapper;
import dev.sbr.location.mapping.LocationPartialUpdateMapper;
import dev.sbr.location.model.Location;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;

import static jakarta.transaction.Transactional.TxType.REQUIRED;
import static jakarta.transaction.Transactional.TxType.SUPPORTS;

/**
 * Service class containing business methods for the application.
 */
@ApplicationScoped
@Transactional(REQUIRED)
public class LocationService {
  @Inject
	Validator validator;

  @Inject
  LocationPartialUpdateMapper locationPartialUpdateMapper;

  @Inject
  LocationFullUpdateMapper locationFullUpdateMapper;

  @Transactional(SUPPORTS)
  @WithSpan("LocationService.findAllLocations")
	public List<Location> findAllLocations() {
    Log.debug("Getting all locations");
    return Optional.ofNullable(Location.<Location>listAll())
      .orElseGet(List::of);
	}

  @Transactional(SUPPORTS)
  @WithSpan("LocationService.findAllLocationsHavingName")
  public List<Location> findAllLocationsHavingName(@SpanAttribute("arg.name") String name) {
    Log.debugf("Finding all locations having name = %s", name);
    return Optional.ofNullable(Location.listAllWhereNameLike(name))
      .orElseGet(List::of);
  }

  @Transactional(SUPPORTS)
  @WithSpan("LocationService.findLocationById")
  public Optional<Location> findLocationById(@SpanAttribute("arg.id") Long id) {
    Log.debugf("Finding location by id = %d", id);
    return Location.findByIdOptional(id);
  }

  @Transactional(SUPPORTS)
  @WithSpan("LocationService.findRandomLocation")
	public Optional<Location> findRandomLocation() {
    Log.debug("Finding a random location");
		return Location.findRandom();
	}

  @WithSpan("LocationService.persistLocation")
	public Location persistLocation(@SpanAttribute("arg.location") @NotNull @Valid Location location) {
    Log.debugf("Persisting location: %s", location);
		Location.persist(location);

		return location;
	}

  @WithSpan("LocationService.replaceLocation")
	public Optional<Location> replaceLocation(@SpanAttribute("arg.location") @NotNull @Valid Location location) {
    Log.debugf("Replacing location: %s", location);
		return Location.findByIdOptional(location.id)
			.map(Location.class::cast) // Only here for type erasure within the IDE
			.map(v -> {
				this.locationFullUpdateMapper.mapFullUpdate(location, v);
				return v;
			});
	}

  @WithSpan("LocationService.partialUpdateLocation")
	public Optional<Location> partialUpdateLocation(@SpanAttribute("arg.location") @NotNull Location location) {
    Log.debugf("Partially updating location: %s", location);
		return Location.findByIdOptional(location.id)
			.map(Location.class::cast) // Only here for type erasure within the IDE
			.map(v -> {
				this.locationPartialUpdateMapper.mapPartialUpdate(location, v);
				return v;
			})
			.map(this::validatePartialUpdate);
	}

  @WithSpan("LocationService.replaceAllLocations")
  public void replaceAllLocations(@SpanAttribute("arg.locations") List<Location> locations) {
    Log.debug("Replacing all locations");
    deleteAllLocations();
    Location.persist(locations);
  }

	/**
	 * Validates a {@link Location} for a partial update according to annotated validation rules on the {@link Location} object.
	 * @param location The {@link Location}
	 * @return The same {@link Location} that was passed in, assuming it passes validation. The return is used as a convenience so the method can be called in a functional pipeline.
	 * @throws ConstraintViolationException If validation fails
	 */
	private Location validatePartialUpdate(Location location) {
		var violations = this.validator.validate(location);

		if ((violations != null) && !violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}

		return location;
	}

  @WithSpan("LocationService.deleteAllLocations")
	public void deleteAllLocations() {
    Log.debug("Deleting all locations");
		List<Location> locations = Location.listAll();
		locations.stream()
			.map(v -> v.id)
			.forEach(this::deleteLocation);
	}

  @WithSpan("LocationService.deleteLocation")
	public void deleteLocation(@SpanAttribute("arg.id") Long id) {
    Log.debugf("Deleting location by id = %d", id);
		Location.deleteById(id);
	}
}
