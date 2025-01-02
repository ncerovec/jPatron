package info.nino.jpatron.query;

import info.nino.jpatron.request.ApiRequest;
import info.nino.jpatron.request.QueryExpression;
import info.nino.jpatron.request.QuerySort;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class QueryBuilder<T> {

    private final PageRequest<T> pageRequest;

    private QueryBuilder(PageRequest<T> pageRequest) {
        this.pageRequest = pageRequest;
    }

    public PageRequest<T> getPageRequest() {
        return pageRequest;
    }

    public static <V extends Comparable<? super V>> QueryExpression.Filter<V> createNewFilter(QueryBuilder<?> queryBuilder,
                                                                                              String fieldPath, QueryExpression.CompareOperator compareOperator,
                                                                                              V... value) {

        return new QueryExpression.Filter<>(queryBuilder.getPageRequest().getRootEntity(), fieldPath, compareOperator, value);
    }

    public static <V extends Comparable<? super V>> QueryExpression.Filter<V> createNewFilter(QueryBuilder<?> queryBuilder,
                                                                                              String fieldPath,
                                                                                              QueryExpression.CompareOperator compareOperator,
                                                                                              QueryExpression.ValueModifier valueModifier,
                                                                                              V... value) {
        return new QueryExpression.Filter<>(queryBuilder.getPageRequest().getRootEntity(), fieldPath, compareOperator, valueModifier, value);
    }

    public static QueryExpression.CompoundFilter createNewCompoundFilter(QueryExpression.LogicOperator logicOperator,
                                                                         QueryExpression.Filter<?>... filters) {
        return new QueryExpression.CompoundFilter(logicOperator, filters);
    }

    public static QueryExpression.CompoundFilter createNewCompoundFilter(QueryExpression.LogicOperator logicOperator,
                                                                         QueryExpression.CompoundFilter... compoundFilters) {
        return new QueryExpression.CompoundFilter(logicOperator, compoundFilters);
    }

    public static <T> QueryBuilder<T> init(Class<T> rootEntity) {
        return QueryBuilder.init(rootEntity, null, null);
    }

    public static <T> QueryBuilder<T> init(Class<T> rootEntity,
                                           Integer pageSize,
                                           Integer pageNumber) {
        PageRequest<T> pageRequest = new PageRequest<>(rootEntity, pageSize, pageNumber);
        return new QueryBuilder<T>(pageRequest);
    }

    public static <T> QueryBuilder<T> init(PageRequest<T> pageRequest) {
        return new QueryBuilder<T>(pageRequest);
    }

    /**
     * Builder initializer for PageRequest (data-service) by generic ApiRequest (common-utils)
     * @param apiRequest generic ApiRequest (common-utils) parameter
     */
    public static <T> QueryBuilder<T> init(ApiRequest<T> apiRequest) {

        QueryBuilder<T> queryBuilder = QueryBuilder.init(apiRequest.getRootEntity(), apiRequest.getQueryParams().getPageSize(), apiRequest.getQueryParams().getPageNumber());

        queryBuilder.setDistinct(apiRequest.isDistinctDataset());
        queryBuilder.setReadOnly(apiRequest.isReadOnlyDataset());
        //requestBuilder.addFetchEntityPaths(apiRequest.getFetchEntityPaths());
        queryBuilder.addEntityGraphPaths(apiRequest.getEntityGraphPaths());

        MapUtils.emptyIfNull(apiRequest.getQueryParams().getSort()).forEach((fieldPath, clazzDirectionEntry) -> {
            queryBuilder.addSorting(fieldPath, clazzDirectionEntry.getValue());
        });

        if (apiRequest.getQueryParams().getCompoundFilter() != null) {
            queryBuilder.addCompoundFilter(QueryExpression.LogicOperator.AND, apiRequest.getQueryParams().getCompoundFilter());
        }

        MapUtils.emptyIfNull(apiRequest.getQueryParams().getFilters()).forEach((clazz, filterPaths) -> {
            filterPaths.forEach((fieldPath, filterValues) -> {
                filterValues.asMap().forEach((compareOperator, values) -> {
                    queryBuilder.addAndFilter(fieldPath, compareOperator, values.toArray(new Comparable[]{}));
                });
            });
        });

        List<QueryExpression.Filter<?>> searchFilters = new ArrayList<>();
        MapUtils.emptyIfNull(apiRequest.getQueryParams().getSearches()).forEach((clazz, searchFields) -> {
            searchFields.forEach((fieldPath, searchValues) -> {
                searchValues.asMap().forEach((valueModifier, values) -> {
                    QueryExpression.CompareOperator compareOperator = QueryExpression.CompareOperator.LIKE;
                    var searchFilter = QueryBuilder.createNewFilter(queryBuilder, fieldPath, compareOperator, valueModifier, values.toArray(new Comparable[]{}));
                    searchFilters.add(searchFilter);
                });
            });
        });

        if (!searchFilters.isEmpty()) {
            var searchCompound = QueryBuilder.createNewCompoundFilter(QueryExpression.LogicOperator.OR, searchFilters.toArray(new QueryExpression.Filter[] {}));
            queryBuilder.addCompoundFilter(QueryExpression.LogicOperator.AND, searchCompound);
        }

        MapUtils.emptyIfNull(apiRequest.getQueryParams().getDistinctValues()).forEach((clazz, distinctFields) -> {
            distinctFields.asMap().forEach((keyField, labelFields) -> {
                labelFields.forEach(labelField -> {
                    if(StringUtils.isNotBlank(labelField)) {
                        queryBuilder.addDistinct(keyField, labelField);
                    } else {
                        queryBuilder.addDistinct(keyField);
                    }
                });
            });
        });

        MapUtils.emptyIfNull(apiRequest.getQueryParams().getMetaValues()).forEach((clazz, metaFields) -> {
            metaFields.forEach((valueField, labelValues) -> {
                labelValues.asMap().forEach((function, labelFields) -> {
                    labelFields.forEach(labelField -> {
                        if(StringUtils.isNotBlank(labelField)) {
                            queryBuilder.addMeta(labelField, function, valueField);
                        } else {
                            queryBuilder.addMeta(function, valueField);
                        }
                    });
                });
            });
        });

        return queryBuilder;
    }

    public QueryBuilder<T> setDistinct(boolean distinct) {
        this.pageRequest.setDistinctDataset(distinct);
        return this;
    }

    public QueryBuilder<T> setReadOnly(boolean readOnly) {
        this.pageRequest.setReadOnlyDataset(readOnly);
        return this;
    }

    public QueryBuilder<T> clearEntityGraphPaths() {
        this.pageRequest.setEntityGraphPaths(null);
        return this;
    }

    public QueryBuilder<T> addEntityGraphPaths(String... entityGraphPaths) {
        var newEntityGraphPaths = ArrayUtils.addAll(this.pageRequest.getEntityGraphPaths(), entityGraphPaths);
        this.pageRequest.setEntityGraphPaths(newEntityGraphPaths);
        return this;
    }

    public QueryBuilder<T> addSorting(String sortFieldPath, QuerySort.Direction direction) {
        this.pageRequest.addSort(this.pageRequest.getRootEntity(), sortFieldPath, direction);
        return this;
    }

    public QueryBuilder<T> addSorting(String... sorts) {
        Arrays.stream(sorts).forEach(sort -> this.pageRequest.addSort(this.pageRequest.getRootEntity(), sort));
        return this;
    }

    public QueryBuilder<T> setRootCompoundFilterLogicOperator(QueryExpression.LogicOperator logicOperator) {
        this.pageRequest.getQueryFilters().setLogicOperator(logicOperator);
        return this;
    }

    public <V extends Comparable<? super V>> QueryBuilder<T> addAndFilter(String fieldPath,
                                                                          QueryExpression.CompareOperator compareOperator,
                                                                          V... value) {
        QueryExpression.Filter<V> newFilter = new QueryExpression.Filter<>(this.pageRequest.getRootEntity(), fieldPath, compareOperator, value);
        this.addFilter(QueryExpression.LogicOperator.AND, newFilter);  //conjunction with existing filters
        return this;
    }

    public <V extends Comparable<? super V>> QueryBuilder<T> addOrFilter(String fieldPath,
                                                                         QueryExpression.CompareOperator compareOperator,
                                                                         V... value) {
        QueryExpression.Filter<V> newFilter = new QueryExpression.Filter<>(this.pageRequest.getRootEntity(), fieldPath, compareOperator, value);
        this.addFilter(QueryExpression.LogicOperator.OR, newFilter);  //disjunction with existing filters
        return this;
    }

    public <V extends Comparable<? super V>> QueryBuilder<T> addAndFilter(String fieldPath,
                                                                          QueryExpression.CompareOperator compareOperator,
                                                                          QueryExpression.ValueModifier valueModifier,
                                                                          V... value) {
        QueryExpression.Filter<V> newFilter = new QueryExpression.Filter<>(this.pageRequest.getRootEntity(), fieldPath, compareOperator, valueModifier, value);
        this.addFilter(QueryExpression.LogicOperator.AND, newFilter);  //conjunction with existing filters
        return this;
    }

    public <V extends Comparable<? super V>> QueryBuilder<T> addOrFilter(String fieldPath,
                                                                         QueryExpression.CompareOperator compareOperator,
                                                                         QueryExpression.ValueModifier valueModifier,
                                                                         V... value) {
        QueryExpression.Filter<V> newFilter = new QueryExpression.Filter<>(this.pageRequest.getRootEntity(), fieldPath, compareOperator, valueModifier, value);
        this.addFilter(QueryExpression.LogicOperator.OR, newFilter);  //disjunction with existing filters
        return this;
    }

    public <V extends Comparable<? super V>> QueryBuilder<T> addAndFilter(QueryExpression.Filter<V>... newFilters) {
        this.addFilter(QueryExpression.LogicOperator.AND, newFilters);  //conjunction with existing filters
        return this;
    }

    public <V extends Comparable<? super V>> QueryBuilder<T> addOrFilter(QueryExpression.Filter<V>... newFilters) {
        this.addFilter(QueryExpression.LogicOperator.OR, newFilters);  //disjunction with existing filters
        return this;
    }

    public <V extends Comparable<? super V>> QueryBuilder<T> addFilter(QueryExpression.LogicOperator logicOperator,
                                                                       QueryExpression.Filter<V>... newFilters) {
        var rootCompoundFilter = this.pageRequest.getQueryFilters();

        if (rootCompoundFilter.getLogicOperator() != logicOperator) {
            if (rootCompoundFilter.isEmpty()) {
                rootCompoundFilter.setLogicOperator(logicOperator);
                rootCompoundFilter.addFilters(newFilters);
            } else if (rootCompoundFilter.getLogicOperator() != logicOperator) {
                var newRootCompoundFilter = new QueryExpression.CompoundFilter(logicOperator, newFilters);
                newRootCompoundFilter.addCompoundFilters(rootCompoundFilter);

                //replace current with correct concatenation (conjunction/disjunction) of root+new
                this.pageRequest.setQueryFilters(newRootCompoundFilter);
            }
        } else {
            rootCompoundFilter.addFilters(newFilters);
        }

        return this;
    }

    public QueryBuilder<T> addCompoundFilter(QueryExpression.LogicOperator logicOperator,
                                             QueryExpression.CompoundFilter... newCompoundFilters) {
        var rootCompoundFilter = this.pageRequest.getQueryFilters();

        if (rootCompoundFilter.getLogicOperator() != logicOperator) {
            if (rootCompoundFilter.isEmpty()) {
                rootCompoundFilter.setLogicOperator(logicOperator);
                rootCompoundFilter.addCompoundFilters(newCompoundFilters);
            } else {
                var newRootCompoundFilter = new QueryExpression.CompoundFilter(logicOperator, newCompoundFilters);
                newRootCompoundFilter.addCompoundFilters(rootCompoundFilter);

                //replace current with correct concatenation (conjunction/disjunction) of root+new
                this.pageRequest.setQueryFilters(newRootCompoundFilter);
            }
        } else {
            rootCompoundFilter.addCompoundFilters(newCompoundFilters);
        }

        return this;
    }

    public QueryBuilder<T> addDistinct(String valueFieldPath) {
        QueryExpression newDistinctExpression = new QueryExpression(this.pageRequest.getRootEntity(), valueFieldPath);
        this.pageRequest.getDistinctColumns().add(newDistinctExpression);
        return this;
    }

    public QueryBuilder<T> addDistinct(String valueFieldPath, String labelFieldPath) {
        QueryExpression newDistinctExpression = new QueryExpression(this.pageRequest.getRootEntity(), valueFieldPath, labelFieldPath);
        this.pageRequest.getDistinctColumns().add(newDistinctExpression);
        return this;
    }

    public QueryBuilder<T> addDistinct(String name, String valueFieldPath, String labelFieldPath) {
        QueryExpression newDistinctExpression = new QueryExpression(name, this.pageRequest.getRootEntity(), valueFieldPath, labelFieldPath);
        this.pageRequest.getDistinctColumns().add(newDistinctExpression);
        return this;
    }

    public QueryBuilder<T> addMeta(QueryExpression.Function function, String valueFieldPath) {
        QueryExpression newMetaExpression = new QueryExpression(this.pageRequest.getRootEntity(), valueFieldPath, function);
        this.pageRequest.getMetaColumns().add(newMetaExpression);
        return this;
    }

    public QueryBuilder<T> addMeta(String labelFieldPath, QueryExpression.Function function, String valueFieldPath) {
        QueryExpression newMetaExpression = new QueryExpression(this.pageRequest.getRootEntity(), valueFieldPath, function, labelFieldPath);
        this.pageRequest.getMetaColumns().add(newMetaExpression);
        return this;
    }


    public QueryBuilder<T> addMeta(String name, String labelFieldPath, QueryExpression.Function function, String valueFieldPath) {
        QueryExpression newMetaExpression = new QueryExpression(name, this.pageRequest.getRootEntity(), valueFieldPath, function, labelFieldPath);
        this.pageRequest.getMetaColumns().add(newMetaExpression);
        return this;
    }

    public PageRequest<T> build() {
        return this.pageRequest;
    }

    //TODO: public static class QueryBuilder<T>
}
