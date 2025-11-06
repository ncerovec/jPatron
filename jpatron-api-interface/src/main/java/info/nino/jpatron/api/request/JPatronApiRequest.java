package info.nino.jpatron.api.request;

import info.nino.jpatron.request.ApiRequest;
import info.nino.jpatron.request.QueryExpression;
import info.nino.jpatron.request.QuerySort;

/**
 * jPatron API request implementation
 */
public class JPatronApiRequest<T> extends ApiRequest<T> {

    public JPatronApiRequest(Class<T> rootEntity,
                         ApiRequest.QueryParams queryParams,
                         boolean distinct,
                         boolean readOnly,
                         String[] entityGraphPaths) {
        super(rootEntity, queryParams, distinct, readOnly, null, entityGraphPaths);
    }

    @Override
    public Class<? extends SortDirectionEnum> getSortDirectionEnum()
    {
        return SortDirection.class;
    }

    @Override
    public Class<? extends CompounderEnum> getCompounderEnum()
    {
        return Compounder.class;
    }

    @Override
    public Class<? extends ComparatorEnum> getComparatorEnum()
    {
        return Comparator.class;
    }

    @Override
    public Class<? extends FunctionEnum> getFunctionEnum()
    {
        return Function.class;
    }

    public enum SortDirection implements ApiRequest.SortDirectionEnum {
        ASC(QuerySort.Direction.ASC, "+"),
        DESC(QuerySort.Direction.DESC, "-");

        private final QuerySort.Direction sortDirection;
        private final String value;

        SortDirection(QuerySort.Direction sortDirection, String value) {
            this.sortDirection = sortDirection;
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public QuerySort.Direction toQuerySortDirection()
        {
            return sortDirection;
        }
    }

    public enum Compounder implements ApiRequest.CompounderEnum {
        AND(QueryExpression.LogicOperator.AND, "AND"),
        OR(QueryExpression.LogicOperator.OR, "OR");

        private final QueryExpression.LogicOperator logicOperator;
        private final String value;

        Compounder(QueryExpression.LogicOperator logicOperator, String value) {
            this.logicOperator = logicOperator;
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public QueryExpression.LogicOperator toQueryLogicOperator() {
            return logicOperator;
        }
    }

    public enum Comparator implements ApiRequest.ComparatorEnum {
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
        public QueryExpression.CompareOperator toQueryComparator() {
            return compareOperator;
        }
    }

    public enum Function implements ApiRequest.FunctionEnum {
        COUNT(QueryExpression.Function.COUNT, "COUNT"),
        COUNT_DISTINCT(QueryExpression.Function.COUNT_DISTINCT, "COUNT_DISTINCT"),
        SUM(QueryExpression.Function.SUM, "SUM"),
        AVG(QueryExpression.Function.AVG, "AVG"),
        MIN(QueryExpression.Function.MIN, "MIN"),
        MAX(QueryExpression.Function.MAX, "MAX");

        private final QueryExpression.Function function;
        private final String value;

        Function(QueryExpression.Function function, String value) {
            this.function = function;
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public QueryExpression.Function toQueryFunction() {
            return function;
        }
    }
}
