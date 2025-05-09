package info.nino.jpatron.api.request;

import info.nino.jpatron.api.annotiation.JPatronApi;
import info.nino.jpatron.api.annotiation.JPatronApiInject;
import info.nino.jpatron.helpers.ConstantsUtil;
import info.nino.jpatron.helpers.ReflectionHelper;
import info.nino.jpatron.request.ApiRequest;
import info.nino.jpatron.request.QueryExpression;
import info.nino.jpatron.request.QuerySort;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * jPatron API request filter implementation
 */
@Provider
@JPatronApi
@Priority(Priorities.ENTITY_CODER + 200)
public class JPatronApiRequestFilter implements ContainerRequestFilter {

    private static final String QUERY_VALUE_SEPARATOR = String.valueOf(ConstantsUtil.COMMA);
    private static final char QUERY_VALUE_LEFT_BRACKET = '(';
    private static final char QUERY_VALUE_RIGHT_BRACKET = ')';
    private static final char QUERY_VALUE_ESCAPE_CHAR = '\\';
    private static final char QUERY_VALUE_QUOTE = '"';
    private static final char FIELD_PATH_CONCATENATOR = '.';
    private static final String SORT_DESC_SIGN = "-";
    private static final String SORT_ASC_SIGN = "+";
    private static final String QUERY_PROPERTY_PAGE_SIZE = "size";
    private static final String QUERY_PROPERTY_PAGE_NUMBER = "number";

    private static final Integer DEFAULT_PAGE_NUMBER = 1;
    private static final Integer DEFAULT_PAGE_SIZE = 10;
    private static final QuerySort.Direction DEFAULT_SORT_DIRECTION = QuerySort.Direction.ASC;
    private static final QueryExpression.CompareOperator DEFAULT_FILTER_COMPARATOR = QueryExpression.CompareOperator.EQ;
    private static final QueryExpression.ValueModifier DEFAULT_SEARCH_MODIFIER = QueryExpression.ValueModifier.LikeLR;
    private static final QueryExpression.Function DEFAULT_META_FUNCTION = QueryExpression.Function.COUNT;

    @Context
    ResourceInfo resourceInfo;

    @Inject
    @JPatronApiInject
    Event<JPatronApiRequest<?>> requestEvent;

    @PostConstruct
    public void init() {

    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Method resourceMethod = this.resourceInfo.getResourceMethod();
        JPatronApi JPatronApiAnn = resourceMethod.getAnnotation(JPatronApi.class);
        if (JPatronApiAnn == null) {
            Class<?> resourceClass = this.resourceInfo.getResourceClass();
            JPatronApiAnn = resourceClass.getAnnotation(JPatronApi.class);
        }

        Class<?> dtoClass = JPatronApiAnn.value();
        if (Object.class.equals(dtoClass)) { //dtoClass.isAnnotationPresent(jakarta.persistence.Entity)
            return;
        }

        String[] searchPaths = JPatronApiAnn.searchPaths();
        boolean pagination = JPatronApiAnn.pagination();
        boolean allowEntityPaths = JPatronApiAnn.allowEntityPaths();
        String[] allowedPaths = JPatronApiAnn.allowedPaths();
        MultivaluedMap<String, String> reqQueryParams = requestContext.getUriInfo().getQueryParameters();
        JPatronRequestContext reqContext = new JPatronRequestContext(dtoClass, searchPaths, pagination, allowEntityPaths, allowedPaths, reqQueryParams);
        ApiRequest.QueryParams queryParams = this.resolveQueryParams(reqContext);

        Class<?> entityClass = ReflectionHelper.resolveEntityClassFromDtoClass(dtoClass);
        boolean distinct = JPatronApiAnn.distinctDataset();
        boolean readOnly = JPatronApiAnn.readOnlyDataset();
        String[] entityGraphPaths = JPatronApiAnn.entityGraphPaths();
        JPatronApiRequest<?> request = new JPatronApiRequest<>(entityClass, queryParams, distinct, readOnly, entityGraphPaths);

        this.requestEvent.fire(request);
    }

