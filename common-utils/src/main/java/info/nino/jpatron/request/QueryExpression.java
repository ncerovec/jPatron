package info.nino.jpatron.request;

import com.google.common.base.Joiner;
import info.nino.jpatron.helpers.ReflectionHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * QueryExpression is Object-Oriented Query Language metamodel for EntityService Query Engine
 * Metamodel implementation for Data/Distinct/Aggregate queries used in EntityService.QueryBuilder
 */
//TODO fix: QueryExpression suffers from "telescoping constructors problem"
public class QueryExpression {

    public enum LogicOperator { AND, OR; }
    public enum CompareOperator { TRUE, FALSE, IsNULL, IsNotNULL, IsEMPTY, IsNotEMPTY, EQ, NEQ, LIKE, GT, LT, GToE, LToE, IN, NotIN, EACH, NotEACH, EXCEPT, NotEXCEPT; }
    public enum Function { COUNT, COUNT_DISTINCT, SUM, AVG, MIN, MAX; }
    public enum ValueModifier { NONE, LikeL, LikeR, LikeLR, SPLIT, SplitLikeL, SplitLikeR, SplitLikeLR; }

    private String name;                                    //arbitrary name for QueryExpression (meta-value fields naming)
    private Class<?> rootEntity;                            //query root entity (base for value/label paths)
    private Pair<Class<?>, String> labelColumnEntityPath;   //pair of entity & column name OR path with label
    private Pair<Class<?>, String> valueColumnEntityPath;   //pair of entity & column name OR path with value
    private Function function = Function.COUNT;             //aggregation function used with value-column (default: Func.COUNT)
    private Filter<?>[] filters;                            //additional query filters
    private boolean distinct = false;                       //distinct aggregation values (default: false)

    public QueryExpression(Class<?> rootEntity) {
        this.rootEntity = rootEntity;
    }

    public QueryExpression(Class<?> rootEntity, String valueColumnPath) {
        this.rootEntity = rootEntity;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
    }

