package info.nino.jpatron.metamodel;

import com.google.common.base.Joiner;
import info.nino.jpatron.helpers.ReflectionHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

/**
 * QueryExpression is Object-Oriented Query Language metamodel for EntityService Query Engine
 * Metamodel implementation for Data/Distinct/Aggregate queries used in EntityService.QueryBuilder
 */
//TODO fix: QueryExpression suffers from "telescoping constructors problem"
public class QueryExpression
{
    public enum Func { COUNT, COUNT_DISTINCT, SUM, AVG, MIN, MAX; }

    private String name;                                    //arbitrary name for QueryExpression (meta-value fields naming)
    private Class<?> rootEntity;                            //query root entity (base for value/label paths)
    private Pair<Class<?>, String> labelColumnEntityPath;   //pair of entity & column name OR path with label
    private Pair<Class<?>, String> valueColumnEntityPath;   //pair of entity & column name OR path with value
    private Func func = Func.COUNT;                         //aggregation function used with value-column (default: Func.COUNT)
    private Filter<?>[] filters;                            //additional query filters
    private boolean distinct = false;                       //distinct aggregation values (default: false)

    public QueryExpression(Class<?> rootEntity)
    {
        this.rootEntity = rootEntity;
    }

    public QueryExpression(Class<?> rootEntity, String valueColumnPath)
    {
        this.rootEntity = rootEntity;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
    }

