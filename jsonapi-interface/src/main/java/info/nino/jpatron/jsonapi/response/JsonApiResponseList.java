package info.nino.jpatron.jsonapi.response;


import com.fasterxml.jackson.annotation.*;
import info.nino.jpatron.jsonapi.complex.Data;
import info.nino.jpatron.jsonapi.complex.JsonApiPage;
import info.nino.jpatron.response.ApiPageResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON:API response implementation
 * @param <T> type of embedded resource object
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"error", "type", "data", "meta"})
public class JsonApiResponseList<T> extends JsonApiResponse<List<T>> implements JsonResponseInterface
{
    @JsonProperty("data")
    private List<Data<T>> data;

    public JsonApiResponseList(){}

    public JsonApiResponseList(ApiPageResponse<T> page)
    {
        this.setListData(page.getContent());

        JsonApiPage p = new JsonApiPage(page.getTotalItems(), page.getPageSize(), page.getPageNumber(), page.getTotalPages());
        this.setPage(p);

        if(page.getDistinctValues() != null) this.getMeta().put("distinctValues", page.getDistinctValues());
        if(page.getMetaValues() != null) this.getMeta().put("metaValues", page.getMetaValues());
    }

    public JsonApiResponseList(T... o) {
        super();
        List<Data<T>> data = new ArrayList<>(o.length);
        for (T t : o) {
            data.add(new Data<T>(t));
        }

        this.data = data;
    }

    public JsonApiResponseList(List<T> o) {
        super();
        setListData(o);
    }

    @JsonAnyGetter
    public void setList(List<Data<T>> data) {
        this.data = data;
    }

    @JsonIgnore
    public List<Data<T>> getListData()
    {
        return data;
    }

    public void setListData(List<T> list) {
        List<Data<T>> data = new ArrayList<>(list.size());
        for (T t : list) {
            data.add(new Data<>(t));
        }

        this.data = data;
    }
}
