package dev.sbr.location.rest;

import dev.sbr.location.model.Location;
import dev.sbr.location.service.LocationService;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
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

@Path("/api/locations")
@Tag(name = "locations")
@Produces(APPLICATION_JSON)
public class LocationResource {

    private final LocationService locationService;

    public LocationResource(LocationService locationService) {
        this.locationService = locationService;
    }

    @GET
    @Path("/random")
    @Operation(summary = "Returns a random location")
    @APIResponse(
            responseCode = "200",
            description = "Gets random location",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = Location.class, required = true),
                    examples = @ExampleObject(name = "location", value = Examples.VALID_EXAMPLE_LOCATION)
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "No location found"
    )
    @RunOnVirtualThread
    public Response getRandomLocation() {
        return this.locationService.findRandomLocation()
                .map(v -> {
                    Log.debugf("Found random location: %s", v);
                    return Response.ok(v).build();
                })
                .orElseGet(() -> {
                    Log.debug("No random location found");
                    return Response.status(Response.Status.NOT_FOUND).build();
                });
    }


    @GET
    @Operation(summary = "Returns all the location from the database")
    @APIResponse(
            responseCode = "200",
            description = "Gets all location",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = Location.class, type = SchemaType.ARRAY),
                    examples = @ExampleObject(name = "location", value = Examples.VALID_EXAMPLE_LOCATION_LIST)
            )
    )
    @RunOnVirtualThread
    public List<Location> getAllLocations(@Parameter(name = "name_filter",
            description = "An optional filter parameter to filter results by name") @QueryParam("name_filter") Optional<String> nameFilter) {
        var location = nameFilter
                .map(this.locationService::findAllLocationsHavingName)
                .orElseGet(this.locationService::findAllLocations);

        Log.debugf("Total number of location: %d", location.size());

        return location;
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Returns a location for a given identifier")
    @APIResponse(
            responseCode = "200",
            description = "Gets a location for a given id",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = Location.class),
                    examples = @ExampleObject(name = "location", value = Examples.VALID_EXAMPLE_LOCATION)
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "The location is not found for a given identifier"
    )
    @RunOnVirtualThread
    public Response getLocation(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        return this.locationService.findLocationById(id)
                .map(v -> {
                    Log.debugf("Found location: %s", v);
                    return Response.ok(v).build();
                })
                .orElseGet(() -> {
                    Log.debugf("No location found with id %d", id);
                    return Response.status(Response.Status.NOT_FOUND).build();
                });
    }


    @POST
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Creates a valid location")
    @APIResponse(
            responseCode = "201",
            description = "The URI of the created location",
            headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid location passed in (or no request body found)"
    )
    @RunOnVirtualThread
    public Response createLocation(
            @RequestBody(
                    name = "location",
                    required = true,
                    content = @Content(
                            mediaType = APPLICATION_JSON,
                            schema = @Schema(implementation = Location.class),
                            examples = @ExampleObject(name = "valid_location", value = Examples.VALID_EXAMPLE_LOCATION_TO_CREATE)
                    )
            )
            @Valid @NotNull Location location,
            @Context UriInfo uriInfo) {
        var v = this.locationService.persistLocation(location);
        var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(v.id));
        Log.debugf("New location created with URI %s", builder.build().toString());
        return Response.created(builder.build()).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Completely updates/replaces an exiting location by replacing it with the passed-in location")
    @APIResponse(
            responseCode = "204",
            description = "Replaced the location"
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid location passed in (or no request body found)"
    )
    @APIResponse(
            responseCode = "404",
            description = "No location found"
    )
    @RunOnVirtualThread
    public Response fullyUpdateLocation(
            @Parameter(name = "id", required = true) @PathParam("id") Long id,
            @RequestBody(
                    name = "location",
                    required = true,
                    content = @Content(
                            mediaType = APPLICATION_JSON,
                            schema = @Schema(implementation = Location.class),
                            examples = @ExampleObject(name = "valid_location", value = Examples.VALID_EXAMPLE_LOCATION)
                    )
            )
            @Valid @NotNull Location location) {
        if (location.id == null) {
            location.id = id;
        }

        return this.locationService.replaceLocation(location)
                .map(v -> {
                    Log.debugf("Location replaced with new values %s", v);
                    return Response.noContent().build();
                })
                .orElseGet(() -> {
                    Log.debugf("No location found with id %d", location.id);
                    return Response.status(Response.Status.NOT_FOUND).build();
                });
    }

    @PUT
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Completely replace all locations with the passed-in locations")
    @APIResponse(
            responseCode = "201",
            description = "The URI to retrieve all the created locations",
            headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid locations passed in (or no request body found)"
    )
    @RunOnVirtualThread
    public Response replaceAllLocations(
            @RequestBody(
                    name = "valid_locations",
                    required = true,
                    content = @Content(
                            mediaType = APPLICATION_JSON,
                            schema = @Schema(implementation = Location.class, type = SchemaType.ARRAY),
                            examples = @ExampleObject(name = "locations", value = Examples.VALID_EXAMPLE_LOCATION_LIST)
                    )
            )
            @NotNull List<Location> locations,
            @Context UriInfo uriInfo) {
        this.locationService.replaceAllLocations(locations);
        var uri = uriInfo.getAbsolutePathBuilder().build();
        Log.debugf("New Locations created with URI %s", uri.toString());
        return Response.created(uri).build();
    }

    @PATCH
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(summary = "Partially updates an exiting location")
    @APIResponse(
            responseCode = "200",
            description = "Updated the location",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    schema = @Schema(implementation = Location.class),
                    examples = @ExampleObject(name = "location", value = Examples.VALID_EXAMPLE_LOCATION)
            )
    )
    @APIResponse(
            responseCode = "400",
            description = "Null location passed in (or no request body found)"
    )
    @APIResponse(
            responseCode = "404",
            description = "No location found"
    )
    @RunOnVirtualThread
    public Response partiallyUpdateLocation(
            @Parameter(name = "id", required = true) @PathParam("id") Long id,
            @RequestBody(
                    name = "valid_location",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = Location.class),
                            examples = @ExampleObject(name = "valid_location", value = Examples.VALID_EXAMPLE_LOCATION)
                    )
            )
            @NotNull Location location) {
        if (location.id == null) {
            location.id = id;
        }

        return this.locationService.partialUpdateLocation(location)
                .map(v -> {
                    Log.debugf("Location updated with new values %s", v);
                    return Response.ok(v).build();
                })
                .orElseGet(() -> {
                    Log.debugf("No location found with id %d", location.id);
                    return Response.status(Response.Status.NOT_FOUND).build();
                });
    }

    @DELETE
    @Operation(summary = "Delete all locations")
    @APIResponse(
            responseCode = "204",
            description = "Deletes all locations"
    )
    @RunOnVirtualThread
    public void deleteAllLocations() {
        this.locationService.deleteAllLocations();
        Log.debug("Deleted all locations");
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deletes an exiting location")
    @APIResponse(
            responseCode = "204",
            description = "Delete a location"
    )
    @RunOnVirtualThread
    public void deleteLocation(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
        this.locationService.deleteLocation(id);
        Log.debugf("Location with id %d deleted ", id);
    }

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    @Tag(name = "hello")
    @Operation(summary = "Ping hello")
    @APIResponse(
            responseCode = "200",
            description = "Ping hello",
            content = @Content(
                    schema = @Schema(implementation = String.class),
                    examples = @ExampleObject(name = "hello_success", value = "Hello Location Resource")
            )
    )
    @NonBlocking
    public String hello() {
        Log.debug("Hello Location Resource");
        return "Hello Location Resource";
    }
    
}
