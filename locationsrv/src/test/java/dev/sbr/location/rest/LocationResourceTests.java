package dev.sbr.location.rest;

import dev.sbr.location.model.Location;
import dev.sbr.location.service.LocationService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit5.virtual.ShouldNotPin;
import io.quarkus.test.junit5.virtual.VirtualThreadUnit;
import io.restassured.RestAssured;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.HttpHeaders;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.ContentType.TEXT;
import static jakarta.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@QuarkusTest
@VirtualThreadUnit
@ShouldNotPin
class LocationResourceTests {
	private static final String DEFAULT_NAME = "Biarritz";
	private static final String UPDATED_NAME = DEFAULT_NAME + " (updated)";
	private static final long DEFAULT_ID = 1;

	@InjectMock
	LocationService locationService;

	@BeforeAll
	static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@Test
	void helloEndpoint() {
		get("/api/locations/hello")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(TEXT)
				.body(is("Hello Location Resource"));

		verifyNoInteractions(this.locationService);
	}

	@Test
	void shouldNotGetUnknownVillain() {
		when(this.locationService.findLocationById(DEFAULT_ID))
			.thenReturn(Optional.empty());

		get("/api/locations/{id}", DEFAULT_ID)
			.then().statusCode(NOT_FOUND.getStatusCode());

		verify(this.locationService).findLocationById(DEFAULT_ID);
		verifyNoMoreInteractions(this.locationService);
	}

	@Test
	void shouldGetRandomLocationNotFound() {
		when(this.locationService.findRandomLocation())
			.thenReturn(Optional.empty());

		get("/api/locations/random")
			.then().statusCode(NOT_FOUND.getStatusCode());

		verify(this.locationService).findRandomLocation();
		verifyNoMoreInteractions(this.locationService);
	}

	@Test
	void shouldGetRandomLocationFound() {
		when(this.locationService.findRandomLocation())
			.thenReturn(Optional.of(createDefaultLocation()));

		var location = get("/api/locations/random")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
        .extract().as(Location.class);

    var defaultLocation = new Location();
    defaultLocation.id = DEFAULT_ID;
    defaultLocation.name = DEFAULT_NAME;

    assertThat(location)
      .isNotNull()
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes(".*_hibernate_.*")
      .isEqualTo(defaultLocation);

		verify(this.locationService).findRandomLocation();
		verifyNoMoreInteractions(this.locationService);
	}

