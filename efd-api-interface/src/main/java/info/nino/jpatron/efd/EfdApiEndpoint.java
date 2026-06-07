package info.nino.jpatron.efd;

import info.nino.jpatron.efd.annotiation.EfdApiInject;
import info.nino.jpatron.efd.request.EfdApiRequest;
import jakarta.enterprise.event.Observes;

public abstract class EfdApiEndpoint {

    //@Inject
    //@EfdApiInject    //Implementation for RestEasy reactive (io.quarkus:quarkus-rest)
    protected EfdApiRequest efdApiRequest;

    //NOTICE: https://quarkus.io/guides/cdi#events-and-observers
    //Implementation for RestEasy classic (io.quarkus:quarkus-resteasy)
    public void onTaskCompleted(@Observes @EfdApiInject EfdApiRequest efdApiRequest) {
        this.efdApiRequest = efdApiRequest;
    }
}
