package info.nino.jpatron.api.request;

import info.nino.jpatron.api.annotiation.JPatronApiInject;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;

@RequestScoped
public class JPatronApiRequestProducer
{
	@Produces
	@JPatronApiInject
	JPatronApiRequest jPatronApiRequest;

	public void handleJPatronApiRequestEvent(@Observes @JPatronApiInject JPatronApiRequest<?> jPatronApiRequest) {
		this.jPatronApiRequest = jPatronApiRequest;
	}
}