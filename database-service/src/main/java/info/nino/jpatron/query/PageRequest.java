package info.nino.jpatron.query;

import info.nino.jpatron.request.QueryExpression;
import info.nino.jpatron.request.QuerySort;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Main Entity Service request object
 * Contains parameters used for data/distinct/meta queries
 */
public class PageRequest<T>
{
    /**
     * Requested root Entity of the PageRequest
     */
    private Class<T> rootEntity;

    /**
     * Requested page size
     */
    private Integer pageSize;

    /**
     * Requested page number
     */
    private Integer pageNumber;

    /**
     * true/false flag for distinct-list of the result list
     */
    private boolean distinctDataset = true;

    /**
     * true/false flag for the read-only result list
     */
    private boolean readOnlyDataset = true;

    /**
     * Paths for related entities to fetch (along with base resource)
     */
    private String[] fetchEntityPaths;

    /**
     * Paths for related entities to load (along with base resource)
     */
    private String[] entityGraphPaths;

    /**
     * Set of sorting parameters of QuerySort type
     */
    private Set<QuerySort> sorts = new LinkedHashSet<>();

    /**
     * EntityService query-filters
     * Filters which should be used to construct SQL 'where' clause
     * Filters are applied to every query in the request (data/distinct/meta)
     * //TODO: replace unnecessary initialization with null-check
     */
    private QueryExpression.CompoundFilter queryFilters = new QueryExpression.CompoundFilter();

    /**
     * EntityService distinct-values query request
     * Columns which should be returned as result of distinct-query result
     */
    private Set<QueryExpression> distinctColumns = new HashSet<>();


    /**
     * EntityService meta-values query request
     * Columns which should be returned as result of aggregate-query result
     */
    private Set<QueryExpression> metaColumns = new HashSet<>();

    /**
     * Constructor for PageRequest (data-service) with custom properties
     * @param rootEntity requested Entity result
     */
    public PageRequest(Class<T> rootEntity)
    {
        this.rootEntity = rootEntity;
    }

    /**
     * Constructor for PageRequest (data-service) with custom properties
     * @param rootEntity requested Entity result
     * @param pageSize requested page size
     * @param pageNumber requested page number
     */
    public PageRequest(Class<T> rootEntity, Integer pageSize, Integer pageNumber)
    {
        this.rootEntity = rootEntity;
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
    }

    /**
     * Constructor for PageRequest (data-service) with custom properties
     * @param rootEntity requested Entity result
     * @param pageSize requested size of the result page
     * @param pageNumber requested number of the result page
     * @param distinct true/false flag for distinct-list of the result list
     * @param readOnly true/false flag for the read-only result list
     */
    public PageRequest(Class<T> rootEntity, Integer pageSize, Integer pageNumber, boolean distinct, boolean readOnly)
    {
        this(rootEntity, pageSize, pageNumber);
        this.distinctDataset = distinct;
        this.readOnlyDataset = readOnly;
    }

    /**
     * Constructor for PageRequest (data-service) with custom properties
     * @param rootEntity requested Entity result
     * @param pageSize requested size of the result page
     * @param pageNumber requested number of the result page
     * @param sort set of sorting parameters of String type (e.g. "-name")
     */
    public PageRequest(Class<T> rootEntity, Integer pageSize, Integer pageNumber, Set<String> sort)
    {
        this(rootEntity, pageSize, pageNumber);
        if(sort != null) sort.forEach(s -> this.addSort(rootEntity, s));
    }

    /**
     * Constructor for PageRequest (data-service) with custom properties
     * @param rootEntity requested Entity result
     * @param pageSize requested size of the result page
     * @param pageNumber requested number of the result page
     * @param distinct true/false flag for distinct-list of the result list
     * @param readOnly true/false flag for the read-only result list
     * @param sorts set of sorting parameters of Sort type
     */
    public PageRequest(Class<T> rootEntity, Integer pageSize, Integer pageNumber, boolean distinct, boolean readOnly, Set<QuerySort> sorts)
    {
        this(rootEntity, pageSize, pageNumber);
        this.distinctDataset = distinct;
        this.readOnlyDataset = readOnly;
        this.sorts = sorts;
    }

    /**
     * Requested root Entity of the PageRequest
     * @return rootEntity
     */
    public Class<T> getRootEntity()
    {
        return rootEntity;
    }

