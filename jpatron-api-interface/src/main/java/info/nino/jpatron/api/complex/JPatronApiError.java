package info.nino.jpatron.api.complex;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "id", "code", "title", "detail" })
public
class JPatronApiError
{
    @JsonProperty("id")
    private String id;

    @JsonProperty("code")
    private String code;

    @JsonProperty("title")
    private String title;

    @JsonProperty("detail")
    private String detail;

    public JPatronApiError() {}
    public JPatronApiError(String title) {
        this.title = title;
    }
    public JPatronApiError(String id, String code, String title) {
        this.id = id;
        this.code = code;
        this.title = title;
    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
