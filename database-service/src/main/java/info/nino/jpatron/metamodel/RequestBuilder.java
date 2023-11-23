package info.nino.jpatron.metamodel;

import info.nino.jpatron.request.ApiRequest;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Set;

public class RequestBuilder<T>
{
    private PageRequest<T> pageRequest;

    private RequestBuilder(PageRequest<T> pageRequest)
    {
        this.pageRequest = pageRequest;
    }

    public static <V extends Comparable<? super V>> QueryExpression.Filter<V> createNewFilter(Class<?> rootEntity, String columnPath, QueryExpression.Filter.Cmp cmp, V... value)
    {
        return new QueryExpression.Filter<>(rootEntity, columnPath, cmp, value);
    }

    public static <T> RequestBuilder<T> init(Class<T> rootEntity)
    {
        //PageRequest<T> pageRequest = new PageRequest<>(rootEntity);
        //return new RequestBuilder<T>(pageRequest);
        return RequestBuilder.init(rootEntity, null, null);
    }

    public static <T> RequestBuilder<T> init(Class<T> rootEntity, Integer pageSize, Integer pageNumber)
    {
        PageRequest<T> pageRequest = new PageRequest<>(rootEntity, pageSize, pageNumber);
        return new RequestBuilder<T>(pageRequest);
    }

    public static <T> RequestBuilder<T> init(ApiRequest<T> apiRequest)
    {
        PageRequest<T> pageRequest = new PageRequest<>(apiRequest);
        return new RequestBuilder<T>(pageRequest);
    }

    public RequestBuilder<T> setDistinct(boolean distinct)
    {
        this.pageRequest.setDistinctDataset(distinct);
        return this;
    }

    public RequestBuilder<T> setReadOnly(boolean readOnly)
    {
        this.pageRequest.setReadOnlyDataset(readOnly);
        return this;
    }

    public RequestBuilder<T> clearEntityGraphPaths()
    {
        this.pageRequest.setEntityGraphPaths(null);
        return this;
    }

    public RequestBuilder<T> addEntityGraphPaths(String... entityGraphPaths)
    {
        var newEntityGraphPaths = ArrayUtils.addAll(this.pageRequest.getEntityGraphPaths(), entityGraphPaths);
        this.pageRequest.setEntityGraphPaths(newEntityGraphPaths);
        return this;
    }

    public RequestBuilder<T> addNewSorting(String... sorts)
    {
        for(String sort : sorts)
        {
            this.pageRequest.addSort(this.pageRequest.getRootEntity(), sort);
        }

        return this;
    }

    public <V extends Comparable<? super V>> RequestBuilder<T> addNewFilter(String columnPath, QueryExpression.Filter.Cmp cmp, V... value)
    {
        QueryExpression.Filter<V> newFilter = new QueryExpression.Filter<>(this.pageRequest.getRootEntity(), columnPath, cmp, value);
        this.pageRequest.getQueryFilters().addFilters(newFilter);

        return this;
    }

    public <V extends Comparable<? super V>> RequestBuilder<T> addNewConditional(QueryExpression.Conditional.Operator logicOperator, QueryExpression.Filter<?>... filters)
    {
        QueryExpression.Conditional newConditional = new QueryExpression.Conditional(logicOperator, filters);
        this.pageRequest.getQueryFilters().addConditionals(newConditional);

        return this;
    }

    public RequestBuilder<T> addNewDistinctColumn(String valueColumnPath)
    {
        QueryExpression newDistinctExpression = new QueryExpression(this.pageRequest.getRootEntity(), valueColumnPath);
        this.pageRequest.getDistinctColumns().add(newDistinctExpression);

        return this;
    }

    public RequestBuilder<T> addNewDistinctColumn(String valueColumnPath, String labelColumnPath)
    {
        QueryExpression newDistinctExpression = new QueryExpression(this.pageRequest.getRootEntity(), valueColumnPath, labelColumnPath);
        this.pageRequest.getDistinctColumns().add(newDistinctExpression);

        return this;
    }

    public RequestBuilder<T> addNewMetaColumn(QueryExpression.Func function, String valueColumnPath)
    {
        QueryExpression newMetaExpression = new QueryExpression(this.pageRequest.getRootEntity(), valueColumnPath, function);
        this.pageRequest.getMetaColumns().add(newMetaExpression);

        return this;
    }

    public RequestBuilder<T> addNewMetaColumn(String labelColumnPath, QueryExpression.Func function, String valueColumnPath)
    {
        QueryExpression newMetaExpression = new QueryExpression(this.pageRequest.getRootEntity(), valueColumnPath, function, labelColumnPath);
        this.pageRequest.getMetaColumns().add(newMetaExpression);

        return this;
    }

    public PageRequest<T> build()
    {
        return this.pageRequest;
    }

    //TODO: public static class QueryBuilder<T>
}
