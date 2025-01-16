package info.nino.jpatron.efd.request;

import info.nino.jpatron.api.ApiRequestNotation;
import info.nino.jpatron.helpers.ConstantsUtil;
import info.nino.jpatron.request.ApiRequest;
import info.nino.jpatron.request.QueryExpression;

import java.util.List;

public enum EfdApiRequestNotation implements ApiRequestNotation {

    INSTANCE;

    private static final String QUERY_VALUE_SEPARATOR = String.valueOf(ConstantsUtil.COMMA);
    private static final char QUERY_VALUE_LEFT_BRACKET = '(';
    private static final char QUERY_VALUE_RIGHT_BRACKET = ')';
    private static final char QUERY_VALUE_ESCAPE_CHAR = '\\';
    private static final char QUERY_VALUE_QUOTE = '"';
    private static final char FIELD_PATH_CONCATENATOR = '.';
    private static final String SORT_DESC_SIGN = "-";
    private static final String SORT_ASC_SIGN = "+";
    private static final String QUERY_PROPERTY_PAGE_SIZE = "size";
    private static final String QUERY_PROPERTY_PAGE_NUMBER = "number";

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
    public List<? extends ApiRequest.QueryParams.QueryParamType> getQueryParamTypeEnumValues()
    {
        return List.of(QueryParamType.values());
    }

    @Override
    public ApiRequest.QueryParams.QueryParamType getPageEnum()
    {
        return QueryParamType.PAGE;
    }

    @Override
    public ApiRequest.QueryParams.QueryParamType getSortEnum()
    {
        return QueryParamType.SORT;
    }

    @Override
    public ApiRequest.QueryParams.QueryParamType getIncludeEnum()
    {
        return QueryParamType.INCLUDE;
    }

    @Override
    public ApiRequest.QueryParams.QueryParamType getFilterEnum()
    {
        return null;
    }

    @Override
    public ApiRequest.QueryParams.QueryParamType getQueryEnum()
    {
        return QueryParamType.QUERY;
    }

    @Override
    public ApiRequest.QueryParams.QueryParamType getSearchEnum()
    {
        return QueryParamType.SEARCH;
    }

    @Override
    public ApiRequest.QueryParams.QueryParamType getDistinctEnum()
    {
        return null;
    }

    @Override
    public ApiRequest.QueryParams.QueryParamType getMetaEnum()
    {
        return null;
    }

    @Override
    public boolean parseUnknownQueryParamTypesAsSimpleFilters() {
        return true;
    }

    public enum QueryParamType implements ApiRequest.QueryParams.QueryParamType
    {
        PAGE("page"),   //pageSize  //pageNumber
        SORT("sort"),
        QUERY("query"),
        SEARCH("search"),
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
        return List.of(CompoundOperator.values());
    }

    @Override
    public ApiRequest.QueryParams.CompounderEnum getAndCompounderEnum()
    {
        return CompoundOperator.AND;
    }

    @Override
    public ApiRequest.QueryParams.CompounderEnum getOrCompounderEnum()
    {
        return CompoundOperator.OR;
    }

    public enum CompoundOperator implements ApiRequest.QueryParams.CompounderEnum {
        AND(QueryExpression.LogicOperator.AND, "AND"),
        OR(QueryExpression.LogicOperator.OR, "OR");

        private final QueryExpression.LogicOperator logicOperator;
        private final String value;

        CompoundOperator(QueryExpression.LogicOperator logicOperator, String value) {
            this.logicOperator = logicOperator;
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public QueryExpression.LogicOperator getLogicOperator() {
            return logicOperator;
        }
    }

    @Override
    public List<? extends ApiRequest.QueryParams.ComparatorEnum> getComparatorEnumValues() {
        return List.of(Comparator.values());
    }

    @Override
    public List<? extends ApiRequest.QueryParams.ComparatorEnum> getMultiValueComparators() {
        return List.of(Comparator.IN);
    }

    public enum Comparator implements ApiRequest.QueryParams.ComparatorEnum {
        IsNULL(QueryExpression.CompareOperator.IsNULL, ":#"),
        IsNotNULL(QueryExpression.CompareOperator.IsNotNULL, ":!#"),
        EQ(QueryExpression.CompareOperator.EQ, ":"),
        NEQ(QueryExpression.CompareOperator.NEQ, ":!"),
        LIKE(QueryExpression.CompareOperator.LIKE, ":~"),
        GT(QueryExpression.CompareOperator.GT, ":>"),
        LT(QueryExpression.CompareOperator.LT, ":<"),
        GToE(QueryExpression.CompareOperator.GToE, ":>="),
        LToE(QueryExpression.CompareOperator.LToE, ":<="),
        IN(QueryExpression.CompareOperator.IN, ":^");

        private final QueryExpression.CompareOperator compareOperator;
        private final String value;

        Comparator(QueryExpression.CompareOperator compareOperator, String value) {
            this.compareOperator = compareOperator;
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public QueryExpression.CompareOperator getCompareOperator() {
            return compareOperator;
        }
    }
}