    public ApiRequest.QueryParams resolveQueryParams(JPatronRequestContext requestContext) {

        Integer defaultPageSize = (requestContext.isPagination()) ? DEFAULT_PAGE_SIZE : null;
        Integer defaultPageNumber = (requestContext.isPagination()) ? DEFAULT_PAGE_NUMBER : null;
        ApiRequest.QueryParams requestQueryParams = new ApiRequest.QueryParams(defaultPageSize, defaultPageNumber);

        if (MapUtils.isEmpty(requestContext.getQueryParams())) {
            return requestQueryParams;
        }

        String jpatronApiQueryParamRegex = "^([^\\[\\]\\s]+)(?:\\[([^\\[\\]\\s]+)\\])?(?:\\[([^\\[\\]\\s]+)\\])?";
        Pattern queryParamRegex = Pattern.compile(jpatronApiQueryParamRegex);

        for (Map.Entry<String, List<String>> entry : requestContext.getQueryParams().entrySet()) {
            String key = entry.getKey();
            List<String> value = entry.getValue();
            Matcher queryParamMatcher = queryParamRegex.matcher(key);
            if (!queryParamMatcher.matches()) {
                continue;
            }

            String option = queryParamMatcher.group(1);
            if (Arrays.stream(QueryParamType.values()).noneMatch(q -> q.getQueryParam().equalsIgnoreCase(option))) {
                continue;
            }

            QueryParamType queryParamOption = QueryParamType.findByQueryParam(option);
            String property = queryParamMatcher.group(2);
            String param = queryParamMatcher.group(3);
            boolean valueIsPresent = value != null && !value.isEmpty();

            switch (queryParamOption) {

                // Page query params
                case PAGE: {
                    if (!valueIsPresent || value.size() != 1 || !value.get(0).matches("\\d+")) {
                        throw new IllegalArgumentException(String.format("Invalid value '%s' in '%s' query parameter!", value, key));
                    }

                    if((QUERY_PROPERTY_PAGE_SIZE).equals(property)) {
                        requestQueryParams.setPageSize(Integer.parseInt(value.get(0)));
                    } else if((QUERY_PROPERTY_PAGE_NUMBER).equals(property)) {
                        requestQueryParams.setPageNumber(Integer.parseInt(value.get(0)));
                    } else {
                        throw new IllegalArgumentException(String.format("Invalid property '%s' in query parameter: %s", property, key));
                    }

                    break;
                }

                // Sort query params
                case SORT: {
                    if (!valueIsPresent || value.size() != 1) {
                        throw new IllegalArgumentException(String.format("Invalid CSV value '%s' in '%s' query parameter!", value, key));
                    }

                    var sort = this.parseSortExpression(requestContext, value);
                    requestQueryParams.setSort(sort);
                    break;
                }

                // Include query params
                case INCLUDE: {
                    if (!valueIsPresent) {
                        throw new IllegalArgumentException(String.format("Invalid value '%s' for '%s' query parameter!", value, key));
                    }

                    var includes = this.parseIncludeQueryParam(requestContext, requestQueryParams.getIncludes(), value);
                    requestQueryParams.setIncludes(includes);
                    break;
                }

                // Property filter (simple) params
                case FILTER: {
                    if (!valueIsPresent) {
                        throw new IllegalArgumentException(String.format("Invalid value '%s' for '%s' query parameter!", value, key));
                    }

                    if (property == null) {
                        throw new IllegalArgumentException(String.format("Missing input property in query parameter: %s", key));
                    }

                    QueryExpression.CompareOperator cmp = (param != null) ? QueryExpression.CompareOperator.valueOf(param) : DEFAULT_FILTER_COMPARATOR;
                    var filters = this.parsePropertyFilter(requestContext, requestQueryParams.getFilters(), property, cmp, value);
                    requestQueryParams.setFilters(filters);
                    break;
                }

                // Filter query (advanced) params
                case QUERY: {
                    if (!valueIsPresent) {
                        throw new IllegalArgumentException(String.format("Invalid value '%s' for '%s' query parameter!", value, key));
                    }

                    QueryExpression.CompoundFilter compoundQueryTerm = this.parseQueryExpression(requestContext, value);
                    requestQueryParams.setCompoundFilter(compoundQueryTerm);
                    break;
                }

                // Search query params
                case SEARCH: {
                    if (!valueIsPresent) {
                        throw new IllegalArgumentException(String.format("Invalid value '%s' for '%s' query parameter!", value, key));
                    }

                    QueryExpression.ValueModifier mod = (param != null) ? QueryExpression.ValueModifier.valueOf(param) : DEFAULT_SEARCH_MODIFIER;
                    var searches = this.parseSearchExpression(requestContext, requestQueryParams.getSearches(), property, mod, value);
                    requestQueryParams.setSearches(searches);
                    break;
                }

                //Distinct query params
                case DISTINCT: {
                    var distinctValues = this.parseDistinctQueryParam(requestContext, requestQueryParams.getDistinctValues(), property, value);
                    requestQueryParams.setDistinctValues(distinctValues);
                    break;
                }

                //Meta query params
                case META: {
                    QueryExpression.Function func = (param != null) ? QueryExpression.Function.valueOf(param) : DEFAULT_META_FUNCTION;
                    var metaValues = this.parseMetaQueryParam(requestContext, requestQueryParams.getMetaValues(), property, func, value);
                    requestQueryParams.setMetaValues(metaValues);
                    break;
                }
            }
        }

        return requestQueryParams;
    }

