package info.nino.jpatron.jsonapi.request;

import info.nino.jpatron.request.ApiRequest;

public class JsonApiRequest extends ApiRequest
{
    public JsonApiRequest(ApiRequest.QueryParams queryParams, boolean pagination, boolean distinct, boolean readOnly, String[] fetchEntityPaths, String[] entityGraphPaths)
    {
        super(queryParams, pagination, distinct, readOnly, fetchEntityPaths, entityGraphPaths);
    }

    public static class QueryParams extends ApiRequest.QueryParams
    {
        public QueryParams(Integer pageSize, Integer pageNumber)
        {
            super(pageSize, pageNumber);
        }
    }
}
