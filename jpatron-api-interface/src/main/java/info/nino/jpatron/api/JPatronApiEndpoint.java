package info.nino.jpatron.api;

import info.nino.jpatron.api.annotiation.JPatronApiInject;
import info.nino.jpatron.api.request.JPatronApiRequest;
import jakarta.enterprise.event.Observes;

public abstract class JPatronApiEndpoint {

    //@Inject
    //@JPatronApiInject    //Implementation for RestEasy reactive (io.quarkus:quarkus-rest)
    protected JPatronApiRequest<?> jpatronApiRequest;

    //NOTICE: https://quarkus.io/guides/cdi#events-and-observers
    //Implementation for RestEasy classic (io.quarkus:quarkus-resteasy)
    public void onTaskCompleted(@Observes @JPatronApiInject JPatronApiRequest<?> jpatronApiRequest) {
        this.jpatronApiRequest = jpatronApiRequest;
    }
}
