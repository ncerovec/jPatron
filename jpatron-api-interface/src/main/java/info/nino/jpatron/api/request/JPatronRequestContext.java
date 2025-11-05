package info.nino.jpatron.api.request;

import info.nino.jpatron.helpers.RegexHelper;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

public class JPatronRequestContext {

    private Class<?> clazz;
    private String[] searchPaths;
    private boolean pagination;
    private boolean allowEntityPaths;
    private List<String> regexAllowedPaths;
    private ContainerRequestContext containerRequest;

    public JPatronRequestContext(Class<?> clazz,
                                 String[] searchPaths,
                                 boolean pagination,
                                 boolean allowEntityPaths,
                                 String[] allowedPaths,
                                 ContainerRequestContext containerRequest) {
        this.clazz = clazz;
        this.searchPaths = searchPaths;
        this.pagination = pagination;
        this.allowEntityPaths = allowEntityPaths;
        this.regexAllowedPaths = (ArrayUtils.isNotEmpty(allowedPaths)) ? RegexHelper.compileRegexWildcards(allowedPaths) : null;
        this.containerRequest = containerRequest;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String[] getSearchPaths() {
        return searchPaths;
    }

    public boolean isPagination() {
        return pagination;
    }

    public boolean isAllowEntityPaths() {
        return allowEntityPaths;
    }

    public List<String> getRegexAllowedPaths()
    {
        return regexAllowedPaths;
    }

    public ContainerRequestContext getContainerRequest()
    {
        return containerRequest;
    }
}
