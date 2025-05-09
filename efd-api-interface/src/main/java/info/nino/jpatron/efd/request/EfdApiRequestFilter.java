package info.nino.jpatron.efd.request;

import info.nino.jpatron.efd.annotiation.EfdApi;
import info.nino.jpatron.efd.annotiation.EfdApiInject;
import info.nino.jpatron.helpers.ConstantsUtil;
import info.nino.jpatron.helpers.ReflectionHelper;
import info.nino.jpatron.helpers.RegexHelper;
import info.nino.jpatron.request.ApiRequest;
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
import jakarta.ws.rs.ext.Provider;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * EFD API request filter implementation
 */
@EfdApi
@Provider
@Priority(Priorities.ENTITY_CODER + 200)
public class EfdApiRequestFilter implements ContainerRequestFilter {

    private static final String QUERY_VALUE_SEPARATOR = String.valueOf(ConstantsUtil.COMMA);
    private static final char QUERY_VALUE_LEFT_BRACKET = '(';
    private static final char QUERY_VALUE_RIGHT_BRACKET = ')';
    private static final char QUERY_VALUE_ESCAPE_CHAR = '\\';
    private static final char QUERY_VALUE_QUOTE = '"';
    private static final char FIELD_PATH_CONCATENATOR = '.';
    private static final String SORT_DESC_SIGN = "-";
    private static final String SORT_ASC_SIGN = "+";
    private static final Integer DEFAULT_PAGE_NUMBER = 1;
    private static final Integer DEFAULT_PAGE_SIZE = 10;
    private static final String DEFAULT_SEARCH_ESCAPE_CHARACTERS = "%:[%],_:[_]";

    @Context
    ResourceInfo resourceInfo;

    @Inject
    @EfdApiInject
    Event<EfdApiRequest> requestEvent;

    @Inject
    @ConfigProperty(name = ConstantsUtil.EFD_API_INTERFACE_SEARCH_ESCAPE_CHARACTERS, defaultValue = DEFAULT_SEARCH_ESCAPE_CHARACTERS)
    Instance<String[]> configPropertySearchEscapeCharacters;

    private List<String> regexAllowedPaths = null;
    private Map<String, String> searchEscapeCharacters = null;

    @PostConstruct
    public void init() {
        this.regexAllowedPaths = RegexHelper.compileRegexWildcards(".*");

        if (this.configPropertySearchEscapeCharacters.isUnsatisfied()) {
            String searchEscapeCharactersConfig = System.getProperty(ConstantsUtil.EFD_API_INTERFACE_SEARCH_ESCAPE_CHARACTERS, DEFAULT_SEARCH_ESCAPE_CHARACTERS);
            this.initSearchEscapeCharactersMap(Arrays.stream(searchEscapeCharactersConfig.split(String.valueOf(ConstantsUtil.COMMA))).map(String::trim).toArray(String[]::new));
        } else {
            this.initSearchEscapeCharactersMap(this.configPropertySearchEscapeCharacters.get());
        }
    }

    private void initSearchEscapeCharactersMap(String[] searchEscapeCharacters) {
        this.searchEscapeCharacters = Arrays.stream(searchEscapeCharacters)
                .map(s -> s.split(":"))
                .collect(Collectors.toMap(s -> s[0], s -> s[1]));
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Method resourceMethod = this.resourceInfo.getResourceMethod();
        EfdApi efdApiAnn = resourceMethod.getAnnotation(EfdApi.class);
        if (efdApiAnn == null) {
            Class<?> resourceClass = this.resourceInfo.getResourceClass();
            efdApiAnn = resourceClass.getAnnotation(EfdApi.class);
        }

        Class<?> dtoClass = efdApiAnn.value();
        if (Object.class.equals(dtoClass)) { //dtoClass.isAnnotationPresent(jakarta.persistence.Entity)
            return;
        }

        Class<?> entityClass = ReflectionHelper.resolveEntityClassFromDtoClass(dtoClass);

        boolean pagination = efdApiAnn.pagination();
        boolean distinct = efdApiAnn.distinctDataset();
        boolean readOnly = efdApiAnn.readOnlyDataset();
        String[] searchPaths = efdApiAnn.searchPaths();
        String[] entityGraphPaths = efdApiAnn.entityGraphPaths();

        MultivaluedMap<String, String> reqQueryParams = requestContext.getUriInfo().getQueryParameters();
        ApiRequest.QueryParams queryParams = this.resolveQueryParams(dtoClass, reqQueryParams, searchPaths, pagination);

        EfdApiRequest request = new EfdApiRequest(entityClass, queryParams, distinct, readOnly, entityGraphPaths);
        this.requestEvent.fire(request);
    }

