package info.nino.jpatron.jsonapi.request;

public enum QueryParamType
{
    PAGE("page"),
    PAGE_SIZE("page[size]"),
    PAGE_NUMBER("page[number]"),
    SORT("sort"),
    FILTER("filter"),
    SEARCH("search"),
    DISTINCT("distinct"),
    META("meta"),
    FIELDS("fields"),
    INCLUDE("include");

    private String name;

    QueryParamType(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }
}