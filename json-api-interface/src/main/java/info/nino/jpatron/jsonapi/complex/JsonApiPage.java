package info.nino.jpatron.jsonapi.complex;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "error", "data", "type", "meta" })
public class JsonApiPage {
    @JsonProperty("total")
    private Long total;

    @JsonProperty("size")
    private Integer size;

    @JsonProperty("current")
    private Integer current;

    @JsonProperty("pages")
    private Long pages;

    public JsonApiPage() {}
    public JsonApiPage(Map<String, Object> page) {
        if (page.containsKey("total")) {
            this.total = Long.parseLong(page.get("total").toString());
        }

        if (page.containsKey("pages")) {
            this.pages = Long.parseLong(page.get("pages").toString());
        }

        if (page.containsKey("current")) {
            this.current = Integer.parseInt(page.get("current").toString());
        }

        if (page.containsKey("size")) {
            this.size = Integer.parseInt(page.get("size").toString());
        }
    }
    public JsonApiPage(Long total, Integer size, Integer current, Long pages) {
        this.total = total;
        this.size = size;
        this.current = current;
        this.pages = pages;
    }

    public Long getTotal()
    {
        return total;
    }

    public void setTotal(Long total)
    {
        this.total = total;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Long getPages()
    {
        return pages;
    }

    public void setPages(Long pages)
    {
        this.pages = pages;
    }
}
