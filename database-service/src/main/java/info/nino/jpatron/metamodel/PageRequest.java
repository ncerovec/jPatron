package info.nino.jpatron.metamodel;

import info.nino.jpatron.helpers.ReflectionHelper;
import info.nino.jpatron.metamodel.QueryExpression;
import info.nino.jpatron.request.ApiRequest;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Main Entity Service request object
 * Contains parameters used for data/distinct/meta queries
 */
//TODO fix: QueryExpression suffers from "telescoping constructors problem"
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
     * Set of sorting parameters of Sort type
     */
    private Set<Sort> sorts = new HashSet<>();

    /**
     * EntityService query-filters
     * Filters which should be used to construct SQL 'where' clause
     * Filters are applied to every query in the request (data/distinct/meta)
     */
    private QueryExpression.Conditional queryFilters = new QueryExpression.Conditional();

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
    public PageRequest(Class<T> rootEntity, Integer pageSize, Integer pageNumber, boolean distinct, boolean readOnly, Set<Sort> sorts)
    {
        this(rootEntity, pageSize, pageNumber);
        this.distinctDataset = distinct;
        this.readOnlyDataset = readOnly;
        this.sorts = sorts;
    }

    /**
     * Constructor for PageRequest (data-service) by generic ApiRequest (common-utils)
     * @param apiRequest generic ApiRequest (common-utils) parameter
     */
    public PageRequest(ApiRequest<T> apiRequest)
    {
        //add page & orders
        this(apiRequest.getRootEntity(), (apiRequest.isPagination()) ? apiRequest.getQueryParams().getPageSize() : null, apiRequest.getQueryParams().getPageNumber());

        //set distinct
        this.distinctDataset = apiRequest.isDistinctDataset();

        //set readOnly
        this.readOnlyDataset = apiRequest.isReadOnlyDataset();

        //set fetch entities
        this.fetchEntityPaths = apiRequest.getFetchEntityPaths();

        //set load-graph entities
        this.entityGraphPaths = apiRequest.getEntityGraphPaths();

        //add sort columns
        if(apiRequest.getQueryParams().getSort() != null)
        {
            for(Map.Entry<String, Map.Entry<Class<?>, String>> sf : apiRequest.getQueryParams().getSort().entrySet())
            {
                    String column = sf.getKey();
                    //Class entity = sf.getValue().getKey();
                    Sort.Direction direction = Sort.Direction.resolveDirectionSign(sf.getValue().getValue());

                    Sort querySort = new Sort(apiRequest.getRootEntity(), column, direction);
                    this.sorts.add(querySort);
            }
        }

        //add filter columns
        if(apiRequest.getQueryParams().getFilters() != null)
        {
            for(Map.Entry<Class<?>, Map<String, MultiValuedMap<String, String>>> ef : apiRequest.getQueryParams().getFilters().entrySet())
            {
                for(Map.Entry<String, MultiValuedMap<String, String>> f : ef.getValue().entrySet())
                {
                    for(Map.Entry<String, Collection<String>> fo : f.getValue().asMap().entrySet())
                    {
                        //Class entity = ef.getKey();
                        String column = f.getKey();
                        QueryExpression.Filter.Cmp op = QueryExpression.Filter.Cmp.valueOf(fo.getKey());
                        String[] values = fo.getValue().toArray(new String[]{}); //Comparable[] values = f.getValue().toArray(new Comparable[]{});

                        QueryExpression.Filter queryFilter = new QueryExpression.Filter(apiRequest.getRootEntity(), column, op, values);
                        this.queryFilters.addFilters(queryFilter);
                    }
                }
            }
        }

        //add search columns
        if(apiRequest.getQueryParams().getSearches() != null)
        {
            QueryExpression.Conditional searchConditional = new QueryExpression.Conditional(QueryExpression.Conditional.Operator.OR);

            for(Map.Entry<Class<?>, Map<String, MultiValuedMap<String, String>>> es : apiRequest.getQueryParams().getSearches().entrySet())
            {
                for(Map.Entry<String, MultiValuedMap<String, String>> s : es.getValue().entrySet())
                {
                    for(Map.Entry<String, Collection<String>> sm : s.getValue().asMap().entrySet())
                    {
                        //Class entity = es.getKey();
                        String column = s.getKey();
                        QueryExpression.Filter.Cmp searchOp = QueryExpression.Filter.Cmp.LIKE;
                        QueryExpression.Filter.Modifier mod = QueryExpression.Filter.Modifier.valueOf(sm.getKey());
                        String[] values = sm.getValue().toArray(new String[]{});

                        QueryExpression.Filter querySearch = new QueryExpression.Filter(apiRequest.getRootEntity(), column, searchOp, mod, values);
                        searchConditional.addFilters(querySearch);
                    }
                }
            }

            this.queryFilters.addConditionals(searchConditional);
        }

        //add distinct columns
        if(apiRequest.getQueryParams().getDistinctValues() != null)
        {
            for(Map.Entry<Class<?>, MultiValuedMap<String, String>> ed : apiRequest.getQueryParams().getDistinctValues().entrySet())
            {
                for(Map.Entry<String, Collection<String>> df : ed.getValue().asMap().entrySet())
                {
                    if(df.getValue().isEmpty()) df.getValue().add(null);

                    for(String dl : df.getValue())
                    {
                        //Class entity = ed.getKey();
                        String keyColumn = df.getKey();
                        String labelColumn = dl;

                        QueryExpression queryDistinct = new QueryExpression(apiRequest.getRootEntity(), keyColumn, labelColumn);
                        this.distinctColumns.add(queryDistinct);
                    }
                }
            }
        }

        //add meta columns
        if(apiRequest.getQueryParams().getMetaValues() != null)
        {
            for(Map.Entry<Class<?>, Map<String, MultiValuedMap<String, String>>> em : apiRequest.getQueryParams().getMetaValues().entrySet())
            {
                for(Map.Entry<String, MultiValuedMap<String, String>> m : em.getValue().entrySet())
                {
                    for(Map.Entry<String, Collection<String>> mf : m.getValue().asMap().entrySet())
                    {
                        if(mf.getValue().isEmpty()) mf.getValue().add(null);

                        for(String ml : mf.getValue())
                        {
                            //Class entity = em.getKey();
                            String valueColumn = m.getKey();
                            QueryExpression.Func func = QueryExpression.Func.valueOf(mf.getKey());
                            String labelColumn = ml;

                            QueryExpression queryMeta = new QueryExpression(apiRequest.getRootEntity(), valueColumn, func, labelColumn);
                            this.metaColumns.add(queryMeta);
                        }
                    }
                }
            }
        }
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
     * @param columnPath sorting column-name parameter of type String
     * @param dir sorting direction parameter of type Sort.Direction
     */
    public void addSort(Class<?> rootEntity, String columnPath, Sort.Direction dir)
    {
        sorts.add(new Sort(rootEntity, columnPath, dir));
    }

    /**
     * Adds sorting parameter to PageRequest object
     * @param rootEntity query root entity (base for column path)
     * @param sort sorting parameter including direction &amp; column-name of type String (e.g. "-name")
     */
    public void addSort(Class<?> rootEntity, String sort)
    {
        sort = sort.trim(); //remove leading and trailing spaces ('+' can be serialized as space)

        if(sort.startsWith("-")) this.addSort(rootEntity, sort.substring(1), Sort.Direction.DESC);
        else if(sort.startsWith("+")) this.addSort(rootEntity, sort.substring(1), Sort.Direction.ASC);
        else this.addSort(rootEntity, sort, Sort.Direction.ASC);
    }

    /**
     * Set of sorting parameters of Sort type
     * @return sorts set
     */
    public Set<Sort> getSorts()
    {
        return sorts;
    }

    /**
     * EntityService query-filters
     * @return queryFilters complex object
     */
    public QueryExpression.Conditional getQueryFilters()
    {
        return queryFilters;
    }

    /**
     * EntityService query-filters
     * @param queryFilters complex object
     */
    public void setQueryFilters(QueryExpression.Conditional queryFilters)
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

    /**
     * SubClass for sorting parameters
     */
    public static class Sort
    {
        //private Class<?> rootEntity;
        private Class<?> sortType;
        private Pair<Class<?>, String> columnEntityPath;
        private Direction direction;

        /**
         * Constructor for Sort object
         * @param rootEntity query root entity (base for column path)
         * @param columnPath sorting column name or path
         * @param direction sorting direction
         */
        Sort(Class<?> rootEntity, String columnPath, Direction direction)
        {
            //this.rootEntity = rootEntity;
            this.columnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, columnPath, true);
            this.direction = direction;
        }

        /**
         * Constructor for Sort object
         * @param rootEntity query root entity (base for column path)
         * @param sortType sorting type class (e.g. sort as integer while column is string)
         * @param columnPath sorting column name or path
         * @param direction sorting direction
         */
        public Sort(Class<?> rootEntity, String columnPath, Direction direction, Class<?> sortType)
        {
            //this.rootEntity = rootEntity;
            this.columnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, columnPath, true);
            this.direction = direction;
            this.sortType = sortType;
        }

        /**
         * @return pair of sort entity & column name OR path (from root entity)
         */
        public Pair<Class<?>, String> getColumnEntityPath()
        {
            return columnEntityPath;
        }

        /**
         * @return sorting type class
         */
        public Class<?> getSortType()
        {
            return sortType;
        }

        /**
         * @return sorting direction
         */
        public Direction getDirection()
        {
            return direction;
        }


        /**
         * Enum used as direction for Sort parameters
         */
        public enum Direction
        {
            ASC, DESC;

            /**
             * Resolves String ("+"/"-") to Direction enum (ASC/DESC)
             * @param directionSign
             * @return
             */
            public static Direction resolveDirectionSign(String directionSign)
            {
                if(("-").equals(directionSign)) return Direction.DESC;
                else return Direction.ASC;
            }
        }
    }
}