    public QueryExpression(Class<?> rootEntity, String valueColumnPath, String labelColumnPath) {
        this.rootEntity = rootEntity;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.labelColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, labelColumnPath, true);
    }

    public QueryExpression(Class<?> rootEntity, String name, String valueColumnPath, String labelColumnPath) {
        this.rootEntity = rootEntity;
        this.name = name;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.labelColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, labelColumnPath, true);
    }

    public QueryExpression(Class<?> rootEntity, String valueColumnPath, Function function) {
        this.rootEntity = rootEntity;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.function = function;
    }

    public QueryExpression(String name, Class<?> rootEntity, String valueColumnPath, Function function) {
        this.name = name;
        this.rootEntity = rootEntity;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.function = function;
    }

    public QueryExpression(Class<?> rootEntity, String valueColumnPath, Function function, String labelColumnPath) {
        this.rootEntity = rootEntity;
        this.function = function;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.labelColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, labelColumnPath, true);
    }

    public QueryExpression(String name, Class<?> rootEntity, String valueColumnPath, Function function, String labelColumnPath) {
        this.name = name;
        this.rootEntity = rootEntity;
        this.function = function;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.labelColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, labelColumnPath, true);
    }

    public QueryExpression(Class<?> rootEntity, String valueColumnPath, Function function, Filter... filters) {
        this.rootEntity = rootEntity;
        this.function = function;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.filters = filters;
    }

    public QueryExpression(String name, Class<?> rootEntity, String valueColumnPath, Function function, Filter... filters) {
        this.name = name;
        this.rootEntity = rootEntity;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.function = function;
        this.filters = filters;
    }

    public QueryExpression(Class<?> rootEntity, String valueColumnPath, Function function, String labelColumnPath, Filter... filters) {
        this.rootEntity = rootEntity;
        this.function = function;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.labelColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, labelColumnPath, true);
        this.filters = filters;
    }

    public QueryExpression(String name, Class<?> rootEntity, String valueColumnPath, Function function, String labelColumnPath, Filter... filters) {
        this.name = name;
        this.rootEntity = rootEntity;
        this.function = function;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.labelColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, labelColumnPath, true);
        this.filters = filters;
    }

    public String getName() {
        return name;
    }

    public Class<?> getRootEntity() {
        return rootEntity;
    }

    public Pair<Class<?>, String> getLabelColumnEntityPath() {
        return labelColumnEntityPath;
    }

    public Pair<Class<?>, String> getValueColumnEntityPath() {
        return valueColumnEntityPath;
    }

    public Function getFunc() {
        return function;
    }

    public Filter[] getFilters() {
        return filters;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public boolean isDistinct() {
        return distinct;
    }

    @Override
    public String toString() {
        String filtersString = (this.getFilters() != null) ? Joiner.on(" AND ").skipNulls().join(this.getFilters()) : null;
        return String.format("(%s) <%s> %s:%s - FILTERS(%s)",
                this.getValueColumnEntityPath().getKey().getSimpleName(),
                this.getFunc(),
                this.getValueColumnEntityPath().getValue(),
                this.getLabelColumnEntityPath().getValue(),
                filtersString);
    }

    public static class CompoundFilter { //NOTICE: Complex Logical Conditional Filters (ex Conditional)

        //NOTICE: filters & conditionals can be null (memory usage reduction) - initialized HashSet only for programming convenience
        private LinkedList<Filter<?>> filters;                                //simple root filters
        private LinkedList<CompoundFilter> compoundFilters;                   //complex/compound nested sub-filters
        private LogicOperator logicOperator = LogicOperator.AND;    //logical compound operator for filters & conditionals

        public CompoundFilter() { }

        public CompoundFilter(LogicOperator logicOperator) {
            this.logicOperator = logicOperator;
        }

        public CompoundFilter(Filter<?>... filters) {
            this.filters = new LinkedList<>(Arrays.asList(filters));
        }

        public CompoundFilter(LogicOperator logicOperator, Filter<?>... filters) {
            this.logicOperator = logicOperator;
            this.filters = new LinkedList<>(Arrays.asList(filters));
        }

        public CompoundFilter(LogicOperator logicOperator, CompoundFilter... compoundFilters) {
            this.logicOperator = logicOperator;
            this.compoundFilters = new LinkedList<>(Arrays.asList(compoundFilters));
        }

        public void setLogicOperator(LogicOperator logicOperator) {
            this.logicOperator = logicOperator;
        }

        public LogicOperator getLogicOperator() {
            return logicOperator;
        }

        public LinkedList<Filter<?>> getFilters() {
            return filters;
        }

        public void addFilters(Filter<?>... filters) {
            if (this.filters == null) {
                this.filters = new LinkedList<>();
            }

            CollectionUtils.addAll(this.filters, filters);
        }

        public LinkedList<CompoundFilter> getCompoundFilters() {
            return compoundFilters;
        }

        public void addCompoundFilters(CompoundFilter... compoundFilters) {
            if (this.compoundFilters == null) {
                this.compoundFilters = new LinkedList<>();
            }

            CollectionUtils.addAll(this.compoundFilters, compoundFilters);
        }

        @Override
        public String toString() {
            String operatorString = String.format(" %s ", this.getLogicOperator());
            return String.format("(%s) %s (%s)",
                    (this.getFilters() != null) ? Joiner.on(operatorString).skipNulls().join(this.getFilters()) : null,
                    operatorString,
                    (this.getCompoundFilters() != null) ? Joiner.on(operatorString).skipNulls().join(this.getCompoundFilters()) : null);
        }
    }

    public static class Filter<T extends Comparable<? super T>> {

        private String name;                                            //arbitrary name for Filter (meta-value fields naming)
        private Class<?> rootEntity;                                    //filter root entity (base for value/label paths)
        private Pair<Class<?>, String> columnEntityPath;                //pair of entity & column name OR path (from root entity)
        private CompareOperator compareOperator = CompareOperator.EQ;   //filter comparison operator (default: Cmp.EQ)
        private ValueModifier valueModifier = ValueModifier.NONE;       //filter value format (default: Modifier.NONE)
        private T[] value;                                              //filter value(s)
        //private String subquery;                                      //filter subquery

        public Filter(Class<?> rootEntity, String columnPath, CompareOperator compareOperator) {
            this.rootEntity = rootEntity;
            this.columnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, columnPath, true);
            this.compareOperator = compareOperator;
        }

        public Filter(Class<?> rootEntity, String columnPath, T... value) {
            this.rootEntity = rootEntity;
            this.columnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, columnPath, true);
            this.value = value;
        }

        public Filter(Class<?> rootEntity, String columnPath, CompareOperator compareOperator, T... value) {
            this.rootEntity = rootEntity;
            this.columnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, columnPath, true);
            this.compareOperator = compareOperator;
            this.value = value;
        }

        public Filter(Class<?> rootEntity, String columnPath, CompareOperator compareOperator, ValueModifier valueModifier, T... value) {
            this.rootEntity = rootEntity;
            this.columnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, columnPath, true);
            this.compareOperator = compareOperator;
            this.valueModifier = valueModifier;
            this.value = value;
        }

        public Filter(String name, Class<?> rootEntity, String columnPath, CompareOperator compareOperator, T... value) {
            this.name = name;
            this.rootEntity = rootEntity;
            this.columnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, columnPath, true);
            this.compareOperator = compareOperator;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public Class<?> getRootEntity() {
            return rootEntity;
        }

        public Pair<Class<?>, String> getColumnEntityPath() {
            return columnEntityPath;
        }

        public CompareOperator getCompareOperator() {
            return compareOperator;
        }

        public ValueModifier getValueModifier() {
            return valueModifier;
        }

        public T[] getValue() {
            return value;
        }

        public void setValue(T... value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format("'%s' (%s) <%s> %s",
                    this.getColumnEntityPath().getValue(),
                    this.getColumnEntityPath().getKey() != null ? this.getColumnEntityPath().getKey().getSimpleName() : null,
                    this.getCompareOperator(),
                    Arrays.toString(this.getValue()));
        }
    }
}