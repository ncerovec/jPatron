package info.nino.jpatron.jsonapi.request;

import info.nino.jpatron.request.ApiRequest;

/**
 * JSON:API request implementation
 */
public class JsonApiRequest<T> extends ApiRequest<T> {

    public JsonApiRequest(Class<T> rootEntity,
                          ApiRequest.QueryParams queryParams,
                          boolean pagination,
                          boolean distinct,
                          boolean readOnly,
                          String[] entityGraphPaths) {
        super(rootEntity, queryParams, pagination, distinct, readOnly, null, entityGraphPaths);
    }
}
