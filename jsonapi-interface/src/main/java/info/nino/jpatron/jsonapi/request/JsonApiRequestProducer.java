package info.nino.jpatron.jsonapi.request;

import info.nino.jpatron.annotiation.JsonApiInject;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;

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