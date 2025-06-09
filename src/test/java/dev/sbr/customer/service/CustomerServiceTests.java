package dev.sbr.customer.service;

import dev.sbr.customer.mapping.CustomerFullUpdateMapper;
import dev.sbr.customer.mapping.CustomerPartialUpdateMapper;
import dev.sbr.customer.model.Customer;
import dev.sbr.customer.repository.CustomerRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.params.ParameterizedTest.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
class CustomerServiceTests {
    private static final String DEFAULT_NAME = "John Doe";
    private static final String UPDATED_NAME = DEFAULT_NAME + " (updated)";
    private static final String DEFAULT_OTHER_NAME = "Doe Junior";
    private static final String UPDATED_OTHER_NAME = DEFAULT_OTHER_NAME + " (updated)";
    private static final String DEFAULT_ADDRESS = "123 St. Jones Str.";
    private static final String UPDATED_ADDRESS = DEFAULT_ADDRESS + " (updated)";
    private static final Long DEFAULT_ID = 1L;

    @Inject
    CustomerService customerService;

    @InjectMock
    CustomerRepository customerRepository;

    @InjectSpy
    CustomerPartialUpdateMapper customerPartialUpdateMapper;

    @InjectSpy
    CustomerFullUpdateMapper customerFullUpdateMapper;

    @Test
    void findAllCustomersNoneFound() {
        when(this.customerRepository.listAll())
                .thenReturn(Uni.createFrom().item(List.of()));

        var allCustomers = this.customerService.findAllCustomers()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertSubscribed()
                .awaitItem(Duration.ofSeconds(5))
                .getItem();

        assertThat(allCustomers)
                .isNotNull()
                .isEmpty();

        verify(this.customerRepository).listAll();
        verifyNoMoreInteractions(this.customerRepository);
    }

    @Test
    void findAllCustomers() {
        when(this.customerRepository.listAll())
                .thenReturn(Uni.createFrom().item(List.of(createDefaultCustomer())));

        var allCustomers = this.customerService.findAllCustomers()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertSubscribed()
                .awaitItem(Duration.ofSeconds(5))
                .getItem();

        assertThat(allCustomers)
                .isNotNull()
                .isNotEmpty()
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

        verify(this.customerRepository).listAll();
        verifyNoMoreInteractions(this.customerRepository);
    }

    @ParameterizedTest(name = DISPLAY_NAME_PLACEHOLDER + "[" + INDEX_PLACEHOLDER + "] (" + ARGUMENTS_WITH_NAMES_PLACEHOLDER + ")")
    @ValueSource(strings = { "name" })
    @NullSource
    void findAllCustomersHavingNameNoneFound(String name) {
        when(this.customerRepository.listAllWhereCustomerNameLike(eq(name))).thenReturn(Uni.createFrom().item(List.of()));

        var allCustomers = this.customerService.findAllCustomersHavingCustomerName(name)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertSubscribed()
                .awaitItem(Duration.ofSeconds(5))
                .getItem();

        assertThat(allCustomers)
                .isNotNull()
                .isEmpty();

        verify(this.customerRepository).listAllWhereCustomerNameLike(eq(name));
        verifyNoMoreInteractions(this.customerRepository);
    }

    @Test
    void findAllCustomersHavingName() {
        when(this.customerRepository.listAllWhereCustomerNameLike(eq("name"))).thenReturn(Uni.createFrom().item(List.of(createDefaultCustomer())));

        var allCustomers = this.customerService.findAllCustomersHavingCustomerName("name")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertSubscribed()
                .awaitItem(Duration.ofSeconds(5))
                .getItem();

        assertThat(allCustomers)
                .isNotNull()
                .isNotEmpty()
                .hasSize(1)
                .extracting(
                        Customer::getId,
                        Customer::getCustomerName,
                        Customer::getOtherName,
                        Customer::getCustomerAddress
                )
                .containsExactly(
                        tuple(
                                DEFAULT_ID,
                                DEFAULT_NAME,
                                DEFAULT_OTHER_NAME,
                                DEFAULT_ADDRESS
                        )
                );

        verify(this.customerRepository).listAllWhereCustomerNameLike(eq("name"));
        verifyNoMoreInteractions(this.customerRepository);
    }

