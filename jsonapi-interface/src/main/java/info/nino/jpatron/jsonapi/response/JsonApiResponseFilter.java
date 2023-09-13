package info.nino.jpatron.jsonapi.response;

import info.nino.jpatron.annotiation.JsonApi;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * JSON:API response filter implementation
 */
@JsonApi
@Provider
public class JsonApiResponseFilter implements ContainerResponseFilter
{
	@Inject
	private Logger log;

	//@Context
	//private HttpServletResponse request;	//maven: javax.servlet.servlet-api:2.5

	@Context
	ResourceInfo ri;

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		if (responseContext.getEntity() instanceof JsonApiResponse) {//!ri.getResourceClass().isAnnotationPresent(JsonApi.class)) {
			//TODO
			responseContext.getHeaders().add("Content-Type", "application/vnd.api+json");
		}

		//responseContext.setEntity(
		//		 new JsonApiResponse(responseContext.getEntity()), null, MediaType.APPLICATION_JSON_TYPE);
	}
}