package info.nino.jpatron.efd.request;

public enum QueryParamType {

    PAGE_SIZE("pageSize"),
    PAGE_NUMBER("pageNumber"),
    SORT("sort"),
    QUERY("query"),
    SEARCH("search");

    private String name;

    QueryParamType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}