    private Pair<Class<?>, String> findEntityFieldByPath(JPatronRequestContext requestContext, String path) {
        this.checkIfPathAllowed(requestContext, path);
        return ReflectionHelper.findEntityFieldByPath(requestContext.getClazz(), path, requestContext.isAllowEntityPaths());
    }

    private void checkIfPathAllowed(JPatronRequestContext requestContext, String fieldPath) {
        if (requestContext.getRegexAllowedPaths() == null) {
            throw new IllegalStateException("Allowed field-paths not defined!");
        }

        if (requestContext.getRegexAllowedPaths().stream().noneMatch(fieldPath::matches)) { //".field.path"
            throw new IllegalAccessError(String.format("Field path '%s' NOT ALLOWED!", fieldPath));
        }
    }

    private Map<String, Map.Entry<Class<?>, QuerySort.Direction>> parseSortExpression(JPatronRequestContext requestContext,
                                                                                      List<String> sortExpression) {
        Map<String, Map.Entry<Class<?>, QuerySort.Direction>> sort = null;

        LinkedList<String> sortValues = sortExpression.stream().map(s -> s.split(QUERY_VALUE_SEPARATOR))
                .flatMap(m -> Arrays.stream(m.clone())).collect(Collectors.toCollection(LinkedList::new));

        for (String sortPath : sortValues) {
            sortPath = sortPath.trim(); //remove leading and trailing spaces ('+' can be serialized as space)
            if (sortPath.isEmpty()) {
                continue;
            }

            QuerySort.Direction sortDirection = DEFAULT_SORT_DIRECTION;
            if (sortPath.startsWith(SORT_DESC_SIGN) || sortPath.startsWith(SORT_ASC_SIGN)) {
                sortDirection = QuerySort.Direction.resolveDirectionSign(sortPath.charAt(0));
                sortPath = sortPath.substring(1);
            }

            Map.Entry<Class<?>, String> sortField = this.findEntityFieldByPath(requestContext, sortPath);

            if (sortField != null) {
                sortPath = sortField.getValue();
                Class<?> sortEntity = sortField.getKey();
                if (sort == null) {
                    sort = new LinkedHashMap<>();
                }
                if (sort.containsKey(sortPath)) {
                    throw new IllegalArgumentException("Multiple sorting by same Field path!");
                }

                sort.put(sortPath, new AbstractMap.SimpleImmutableEntry<>(sortEntity, sortDirection));
            }
        }

        return sort;
    }

    private MultiValuedMap<Class<?>, String> parseIncludeQueryParam(JPatronRequestContext requestContext,
                                                                    MultiValuedMap<Class<?>, String> includes,
                                                                    List<String> value) {
        LinkedList<String> includeValues = value.stream().map(s -> s.split(QUERY_VALUE_SEPARATOR)).flatMap(m -> Arrays.stream(m.clone())).collect(Collectors.toCollection(LinkedList::new));

        for(String includePath : includeValues) {
            includePath = includePath.trim();   //remove leading and trailing spaces
            if(includePath.isEmpty()) continue;

            Map.Entry<Class<?>, String> includeClassField = this.findEntityFieldByPath(requestContext, includePath);
            //TODO: check why not just: includes.put(includeClassField.getKey(), includeClassField.getValue());
            if(includeClassField != null) {
                Optional<Field> includeField = ReflectionHelper.findModelField(includeClassField.getKey(), ReflectionHelper.getFieldNameFromPath(includeClassField.getValue()));
                if(includeField.isEmpty()) throw new RuntimeException(String.format("Include Entity field '%s' NOT FOUND in Class: %s!", ReflectionHelper.getFieldNameFromPath(includeClassField.getValue()), includeClassField.getKey()));

                Class<?> includeEntity = includeField.get().getType();
                if(Collection.class.isAssignableFrom(includeEntity)) includeEntity = (Class<?>) ((ParameterizedType) includeField.get().getGenericType()).getActualTypeArguments()[0];

                if(includes == null) includes = new HashSetValuedHashMap<>();
                includes.put(includeEntity, includeClassField.getValue());
            }
        }

        return includes;
    }

