package info.nino.jpatron.request;

import org.apache.commons.collections4.MultiValuedMap;

import java.io.Serializable;
import java.util.Map;

public abstract class ApiRequest implements Serializable
{
    protected QueryParams queryParams;
    protected boolean pagination;
    protected boolean distinctDataset;
    protected boolean readOnlyDataset;
    protected String[] fetchEntityPaths;      //paths for related entities to fetch
    protected String[] entityGraphPaths;      //paths for related entities to load

    public ApiRequest()
    {

    }

    public ApiRequest(QueryParams queryParams, boolean pagination, boolean distinct, boolean readOnly, String[] fetchEntityPaths, String[] entityGraphPaths)
    {
        this.queryParams = queryParams;
        this.pagination = pagination;
        this.distinctDataset = distinct;
        this.readOnlyDataset = readOnly;
        this.fetchEntityPaths = fetchEntityPaths;
        this.entityGraphPaths = entityGraphPaths;
    }

    public QueryParams getQueryParams()
    {
        return queryParams;
    }

    public void setQueryParams(QueryParams queryParams)
    {
        this.queryParams = queryParams;
    }

    public boolean isPagination()
    {
        return pagination;
    }

    public void setPagination(boolean pagination)
    {
        this.pagination = pagination;
    }

    public boolean isDistinctDataset()
    {
        return distinctDataset;
    }

    public void setDistinctDataset(boolean distinctDataset)
    {
        this.distinctDataset = distinctDataset;
    }

    public boolean isReadOnlyDataset()
    {
        return readOnlyDataset;
    }

    public void setReadOnlyDataset(boolean readOnlyDataset)
    {
        this.readOnlyDataset = readOnlyDataset;
    }

    public String[] getFetchEntityPaths()
    {
        return fetchEntityPaths;
    }

    public void setFetchEntityPaths(String[] fetchEntityPaths)
    {
        this.fetchEntityPaths = fetchEntityPaths;
    }

    public String[] getEntityGraphPaths()
    {
        return entityGraphPaths;
    }

    public void setEntityGraphPaths(String[] entityGraphPaths)
    {
        this.entityGraphPaths = entityGraphPaths;
    }

    public static abstract class QueryParams implements Serializable
    {
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
