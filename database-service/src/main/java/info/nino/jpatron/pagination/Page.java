package info.nino.jpatron.pagination;

import info.nino.jpatron.query.PageRequest;
import info.nino.jpatron.response.ApiPageResponse;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Page<T> extends ApiPageResponse<T>
{
    /**
     * Constructor for main result data object returned by Entity Service
     * @param pageRequest initializes Page number &amp; size from request
     * @param totalElements total number of target entity objects in datasource (by filters)
     * @param content result list of the target entity objects (size of page-size)
     */
    public Page(PageRequest<T> pageRequest, long totalElements, List<T> content)
    {
        super(pageRequest.getPageNumber(), pageRequest.getPageSize(), totalElements, content);
    }

    /**
     * Constructor for main result data object returned by Entity Service
     * @param pageNumber number of the page for the data result list
     * @param pageSize size of the page for the returned data result list
     * @param totalItems total number of target entity objects in datasource (by filters)
     * @param content result list of the target entity objects (size of page-size)
     */
    public Page(Integer pageNumber, Integer pageSize, long totalItems, List<T> content)
    {
        super(pageNumber, pageSize, totalItems, content);
    }

    /**
     * Converts Page from one result class to different one using provided mapper function
     * @param mapper function used for mapping between source &amp; target classes
     * @return converted Page with data result list of target class
     * @param <R> target Class of converter function
     */
    public <R> Page<R> convert(Function<? super T, ? extends R> mapper)
    {
        List<R> result = content.stream().map(mapper).collect(Collectors.toList());

        Page<R> p = new Page<>(pageNumber, pageSize, totalItems, result);
        p.setDistinctValues(distinctValues);
        p.setMetaValues(metaValues);

        return p;
    }

    /**
     * Maps Page to different result Object type
     * @param mapper function used for mapping between source &amp; target class
     * @return converted object of type R target class
     * @param <R> target Class of converter function
     */
    public <R> R map(Function<ApiPageResponse<T>, ? extends R> mapper)
    {
        return mapper.apply(this);
    }
}