    private Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.CompareOperator, String>>> parsePropertyFilter(
            JPatronRequestContext requestContext,
            Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.CompareOperator, String>>> filters,
            String property, QueryExpression.CompareOperator cmp,  List<String> values) {

        Map.Entry<Class<?>, String> filterField = this.findEntityFieldByPath(requestContext, property);

        if (filterField != null) {
            Class<?> filterEntity = filterField.getKey();
            if (filters == null) filters = new HashMap<>();
            if (!filters.containsKey(filterEntity)) filters.put(filterEntity, new HashMap<>());
            Map<String, MultiValuedMap<QueryExpression.CompareOperator, String>> entityFilters = filters.get(filterEntity);

            String fieldPath = filterField.getValue(); //NOTICE: use filterPath to allow only entityPaths from client
            if (!entityFilters.containsKey(fieldPath)) entityFilters.put(fieldPath, new HashSetValuedHashMap<>());
            MultiValuedMap<QueryExpression.CompareOperator, String> filterOps = entityFilters.get(fieldPath);

            values = values.stream().map(String::trim).map(this::removeSurroundingQuotes).toList();
            filterOps.putAll(cmp, values);
        }

        return filters;
    }

    private QueryExpression.CompoundFilter parseQueryExpression(JPatronRequestContext requestContext,
                                                                List<String> queryTerms) {
        List<QueryExpression.CompoundFilter> compoundQueryTerms = queryTerms.stream()
                .map(queryTerm -> this.parseQueryExpression(requestContext, null, queryTerm))
                .toList();

        return (compoundQueryTerms.size() > 1)
                ? new QueryExpression.CompoundFilter(QueryExpression.LogicOperator.AND, compoundQueryTerms.toArray(new QueryExpression.CompoundFilter[0]))
                : compoundQueryTerms.get(0);
    }

