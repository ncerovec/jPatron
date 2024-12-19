package info.nino.jpatron.efd.request;

import info.nino.jpatron.efd.annotiation.EfdApiInject;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;

@RequestScoped
public class EfdApiRequestProducer {

	@Produces
	@EfdApiInject
	EfdApiRequest efdApiRequest;

	public void handleJsonApiRequestEvent(@Observes @EfdApiInject EfdApiRequest efdApiRequest) {
		this.efdApiRequest = efdApiRequest;
	}
}