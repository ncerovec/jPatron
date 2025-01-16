package info.nino.jpatron.api;

import info.nino.jpatron.request.ApiRequest;
import info.nino.jpatron.request.QuerySort;

import java.util.List;

public interface ApiRequestNotation {

    String getQueryValueSeparator();
    char getQueryValueLeftBracket();
    char getQueryValueRightBracket();
    char getQueryValueEscapeChar();
    char getQueryValueQuote();
    char getFieldPathConcatenator();
    String getSortDescSign();
    String getSortAscSign();
    String getQueryPropertyPageSize();
    String getQueryPropertyPageNumber();

    List<? extends ApiRequest.QueryParams.QueryParamType> getQueryParamTypeEnumValues();
    ApiRequest.QueryParams.QueryParamType getPageEnum();
    ApiRequest.QueryParams.QueryParamType getSortEnum();
    ApiRequest.QueryParams.QueryParamType getIncludeEnum();
    ApiRequest.QueryParams.QueryParamType getFilterEnum();
    ApiRequest.QueryParams.QueryParamType getQueryEnum();
    ApiRequest.QueryParams.QueryParamType getSearchEnum();
    ApiRequest.QueryParams.QueryParamType getDistinctEnum();
    ApiRequest.QueryParams.QueryParamType getMetaEnum();
    boolean parseUnknownQueryParamTypesAsSimpleFilters();

    List<? extends ApiRequest.QueryParams.CompounderEnum> getCompounderEnumValues();
    ApiRequest.QueryParams.CompounderEnum getAndCompounderEnum();
    ApiRequest.QueryParams.CompounderEnum getOrCompounderEnum();

    List<? extends ApiRequest.QueryParams.ComparatorEnum> getComparatorEnumValues();
    List<? extends ApiRequest.QueryParams.ComparatorEnum> getMultiValueComparators();

    /**
     * Resolves String ("+"/"-") to Direction enum (ASC/DESC)
     * @param directionSign
     * @return
     */
    default QuerySort.Direction resolveDirectionSign(String directionSign) {
        if (this.getSortDescSign().equals(directionSign)) {
            return QuerySort.Direction.DESC;
        } else if (this.getSortDescSign().equals(directionSign)) {
            return QuerySort.Direction.ASC;
        } else {
            throw new IllegalArgumentException(String.format("Unresolvable QuerySort.Direction '%s' sign!", directionSign));
        }
    }

    default ApiRequest.QueryParams.QueryParamType findByQueryParam(String queryParam) {
        return this.getQueryParamTypeEnumValues().stream()
                .filter(e -> e.getQueryParam().equals(queryParam))
                .findAny()
                .orElseThrow(() -> new RuntimeException(String.format("QueryParamType value '%s' not found!", queryParam)));
    }
}
