package info.nino.jpatron.api.request;

import java.util.Arrays;

public enum QueryParamType
{
    PAGE("page"),
    SORT("sort"),
    FILTER("filter"),
    QUERY("query"),
    SEARCH("search"),
    DISTINCT("distinct"),
    META("meta"),
    FIELDS("fields"),
    INCLUDE("include");

    private String queryParam;

    QueryParamType(String queryParam) {
        this.queryParam = queryParam;
    }

    public String getQueryParam() {
        return queryParam;
    }

    public static QueryParamType findByQueryParam(String queryParam) {
        return Arrays.stream(QueryParamType.values())
                .filter(e -> e.getQueryParam().equals(queryParam))
                .findAny()
                .orElseThrow(() -> {
                    return new RuntimeException(String.format("QueryParamType value '%s' not found!", queryParam));
                });
    }
}