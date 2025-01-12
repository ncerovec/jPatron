package info.nino.jpatron.jsonapi.request;

import info.nino.jpatron.request.ApiRequest;

/**
 * JSON:API request implementation
 */
public class JsonApiRequest extends ApiRequest {

    public JsonApiRequest(Class<?> rootEntity,
                          ApiRequest.QueryParams queryParams,
                          boolean pagination,
                          boolean distinct,
                          boolean readOnly,
                          String[] entityGraphPaths) {
        super(rootEntity, queryParams, pagination, distinct, readOnly, null, entityGraphPaths);
    }
}
