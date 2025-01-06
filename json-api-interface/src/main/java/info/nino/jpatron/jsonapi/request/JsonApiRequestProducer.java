package info.nino.jpatron.jsonapi.request;

import info.nino.jpatron.jsonapi.annotiation.JsonApiInject;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;

@RequestScoped
public class JsonApiRequestProducer
{
	@Produces
	@JsonApiInject
	JsonApiRequest jsonApiRequest;

	public void handleJsonApiRequestEvent(@Observes @JsonApiInject JsonApiRequest jsonApiRequest)
	{
		this.jsonApiRequest = jsonApiRequest;
	}
}