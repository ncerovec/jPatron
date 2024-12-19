package info.nino.jpatron.efd.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Date;
import java.util.List;
import java.util.Map;

@JsonPropertyOrder({"source", "code", "message", "requestId", "timestamp", "errorParameters", "errors"})
public class EfdApiErrorResponse {

    @JsonProperty("source")
    private String source;

    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("timestamp")
    private Date timestamp;

    @JsonProperty("errorParameters")
    private Map<String, String> errorParameters;

    @JsonProperty("errors")
    private List<Error> errors;

    public EfdApiErrorResponse(String source, String code) {
        this.source = source;
        this.code = code;
        this.timestamp = new Date();
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getErrorParameters() {
        return errorParameters;
    }

    public void setErrorParameters(Map<String, String> errorParameters) {
        this.errorParameters = errorParameters;
    }

    public List<Error> getErrors() {
        return errors;
    }

    public void setErrors(List<Error> errors) {
        this.errors = errors;
    }

    @JsonPropertyOrder({"name", "code", "message", "value"})
    public static class Error {
        @JsonProperty("name")
        private String name;

        @JsonProperty("code")
        private String code;

        @JsonProperty("message")
        private String message;

        @JsonProperty("value")
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