    private QueryExpression.CompoundFilter parseQueryExpression(JPatronRequestContext requestContext,
                                                                QueryExpression.CompoundFilter parentTerm,
                                                                String queryTerm) {
        //NOTICE: same level compound hierarchy nesting priority: OR -> AND -> (compound)
        queryTerm = queryTerm.trim();

        // Handle parentheses (sub query term)
        if (queryTerm.length() > 2
                && queryTerm.indexOf(QUERY_VALUE_LEFT_BRACKET) == 0
                && indexOfFirstUnescapedChar(queryTerm, QUERY_VALUE_RIGHT_BRACKET) == queryTerm.length() - 1) {
            var subQueryEndIndex = queryTerm.length() - 1;
            var subExpression = queryTerm.substring(1, subQueryEndIndex);
            queryTerm = queryTerm.substring(subQueryEndIndex + 1);

            var subTerm = parseQueryExpression(requestContext, null, subExpression);

            if (parentTerm != null) {
                parentTerm.addCompoundFilters(subTerm);
            } else {
                parentTerm = subTerm;
            }
        }

        if (queryTerm.isBlank()) {
            return parentTerm;
        }

        // Check for logical operators (AND / OR)
        String andOperand = (" %s ").formatted(JPatronApiRequest.CompoundOperator.AND.getValue());
        String orOperand = (" %s ").formatted(JPatronApiRequest.CompoundOperator.OR.getValue());
        var literalsFirstIndex = findFirstIndexesForLiteralsOnQueryRootLevel(queryTerm, andOperand, orOperand);
        int andIndex = literalsFirstIndex.get(andOperand);
        int orIndex = literalsFirstIndex.get(orOperand);

        String left = null;
        String right = null;
        QueryExpression.LogicOperator compoundOperator = null;
        if (andIndex != -1 && (orIndex == -1 || andIndex < orIndex)) {
            left = queryTerm.substring(0, andIndex).trim();
            right = queryTerm.substring(andIndex + andOperand.length()).trim();
            compoundOperator = QueryExpression.LogicOperator.AND;
        } else if (orIndex != -1 && (andIndex == -1 || orIndex < andIndex)) {
            left = queryTerm.substring(0, orIndex).trim();
            right = queryTerm.substring(orIndex + orOperand.length()).trim();
            compoundOperator = QueryExpression.LogicOperator.OR;
        }

        if (parentTerm == null) {
            //NOTICE: compoundOperator can be null
            parentTerm = new QueryExpression.CompoundFilter((compoundOperator != null) ? compoundOperator : QueryExpression.LogicOperator.AND);
        }

        if (compoundOperator == null) {
            var simpleTerm = parseSimpleTerm(requestContext, queryTerm);
            parentTerm.addFilters(simpleTerm);
        } else {
            if (compoundOperator == QueryExpression.LogicOperator.AND
                    && parentTerm.getLogicOperator() == QueryExpression.LogicOperator.OR) {
                //hierarchy priority: current AND compound should be added to parent OR
                QueryExpression.CompoundFilter newCompound = new QueryExpression.CompoundFilter(compoundOperator);
                parentTerm.addCompoundFilters(newCompound);
                parentTerm = newCompound;
            }

            parseQueryExpression(requestContext, parentTerm, left);

            if (compoundOperator == QueryExpression.LogicOperator.OR
                    && parentTerm.getLogicOperator() == QueryExpression.LogicOperator.AND) {
                //hierarchy priority: current OR compound should switch place with parent AND
                QueryExpression.CompoundFilter newCompound = new QueryExpression.CompoundFilter(compoundOperator);
                newCompound.addCompoundFilters(parentTerm);
                parentTerm = newCompound;
            }

            var rightTerm = parseQueryExpression(requestContext, parentTerm, right);

            //current compound is AND while next (right) is OR - right part of expression has priority (in parent "AND" recursion iteration)
            if (orIndex > andIndex) {
                parentTerm = rightTerm;
            }
        }

        return parentTerm;
    }

    private QueryExpression.Filter<?> parseSimpleTerm(JPatronRequestContext requestContext, String query) {
        Pattern termRegex = Pattern.compile("^([^\\s:=<>!#^~]+)[\\s]*(:[=<>!#^~]{0,2})[\\s]*([^\\n]*)");
        Matcher termMatcher = termRegex.matcher(query);
        if (termMatcher.matches()) {
            String fieldPath = termMatcher.group(1);
            this.checkIfPathAllowed(requestContext, fieldPath);

            JPatronApiRequest.Comparator cmp = Arrays.stream(JPatronApiRequest.Comparator.values())
                    .filter(c -> c.getValue().equals(termMatcher.group(2)))
                    .findAny().orElseThrow();
            String value = termMatcher.group(3).trim();

            if (cmp == JPatronApiRequest.Comparator.IN) {
                String[] values = this.splitCSValue(value);
                return new QueryExpression.Filter<>(requestContext.getClazz(), fieldPath, cmp.getCompareOperator(), values);
            } else {
                value = this.removeSurroundingQuotes(value);
                return new QueryExpression.Filter<>(requestContext.getClazz(), fieldPath, cmp.getCompareOperator(), value);
            }
        } else {
            throw new IllegalArgumentException("Term '%s' doesn't match JPatron REST-API guideline syntax!".formatted(query));
        }
    }

    //helper method to find the top-level logical operator (ignoring nested ones)
    private LinkedHashMap<String, Integer> findFirstIndexesForLiteralsOnQueryRootLevel(String query,
                                                                                       String... literals) {
        LinkedHashMap<String, Integer> literalIndexes = new LinkedHashMap<>();
        Arrays.stream(literals).forEach(k -> literalIndexes.put(k, -1));

        int depth = 0;
        for (int i = 0; i < query.length(); i++) {
            if (query.charAt(i) == QUERY_VALUE_LEFT_BRACKET && query.charAt(Math.abs(i - 1)) != QUERY_VALUE_ESCAPE_CHAR) {
                depth++;
            }
            if (query.charAt(i) == QUERY_VALUE_RIGHT_BRACKET && query.charAt(Math.abs(i - 1)) != QUERY_VALUE_ESCAPE_CHAR) {
                depth--;
            }

            if (depth == 0) {
                for (Map.Entry<String, Integer> entry : literalIndexes.entrySet()) {
                    if (entry.getValue() < 0 && query.startsWith(entry.getKey(), i)) {
                        literalIndexes.put(entry.getKey(), i);
                    }
                }
            }
        }

        return literalIndexes;
    }

