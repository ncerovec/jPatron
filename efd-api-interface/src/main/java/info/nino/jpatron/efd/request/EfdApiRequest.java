package info.nino.jpatron.efd.request;

import info.nino.jpatron.request.ApiRequest;

/**
 * EFD API request implementation
 */
public class EfdApiRequest<T> extends ApiRequest<T> {

    public EfdApiRequest(Class<T> rootEntity,
                         QueryParams queryParams,
                         boolean pagination,
                         boolean distinct,
                         boolean readOnly,
                         String[] entityGraphPaths) {
        super(rootEntity, queryParams, pagination, distinct, readOnly, null, entityGraphPaths);
    }
}
