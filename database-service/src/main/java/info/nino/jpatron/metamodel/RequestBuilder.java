package info.nino.jpatron.metamodel;

import info.nino.jpatron.request.ApiRequest;
import info.nino.jpatron.request.QueryExpression;
import info.nino.jpatron.request.QuerySort;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RequestBuilder<T>
{
    private final PageRequest<T> pageRequest;

    private RequestBuilder(PageRequest<T> pageRequest)
    {
        this.pageRequest = pageRequest;
    }

    public PageRequest<T> getPageRequest()
    {
        return pageRequest;
    }

    public static <V extends Comparable<? super V>> QueryExpression.Filter<V> createNewFilter(RequestBuilder<?> requestBuilder, String fieldPath, QueryExpression.CompareOperator compareOperator, V... value)
    {
        return new QueryExpression.Filter<>(requestBuilder.getPageRequest().getRootEntity(), fieldPath, compareOperator, value);
    }

    public static <V extends Comparable<? super V>> QueryExpression.Filter<V> createNewFilter(RequestBuilder<?> requestBuilder, String fieldPath, QueryExpression.CompareOperator compareOperator, QueryExpression.ValueModifier valueModifier, V... value)
    {
        return new QueryExpression.Filter<>(requestBuilder.getPageRequest().getRootEntity(), fieldPath, compareOperator, valueModifier, value);
    }

    public static QueryExpression.CompoundFilter createNewCompoundFilter(QueryExpression.LogicOperator logicOperator, QueryExpression.Filter<?>... filters)
    {
        return new QueryExpression.CompoundFilter(logicOperator, filters);
    }

    public static QueryExpression.CompoundFilter createNewCompoundFilter(QueryExpression.LogicOperator logicOperator, QueryExpression.CompoundFilter... compoundFilters)
    {
        return new QueryExpression.CompoundFilter(logicOperator, compoundFilters);
    }

    public static <T> RequestBuilder<T> init(Class<T> rootEntity)
    {
        return RequestBuilder.init(rootEntity, null, null);
    }

    public static <T> RequestBuilder<T> init(Class<T> rootEntity, Integer pageSize, Integer pageNumber)
    {
        PageRequest<T> pageRequest = new PageRequest<>(rootEntity, pageSize, pageNumber);
        return new RequestBuilder<T>(pageRequest);
    }

    public static <T> RequestBuilder<T> init(PageRequest<T> pageRequest)
    {
        return new RequestBuilder<T>(pageRequest);
    }

    /**
     * Builder initializer for PageRequest (data-service) by generic ApiRequest (common-utils)
     * @param apiRequest generic ApiRequest (common-utils) parameter
     */
    public static <T> RequestBuilder<T> init(ApiRequest<T> apiRequest)
    {
        RequestBuilder<T> requestBuilder = RequestBuilder.init(apiRequest.getRootEntity(), apiRequest.getQueryParams().getPageSize(), apiRequest.getQueryParams().getPageNumber());

        requestBuilder.setDistinct(apiRequest.isDistinctDataset());
        requestBuilder.setReadOnly(apiRequest.isReadOnlyDataset());
        //requestBuilder.addFetchEntityPaths(apiRequest.getFetchEntityPaths());
        requestBuilder.addEntityGraphPaths(apiRequest.getEntityGraphPaths());

        MapUtils.emptyIfNull(apiRequest.getQueryParams().getSort()).forEach((fieldPath, clazzDirectionEntry) ->
        {
            requestBuilder.addSorting(fieldPath, clazzDirectionEntry.getValue());
        });

        if (apiRequest.getQueryParams().getCompoundFilter() != null) {
            requestBuilder.getPageRequest().setQueryFilters(apiRequest.getQueryParams().getCompoundFilter());
        }

        MapUtils.emptyIfNull(apiRequest.getQueryParams().getFilters()).forEach((clazz, filterPaths) -> {
            filterPaths.forEach((fieldPath, filterValues) -> {
                filterValues.asMap().forEach((compareOperator, values) -> {
                    requestBuilder.addFilter(fieldPath, compareOperator, values.toArray(new Comparable[]{}));
                });
            });
        });

        List<QueryExpression.Filter<?>> searchFilters = new ArrayList<>();
        MapUtils.emptyIfNull(apiRequest.getQueryParams().getSearches()).forEach((clazz, searchFields) -> {
            searchFields.forEach((fieldPath, searchValues) -> {
                searchValues.asMap().forEach((valueModifier, values) -> {
                    QueryExpression.CompareOperator compareOperator = QueryExpression.CompareOperator.LIKE;
                    var searchFilter = RequestBuilder.createNewFilter(requestBuilder, fieldPath, compareOperator, valueModifier, values.toArray(new Comparable[]{}));
                    searchFilters.add(searchFilter);
                });
            });
        });

        if (!searchFilters.isEmpty()) {
            requestBuilder.addCompoundFilter(QueryExpression.LogicOperator.OR, searchFilters.toArray(new QueryExpression.Filter[] {}));
        }

        MapUtils.emptyIfNull(apiRequest.getQueryParams().getDistinctValues()).forEach((clazz, distinctFields) -> {
            distinctFields.asMap().forEach((keyField, labelFields) -> {
                labelFields.forEach(labelField -> {
                    if(StringUtils.isNotBlank(labelField)) {
                        requestBuilder.addDistinct(keyField, labelField);
                    } else {
                        requestBuilder.addDistinct(keyField);
                    }
                });
            });
        });

        MapUtils.emptyIfNull(apiRequest.getQueryParams().getMetaValues()).forEach((clazz, metaFields) -> {
            metaFields.forEach((valueField, labelValues) -> {
                labelValues.asMap().forEach((function, labelFields) -> {
                    labelFields.forEach(labelField -> {
                        if(StringUtils.isNotBlank(labelField)) {
                            requestBuilder.addMeta(labelField, function, valueField);
                        } else {
                            requestBuilder.addMeta(function, valueField);
                        }
                    });
                });
            });
        });

        return requestBuilder;
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

    public RequestBuilder<T> addSorting(String sortFieldPath, QuerySort.Direction direction)
    {
        this.pageRequest.addSort(this.pageRequest.getRootEntity(), sortFieldPath, direction);
        return this;
    }

    public RequestBuilder<T> addSorting(String... sorts)
    {
        Arrays.stream(sorts).forEach(sort -> this.pageRequest.addSort(this.pageRequest.getRootEntity(), sort));
        return this;
    }

    public RequestBuilder<T> setRootCompoundFilterLogicOperator(QueryExpression.LogicOperator logicOperator)
    {
        this.pageRequest.getQueryFilters().setLogicOperator(logicOperator);
        return this;
    }

    public <V extends Comparable<? super V>> RequestBuilder<T> addFilter(String fieldPath, QueryExpression.CompareOperator compareOperator, V... value)
    {
        QueryExpression.Filter<V> newFilter = new QueryExpression.Filter<>(this.pageRequest.getRootEntity(), fieldPath, compareOperator, value);
        this.pageRequest.getQueryFilters().addFilters(newFilter);
        return this;
    }

    public <V extends Comparable<? super V>> RequestBuilder<T> addFilter(String fieldPath, QueryExpression.CompareOperator compareOperator, QueryExpression.ValueModifier valueModifier, V... value)
    {
        QueryExpression.Filter<V> newFilter = new QueryExpression.Filter<>(this.pageRequest.getRootEntity(), fieldPath, compareOperator, valueModifier, value);
        this.pageRequest.getQueryFilters().addFilters(newFilter);
        return this;
    }

    public <V extends Comparable<? super V>> RequestBuilder<T> addFilter(QueryExpression.Filter<V>... newFilters)
    {
        this.pageRequest.getQueryFilters().addFilters(newFilters);
        return this;
    }

    public RequestBuilder<T> addCompoundFilter(QueryExpression.LogicOperator logicOperator, QueryExpression.Filter<?>... nestedFilters)
    {
        QueryExpression.CompoundFilter newCompoundFilter = new QueryExpression.CompoundFilter(logicOperator, nestedFilters);
        this.pageRequest.getQueryFilters().addCompoundFilters(newCompoundFilter);
        return this;
    }

    public RequestBuilder<T> addCompoundFilter(QueryExpression.LogicOperator logicOperator, QueryExpression.CompoundFilter... nestedCompoundFilters)
    {
        QueryExpression.CompoundFilter newCompoundFilter = new QueryExpression.CompoundFilter(logicOperator, nestedCompoundFilters);
        this.pageRequest.getQueryFilters().addCompoundFilters(newCompoundFilter);
        return this;
    }

    public RequestBuilder<T> addCompoundFilter(QueryExpression.CompoundFilter... newCompoundFilters)
    {
        this.pageRequest.getQueryFilters().addCompoundFilters(newCompoundFilters);
        return this;
    }

    public RequestBuilder<T> addDistinct(String valueFieldPath)
    {
        QueryExpression newDistinctExpression = new QueryExpression(this.pageRequest.getRootEntity(), valueFieldPath);
        this.pageRequest.getDistinctColumns().add(newDistinctExpression);
        return this;
    }

    public RequestBuilder<T> addDistinct(String valueFieldPath, String labelFieldPath)
    {
        QueryExpression newDistinctExpression = new QueryExpression(this.pageRequest.getRootEntity(), valueFieldPath, labelFieldPath);
        this.pageRequest.getDistinctColumns().add(newDistinctExpression);
        return this;
    }

    public RequestBuilder<T> addMeta(QueryExpression.Function function, String valueFieldPath)
    {
        QueryExpression newMetaExpression = new QueryExpression(this.pageRequest.getRootEntity(), valueFieldPath, function);
        this.pageRequest.getMetaColumns().add(newMetaExpression);
        return this;
    }

    public RequestBuilder<T> addMeta(String labelFieldPath, QueryExpression.Function function, String valueFieldPath)
    {
        QueryExpression newMetaExpression = new QueryExpression(this.pageRequest.getRootEntity(), valueFieldPath, function, labelFieldPath);
        this.pageRequest.getMetaColumns().add(newMetaExpression);
        return this;
    }

    public PageRequest<T> build()
    {
        return this.pageRequest;
    }

    //TODO: public static class QueryBuilder<T>
}