    private int indexOfFirstUnescapedChar(String queryTerm, char character) {
        int index = queryTerm.indexOf(character);
        if (index > 0 && queryTerm.charAt(index - 1) == QUERY_VALUE_ESCAPE_CHAR) {
            return indexOfFirstUnescapedChar(queryTerm.substring(index), character);
        }

        return index;
    }

    public String[] splitCSValue(String csv) {
        List<String> result = new ArrayList<>();

        boolean insideQuotes = false;
        StringBuilder currentField = new StringBuilder();

        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);

            // Check for escaped quotes
            if (c == QUERY_VALUE_ESCAPE_CHAR && i+1 < csv.length() && csv.charAt(i+1) == QUERY_VALUE_QUOTE) {
                currentField.append(QUERY_VALUE_QUOTE); // Add the quote to the field
                i++; // Skip the backslash
            } else if (c == QUERY_VALUE_QUOTE) {
                insideQuotes = !insideQuotes;   // Toggle insideQuotes unless it is escaped
            }

            if (c == ConstantsUtil.COMMA && !insideQuotes) {
                addValueToCSVList(result, currentField);
            } else {
                // Append the character to the current field
                currentField.append(c);
            }
        }

        addValueToCSVList(result, currentField); // Add the last field
        return result.toArray(new String[0]);
    }

    private void addValueToCSVList(List<String> resultList,
                                   StringBuilder currentField) {
        String newField = currentField.toString().trim();
        newField = removeSurroundingQuotes(newField);
        resultList.add(newField);
        currentField.setLength(0); // Clear the StringBuilder
    }

    private String removeSurroundingQuotes(String str) {
        if (str != null && str.length() >= 2
                && str.startsWith(String.valueOf(QUERY_VALUE_QUOTE))
                && str.endsWith(String.valueOf(QUERY_VALUE_QUOTE))) {
            return str.substring(1, str.length() - 1);
        }

        return str; // Return the original string if no quotes are present
    }

    private Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.ValueModifier, String>>> parseSearchExpression(
            JPatronRequestContext requestContext,
            Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.ValueModifier, String>>> searches,
            String reqSearchPath, QueryExpression.ValueModifier modifier, List<String> value) {
        Map<String, Class<?>> searchFieldsPaths = new HashMap<>();

        if (reqSearchPath != null) {
            Pair<Class<?>, String> searchField = this.findEntityFieldByPath(requestContext, reqSearchPath);
            searchFieldsPaths.put(searchField.getValue(), searchField.getKey());
        } else {
            for (String searchPath : requestContext.getSearchPaths()) {
                if (searchPath.endsWith(String.valueOf(FIELD_PATH_CONCATENATOR))) {
                    String parentClassPath = searchPath.substring(0, searchPath.length() - 1);
                    Class<?> searchFieldClass = ReflectionHelper.findFieldByPath(requestContext.getClazz(), parentClassPath)
                            .map(f -> (Class) f.getType()).orElse(requestContext.getClazz());

                    ReflectionHelper.getAllModelFields(searchFieldClass)
                            .stream().filter(f -> f.getType() == String.class)
                            .forEach(fieldPath -> searchFieldsPaths.put(
                                    StringUtils.isNotBlank(parentClassPath) ? searchPath + fieldPath.getName() : fieldPath.getName(),
                                    searchFieldClass));
                } else {
                    Pair<Class<?>, String> searchField = ReflectionHelper.findEntityFieldByPath(requestContext.getClazz(), searchPath, true);
                    searchFieldsPaths.put(searchField.getValue(), searchField.getKey());
                }
            }
        }

        if (MapUtils.isEmpty(searchFieldsPaths)) {
            throw new IllegalArgumentException("Search query-param is not supported - search paths are not resolved!");
        }

        for (Map.Entry<String, Class<?>> searchField : searchFieldsPaths.entrySet()) {
            Class<?> searchEntity = searchField.getValue();

            if (searches == null)  searches = new HashMap<>();
            if (!searches.containsKey(searchEntity)) searches.put(searchEntity, new HashMap<>());

            Map<String, MultiValuedMap<QueryExpression.ValueModifier, String>> entitySearches = searches.get(searchEntity);

            String fieldPath = searchField.getKey();
            if (!entitySearches.containsKey(fieldPath)) entitySearches.put(fieldPath, new HashSetValuedHashMap<>());

            MultiValuedMap<QueryExpression.ValueModifier, String> searchModifiers = entitySearches.get(fieldPath);
            searchModifiers.putAll(modifier, value);
        }

        return searches;
    }

    private Map<Class<?>, MultiValuedMap<String, String>> parseDistinctQueryParam(JPatronRequestContext requestContext,
                                                                                  Map<Class<?>, MultiValuedMap<String, String>> distinctValues,
                                                                                  String keyColumnPath, List<String> labelColumnPaths) {
        Map.Entry<Class<?>, String> keyField = this.findEntityFieldByPath(requestContext, keyColumnPath);

        if(keyField != null) {
            Class<?> distinctEntity = keyField.getKey();
            if(distinctValues == null) distinctValues = new HashMap<>();
            if(!distinctValues.containsKey(distinctEntity)) distinctValues.put(distinctEntity, new HashSetValuedHashMap<>());
            MultiValuedMap<String, String> entityDistincts = distinctValues.get(distinctEntity);
            //NOTICE allowed: if(entityDistincts.containsKey(keyField.getValue())) throw new RuntimeException(String.format("Distinct '%s' KEY field is duplicate!", keyField.getValue()));

            CollectionUtils.emptyIfNull(labelColumnPaths).forEach(labelColumnPath -> {
                if(StringUtils.isNotBlank(labelColumnPath)) {
                    Map.Entry<Class<?>, String> labelField = this.findEntityFieldByPath(requestContext, labelColumnPath);
                    if (labelField == null) {
                        throw new IllegalStateException(String.format("Distinct Value '%s' - label field path '%s' not resolved!", keyColumnPath, labelColumnPath));
                    }
                    //NOTICE allowed: if(keyField.getKey() != labelField.getKey()) throw new RuntimeException(String.format("Distinct KEY Entity (%s) different from LABEL Entity (%s)!", keyField.getKey(), labelField.getKey()));
                    entityDistincts.put(keyField.getValue(), labelField.getValue());
                } else {
                    entityDistincts.put(keyField.getValue(), null);
                }
            });
        }

        return distinctValues;
    }

    private Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.Function, String>>> parseMetaQueryParam(JPatronRequestContext requestContext,
                                                                                                             Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.Function, String>>> metaValues,
                                                                                                             String valueColumnPath, QueryExpression.Function function, List<String> labelColumnPaths) {
        Map.Entry<Class<?>, String> valueField = this.findEntityFieldByPath(requestContext, valueColumnPath);

        if(valueField != null) {
            Class<?> metaEntity = valueField.getKey();
            if(metaValues == null) metaValues = new HashMap<>();
            if(!metaValues.containsKey(metaEntity)) metaValues.put(metaEntity, new HashMap<>());
            Map<String, MultiValuedMap<QueryExpression.Function, String>> entityMetaValues = metaValues.get(metaEntity);

            if(!entityMetaValues.containsKey(valueField.getValue())) entityMetaValues.put(valueField.getValue(), new HashSetValuedHashMap<>());
            MultiValuedMap<QueryExpression.Function, String> metaFuncs = entityMetaValues.get(valueField.getValue());

            //replaced: metaFuncs.addAll(function, labelColumnPaths);
            CollectionUtils.emptyIfNull(labelColumnPaths).forEach(labelColumnPath -> {
                if(StringUtils.isNotBlank(labelColumnPath)) {
                    Map.Entry<Class<?>, String> labelField = this.findEntityFieldByPath(requestContext, labelColumnPath);
                    if (labelField == null) {
                        throw new IllegalStateException(String.format("Meta Value '%s' - label field path '%s' not resolved!", valueColumnPath, labelColumnPath));
                    }

                    metaFuncs.put(function, labelField.getValue());
                } else {
                    metaFuncs.put(function, null);
                }
            });
        }

        return metaValues;
    }
}

