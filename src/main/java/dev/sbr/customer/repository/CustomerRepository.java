package dev.sbr.customer.repository;

import dev.sbr.customer.model.Customer;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Random;

@ApplicationScoped
@WithSession
public class CustomerRepository  implements PanacheRepository<Customer> {

    public Uni<List<Customer>> listAllWhereCustomerNameLike(String name) {
        return (name != null) ?
                list("LOWER(customerName) LIKE CONCAT('%', ?1, '%')", name.toLowerCase()) :
                Uni.createFrom().item(List::of);
    }

    public Uni<Customer> findRandom() {
        return count()
                .map(count -> (count > 0) ? count : null)
                .onItem().ifNotNull().transform(count -> new Random().nextInt(count.intValue()))
                .onItem().ifNotNull().transformToUni(randomCustomer -> findAll().page(randomCustomer, 1).firstResult());    }
}
