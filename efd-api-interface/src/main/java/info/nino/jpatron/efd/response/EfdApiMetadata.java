package info.nino.jpatron.efd.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "pageSize", "pageNumber", "totalPages", "totalItems" })
public class EfdApiMetadata {

    @JsonProperty("pageSize")
    private Integer pageSize;

    @JsonProperty("pageNumber")
    private Integer pageNumber;

    @JsonProperty("totalPages")
    private Long totalPages;

    @JsonProperty("totalItems")
    private Long totalItems;

    public EfdApiMetadata(Integer pageNumber, Integer pageSize, Long totalPages, Long totalItems) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
        this.totalItems = totalItems;
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
