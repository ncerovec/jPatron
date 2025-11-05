package info.nino.jpatron.api.request.payload;

import info.nino.jpatron.request.ApiRequest;
import info.nino.jpatron.request.QueryExpression;

import java.util.List;
import java.util.Set;

public record JPatronRequestPayload(Integer pageSize, Integer pageNumber, Set<JPatronSort> sorts, Set<JPatronDistinct> distincts, Set<JPatronMeta> metas)
{

    public record JPatronSort(String columnPath, ApiRequest.SortDirectionEnum direction)
    {

    }

    public record JPatronDistinct(String name, String valuePath, String labelPath)
    {

    }

    public record JPatronMeta(String name, String valuePath, QueryExpression.Function function, String[] labelPaths)
    {

    }
}
