package dev.sbr.customer.repositories;

import dev.sbr.customer.repository.CustomerRepository;
import io.quarkus.logging.Log;
import io.quarkus.test.TestReactiveTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

import dev.sbr.customer.model.Customer;

@QuarkusTest
@TestReactiveTransaction
class CustomerRepositoryTest {
    private static final String DEFAULT_NAME = "John Doe";
    private static final String DEFAULT_OTHER_NAME = "Doe Junior";
    private static final String DEFAULT_CUSTOMER_ADDRESS = "123 St. Jones Str.";

    @Inject
    CustomerRepository customerRepository;

    @Test
    void findRandomNotFound(UniAsserter asserter) {
        asserter.execute(this.customerRepository::deleteAll)
                .assertEquals(this.customerRepository::count, 0L)
                .assertThat(
                        this.customerRepository::findRandom,
                        customer -> assertThat(customer).isNull()
                );
    }

    @Test
    void findRandomFound(UniAsserter asserter) {
        Customer customer = new Customer();
        customer.setCustomerName(DEFAULT_NAME);
        customer.setOtherName(DEFAULT_OTHER_NAME);
        customer.setCustomerAddress(DEFAULT_CUSTOMER_ADDRESS);

        asserter.execute(this.customerRepository::deleteAll)
                .assertEquals(this.customerRepository::count, 0L)
                .execute(() -> this.customerRepository.persist(customer))
                .assertEquals(this.customerRepository::count, 1L)
                .assertThat(
                        this.customerRepository::findRandom,
                        h -> {
                            assertThat(h)
                                    .isNotNull()
                                    .usingRecursiveComparison()
                                    .isEqualTo(customer);

                            assertThat(h.getId())
                                    .isNotNull()
                                    .isPositive();
                        }
                );
    }

    @Test
    void findAllWhereNameLikeFound(UniAsserter asserter) {
        // Doing it this way because UniAsserter doesn't work well with ParameterizedTest
        var names = Stream.of(DEFAULT_NAME, "choco", "Choco", "CHOCO", "Chocolatine", "super", "l", "");

        Customer customer = new Customer();
        customer.setCustomerName(DEFAULT_NAME);
        customer.setOtherName(DEFAULT_OTHER_NAME);
        customer.setCustomerAddress(DEFAULT_CUSTOMER_ADDRESS);

        asserter.execute(this.customerRepository::deleteAll)
                .assertEquals(this.customerRepository::count, 0L)
                .execute(() -> this.customerRepository.persist(customer))
                .assertEquals(this.customerRepository::count, 1L);

        names.forEach(name ->
                asserter.execute(() -> Log.infof("Inside listAllWhereNameLike(%s)", name))
                        .assertThat(
                                () -> this.customerRepository.listAllWhereCustomerNameLike(name),
                                customers ->
                                        assertThat(customers)
                                                .isNotNull()
                                                .hasSize(1)
                                                .first()
                                                .usingRecursiveComparison()
                                                .isEqualTo(customer)
                        )
        );
    }

    @Test
    void findAllWhereNameLikeNotFound(UniAsserter asserter) {
        // Doing it this way because UniAsserter doesn't work well with ParameterizedTest
        var names = Stream.of("v", "support", "chocolate", null);

        Customer customer = new Customer();
        customer.setCustomerName(DEFAULT_NAME);
        customer.setOtherName(DEFAULT_OTHER_NAME);

        asserter.execute(this.customerRepository::deleteAll)
                .assertEquals(this.customerRepository::count, 0L)
                .execute(() -> this.customerRepository.persist(customer))
                .assertEquals(this.customerRepository::count, 1L);

        names.forEach(name ->
                asserter.execute(() -> Log.infof("Inside findAllWhereNameLikeNotFound(%s)", name))
                        .assertThat(
                                () -> this.customerRepository.listAllWhereCustomerNameLike(name),
                                customers ->
                                        assertThat(customers)
                                                .isNotNull()
                                                .isEmpty()
                        )
        );
    }
}

