package info.nino.jpatron.jsonapi.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import info.nino.jpatron.jsonapi.complex.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "data"})
public class JsonApiRequestPayload<T> implements java.io.Serializable {

    @JsonProperty("data")
    protected Data<T> data;

    public JsonApiRequestPayload() {}

    public JsonApiRequestPayload(T o) {
        this.data = new Data<>(o);
    }

    public Data<T> getData()
    {
        return data;
    }

    public void setData(Data<T> data) {
        this.data = data;
    }
}
