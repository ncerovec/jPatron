package info.nino.jpatron.api.request;

import info.nino.jpatron.helpers.RegexHelper;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

public class JPatronRequestContext {

    private Class<?> clazz;
    private String[] searchPaths;
    private boolean allowEntityPaths;
    private List<String> regexAllowedPaths;
    private MultivaluedMap<String, String> queryParams;

    public JPatronRequestContext(Class<?> clazz,
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
