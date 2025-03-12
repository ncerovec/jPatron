package info.nino.jpatron.jsonapi.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import info.nino.jpatron.jsonapi.complex.JsonApiError;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "errors" })
public class JsonApiErrorResponse implements JsonResponseInterface {

    @JsonProperty("errors")
    private List<JsonApiError> errors = new ArrayList<>();

    public JsonApiErrorResponse() {

    }

    public List<JsonApiError> getErrors() {
        return errors;
    }

    public void setErrors(List<JsonApiError> errors) {
        this.errors = errors;
    }
}
