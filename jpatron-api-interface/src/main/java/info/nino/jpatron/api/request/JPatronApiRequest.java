package info.nino.jpatron.api.request;

import info.nino.jpatron.request.ApiRequest;

/**
 * jPatron API request implementation
 */
public class JPatronApiRequest<T> extends ApiRequest<T> {

    public JPatronApiRequest(Class<T> rootEntity,
                         ApiRequest.QueryParams queryParams,
                         boolean pagination,
                         boolean distinct,
                         boolean readOnly,
                         String[] entityGraphPaths) {
        super(rootEntity, queryParams, pagination, distinct, readOnly, null, entityGraphPaths);
    }
}
