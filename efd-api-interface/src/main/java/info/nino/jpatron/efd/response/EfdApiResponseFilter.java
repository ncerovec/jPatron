package info.nino.jpatron.efd.response;

import info.nino.jpatron.efd.annotiation.EfdApi;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * EFD API response filter implementation
 */
@EfdApi
@Provider
public class EfdApiResponseFilter implements ContainerResponseFilter {

	@Context
	ResourceInfo ri;

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
		if ((ri.getResourceClass().isAnnotationPresent(EfdApi.class) || ri.getResourceMethod().isAnnotationPresent(EfdApi.class))) {
			if (responseContext.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
				if (responseContext.getEntity() instanceof EfdApiResponseList) {
					responseContext.setEntity(responseContext.getEntity(),
							responseContext.getEntityAnnotations(),
							MediaType.valueOf(MediaType.APPLICATION_JSON));
				}
			}
		}
	}
}