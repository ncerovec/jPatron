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

import static info.nino.jpatron.request.QueryExpression.LABEL_PATHS_SEPARATOR;

public final class RequestBuilder<T> {

    private final EntityPageRequest<T> request;

    private RequestBuilder(EntityPageRequest<T> request) {
        this.request = request;
    }

    public EntityPageRequest<T> getPageRequest() {
        return request;
    }

    public static <V extends Comparable<? super V>> QueryExpression.Filter<V> createNewFilter(RequestBuilder<?> requestBuilder,
                                                                                              String fieldPath, QueryExpression.CompareOperator compareOperator,
                                                                                              V... value) {

        return new QueryExpression.Filter<>(requestBuilder.getPageRequest().getRootEntity(), fieldPath, compareOperator, value);
    }

    public static <V extends Comparable<? super V>> QueryExpression.Filter<V> createNewFilter(RequestBuilder<?> requestBuilder,
                                                                                              String fieldPath,
                                                                                              QueryExpression.CompareOperator compareOperator,
                                                                                              QueryExpression.ValueModifier valueModifier,
                                                                                              V... value) {
        return new QueryExpression.Filter<>(requestBuilder.getPageRequest().getRootEntity(), fieldPath, compareOperator, valueModifier, value);
    }

    public static QueryExpression.CompoundFilter createNewCompoundFilter(QueryExpression.LogicOperator logicOperator,
                                                                         QueryExpression.Filter<?>... filters) {
        return new QueryExpression.CompoundFilter(logicOperator, filters);
    }

    public static QueryExpression.CompoundFilter createNewCompoundFilter(QueryExpression.LogicOperator logicOperator,
                                                                         QueryExpression.CompoundFilter... compoundFilters) {
        return new QueryExpression.CompoundFilter(logicOperator, compoundFilters);
    }

    public static <T> RequestBuilder<T> init(Class<T> rootEntity) {
        return RequestBuilder.init(rootEntity, null, null);
    }

    public static <T> RequestBuilder<T> init(Class<T> rootEntity,
                                             Integer pageSize,
                                             Integer pageNumber) {
        EntityPageRequest<T> request = new EntityPageRequest<>(rootEntity, pageSize, pageNumber);
        return new RequestBuilder<T>(request);
    }

    public static <T> RequestBuilder<T> init(EntityPageRequest<T> request) {
        return new RequestBuilder<T>(request);
    }

