package info.nino.jpatron.efd.request;

import info.nino.jpatron.api.ApiRequestDefaults;
import info.nino.jpatron.request.QueryExpression;
import info.nino.jpatron.request.QuerySort;

public enum EfdApiRequestDefaults implements ApiRequestDefaults {

    INSTANCE;

    private static final Integer DEFAULT_PAGE_NUMBER = 1;
    private static final Integer DEFAULT_PAGE_SIZE = 10;
    private static final QuerySort.Direction DEFAULT_SORT_DIRECTION = QuerySort.Direction.ASC;
    private static final QueryExpression.CompareOperator DEFAULT_FILTER_COMPARATOR = QueryExpression.CompareOperator.EQ;
    private static final QueryExpression.ValueModifier DEFAULT_SEARCH_MODIFIER = QueryExpression.ValueModifier.LikeLR;
    private static final QueryExpression.Function DEFAULT_META_FUNCTION = null;

    @Override
    public Integer getDefaultPageNumber()
    {
        return DEFAULT_PAGE_NUMBER;
    }

    @Override
    public Integer getDefaultPageSize()
    {
        return DEFAULT_PAGE_SIZE;
    }

    @Override
    public QuerySort.Direction getDefaultSortDirection()
    {
        return DEFAULT_SORT_DIRECTION;
    }

    @Override
    public QueryExpression.CompareOperator getDefaultFilterComparator()
    {
        return DEFAULT_FILTER_COMPARATOR;
    }

    @Override
    public QueryExpression.ValueModifier getDefaultSearchModifier()
    {
        return DEFAULT_SEARCH_MODIFIER;
    }

    @Override
    public QueryExpression.Function getDefaultMetaFunction()
    {
        return DEFAULT_META_FUNCTION;
    }
}
