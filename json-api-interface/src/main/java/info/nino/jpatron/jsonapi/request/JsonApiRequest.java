package info.nino.jpatron.jsonapi.request;

import info.nino.jpatron.request.ApiRequest;
import org.apache.commons.lang3.NotImplementedException;

/**
 * JSON:API request implementation
 */
public class JsonApiRequest<T> extends ApiRequest<T> {

    public JsonApiRequest(Class<T> rootEntity,
                          ApiRequest.QueryParams queryParams,
                          boolean distinct,
                          boolean readOnly,
                          String[] entityGraphPaths) {
        super(rootEntity, queryParams, distinct, readOnly, null, entityGraphPaths);
    }

    @Override
    public Class<? extends SortDirectionEnum> getSortDirectionEnum()
    {
        throw new NotImplementedException();
    }

    @Override
    public Class<? extends CompounderEnum> getCompounderEnum()
    {
        throw new NotImplementedException();
    }

    @Override
    public Class<? extends ComparatorEnum> getComparatorEnum()
    {
        throw new NotImplementedException();
    }

    @Override
    public Class<? extends FunctionEnum> getFunctionEnum()
    {
        throw new NotImplementedException();
    }
}
