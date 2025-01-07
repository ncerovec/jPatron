package info.nino.jpatron.jsonapi.response;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import info.nino.jpatron.jsonapi.complex.Data;
import info.nino.jpatron.jsonapi.complex.JsonApiError;
import info.nino.jpatron.jsonapi.complex.JsonApiPage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"error", "data", "type", "meta"})
public class JsonApiResponse<T> implements JsonResponseInterface
{
    @JsonProperty("errors")
    protected List<JsonApiError> errors;

    @JsonProperty("data")
    protected Data<T> data;

    @JsonProperty("meta")
    protected Map<String, Object> meta;

    /**
     * Use "included" instead as relationships
     * should be only in resource object
     * It should be dropped in future
     */
    @Deprecated
    @JsonProperty("relationships")
    protected Map<String, JsonResponseInterface> relationships;

    @JsonProperty("included")
    protected Map<String, JsonResponseInterface> included;

    public JsonApiResponse() {
    }

    public JsonApiResponse(T o) {
        if (o != null) {
            this.data = new Data<>(o);
        }
    }

    public JsonApiResponse(JsonApiError error) {
        this.errors = new ArrayList<>();
        this.errors.add(error);
    }

    public JsonApiResponse(ArrayList<JsonApiError> errors) {
        this.errors = errors;
    }

    @JsonIgnore
    public Data<T> getDataItem() {
        return data;
    }

    public void setDataItem(T item) {
        this.data = new Data<>(item);
    }

    public List<JsonApiError> getErrors() {
        return errors;
    }

    public void setErrors(List<JsonApiError> errors) {
        this.errors = errors;
    }

    public void setError(JsonApiError error) {
        this.errors = new ArrayList<>();
        this.errors.add(error);
    }

    public void addError(JsonApiError error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }

        this.errors.add(error);
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }

    public Map<String, JsonResponseInterface> getRelationships() {
        return relationships;
    }

    public void setRelationships(Map<String, JsonResponseInterface> relationships) {
        this.relationships = relationships;
    }

    public Map<String, JsonResponseInterface> getIncluded() {
        return included;
    }

    public void setIncluded(Map<String, JsonResponseInterface> included) {
        this.included = included;
    }

    public void setPage(JsonApiPage page) {
        if (this.meta == null) {
            this.meta = new HashMap<>();
        }

        this.meta.put("page", page);
    }
}
