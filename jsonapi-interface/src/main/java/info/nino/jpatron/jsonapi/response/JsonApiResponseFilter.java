package info.nino.jpatron.jsonapi.response;

import info.nino.jpatron.annotiation.JsonApi;
import info.nino.jpatron.jsonapi.JsonApiMediaType;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

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
		if((ri.getResourceClass().isAnnotationPresent(JsonApi.class) || ri.getResourceMethod().isAnnotationPresent(JsonApi.class))
			&& responseContext.getEntity() instanceof JsonApiResponse)
		{
			responseContext.setEntity(responseContext.getEntity(),
				responseContext.getEntityAnnotations(),
				MediaType.valueOf(JsonApiMediaType.APPLICATION_JSON_API));
		}
	}
}