	@Test
	void shouldNotAddInvalidItem() {
		var location = new Location();
		location.name = null;

		given()
			.when()
				.body(location)
				.contentType(JSON)
				.accept(JSON)
				.post("/api/locations")
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.locationService);
	}

	@Test
	void shouldNotAddNullItem() {
		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.post("/api/locations")
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.locationService);
	}

	@Test
	void shouldNotFullyUpdateNullItem() {
		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.body("")
				.put("/api/locations/{id}", DEFAULT_ID)
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.locationService);
	}

	@Test
	void shouldNotFullyUpdateInvalidItem() {
		var location = createFullyUpdatedLocation();
		location.name = null;

		given()
			.when()
				.body(location)
				.contentType(JSON)
				.accept(JSON)
				.put("/api/locations/{id}", location.id)
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.locationService);
	}

	@Test
	void shouldNotPartiallyUpdateInvalidItem() {
		ArgumentMatcher<Location> locationMatcher = v ->
			(v.id == DEFAULT_ID) &&
				(v.name == null);

		when(this.locationService.partialUpdateLocation(argThat(locationMatcher)))
			.thenThrow(new ConstraintViolationException(Set.of()));

		var location = createPartiallyUpdatedLocation();
		location.name = null;

		given()
			.when()
				.body(location)
				.contentType(JSON)
				.accept(JSON)
				.patch("/api/locations/{id}", DEFAULT_ID)
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verify(this.locationService).partialUpdateLocation(argThat(locationMatcher));
		verifyNoMoreInteractions(this.locationService);
	}

	@Test
	void shouldNotPartiallyUpdateNullItem() {
		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.body("")
				.patch("/api/locations/{id}", DEFAULT_ID)
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.locationService);
	}

	@Test
	void shouldGetItems() {
		when(this.locationService.findAllLocations())
			.thenReturn(List.of(createDefaultLocation()));

    var defaultLocation = new Location();
    defaultLocation.id = DEFAULT_ID;
    defaultLocation.name = DEFAULT_NAME;

		var locations = get("/api/locations")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
        .extract().body()
        .jsonPath().getList(".", Location.class);

    assertThat(locations)
      .singleElement()
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes(".*_hibernate_.*")
      .isEqualTo(defaultLocation);

		verify(this.locationService).findAllLocations();
		verifyNoMoreInteractions(this.locationService);
	}

	@Test
	void shouldGetEmptyItems() {
		when(this.locationService.findAllLocations())
			.thenReturn(List.of());

		get("/api/locations")
			.then()
				.statusCode(OK.getStatusCode())
				.body("$.size()", is(0));

		verify(this.locationService).findAllLocations();
		verifyNoMoreInteractions(this.locationService);
	}

  @Test
  void shouldGetItemsWithNameFilter() {
    when(this.locationService.findAllLocationsHavingName("name"))
      .thenReturn(List.of(createDefaultLocation()));

    var defaultLocation = new Location();
    defaultLocation.id = DEFAULT_ID;
    defaultLocation.name = DEFAULT_NAME;

    var locations = given()
      .when()
        .queryParam("name_filter", "name")
        .get("/api/locations")
      .then()
        .statusCode(OK.getStatusCode())
        .contentType(JSON)
        .extract().body()
        .jsonPath().getList(".", Location.class);

    assertThat(locations)
      .singleElement()
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes(".*_hibernate_.*")
      .isEqualTo(defaultLocation);

    verify(this.locationService).findAllLocationsHavingName("name");
    verifyNoMoreInteractions(this.locationService);
  }

  @Test
  void shouldGetEmptyItemsWithNameFilter() {
    when(this.locationService.findAllLocationsHavingName("name"))
      .thenReturn(List.of());

    given()
      .when()
        .queryParam("name_filter", "name")
        .get("/api/locations")
      .then()
        .statusCode(OK.getStatusCode())
        .body("$.size()", is(0));

    verify(this.locationService).findAllLocationsHavingName("name");
    verifyNoMoreInteractions(this.locationService);
  }

	@Test
	void shouldAddAnItem() {
		ArgumentMatcher<Location> locationMatcher = v ->
			(v.id == null) &&
			v.name.equals(DEFAULT_NAME);

		when(this.locationService.persistLocation(argThat(locationMatcher)))
			.thenReturn(createDefaultLocation());

		var location = new Location();
		location.name = DEFAULT_NAME;

		given()
			.when()
				.body(location)
				.contentType(JSON)
				.accept(JSON)
				.post("/api/locations")
			.then()
				.statusCode(CREATED.getStatusCode())
				.header(HttpHeaders.LOCATION, containsString("/api/locations/" + DEFAULT_ID));

		verify(this.locationService).persistLocation(argThat(locationMatcher));
		verifyNoMoreInteractions(this.locationService);
	}

	@Test
	void shouldNotFullyUpdateNotFoundItem() {
		var location = createFullyUpdatedLocation();
		ArgumentMatcher<Location> locationMatcher = v ->
			(v.id == DEFAULT_ID) &&
				v.name.equals(UPDATED_NAME);

		when(this.locationService.replaceLocation(argThat(locationMatcher)))
			.thenReturn(Optional.empty());

		given()
			.when()
				.body(location)
				.contentType(JSON)
				.accept(JSON)
				.put("/api/locations/{id}", location.id)
			.then()
				.statusCode(NOT_FOUND.getStatusCode())
				.body(blankOrNullString());

		verify(this.locationService).replaceLocation(argThat(locationMatcher));
		verifyNoMoreInteractions(this.locationService);
	}

	@Test
	void shouldFullyUpdateAnItem() {
		var location = createFullyUpdatedLocation();
		ArgumentMatcher<Location> locationMatcher = v ->
			(v.id == DEFAULT_ID) &&
				v.name.equals(UPDATED_NAME);

		when(this.locationService.replaceLocation(argThat(locationMatcher)))
			.thenReturn(Optional.of(location));

		given()
			.when()
				.body(location)
				.contentType(JSON)
				.accept(JSON)
				.put("/api/locations/{id}", location.id)
			.then()
				.statusCode(NO_CONTENT.getStatusCode())
				.body(blankOrNullString());

		verify(this.locationService).replaceLocation(argThat(locationMatcher));
		verifyNoMoreInteractions(this.locationService);
	}

	@Test
	void shouldNotPartiallyUpdateNotFoundItem() {
		ArgumentMatcher<Location> locationMatcher = v ->
			(v.id == DEFAULT_ID) &&
				(v.name == null);

		var partialLocation = new Location();

		when(this.locationService.partialUpdateLocation(argThat(locationMatcher)))
			.thenReturn(Optional.empty());

		given()
			.when()
				.body(partialLocation)
				.contentType(JSON)
				.accept(JSON)
				.patch("/api/locations/{id}", DEFAULT_ID)
			.then()
				.statusCode(NOT_FOUND.getStatusCode())
				.body(blankOrNullString());

		verify(this.locationService).partialUpdateLocation(argThat(locationMatcher));
		verifyNoMoreInteractions(this.locationService);
	}

	@Test
	void shouldPartiallyUpdateAnItem() {
		ArgumentMatcher<Location> locationMatcher = v ->
			(v.id == DEFAULT_ID) &&
				(v.name == null);

		var partialLocation = new Location();

    var defaultLocation = new Location();
    defaultLocation.id = DEFAULT_ID;
    defaultLocation.name = DEFAULT_NAME;

		when(this.locationService.partialUpdateLocation(argThat(locationMatcher)))
			.thenReturn(Optional.of(createPartiallyUpdatedLocation()));

		var location = given()
			.when()
				.body(partialLocation)
				.contentType(JSON)
				.accept(JSON)
				.patch("/api/locations/{id}", DEFAULT_ID)
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
        .extract().as(Location.class);

    assertThat(location)
      .isNotNull()
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes(".*_hibernate_.*")
      .isEqualTo(defaultLocation);

		verify(this.locationService).partialUpdateLocation(argThat(locationMatcher));
		verifyNoMoreInteractions(this.locationService);
	}

	@Test
	void shouldDeleteLocation() {
		doNothing()
			.when(this.locationService)
			.deleteLocation(DEFAULT_ID);

		delete("/api/locations/{id}", DEFAULT_ID)
			.then()
				.statusCode(NO_CONTENT.getStatusCode())
				.body(blankOrNullString());

		verify(this.locationService).deleteLocation(DEFAULT_ID);
		verifyNoMoreInteractions(this.locationService);
	}

	@Test
	void shouldDeleteAllLocations() {
		doNothing()
			.when(this.locationService)
			.deleteAllLocations();

		delete("/api/locations")
			.then()
			.statusCode(NO_CONTENT.getStatusCode())
			.body(blankOrNullString());

		verify(this.locationService).deleteAllLocations();
		verifyNoMoreInteractions(this.locationService);
	}

  @Test
  void shouldReplaceAllLocations() {
    var locations = List.of(createDefaultLocation(), createFullyUpdatedLocation());
    locations.forEach(v -> v.id = null);

    ArgumentMatcher<List<Location>> locationsMatcher = v ->
      (v.size() == 2) &&
			(v.get(0).id == null) &&
			v.get(0).name.equals(DEFAULT_NAME) &&
      (v.get(1).id == null) &&
			v.get(1).name.equals(UPDATED_NAME);

    doNothing()
      .when(this.locationService)
      .replaceAllLocations(argThat(locationsMatcher));

		given()
			.when()
				.body(locations)
				.contentType(JSON)
				.accept(JSON)
				.put("/api/locations")
			.then()
				.statusCode(CREATED.getStatusCode())
				.header(HttpHeaders.LOCATION, Matchers.endsWith("/api/locations"));

    verify(this.locationService).replaceAllLocations(argThat(locationsMatcher));
    verifyNoMoreInteractions(this.locationService);
  }

	@Test
	void shouldPingOpenAPI() {
		get("/q/openapi").then()
			.statusCode(OK.getStatusCode());
	}

	@Test
	void shouldPingHealthCheck() {
		var expected = Map.of(
			"name", "Ping Location REST Endpoint",
			"status", "UP",
			"data", Map.of("Response", "Hello Location Resource")
		);

		var health = get("/q/health/live").then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
			.extract().as(Map.class);

		assertThat(health)
			.isNotNull()
			.containsKey("checks");

		var pingElement = ((List<Map<String, Object>>) health.get("checks")).stream()
			.filter(map -> "Ping Location REST Endpoint".equals(map.get("name")))
			.findFirst();

		assertThat(pingElement)
			.isPresent();

		assertThat(pingElement.get())
			.containsAllEntriesOf(expected);
	}

	private static Location createDefaultLocation() {
		var location = new Location();
		location.id = DEFAULT_ID;
		location.name = DEFAULT_NAME;

		return location;
	}

	public static Location createFullyUpdatedLocation() {
		var location = createDefaultLocation();
		location.name = UPDATED_NAME;

		return location;
	}

	public static Location createPartiallyUpdatedLocation() {
		var location = createDefaultLocation();

		return location;
	}
}
