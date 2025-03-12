package info.nino.jpatron.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import info.nino.jpatron.response.ApiPageResponse;

import java.io.Serializable;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"page", "distinctValues", "metaValues"})
public class JPatronApiMeta implements Serializable {

    @JsonProperty("page")
    private JPatronApiPage page;

    @JsonProperty("distinctValues")
    private Map<String, Map<Object, Object>> distinctValues;

    @JsonProperty("metaValues")
    protected Map<String, Map<Object, Object>> metaValues;

    public JPatronApiMeta(Integer pageNumber, Integer pageSize, Long totalPages, Long totalItems) {
        this.page = new JPatronApiPage(pageNumber, pageSize, totalPages, totalItems);
    }

    public JPatronApiMeta(ApiPageResponse<?> page) {
        this.page = new JPatronApiPage(page.getPageNumber(), page.getPageSize(), page.getTotalPages(), page.getTotalItems());
        this.distinctValues = page.getDistinctValues();
        this.metaValues = page.getMetaValues();
    }

    public JPatronApiPage getPage() {
        return page;
    }

    public void setPage(JPatronApiPage page) {
        this.page = page;
    }

    public Map<String, Map<Object, Object>> getDistinctValues() {
        return distinctValues;
    }

    public void setDistinctValues(Map<String, Map<Object, Object>> distinctValues) {
        this.distinctValues = distinctValues;
    }

    public Map<String, Map<Object, Object>> getMetaValues() {
        return metaValues;
    }

    public void setMetaValues(Map<String, Map<Object, Object>> metaValues) {
        this.metaValues = metaValues;
    }
}
