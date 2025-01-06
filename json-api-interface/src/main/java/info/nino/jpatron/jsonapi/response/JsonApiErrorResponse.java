package info.nino.jpatron.jsonapi.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import info.nino.jpatron.jsonapi.complex.JsonApiError;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "errors" })
public class JsonApiErrorResponse implements JsonResponseInterface {

    @JsonProperty("errors")
    private JsonApiError errors;

    public JsonApiErrorResponse() {
        this.errors = new JsonApiError();
    }

    public JsonApiError getErrors() {
        return errors;
    }

    public void setErrors(JsonApiError errors) {
        this.errors = errors;
    }
}