    public QueryExpression(Class<?> rootEntity, String valueColumnPath, String labelColumnPath)
    {
        this.rootEntity = rootEntity;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.labelColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, labelColumnPath, true);
    }

    public QueryExpression(Class<?> rootEntity, String name, String valueColumnPath, String labelColumnPath)
    {
        this.rootEntity = rootEntity;
        this.name = name;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.labelColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, labelColumnPath, true);
    }

    public QueryExpression(Class<?> rootEntity, String valueColumnPath, Func func)
    {
        this.rootEntity = rootEntity;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.func = func;
    }

    public QueryExpression(String name, Class<?> rootEntity, String valueColumnPath, Func func)
    {
        this.name = name;
        this.rootEntity = rootEntity;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.func = func;
    }

    public QueryExpression(Class<?> rootEntity, String valueColumnPath, Func func, String labelColumnPath)
    {
        this.rootEntity = rootEntity;
        this.func = func;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.labelColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, labelColumnPath, true);
    }

    public QueryExpression(String name, Class<?> rootEntity, String valueColumnPath, Func func, String labelColumnPath)
    {
        this.name = name;
        this.rootEntity = rootEntity;
        this.func = func;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.labelColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, labelColumnPath, true);
    }

    public QueryExpression(Class<?> rootEntity, String valueColumnPath, Func func, Filter... filters)
    {
        this.rootEntity = rootEntity;
        this.func = func;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.filters = filters;
    }

    public QueryExpression(String name, Class<?> rootEntity, String valueColumnPath, Func func, Filter... filters)
    {
        this.name = name;
        this.rootEntity = rootEntity;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.func = func;
        this.filters = filters;
    }

    public QueryExpression(Class<?> rootEntity, String valueColumnPath, Func func, String labelColumnPath, Filter... filters)
    {
        this.rootEntity = rootEntity;
        this.func = func;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.labelColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, labelColumnPath, true);
        this.filters = filters;
    }

    public QueryExpression(String name, Class<?> rootEntity, String valueColumnPath, Func func, String labelColumnPath, Filter... filters)
    {
        this.name = name;
        this.rootEntity = rootEntity;
        this.func = func;
        this.valueColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, valueColumnPath, true);
        this.labelColumnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, labelColumnPath, true);
        this.filters = filters;
    }

    public String getName()
    {
        return name;
    }

    public Class<?> getRootEntity()
    {
        return rootEntity;
    }

    public Pair<Class<?>, String> getLabelColumnEntityPath()
    {
        return labelColumnEntityPath;
    }

    public Pair<Class<?>, String> getValueColumnEntityPath()
    {
        return valueColumnEntityPath;
    }

    public Func getFunc()
    {
        return func;
    }

    public Filter[] getFilters()
    {
        return filters;
    }

    public void setDistinct(boolean distinct)
    {
        this.distinct = distinct;
    }

    public boolean isDistinct()
    {
        return distinct;
    }

    @Override
    public String toString()
    {
        String filtersString = (this.getFilters() != null) ? Joiner.on(" AND ").skipNulls().join(this.getFilters()) : null;
        return String.format("(%s) <%s> %s:%s - FILTERS(%s)", this.getValueColumnEntityPath().getKey().getSimpleName(), this.getFunc(),  this.getValueColumnEntityPath().getValue(), this.getLabelColumnEntityPath().getValue(), filtersString);
    }

    public static class Conditional //NOTICE: Complex Logical Conditional Filters
    {
        public enum Operator { AND, OR; }

        //NOTICE: filters & conditionals can be null (memory usage reduction) - initialized HashSet only for programming convenience
        private Filter<?>[] filters;                                //simple root filters
        private Conditional[] conditionals;                         //complex/compound nested sub-filters
        private Operator logicOperator = Operator.AND;              //logical compound operator for filters & conditionals

        public Conditional() { }

        public Conditional(Operator logicOperator)
        {
            this.logicOperator = logicOperator;
        }

        public Conditional(Filter<?>... filters)
        {
            this.filters = filters;
        }

        public Conditional(Operator logicOperator, Filter<?>... filters)
        {
            this.logicOperator = logicOperator;
            this.filters = filters;
        }

        public Conditional(Operator logicOperator, Conditional... conditionals)
        {
            this.logicOperator = logicOperator;
            this.conditionals = conditionals;
        }

        public Operator getLogicOperator()
        {
            return logicOperator;
        }

        public Filter<?>[] getFilters()
        {
            return filters;
        }

        public void addFilters(Filter<?>... filters)
        {
            this.filters = ArrayUtils.addAll(this.filters, filters);
        }

        public Conditional[] getConditionals()
        {
            return conditionals;
        }

        public void addConditionals(Conditional... conditionals)
        {
            this.conditionals = ArrayUtils.addAll(this.conditionals, conditionals);
        }

        @Override
        public String toString()
        {
            String operatorString = String.format(" %s ", this.getLogicOperator());
            return String.format("(%s) %s (%s)", Joiner.on(operatorString).skipNulls().join(this.getFilters()), operatorString, Joiner.on(operatorString).skipNulls().join(this.getConditionals()));
        }
    }

    public static class Filter<T extends Comparable<? super T>>
    {
        public enum Cmp { TRUE, FALSE, IsNULL, IsNotNULL, IsEMPTY, IsNotEMPTY, EQ, NEQ, LIKE, GT, LT, GToE, LToE, IN, NotIN, EACH, NotEACH, EXCEPT, NotEXCEPT; }
        public enum Modifier { NONE, LikeL, LikeR, LikeLR, SPLIT, SplitLikeL, SplitLikeR, SplitLikeLR; }

        public static List<Cmp> booleanComparators = Arrays.asList(Cmp.TRUE, Cmp.FALSE);
        public static List<Cmp> subqueryComparators = Arrays.asList(Cmp.EACH, Cmp.NotEACH, Cmp.EXCEPT, Cmp.NotEXCEPT);
        public static List<Cmp> nonValueComparators = Arrays.asList(Cmp.IsNULL, Cmp.IsNotNULL, Cmp.IsEMPTY, Cmp.IsNotEMPTY);
        public static List<Cmp> valueComparators = Arrays.asList(Cmp.EQ, Cmp.NEQ, Cmp.LIKE, Cmp.GT, Cmp.LT, Cmp.GToE, Cmp.LToE, Cmp.IN, Cmp.NotIN, Cmp.EACH, Cmp.NotEACH, Cmp.EXCEPT, Cmp.NotEXCEPT);

        private String name;                                //arbitrary name for Filter (meta-value fields naming)
        private Class<?> rootEntity;                        //filter root entity (base for value/label paths)
        private Pair<Class<?>, String> columnEntityPath;    //pair of entity & column name OR path (from root entity)
        private Cmp cmp = Cmp.EQ;                           //filter comparison operator (default: Cmp.EQ)
        private Modifier mod = Modifier.NONE;               //filter value format (default: Modifier.NONE)
        private T[] value;                                  //filter value(s)
        //private String subquery;                          //filter subquery

        public Filter(Class<?> rootEntity, String columnPath, Cmp cmp)
        {
            this.rootEntity = rootEntity;
            this.columnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, columnPath, true);
            this.cmp = cmp;
        }

        public Filter(Class<?> rootEntity, String columnPath, T... value)
        {
            this.rootEntity = rootEntity;
            this.columnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, columnPath, true);
            this.value = value;
        }

        public Filter(Class<?> rootEntity, String columnPath, Cmp cmp, T... value)
        {
            this.rootEntity = rootEntity;
            this.columnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, columnPath, true);
            this.cmp = cmp;
            this.value = value;
        }

        public Filter(Class<?> rootEntity, String columnPath, Cmp cmp, Modifier mod, T[] value)
        {
            this.rootEntity = rootEntity;
            this.columnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, columnPath, true);
            this.cmp = cmp;
            this.mod = mod;
            this.value = value;
        }

        public Filter(String name, Class<?> rootEntity, String columnPath, Cmp cmp, T... value)
        {
            this.name = name;
            this.rootEntity = rootEntity;
            this.columnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, columnPath, true);
            this.cmp = cmp;
            this.value = value;
        }

        public String getName()
        {
            return name;
        }

        public Class<?> getRootEntity()
        {
            return rootEntity;
        }

        public Pair<Class<?>, String> getColumnEntityPath()
        {
            return columnEntityPath;
        }

        public Cmp getCmp()
        {
            return cmp;
        }

        public Modifier getMod()
        {
            return mod;
        }

        public T[] getValue()
        {
            return value;
        }

        public void setValue(T... value)
        {
            this.value = value;
        }

        @Override
        public String toString()
        {
            return String.format("'%s' (%s) <%s> %s", this.getColumnEntityPath().getValue(), this.getColumnEntityPath().getKey() != null ? this.getColumnEntityPath().getKey().getSimpleName() : null, this.getCmp(), Arrays.toString(this.getValue()));
        }
    }
}