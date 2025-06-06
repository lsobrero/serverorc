package dev.sbr.customer.health;

import dev.sbr.customer.rest.CustomerResource;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/**
 * {@link HealthCheck} to ping the Hero service
 */
@Liveness
public class PingHeroResourceHealthCheck implements HealthCheck {
	private final CustomerResource customerResource;

  public PingHeroResourceHealthCheck(CustomerResource heroResource) {
    this.customerResource = heroResource;
  }

  @Override
	public HealthCheckResponse call() {
		var response = this.customerResource.hello();

		return HealthCheckResponse.named("Ping Hero REST Endpoint")
			.withData("Response", response)
			.up()
			.build();
	}
}
