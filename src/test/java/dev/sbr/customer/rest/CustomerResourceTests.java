package dev.sbr.customer.rest;

import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.sbr.customer.model.Customer;
import dev.sbr.customer.service.CustomerService;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.HttpHeaders;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;


import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;

@QuarkusTest
public class CustomerResourceTests {
	private static final String DEFAULT_NAME = "John Doe";
	private static final String UPDATED_NAME = DEFAULT_NAME + " (updated)";
	private static final String DEFAULT_OTHER_NAME = "Doe Junior";
	private static final String UPDATED_OTHER_NAME = DEFAULT_OTHER_NAME + " (updated)";
	private static final String DEFAULT_ADDRESS = "123 St. Jones Str.";
	private static final String UPDATED_ADDRESS = DEFAULT_ADDRESS + " (updated)";
	private static final Long DEFAULT_ID = 1L;


	@InjectMock
	CustomerService customerService;

	@BeforeAll
	static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@Test
	void helloEndpoint() {
		get("/api/customers/hello")
			.then()
				.statusCode(200)
				.body(is("Hello Customer Resource"));

		verifyNoInteractions(this.customerService);
	}

	@Test
	void shouldNotGetUnknownCustomer() {
		when(this.customerService.findCustomerById(DEFAULT_ID))
			.thenReturn(Uni.createFrom().nullItem());

		get("/api/customers/{id}", DEFAULT_ID)
			.then()
			.statusCode(NOT_FOUND.getStatusCode());

		verify(this.customerService).findCustomerById(DEFAULT_ID);
		verifyNoMoreInteractions(this.customerService);
	}

	@Test
	void shouldGetRandomCustomerNotFound() {
		when(this.customerService.findRandomCustomer())
			.thenReturn(Uni.createFrom().nullItem());

		get("/api/customers/random")
			.then()
			.statusCode(NOT_FOUND.getStatusCode());

		verify(this.customerService).findRandomCustomer();
		verifyNoMoreInteractions(this.customerService);
	}

	@Test
	void shouldGetRandomCustomerFound() {
		when(this.customerService.findRandomCustomer())
			.thenReturn(Uni.createFrom().item(createDefaultCustomer()));

		var customer = get("/api/customers/random")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
        .extract().as(Customer.class);

    assertThat(customer)
      .isNotNull()
      .extracting(
        Customer::getId,
        Customer::getCustomerName,
   	    Customer::getOtherName,
		Customer::getCustomerAddress
      )
      .containsExactly(
        DEFAULT_ID,
        DEFAULT_NAME,
        DEFAULT_OTHER_NAME,
        DEFAULT_ADDRESS
      );

		verify(this.customerService).findRandomCustomer();
		verifyNoMoreInteractions(this.customerService);
	}

