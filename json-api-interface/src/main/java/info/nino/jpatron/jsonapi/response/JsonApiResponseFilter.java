package info.nino.jpatron.jsonapi.response;

import info.nino.jpatron.jsonapi.JsonApiMediaType;
import info.nino.jpatron.jsonapi.annotiation.JsonApi;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

/**
 * JSON:API response filter implementation
 */
@JsonApi
@Provider
public class JsonApiResponseFilter implements ContainerResponseFilter
{
	@Context
	ResourceInfo ri;

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
	{
		if((ri.getResourceClass().isAnnotationPresent(JsonApi.class) || ri.getResourceMethod().isAnnotationPresent(JsonApi.class))) {
			if (responseContext.getEntity() instanceof JsonApiResponse<?> entity) {
				responseContext.setEntity(entity, responseContext.getEntityAnnotations(),
						MediaType.valueOf(JsonApiMediaType.APPLICATION_JSON_API));
			} else if (responseContext.getEntity() instanceof JsonApiErrorResponse entity) {
				responseContext.setEntity(entity, responseContext.getEntityAnnotations(),
						MediaType.valueOf(JsonApiMediaType.APPLICATION_JSON_API));
			}
		}
	}
}