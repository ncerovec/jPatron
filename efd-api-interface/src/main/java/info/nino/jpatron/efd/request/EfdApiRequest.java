package info.nino.jpatron.efd.request;

import info.nino.jpatron.request.ApiRequest;
import info.nino.jpatron.request.QueryExpression;
import org.apache.commons.lang3.NotImplementedException;

/**
 * EFD API request implementation
 */
public class EfdApiRequest<T> extends ApiRequest<T> {

    public EfdApiRequest(Class<T> rootEntity,
                         ApiRequest.QueryParams queryParams,
                         boolean distinct,
                         boolean readOnly,
                         String[] entityGraphPaths) {
        super(rootEntity, queryParams, distinct, readOnly, null, entityGraphPaths);
    }

    @Override
    public Class<? extends SortDirectionEnum> getSortDirectionEnum()
    {
        throw new NotImplementedException();
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
        throw new NotImplementedException();
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
}