	@Test
	void shouldNotAddInvalidItem() {
		var customer = new Customer();
		customer.setCustomerName(null);
		customer.setOtherName(DEFAULT_OTHER_NAME);
		customer.setCustomerAddress(DEFAULT_ADDRESS);

		given()
			.when()
				.body(customer)
				.contentType(JSON)
				.accept(JSON)
				.post("/api/customers")
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.customerService);
	}

	@Test
	void shouldNotAddNullItem() {
		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.post("/api/customers")
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.customerService);
	}

	@Test
	void shouldNotFullyUpdateNullItem() {
		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.body("")
				.put("/api/customers/{id}", DEFAULT_ID)
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.customerService);
	}

	@Test
	void shouldNotFullyUpdateInvalidItem() {
		var customer = createFullyUpdatedCustomer();
		customer.setCustomerName(null);
		customer.setOtherName(UPDATED_OTHER_NAME);
		customer.setCustomerAddress(UPDATED_ADDRESS);

		given()
			.when()
				.body(customer)
				.contentType(JSON)
				.accept(JSON)
				.put("/api/customers/{id}", customer.getId())
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.customerService);
	}

	@Test
	void shouldNotPartiallyUpdateInvalidItem() {
		ArgumentMatcher<Customer> customerMatcher = h ->
			(h.getId() == DEFAULT_ID) &&
				(h.getCustomerName() == null) &&
				h.getOtherName().equals(UPDATED_OTHER_NAME) &&
				h.getCustomerAddress().equals(UPDATED_ADDRESS);

		when(this.customerService.partialUpdateCustomer(argThat(customerMatcher)))
			.thenReturn(Uni.createFrom().failure(new ConstraintViolationException(Set.of())));

		var customer = createPartiallyUpdatedCustomer();
		customer.setCustomerName(null);
		customer.setOtherName(UPDATED_OTHER_NAME);

		given()
			.when()
				.body(customer)
				.contentType(JSON)
				.accept(JSON)
				.patch("/api/customers/{id}", DEFAULT_ID)
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verify(this.customerService).partialUpdateCustomer(argThat(customerMatcher));
		verifyNoMoreInteractions(this.customerService);
	}

	@Test
	void shouldNotPartiallyUpdateNullItem() {
		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.body("")
				.patch("/api/customers/{id}", DEFAULT_ID)
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.customerService);
	}

	@Test
	void shouldGetItems() {
		when(this.customerService.findAllCustomers())
			.thenReturn(Uni.createFrom().item(List.of(createDefaultCustomer())));

		var customers = get("/api/customers")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
        .extract().body()
        .jsonPath().getList(".", Customer.class);

    assertThat(customers)
      .singleElement()
      .extracting(
        Customer::getId,
        Customer::getCustomerName,
        Customer::getOtherName,
        Customer::getCustomerAddress
      )
      .containsExactly(
        DEFAULT_ID,
        DEFAULT_NAME,
        DEFAULT_OTHER_NAME,
        DEFAULT_ADDRESS
      );

		verify(this.customerService).findAllCustomers();
		verifyNoMoreInteractions(this.customerService);
	}

	@Test
	void shouldGetEmptyItems() {
		when(this.customerService.findAllCustomers())
			.thenReturn(Uni.createFrom().item(List.of()));

		get("/api/customers")
			.then()
				.statusCode(OK.getStatusCode())
				.body("$.size()", is(0));

		verify(this.customerService).findAllCustomers();
		verifyNoMoreInteractions(this.customerService);
	}

  @Test
  void shouldGetItemsWithNameFilter() {
    when(this.customerService.findAllCustomersHavingCustomerName("name"))
      .thenReturn(Uni.createFrom().item(List.of(createDefaultCustomer())));

    var customers = given()
      .when()
        .queryParam("name_filter", "name")
        .get("/api/customers")
      .then()
        .statusCode(OK.getStatusCode())
        .contentType(JSON)
        .extract().body()
        .jsonPath().getList(".", Customer.class);

    assertThat(customers)
      .singleElement()
      .extracting(
        Customer::getId,
        Customer::getCustomerName,
        Customer::getOtherName,
        Customer::getCustomerAddress
      )
      .containsExactly(
        DEFAULT_ID,
        DEFAULT_NAME,
        DEFAULT_OTHER_NAME,
        DEFAULT_ADDRESS
      );

    verify(this.customerService).findAllCustomersHavingCustomerName("name");
    verifyNoMoreInteractions(this.customerService);
  }

  @Test
  void shouldGetEmptyItemsWithNameFilter() {
    when(this.customerService.findAllCustomersHavingCustomerName("name"))
      .thenReturn(Uni.createFrom().item(List.of()));

    given()
      .when()
        .queryParam("name_filter", "name")
        .get("/api/customers")
      .then()
        .statusCode(OK.getStatusCode())
        .body("$.size()", is(0));

    verify(this.customerService).findAllCustomersHavingCustomerName("name");
    verifyNoMoreInteractions(this.customerService);
  }

	@Test
	void shouldGetNullItems() {
		when(this.customerService.findAllCustomers())
			.thenReturn(Uni.createFrom().nullItem());

		get("/api/customers").then()
			.statusCode(OK.getStatusCode())
			.body("$.size()", is(0));

		verify(this.customerService).findAllCustomers();
		verifyNoMoreInteractions(this.customerService);
	}

	@Test
	void shouldAddAnItem() {
		ArgumentMatcher<Customer> cutomerMatcher = h ->
			(h.getId() == null) &&
				h.getCustomerName().equals(DEFAULT_NAME) &&
				h.getOtherName().equals(DEFAULT_OTHER_NAME) &&
				h.getCustomerAddress().equals(DEFAULT_ADDRESS);

		when(this.customerService.persistCustomer(argThat(cutomerMatcher)))
			.thenReturn(Uni.createFrom().item(createDefaultCustomer()));

		var customer = new Customer();
		customer.setCustomerName(DEFAULT_NAME);
		customer.setOtherName(DEFAULT_OTHER_NAME);
		customer.setCustomerAddress(DEFAULT_ADDRESS);

		given()
			.when()
				.body(customer)
				.contentType(JSON)
				.accept(JSON)
				.post("/api/customers")
			.then()
				.statusCode(CREATED.getStatusCode())
				.header(HttpHeaders.LOCATION, containsString("/api/customers/" + DEFAULT_ID));

		verify(this.customerService).persistCustomer(argThat(cutomerMatcher));
		verifyNoMoreInteractions(this.customerService);
	}

	@Test
	void shouldNotFullyUpdateNotFoundItem() {
		var customer = createFullyUpdatedCustomer();
		ArgumentMatcher<Customer> customerMatcher = h ->
			(h.getId() == DEFAULT_ID) &&
				h.getCustomerName().equals(UPDATED_NAME) &&
				h.getOtherName().equals(UPDATED_OTHER_NAME) &&
				h.getCustomerAddress().equals(UPDATED_ADDRESS);

		when(this.customerService.replaceCustomer(argThat(customerMatcher)))
			.thenReturn(Uni.createFrom().nullItem());

		given()
			.when()
				.body(customer)
				.contentType(JSON)
				.accept(JSON)
				.put("/api/customers/{id}", customer.getId())
			.then()
				.statusCode(NOT_FOUND.getStatusCode())
				.body(blankOrNullString());

		verify(this.customerService).replaceCustomer(argThat(customerMatcher));
		verifyNoMoreInteractions(this.customerService);
	}

	@Test
	void shouldFullyUpdateAnItem() {
		var customer = createFullyUpdatedCustomer();
		ArgumentMatcher<Customer> customerMatcher = h ->
			(h.getId() == DEFAULT_ID) &&
				h.getCustomerName().equals(UPDATED_NAME) &&
				h.getOtherName().equals(UPDATED_OTHER_NAME) &&
				h.getCustomerAddress().equals(UPDATED_ADDRESS);

		when(this.customerService.replaceCustomer(argThat(customerMatcher)))
			.thenReturn(Uni.createFrom().item(customer));

		given()
			.when()
				.body(customer)
				.contentType(JSON)
				.accept(JSON)
				.put("/api/customers/{id}", customer.getId())
			.then()
				.statusCode(NO_CONTENT.getStatusCode())
				.body(blankOrNullString());

		verify(this.customerService).replaceCustomer(argThat(customerMatcher));
		verifyNoMoreInteractions(this.customerService);
	}

	@Test
	void shouldNotPartiallyUpdateNotFoundItem() {
		ArgumentMatcher<Customer> customerMatcher = h ->
			(h.getId() == DEFAULT_ID) &&
				(h.getCustomerName() == null) &&
				(h.getOtherName() == null) &&
				h.getCustomerAddress().equals(UPDATED_ADDRESS);

		var partialCustomer = new Customer();
		partialCustomer.setCustomerAddress(UPDATED_ADDRESS);

		when(this.customerService.partialUpdateCustomer(argThat(customerMatcher)))
			.thenReturn(Uni.createFrom().nullItem());

		given()
			.when()
				.body(partialCustomer)
				.contentType(JSON)
				.accept(JSON)
				.patch("/api/customers/{id}", DEFAULT_ID)
			.then()
				.statusCode(NOT_FOUND.getStatusCode())
				.body(blankOrNullString());

		verify(this.customerService).partialUpdateCustomer(argThat(customerMatcher));
		verifyNoMoreInteractions(this.customerService);
	}

	@Test
	void shouldPartiallyUpdateAnItem() {
		ArgumentMatcher<Customer> customerMatcher = h ->
			(h.getId() == DEFAULT_ID) &&
				(h.getCustomerName() == null) &&
				(h.getOtherName() == null) &&
				h.getCustomerAddress().equals(UPDATED_ADDRESS);

		var partialCustomer = new Customer();
		partialCustomer.setCustomerAddress(UPDATED_ADDRESS);

		when(this.customerService.partialUpdateCustomer(argThat(customerMatcher)))
			.thenReturn(Uni.createFrom().item(createPartiallyUpdatedCustomer()));

		var customer = given()
			.when()
				.body(partialCustomer)
				.contentType(JSON)
				.accept(JSON)
				.patch("/api/customers/{id}", DEFAULT_ID)
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
        .extract().as(Customer.class);

    assertThat(customer)
      .isNotNull()
      .extracting(
        Customer::getId,
        Customer::getCustomerName,
        Customer::getOtherName,
        Customer::getCustomerAddress
      )
      .containsExactly(
        DEFAULT_ID,
        DEFAULT_NAME,
        DEFAULT_OTHER_NAME,
        UPDATED_ADDRESS
      );

		verify(this.customerService).partialUpdateCustomer(argThat(customerMatcher));
		verifyNoMoreInteractions(this.customerService);
	}

	@Test
	void shouldDeleteCustomer() {
		when(this.customerService.deleteCustomer(DEFAULT_ID))
			.thenReturn(Uni.createFrom().voidItem());

		delete("/api/customers/{id}", DEFAULT_ID)
			.then()
				.statusCode(NO_CONTENT.getStatusCode())
				.body(blankOrNullString());

		verify(this.customerService).deleteCustomer(DEFAULT_ID);
		verifyNoMoreInteractions(this.customerService);
	}

  @Test
  void shouldreplaceAllCustomers() {
    var customers = List.of(createDefaultCustomer(), createFullyUpdatedCustomer());
    customers.forEach(h -> h.setId(null));

    ArgumentMatcher<List<Customer>> heroesMatcher = h ->
      (h.size() == 2) &&
			(h.get(0).getId() == null) &&
			h.get(0).getCustomerName().equals(DEFAULT_NAME) &&
			h.get(0).getOtherName().equals(DEFAULT_OTHER_NAME) &&
			h.get(0).getCustomerAddress().equals(DEFAULT_ADDRESS) &&
            (h.get(1).getId() == null) &&
			h.get(1).getCustomerName().equals(UPDATED_NAME) &&
			h.get(1).getOtherName().equals(UPDATED_OTHER_NAME) &&
			h.get(1).getCustomerAddress().equals(UPDATED_ADDRESS);

    when(this.customerService.replaceAllCustomers(argThat(heroesMatcher)))
      .thenReturn(Uni.createFrom().voidItem());

		given()
			.when()
				.body(customers)
				.contentType(JSON)
				.accept(JSON)
				.put("/api/customers")
			.then()
				.statusCode(CREATED.getStatusCode())
				.header(HttpHeaders.LOCATION, Matchers.endsWith("/api/customers"));

    verify(this.customerService).replaceAllCustomers(argThat(heroesMatcher));
    verifyNoMoreInteractions(this.customerService);
  }

	@Test
	void shouldDeleteAllCustomers() {
		when(this.customerService.deleteAllCustomers()).thenReturn(Uni.createFrom().voidItem());

		delete("/api/customers")
			.then()
				.statusCode(NO_CONTENT.getStatusCode())
				.body(blankOrNullString());

		verify(this.customerService).deleteAllCustomers();
		verifyNoMoreInteractions(this.customerService);
	}

	@Test
	void shouldPingOpenAPI() {
		get("/q/openapi")
			.then().statusCode(OK.getStatusCode());
	}

	@Test
	void shouldPingHealthCheck() {
		var expected = Map.of(
			"name", "Ping Customer REST Endpoint",
			"status", "UP",
			"data", Map.of("Response", "Hello Customer Resource")
		);

		var health = get("/q/health/live").then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
			.extract().as(Map.class);

		assertThat(health)
			.isNotNull()
			.containsKey("checks");

		var pingElement = ((List<Map<String, Object>>) health.get("checks")).stream()
			.filter(map -> "Ping Customer REST Endpoint".equals(map.get("name")))
			.findFirst();

		assertThat(pingElement)
			.isPresent();

		assertThat(pingElement.get())
			.containsAllEntriesOf(expected);
	}

	private static Customer createDefaultCustomer() {
		var customer = new Customer();
		customer.setId(DEFAULT_ID);
		customer.setCustomerName(DEFAULT_NAME);
		customer.setOtherName(DEFAULT_OTHER_NAME);
		customer.setCustomerAddress(DEFAULT_ADDRESS);

		return customer;
	}

	public static Customer createFullyUpdatedCustomer() {
		var customer = createDefaultCustomer();
		customer.setCustomerName(UPDATED_NAME);
		customer.setOtherName(UPDATED_OTHER_NAME);
		customer.setCustomerAddress(UPDATED_ADDRESS);

		return customer;
	}

	public static Customer createPartiallyUpdatedCustomer() {
		var customer = createDefaultCustomer();
		customer.setCustomerAddress(UPDATED_ADDRESS);

		return customer;
	}
}
