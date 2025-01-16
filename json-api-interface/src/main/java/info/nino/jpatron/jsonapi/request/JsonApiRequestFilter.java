package info.nino.jpatron.jsonapi.request;

import info.nino.jpatron.api.ApiRequestDefaults;
import info.nino.jpatron.api.ApiRequestFilterInterface;
import info.nino.jpatron.api.ApiRequestNotation;
import info.nino.jpatron.api.FilterRequestContext;
import info.nino.jpatron.jsonapi.annotiation.JsonApi;
import info.nino.jpatron.jsonapi.annotiation.JsonApiInject;
import info.nino.jpatron.request.ApiRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;

/**
 * JSON API request filter implementation
 */
@Provider
@JsonApi
@Priority(Priorities.ENTITY_CODER + 200)
public class JsonApiRequestFilter implements ApiRequestFilterInterface
{
    private final static String API_QUERY_PARAM_REGEX = "^([^\\[\\]\\s]+)\\[([^\\[\\]\\s]+)\\](?:\\[([^\\[\\]\\s]+)\\])?$";
    private final static String ADVANCED_FILTER_TERM_REGEX = null;

    @Context
    ResourceInfo resourceInfo;

    @Inject
    @JsonApiInject
    Event<JsonApiRequest<?>> requestEvent;

    @PostConstruct
    public void init() {

    }

    @Override
    public ResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    @Override
    public Event getRequestEventDelegate() {
        return requestEvent;
    }

    @Override
    public Class<? extends Annotation> getApiAnnotation() {
        return JsonApi.class;
    }

    @Override
    public FilterRequestContext initializeFilterContext(Annotation annotation, MultivaluedMap<String, String> reqQueryParams) {
        JsonApi ann = (JsonApi) annotation;
        Class<?> dtoClass = ann.value();
        boolean allowEntityPaths = ann.allowEntityPaths();
        String[] allowedPaths = ann.allowedPaths();
        return new FilterRequestContext(dtoClass, null, allowEntityPaths, allowedPaths, reqQueryParams);
    }

    @Override
    public ApiRequest<?> initApiRequest(Annotation annotation, Class<?> entityClass, ApiRequest.QueryParams queryParams) {
        JsonApi ann = (JsonApi) annotation;
        boolean pagination = ann.pagination();
        boolean distinct = ann.distinctDataset();
        boolean readOnly = ann.readOnlyDataset();
        String[] entityGraphPaths = ann.entityGraphPaths();
        return new JsonApiRequest<>(entityClass, queryParams, pagination, distinct, readOnly, entityGraphPaths);
    }

    @Override
    public String getApiQueryParamRegex() {
        return API_QUERY_PARAM_REGEX;
    }

    public String getAdvancedFilterTermRegex()
    {
        return ADVANCED_FILTER_TERM_REGEX;
    }

    @Override
    public ApiRequestNotation getApiRequestNotation() {
        return JsonApiRequestNotation.INSTANCE;
    }

    @Override
    public ApiRequestDefaults getApirequestDefaults() {
        return JsonApiRequestDefaults.INSTANCE;
    }
}
