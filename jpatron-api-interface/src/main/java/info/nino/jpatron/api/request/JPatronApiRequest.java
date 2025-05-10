package info.nino.jpatron.api.request;

import info.nino.jpatron.request.ApiRequest;
import info.nino.jpatron.request.QueryExpression;

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
