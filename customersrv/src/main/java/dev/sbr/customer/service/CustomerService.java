package dev.sbr.customer.service;

import dev.sbr.customer.mapping.CustomerFullUpdateMapper;
import dev.sbr.customer.mapping.CustomerPartialUpdateMapper;
import dev.sbr.customer.model.Customer;
import dev.sbr.customer.repository.CustomerRepository;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@ApplicationScoped
public class CustomerService {
    private final Validator validator;

    private final CustomerRepository customerRepository;
    private final CustomerFullUpdateMapper customerFullUpdateMapper;
    private final CustomerPartialUpdateMapper customerPartialUpdateMapper;

    public CustomerService(CustomerRepository customerRepository,
                            Validator validator,
                           CustomerFullUpdateMapper customerFullUpdateMapper,
                           CustomerPartialUpdateMapper customerPartialUpdateMapper) {

        this.customerRepository = customerRepository;
        this.validator = validator;
        this.customerFullUpdateMapper = customerFullUpdateMapper;
        this.customerPartialUpdateMapper = customerPartialUpdateMapper;
    }


    @WithSpan("CustomerService.findAllCustomers")
    public Uni<List<Customer>> findAllCustomers() {
        Log.debug("Getting all customers");
        return this.customerRepository.listAll();
    }

    @WithSpan("CustomerService.findAllCustomersHavingCustomerName")
    public Uni<List<Customer>> findAllCustomersHavingCustomerName(@SpanAttribute("arg.name") String name) {
        Log.debugf("Finding all customers having name = %s", name);
        return this.customerRepository.listAllWhereCustomerNameLike(name);
    }

    @WithSpan("CustomerService.findRandomCustomer")
    public Uni<Customer> findRandomCustomer() {
        return customerRepository.findRandom();
    }

    @WithSpan("CustomerService.findCustomerById")
    public Uni<Customer> findCustomerById(@SpanAttribute("arg.id") Long id) {
        Log.debugf("Finding customer by id = %d", id);
        return this.customerRepository.findById(id);
    }

    @WithSpan("CustomerService.persistCustomer")
    @WithTransaction
    public Uni<Customer> persistCustomer(@SpanAttribute("arg.customer") @NotNull @Valid Customer customer) {
        Log.debugf("Persisting customer: %s", customer);
        return this.customerRepository.persist(customer);
    }

    @WithSpan("CustomerService.replaceCustomer")
    @WithTransaction
    public Uni<Customer> replaceCustomer(@SpanAttribute("arg.customer") @NotNull @Valid Customer customer) {
        Log.debugf("Replacing customer: %s", customer);
        return this.customerRepository.findById(customer.getId())
                .onItem().ifNotNull().transform(h -> {
                    this.customerFullUpdateMapper.mapFullUpdate(customer, h);
                    return h;
                });
    }

    @WithSpan("CustomerService.partialUpdateCustomer")
    @WithTransaction
    public Uni<Customer> partialUpdateCustomer(@SpanAttribute("arg.customer") @NotNull Customer customer) {
        Log.infof("Partially updating customer: %s", customer);
        return this.customerRepository.findById(customer.getId())
                .onItem().ifNotNull().transform(h -> {
                    this.customerPartialUpdateMapper.mapPartialUpdate(customer, h);
                    return h;
                })
                .onItem().ifNotNull().transform(this::validatePartialUpdate);
    }

    @WithSpan("CustomerService.replaceAllCustomers")
    @WithTransaction
    public Uni<Void> replaceAllCustomers(@SpanAttribute("arg.customers") List<Customer> customers) {
        Log.debug("Replacing all customers");
        return deleteAllCustomers()
                .replaceWith(this.customerRepository.persist(customers));
    }

    @WithSpan("CustomerService.deleteAllCustomers")
    @WithTransaction
    public Uni<Void> deleteAllCustomers() {
        Log.debug("Deleting all customers");
        return this.customerRepository.listAll()
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list))
                .map(Customer::getId)
                .onItem().transformToUniAndMerge(this::deleteCustomer)
                .collect().asList()
                .replaceWithVoid();
    }

    @WithSpan("CustomerService.deleteCustomer")
    @WithTransaction
    public Uni<Void> deleteCustomer(@SpanAttribute("arg.id") Long id) {
        Log.debugf("Deleting customer by id = %d", id);
        return this.customerRepository.deleteById(id).replaceWithVoid();
    }

    /**
     * Validates a {@link Customer} for partial update according to annotation validation rules on the {@link Customer} object.
     * @param customer The {@link Customer}
     * @return The same {@link Customer} that was passed in, assuming it passes validation. The return is used as a convenience so the method can be called in a functional pipeline.
     * @throws ConstraintViolationException If validation fails
     */
    private Customer validatePartialUpdate(Customer customer) {
        var violations = this.validator.validate(customer);
        if ((violations != null) && !violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return customer;
    }
}
