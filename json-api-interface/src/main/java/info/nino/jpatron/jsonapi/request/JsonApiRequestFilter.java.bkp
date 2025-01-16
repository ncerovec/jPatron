package info.nino.jpatron.jsonapi.request;

import info.nino.jpatron.helpers.ConstantsUtil;
import info.nino.jpatron.helpers.ReflectionHelper;
import info.nino.jpatron.helpers.RegexHelper;
import info.nino.jpatron.jsonapi.annotiation.JsonApi;
import info.nino.jpatron.jsonapi.annotiation.JsonApiInject;
import info.nino.jpatron.request.QueryExpression;
import info.nino.jpatron.request.QuerySort;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * JSON:API request filter implementation
 */
@JsonApi
@Provider
//@PreMatching
@Priority(Priorities.ENTITY_CODER + 200)
public class JsonApiRequestFilter implements ContainerRequestFilter {   //, RequestContext

    private static final String DEFAULT_FILTER_COMPARATOR = QueryExpression.CompareOperator.EQ.name();
    private static final String DEFAULT_SEARCH_MODIFIER = QueryExpression.ValueModifier.LikeLR.name();
    private static final String DEFAULT_META_FUNCTION = QueryExpression.Function.COUNT.name();
    private static final String QUERY_PARAM_VALUE_SEPARATOR = String.valueOf(ConstantsUtil.COMMA);
    private static final Integer DEFAULT_PAGE_NUMBER = 1;
    private static final Integer DEFAULT_PAGE_SIZE = 10;

    @Inject
    @ConfigProperty(name = ConstantsUtil.JSONAPI_INTERFACE_THROW_INVALID_PATH_EXCEPTION, defaultValue = BooleanUtils.TRUE)
    Instance<Boolean> configPropertyThrowInvalidPathExceptions;

    @Context
    ResourceInfo resourceInfo;

    //@Context
    //HttpServletRequest request;

    @Inject
    @JsonApiInject
    Event<JsonApiRequest> jsonApiRequestEvent;

    private Boolean throwInvalidPathExceptions;

    protected Event<JsonApiRequest> getJsonApiRequestEvent()
    {
        return this.jsonApiRequestEvent;
    }

    @PostConstruct
    public void init()
    {
        if(this.configPropertyThrowInvalidPathExceptions.isUnsatisfied())
        {
            String throwInvalidPathExceptionsConfig = System.getProperty(ConstantsUtil.JSONAPI_INTERFACE_THROW_INVALID_PATH_EXCEPTION, BooleanUtils.TRUE);
            this.throwInvalidPathExceptions = BooleanUtils.toBoolean(throwInvalidPathExceptionsConfig);
        }
        else
        {
            this.throwInvalidPathExceptions = this.configPropertyThrowInvalidPathExceptions.get();
        }
    }

    @Override
    //@ServerRequestFilter(preMatching = true)
    public void filter(ContainerRequestContext requestContext)
    {
        ResourceInfo resourceInfo = this.resourceInfo;

        Method resourceMethod = resourceInfo.getResourceMethod();
        JsonApi jsonApiAnnot = resourceMethod.getAnnotation(JsonApi.class);
        if(jsonApiAnnot == null)
        {
            Class<?> resourceClass = resourceInfo.getResourceClass();
            jsonApiAnnot = resourceClass.getAnnotation(JsonApi.class);
        }

        Class<?> dtoClass = jsonApiAnnot.value();
        if(Object.class.equals(dtoClass)) return;    //dtoClass.isAnnotationPresent(jakarta.persistence.Entity)

        Class<?> entityClass = ReflectionHelper.resolveEntityClassFromDtoClass(dtoClass);

        boolean pagination = jsonApiAnnot.pagination();
        boolean distinct = jsonApiAnnot.distinctDataset();
        boolean readOnly = jsonApiAnnot.readOnlyDataset();
        boolean allowEntityPaths = jsonApiAnnot.allowEntityPaths();
        String[] allowedPaths = jsonApiAnnot.allowedPaths();
        //String[] fetchEntityPaths = jsonApiAnnot.fetchEntityPaths();
        String[] entityGraphPaths = jsonApiAnnot.entityGraphPaths();

        MultivaluedMap<String, String> reqQueryParams = requestContext.getUriInfo().getQueryParameters();
        JsonApiRequest.QueryParams queryParams = this.resolveQueryParams(dtoClass, reqQueryParams, allowEntityPaths, allowedPaths);

        JsonApiRequest jsonApiRequest = new JsonApiRequest(entityClass, queryParams, pagination, distinct, readOnly, entityGraphPaths);
        //this.getJsonApiRequestContext().setJsonApiRequest(jsonApiRequest);
        this.getJsonApiRequestEvent().fire(jsonApiRequest);
    }

