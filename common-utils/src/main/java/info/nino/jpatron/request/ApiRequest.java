package info.nino.jpatron.request;

import org.apache.commons.collections4.MultiValuedMap;

import java.io.Serializable;
import java.util.Map;

/**
 *  Generic abstract class for API request implementations
 *  It serves as abstract type which can be used across jPatron library artefacts with the concrete implementation in final interface artefact
 */
public abstract class ApiRequest implements Serializable
{
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
    public ApiRequest()
    {
        super();
    }

    /**
     * Default constructor - with all mandatory parameters
     * @param queryParams {@link ApiRequest#queryParams}
     * @param distinct {@link ApiRequest#distinctDataset}
     * @param readOnly {@link ApiRequest#readOnlyDataset}
     * @param fetchEntityPaths {@link ApiRequest#fetchEntityPaths}
     * @param entityGraphPaths {@link ApiRequest#entityGraphPaths}
     */
    public ApiRequest(QueryParams queryParams, boolean distinct, boolean readOnly, String[] fetchEntityPaths, String[] entityGraphPaths)
    {
        this.queryParams = queryParams;
        this.distinctDataset = distinct;
        this.readOnlyDataset = readOnly;
        this.fetchEntityPaths = fetchEntityPaths;
        this.entityGraphPaths = entityGraphPaths;
    }

    /**
     * {@link ApiRequest#queryParams}
     * @return queryParams
     */
    public QueryParams getQueryParams()
    {
        return queryParams;
    }

    /**
     * {@link ApiRequest#queryParams}
     * @param queryParams object
     */
    public void setQueryParams(QueryParams queryParams)
    {
        this.queryParams = queryParams;
    }

    /**
     * {@link ApiRequest#distinctDataset}
     * @return distinctDataset flag
     */
    public boolean isDistinctDataset()
    {
        return distinctDataset;
    }

    /**
     * {@link ApiRequest#distinctDataset}
     * @param distinctDataset flag
     */
    public void setDistinctDataset(boolean distinctDataset)
    {
        this.distinctDataset = distinctDataset;
    }

    /**
     * {@link ApiRequest#readOnlyDataset}
     * @return readOnlyDataset flag
     */
    public boolean isReadOnlyDataset()
    {
        return readOnlyDataset;
    }

    /**
     * {@link ApiRequest#readOnlyDataset}
     * @param readOnlyDataset flag
     */
    public void setReadOnlyDataset(boolean readOnlyDataset)
    {
        this.readOnlyDataset = readOnlyDataset;
    }

    /**
     * {@link ApiRequest#fetchEntityPaths}
     * @return fetchEntityPaths array
     */
    public String[] getFetchEntityPaths()
    {
        return fetchEntityPaths;
    }

    /**
     * {@link ApiRequest#fetchEntityPaths}
     * @param fetchEntityPaths array
     */
    public void setFetchEntityPaths(String[] fetchEntityPaths)
    {
        this.fetchEntityPaths = fetchEntityPaths;
    }

    /**
     * {@link ApiRequest#entityGraphPaths}
     * @return entityGraphPaths array
     */
    public String[] getEntityGraphPaths()
    {
        return entityGraphPaths;
    }

    /**
     * {@link ApiRequest#entityGraphPaths}
     * @param entityGraphPaths array
     */
    public void setEntityGraphPaths(String[] entityGraphPaths)
    {
        this.entityGraphPaths = entityGraphPaths;
    }

    /**
     *  Generic abstract class for request query-params implementations
     *  It serves as abstract type which can be used across jPatron library artefacts with the concrete implementation in final interface artefact
     */
    public static abstract class QueryParams implements Serializable
    {
        /**
         *
         */
        protected Integer pageSize;
        protected Integer pageNumber;
        protected Map<String, Map.Entry<Class<?>, String>> sort;                          //SortFieldPath : Entity, SortDirection
        protected MultiValuedMap<Class<?>, String> includes;                              //Entity -> IncludeEntityPath(s)
        protected Map<Class<?>, Map<String, MultiValuedMap<String, String>>> filters;     //Entity -> FilterFieldPath -> Operator : FilterValue(s)
        protected Map<Class<?>, Map<String, MultiValuedMap<String, String>>> searches;    //Entity -> SearchFieldPath -> Modifier : SearchValue(s)
        protected Map<Class<?>, MultiValuedMap<String, String>> distinctValues;           //Entity -> KeyFieldPath : LabelFieldPath(s)
        protected Map<Class<?>, Map<String, MultiValuedMap<String, String>>> metaValues;  //Entity -> ValueFieldPath -> Function : LabelFieldPath(s)

        public QueryParams(Integer pageSize, Integer pageNumber)
        {
            this.pageSize = pageSize;
            this.pageNumber = pageNumber;
        }

        public Integer getPageSize()
        {
            return pageSize;
        }

        public void setPageSize(Integer pageSize)
        {
            this.pageSize = pageSize;
        }

        public Integer getPageNumber()
        {
            return pageNumber;
        }

        public void setPageNumber(Integer pageNumber)
        {
            this.pageNumber = pageNumber;
        }

        public Map<String, Map.Entry<Class<?>, String>> getSort()
        {
            return sort;
        }

        public void setSort(Map<String, Map.Entry<Class<?>, String>> sort)
        {
            this.sort = sort;
        }

        public MultiValuedMap<Class<?>, String> getIncludes()
        {
            return includes;
        }

        public void setIncludes(MultiValuedMap<Class<?>, String> includes)
        {
            this.includes = includes;
        }

        public Map<Class<?>, Map<String, MultiValuedMap<String, String>>> getFilters()
        {
            return filters;
        }

        public void setFilters(Map<Class<?>, Map<String, MultiValuedMap<String, String>>> filters)
        {
            this.filters = filters;
        }

        public Map<Class<?>, Map<String, MultiValuedMap<String, String>>> getSearches()
        {
            return searches;
        }

        public void setSearches(Map<Class<?>, Map<String, MultiValuedMap<String, String>>> searches)
        {
            this.searches = searches;
        }

        public Map<Class<?>, MultiValuedMap<String, String>> getDistinctValues()
        {
            return distinctValues;
        }

        public void setDistinctValues(Map<Class<?>, MultiValuedMap<String, String>> distinctValues)
        {
            this.distinctValues = distinctValues;
        }

        public Map<Class<?>, Map<String, MultiValuedMap<String, String>>> getMetaValues()
        {
            return metaValues;
        }

        public void setMetaValues(Map<Class<?>, Map<String, MultiValuedMap<String, String>>> metaValues)
        {
            this.metaValues = metaValues;
        }
    }
}
