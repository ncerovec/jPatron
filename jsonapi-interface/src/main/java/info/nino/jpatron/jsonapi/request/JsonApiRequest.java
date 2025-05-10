package info.nino.jpatron.jsonapi.request;

import info.nino.jpatron.request.ApiRequest;

/**
 * JSON:API request implementation
 */
public class JsonApiRequest extends ApiRequest
{
    public JsonApiRequest(ApiRequest.QueryParams queryParams, boolean distinct, boolean readOnly, String[] fetchEntityPaths, String[] entityGraphPaths)
    {
        super(queryParams, distinct, readOnly, fetchEntityPaths, entityGraphPaths);
    }

    /**
     * JSON:API request query-params implementation
     */
    public static class QueryParams extends ApiRequest.QueryParams
    {
        public QueryParams(Integer pageSize, Integer pageNumber)
        {
            super(pageSize, pageNumber);
        }
    }
}
