package info.nino.jpatron.api;

import info.nino.jpatron.helpers.ConstantsUtil;
import info.nino.jpatron.helpers.ReflectionHelper;
import info.nino.jpatron.request.ApiRequest;
import info.nino.jpatron.request.QueryExpression;
import info.nino.jpatron.request.QuerySort;
import jakarta.enterprise.event.Event;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public interface ApiRequestFilterInterface extends ContainerRequestFilter {

    ResourceInfo getResourceInfo();
    Event getRequestEventDelegate();

    Class<? extends Annotation> getApiAnnotation();
    FilterRequestContext initializeFilterContext(Annotation annotation, MultivaluedMap<String, String> reqQueryParam);
    ApiRequest<?> initApiRequest(Annotation annotation, Class<?> entityClass, ApiRequest.QueryParams queryParams);

    String getApiQueryParamRegex();
    String getAdvancedFilterTermRegex();
    ApiRequestNotation getApiRequestNotation();
    ApiRequestDefaults getApirequestDefaults();

    @Override
    default void filter(ContainerRequestContext requestContext) {
        Class<? extends Annotation> apiAnnotation = this.getApiAnnotation();
        Method resourceMethod = this.getResourceInfo().getResourceMethod();
        var apiAnnInstance = resourceMethod.getAnnotation(apiAnnotation);
        if (apiAnnInstance == null) {
            Class<?> resourceClass = this.getResourceInfo().getResourceClass();
            apiAnnInstance = resourceClass.getAnnotation(apiAnnotation);
        }


        MultivaluedMap<String, String> reqQueryParams = requestContext.getUriInfo().getQueryParameters();
        FilterRequestContext reqContext = this.initializeFilterContext(apiAnnInstance, reqQueryParams);
        if (Object.class.equals(reqContext.getClazz())) { //reqContext.getClazz().isAnnotationPresent(jakarta.persistence.Entity)
            return;
        }

        ApiRequest.QueryParams queryParams = this.resolveQueryParams(reqContext);
        Class<?> entityClass = ReflectionHelper.resolveEntityClassFromDtoClass(reqContext.getClazz());
        ApiRequest<?> request = this.initApiRequest(apiAnnInstance, entityClass, queryParams);

        this.getRequestEventDelegate().fire(request);
    }

    default ApiRequest.QueryParams resolveQueryParams(FilterRequestContext requestContext) {

        final var QUERY_PROPERTY_PAGE_SIZE = getApiRequestNotation().getQueryPropertyPageSize();
        final var QUERY_PROPERTY_PAGE_NUMBER = getApiRequestNotation().getQueryPropertyPageNumber();
        final var DEFAULT_PAGE_SIZE = getApirequestDefaults().getDefaultPageSize();
        final var DEFAULT_PAGE_NUMBER = getApirequestDefaults().getDefaultPageNumber();
        final var DEFAULT_FILTER_COMPARATOR = getApirequestDefaults().getDefaultFilterComparator();
        final var DEFAULT_SEARCH_MODIFIER = getApirequestDefaults().getDefaultSearchModifier();
        final var DEFAULT_META_FUNCTION = getApirequestDefaults().getDefaultMetaFunction();

        ApiRequest.QueryParams requestQueryParams = new ApiRequest.QueryParams(DEFAULT_PAGE_SIZE, DEFAULT_PAGE_NUMBER);
        if (MapUtils.isEmpty(requestContext.getQueryParams())) {
            return requestQueryParams;
        }

        String apiQueryParamRegex = this.getApiQueryParamRegex();
        Pattern queryParamRegex = Pattern.compile(apiQueryParamRegex);

        for (Map.Entry<String, List<String>> entry : requestContext.getQueryParams().entrySet()) {
            String key = entry.getKey();
            List<String> value = entry.getValue();
            Matcher queryParamMatcher = queryParamRegex.matcher(key);
            if (!queryParamMatcher.matches()) {
                continue;
            }

            String option = (queryParamMatcher.groupCount() > 0) ? queryParamMatcher.group(1) : null;
            if (getApiRequestNotation().getQueryParamTypeEnumValues().stream()
                    .noneMatch(q -> q.getQueryParam().equalsIgnoreCase(option))) {
                if (this.getApiRequestNotation().parseUnknownQueryParamTypesAsSimpleFilters()) {
                    var filters = this.parsePropertyFilter(requestContext, requestQueryParams.getFilters(), key, DEFAULT_FILTER_COMPARATOR, value);
                    requestQueryParams.setFilters(filters);
                }

                continue;
            }

            ApiRequest.QueryParams.QueryParamType queryParamOption = getApiRequestNotation().findByQueryParam(option);
            if (queryParamOption == null) {
                continue;
            }

            String property = (queryParamMatcher.groupCount() > 1) ? queryParamMatcher.group(2) : null;
            String param = (queryParamMatcher.groupCount() > 2) ? queryParamMatcher.group(3) : null;
            boolean valueIsPresent = value != null && !value.isEmpty();

            if (queryParamOption.equals(getApiRequestNotation().getPageEnum())) {   // Page query params
                if (!valueIsPresent || value.size() != 1 || !value.get(0).matches("\\d+")) {
                    throw new IllegalArgumentException(String.format("Invalid value '%s' in '%s' query parameter!", value, key));
                }

                if(QUERY_PROPERTY_PAGE_SIZE.equals(property)) {
                    requestQueryParams.setPageSize(Integer.parseInt(value.get(0)));
                } else if(QUERY_PROPERTY_PAGE_NUMBER.equals(property)) {
                    requestQueryParams.setPageNumber(Integer.parseInt(value.get(0)));
                } else {
                    throw new IllegalArgumentException(String.format("Invalid property '%s' in query parameter: %s", property, key));
                }
            } else if (queryParamOption.equals(getApiRequestNotation().getSortEnum())) {    // Sort query params
                if (!valueIsPresent || value.size() != 1) {
                    throw new IllegalArgumentException(String.format("Invalid CSV value '%s' in '%s' query parameter!", value, key));
                }

                var sort = this.parseSortExpression(requestContext, value);
                requestQueryParams.setSort(sort);
            } else if (queryParamOption.equals(getApiRequestNotation().getIncludeEnum())) { // Include query params
                if (!valueIsPresent) {
                    throw new IllegalArgumentException(String.format("Invalid value '%s' for '%s' query parameter!", value, key));
                }

                var includes = this.parseIncludeQueryParam(requestContext, requestQueryParams.getIncludes(), value);
                requestQueryParams.setIncludes(includes);
            } else if (queryParamOption.equals(getApiRequestNotation().getFilterEnum())) { // Property filter (simple) params
                if (!valueIsPresent) {
                    throw new IllegalArgumentException(String.format("Invalid value '%s' for '%s' query parameter!", value, key));
                }

                if (property == null) {
                    throw new IllegalArgumentException(String.format("Missing input property in query parameter: %s", key));
                }

                QueryExpression.CompareOperator cmp = (param != null) ? QueryExpression.CompareOperator.valueOf(param) : DEFAULT_FILTER_COMPARATOR;
                var filters = this.parsePropertyFilter(requestContext, requestQueryParams.getFilters(), property, cmp, value);
                requestQueryParams.setFilters(filters);
            } else if (queryParamOption.equals(getApiRequestNotation().getQueryEnum())) { // Filter query (advanced) params
                if (!valueIsPresent) {
                    throw new IllegalArgumentException(String.format("Invalid value '%s' for '%s' query parameter!", value, key));
                }

                QueryExpression.CompoundFilter compoundQueryTerm = this.parseQueryExpression(requestContext, value);
                requestQueryParams.setCompoundFilter(compoundQueryTerm);
            } else if (queryParamOption.equals(getApiRequestNotation().getSearchEnum())) { // Search query params
                if (!valueIsPresent) {
                    throw new IllegalArgumentException(String.format("Invalid value '%s' for '%s' query parameter!", value, key));
                }

                QueryExpression.ValueModifier mod = (param != null) ? QueryExpression.ValueModifier.valueOf(param) : DEFAULT_SEARCH_MODIFIER;
                var searches = this.parseSearchExpression(requestContext, requestQueryParams.getSearches(), property, mod, value);
                requestQueryParams.setSearches(searches);
            } else if (queryParamOption.equals(getApiRequestNotation().getDistinctEnum())) { // Distinct query params
                var distinctValues = this.parseDistinctQueryParam(requestContext, requestQueryParams.getDistinctValues(), property, value);
                requestQueryParams.setDistinctValues(distinctValues);
            } else if (queryParamOption.equals(getApiRequestNotation().getMetaEnum())) { // Meta query params
                QueryExpression.Function func = (param != null) ? QueryExpression.Function.valueOf(param) : DEFAULT_META_FUNCTION;
                var metaValues = this.parseMetaQueryParam(requestContext, requestQueryParams.getMetaValues(), property, func, value);
                requestQueryParams.setMetaValues(metaValues);
            }
        }

        return requestQueryParams;
    }

    private Pair<Class<?>, String> findEntityFieldByPath(FilterRequestContext requestContext, String path) {
        this.checkIfPathAllowed(requestContext, path);
        return ReflectionHelper.findEntityFieldByPath(requestContext.getClazz(), path, requestContext.isAllowEntityPaths());
    }

    private void checkIfPathAllowed(FilterRequestContext requestContext, String fieldPath) {
        if (requestContext.getRegexAllowedPaths() == null) {
            throw new IllegalStateException("Allowed field-paths not defined!");
        }

        if (requestContext.getRegexAllowedPaths().stream().noneMatch(fieldPath::matches)) { //".field.path"
            throw new IllegalAccessError(String.format("Field path '%s' NOT ALLOWED!", fieldPath));
        }
    }

    private Map<String, Map.Entry<Class<?>, QuerySort.Direction>> parseSortExpression(FilterRequestContext requestContext,
                                                                                      List<String> sortExpression) {
        final var SORT_DESC_SIGN = getApiRequestNotation().getSortDescSign();
        final var SORT_ASC_SIGN = getApiRequestNotation().getSortAscSign();
        final var QUERY_VALUE_SEPARATOR = getApiRequestNotation().getQueryValueSeparator();

        LinkedList<String> sortValues = sortExpression.stream().map(s -> s.split(QUERY_VALUE_SEPARATOR))
                .flatMap(m -> Arrays.stream(m.clone())).collect(Collectors.toCollection(LinkedList::new));

        Map<String, Map.Entry<Class<?>, QuerySort.Direction>> sort = null;
        for (String sortPath : sortValues) {
            sortPath = sortPath.trim(); //remove leading and trailing spaces ('+' can be serialized as space)
            if (sortPath.isEmpty()) {
                continue;
            }

            QuerySort.Direction sortDirection = getApirequestDefaults().getDefaultSortDirection();
            if (sortPath.startsWith(SORT_DESC_SIGN) || sortPath.startsWith(SORT_ASC_SIGN)) {
                sortDirection = getApiRequestNotation().resolveDirectionSign(String.valueOf(sortPath.charAt(0)));
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

    private MultiValuedMap<Class<?>, String> parseIncludeQueryParam(FilterRequestContext requestContext,
                                                                    MultiValuedMap<Class<?>, String> includes,
                                                                    List<String> value) {
        final var QUERY_VALUE_SEPARATOR = getApiRequestNotation().getQueryValueSeparator();

        LinkedList<String> includeValues = value.stream().map(s -> s.split(QUERY_VALUE_SEPARATOR))
                .flatMap(m -> Arrays.stream(m.clone())).collect(Collectors.toCollection(LinkedList::new));

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
            FilterRequestContext requestContext,
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

    private QueryExpression.CompoundFilter parseQueryExpression(FilterRequestContext requestContext,
                                                                List<String> queryTerms) {
        List<QueryExpression.CompoundFilter> compoundQueryTerms = queryTerms.stream()
                .map(queryTerm -> this.parseQueryExpression(requestContext, null, queryTerm))
                .toList();

        return (compoundQueryTerms.size() > 1)
                ? new QueryExpression.CompoundFilter(QueryExpression.LogicOperator.AND, compoundQueryTerms.toArray(new QueryExpression.CompoundFilter[0]))
                : compoundQueryTerms.get(0);
    }

    private QueryExpression.CompoundFilter parseQueryExpression(FilterRequestContext requestContext,
                                                                QueryExpression.CompoundFilter parentTerm,
                                                                String queryTerm) {
        //NOTICE: same level compound hierarchy nesting priority: OR -> AND -> (compound)
        final var QUERY_VALUE_LEFT_BRACKET = getApiRequestNotation().getQueryValueLeftBracket();
        final var QUERY_VALUE_RIGHT_BRACKET = getApiRequestNotation().getQueryValueRightBracket();

        //trim current queryTerm value to remove any excess leading/trailing white-spaces before parsing
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
        String andOperand = (" %s ").formatted(this.getApiRequestNotation().getAndCompounderEnum().getValue());
        String orOperand = (" %s ").formatted(this.getApiRequestNotation().getOrCompounderEnum().getValue());
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

    private QueryExpression.Filter<?> parseSimpleTerm(FilterRequestContext requestContext, String query) {

        Pattern termRegex = Pattern.compile(this.getAdvancedFilterTermRegex());
        Matcher termMatcher = termRegex.matcher(query);
        if (termMatcher.matches()) {
            String fieldPath = termMatcher.group(1);
            this.checkIfPathAllowed(requestContext, fieldPath);

            ApiRequest.QueryParams.ComparatorEnum cmp = this.getApiRequestNotation().getComparatorEnumValues().stream()
                    .filter(c -> c.getValue().equals(termMatcher.group(2)))
                    .findAny().orElseThrow();
            String value = termMatcher.group(3).trim();

            if (this.getApiRequestNotation().getMultiValueComparators().contains(cmp)) {
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
        final var QUERY_VALUE_ESCAPE_CHAR = getApiRequestNotation().getQueryValueEscapeChar();
        final var QUERY_VALUE_LEFT_BRACKET = getApiRequestNotation().getQueryValueLeftBracket();
        final var QUERY_VALUE_RIGHT_BRACKET = getApiRequestNotation().getQueryValueRightBracket();

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
        final var QUERY_VALUE_ESCAPE_CHAR = getApiRequestNotation().getQueryValueEscapeChar();

        int index = queryTerm.indexOf(character);
        if (index > 0 && queryTerm.charAt(index - 1) == QUERY_VALUE_ESCAPE_CHAR) {
            return indexOfFirstUnescapedChar(queryTerm.substring(index), character);
        }

        return index;
    }

    private String[] splitCSValue(String csv) {
        final var QUERY_VALUE_QUOTE = getApiRequestNotation().getQueryValueQuote();
        final var QUERY_VALUE_ESCAPE_CHAR = getApiRequestNotation().getQueryValueEscapeChar();

        boolean insideQuotes = false;
        StringBuilder currentField = new StringBuilder();

        List<String> result = new ArrayList<>();
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
        final var QUERY_VALUE_QUOTE = getApiRequestNotation().getQueryValueQuote();

        if (str != null && str.length() >= 2
                && str.startsWith(String.valueOf(QUERY_VALUE_QUOTE))
                && str.endsWith(String.valueOf(QUERY_VALUE_QUOTE))) {
            return str.substring(1, str.length() - 1);
        }

        return str; // Return the original string if no quotes are present
    }

    private Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.ValueModifier, String>>> parseSearchExpression(
            FilterRequestContext requestContext,
            Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.ValueModifier, String>>> searches,
            String reqSearchPath, QueryExpression.ValueModifier modifier, List<String> value) {
        final var FIELD_PATH_CONCATENATOR = getApiRequestNotation().getFieldPathConcatenator();

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

    private Map<Class<?>, MultiValuedMap<String, String>> parseDistinctQueryParam(FilterRequestContext requestContext,
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

    private Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.Function, String>>> parseMetaQueryParam(FilterRequestContext requestContext,
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

