package info.nino.jpatron.efd.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import info.nino.jpatron.response.ApiPageResponse;

import java.util.ArrayList;
import java.util.List;

@JsonPropertyOrder({"data", "metadata"})
public class EfdApiResponseList<T> implements EfdResponseInterface
{
    @JsonProperty("data")
    private List<T> data;

    @JsonProperty("metadata")
    private EfdApiMetadata metadata;

    public EfdApiResponseList() {
    }

    public EfdApiResponseList(T... o) {
        this.data = new ArrayList<>(List.of(o));
    }

    public EfdApiResponseList(List<T> o) {
        this.data = o;
    }

    public EfdApiResponseList(ApiPageResponse<T> page)
    {
        this.setData(page.getContent());
        this.setMetadata(new EfdApiMetadata(page.getPageNumber(),
                                            page.getPageSize(),
                                            page.getTotalPages(),
                                            page.getTotalItems()));
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public EfdApiMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(EfdApiMetadata metadata) {
        this.metadata = metadata;
    }
}
