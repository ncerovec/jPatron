package info.nino.jpatron.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "pageSize", "pageNumber", "totalPages", "totalItems" })
public class JPatronApiPage implements Serializable {

    @JsonProperty("pageSize")
    private Integer pageSize;

    @JsonProperty("pageNumber")
    private Integer pageNumber;

    @JsonProperty("totalPages")
    private Long totalPages;

    @JsonProperty("totalItems")
    private Long totalItems;

    public JPatronApiPage() {}

    public JPatronApiPage(Integer pageNumber, Integer pageSize, Long totalPages, Long totalItems) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
        this.totalItems = totalItems;
    }

    public JPatronApiPage(Map<String, Object> page) {
        if (page.containsKey("pageSize")) {
            this.pageSize = Integer.parseInt(page.get("pageSize").toString());
        }

        if (page.containsKey("pageNumber")) {
            this.pageNumber = Integer.parseInt(page.get("pageNumber").toString());
        }

        if (page.containsKey("totalPages")) {
            this.totalPages = Long.parseLong(page.get("totalPages").toString());
        }

        if (page.containsKey("totalItems")) {
            this.totalItems = Long.parseLong(page.get("totalItems").toString());
        }
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Long getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Long totalPages) {
        this.totalPages = totalPages;
    }

    public Long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Long totalItems) {
        this.totalItems = totalItems;
    }
}
