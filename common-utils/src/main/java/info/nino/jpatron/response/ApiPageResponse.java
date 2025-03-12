package info.nino.jpatron.response;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Generic abstract class for page response implementations
 * It serves as abstract type which can be used across jPatron library artefacts with the concrete implementation in final interface artefact
 * @param <T> type of embedded resource object
 */
public abstract class ApiPageResponse<T> implements Serializable
{
    /**
     * Ordinal number of current page (index + 1)
     */
    protected Integer pageNumber;

    /**
     * Size of current page (number of result items)
     */
    protected Integer pageSize;

    /**
     * Total number of available pages (for current page size)
     */
    protected long totalPages;

    /**
     * Total number of available resource items (&lt;= totalPages * pageSize)
     */
    protected long totalItems;

    /**
     * Current page result-list of the resource objects
     */
    protected List<T> content;

    /**
     * Key-Value pairs of distinct values for the selected properties of resource type
     */
    protected Map<String, Map<Object, Object>> distinctValues;

    /**
     * Key-Value pairs of aggregate values for the selected properties of resource type
     */
    protected Map<String, Map<Object, Object>> metaValues;

    /**
     * Constructor with mandatory object properties
     * @param pageNumber current page number
     * @param pageSize current page size
     * @param totalItems total resource items value
     * @param content current page list of resource objects
     */
    public ApiPageResponse(Integer pageNumber, Integer pageSize, long totalItems, List<T> content)
    {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        this.content = content;

        if(pageSize != null)
        {
            this.totalPages = totalItems / pageSize;
            if(totalItems % pageSize != 0) this.totalPages++;
        }
    }

    /**
     * {@link ApiPageResponse#pageNumber}
     * @return pageNumber
     */
    public Integer getPageNumber()
    {
        return pageNumber;
    }

    /**
     * {@link ApiPageResponse#pageSize}
     * @return pageSize
     */
    public Integer getPageSize()
    {
        return pageSize;
    }

    /**
     * {@link ApiPageResponse#totalPages}
     * @return totalPages
     */
    public long getTotalPages()
    {
        return totalPages;
    }

    /**
     * {@link ApiPageResponse#totalItems}
     * @return totalItems
     */
    public long getTotalItems()
    {
        return totalItems;
    }

    /**
     * {@link ApiPageResponse#content}
     * @return content
     */
    public List<T> getContent()
    {
        return content;
    }

    /**
     * {@link ApiPageResponse#distinctValues}
     * @return distinctValues
     */
    public Map<String, Map<Object, Object>> getDistinctValues()
    {
        return distinctValues;
    }

    /**
     * {@link ApiPageResponse#distinctValues}
     * @param distinctValues map
     */
    public void setDistinctValues(Map<String, Map<Object, Object>> distinctValues)
    {
        this.distinctValues = distinctValues;
    }

    /**
     * {@link ApiPageResponse#metaValues}
     * @return metaValues
     */
    public Map<String, Map<Object, Object>> getMetaValues()
    {
        return metaValues;
    }

    /**
     * {@link ApiPageResponse#metaValues}
     * @param metaValues map
     */
    public void setMetaValues(Map<String, Map<Object, Object>> metaValues)
    {
        this.metaValues = metaValues;
    }
}