    @Test
    void findCustomerByIdFound() {
        when(this.customerRepository.findById(eq(DEFAULT_ID)))
                .thenReturn(Uni.createFrom().item(createDefaultCustomer()));

        var customer = this.customerService.findCustomerById(DEFAULT_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertSubscribed()
                .awaitItem(Duration.ofSeconds(5))
                .getItem();

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

        verify(this.customerRepository).findById(eq(DEFAULT_ID));
        verifyNoMoreInteractions(this.customerRepository);
    }

    @Test
    void findCustomerByIdNotFound() {
        when(this.customerRepository.findById(eq(DEFAULT_ID)))
                .thenReturn(Uni.createFrom().nullItem());

        var customer = this.customerService.findCustomerById(DEFAULT_ID)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertSubscribed()
                .awaitItem(Duration.ofSeconds(5))
                .getItem();

        assertThat(customer)
                .isNull();

        verify(this.customerRepository).findById(eq(DEFAULT_ID));
        verifyNoMoreInteractions(this.customerRepository);
    }

    @Test
    void findRandomCustomerNotFound() {
        when(this.customerRepository.findRandom())
                .thenReturn(Uni.createFrom().nullItem());

        var randomCustomer = this.customerService.findRandomCustomer()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertSubscribed()
                .awaitItem(Duration.ofSeconds(5))
                .getItem();

        assertThat(randomCustomer)
                .isNull();

        verify(this.customerRepository).findRandom();
        verifyNoMoreInteractions(this.customerRepository);
    }

    @Test
    void findRandomCustomerFound() {
        when(this.customerRepository.findRandom())
                .thenReturn(Uni.createFrom().item(createDefaultCustomer()));

        var randomCustomer = this.customerService.findRandomCustomer()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertSubscribed()
                .awaitItem(Duration.ofSeconds(5))
                .getItem();

        assertThat(randomCustomer)
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

        verify(this.customerRepository).findRandom();
        verifyNoMoreInteractions(this.customerRepository);
    }

    @Test
    @RunOnVertxContext
    void persistNullCustomer(UniAsserter asserter) {
        asserter.assertFailedWith(
                () -> this.customerService.persistCustomer(null),
                cve -> {
                    assertThat(cve)
                            .isNotNull()
                            .isInstanceOf(ConstraintViolationException.class);

                    var violations = ((ConstraintViolationException) cve).getConstraintViolations();

                    assertThat(violations)
                            .isNotNull()
                            .singleElement()
                            .isNotNull()
                            .extracting(
                                    ConstraintViolation::getInvalidValue,
                                    ConstraintViolation::getMessage
                            )
                            .containsExactly(
                                    null,
                                    "must not be null"
                            );

                    verifyNoInteractions(this.customerRepository);
                }
        );
    }

    @Test
    @RunOnVertxContext
    void persistInvalidCustomer(UniAsserter asserter) {
        var customer = createDefaultCustomer();
        customer.setCustomerName("a");

        asserter.assertFailedWith(
                () -> this.customerService.persistCustomer(customer),
                cve -> {
                    assertThat(cve)
                            .isNotNull()
                            .isInstanceOf(ConstraintViolationException.class);

                    var violations = ((ConstraintViolationException) cve).getConstraintViolations();

                    assertThat(violations)
                            .isNotNull()
                            .singleElement()
                            .isNotNull()
                            .extracting(
                                    ConstraintViolation::getInvalidValue,
                                    ConstraintViolation::getMessage
                            )
                            .containsExactly(
                                    "a",
                                    "size must be between 3 and 50"
                            );

                    verifyNoInteractions(this.customerRepository);
                }
        );
    }

    @Test
    @RunOnVertxContext
    void persistCustomer(UniAsserter asserter) {
        when(this.customerRepository.persist(any(Customer.class)))
                .thenReturn(Uni.createFrom().item(createDefaultCustomer()));

        var customerToPersist = createDefaultCustomer();
        customerToPersist.setId(null);

        asserter.assertThat(
                () -> this.customerService.persistCustomer(customerToPersist),
                persistedCustomer -> {
                    assertThat(persistedCustomer)
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

                    verify(this.customerRepository).persist(any(Customer.class));
                    verifyNoMoreInteractions(this.customerRepository);
                }
        );
    }

    @Test
    @RunOnVertxContext
    void fullyUpdateNullCustomer(UniAsserter asserter) {
        asserter.assertFailedWith(
                () -> this.customerService.replaceCustomer(null),
                cve -> {
                    assertThat(cve)
                            .isNotNull()
                            .isInstanceOf(ConstraintViolationException.class);

                    var violations = ((ConstraintViolationException) cve).getConstraintViolations();

                    assertThat(violations)
                            .isNotNull()
                            .singleElement()
                            .isNotNull()
                            .extracting(
                                    ConstraintViolation::getInvalidValue,
                                    ConstraintViolation::getMessage
                            )
                            .containsExactly(
                                    null,
                                    "must not be null"
                            );

                    verifyNoInteractions(this.customerRepository, this.customerFullUpdateMapper, this.customerPartialUpdateMapper);
                }
        );
    }

    @Test
    @RunOnVertxContext
    void fullyUpdateInvalidCustomer(UniAsserter asserter) {
        var customer = createDefaultCustomer();
        customer.setCustomerName(null);

        asserter.assertFailedWith(
                () -> this.customerService.replaceCustomer(customer),
                cve -> {
                    assertThat(cve)
                            .isNotNull()
                            .isInstanceOf(ConstraintViolationException.class);

                    var violations = ((ConstraintViolationException) cve).getConstraintViolations();

                    assertThat(violations)
                            .isNotNull()
                            .singleElement()
                            .isNotNull()
                            .extracting(
                                    ConstraintViolation::getInvalidValue,
                                    ConstraintViolation::getMessage
                            )
                            .containsExactly(
                                    null,
                                    "must not be null"
                            );

                    verifyNoInteractions(this.customerRepository, this.customerFullUpdateMapper, this.customerPartialUpdateMapper);
                }
        );
    }

    @Test
    @RunOnVertxContext
    void fullyUpdateNotFoundCustomer(UniAsserter asserter) {
        when(this.customerRepository.findById(eq(DEFAULT_ID)))
                .thenReturn(Uni.createFrom().nullItem());

        asserter.assertThat(
                () -> this.customerService.replaceCustomer(createUpdatedCustomer()),
                customer -> {
                    assertThat(customer)
                            .isNull();

                    verify(this.customerRepository).findById(eq(DEFAULT_ID));
                    verifyNoMoreInteractions(this.customerRepository);
                    verifyNoInteractions(this.customerPartialUpdateMapper, this.customerFullUpdateMapper);
                }
        );
    }

    @Test
    @RunOnVertxContext
    void fullyUpdateCustomer(UniAsserter asserter) {
        when(this.customerRepository.findById(eq(DEFAULT_ID)))
                .thenReturn(Uni.createFrom().item(createDefaultCustomer()));

        asserter.assertThat(
                () -> this.customerService.replaceCustomer(createUpdatedCustomer()),
                replacedCustomer -> {
                    assertThat(replacedCustomer)
                            .isNotNull()
                            .extracting(
                                    Customer::getId,
                                    Customer::getCustomerName,
                                    Customer::getOtherName,
                                    Customer::getCustomerAddress
                            )
                            .containsExactly(
                                    DEFAULT_ID,
                                    UPDATED_NAME,
                                    UPDATED_OTHER_NAME,
                                    UPDATED_ADDRESS
                            );

                    verify(this.customerRepository).findById(eq(DEFAULT_ID));
                    verifyNoMoreInteractions(this.customerRepository);
                    verify(this.customerFullUpdateMapper).mapFullUpdate(any(Customer.class), any(Customer.class));
                    verifyNoInteractions(this.customerPartialUpdateMapper);
                }
        );
    }

    @Test
    @RunOnVertxContext
    void partiallyUpdateNullCustomer(UniAsserter asserter) {
        asserter.assertFailedWith(
                () -> this.customerService.partialUpdateCustomer(null),
                cve -> {
                    assertThat(cve)
                            .isNotNull()
                            .isInstanceOf(ConstraintViolationException.class);

                    var violations = ((ConstraintViolationException) cve).getConstraintViolations();

                    assertThat(violations)
                            .isNotNull()
                            .singleElement()
                            .isNotNull()
                            .extracting(
                                    ConstraintViolation::getInvalidValue,
                                    ConstraintViolation::getMessage
                            )
                            .containsExactly(
                                    null,
                                    "must not be null"
                            );

                    verifyNoInteractions(this.customerRepository, this.customerFullUpdateMapper, this.customerPartialUpdateMapper);
                }
        );
    }

    @Test
    @RunOnVertxContext
    void partiallyUpdateInvalidCustomer(UniAsserter asserter) {
        when(this.customerRepository.findById(eq(DEFAULT_ID)))
                .thenReturn(Uni.createFrom().item(createDefaultCustomer()));

        var customer = createDefaultCustomer();
        customer.setCustomerName("a");

        asserter.assertFailedWith(
                () -> this.customerService.partialUpdateCustomer(customer),
                cve -> {
                    assertThat(cve)
                            .isNotNull()
                            .isInstanceOf(ConstraintViolationException.class);

                    var violations = ((ConstraintViolationException) cve).getConstraintViolations();

                    assertThat(violations)
                            .isNotNull()
                            .singleElement()
                            .isNotNull()
                            .extracting(
                                    ConstraintViolation::getInvalidValue,
                                    ConstraintViolation::getMessage
                            )
                            .containsExactly(
                                    "a",
                                    "size must be between 3 and 50"
                            );

                    verify(this.customerRepository).findById(eq(DEFAULT_ID));
                    verifyNoMoreInteractions(this.customerRepository);
                    verify(this.customerPartialUpdateMapper).mapPartialUpdate(any(Customer.class), any(Customer.class));
                    verifyNoInteractions(this.customerFullUpdateMapper);
                }
        );
    }

    @Test
    @RunOnVertxContext
    void partiallyUpdateNotFoundCustomer(UniAsserter asserter) {
        when(this.customerRepository.findById(eq(DEFAULT_ID)))
                .thenReturn(Uni.createFrom().nullItem());

        asserter.assertThat(
                () -> this.customerService.partialUpdateCustomer(createPartialUpdatedCustomer()),
                customer -> {
                    assertThat(customer)
                            .isNull();

                    verify(this.customerRepository).findById(eq(DEFAULT_ID));
                    verifyNoMoreInteractions(this.customerRepository);
                    verifyNoInteractions(this.customerFullUpdateMapper, this.customerPartialUpdateMapper);
                }
        );
    }

    @Test
    @RunOnVertxContext
    void partiallyUpdateCustomer(UniAsserter asserter) {
        when(this.customerRepository.findById(eq(DEFAULT_ID)))
                .thenReturn(Uni.createFrom().item(createDefaultCustomer()));

        asserter.assertThat(
                () -> this.customerService.partialUpdateCustomer(createPartialUpdatedCustomer()),
                customer -> {
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

                    verify(this.customerRepository).findById(eq(DEFAULT_ID));
                    verifyNoMoreInteractions(this.customerRepository);
                    verify(this.customerPartialUpdateMapper).mapPartialUpdate(any(Customer.class), any(Customer.class));
                    verifyNoInteractions(this.customerFullUpdateMapper);
                }
        );
    }

    @Test
    @RunOnVertxContext
    void deleteCustomer(UniAsserter asserter) {
        when(this.customerRepository.deleteById(eq(DEFAULT_ID)))
                .thenReturn(Uni.createFrom().item(true));

        asserter.assertThat(
                () -> this.customerService.deleteCustomer(DEFAULT_ID),
                v -> {
                    verify(this.customerRepository).deleteById(eq(DEFAULT_ID));
                    verifyNoMoreInteractions(this.customerRepository);
                }
        );
    }

    @Test
    @RunOnVertxContext
    void deleteAllCustomers(UniAsserter asserter) {
        var h1 = createDefaultCustomer();
        var h2 = createUpdatedCustomer();
        h2.setId(h1.getId() + 1);

        when(this.customerRepository.deleteById(anyLong()))
                .thenReturn(Uni.createFrom().item(true));

        when(this.customerRepository.listAll())
                .thenReturn(Uni.createFrom().item(List.of(h1, h2)));

        asserter.assertThat(
                () -> this.customerService.deleteAllCustomers(),
                v -> {
                    verify(this.customerRepository).listAll();
                    verify(this.customerRepository).deleteById(h1.getId());
                    verify(this.customerRepository).deleteById(h2.getId());
                    verifyNoMoreInteractions(this.customerRepository);
                }
        );
    }

    @Test
    @RunOnVertxContext
    void replaceAllCustomers(UniAsserter asserter) {
        var h1 = createDefaultCustomer();
        var h2 = createUpdatedCustomer();
        h2.setId(h1.getId() + 1);

        var customers = List.of(createDefaultCustomer(), createPartialUpdatedCustomer());
        customers.forEach(h -> h.setId(null));

        when(this.customerRepository.deleteById(anyLong()))
                .thenReturn(Uni.createFrom().item(true));

        when(this.customerRepository.listAll())
                .thenReturn(Uni.createFrom().item(List.of(h1, h2)));

        when(this.customerRepository.persist(anyIterable()))
                .thenReturn(Uni.createFrom().voidItem());

        asserter.assertThat(
                () -> this.customerService.replaceAllCustomers(customers),
                v -> {
                    verify(this.customerRepository).listAll();
                    verify(this.customerRepository).deleteById(eq(h1.getId()));
                    verify(this.customerRepository).deleteById(eq(h2.getId()));
                    verify(this.customerRepository).persist(anyIterable());
                    verifyNoMoreInteractions(this.customerRepository);
                }
        );
    }

    private static Customer createDefaultCustomer() {
        Customer customer = new Customer();
        customer.setId(DEFAULT_ID);
        customer.setCustomerName(DEFAULT_NAME);
        customer.setOtherName(DEFAULT_OTHER_NAME);
        customer.setCustomerAddress(DEFAULT_ADDRESS);

        return customer;
    }

    public static Customer createUpdatedCustomer() {
        Customer customer = createDefaultCustomer();
        customer.setCustomerName(UPDATED_NAME);
        customer.setOtherName(UPDATED_OTHER_NAME);
        customer.setCustomerAddress(UPDATED_ADDRESS);

        return customer;
    }

    public static Customer createPartialUpdatedCustomer() {
        Customer customer = createDefaultCustomer();
        customer.setCustomerAddress(UPDATED_ADDRESS);

        return customer;
    }
}

