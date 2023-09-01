package info.nino.jpatron.pagination;

import info.nino.jpatron.request.ApiRequest;
import info.nino.jpatron.services.entity.EntityService;
import org.apache.commons.collections4.MultiValuedMap;

import java.util.*;

/**
 * Main Entity Service request object
 * Contains parameters used for data/distinct/meta queries
 */
public class PageRequest
{
    private Integer pageSize;
    private Integer pageNumber;

    private boolean distinctDataset = true;
    private boolean readOnlyDataset = false;

    private String[] fetchEntityPaths;      //paths for related entities to fetch
    private String[] entityGraphPaths;      //paths for related entities to load

    private Set<Sort> sorts = new HashSet<>();

    private EntityService.QueryExpression.Conditional queryFilters = new EntityService.QueryExpression.Conditional();

    private Set<EntityService.QueryExpression> distinctColumns = new HashSet<>();

    private Set<EntityService.QueryExpression> metaColumns = new HashSet<>();

    /**
     * Constructor for PageRequest (data-service) with custom properties
     * @param pageSize requested page size
     * @param pageNumber requested page number
     */
    public PageRequest(Integer pageSize, Integer pageNumber)
    {
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
    }

    /**
     * Constructor for PageRequest (data-service) with custom properties
     * @param pageSize requested size of the result page
     * @param pageNumber requested number of the result page
     * @param distinct true/false parameter for distinct-list of the result list
     * @param readOnly true/false parameter for the read-only result list
     */
    public PageRequest(Integer pageSize, Integer pageNumber, boolean distinct, boolean readOnly)
    {
        this(pageSize, pageNumber);
        this.distinctDataset = distinct;
        this.readOnlyDataset = readOnly;
    }

    /**
     * Constructor for PageRequest (data-service) with custom properties
     * @param pageSize requested size of the result page
     * @param pageNumber requested number of the result page
     * @param sort set of sorting parameters of String type (e.g. "-name")
     */
    public PageRequest(Integer pageSize, Integer pageNumber, Set<String> sort)
    {
        this(pageSize, pageNumber);
        if(sort != null) sort.forEach(this::addSort);
    }

    /**
     * Constructor for PageRequest (data-service) with custom properties
     * @param pageSize requested size of the result page
     * @param pageNumber requested number of the result page
     * @param distinct true/false parameter for distinct-list of the result list
     * @param readOnly true/false parameter for the read-only result list
     * @param sorts set of sorting parameters of Sort type
     */
    public PageRequest(Integer pageSize, Integer pageNumber, boolean distinct, boolean readOnly, Set<Sort> sorts)
    {
        this(pageSize, pageNumber);
        this.distinctDataset = distinct;
        this.readOnlyDataset = readOnly;
        this.sorts = sorts;
    }

