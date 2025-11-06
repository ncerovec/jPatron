package info.nino.jpatron.request;

import org.apache.commons.collections4.MultiValuedMap;

import java.io.Serializable;
import java.util.*;

/**
 *  Generic abstract class for API request implementations
 *  It serves as abstract type which can be used across jPatron library artefacts with the concrete implementation in final interface artefact
 */
public abstract class ApiRequest<T> implements Serializable {

    /**
     * Root Entity for the request
     */
    protected Class<T> rootEntity;

    /**
     * Request query parameters
     */
    protected QueryParams queryParams;

    /**
     * Flag if result-set should be distinct or not
     */
    protected boolean distinctDataset;

    /**
     * Flag if result-set should be read-only or not
     */
    protected boolean readOnlyDataset;

    /**
     * Paths for related entities to fetch
     */
    protected String[] fetchEntityPaths;

    /**
     * Paths for related entities to load along with
     */
    protected String[] entityGraphPaths;

    /**
     * Default empty-constructor - all properties should be set manually
     */
    protected ApiRequest() {
        super();
    }

    /**
     * Default constructor - with all mandatory parameters
     * @param rootEntity {@link ApiRequest#rootEntity}
     * @param queryParams {@link ApiRequest#queryParams}
     * @param distinct {@link ApiRequest#distinctDataset}
     * @param readOnly {@link ApiRequest#readOnlyDataset}
     * @param fetchEntityPaths {@link ApiRequest#fetchEntityPaths}
     * @param entityGraphPaths {@link ApiRequest#entityGraphPaths}
     */
    public ApiRequest(Class<T> rootEntity,
                      QueryParams queryParams,
                      boolean distinct,
                      boolean readOnly,
                      String[] fetchEntityPaths,
                      String[] entityGraphPaths) {
        this.rootEntity = rootEntity;
        this.queryParams = queryParams;
        this.distinctDataset = distinct;
        this.readOnlyDataset = readOnly;
        this.fetchEntityPaths = fetchEntityPaths;
        this.entityGraphPaths = entityGraphPaths;
    }

    public abstract Class<? extends SortDirectionEnum> getSortDirectionEnum();
    public abstract Class<? extends CompounderEnum> getCompounderEnum();
    public abstract Class<? extends ComparatorEnum> getComparatorEnum();
    public abstract Class<? extends FunctionEnum> getFunctionEnum();

    /**
     * {@link ApiRequest#rootEntity}
     * @return rootEntity
     */
    public Class<T> getRootEntity() {
        return rootEntity;
    }

    /**
     * {@link ApiRequest#rootEntity}
     * @param rootEntity object
     */
    public void setRootEntity(Class<T> rootEntity) {
        this.rootEntity = rootEntity;
    }

    /**
     * {@link ApiRequest#queryParams}
     * @return queryParams
     */
    public QueryParams getQueryParams() {
        return queryParams;
    }

    /**
     * {@link ApiRequest#queryParams}
     * @param queryParams object
     */
    public void setQueryParams(QueryParams queryParams) {
        this.queryParams = queryParams;
    }

    /**
     * {@link ApiRequest#distinctDataset}
     * @return distinctDataset flag
     */
    public boolean isDistinctDataset() {
        return distinctDataset;
    }

    /**
     * {@link ApiRequest#distinctDataset}
     * @param distinctDataset flag
     */
    public void setDistinctDataset(boolean distinctDataset) {
        this.distinctDataset = distinctDataset;
    }

    /**
     * {@link ApiRequest#readOnlyDataset}
     * @return readOnlyDataset flag
     */
    public boolean isReadOnlyDataset() {
        return readOnlyDataset;
    }

    /**
     * {@link ApiRequest#readOnlyDataset}
     * @param readOnlyDataset flag
     */
    public void setReadOnlyDataset(boolean readOnlyDataset) {
        this.readOnlyDataset = readOnlyDataset;
    }

    /**
     * {@link ApiRequest#fetchEntityPaths}
     * @return fetchEntityPaths array
     */
    public String[] getFetchEntityPaths() {
        return fetchEntityPaths;
    }

    /**
     * {@link ApiRequest#fetchEntityPaths}
     * @param fetchEntityPaths array
     */
    public void setFetchEntityPaths(String[] fetchEntityPaths) {
        this.fetchEntityPaths = fetchEntityPaths;
    }

    /**
     * {@link ApiRequest#entityGraphPaths}
     * @return entityGraphPaths array
     */
    public String[] getEntityGraphPaths() {
        return entityGraphPaths;
    }

    /**
     * {@link ApiRequest#entityGraphPaths}
     * @param entityGraphPaths array
     */
    public void setEntityGraphPaths(String[] entityGraphPaths) {
        this.entityGraphPaths = entityGraphPaths;
    }

    /**
     *  Generic abstract class for request query-params implementations
     *  It serves as abstract type which can be used across jPatron library artefacts with the concrete implementation in final interface artefact
     */
    public static class QueryParams implements Serializable {

        private Integer pageSize;
        private Integer pageNumber;

        //list of singular filters  (replaces legacyFilters property)
        private LinkedHashSet<QuerySort> sorts = new LinkedHashSet<>();

        //Entity -> IncludeEntityPath(s)
        private MultiValuedMap<Class<?>, String> includes;

        //list of singular filters  (replaces legacyFilters property)
        private List<QueryExpression.Filter<?>> filters = new ArrayList<>();

