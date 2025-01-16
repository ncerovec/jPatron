package info.nino.jpatron.efd.request;

import info.nino.jpatron.api.ApiRequestDefaults;
import info.nino.jpatron.api.ApiRequestFilterInterface;
import info.nino.jpatron.api.ApiRequestNotation;
import info.nino.jpatron.api.FilterRequestContext;
import info.nino.jpatron.efd.annotiation.EfdApi;
import info.nino.jpatron.efd.annotiation.EfdApiInject;
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
 * EFD API request filter implementation
 */
@Provider
@EfdApi
@Priority(Priorities.ENTITY_CODER + 200)
public class EfdApiRequestFilter implements ApiRequestFilterInterface {

    private final static String API_QUERY_PARAM_REGEX = "^([^\\[\\]\\s]+)$";
    private final static String ADVANCED_FILTER_TERM_REGEX = "^([^\\s:=<>!#^~]+)[\\s]*(:[=<>!#^~]{0,2})[\\s]*([^\\n]*)$";

    @Context
    ResourceInfo resourceInfo;

    @Inject
    @EfdApiInject
    Event<EfdApiRequest<?>> requestEvent;

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
        return EfdApi.class;
    }

    @Override
    public FilterRequestContext initializeFilterContext(Annotation annotation, MultivaluedMap<String, String> reqQueryParams) {
        EfdApi ann = (EfdApi) annotation;
        Class<?> dtoClass = ann.value();
        String[] searchPaths = ann.searchPaths();
        boolean allowEntityPaths = false;
        String[] allowedPaths = {".*"};
        return new FilterRequestContext(dtoClass, searchPaths, allowEntityPaths, allowedPaths, reqQueryParams);
    }

    @Override
    public ApiRequest<?> initApiRequest(Annotation annotation, Class<?> entityClass, ApiRequest.QueryParams queryParams) {
        EfdApi ann = (EfdApi) annotation;
        boolean pagination = ann.pagination();
        boolean distinct = ann.distinctDataset();
        boolean readOnly = ann.readOnlyDataset();
        String[] entityGraphPaths = ann.entityGraphPaths();
        return new EfdApiRequest<>(entityClass, queryParams, pagination, distinct, readOnly, entityGraphPaths);
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
        return EfdApiRequestNotation.INSTANCE;
    }

    @Override
    public ApiRequestDefaults getApirequestDefaults() {
        return EfdApiRequestDefaults.INSTANCE;
    }
}