    /**
     * Requested page number
     * @return pageNumber
     */
    public Integer getPageNumber()
    {
        return pageNumber;
    }

    /**
     * Requested page size
     * @return pageSize
     */
    public Integer getPageSize()
    {
        return pageSize;
    }

    /**
     * Boolean flag if result-set should be read-only
     * @return readOnlyDataset flag
     */
    public boolean isReadOnlyDataset()
    {
        return readOnlyDataset;
    }

    /**
     * Set requested result-set to be read-only
     * @param readOnlyDataset boolean flag
     */
    public void setReadOnlyDataset(boolean readOnlyDataset)
    {
        this.readOnlyDataset = readOnlyDataset;
    }

    /**
     * Boolean flag if result-set should be distinct
     * @return distinctDataset flag
     */
    public boolean isDistinctDataset()
    {
        return distinctDataset;
    }

    /**
     * set requested result-set to be distinct
     * @param distinctDataset boolean flag
     */
    public void setDistinctDataset(boolean distinctDataset)
    {
        this.distinctDataset = distinctDataset;
    }

    /**
     * Paths for related entities to fetch (along with base resource)
     * @return fetchEntityPaths array
     */
    public String[] getFetchEntityPaths()
    {
        return fetchEntityPaths;
    }

    /**
     * Paths for related entities to fetch (along with base resource)
     * @param fetchEntityPaths array
     */
    public void setFetchEntityPaths(String[] fetchEntityPaths)
    {
        this.fetchEntityPaths = fetchEntityPaths;
    }

    /**
     * Paths for related entities to load (along with base resource)
     * @return entityGraphPaths array
     */
    public String[] getEntityGraphPaths()
    {
        return entityGraphPaths;
    }

    /**
     * Paths for related entities to load (along with base resource)
     * @param entityGraphPaths array
     */
    public void setEntityGraphPaths(String[] entityGraphPaths)
    {
        this.entityGraphPaths = entityGraphPaths;
    }

    /**
     * Adds sorting parameter to PageRequest object
     * @param rootEntity query root entity (base for column path)
     * @param fieldPath path from rootEntity to sorting field of type String
     * @param dir sorting direction parameter of type QuerySort.Direction
     */
    public void addSort(Class<?> rootEntity, String fieldPath, QuerySort.Direction dir)
    {
        sorts.add(new QuerySort(rootEntity, fieldPath, dir));
    }

    /**
     * Adds sorting parameter to PageRequest object
     * @param rootEntity query root entity (base for column path)
     * @param sort sorting parameter including direction &amp; column-name of type String (e.g. "-name")
     */
    public void addSort(Class<?> rootEntity, String sort)
    {
        sort = sort.trim(); //remove leading and trailing spaces ('+' can be serialized as space)

        if(sort.startsWith("-")) this.addSort(rootEntity, sort.substring(1), QuerySort.Direction.DESC);
        else if(sort.startsWith("+")) this.addSort(rootEntity, sort.substring(1), QuerySort.Direction.ASC);
        else this.addSort(rootEntity, sort, QuerySort.Direction.ASC);
    }

    /**
     * Set of sorting parameters of QuerySort type
     * @return sorts set
     */
    public Set<QuerySort> getSorts()
    {
        return sorts;
    }

    /**
     * EntityService query-filters
     * @return queryFilters complex object
     */
    public QueryExpression.CompoundFilter getQueryFilters()
    {
        return queryFilters;
    }

    /**
     * EntityService query-filters
     * @param queryFilters complex object
     */
    public void setQueryFilters(QueryExpression.CompoundFilter queryFilters)
    {
        this.queryFilters = queryFilters;
    }

    /**
     * EntityService distinct-values query request
     * @return distinctColumns set
     */
    public Set<QueryExpression> getDistinctColumns()
    {
        return distinctColumns;
    }

    /**
     * EntityService distinct-values query request
     * @param distinctColumns set
     */
    public void setDistinctColumns(Set<QueryExpression> distinctColumns)
    {
        this.distinctColumns = distinctColumns;
    }

    /**
     * EntityService meta-values query request
     * @return metaColumns set
     */
    public Set<QueryExpression> getMetaColumns()
    {
        return metaColumns;
    }

    /**
     * EntityService meta-values query request
     * @param metaColumns set
     */
    public void setMetaColumns(Set<QueryExpression> metaColumns)
    {
        this.metaColumns = metaColumns;
    }
}