    /**
     * Constructor for PageRequest (data-service) by generic ApiRequest (common-utils)
     * @param apiRequest generic ApiRequest (common-utils) parameter
     */
    public PageRequest(ApiRequest apiRequest)
    {
        //add page & orders
        this((apiRequest.isPagination()) ? apiRequest.getQueryParams().getPageSize() : null, apiRequest.getQueryParams().getPageNumber());

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
                    Class entity = sf.getValue().getKey();
                    Sort.Direction direction = Sort.Direction.resolveDirectionSign(sf.getValue().getValue());

                    Sort querySort = new Sort(entity, column, direction);
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
                        Class entity = ef.getKey();
                        String column = f.getKey();
                        EntityService.QueryExpression.Filter.Cmp op = EntityService.QueryExpression.Filter.Cmp.valueOf(fo.getKey());
                        String[] values = fo.getValue().toArray(new String[]{}); //Comparable[] values = f.getValue().toArray(new Comparable[]{});

                        EntityService.QueryExpression.Filter queryFilter = new EntityService.QueryExpression.Filter(entity, column, op, values);
                        this.queryFilters.getFilters().add(queryFilter);
                    }
                }
            }
        }

        //add search columns
        if(apiRequest.getQueryParams().getSearches() != null)
        {
            EntityService.QueryExpression.Conditional searchConditional = new EntityService.QueryExpression.Conditional(EntityService.QueryExpression.Conditional.Operator.OR);

            for(Map.Entry<Class<?>, Map<String, MultiValuedMap<String, String>>> es : apiRequest.getQueryParams().getSearches().entrySet())
            {
                for(Map.Entry<String, MultiValuedMap<String, String>> s : es.getValue().entrySet())
                {
                    for(Map.Entry<String, Collection<String>> sm : s.getValue().asMap().entrySet())
                    {
                        Class entity = es.getKey();
                        String column = s.getKey();
                        EntityService.QueryExpression.Filter.Cmp searchOp = EntityService.QueryExpression.Filter.Cmp.LIKE;
                        EntityService.QueryExpression.Filter.Modifier mod = EntityService.QueryExpression.Filter.Modifier.valueOf(sm.getKey());
                        String[] values = sm.getValue().toArray(new String[]{});

                        EntityService.QueryExpression.Filter querySearch = new EntityService.QueryExpression.Filter(entity, column, searchOp, mod, values);
                        searchConditional.getFilters().add(querySearch);
                    }
                }
            }

            this.queryFilters.getConditionals().add(searchConditional);
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
                        Class entity = ed.getKey();
                        String keyColumn = df.getKey();
                        String labelColumn = dl;

                        EntityService.QueryExpression queryDistinct = new EntityService.QueryExpression(entity, keyColumn, labelColumn);
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
                            Class entity = em.getKey();
                            String valueColumn = m.getKey();
                            EntityService.QueryExpression.Func func = EntityService.QueryExpression.Func.valueOf(mf.getKey());
                            String labelColumn = ml;

                            EntityService.QueryExpression queryMeta = new EntityService.QueryExpression(entity, valueColumn, func, labelColumn);
                            this.metaColumns.add(queryMeta);
                        }
                    }
                }
            }
        }
    }

    public Integer getPageNumber()
    {
        return pageNumber;
    }

    public Integer getPageSize()
    {
        return pageSize;
    }

    public boolean isReadOnlyDataset()
    {
        return readOnlyDataset;
    }

    public void setReadOnlyDataset(boolean readOnlyDataset)
    {
        this.readOnlyDataset = readOnlyDataset;
    }

    public boolean isDistinctDataset()
    {
        return distinctDataset;
    }

    public void setDistinctDataset(boolean distinctDataset)
    {
        this.distinctDataset = distinctDataset;
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

    /**
     * Adds sorting parameter to PageRequest object
     * @param name sorting column-name parameter of type String
     * @param dir sorting direction parameter of type Sort.Direction
     */
    public void addSort(String name, Sort.Direction dir)
    {
        sorts.add(new Sort(name, dir));
    }

    /**
     * Adds sorting parameter to PageRequest object
     * @param sort sorting parameter including direction & column-name of type String (e.g. "-name")
     */
    public void addSort(String sort)
    {
        sort = sort.trim(); //remove leading and trailing spaces ('+' can be serialized as space)

        if(sort.startsWith("-")) this.addSort(sort.substring(1), Sort.Direction.DESC);
        else if(sort.startsWith("+")) this.addSort(sort.substring(1), Sort.Direction.ASC);
        else this.addSort(sort, Sort.Direction.ASC);
    }

    public Set<Sort> getSorts()
    {
        return sorts;
    }

    public EntityService.QueryExpression.Conditional getQueryFilters()
    {
        return queryFilters;
    }

    public void setQueryFilters(EntityService.QueryExpression.Conditional queryFilters)
    {
        this.queryFilters = queryFilters;
    }

    public Set<EntityService.QueryExpression> getDistinctColumns()
    {
        return distinctColumns;
    }

    public void setDistinctColumns(Set<EntityService.QueryExpression> distinctColumns)
    {
        this.distinctColumns = distinctColumns;
    }

    public Set<EntityService.QueryExpression> getMetaColumns()
    {
        return metaColumns;
    }

    public void setMetaColumns(Set<EntityService.QueryExpression> metaColumns)
    {
        this.metaColumns = metaColumns;
    }


    /**
     * SubClass for sorting parameters
     */
    public static class Sort
    {
        private Class<?> entity;
        private Class<?> sortType;
        private String columnPath;
        private Direction direction;

        /**
         * Constructor for Sort object
         * @param columnPath sorting column name
         * @param direction sorting direction
         */
        Sort(String columnPath, Direction direction)
        {
            this.columnPath = columnPath;
            this.direction = direction;
        }

        /**
         * Constructor for Sort object
         * @param entity sorting entity class
         * @param columnPath sorting column name
         * @param direction sorting direction
         */
        public Sort(Class<?> entity, String columnPath, Direction direction)
        {
            this.entity = entity;
            this.columnPath = columnPath;
            this.direction = direction;
        }

        /**
         * Constructor for Sort object
         * @param entity sorting entity class
         * @param sortType sorting type class (e.g. sort as integer while column is string)
         * @param columnPath sorting column name
         * @param direction sorting direction
         */
        public Sort(Class<?> entity, Class<?> sortType, String columnPath, Direction direction)
        {
            this.entity = entity;
            this.sortType = sortType;
            this.columnPath = columnPath;
            this.direction = direction;
        }

        public Class<?> getEntity()
        {
            return entity;
        }

        public Class<?> getSortType()
        {
            return sortType;
        }

        public String getColumnPath()
        {
            return columnPath;
        }

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
