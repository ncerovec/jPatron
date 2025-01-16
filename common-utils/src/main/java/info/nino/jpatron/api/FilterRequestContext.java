package info.nino.jpatron.api;

import info.nino.jpatron.helpers.RegexHelper;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

public class FilterRequestContext {

    private final Class<?> clazz;
    private final String[] searchPaths;
    private final boolean allowEntityPaths;
    private final List<String> regexAllowedPaths;
    private final MultivaluedMap<String, String> queryParams;

    public FilterRequestContext(Class<?> clazz,
                                String[] searchPaths,
                                boolean allowEntityPaths,
                                String[] allowedPaths,
                                MultivaluedMap<String, String> queryParams) {
        this.clazz = clazz;
        this.queryParams = queryParams;
        this.searchPaths = searchPaths;
        this.allowEntityPaths = allowEntityPaths;
        this.regexAllowedPaths = (ArrayUtils.isNotEmpty(allowedPaths)) ? RegexHelper.compileRegexWildcards(allowedPaths) : null;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String[] getSearchPaths() {
        return searchPaths;
    }

    public boolean isAllowEntityPaths() {
        return allowEntityPaths;
    }

    public List<String> getRegexAllowedPaths()
    {
        return regexAllowedPaths;
    }

    public MultivaluedMap<String, String> getQueryParams() {
        return queryParams;
    }
}
