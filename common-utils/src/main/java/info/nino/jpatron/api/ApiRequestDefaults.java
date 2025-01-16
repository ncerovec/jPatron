package info.nino.jpatron.api;

import info.nino.jpatron.request.QueryExpression;
import info.nino.jpatron.request.QuerySort;

public interface ApiRequestDefaults {

    Integer getDefaultPageNumber();
    Integer getDefaultPageSize();
    QuerySort.Direction getDefaultSortDirection();
    QueryExpression.CompareOperator getDefaultFilterComparator();
    QueryExpression.ValueModifier getDefaultSearchModifier();
    QueryExpression.Function getDefaultMetaFunction();
}