    /**
     * Builder initializer for PageRequest (data-service) by generic ApiRequest (common-utils)
     * @param apiRequest generic ApiRequest (common-utils) parameter
     */
    public static <T> RequestBuilder<T> init(ApiRequest<T> apiRequest) {

        RequestBuilder<T> requestBuilder = RequestBuilder.init(apiRequest.getRootEntity(), apiRequest.getQueryParams().getPageSize(), apiRequest.getQueryParams().getPageNumber());

        requestBuilder.setDistinct(apiRequest.isDistinctDataset());
        requestBuilder.setReadOnly(apiRequest.isReadOnlyDataset());
        //requestBuilder.addFetchEntityPaths(apiRequest.getFetchEntityPaths());
        requestBuilder.addEntityGraphPaths(apiRequest.getEntityGraphPaths());

        //NEW: sorts
        apiRequest.getQueryParams().getSorts().forEach(sort ->
                requestBuilder.addSorting(sort.getColumnEntityPath().getValue(), sort.getDirection())
        );

        //LEGACY: sorts
        MapUtils.emptyIfNull(apiRequest.getQueryParams().getLegacySort()).forEach((fieldPath, clazzDirectionEntry) -> {
            requestBuilder.addSorting(fieldPath, clazzDirectionEntry.getValue());
        });

        //NEW: compound-filters
        if (apiRequest.getQueryParams().getCompoundFilter() != null) {
            requestBuilder.addCompoundFilter(QueryExpression.LogicOperator.AND, apiRequest.getQueryParams().getCompoundFilter());
        }

        //NEW: filters
        requestBuilder.addAndFilter(apiRequest.getQueryParams().getFilters().toArray(QueryExpression.Filter[]::new));

        //LEGACY: filters
        MapUtils.emptyIfNull(apiRequest.getQueryParams().getLegacyFilters()).forEach((clazz, filterPaths) -> {
            filterPaths.forEach((fieldPath, filterValues) -> {
                filterValues.asMap().forEach((compareOperator, values) -> {
                    requestBuilder.addAndFilter(fieldPath, compareOperator, values.toArray(new Comparable[]{}));
                });
            });
        });

        //NEW: searches
        requestBuilder.addOrFilter(apiRequest.getQueryParams().getSearches().toArray(QueryExpression.Search[]::new));

        //LEGACY: searches
        List<QueryExpression.Filter<?>> searchFilters = new ArrayList<>();
        MapUtils.emptyIfNull(apiRequest.getQueryParams().getLegacySearches()).forEach((clazz, searchFields) -> {
            searchFields.forEach((fieldPath, searchValues) -> {
                searchValues.asMap().forEach((valueModifier, values) -> {
                    QueryExpression.CompareOperator compareOperator = QueryExpression.CompareOperator.LIKE;
                    var searchFilter = RequestBuilder.createNewFilter(requestBuilder, fieldPath, compareOperator, valueModifier, values.toArray(new Comparable[]{}));
                    searchFilters.add(searchFilter);
                });
            });
        });

        if (!searchFilters.isEmpty()) {
            var searchCompound = RequestBuilder.createNewCompoundFilter(QueryExpression.LogicOperator.OR, searchFilters.toArray(new QueryExpression.Filter[] {}));
            requestBuilder.addCompoundFilter(QueryExpression.LogicOperator.AND, searchCompound);
        }

        //NEW: distinct-columns
        requestBuilder.getPageRequest().getDistinctColumns().addAll(apiRequest.getQueryParams().getDistinctColumns());

        //LEGACY: distinct-columns
        MapUtils.emptyIfNull(apiRequest.getQueryParams().getLegacyDistinctValues()).forEach((clazz, distinctFields) -> {
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

        //NEW: meta-columns
        requestBuilder.getPageRequest().getMetaColumns().addAll(apiRequest.getQueryParams().getMetaColumns());

        //LEGACY: meta-columns
        MapUtils.emptyIfNull(apiRequest.getQueryParams().getLegacyMetaValues()).forEach((clazz, metaFields) -> {
            metaFields.forEach((valueField, labelValues) -> {
                labelValues.asMap().forEach((function, labelFields) -> {
                    labelFields.forEach(labelFieldPath -> {
                        if(StringUtils.isNotBlank(labelFieldPath)) {
                            String[] labelFieldPaths = Arrays.stream(labelFieldPath.split(LABEL_PATHS_SEPARATOR))
                                    .map(String::trim).toArray(String[]::new);
                            requestBuilder.addMeta(valueField, function, labelFieldPaths);
                        } else {
                            requestBuilder.addMeta(valueField, function);
                        }
                    });
                });
            });
        });

        return requestBuilder;
    }

    public RequestBuilder<T> setDistinct(boolean distinct) {
        this.request.setDistinctDataset(distinct);
        return this;
    }

    public RequestBuilder<T> setReadOnly(boolean readOnly) {
        this.request.setReadOnlyDataset(readOnly);
        return this;
    }

    public RequestBuilder<T> clearEntityGraphPaths() {
        this.request.setEntityGraphPaths(null);
        return this;
    }

    public RequestBuilder<T> addEntityGraphPaths(String... entityGraphPaths) {
        var newEntityGraphPaths = ArrayUtils.addAll(this.request.getEntityGraphPaths(), entityGraphPaths);
        this.request.setEntityGraphPaths(newEntityGraphPaths);
        return this;
    }

    public RequestBuilder<T> addSorting(String sortFieldPath, QuerySort.Direction direction) {
        this.request.addSort(this.request.getRootEntity(), sortFieldPath, direction);
        return this;
    }

    public RequestBuilder<T> addSorting(String... sorts) {
        Arrays.stream(sorts).forEach(sort -> this.request.addSort(this.request.getRootEntity(), sort));
        return this;
    }

    public RequestBuilder<T> setRootCompoundFilterLogicOperator(QueryExpression.LogicOperator logicOperator) {
        this.request.getQueryFilters().setLogicOperator(logicOperator);
        return this;
    }

    public <V extends Comparable<? super V>> RequestBuilder<T> addAndFilter(String fieldPath,
                                                                            QueryExpression.CompareOperator compareOperator,
                                                                            V... value) {
        QueryExpression.Filter<V> newFilter = new QueryExpression.Filter<>(this.request.getRootEntity(), fieldPath, compareOperator, value);
        this.addFilter(QueryExpression.LogicOperator.AND, newFilter);  //conjunction with existing filters
        return this;
    }

    public <V extends Comparable<? super V>> RequestBuilder<T> addOrFilter(String fieldPath,
                                                                           QueryExpression.CompareOperator compareOperator,
                                                                           V... value) {
        QueryExpression.Filter<V> newFilter = new QueryExpression.Filter<>(this.request.getRootEntity(), fieldPath, compareOperator, value);
        this.addFilter(QueryExpression.LogicOperator.OR, newFilter);  //disjunction with existing filters
        return this;
    }

    public <V extends Comparable<? super V>> RequestBuilder<T> addAndFilter(String fieldPath,
                                                                            QueryExpression.CompareOperator compareOperator,
                                                                            QueryExpression.ValueModifier valueModifier,
                                                                            V... value) {
        QueryExpression.Filter<V> newFilter = new QueryExpression.Filter<>(this.request.getRootEntity(), fieldPath, compareOperator, valueModifier, value);
        this.addFilter(QueryExpression.LogicOperator.AND, newFilter);  //conjunction with existing filters
        return this;
    }

    public <V extends Comparable<? super V>> RequestBuilder<T> addOrFilter(String fieldPath,
                                                                           QueryExpression.CompareOperator compareOperator,
                                                                           QueryExpression.ValueModifier valueModifier,
                                                                           V... value) {
        QueryExpression.Filter<V> newFilter = new QueryExpression.Filter<>(this.request.getRootEntity(), fieldPath, compareOperator, valueModifier, value);
        this.addFilter(QueryExpression.LogicOperator.OR, newFilter);  //disjunction with existing filters
        return this;
    }

    public <V extends Comparable<? super V>> RequestBuilder<T> addAndFilter(QueryExpression.Filter<V>... newFilters) {
        this.addFilter(QueryExpression.LogicOperator.AND, newFilters);  //conjunction with existing filters
        return this;
    }

    public <V extends Comparable<? super V>> RequestBuilder<T> addOrFilter(QueryExpression.Filter<V>... newFilters) {
        this.addFilter(QueryExpression.LogicOperator.OR, newFilters);  //disjunction with existing filters
        return this;
    }

    public <V extends Comparable<? super V>> RequestBuilder<T> addFilter(QueryExpression.LogicOperator logicOperator,
                                                                         QueryExpression.Filter<V>... newFilters) {
        var rootCompoundFilter = this.request.getQueryFilters();

        if (rootCompoundFilter.getLogicOperator() != logicOperator) {
            if (rootCompoundFilter.isEmpty()) {
                rootCompoundFilter.setLogicOperator(logicOperator);
                rootCompoundFilter.addFilters(newFilters);
            } else if (rootCompoundFilter.getLogicOperator() != logicOperator) {
                var newRootCompoundFilter = new QueryExpression.CompoundFilter(logicOperator, newFilters);
                newRootCompoundFilter.addCompoundFilters(rootCompoundFilter);

                //replace current with correct concatenation (conjunction/disjunction) of root+new
                this.request.setQueryFilters(newRootCompoundFilter);
            }
        } else {
            rootCompoundFilter.addFilters(newFilters);
        }

        return this;
    }

    public RequestBuilder<T> addCompoundFilter(QueryExpression.LogicOperator logicOperator,
                                               QueryExpression.CompoundFilter... newCompoundFilters) {
        var rootCompoundFilter = this.request.getQueryFilters();

        if (rootCompoundFilter.getLogicOperator() != logicOperator) {
            if (rootCompoundFilter.isEmpty()) {
                rootCompoundFilter.setLogicOperator(logicOperator);
                rootCompoundFilter.addCompoundFilters(newCompoundFilters);
            } else {
                var newRootCompoundFilter = new QueryExpression.CompoundFilter(logicOperator, newCompoundFilters);
                newRootCompoundFilter.addCompoundFilters(rootCompoundFilter);

                //replace current with correct concatenation (conjunction/disjunction) of root+new
                this.request.setQueryFilters(newRootCompoundFilter);
            }
        } else {
            rootCompoundFilter.addCompoundFilters(newCompoundFilters);
        }

        return this;
    }

    public RequestBuilder<T> addDistinct(String valueFieldPath) {
        QueryExpression newDistinctExpression = new QueryExpression(this.request.getRootEntity(), valueFieldPath);
        this.request.getDistinctColumns().add(newDistinctExpression);
        return this;
    }

    public RequestBuilder<T> addDistinct(String valueFieldPath, String labelFieldPath) {
        QueryExpression newDistinctExpression = new QueryExpression(this.request.getRootEntity(), valueFieldPath, labelFieldPath);
        this.request.getDistinctColumns().add(newDistinctExpression);
        return this;
    }

    public RequestBuilder<T> addDistinct(String name, String valueFieldPath, String labelFieldPath) {
        QueryExpression newDistinctExpression = new QueryExpression(name, this.request.getRootEntity(), valueFieldPath, labelFieldPath);
        this.request.getDistinctColumns().add(newDistinctExpression);
        return this;
    }

    public RequestBuilder<T> addMeta(String valueFieldPath, QueryExpression.Function function) {
        QueryExpression newMetaExpression = new QueryExpression(this.request.getRootEntity(), valueFieldPath, function);
        this.request.getMetaColumns().add(newMetaExpression);
        return this;
    }

    public RequestBuilder<T> addMeta(String valueFieldPath, QueryExpression.Function function, String... labelFieldPath) {
        QueryExpression newMetaExpression = new QueryExpression(this.request.getRootEntity(), valueFieldPath, function, labelFieldPath);
        this.request.getMetaColumns().add(newMetaExpression);
        return this;
    }


    public RequestBuilder<T> addMeta(String name, String valueFieldPath, QueryExpression.Function function, String... labelFieldPath) {
        QueryExpression newMetaExpression = new QueryExpression(name, this.request.getRootEntity(), valueFieldPath, function, labelFieldPath);
        this.request.getMetaColumns().add(newMetaExpression);
        return this;
    }

    public EntityPageRequest<T> build() {
        return this.request;
    }

    //TODO: public static class QueryBuilder<T>
}
