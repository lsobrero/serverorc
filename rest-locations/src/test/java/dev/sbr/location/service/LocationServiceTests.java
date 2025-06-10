package dev.sbr.location.service;

import dev.sbr.location.mapping.LocationFullUpdateMapper;
import dev.sbr.location.mapping.LocationPartialUpdateMapper;
import dev.sbr.location.model.Location;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.ParameterizedTest.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class LocationServiceTests {
	private static final String DEFAULT_NAME = "Biarritz";
	private static final String UPDATED_NAME = DEFAULT_NAME + " (updated)";
	private static final Long DEFAULT_ID = 1L;

	@Inject
	LocationService locationService;


	@InjectSpy
	LocationPartialUpdateMapper locationPartialUpdateMapper;

	@InjectSpy
	LocationFullUpdateMapper locationFullUpdateMapper;

	@Test
	void findAllLocationsNoneFound() {
		PanacheMock.mock(Location.class);
		when(Location.listAll()).thenReturn(List.of());

		assertThat(this.locationService.findAllLocations())
			.isNotNull()
			.isEmpty();

		PanacheMock.verify(Location.class).listAll();
		PanacheMock.verifyNoMoreInteractions(Location.class);
	}

	@Test
	void findAllLocations() {
		PanacheMock.mock(Location.class);
		when(Location.listAll()).thenReturn(List.of(createDefaultVillian()));

		assertThat(this.locationService.findAllLocations())
			.isNotNull()
			.isNotEmpty()
			.hasSize(1)
			.extracting(
				"id",
				"name"
			)
			.containsExactly(
				tuple(
					DEFAULT_ID,
					DEFAULT_NAME
				)
			);

		PanacheMock.verify(Location.class).listAll();
		PanacheMock.verifyNoMoreInteractions(Location.class);
	}

  @ParameterizedTest(name = DISPLAY_NAME_PLACEHOLDER + "[" + INDEX_PLACEHOLDER + "] (" + ARGUMENTS_WITH_NAMES_PLACEHOLDER + ")")
  @ValueSource(strings = { "name" })
  @NullSource
  void findAllLocationsHavingNameNoneFound(String name) {
    PanacheMock.mock(Location.class);
    when(Location.listAllWhereNameLike(eq(name))).thenReturn(List.of());

    assertThat(this.locationService.findAllLocationsHavingName(name))
      .isNotNull()
      .isEmpty();

    PanacheMock.verify(Location.class).listAllWhereNameLike(eq(name));
    PanacheMock.verifyNoMoreInteractions(Location.class);
  }

  @Test
  void findAllLocationsHavingName() {
    PanacheMock.mock(Location.class);
    when(Location.listAllWhereNameLike(eq("name"))).thenReturn(List.of(createDefaultVillian()));

    assertThat(this.locationService.findAllLocationsHavingName("name"))
      .isNotNull()
      .isNotEmpty()
      .hasSize(1)
      .extracting(
        "id",
        "name"
      )
      .containsExactly(
        tuple(
          DEFAULT_ID,
          DEFAULT_NAME
        )
      );

    PanacheMock.verify(Location.class).listAllWhereNameLike(eq("name"));
    PanacheMock.verifyNoMoreInteractions(Location.class);
  }

	@Test
	void findLocationByIdFound() {
		PanacheMock.mock(Location.class);
		when(Location.findByIdOptional(eq(DEFAULT_ID)))
			.thenReturn(Optional.of(createDefaultVillian()));

		assertThat(this.locationService.findLocationById(DEFAULT_ID))
			.isNotNull()
			.isPresent()
			.get()
			.extracting(
				"id",
				"name"
			)
			.containsExactly(
				DEFAULT_ID,
				DEFAULT_NAME
			);

		PanacheMock.verify(Location.class).findByIdOptional(eq(DEFAULT_ID));
		PanacheMock.verifyNoMoreInteractions(Location.class);
	}

	@Test
	void findLocationByIdNotFound() {
		PanacheMock.mock(Location.class);
		when(Location.findByIdOptional(eq(DEFAULT_ID))).thenReturn(Optional.empty());

		assertThat(this.locationService.findLocationById(DEFAULT_ID))
			.isNotNull()
			.isNotPresent();

		PanacheMock.verify(Location.class).findByIdOptional(eq(DEFAULT_ID));
		PanacheMock.verifyNoMoreInteractions(Location.class);
	}

	@Test
	void findRandomLocationNotFound() {
		PanacheMock.mock(Location.class);
		when(Location.findRandom()).thenReturn(Optional.empty());

		assertThat(this.locationService.findRandomLocation())
			.isNotNull()
			.isEmpty();

		PanacheMock.verify(Location.class).findRandom();
		PanacheMock.verifyNoMoreInteractions(Location.class);
	}

	@Test
	void findRandomLocationFound() {
		PanacheMock.mock(Location.class);
		when(Location.findRandom()).thenReturn(Optional.of(createDefaultVillian()));

		assertThat(this.locationService.findRandomLocation())
			.isNotNull()
			.isPresent()
			.get()
			.extracting(
				"id",
				"name"
			)
			.containsExactly(
				DEFAULT_ID,
				DEFAULT_NAME
			);

		PanacheMock.verify(Location.class).findRandom();
		PanacheMock.verifyNoMoreInteractions(Location.class);
	}

	@Test
	void persistNullLocation() {
		PanacheMock.mock(Location.class);
		var cve = catchThrowableOfType(ConstraintViolationException.class, () -> this.locationService.persistLocation(null));

		assertThat(cve)
			.isNotNull();

		var violations = cve.getConstraintViolations();

		assertThat(violations)
			.isNotNull()
			.hasSize(1);

		assertThat(violations.stream().findFirst())
			.isNotNull()
			.isPresent()
			.get()
			.extracting(
				ConstraintViolation::getInvalidValue,
				ConstraintViolation::getMessage
			)
			.containsExactly(
				null,
				"must not be null"
			);

		PanacheMock.verifyNoInteractions(Location.class);
	}

	@Test
	void persistInvalidLocation() {
		PanacheMock.mock(Location.class);
		var location = createDefaultVillian();
		location.name = "a";

		var cve = catchThrowableOfType(ConstraintViolationException.class, () -> this.locationService.persistLocation(location));

		assertThat(cve)
			.isNotNull();

		var violations = cve.getConstraintViolations();

		assertThat(violations)
			.isNotNull()
			.hasSize(1);

		assertThat(violations.stream().findFirst())
			.isNotNull()
			.isPresent()
			.get()
			.extracting(
				ConstraintViolation::getInvalidValue,
				ConstraintViolation::getMessage
			)
			.containsExactly(
				"a",
				"size must be between 3 and 50"
			);

		PanacheMock.verifyNoInteractions(Location.class);
	}

	@Test
	void persistLocation() {
		PanacheMock.mock(Location.class);
		PanacheMock.doNothing()
			.when(Location.class).persist(any(Location.class), any());

		assertThat(this.locationService.persistLocation(createDefaultVillian()))
			.isNotNull()
			.extracting(
				"id",
				"name"
			)
			.containsExactly(
				DEFAULT_ID,
				DEFAULT_NAME
			);

		PanacheMock.verify(Location.class).persist(any(Location.class), any());
		PanacheMock.verifyNoMoreInteractions(Location.class);
	}

	@Test
	void fullyUpdateNullLocation() {
		PanacheMock.mock(Location.class);
		var cve = catchThrowableOfType(ConstraintViolationException.class, () -> this.locationService.replaceLocation(null));

		assertThat(cve)
			.isNotNull();

		var violations = cve.getConstraintViolations();

		assertThat(violations)
			.isNotNull()
			.hasSize(1);

		assertThat(violations.stream().findFirst())
			.isNotNull()
			.isPresent()
			.get()
			.extracting(
				ConstraintViolation::getInvalidValue,
				ConstraintViolation::getMessage
			)
			.containsExactly(
				null,
				"must not be null"
			);

		PanacheMock.verifyNoInteractions(Location.class);
		Mockito.verifyNoInteractions(this.locationFullUpdateMapper, this.locationPartialUpdateMapper);
	}

	@Test
	void fullyUpdateInvalidLocation() {
		PanacheMock.mock(Location.class);
		var location = createDefaultVillian();
		location.name = null;

		var cve = catchThrowableOfType(ConstraintViolationException.class, () -> this.locationService.replaceLocation(location));

		assertThat(cve)
			.isNotNull();

		var violations = cve.getConstraintViolations();

		assertThat(violations)
			.isNotNull()
			.hasSize(1);

		assertThat(violations.stream().findFirst())
			.isNotNull()
			.isPresent()
			.get()
			.extracting(
				ConstraintViolation::getInvalidValue,
				ConstraintViolation::getMessage
			)
			.containsExactly(
				null,
				"must not be null"
			);

		PanacheMock.verifyNoInteractions(Location.class);
		Mockito.verifyNoInteractions(this.locationFullUpdateMapper, this.locationPartialUpdateMapper);
	}

	@Test
	void fullyUpdateNotFoundLocation() {
		PanacheMock.mock(Location.class);
		when(Location.findByIdOptional(eq(DEFAULT_ID))).thenReturn(Optional.empty());

		assertThat(this.locationService.replaceLocation(createUpdatedLocation()))
			.isNotNull()
			.isNotPresent();

		PanacheMock.verify(Location.class).findByIdOptional(eq(DEFAULT_ID));
		PanacheMock.verifyNoMoreInteractions(Location.class);
		Mockito.verifyNoInteractions(this.locationPartialUpdateMapper, this.locationFullUpdateMapper);
	}

	@Test
	void fullyUpdateLocation() {
		PanacheMock.mock(Location.class);
		when(Location.findByIdOptional(eq(DEFAULT_ID))).thenReturn(Optional.of(createDefaultVillian()));

		assertThat(this.locationService.replaceLocation(createUpdatedLocation()))
			.isNotNull()
			.isPresent()
			.get()
			.extracting(
				"id",
				"name"
			)
			.containsExactly(
				DEFAULT_ID,
				UPDATED_NAME
			);

		PanacheMock.verify(Location.class).findByIdOptional(eq(DEFAULT_ID));
		PanacheMock.verifyNoMoreInteractions(Location.class);
		Mockito.verify(this.locationFullUpdateMapper).mapFullUpdate(any(Location.class), any(Location.class));
		Mockito.verifyNoInteractions(this.locationPartialUpdateMapper);
	}

	@Test
	void partiallyUpdateNullLocation() {
		PanacheMock.mock(Location.class);
		var cve = catchThrowableOfType(ConstraintViolationException.class, () -> this.locationService.partialUpdateLocation(null));

		assertThat(cve)
			.isNotNull();

		var violations = cve.getConstraintViolations();

		assertThat(violations)
			.isNotNull()
			.hasSize(1);

		assertThat(violations.stream().findFirst())
			.isNotNull()
			.isPresent()
			.get()
			.extracting(
				ConstraintViolation::getInvalidValue,
				ConstraintViolation::getMessage
			)
			.containsExactly(
				null,
				"must not be null"
			);

		PanacheMock.verifyNoInteractions(Location.class);
		Mockito.verifyNoInteractions(this.locationFullUpdateMapper, this.locationPartialUpdateMapper);
	}

	@Test
	void partiallyUpdateInvalidLocation() {
		PanacheMock.mock(Location.class);
		when(Location.findByIdOptional(eq(DEFAULT_ID))).thenReturn(Optional.of(createDefaultVillian()));
		var location = createDefaultVillian();
		location.name = "a";

		var cve = catchThrowableOfType(ConstraintViolationException.class, () -> this.locationService.partialUpdateLocation(location));

		assertThat(cve)
			.isNotNull();

		var violations = cve.getConstraintViolations();

		assertThat(violations)
			.isNotNull()
			.hasSize(1);

		assertThat(violations.stream().findFirst())
			.isNotNull()
			.isPresent()
			.get()
			.extracting(
				ConstraintViolation::getInvalidValue,
				ConstraintViolation::getMessage
			)
			.containsExactly(
				"a",
				"size must be between 3 and 50"
			);

		PanacheMock.verify(Location.class).findByIdOptional(eq(DEFAULT_ID));
		PanacheMock.verifyNoMoreInteractions(Location.class);
		Mockito.verify(this.locationPartialUpdateMapper).mapPartialUpdate(any(Location.class), any(Location.class));
		Mockito.verifyNoInteractions(this.locationFullUpdateMapper);
	}

	@Test
	void partiallyUpdateNotFoundLocation() {
		PanacheMock.mock(Location.class);
		when(Location.findByIdOptional(eq(DEFAULT_ID))).thenReturn(Optional.empty());

		assertThat(this.locationService.partialUpdateLocation(createPartialUpdatedLocation()))
			.isNotNull()
			.isNotPresent();

		PanacheMock.verify(Location.class).findByIdOptional(eq(DEFAULT_ID));
		PanacheMock.verifyNoMoreInteractions(Location.class);
		Mockito.verifyNoInteractions(this.locationFullUpdateMapper, this.locationPartialUpdateMapper);
	}

	@Test
	void partiallyUpdateLocation() {
		PanacheMock.mock(Location.class);
		when(Location.findByIdOptional(eq(DEFAULT_ID))).thenReturn(Optional.of(createDefaultVillian()));

		assertThat(this.locationService.partialUpdateLocation(createPartialUpdatedLocation()))
			.isNotNull()
			.isPresent()
			.get()
			.extracting(
				"id",
				"name"
			)
			.containsExactly(
				DEFAULT_ID,
				DEFAULT_NAME
			);

		PanacheMock.verify(Location.class).findByIdOptional(eq(DEFAULT_ID));
		PanacheMock.verifyNoMoreInteractions(Location.class);
		Mockito.verify(this.locationPartialUpdateMapper).mapPartialUpdate(any(Location.class), any(Location.class));
		Mockito.verifyNoInteractions(this.locationFullUpdateMapper);
	}

	@Test
	void deleteLocation() {
		PanacheMock.mock(Location.class);
		when(Location.deleteById(eq(DEFAULT_ID))).thenReturn(true);

		this.locationService.deleteLocation(DEFAULT_ID);

		PanacheMock.verify(Location.class).deleteById(eq(DEFAULT_ID));
		PanacheMock.verifyNoMoreInteractions(Location.class);
	}

	@Test
	void deleteAllLocations() {
		var v1 = createDefaultVillian();
		var v2 = createUpdatedLocation();
		v2.id = v1.id + 1;

		PanacheMock.mock(Location.class);
		when(Location.deleteById(anyLong())).thenReturn(true);
		when(Location.listAll()).thenReturn(List.of(v1, v2));

		this.locationService.deleteAllLocations();

		PanacheMock.verify(Location.class).listAll();
		PanacheMock.verify(Location.class).deleteById(eq(v1.id));
		PanacheMock.verify(Location.class).deleteById(eq(v2.id));
		PanacheMock.verifyNoMoreInteractions(Location.class);
	}

  @Test
  void replaceAllLocations() {
    var v1 = createDefaultVillian();
		var v2 = createUpdatedLocation();
		v2.id = v1.id + 1;

    var locations = List.of(createDefaultVillian(), createPartialUpdatedLocation());
    locations.forEach(v -> v.id = null);

		PanacheMock.mock(Location.class);
		when(Location.deleteById(anyLong())).thenReturn(true);
		when(Location.listAll()).thenReturn(List.of(v1, v2));
    PanacheMock.doNothing().when(Location.class).persist(anyIterable());

    this.locationService.replaceAllLocations(locations);

    PanacheMock.verify(Location.class).listAll();
		PanacheMock.verify(Location.class).deleteById(eq(v1.id));
		PanacheMock.verify(Location.class).deleteById(eq(v2.id));
		PanacheMock.verify(Location.class).persist(anyIterable());
		PanacheMock.verifyNoMoreInteractions(Location.class);
  }

	private static Location createDefaultVillian() {
		Location location = new Location();
		location.id = DEFAULT_ID;
		location.name = DEFAULT_NAME;

		return location;
	}

	public static Location createUpdatedLocation() {
		Location location = createDefaultVillian();
		location.name = UPDATED_NAME;

		return location;
	}

	public static Location createPartialUpdatedLocation() {
		Location location = createDefaultVillian();

		return location;
	}
}
