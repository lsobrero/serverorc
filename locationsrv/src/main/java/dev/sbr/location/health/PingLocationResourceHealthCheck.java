package dev.sbr.location.health;

import dev.sbr.location.rest.LocationResource;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

public class PingLocationResourceHealthCheck implements HealthCheck {

    @Inject
    LocationResource locationResource;

    @Override
    public HealthCheckResponse call() {
        var response = this.locationResource.hello();

        return HealthCheckResponse.named("Ping Location REST Endpoint")
                .withData("Response", response)
                .up()
                .build();
    }
}
