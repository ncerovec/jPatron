package info.nino.jpatron.jsonapi.request;

import info.nino.jpatron.api.ApiRequestNotation;
import info.nino.jpatron.helpers.ConstantsUtil;
import info.nino.jpatron.request.ApiRequest;

import java.util.List;

public enum JsonApiRequestNotation implements ApiRequestNotation {

    INSTANCE;

    private static final String QUERY_VALUE_SEPARATOR = String.valueOf(ConstantsUtil.COMMA);
    private static final char FIELD_PATH_CONCATENATOR = '.';
    private static final String SORT_DESC_SIGN = "-";
    private static final String SORT_ASC_SIGN = "+";
    private static final String QUERY_PROPERTY_PAGE_SIZE = "size";
    private static final String QUERY_PROPERTY_PAGE_NUMBER = "number";

    private static final char QUERY_VALUE_LEFT_BRACKET = 0;
    private static final char QUERY_VALUE_RIGHT_BRACKET = 0;
    private static final char QUERY_VALUE_ESCAPE_CHAR = 0;
    private static final char QUERY_VALUE_QUOTE = 0;

    @Override
    public String getQueryValueSeparator() {
        return QUERY_VALUE_SEPARATOR;
    }

    @Override
    public char getQueryValueLeftBracket() {
        return QUERY_VALUE_LEFT_BRACKET;
    }

    @Override
    public char getQueryValueRightBracket() {
        return QUERY_VALUE_RIGHT_BRACKET;
    }

    @Override
    public char getQueryValueEscapeChar() {
        return QUERY_VALUE_ESCAPE_CHAR;
    }

    @Override
    public char getQueryValueQuote() {
        return QUERY_VALUE_QUOTE;
    }

    @Override
    public char getFieldPathConcatenator() {
        return FIELD_PATH_CONCATENATOR;
    }

    @Override
    public String getSortDescSign() {
        return SORT_DESC_SIGN;
    }

    @Override
    public String getSortAscSign() {
        return SORT_ASC_SIGN;
    }

    @Override
    public String getQueryPropertyPageSize() {
        return QUERY_PROPERTY_PAGE_SIZE;
    }

    @Override
    public String getQueryPropertyPageNumber() {
        return QUERY_PROPERTY_PAGE_NUMBER;
    }

    @Override
    public List<? extends ApiRequest.QueryParams.QueryParamType> getQueryParamTypeEnumValues() {
        return List.of(QueryParamType.values());
    }

    @Override
    public ApiRequest.QueryParams.QueryParamType getPageEnum() {
        return QueryParamType.PAGE;
    }

    @Override
    public ApiRequest.QueryParams.QueryParamType getSortEnum() {
        return QueryParamType.SORT;
    }

    @Override
    public ApiRequest.QueryParams.QueryParamType getIncludeEnum() {
        return QueryParamType.INCLUDE;
    }

    @Override
    public ApiRequest.QueryParams.QueryParamType getFilterEnum() {
        return QueryParamType.FILTER;
    }

    @Override
    public ApiRequest.QueryParams.QueryParamType getQueryEnum() {
        return null;
    }

    @Override
    public ApiRequest.QueryParams.QueryParamType getSearchEnum() {
        return QueryParamType.SEARCH;
    }

    @Override
    public ApiRequest.QueryParams.QueryParamType getDistinctEnum() {
        return QueryParamType.DISTINCT;
    }

    @Override
    public ApiRequest.QueryParams.QueryParamType getMetaEnum() {
        return QueryParamType.META;
    }

    @Override
    public boolean parseUnknownQueryParamTypesAsSimpleFilters() {
        return false;
    }

    public enum QueryParamType implements ApiRequest.QueryParams.QueryParamType {

        PAGE("page"),
        //PAGE_SIZE("page[size]"),
        //PAGE_NUMBER("page[number]"),
        SORT("sort"),
        FILTER("filter"),
        SEARCH("search"),
        DISTINCT("distinct"),
        META("meta"),
        FIELDS("fields"),
        INCLUDE("include");

        private final String queryParam;

        QueryParamType(String queryParam) {
            this.queryParam = queryParam;
        }

        @Override
        public String getQueryParam() {
            return queryParam;
        }
    }

    @Override
    public List<? extends ApiRequest.QueryParams.CompounderEnum> getCompounderEnumValues() {
        return null;
    }

    @Override
    public ApiRequest.QueryParams.CompounderEnum getAndCompounderEnum() {
        return null;
    }

    @Override
    public ApiRequest.QueryParams.CompounderEnum getOrCompounderEnum() {
        return null;
    }

    @Override
    public List<? extends ApiRequest.QueryParams.ComparatorEnum> getComparatorEnumValues() {
        return null;
    }

    @Override
    public List<? extends ApiRequest.QueryParams.ComparatorEnum> getMultiValueComparators() {
        return null;
    }
}
