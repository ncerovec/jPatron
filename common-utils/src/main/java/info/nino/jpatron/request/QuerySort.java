package info.nino.jpatron.request;

import info.nino.jpatron.helpers.ReflectionHelper;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Query Sort class for sorting parameters
 */
public class QuerySort {

    public enum Direction { ASC, DESC; }

    private final Pair<Class<?>, String> columnEntityPath;
    private final Direction direction;
    private Class<?> sortType;

    /**
     * Constructor for Sort object
     * @param columnPath sorting column name or path
     * @param direction sorting direction
     */
    public QuerySort(Class<?> rootEntity, String columnPath, Direction direction) {
        this.columnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, columnPath, true);
        this.direction = direction;
    }

    /**
     * Constructor for Sort object
     * @param sortType sorting type class (e.g. sort as integer while column is string)
     * @param columnPath sorting column name or path
     * @param direction sorting direction
     */
    public QuerySort(Class<?> rootEntity, String columnPath, Direction direction, Class<?> sortType) {
        this.columnEntityPath = ReflectionHelper.findEntityFieldByPath(rootEntity, columnPath, true);
        this.direction = direction;
        this.sortType = sortType;
    }

    /**
     * @return pair of sort entity &amp; column name OR path (from root entity)
     */
    public Pair<Class<?>, String> getColumnEntityPath() {
        return columnEntityPath;
    }

    /**
     * @return sorting type class
     */
    public Class<?> getSortType() {
        return sortType;
    }

    /**
     * @return sorting direction
     */
    public Direction getDirection() {
        return direction;
    }
}