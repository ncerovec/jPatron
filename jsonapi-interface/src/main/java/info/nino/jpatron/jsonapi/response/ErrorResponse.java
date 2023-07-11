package info.nino.jpatron.jsonapi.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "errors" })
public class ErrorResponse {

    @JsonProperty("errors")
    private Errors errors;

    public ErrorResponse() {
        this.errors = new Errors();
    }

    class Errors {
        @JsonProperty("id")
        private String id;

        @JsonProperty("code")
        private String code;

        @JsonProperty("title")
        private String title;

        @JsonProperty("detail")
        private String detail;
    }
}
