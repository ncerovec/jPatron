package info.nino.jpatron.efd.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import info.nino.jpatron.response.ApiPageResponse;

import java.util.ArrayList;
import java.util.List;

@JsonPropertyOrder({"data", "metadata"})
public class EfdApiListResponse<T> implements EfdResponseInterface {

    @JsonProperty("data")
    private List<T> data;

    @JsonProperty("metadata")
    private EfdApiMetadata metadata;

    public EfdApiListResponse() {

    }

    public EfdApiListResponse(T... data) {
        List<T> dataList = List.of(data);
        this.data = new ArrayList<>(dataList);
        this.metadata = new EfdApiMetadata(null, dataList.size(), null, null);
    }

    public EfdApiListResponse(List<T> data) {
        this.data = data;
        this.metadata = new EfdApiMetadata(null, data.size(), null, null);
    }

    public EfdApiListResponse(ApiPageResponse<T> page) {
        this.data = page.getContent();
        this.metadata = new EfdApiMetadata(page.getPageNumber(),
                                            page.getPageSize(),
                                            page.getTotalPages(),
                                            page.getTotalItems());
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