    public JsonApiRequest.QueryParams resolveQueryParams(@Context UriInfo info, Class<?> dtoClass)
    {
        MultivaluedMap<String, String> queryParams = info.getQueryParameters();

        return this.resolveQueryParams(dtoClass, queryParams, true, null);
    }

    public JsonApiRequest.QueryParams resolveQueryParams(Class<?> dtoClass, MultivaluedMap<String, String> queryParams, boolean allowEntityPaths, String[] allowedPaths)
    {
        Integer pageSize = JsonApiRequestFilter.DEFAULT_PAGE_SIZE;
        Integer pageNumber =JsonApiRequestFilter.DEFAULT_PAGE_NUMBER;
        MultiValuedMap<Class<?>, String> includes = null;
        Map<String, Map.Entry<Class<?>, QuerySort.Direction>> sort = null;
        Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.CompareOperator, String>>> filters = null;
        Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.ValueModifier, String>>> searches = null;
        Map<Class<?>, MultiValuedMap<String, String>> distinctValues = null;
        Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.Function, String>>> metaValues = null;

        String jsonApiQueryParamRegex = "^%s\\[([^\\[\\]\\s]+)\\](?:\\[([^\\[\\]\\s]+)\\])?";
        List<String> regexAllowedPaths = (allowedPaths != null && allowedPaths.length > 0) ? RegexHelper.compileRegexWildcards(allowedPaths) : null;

        String filterQueryParamRegex = String.format(jsonApiQueryParamRegex, QueryParamType.FILTER.getName());
        Pattern filterRegex = Pattern.compile(filterQueryParamRegex);

        String searchQueryParamRegex = String.format(jsonApiQueryParamRegex, QueryParamType.SEARCH.getName());
        Pattern searchRegex = Pattern.compile(searchQueryParamRegex);

        String distinctQueryParamRegex = String.format(jsonApiQueryParamRegex, QueryParamType.DISTINCT.getName());
        Pattern distinctRegex = Pattern.compile(distinctQueryParamRegex);

        String metaQueryParamRegex = String.format(jsonApiQueryParamRegex, QueryParamType.META.getName());
        Pattern metaRegex = Pattern.compile(metaQueryParamRegex);

        if(queryParams != null && !queryParams.isEmpty())
        {
            for(Map.Entry<String, List<String>> entry : queryParams.entrySet())
            {
                String k = entry.getKey();
                List<String> v = entry.getValue();
                boolean valueIsPresent = v != null && !v.isEmpty();

                // Page query params
                boolean pageNumberIsPresent = k != null && !k.isEmpty() && QueryParamType.PAGE_NUMBER.getName().equals(k);
                if(valueIsPresent && pageNumberIsPresent)
                {
                    pageNumber = Integer.parseInt(v.get(0));
                }

                boolean pageSizeIsPresent = k != null && !k.isEmpty() && QueryParamType.PAGE_SIZE.getName().equals(k);
                if(valueIsPresent && pageSizeIsPresent)
                {
                    pageSize = Integer.parseInt(v.get(0));
                }

                // Sort query params
                boolean sortIsPresent = k != null && !k.isEmpty() && QueryParamType.SORT.getName().equals(k);
                if(valueIsPresent && sortIsPresent)
                {
                    LinkedList<String> sortValues = v.stream().map(s -> s.split(QUERY_PARAM_VALUE_SEPARATOR)).flatMap(m -> Arrays.stream(m.clone())).collect(Collectors.toCollection(LinkedList::new));

                    for(String sortPath : sortValues)
                    {
                        sortPath = sortPath.trim(); //remove leading and trailing spaces ('+' can be serialized as space)
                        if(sortPath.isEmpty()) continue;

                        QuerySort.Direction sortDirection = null;
                        if (sortPath.startsWith("-") || sortPath.startsWith("+")) {
                            sortDirection = QuerySort.Direction.resolveDirectionSign(sortPath.charAt(0));
                            sortPath = sortPath.substring(1);
                        }

                        Map.Entry<Class<?>, String> sortField = this.findEntityFieldByPath(dtoClass, sortPath, allowEntityPaths, regexAllowedPaths);

                        if(sortField != null)
                        {
                            sortPath = sortField.getValue();
                            Class<?> sortEntity = sortField.getKey();
                            if(sort == null) sort = new LinkedHashMap<>();
                            if(sort.containsKey(sortPath))
                            {
                                throw new RuntimeException("Multiple sorting by same Field path!");
                            }

                            sort.put(sortPath, new AbstractMap.SimpleImmutableEntry<>(sortEntity, sortDirection));
                        }
                    }
                }

                // Include query params
                boolean includeIsPresent = k != null && !k.isEmpty() && QueryParamType.INCLUDE.getName().equals(k);
                if(valueIsPresent && includeIsPresent)
                {
                    LinkedList<String> includeValues = v.stream().map(s -> s.split(QUERY_PARAM_VALUE_SEPARATOR)).flatMap(m -> Arrays.stream(m.clone())).collect(Collectors.toCollection(LinkedList::new));

                    for(String includePath : includeValues)
                    {
                        includePath = includePath.trim();   //remove leading and trailing spaces
                        if(includePath.isEmpty()) continue;

                        Map.Entry<Class<?>, String> includeClassField = this.findEntityFieldByPath(dtoClass, includePath, allowEntityPaths, regexAllowedPaths);

                        if(includeClassField != null)
                        {
                            Optional<Field> includeField = ReflectionHelper.findModelField(includeClassField.getKey(), ReflectionHelper.getFieldNameFromPath(includeClassField.getValue()));
                            if(!includeField.isPresent()) throw new RuntimeException(String.format("Include Entity field '%s' NOT FOUND in Class: %s!", ReflectionHelper.getFieldNameFromPath(includeClassField.getValue()), includeClassField.getKey()));

                            Class<?> includeEntity = includeField.get().getType();
                            if(Collection.class.isAssignableFrom(includeEntity)) includeEntity = (Class<?>) ((ParameterizedType) includeField.get().getGenericType()).getActualTypeArguments()[0];

                            if(includes == null) includes = new HashSetValuedHashMap<>();
                            includes.put(includeEntity, includeClassField.getValue());
                        }
                    }
                }

                //Filter query params
                Matcher filterMatcher = filterRegex.matcher(k);
                boolean filterIsPresent = k != null && !k.isEmpty() && filterMatcher.matches();
                if(filterIsPresent)
                {
                    String filterPath = filterMatcher.group(1);
                    String operator = (filterMatcher.group(2) != null) ? filterMatcher.group(2) : JsonApiRequestFilter.DEFAULT_FILTER_COMPARATOR;
                    List<String> filterValues = v;

                    Map.Entry<Class<?>, String> filterField = this.findEntityFieldByPath(dtoClass, filterPath, allowEntityPaths, regexAllowedPaths);

                    if(filterField != null)
                    {
                        Class filterEntity = filterField.getKey();
                        if(filters == null) filters = new HashMap<>();
                        if(!filters.containsKey(filterEntity)) filters.put(filterEntity, new HashMap<>());
                        Map<String, MultiValuedMap<QueryExpression.CompareOperator, String>> entityFilters = filters.get(filterEntity);

                        String fieldPath = filterField.getValue(); //NOTICE: use filterPath to allow only entityPaths from client
                        if(!entityFilters.containsKey(fieldPath)) entityFilters.put(fieldPath, new HashSetValuedHashMap<>());
                        MultiValuedMap<QueryExpression.CompareOperator, String> filterOps = entityFilters.get(fieldPath);

                        filterOps.putAll(QueryExpression.CompareOperator.valueOf(operator), filterValues);
                    }
                }

                //Search query params
                Matcher searchMatcher = searchRegex.matcher(k);
                boolean searchIsPresent = k != null && !k.isEmpty() && searchMatcher.matches();
                if(searchIsPresent)
                {
                    String searchPath = searchMatcher.group(1);
                    String searchModifier = (searchMatcher.group(2) != null) ? searchMatcher.group(2) : JsonApiRequestFilter.DEFAULT_SEARCH_MODIFIER;
                    List<String> searchValues = v;

                    Map.Entry<Class<?>, String> searchField = this.findEntityFieldByPath(dtoClass, searchPath, allowEntityPaths, regexAllowedPaths);

                    if(searchField != null)
                    {
                        Class searchEntity = searchField.getKey();
                        if(searches == null) searches = new HashMap<>();
                        if(!searches.containsKey(searchEntity)) searches.put(searchEntity, new HashMap<>());
                        Map<String, MultiValuedMap<QueryExpression.ValueModifier, String>> entitySearches = searches.get(searchEntity);

                        String fieldPath = searchField.getValue();
                        if(!entitySearches.containsKey(fieldPath)) entitySearches.put(fieldPath, new HashSetValuedHashMap<>());
                        MultiValuedMap<QueryExpression.ValueModifier, String> searchModifiers = entitySearches.get(fieldPath);

                        searchModifiers.putAll(QueryExpression.ValueModifier.valueOf(searchModifier), searchValues);
                    }
                }

                //Distinct query params
                Matcher distinctMatcher = distinctRegex.matcher(k);
                boolean distinctIsPresent = k != null && !k.isEmpty() && distinctMatcher.matches();
                if(distinctIsPresent)
                {
                    String keyColumnPath = distinctMatcher.group(1);
                    List<String> labelColumnPaths = v;

                    Map.Entry<Class<?>, String> keyField = this.findEntityFieldByPath(dtoClass, keyColumnPath, allowEntityPaths, regexAllowedPaths);

                    if(keyField != null)
                    {
                        Class distinctEntity = keyField.getKey();
                        if(distinctValues == null) distinctValues = new HashMap<>();
                        if(!distinctValues.containsKey(distinctEntity)) distinctValues.put(distinctEntity, new HashSetValuedHashMap<>());
                        MultiValuedMap<String, String> entityDistincts = distinctValues.get(distinctEntity);
                        //NOTICE allowed: if(entityDistincts.containsKey(keyField.getValue())) throw new RuntimeException(String.format("Distinct '%s' KEY field is duplicate!", keyField.getValue()));

                        CollectionUtils.emptyIfNull(labelColumnPaths).forEach(labelColumnPath ->
                        {
                            if(StringUtils.isNotBlank(labelColumnPath))
                            {
                                Map.Entry<Class<?>, String> labelField = this.findEntityFieldByPath(dtoClass, labelColumnPath, allowEntityPaths, regexAllowedPaths);
                                if(labelField != null)
                                {
                                    //NOTICE allowed: if(keyField.getKey() != labelField.getKey()) throw new RuntimeException(String.format("Distinct KEY Entity (%s) different from LABEL Entity (%s)!", keyField.getKey(), labelField.getKey()));

                                    //entityDistincts.add(keyField.getValue(), labelColumnPath);
                                    entityDistincts.put(keyField.getValue(), labelField.getValue());
                                }
                            }
                            else
                            {
                                entityDistincts.put(keyField.getValue(), null);
                            }
                        });
                    }
                }

                //Meta query params
                Matcher metaMatcher = metaRegex.matcher(k);
                boolean metaIsPresent = k != null && !k.isEmpty() && metaMatcher.matches();
                if(metaIsPresent)
                {
                    String valueColumnPath = metaMatcher.group(1);
                    String function = (metaMatcher.group(2) != null) ? metaMatcher.group(2) : JsonApiRequestFilter.DEFAULT_META_FUNCTION;
                    List<String> labelColumnPaths = v;

                    Map.Entry<Class<?>, String> valueField = this.findEntityFieldByPath(dtoClass, valueColumnPath, allowEntityPaths, regexAllowedPaths);

                    if(valueField != null)
                    {
                        Class metaEntity = valueField.getKey();
                        if(metaValues == null) metaValues = new HashMap<>();
                        if(!metaValues.containsKey(metaEntity)) metaValues.put(metaEntity, new HashMap<>());
                        Map<String, MultiValuedMap<QueryExpression.Function, String>> entityMetaValues = metaValues.get(metaEntity);

                        if(!entityMetaValues.containsKey(valueField.getValue())) entityMetaValues.put(valueField.getValue(), new HashSetValuedHashMap<>());
                        MultiValuedMap<QueryExpression.Function, String> metaFuncs = entityMetaValues.get(valueField.getValue());

                        //replaced: metaFuncs.addAll(function, labelColumnPaths);
                        CollectionUtils.emptyIfNull(labelColumnPaths).forEach(labelColumnPath ->
                        {
                            if(StringUtils.isNotBlank(labelColumnPath))
                            {
                                Map.Entry<Class<?>, String> labelField = this.findEntityFieldByPath(dtoClass, labelColumnPath, allowEntityPaths, regexAllowedPaths);
                                if(labelField != null)
                                {
                                    metaFuncs.put(QueryExpression.Function.valueOf(function), labelField.getValue());
                                }
                            }
                            else
                            {
                                metaFuncs.put(QueryExpression.Function.valueOf(function), null);
                            }
                        });
                    }
                }
            }
        }

        JsonApiRequest.QueryParams requestQueryParams = new JsonApiRequest.QueryParams(pageSize, pageNumber);
        if(sort != null) requestQueryParams.setSort(sort);
        if(includes != null) requestQueryParams.setIncludes(includes);
        if(filters != null) requestQueryParams.setFilters(filters);
        if(searches != null) requestQueryParams.setSearches(searches);
        if(distinctValues != null) requestQueryParams.setDistinctValues(distinctValues);
        if(metaValues != null) requestQueryParams.setMetaValues(metaValues);

        return requestQueryParams;
    }

    private Map.Entry<Class<?>, String> findEntityFieldByPath(Class<?> clazz, String path, boolean allowEntityDive, List<String> regexAllowedPaths)
    {
        try
        {
            Map.Entry<Class<?>, String> entityField = ReflectionHelper.findEntityFieldByPath(clazz, path, allowEntityDive);
            this.checkIfPathAllowed(path, regexAllowedPaths);
            return entityField;
        }
        catch(IllegalStateException ex)
        {
            if(this.throwInvalidPathExceptions)
            {
                throw ex;
            }
            else
            {
                return null;
            }
        }
    }

    private void checkIfPathAllowed(String fieldPath, List<String> regexAllowedPaths) throws RuntimeException
    {
        if(regexAllowedPaths != null && regexAllowedPaths.stream().noneMatch(rgx -> (ReflectionHelper.PATH_SEPARATOR + fieldPath).matches(rgx)))    //".field.path"
        {
            throw new IllegalAccessError(String.format("Field path '%s' NOT ALLOWED!", fieldPath));
        }
    }
}