    public ApiRequest.QueryParams resolveQueryParams(Class<?> dtoClass, MultivaluedMap<String, String> queryParams, String[] searchPaths, boolean pagination) {

        Integer defaultPageSize = (pagination) ? DEFAULT_PAGE_SIZE : null;
        Integer defaultPageNumber = (pagination) ? DEFAULT_PAGE_NUMBER : null;
        ApiRequest.QueryParams requestQueryParams = new ApiRequest.QueryParams(defaultPageSize, defaultPageNumber);

        if (queryParams != null && !queryParams.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
                String k = entry.getKey();
                List<String> v = entry.getValue();
                boolean valueIsPresent = v != null && !v.isEmpty();

                // Page query params
                boolean pageNumberIsPresent = QueryParamType.PAGE_NUMBER.getName().equals(k);
                if (valueIsPresent && pageNumberIsPresent) {
                    requestQueryParams.setPageNumber(Integer.parseInt(v.get(0)));
                }

                boolean pageSizeIsPresent = QueryParamType.PAGE_SIZE.getName().equals(k);
                if (valueIsPresent && pageSizeIsPresent) {
                    requestQueryParams.setPageSize(Integer.parseInt(v.get(0)));
                }

                // Sort query params
                boolean sortIsPresent = QueryParamType.SORT.getName().equals(k);
                if (valueIsPresent && sortIsPresent) {
                    var sort = parseSortExpression(dtoClass, v);
                    requestQueryParams.setSort(sort);
                }

                //Simple query params
                boolean propertyFilterPresent = Arrays.stream(QueryParamType.values()).noneMatch(q -> q.getName().equalsIgnoreCase(k));
                if (propertyFilterPresent) {
                    var filters = parsePropertyFilter(requestQueryParams.getFilters(), dtoClass, k, v);
                    requestQueryParams.setFilters(filters);
                }

                //Filter query params
                boolean filterIsPresent = QueryParamType.QUERY.getName().equals(k);
                if (valueIsPresent && filterIsPresent) {
                    QueryExpression.CompoundFilter compoundQueryTerm = this.parseQueryExpression(dtoClass, v);
                    requestQueryParams.setCompoundFilter(compoundQueryTerm);
                }

                //Search query params
                boolean searchIsPresent = QueryParamType.SEARCH.getName().equals(k);
                if (valueIsPresent && searchIsPresent) {
                    var searches = parseSearchExpression(requestQueryParams.getSearches(), dtoClass, searchPaths, v);
                    requestQueryParams.setSearches(searches);
                }
            }
        }

