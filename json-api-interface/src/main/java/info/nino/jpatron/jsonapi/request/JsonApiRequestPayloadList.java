package info.nino.jpatron.jsonapi.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import info.nino.jpatron.jsonapi.complex.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "data"})
public class JsonApiRequestPayloadList<T> implements Serializable {

    @JsonProperty("data")
    protected List<Data<T>> data;

    public JsonApiRequestPayloadList() {}

    public JsonApiRequestPayloadList(T... o) {
        super();
        List<Data<T>> data = new ArrayList<>(o.length);
        for (T t : o) {
            data.add(new Data<T>(t));
        }

        this.data = data;
    }

    public List<Data<T>> getData()
    {
        return data;
    }

    public void setData(List<Data<T>> data)
    {
        this.data = data;
    }
}