        //compound/complex filter in nested object hierarchy
        private QueryExpression.CompoundFilter compoundFilter;

        //list of singular searches (replaces legacySearches property)
        private List<QueryExpression.Search> searches = new ArrayList<>();

        //set of requested distinct-value columns (replaces legacyDistinctValues property)
        private Set<QueryExpression> distinctColumns = new HashSet<>();

        //set of requested meta-value columns (replaces legacyMetaValues property)
        private Set<QueryExpression> metaColumns = new HashSet<>();

        @Deprecated
        //SortFieldPath : Entity, SortDirection
        private Map<String, Map.Entry<Class<?>, QuerySort.Direction>> legacySort;

        @Deprecated
        //Entity -> FilterFieldPath -> Operator : FilterValue(s)
        private Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.CompareOperator, String>>> legacyFilters;

        @Deprecated
        //Entity -> SearchFieldPath -> Modifier : SearchValue(s)
        private Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.ValueModifier, String>>> legacySearches;

        @Deprecated
        //Entity -> KeyFieldPath : LabelFieldPath(s)
        private Map<Class<?>, MultiValuedMap<String, String>> legacyDistinctValues;

        @Deprecated
        //Entity -> ValueFieldPath -> Function : LabelFieldPath(s)
        private Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.Function, String>>> legacyMetaValues;

        public QueryParams(Integer pageSize, Integer pageNumber) {
            this.pageSize = pageSize;
            this.pageNumber = pageNumber;
        }

        public Integer getPageSize() {
            return pageSize;
        }

        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }

        public Integer getPageNumber() {
            return pageNumber;
        }

        public void setPageNumber(Integer pageNumber) {
            this.pageNumber = pageNumber;
        }

        public LinkedHashSet<QuerySort> getSorts()
        {
            return sorts;
        }

        public void setSorts(LinkedHashSet<QuerySort> sorts)
        {
            this.sorts = sorts;
        }

        public MultiValuedMap<Class<?>, String> getIncludes() {
            return includes;
        }

        public void setIncludes(MultiValuedMap<Class<?>, String> includes) {
            this.includes = includes;
        }

        public List<QueryExpression.Filter<?>> getFilters()
        {
            return filters;
        }

        public void setFilters(List<QueryExpression.Filter<?>> filters)
        {
            this.filters = filters;
        }

        public QueryExpression.CompoundFilter getCompoundFilter() {
            return compoundFilter;
        }

        public void setCompoundFilter(QueryExpression.CompoundFilter compoundFilter) {
            this.compoundFilter = compoundFilter;
        }

        public List<QueryExpression.Search> getSearches()
        {
            return searches;
        }

        public void setSearches(List<QueryExpression.Search> searches)
        {
            this.searches = searches;
        }

        public Set<QueryExpression> getDistinctColumns()
        {
            return distinctColumns;
        }

        public void setDistinctColumns(Set<QueryExpression> distinctColumns)
        {
            this.distinctColumns = distinctColumns;
        }

        public Set<QueryExpression> getMetaColumns()
        {
            return metaColumns;
        }

        public void setMetaColumns(Set<QueryExpression> metaColumns)
        {
            this.metaColumns = metaColumns;
        }

        @Deprecated
        public Map<String, Map.Entry<Class<?>, QuerySort.Direction>> getLegacySort() {
            return legacySort;
        }

        @Deprecated
        public void setLegacySort(Map<String, Map.Entry<Class<?>, QuerySort.Direction>> legacySort) {
            this.legacySort = legacySort;
        }

        @Deprecated
        public Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.CompareOperator, String>>> getLegacyFilters() {
            return legacyFilters;
        }

        @Deprecated
        public void setLegacyFilters(
                Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.CompareOperator, String>>> legacyFilters) {
            this.legacyFilters = legacyFilters;
        }

        @Deprecated
        public Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.ValueModifier, String>>> getLegacySearches() {
            return legacySearches;
        }

        @Deprecated
        public void setLegacySearches(
                Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.ValueModifier, String>>> legacySearches) {
            this.legacySearches = legacySearches;
        }

        @Deprecated
        public Map<Class<?>, MultiValuedMap<String, String>> getLegacyDistinctValues() {
            return legacyDistinctValues;
        }

        @Deprecated
        public void setLegacyDistinctValues(
                Map<Class<?>, MultiValuedMap<String, String>> legacyDistinctValues) {
            this.legacyDistinctValues = legacyDistinctValues;
        }

        @Deprecated
        public Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.Function, String>>> getLegacyMetaValues() {
            return legacyMetaValues;
        }

        @Deprecated
        public void setLegacyMetaValues(
                Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.Function, String>>> legacyMetaValues) {
            this.legacyMetaValues = legacyMetaValues;
        }
    }

    public interface SortDirectionEnum extends ValueEnum {
        QuerySort.Direction toQuerySortDirection();
    }

    public interface CompounderEnum extends ValueEnum {
        QueryExpression.LogicOperator toQueryLogicOperator();
    }

    public interface ComparatorEnum extends ValueEnum {
        QueryExpression.CompareOperator toQueryComparator();
    }

    public interface FunctionEnum extends ValueEnum {
        QueryExpression.Function toQueryFunction();
    }

    public interface ValueEnum {
        String getValue();

        static <T extends ValueEnum> T findByValue(Class<T> enumClass, String value) {
            return Arrays.stream(enumClass.getEnumConstants())
                    .filter(e -> e.getValue().equals(value))
                    .findAny().orElseThrow();
        }
    }
}
