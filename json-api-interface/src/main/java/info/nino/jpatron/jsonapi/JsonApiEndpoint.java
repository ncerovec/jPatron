package info.nino.jpatron.jsonapi;

import info.nino.jpatron.jsonapi.annotiation.JsonApiInject;
import info.nino.jpatron.jsonapi.request.JsonApiRequest;
import jakarta.enterprise.event.Observes;

public abstract class JsonApiEndpoint {

    //@Inject
    //@JsonApiInject    //Implementation for RestEasy reactive (io.quarkus:quarkus-rest)
    protected JsonApiRequest jsonApiRequest;

    //NOTICE: https://quarkus.io/guides/cdi#events-and-observers
    //Implementation for RestEasy classic (io.quarkus:quarkus-resteasy)
    public void onTaskCompleted(@Observes @JsonApiInject JsonApiRequest jsonApiRequest) {
        this.jsonApiRequest = jsonApiRequest;
    }
}
