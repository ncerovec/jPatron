package info.nino.jpatron.api.response;


import com.fasterxml.jackson.annotation.*;
import info.nino.jpatron.helpers.ReflectionHelper;
import info.nino.jpatron.response.ApiPageResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JPatron API response implementation
 * @param <T> type of embedded resource object
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "data", "meta"})
public class JPatronApiResponseList<T> implements JPatronResponseInterface
{
    @JsonProperty(value = "type", access = JsonProperty.Access.READ_ONLY)
    private String type;

    @JsonProperty("data")
    private List<T> data;

    @JsonProperty("meta")
    protected JPatronApiMeta meta;

    public JPatronApiResponseList() {

    }

    public JPatronApiResponseList(T... data) {
        List<T> dataList = List.of(data);
        //this.type = ReflectionHelper.findGenericClassParameter(dataList.getClass(), List.class, 0).getName();
        this.data = new ArrayList<>(dataList);
        this.meta = new JPatronApiMeta(null, dataList.size(), null, null);
    }

    public JPatronApiResponseList(List<T> data) {
        //this.type = ReflectionHelper.findGenericClassParameter(data.getClass(), List.class, 0).getName();
        this.data = data;
        this.meta = new JPatronApiMeta(null, data.size(), null, null);
    }

    public JPatronApiResponseList(ApiPageResponse<T> page) {
        //this.type = ReflectionHelper.findGenericClassParameter(page.getClass(), ApiPageResponse.class, 0).getName();
        this.data = page.getContent();
        this.meta = new JPatronApiMeta(page.getPageNumber(),
                page.getPageSize(),
                page.getTotalPages(),
                page.getTotalItems());
    }

    public List<T> getData()
    {
        return data;
    }

    public void setData(List<T> data)
    {
        this.data = data;
    }

    public JPatronApiMeta getMeta()
    {
        return meta;
    }

    public void setMeta(JPatronApiMeta meta)
    {
        this.meta = meta;
    }
}
