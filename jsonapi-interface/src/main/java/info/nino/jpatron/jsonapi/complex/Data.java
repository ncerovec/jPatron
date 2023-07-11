package info.nino.jpatron.jsonapi.complex;

import com.fasterxml.jackson.annotation.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "id", "type", "attributes", "relationships", "meta" })
public class Data<T> implements java.io.Serializable {

    @JsonProperty("id")
    private Number id;

    //@ApiModelProperty(readOnly = true)
    @JsonProperty(value = "type", access = JsonProperty.Access.READ_ONLY)
    private String type;

    @JsonProperty("attributes")
    private T attributes;

    @JsonProperty("relationships")
    protected Map<String, Object> relationships;

    //@ApiModelProperty(readOnly = true)
    @JsonProperty(value = "meta", access = JsonProperty.Access.READ_ONLY)
    private Map<String, Object> meta = new HashMap<>();

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Data() {
        super();
    }

    public Data(T o) {
        try {
            Method idm = o.getClass().getMethod("getId");
            id = (Number)idm.invoke(o);

        } catch(Throwable e) {
            throw new RuntimeException("JsonApi internal development error, could not find ID of object: " + o.getClass(), e);
        }
        type = o.getClass().getTypeName();
        this.attributes = o;
    }

    public Number getId() {
        return id;
    }

    public void setId(Number id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public T getAttributes() {
        return attributes;
    }

    public void setAttributes(T attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }

    public void addMeta(String key, Object value) {
        this.meta.put(key, value);
    }

    public Map<String, Object> getRelationships() {
        return relationships;
    }

    public void setRelationships(Map<String, Object> relationships) {
        this.relationships = relationships;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
