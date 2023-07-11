package info.nino.jpatron.response;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public abstract class ApiPageResponse<T> implements Serializable
{
    protected Integer pageNumber;
    protected Integer pageSize;
    protected long totalPages;
    protected long totalItems;

    protected List<T> content;

    protected Map<String, Map<Object, Object>> distinctValues;

    protected Map<String, Map<Object, Object>> metaValues;

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

    public Integer getPageNumber()
    {
        return pageNumber;
    }

    public Integer getPageSize()
    {
        return pageSize;
    }

    public long getTotalPages()
    {
        return totalPages;
    }

    public long getTotalItems()
    {
        return totalItems;
    }

    public List<T> getContent()
    {
        return content;
    }

    public Map<String, Map<Object, Object>> getDistinctValues()
    {
        return distinctValues;
    }

    public void setDistinctValues(Map<String, Map<Object, Object>> distinctValues)
    {
        this.distinctValues = distinctValues;
    }

    public Map<String, Map<Object, Object>> getMetaValues()
    {
        return metaValues;
    }

    public void setMetaValues(Map<String, Map<Object, Object>> metaValues)
    {
        this.metaValues = metaValues;
    }

}