        return requestQueryParams;
    }

    private Pair<Class<?>, String> findEntityFieldByPath(Class<?> clazz, String path) {
        this.checkIfPathAllowed(path, this.regexAllowedPaths);
        return ReflectionHelper.findEntityFieldByPath(clazz, path, false);
    }

    private void checkIfPathAllowed(String fieldPath, List<String> regexAllowedPaths) {
        if (regexAllowedPaths == null) {
            throw new IllegalStateException("Allowed field-paths not defined!");
        }

        if (regexAllowedPaths.stream().noneMatch(fieldPath::matches)) { //".field.path"
            throw new IllegalAccessError(String.format("Field path '%s' NOT ALLOWED!", fieldPath));
        }
    }

    private Map<String, Map.Entry<Class<?>, QuerySort.Direction>> parseSortExpression(Class<?> clazz, List<String> sortExpression) {

        Map<String, Map.Entry<Class<?>, QuerySort.Direction>> sort = null;

        LinkedList<String> sortValues = sortExpression.stream().map(s -> s.split(QUERY_VALUE_SEPARATOR))
                .flatMap(m -> Arrays.stream(m.clone())).collect(Collectors.toCollection(LinkedList::new));

        for (String sortPath : sortValues) {
            sortPath = sortPath.trim(); //remove leading and trailing spaces ('+' can be serialized as space)
            if (sortPath.isEmpty()) {
                continue;
            }

            QuerySort.Direction sortDirection = QuerySort.Direction.ASC;
            if (sortPath.startsWith(SORT_DESC_SIGN) || sortPath.startsWith(SORT_ASC_SIGN)) {
                sortDirection = QuerySort.Direction.resolveDirectionSign(sortPath.charAt(0));
                sortPath = sortPath.substring(1);
            }

            Map.Entry<Class<?>, String> sortField = this.findEntityFieldByPath(clazz, sortPath);

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

    private Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.CompareOperator, String>>> parsePropertyFilter(Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.CompareOperator, String>>> filters, Class<?> clazz, String property, List<String> values) {

        Map.Entry<Class<?>, String> filterField = this.findEntityFieldByPath(clazz, property);

        if (filterField != null) {
            Class<?> filterEntity = filterField.getKey();
            if (filters == null) filters = new HashMap<>();
            if (!filters.containsKey(filterEntity)) filters.put(filterEntity, new HashMap<>());
            Map<String, MultiValuedMap<QueryExpression.CompareOperator, String>> entityFilters = filters.get(filterEntity);

            String fieldPath = filterField.getValue(); //NOTICE: use filterPath to allow only entityPaths from client
            if (!entityFilters.containsKey(fieldPath)) entityFilters.put(fieldPath, new HashSetValuedHashMap<>());
            MultiValuedMap<QueryExpression.CompareOperator, String> filterOps = entityFilters.get(fieldPath);

            values = values.stream().map(String::trim).map(this::removeSurroundingQuotes).toList();
            filterOps.putAll(QueryExpression.CompareOperator.EQ, values);
        }

        return filters;
    }

    private QueryExpression.CompoundFilter parseQueryExpression(Class<?> clazz, List<String> queryTerms) {

        List<QueryExpression.CompoundFilter> compoundQueryTerms = queryTerms.stream()
                .map(queryTerm -> this.parseQueryExpression(null, clazz, queryTerm))
                .toList();

        return (compoundQueryTerms.size() > 1)
                ? new QueryExpression.CompoundFilter(QueryExpression.LogicOperator.AND, compoundQueryTerms.toArray(new QueryExpression.CompoundFilter[0]))
                : compoundQueryTerms.get(0);
    }

    private QueryExpression.CompoundFilter parseQueryExpression(QueryExpression.CompoundFilter parentTerm,
                                                                Class<?> clazz, String queryTerm) {

        //NOTICE: same level compound hierarchy nesting priority: OR -> AND -> (compound)
        queryTerm = queryTerm.trim();

        // Handle parentheses (sub query term)
        if (queryTerm.length() > 2
                && queryTerm.indexOf(QUERY_VALUE_LEFT_BRACKET) == 0
                && indexOfFirstUnescapedChar(queryTerm, QUERY_VALUE_RIGHT_BRACKET) == queryTerm.length() - 1) {
            var subQueryEndIndex = queryTerm.length() - 1;
            var subExpression = queryTerm.substring(1, subQueryEndIndex);
            queryTerm = queryTerm.substring(subQueryEndIndex + 1);

            var subTerm = parseQueryExpression(null, clazz, subExpression);

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
        String andOperand = (" %s ").formatted(EfdApiRequest.CompoundOperator.AND.getValue());
        String orOperand = (" %s ").formatted(EfdApiRequest.CompoundOperator.OR.getValue());
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
            var simpleTerm = parseSimpleTerm(clazz, queryTerm);
            parentTerm.addFilters(simpleTerm);
        } else {
            if (compoundOperator == QueryExpression.LogicOperator.AND
                    && parentTerm.getLogicOperator() == QueryExpression.LogicOperator.OR) {
                //hierarchy priority: current AND compound should be added to parent OR
                QueryExpression.CompoundFilter newCompound = new QueryExpression.CompoundFilter(compoundOperator);
                parentTerm.addCompoundFilters(newCompound);
                parentTerm = newCompound;
            }

            parseQueryExpression(parentTerm, clazz, left);

            if (compoundOperator == QueryExpression.LogicOperator.OR
                    && parentTerm.getLogicOperator() == QueryExpression.LogicOperator.AND) {
                //hierarchy priority: current OR compound should switch place with parent AND
                QueryExpression.CompoundFilter newCompound = new QueryExpression.CompoundFilter(compoundOperator);
                newCompound.addCompoundFilters(parentTerm);
                parentTerm = newCompound;
            }

            var rightTerm = parseQueryExpression(parentTerm, clazz, right);

            //current compound is AND while next (right) is OR - right part of expression has priority (in parent "AND" recursion iteration)
            if (orIndex > andIndex) {
                parentTerm = rightTerm;
            }
        }

        return parentTerm;
    }

    private QueryExpression.Filter<?> parseSimpleTerm(Class<?> clazz, String query) {
        Pattern termRegex = Pattern.compile("^([^\\s:=<>!#^~]+)[\\s]*(:[=<>!#^~]{0,2})[\\s]*([^\\n]*)");
        Matcher termMatcher = termRegex.matcher(query);
        if (termMatcher.matches()) {
            String fieldPath = termMatcher.group(1);
            this.checkIfPathAllowed(fieldPath, this.regexAllowedPaths);

            EfdApiRequest.Comparator cmp = Arrays.stream(EfdApiRequest.Comparator.values())
                    .filter(c -> c.getValue().equals(termMatcher.group(2)))
                    .findAny().orElseThrow();
            String value = termMatcher.group(3).trim();

            if (cmp == EfdApiRequest.Comparator.IN) {
                String[] values = this.splitCSValue(value);
                return new QueryExpression.Filter<>(clazz, fieldPath, cmp.getCompareOperator(), values);
            } else {
                value = this.removeSurroundingQuotes(value);
                return new QueryExpression.Filter<>(clazz, fieldPath, cmp.getCompareOperator(), value);
            }
        } else {
            throw new IllegalArgumentException("Term '%s' doesn't match EFD REST-API guideline syntax!".formatted(query));
        }
    }

    //helper method to find the top-level logical operator (ignoring nested ones)
    private LinkedHashMap<String, Integer> findFirstIndexesForLiteralsOnQueryRootLevel(String query, String... literals) {
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

    private void addValueToCSVList(List<String> resultList, StringBuilder currentField) {
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
            Map<Class<?>, Map<String, MultiValuedMap<QueryExpression.ValueModifier, String>>> searches,
            Class<?> clazz, String[] searchPaths, List<String> values) {

        Map<String, Class<?>> searchFieldsPaths = new HashMap<>();
        for (String searchPath : searchPaths) {
            if (searchPath.endsWith(String.valueOf(FIELD_PATH_CONCATENATOR))) {
                String parentClassPath = searchPath.substring(0, searchPath.length() - 1);
                Class<?> searchFieldClass = ReflectionHelper.findFieldByPath(clazz, parentClassPath)
                        .map(f -> (Class) f.getType()).orElse(clazz);
                ReflectionHelper.getAllModelFields(searchFieldClass)
                        .stream().filter(f -> f.getType() == String.class)
                        .forEach(fieldPath -> searchFieldsPaths.put(StringUtils.isNotBlank(parentClassPath)
                                ? searchPath + fieldPath.getName() : fieldPath.getName(), searchFieldClass));
            } else {
                Pair<Class<?>, String> searchField = ReflectionHelper.findEntityFieldByPath(clazz, searchPath, true);
                searchFieldsPaths.put(searchField.getValue(), searchField.getKey());
            }
        }

        if (MapUtils.isEmpty(searchFieldsPaths)) {
            throw new IllegalArgumentException("Search query-param is not supported - search paths are not resolved!");
        }

        values = this.escapeSearchValuesForSqlLikeQuery(values);

        for (Map.Entry<String, Class<?>> searchField : searchFieldsPaths.entrySet()) {
            Class<?> searchEntity = searchField.getValue();

            if (searches == null)  searches = new HashMap<>();
            if (!searches.containsKey(searchEntity)) searches.put(searchEntity, new HashMap<>());

            Map<String, MultiValuedMap<QueryExpression.ValueModifier, String>> entitySearches = searches.get(searchEntity);

            String fieldPath = searchField.getKey();
            if (!entitySearches.containsKey(fieldPath)) entitySearches.put(fieldPath, new HashSetValuedHashMap<>());

            MultiValuedMap<QueryExpression.ValueModifier, String> searchModifiers = entitySearches.get(fieldPath);
            searchModifiers.putAll(QueryExpression.ValueModifier.LikeLR, values);
        }

        return searches;
    }

    private List<String> escapeSearchValuesForSqlLikeQuery(List<String> values) {
        return values.stream().map(v -> {
            for (Map.Entry<String, String> entry : this.searchEscapeCharacters.entrySet()) {
                v = v.replace(entry.getKey(), entry.getValue());
            }
            return v;
        }).collect(Collectors.toList());
    }
}
