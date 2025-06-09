package dev.sbr.customer.rest;

import dev.sbr.customer.model.Customer;
import dev.sbr.customer.service.CustomerService;
import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

@Path("/api/customers")
@Tag(name = "customers")
@Produces(APPLICATION_JSON)
public class CustomerResource {
    CustomerService customerService;

    public CustomerResource(CustomerService customerService) {
        this.customerService = customerService;
    }


    @GET
    @Path("/random")
    @Operation(summary = "Returns a random customer")
    @APIResponse(
            responseCode = "200",
            description = "Gets a random customer",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = Customer.class, required = true),
                    examples = @ExampleObject(name = "customer", value = Examples.VALID_EXAMPLE_CUSTOMER)
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "No customer found"
    )
    public Uni<Response> getRandomCustomer() {
        return this.customerService.findRandomCustomer()
                .onItem().ifNotNull().transform(h -> {
                    Log.debugf("Found random customer: %s", h);
                    return Response.ok(h).build();
                })
                .replaceIfNullWith(() -> {
                    Log.debug("No random customer found");
                    return Response.status(Response.Status.NOT_FOUND).build();
                });
    }

    @GET
    @Operation(summary = "Returns all the customers from the database")
    @APIResponse(
            responseCode = "200",
            description = "Gets all customers",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = Customer.class, type = SchemaType.ARRAY),
                    examples = @ExampleObject(name = "customer", value = Examples.VALID_EXAMPLE_CUSTOMER_LIST)
            )
    )
    public Uni<List<Customer>> getAllCustomers(@Parameter(name = "name_filter", description = "An optional filter parameter to filter results by name") @QueryParam("name_filter")
    Optional<String> nameFilter) {
        return nameFilter
                .map(this.customerService::findAllCustomersHavingCustomerName)
                .orElseGet(() -> this.customerService.findAllCustomers().replaceIfNullWith(List::of))
                .invoke(customers -> Log.debugf("Total number of customers: %d", customers.size()));
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Returns a customer for a given identifier")
    @APIResponse(
            responseCode = "200",
            description = "Gets a customer for a given id",
            content = @Content(
                    mediaType = APPLICATION_JSON, schema = @Schema(implementation = Customer.class),
                    examples = @ExampleObject(name = "customer", value = Examples.VALID_EXAMPLE_CUSTOMER)
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "The customer is not found for a given identifier"
    )
    public Uni<Response> getCustomer(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return this.customerService.findCustomerById(id)
                .onItem().ifNotNull().transform(h -> {
                    Log.debugf("Found customer: %s", h);
                    return Response.ok(h).build();
                })
                .replaceIfNullWith(() -> {
                    Log.debugf("No customer found with id %d", id);
                    return Response.status(Response.Status.NOT_FOUND).build();
                });
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Creates a valid customer")
    @APIResponse(
            responseCode = "201",
            description = "The URI of the created customer",
            headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid customer passed in (or no request body found)"
    )
    public Uni<Response> createCustomer(
            @RequestBody(
                    name = "customer",
                    required = true,
                    content = @Content(
                            mediaType = APPLICATION_JSON,
                            schema = @Schema(implementation = Customer.class),
                            examples = @ExampleObject(name = "valid_customer", value = Examples.VALID_EXAMPLE_CUSTOMER_TO_CREATE)
                    )
            )
            @Valid @NotNull Customer customer, @
                    Context UriInfo uriInfo) {
        return this.customerService.persistCustomer(customer)
                .map(h -> {
                    var uri = uriInfo.getAbsolutePathBuilder().path(Long.toString(h.getId())).build();
                    Log.debugf("New Customer created with URI %s", uri.toString());
                    return Response.created(uri).build();
                });
    }

    @PUT
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Completely updates/replaces an exiting customer by replacing it with the passed-in customer")
    @APIResponse(
            responseCode = "204",
            description = "Replaced the customer"
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid customer passed in (or no request body found)"
    )
    @APIResponse(
            responseCode = "404",
            description = "No customer found"
    )
    public Uni<Response> fullyUpdateCustomer(
            @Parameter(name = "id", required = true) @PathParam("id") Long id,
            @RequestBody(
                    name = "customer",
                    required = true,
                    content = @Content(
                            mediaType = APPLICATION_JSON,
                            schema = @Schema(implementation = Customer.class),
                            examples = @ExampleObject(name = "valid_customer", value = Examples.VALID_EXAMPLE_CUSTOMER)
                    )
            )
            @Valid @NotNull Customer customer) {
        if (customer.getId() == null) {
            customer.setId(id);
        }

        return this.customerService.replaceCustomer(customer)
                .onItem().ifNotNull().transform(h -> {
                    Log.debugf("Customer replaced with new values %s", h);
                    return Response.noContent().build();
                })
                .replaceIfNullWith(() -> {
                    Log.debugf("No customer found with id %d", customer.getId());
                    return Response.status(Response.Status.NOT_FOUND).build();
                });
    }

    @PUT
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Completely replace all customers with the passed-in customers")
    @APIResponse(
            responseCode = "201",
            description = "The URI to retrieve all the created customers",
            headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid customers passed in (or no request body found)"
    )
    public Uni<Response> replaceAllCustomers(
            @RequestBody(
                    name = "valid_customers",
                    required = true,
                    content = @Content(
                            mediaType = APPLICATION_JSON,
                            schema = @Schema(implementation = Customer.class, type = SchemaType.ARRAY),
                            examples = @ExampleObject(name = "customers", value = Examples.VALID_EXAMPLE_CUSTOMER_LIST)
                    )
            )
            @NotNull List<Customer> customers,
            @Context UriInfo uriInfo) {
        return this.customerService.replaceAllCustomers(customers)
                .map(h -> {
                    var uri = uriInfo.getAbsolutePathBuilder().build();
                    Log.debugf("New Customers created with URI %s", uri.toString());
                    return Response.created(uri).build();
                });
    }

    @PATCH
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Partially updates an exiting customer")
    @APIResponse(
            responseCode = "200",
            description = "Updated the customer",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = Customer.class),
                    examples = @ExampleObject(name = "customer", value = Examples.VALID_EXAMPLE_CUSTOMER)
            )
    )
    @APIResponse(
            responseCode = "400",
            description = "Null customer passed in (or no request body found)"
    )
    @APIResponse(
            responseCode = "404",
            description = "No customer found"
    )
    public Uni<Response> partiallyUpdateCustomer(
            @Parameter(name = "id", required = true) @PathParam("id") Long id,
            @RequestBody(
                    name = "valid_customer",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = Customer.class),
                            examples = @ExampleObject(name = "valid_customer", value = Examples.VALID_EXAMPLE_CUSTOMER)
                    )
            )
            @NotNull Customer customer) {
        if (customer.getId() == null) {
            customer.setId(id);
        }

        return this.customerService.partialUpdateCustomer(customer)
                .onItem().ifNotNull().transform(h -> {
                    Log.debugf("Customer updated with new values %s", h);
                    return Response.ok(h).build();
                })
                .replaceIfNullWith(() -> {
                    Log.debugf("No customer found with id %d", customer.getId());
                    return Response.status(Response.Status.NOT_FOUND).build();
                })
                .onFailure(ConstraintViolationException.class)
                .transform(cve -> new ResteasyReactiveViolationException(((ConstraintViolationException) cve).getConstraintViolations()));
    }

    @DELETE
    @Operation(summary = "Delete all customers")
    @APIResponse(
            responseCode = "204",
            description = "Deletes all customers"
    )
    public Uni<Void> deleteAllCustomers() {
        return this.customerService.deleteAllCustomers()
                .invoke(() -> Log.debug("Deleted all customers"));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deletes an exiting customer")
    @APIResponse(
            responseCode = "204",
            description = "Deletes a customer"
    )
    public Uni<Void> deleteCustomer(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return this.customerService.deleteCustomer(id)
                .invoke(() -> Log.debugf("Customer deleted with %d", id));
    }

    @GET
    @Path("/hello")
    @Produces(TEXT_PLAIN)
    @Tag(name = "hello")
    @Operation(summary = "Ping hello")
    @APIResponse(
            responseCode = "200",
            description = "Ping hello",
            content = @Content(
                    schema = @Schema(implementation = String.class),
                    examples = @ExampleObject(name = "hello_success", value = "Hello Customer Resource")
            )
    )
    @NonBlocking
    public String hello() {
        Log.debug("Hello Customer Resource");
        return "Hello Customer Resource";
    }

}
