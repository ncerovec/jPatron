package info.nino.jpatron.pagination;

import info.nino.jpatron.response.ApiPageResponse;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Page<T> extends ApiPageResponse<T>
{

    public Page(PageRequest pageRequest, long totalElements, List<T> content)
    {
        super(pageRequest.getPageNumber(), pageRequest.getPageSize(), totalElements, content);
    }

    public Page(Integer pageNumber, Integer pageSize, long totalItems, List<T> content)
    {
        super(pageNumber, pageSize, totalItems, content);
    }

    public <R> Page<R> convert(Function<? super T, ? extends R> mapper)
    {
        List<R> result = content.stream().map(mapper).collect(Collectors.toList());

        Page<R> p = new Page<>(pageNumber, pageSize, totalItems, result);
        p.setDistinctValues(distinctValues);
        p.setMetaValues(metaValues);

        return p;
    }
}
