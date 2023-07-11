package info.nino.jpatron.services.entity;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import info.nino.jpatron.helpers.ConstantsUtil;
import info.nino.jpatron.helpers.DateTimeFormatUtil;
import info.nino.jpatron.helpers.ReflectionHelper;
import info.nino.jpatron.pagination.Page;
import info.nino.jpatron.pagination.PageRequest;
import javax.persistence.*;
import javax.persistence.criteria.*;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.QueryHints;
import org.hibernate.query.criteria.internal.path.AbstractPathImpl;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//TODO: refactoring using JAVA 11+ Interface private methods
//@LocalBean  //WARNING: JavaBean has to be annotated as @LocalBean when implementing interface!
public interface EntityService<E>
{
    Logger logger = Logger.getLogger(EntityService.class.getName());

    String LABEL_PATHS_SEPARATOR = ConstantsUtil.COMMA;

    //@PersistenceContext(unitName = "primary")
    //EntityManager em = null;

    Base baseInstance = null;

    public EntityManager getEntityManager();

    default boolean isLoggingEnabled()
    {
        return false;
    }

    //TODO: fix unnecessary instantiation
    default Base<E> getBaseInstance()
    {
        EntityManager em = this.getEntityManager();
        return new Base<E>(this, em);
    }

    default PredicateBuilder<E> getPredicateBuilderInstance()
    {
        Base<E> base = this.getBaseInstance();
        return new PredicateBuilder<E>(this, base);
    }

    default QueryBuilder<E> getQueryBuilderInstance()
    {
        PredicateBuilder<E> pb = this.getPredicateBuilderInstance();
        return new QueryBuilder<E>(this, pb);
    }

    //public Class<E> entityClass = null;
    default Class<E> getEntityClass()
    {
        Class<E> type = null;
        //NOTICE: Hibernate problem with Properties Entity keyword (https://hibernate.atlassian.net/browse/HHH-11368)

        try
        {
            type = this.getBaseInstance().findEntityClass();
        }
        catch(RuntimeException ex)
        {
            throw new RuntimeException("EntityClass NOT FOUND - Override EntityService.getEntityClass() method: " + ex.getMessage());
        }

        return type;
    }

    //default void extendFromQuery(Root<E> root)
    //{
    //    //WARNING: hardcoded/static Joins MUST have ALIAS to work properly!
    //}

    default Page<E> dataQuery(PageRequest request)
    {
        Class<E> entity = this.getEntityClass();
        EntityManager em = this.getBaseInstance().resolveEntityManager();

        return this.getQueryBuilderInstance().dataQuery(em, entity, request);
    }

    default Map<String, Map<Object, Object>> distinctQuery(PageRequest request)
    {
        Class<E> entity = this.getEntityClass();
        EntityManager em = this.getBaseInstance().resolveEntityManager();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = Core.createTupleQuery(cb, entity);

        if(request.getQueryFilters() != null)
        {
            Predicate queryPredicate = query.getRestriction();
            List<Predicate> newPredicates = this.getQueryBuilderInstance().getFilters(cb, query, entity, request.getQueryFilters());
            for(Predicate p : newPredicates)
            {
                queryPredicate = PredicateUtil.combinePredicates(cb, queryPredicate, p, QueryExpression.Conditional.Operator.AND);
            }

            if(queryPredicate != null)
            {
                query.where(queryPredicate);
            }
        }

        return this.getQueryBuilderInstance().getDistinctValues(em, query, entity, request.getDistinctColumns());
    }

    default Map<String, Map<Object, Object>> metaQuery(PageRequest request)
    {
        Class<E> entity = this.getEntityClass();
        EntityManager em = this.getBaseInstance().resolveEntityManager();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = Core.createTupleQuery(cb, entity);

        if(request.getQueryFilters() != null)
        {
            Predicate queryPredicate = query.getRestriction();
            List<Predicate> newPredicates = this.getQueryBuilderInstance().getFilters(cb, query, entity, request.getQueryFilters());
            for(Predicate p : newPredicates)
            {
                queryPredicate = PredicateUtil.combinePredicates(cb, queryPredicate, p, QueryExpression.Conditional.Operator.AND);
            }

            if(queryPredicate != null)
            {
                query.where(queryPredicate);
            }
        }

        return this.getQueryBuilderInstance().getMetaValues(em, query, entity, request.getMetaColumns());
    }

    public class Base<E>
    {
        private EntityService<E> es = null;
        private EntityManager em = null;

        public Base(EntityService<E> es, EntityManager em)
        {
            this.es = es;
            this.em = em;
        }

        private Class<E> findEntityClass()
        {
            Class<E> type = null;

            try
            {
                type = ReflectionHelper.getGenericClassParameter((Class<?>) es.getClass().getGenericSuperclass(), EntityService.class, 0);
            }
            catch(RuntimeException ex)
            {
                if(es.isLoggingEnabled()) logger.log(Level.WARNING, String.format("EntityClass not resolved by EntityService superclass (fallback to manual resolver): %s", ex.getMessage()));

                Class<?> genericInterfaceImpl = Helper.findGenericClassImplementation(es.getClass(), EntityService.class);
                Optional<Type> genericInterface = Helper.findGenericClassInterface(genericInterfaceImpl, EntityService.class);

                ParameterizedType parametrizedType = null;
                if(genericInterface.isPresent() && genericInterface.get() instanceof ParameterizedType) parametrizedType = (ParameterizedType) genericInterface.get();
                else throw new RuntimeException(String.format("%s type is not instance of ParameterizedType!", genericInterface.orElse(es.getClass()).getTypeName()));

                Type entityType = parametrizedType.getActualTypeArguments()[0];
                if(!(entityType instanceof Class<?>)) throw new RuntimeException(String.format("%s type is not instance of Class!", genericInterface.orElse(es.getClass()).getTypeName()));

                type = (Class<E>) entityType;
            }

            return type;
        }

        private EntityManager resolveEntityManager()
        {
            return this.em;
        }

        private static EntityManager createEntityManagerJPA(EntityManager em)
        {
            Map<String, Object> emFactoryProperties = em.getEntityManagerFactory().getProperties();
            String puName = emFactoryProperties.get(AvailableSettings.PERSISTENCE_UNIT_NAME).toString(); //hibernate.ejb.persistenceUnitName
            if(StringUtils.isNotEmpty(puName))
            {
                Map<String, Object> configOverrides = new HashMap<>(); //NOT NEEDED: new HashMap<>(emFactoryProperties);
                //configOverrides.put(AvailableSettings.SHOW_SQL, this.isLoggingEnabled());
                //configOverrides.put(AvailableSettings.CRITERIA_LITERAL_HANDLING_MODE, literalHandlingMode);
                //configOverrides.put(AvailableSettings.USE_QUERY_CACHE, true);
                //configOverrides.put(AvailableSettings.USE_SECOND_LEVEL_CACHE, true);
                //configOverrides.put(AvailableSettings.USE_STRUCTURED_CACHE, true);

                //EntityManagerFactory emf = Persistence.createEntityManagerFactory(puName);
                EntityManagerFactory emf = Persistence.createEntityManagerFactory(puName, configOverrides);
                em = emf.createEntityManager(); //retrieve an application managed entity manager

                //TODO fix: close EntityManagerFactory & EntityManager
                //em.close(); //close after work with the EM
                //emf.close(); //close at application end
            }

            return em;
        }

        /*
        //TODO: refactoring to Jakarta JPA 3.0
        public static EntityManager createEntityManagerHibernate(EntityManager em, List<Class<?>> entityClasses, LiteralHandlingMode literalHandlingMode)
        {
            Session session = em.unwrap(Session.class);
            //SessionImplementor sessionImpl = em.unwrap(SessionImplementor.class);

            Map<String, Object> emFactoryProperties = session.getSessionFactory().getProperties();

            Properties properties = new Properties();
            properties.putAll(emFactoryProperties);
            properties.setProperty(AvailableSettings.CRITERIA_LITERAL_HANDLING_MODE, literalHandlingMode.toString());
            //properties.setProperty(AvailableSettings.SHOW_SQL, String.valueOf(true));
            //properties.setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
            //properties.setProperty("hibernate.connection.url", "jdbc:mysql://localhost:3306/bookstoredb");
            //properties.setProperty("hibernate.connection.username", "root");
            //properties.setProperty("hibernate.connection.password", "password");
            //properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");

            Configuration config = new Configuration();
            config.setProperties(properties);
            for(Class<?> entityType : entityClasses)
            {
                config.addAnnotatedClass(entityType);
            }

            SessionFactory sessionFactory = config.buildSessionFactory();
            //SessionFactory sessionFactory = session.getSessionFactory();
            //sessionFactory.getProperties().putAll((Map) properties);

            //em = sessionFactory.openStatelessSession();
            em = sessionFactory.createEntityManager();

            //TODO fix: close EntityManagerFactory & EntityManager
            //em.close();
            //sessionFactory.close();

            return em;
        }
        */
    }

    public static class QueryBuilder<E>
    {
        private EntityService<E> es = null;
        private PredicateBuilder<E> pb = null;

        public QueryBuilder(EntityService<E> es, PredicateBuilder<E> pb)
        {
            this.es = es;
            this.pb = pb;
        }

        private Long countQuery(EntityManager em, CriteriaQuery<?> criteria, Class<E> clazz)
        {
            Stream<Tuple> countTuples = this.aggQuery(em, criteria, clazz, new QueryExpression(clazz));

            List<Tuple> countTuple = countTuples.collect(Collectors.toList());
            if(countTuple.size() != 1)
            {
                throw new RuntimeException("CountQuery - Entity ROW COUNT is not one-dimensional!");
            }

            return (Long) countTuple.get(0).get(0);
        }

        private Page<E> dataQuery(EntityManager em, Class<E> entity, PageRequest request)
        {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<E> query = Core.createEntityQuery(cb, entity);
            //CriteriaQuery<Tuple> query = this.createTupleQuery(cb, entity);
            Root<E> root = Core.findEntityRootPath(query.getRoots(), entity);
            if(root == null) throw new RuntimeException(String.format("DataQuery - RootPath NOT FOUND for ENTITY Class: %s!", entity.getSimpleName()));

            if(request.getQueryFilters() != null)
            {
                List<Predicate> newPredicates = this.getFilters(cb, query, entity, request.getQueryFilters());

                List<Expression> orgPredicates = (query.getRestriction() != null) ? new ArrayList(query.getRestriction().getExpressions()) : new ArrayList();
                orgPredicates.addAll(newPredicates);
                query.where(orgPredicates.toArray(new Predicate[]{}));

                //NOTICE: filters are not accessible from CriteriaQuery.getRestriction().getExpressions() nor CriteriaQuery.getGroupRestriction().getExpressions() when added as single Predicate
                //Predicate predicate = this.createPredicate(cb, query, filters);
                //if(predicate != null) query.where(predicate);
            }

            if(request.getFetchEntityPaths() != null)
            {
                this.setFetchEntityJoins(query, entity, request.getFetchEntityPaths());
            }

            query.select(root); //query.select(cb.tuple(root));

            if(request.getSorts() != null)
            {
                List<Order> orders = this.getSorting(cb, query, entity, request.getSorts());
                query.orderBy(orders.toArray(new Order[]{}));
            }

            //NOTICE: dynamically omit distinct/group-by clause if none join-to-many relations (performance optimization)
            boolean distinctRequired = Core.queryContainsJoinToMany(query);
            if(distinctRequired && request.isDistinctDataset())
            {
                //if(ArrayUtils.isNotEmpty(request.getEntityGraphPaths()) || ArrayUtils.isNotEmpty(request.getFetchEntityPaths()))
                {
                    query.distinct(request.isDistinctDataset());
                }
                //else
                //{
                //    Path<?>[] rootColumns = root.getModel().getAttributes().stream().map(a -> root.get(a.getName())).toArray(Path[]::new);
                //    query.groupBy(rootColumns); //group-by vs distinct - performance optimization (needs testing)
                //}
            }

            Page<E> page = this.pageQuery(em, query, entity, request);

            if(request.getDistinctColumns() != null) page.setDistinctValues(this.getDistinctValues(em, query, entity, request.getDistinctColumns()));
            if(request.getMetaColumns() != null) page.setMetaValues(this.getMetaValues(em, query, entity, request.getMetaColumns()));

            return page;
        }

        private Page<E> pageQuery(EntityManager em, CriteriaQuery<E> query, Class<E> entity, PageRequest request)
        {
            TypedQuery<E> dataQuery = em.createQuery(query);
            //TypedQuery<Tuple> dataQuery = em.createQuery(query);
            //Query dataQuery = sessionImpl.createSQLQuery(jpqlQuery);

            if(request.getPageSize() != null)
            {
                if(request.getPageNumber() != null) dataQuery.setFirstResult((request.getPageNumber() - 1) * request.getPageSize());
                dataQuery.setMaxResults(request.getPageSize());
            }

            //MSSQL DISTINCT+SORT technical-limitation: sorting by non-select fields is not allowed when using DISTINCT clause
            //ERROR: ORDER BY items must appear in the select list if SELECT DISTINCT is specified
            //WORKAROUND: add Sort-Field-Entity to Entity-Graph select list (eager load sort-entity with root-select query)
            if(query.isDistinct())
            {
                this.extendEntityGraphBySortEntities(request);
            }

            Map.Entry<String, Object> entityGraphHint = QueryBuilder.getEntityGraphHint(em, entity, request.getEntityGraphPaths());
            if(entityGraphHint != null)
            {
                dataQuery.setHint(entityGraphHint.getKey(), entityGraphHint.getValue());
            }

            dataQuery.setHint(QueryHints.HINT_READONLY, request.isReadOnlyDataset());

            long queryStartTimeNs = System.nanoTime();
            List<E> contentResult = dataQuery.getResultList();
            //List<Tuple> result = dataQuery.getResultList();
            //List<E> contentResult = result.stream().map(e -> (T) e.get(0)).collect(Collectors.toList());

            if(es.isLoggingEnabled())
            {
                EsUtil.logQuery(QueryType.DATA, dataQuery, queryStartTimeNs);
            }

            long count = (request.getPageSize() != null) ? this.countQuery(em, query, entity) : contentResult.size();

            return new Page<>(request, count, contentResult);
        }

        private Stream<Tuple> aggQuery(EntityManager em, CriteriaQuery<?> query, Class<E> clazz, QueryExpression metaVQ)
        {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Tuple> aggQuery = Core.replicateTupleQuery(cb, query);

            //TODO: check if any operations with non-number columns
            Path<? extends Number> valueColumn = Core.findOrGenerateFieldJoinPath(aggQuery.getRoots(), clazz, metaVQ.getEntity(), metaVQ.getValueColumnPath());
            //Path<?> labelColumn = this.findOrGenerateFieldJoinPath(aggQuery.getRoots(), clazz, metaVQ.getEntity(), metaVQ.getLabelColumnPath());

            Path[] labelColumns = null;
            Expression<String> labelColumn = null;
            if(StringUtils.isNotBlank(metaVQ.getLabelColumnPath()))
            {
                String[] labelColumnsPaths = metaVQ.getLabelColumnPath().split(EntityService.LABEL_PATHS_SEPARATOR);
                labelColumns = Arrays.stream(labelColumnsPaths).map(l -> Core.findOrGenerateFieldJoinPath(aggQuery.getRoots(), clazz, metaVQ.getEntity(), l.trim())).toArray(Path[]::new);
                //labelColumn = Arrays.stream(labelColumns).map(l -> (Expression<String>) l).reduce(cb::concat).orElse(null);
                //.reduce(cb.literal(StringUtils.EMPTY),                                      //identity
                //(concat, nextLabel) -> cb.concat(concat, (Expression<String>) nextLabel),   //accumulator
                //(concat, nextLabel) -> cb.concat(concat, nextLabel));                       //combiner
            }

            Expression<?>[] selectColumns = null;
            if(valueColumn != null)
            {
                QueryExpression.Func function = metaVQ.getFunc();
                if(function == null) throw new RuntimeException(String.format("AggQuery (%s) - QueryExpression.Func must NOT be null!", EsUtil.getMetaValueKey(metaVQ)));
                switch(function)
                {
                    case COUNT:
                    {
                        selectColumns = new Expression[] { cb.count(valueColumn) };
                        if(labelColumns != null) selectColumns = ArrayUtils.addAll(selectColumns, labelColumns);
                        break;
                    }

                    case COUNT_DISTINCT:
                    {
                        selectColumns = new Expression[] { cb.countDistinct(valueColumn) };
                        if(labelColumns != null) selectColumns = ArrayUtils.addAll(selectColumns, labelColumns);
                        break;
                    }

                    case SUM:
                    {
                        selectColumns = new Expression[] { cb.sum(valueColumn) };
                        if(labelColumns != null) selectColumns = ArrayUtils.addAll(selectColumns, labelColumns);
                        break;
                    }

                    case AVG:
                    {
                        selectColumns = new Expression[] { cb.avg(valueColumn), cb.count(valueColumn) };
                        if(labelColumns != null) selectColumns = ArrayUtils.addAll(selectColumns, labelColumns);
                        break;
                    }

                    case MIN:
                    {
                        selectColumns = new Expression[] { cb.min(valueColumn) };
                        if(labelColumns != null) selectColumns = ArrayUtils.addAll(selectColumns, labelColumns);
                        break;
                    }

                    case MAX:
                    {
                        selectColumns = new Expression[] { cb.max(valueColumn) };
                        if(labelColumns != null) selectColumns = ArrayUtils.addAll(selectColumns, labelColumns);
                        break;
                    }
                }
            }
            else    //entity-count
            {
                String joinPathString = Helper.getPathWithoutLastItem(metaVQ.getValueColumnPath());
                Path<?> path = Core.findFromPath(aggQuery.getRoots(), metaVQ.getEntity(), joinPathString);
                if(path == null) throw new RuntimeException(String.format("AggQuery - Path NOT FOUND for ENTITY Class: %s!", metaVQ.getEntity().getSimpleName()));
                selectColumns = new Expression[] { cb.countDistinct(path) };
            }

            aggQuery.select(cb.tuple(selectColumns));   //SELECT selectColumns from switch-case/entity-count
            aggQuery.distinct(metaVQ.isDistinct());
            aggQuery.orderBy(); //clear order-by from copied query

            Predicate orgPredicate = aggQuery.getRestriction();
            if(ArrayUtils.isNotEmpty(metaVQ.getFilters()))
            {
                Predicate p = pb.createPredicate(cb, aggQuery, clazz, QueryExpression.Conditional.Operator.AND, metaVQ.getFilters());
                orgPredicate = PredicateUtil.combinePredicates(cb, orgPredicate, p, QueryExpression.Conditional.Operator.AND);
            }

            if(orgPredicate != null)
            {
                aggQuery.where(orgPredicate);
            }

            //TODO verify: incorrect aggregation value (duplicate root Entity values with one-to-many relations)?
            if(labelColumns != null)
            {
                aggQuery.groupBy(labelColumns);
            }

            TypedQuery<Tuple> aggregationQuery = em.createQuery(aggQuery);
            aggregationQuery.setHint(QueryHints.HINT_READONLY, true);

            long queryStartTimeNs = System.nanoTime();
            Stream<Tuple> aggResult = aggregationQuery.getResultStream();

            if(es.isLoggingEnabled())
            {
                EsUtil.logQuery(QueryType.AGGREGATION, aggregationQuery, queryStartTimeNs);
            }

            return aggResult;
        }

        private Stream<Tuple> distinctQuery(EntityManager em, CriteriaQuery<?> query, Class<E> clazz, QueryExpression metaVQ)
        {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Tuple> distQuery = Core.replicateTupleQuery(cb, query);
            //Path<?> path = Core.findFromPath(distinctQuery.getRoots(), metaVQ.getEntity());
            //if(path == null) throw new RuntimeException(String.format("DistinctQuery - Path NOT FOUND for ENTITY Class: %s!", metaVQ.getEntity().getSimpleName()));

            Path<?> valueColumn = Core.findOrGenerateFieldJoinPath(distQuery.getRoots(), clazz, metaVQ.getEntity(), metaVQ.getValueColumnPath());
            Path<?> labelColumn = Core.findOrGenerateFieldJoinPath(distQuery.getRoots(), clazz, metaVQ.getEntity(), metaVQ.getLabelColumnPath());

            Expression<?>[] selectColumns = (labelColumn != null) ? new Expression[] { valueColumn, labelColumn } : new Expression[] { valueColumn };

            distQuery.select(cb.tuple(selectColumns));
            distQuery.distinct(true);
            distQuery.orderBy(); //clear order-by from copied query

            Predicate orgPredicate = distQuery.getRestriction();
            if(ArrayUtils.isNotEmpty(metaVQ.getFilters()))
            {
                Predicate p = pb.createPredicate(cb, distQuery, clazz, QueryExpression.Conditional.Operator.AND, metaVQ.getFilters());
                orgPredicate = PredicateUtil.combinePredicates(cb, orgPredicate, p, QueryExpression.Conditional.Operator.AND);
            }

            if(orgPredicate != null)
            {
                distQuery.where(orgPredicate);
            }

            TypedQuery<Tuple> distinctQuery = em.createQuery(distQuery);
            distinctQuery.setHint(QueryHints.HINT_READONLY, true);

            long queryStartTimeNs = System.nanoTime();
            Stream<Tuple> distinctResult = distinctQuery.getResultStream();

            if(es.isLoggingEnabled())
            {
                EsUtil.logQuery(QueryType.DISTINCT, distinctQuery, queryStartTimeNs);
            }

            return distinctResult;
        }

        private <E> List<Order> getSorting(CriteriaBuilder cb, CriteriaQuery<?> query, Class<E> clazz, Set<PageRequest.Sort> sorts)
        {
            return sorts.stream()
                    .map(s ->
                    {
                        //Expression<?> sortColumn = Core.generateFieldPath(query.getRoots(), clazz, s.getColumnPath());
                        Expression<?> sortColumn = Core.findOrGenerateFieldJoinPath(query.getRoots(), clazz, s.getEntity(), s.getColumnPath());
                        if(s.getSortType() != null) sortColumn = sortColumn.as(s.getSortType());

                        switch(s.getDirection())
                        {
                            case ASC: return cb.asc(sortColumn);
                            case DESC: return cb.desc(sortColumn);
                            default: throw new RuntimeException(String.format("Unknown Sort direction: %s!", s.getDirection()));
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedList::new));
        }

        private void setFetchEntityJoins(CriteriaQuery<E> query, Class<E> entity, String[] fetchEntityPaths)
        {
            for(String fetchEntityPath : fetchEntityPaths)
            {
                Map.Entry<Class<?>, String> entityField = ReflectionHelper.findEntityFieldByPath(entity, fetchEntityPath, true);

                Class<?> parentEntity = entityField.getKey();
                LinkedList<String> entityPaths = ReflectionHelper.pathToLinkedList(fetchEntityPath);
                From<?,?> parentPath = Core.findOrGenerateJoinPath(query.getRoots(), entity, parentEntity, entityPaths);
                Core.addLeftFetch(parentPath, entityPaths.getLast());
            }
        }

        private static <E> Map.Entry<String, Object> getEntityGraphHint(EntityManager em, Class<E> entity, String[] getEntityGraphPaths)
        {
            Map.Entry<String, Object> entityGraphHint = null;

            if(ArrayUtils.isNotEmpty(getEntityGraphPaths))
            {
                Map<String, Subgraph<?>> subgraphMap = new HashMap<>();
                EntityGraph<E> rootGraph = em.createEntityGraph(entity);

                for(String entityGraphPath : getEntityGraphPaths)
                {
                    LinkedList<String> entityGraphPaths = ReflectionHelper.pathToLinkedList(entityGraphPath);

                    Iterator<String> pathIterator = entityGraphPaths.iterator();
                    //Optional<AttributeNode> pathNode = ((List<AttributeNode>) rootGraph.getAttributeNodes()).stream().filter(n -> nextPaht.equals(n.getAttributeName())).findAny();

                    String rootPath = pathIterator.next();
                    Subgraph<?> parentGraph = subgraphMap.get(rootPath);
                    if(parentGraph == null)
                    {
                        parentGraph = rootGraph.addSubgraph(rootPath);
                        subgraphMap.put(rootPath, parentGraph);
                    }

                    while(pathIterator.hasNext())
                    {
                        String nextPath = pathIterator.next();
                        rootPath += ReflectionHelper.PATH_SEPARATOR + nextPath;

                        Subgraph<?> currentGraph = subgraphMap.get(rootPath);
                        if(currentGraph == null)
                        {
                            currentGraph = parentGraph.addSubgraph(nextPath);
                            subgraphMap.put(rootPath, currentGraph);
                        }

                        parentGraph = currentGraph;
                    }
                }

                //NOTICE: https://stackoverflow.com/questions/46455325/whats-the-difference-between-fetchgraph-and-loadgraph-in-jpa-2-1
                //entityGraphHint = new AbstractMap.SimpleEntry<>(QueryHints.HINT_FETCHGRAPH, rootGraph); //javax.persistence.fetchgraph
                entityGraphHint = new AbstractMap.SimpleEntry<>(QueryHints.HINT_LOADGRAPH, rootGraph); //javax.persistence.loadgraph
                subgraphMap.clear(); //free-up memory
            }

            return entityGraphHint;
        }

        private void extendEntityGraphBySortEntities(PageRequest request)
        {
            List<String> entityGraphPaths = new ArrayList<>(Arrays.asList(ArrayUtils.nullToEmpty(request.getEntityGraphPaths())));

            for(PageRequest.Sort sort : CollectionUtils.emptyIfNull(request.getSorts()))
            {
                String sortEntityPath = Helper.getPathWithoutLastItem(sort.getColumnPath());
                if(StringUtils.isNotBlank(sortEntityPath) && !entityGraphPaths.contains(sortEntityPath)) entityGraphPaths.add(sortEntityPath);
            }

            if(CollectionUtils.isNotEmpty(entityGraphPaths)) request.setEntityGraphPaths(entityGraphPaths.toArray(new String[]{}));
        }

        private List<Predicate> getFilters(CriteriaBuilder cb, CriteriaQuery<?> query, Class<E> clazz, QueryExpression.Conditional queryFilters)
        {
            List<Predicate> predicates = new ArrayList<>();

            Predicate complexConditionalPredicate = pb.createPredicate(cb, query, clazz, QueryExpression.Conditional.Operator.AND, queryFilters);
            if(complexConditionalPredicate != null) predicates.add(complexConditionalPredicate);

            return predicates;
        }

        private Map<String, Map<Object, Object>> getDistinctValues(EntityManager em, CriteriaQuery<?> query, Class<E> clazz, Set<QueryExpression> distinctColumns)
        {
            Map<String, Map<Object, Object>> distinctValues = new HashMap<>();

            for(QueryExpression dtc : distinctColumns)
            {
                //logger.info(String.format("%s - ADD new DistinctValue: %s", EntityService.class.getSimpleName(), dtc.toString()));
                Stream<Tuple> columnDistincts = this.distinctQuery(em, query, clazz, dtc);

                Map<Object, Object> distinctPairs = columnDistincts.map(d ->
                {
                    //WARNING BUG: jakarta.persistence.Tuple.getElements().size() throws ArrayIndexOutOfBoundsException
                    int size = d.toArray().length;

                    switch(size)
                    {
                        case 1: return new AbstractMap.SimpleEntry(d.get(0), d.get(0));
                        case 2: return new AbstractMap.SimpleEntry(d.get(0), d.get(1));
                        default: throw new RuntimeException(String.format("DistinctQuery returned %d results!", size));
                    }
                })
                .filter(dp -> dp != null && dp.getKey() != null && dp.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (dp1, dp2) ->
                {
                    if(Objects.equals(dp1, dp2)) return dp1;
                    else return dp1 +"/"+ dp2; //throw new RuntimeException(String.format("DistinctQuery result contains duplicate key for values '%s' <> '%s'!", dp1, dp2));
                }));

                String key = EsUtil.getDistinctValueKey(dtc);
                if(distinctValues.containsKey(key)) throw new RuntimeException(String.format("DistinctValues '%s' key is duplicate!", key));
                distinctValues.put(key, distinctPairs);
            }

            return distinctValues;
        }

        private Map<String, Map<Object, Object>> getMetaValues(EntityManager em, CriteriaQuery<?> query, Class<E> clazz, Set<QueryExpression> metaColumns)
        {
            Map<String, Map<Object, Object>> metaValues = new HashMap<>();

            for(QueryExpression agg : metaColumns)
            {
                //logger.info(String.format("%s - ADD new MetaValue: %s", EntityService.class.getSimpleName(), agg.toString()));
                Stream<Tuple> columnAggs = this.aggQuery(em, query, clazz, agg);

                Map<Object, Object> aggValues = columnAggs.map(a ->
                {
                    int labelStartIndex = (agg.getFunc() == QueryExpression.Func.AVG) ? 2 : 1;
                    String labelsConcat = (a.getElements().size() > labelStartIndex) ? a.getElements().subList(labelStartIndex, a.getElements().size()).stream().map(te -> String.valueOf(a.get(te))).collect(Collectors.joining()) : "value";

                    Number valueCount = (agg.getFunc() == QueryExpression.Func.AVG) ? (Number) a.get(1) : 1;
                    return new AbstractMap.SimpleEntry<String, Map.Entry<Number, Number>>(labelsConcat, new AbstractMap.SimpleEntry<>((Number) a.get(0), valueCount));
                })
                .filter(av -> av.getValue().getKey() != null && av.getValue().getValue() != null)
                //.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getKey()));
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList()))) //grouping of duplicate keys
                .entrySet().stream().map(av ->  //Map.Entry(String, Map.Entry(AggValue, AggValueCount))
                {
                    Number aggValue = av.getValue().get(0).getKey();    //set single key agg-value

                    if(av.getValue().size() > 1) //resolve multi key agg-values
                    {
                        if(es.isLoggingEnabled())
                        {
                            logger.warning(String.format("Meta value with key '%s' contains multiple values '%s' - merging values using %s function!", av.getKey(), av.getValue(), agg.getFunc()));
                        }

                        double aggTotalValueCount = av.getValue().stream().mapToInt(v -> v.getValue().intValue()).reduce(0, Integer::sum);
                        switch(agg.getFunc())
                        {
                            case COUNT:
                            case COUNT_DISTINCT: aggValue = av.getValue().stream().mapToInt(v -> v.getKey().intValue()).reduce(0, Integer::sum); break;
                            case SUM: aggValue = av.getValue().stream().mapToDouble(v -> v.getKey().doubleValue()).mapToObj(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add); break;

                            //NOTICE: FIXED accuracy BUG with duplicate AVG keys using weighted-averages: (6+2+1+1+1+2)/6 != (4+3+2)/3 -> (6+2+1+1+1+2)/6 == 4*(2/6)+3*(3/6)+2*(1/6) -> 2.16 == 0.33+1.5+0.33
                            case AVG: aggValue = av.getValue().stream().mapToDouble(v -> v.getKey().doubleValue()*(v.getValue().doubleValue()/aggTotalValueCount)).mapToObj(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add); break;

                            //WARNING: fictitious average with accuracy problem
                            //case AVG: aggValue = av.getValue().stream().mapToDouble(v -> v.getKey().doubleValue()).mapToObj(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(av.getValue().size())); break;

                            case MIN: aggValue = av.getValue().stream().mapToDouble(v -> v.getKey().doubleValue()).mapToObj(BigDecimal::new).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO); break; //.reduce((x,y) -> (x.compareTo(y) <= 0) ? x : y);
                            case MAX: aggValue = av.getValue().stream().mapToDouble(v -> v.getKey().doubleValue()).mapToObj(BigDecimal::new).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO); break; //.reduce((x,y) -> (x.compareTo(y) <= 0) ? y : x);
                        }
                    }

                    return new AbstractMap.SimpleEntry<>(av.getKey(), aggValue);
                })
                .filter(av -> av.getKey() != null && av.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                String key = EsUtil.getMetaValueKey(agg);
                if(metaValues.containsKey(key)) throw new RuntimeException(String.format("MetaValues '%s' key is duplicate!", key));
                metaValues.put(key, aggValues);
            }

            return metaValues;
        }
    }

    public static class PredicateBuilder<E>
    {
        private EntityService<E> es = null;
        private Base<E> base = null;

        public PredicateBuilder(EntityService<E> es, Base<E> base)
        {
            this.es = es;
            this.base = base;
        }

        private <T> Predicate createPredicate(CriteriaBuilder cb, CriteriaQuery<?> query, Class<T> rootEntity, QueryExpression.Conditional.Operator operator, QueryExpression.Conditional... condFilters)
        {
            Predicate condPredicate = null;

            for(QueryExpression.Conditional condFilter : condFilters)
            {
                QueryExpression.Conditional.Operator condOperator = condFilter.getLogicOperator();

                Predicate simpleFilters = this.createPredicate(cb, query, rootEntity, condOperator, condFilter.getFilters().toArray(new QueryExpression.Filter[]{}));
                Predicate complexSubFilters = this.createPredicate(cb, query, rootEntity, condOperator, condFilter.getConditionals().toArray(new QueryExpression.Conditional[]{}));
                Predicate complexPredicate = PredicateUtil.combinePredicates(cb, simpleFilters, complexSubFilters, condOperator);

                condPredicate = PredicateUtil.combinePredicates(cb, condPredicate, complexPredicate, operator);
            }

            return condPredicate;
        }

        private <T extends Comparable<? super T>> Predicate createPredicate(CriteriaBuilder cb, CriteriaQuery<?> query, Class<?> rootEntity, QueryExpression.Conditional.Operator operator, QueryExpression.Filter<?>... filters)
        {
            Predicate filterPredicate = null;

            for(QueryExpression.Filter f : filters)
            {
                Predicate p = this.createPredicate(cb, query, rootEntity, f);
                filterPredicate = PredicateUtil.combinePredicates(cb, filterPredicate, p, operator);
            }

            return filterPredicate;
        }

        private <T extends Comparable<? super T>> Predicate createPredicate(CriteriaBuilder cb, CriteriaQuery<?> query, Class<?> rootEntity, QueryExpression.Filter<T> filter)
        {
            Predicate filterPredicate = null;

            //logger.info(String.format("%s - ADD new Filter: %s", EntityService.class.getSimpleName(), filter.toString()));
            //String entityPathString = this.removeLastItemFromPath(filter.getColumnPath());
            //Path<?> path = this.findFromPath(query.getRoots(), filter.getEntity(), entityPathString);
            //Predicate p = this.createPredicateFromEntityPath(cb, path, filter);

            Path<T> fieldPath = null;
            if(QueryExpression.Filter.subqueryComparators.contains(filter.getCmp()))
            {
                String filterEntityPath = Helper.getPathWithoutLastItem(filter.getColumnPath());
                LinkedList<String> filterEntityPaths = ReflectionHelper.pathToLinkedList(filterEntityPath);
                if(filterEntityPaths.isEmpty()) throw new RuntimeException(String.format("Root entity fields not allowed with subquery comparators: %s!", QueryExpression.Filter.subqueryComparators.toString()));

                //NOTICE: auto-join only until Subquery root entity
                From<?,?> parentEntityPath = Core.findOrGenerateJoinPath(query.getRoots(), rootEntity, null, filterEntityPaths);

                CriteriaQuery<?> fictionalQuery = cb.createQuery(parentEntityPath.getJavaType()); //query.subquery(parentEntityPath.getJavaType());
                Root<?> filterEntityParentRoot = fictionalQuery.from(parentEntityPath.getJavaType());
                Path<?> filterEntityJoinPath = filterEntityParentRoot.join(ReflectionHelper.getFieldNameFromPath(filterEntityPath));

                String columnName = ReflectionHelper.getFieldNameFromPath(filter.getColumnPath());
                fieldPath = filterEntityJoinPath.get(columnName); //NOTICE: "fictional" fieldPath

                //String filterEntityField = ReflectionHelper.getFieldNameFromPath(filterEntityPath);
                //String filterField = ReflectionHelper.getFieldNameFromPath(filter.getColumnPath());
                //fieldPath = parentEntityPath.get(filterEntityField).get(filterField);
                //fieldPath = (Path<T>) Core.generateFieldPath(((Join<?,?>) parentEntityPath).getParent().getJoins(), parentEntityPath.getJavaType(), String.join(ReflectionHelper.PATH_SEPARATOR, filterEntityField, filterField));
                //fieldPath = (Path<T>) parentEntityPath;
            }
            else
            {
                //fieldPath = Core.generateFieldPath(query.getRoots(), rootEntity, filter.getColumnPath());                                 //field-path with auto cross-join
                fieldPath = Core.findOrGenerateFieldJoinPath(cb, query, rootEntity, filter.getEntity(), filter.getColumnPath());            //field-path with left-joins (cross-join allowed)
                //fieldPath = Core.findOrGenerateFieldJoinPath(query.getRoots(), rootEntity, filter.getEntity(), filter.getColumnPath());   //field-path with left-joins (cross-join not allowed)
            }

            //filterPredicate = PredicateBuilder.createPredicate(cb, query fieldPath, filter);
            filterPredicate = this.createFilterPredicate(cb, query, fieldPath, filter); //create predicate using AttributeMapper resolver

            return filterPredicate;
        }

        /**
         * Method resolves & updates Filter.values for applied EntityService.AttributeMapper
         * @param filter
         * @param <T>
         * @return
         */
        private <T extends Comparable<? super T>> Predicate createFilterPredicate(CriteriaBuilder cb, CriteriaQuery<?> query, Path<? extends T> filterColumn, QueryExpression.Filter<T> filter)
        {
            Predicate filterPredicate = null;

            //AbstractPathImpl.class replaced with AbstractSqmSimplePath.class (since Hibernate 6.2.4.Final or earlier)
            String fieldName = (AbstractPathImpl.class.isAssignableFrom(filterColumn.getClass())) ? ((AbstractPathImpl) filterColumn).getAttribute().getName() : ReflectionHelper.getFieldNameFromPath(filter.getColumnPath());

            Optional<Field> fieldOptional = ReflectionHelper.findModelField(filterColumn.getParentPath().getJavaType(), fieldName);
            if(!fieldOptional.isPresent()) throw new RuntimeException(String.format("Field '%s' NOT FOUND in ENTITY Class: %s!", fieldName, filterColumn.getParentPath().getJavaType().getSimpleName()));

            Convert convertAnnot = ReflectionHelper.getFieldOrAccessorOrMutatorAnnotation(filterColumn.getParentPath().getJavaType(), fieldOptional.get(), Convert.class);
            if(convertAnnot != null && AttributeMapper.class.isAssignableFrom(convertAnnot.converter()) && !convertAnnot.disableConversion())
            {
                Class<?> converterClazz = convertAnnot.converter();
                if(es.isLoggingEnabled()) logger.info(String.format("Converting filter to its mapped DB values: %s", filter));

                try
                {
                    Constructor<?> ctor = converterClazz.getConstructor();
                    AttributeMapper<T, ?> attributeMapper = (AttributeMapper) ctor.newInstance();
                    T defaultValue = attributeMapper.getDefaultValue(); //TODO: check if not a problem with AttributeMapper.DefaultValue = NULL

                    for(T val : filter.getValue())
                    {
                        if(val instanceof String) val = (T) Helper.parseValue(attributeMapper.getEntityType(), (String) val);

                        if(val == defaultValue || (val instanceof String && ((String) val).equalsIgnoreCase(String.valueOf(defaultValue))))
                        {
                            //T[] allFilterMappedValues = (T[]) Arrays.stream(filter.getValue()).filter(v -> v == val).map(v -> attributeMapper.getMappedValues(v)).flatMap(Arrays::stream).toArray();
                            //List<T> allFilterMappedValues = (List<T>) Arrays.stream(filter.getValue()).filter(v -> v == val).map(v -> Arrays.asList(attributeMapper.getMappedValues(v))).flatMap(Collection::stream).collect(Collectors.toList());
                            //List<T> allOtherMappedValues = (List<T>) Arrays.asList(attributeMapper.getAllMappedValues());
                            //allOtherMappedValues.removeAll(allFilterMappedValues);
                            //T[] allOtherMappedValuesArray = Helper.castValues(filterColumn.getJavaType(), allOtherMappedValues.toArray());

                            //QueryExpression.Filter<T> defaultFilter = new QueryExpression.Filter(filter.getEntity(), filter.getColumnPath(), filter.getCmp(), filter.getMod(), attributeMapper.convertToDatabaseValue(attributeMapper.getDefaultValue()));
                            //if(this.isLoggingEnabled()) logger.info(String.format("Converted value '%s' to value: %s", val, defaultFilter));
                            QueryExpression.Filter<T> allMappedFilter = new QueryExpression.Filter(filter.getEntity(), filter.getColumnPath(), filter.getCmp(), filter.getMod(), attributeMapper.getAllMappedValues());
                            if(es.isLoggingEnabled()) logger.info(String.format("Converted value '%s' to NOT values: %s", val, allMappedFilter));

                            //Predicate pDefault = this.createPredicate(cb, query, filterColumn, defaultFilter);
                            Predicate pNotMapped = this.createPredicate(cb, query, filterColumn, allMappedFilter).not();
                            //Predicate pDefaultOrNotMapped = cb.or(pDefault, pNotMapped);
                            filterPredicate = PredicateUtil.combinePredicates(cb, filterPredicate, pNotMapped, QueryExpression.Conditional.Operator.OR);
                        }
                        else
                        {
                            QueryExpression.Filter<T> mappedFilter = new QueryExpression.Filter(filter.getEntity(), filter.getColumnPath(), filter.getCmp(), filter.getMod(), attributeMapper.mapToDatabaseValues(val));
                            if(es.isLoggingEnabled()) logger.info(String.format("Converted value '%s' to values: %s", val, mappedFilter));

                            Predicate pMapped = this.createPredicate(cb, query, filterColumn, mappedFilter);
                            filterPredicate = PredicateUtil.combinePredicates(cb, filterPredicate, pMapped, QueryExpression.Conditional.Operator.OR);
                        }
                    }
                }
                catch(NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException ex)
                {
                    filterPredicate = this.createPredicate(cb, query, filterColumn, filter);
                    logger.log(Level.WARNING, String.format("Could not obtain instance of %s class - using original filter: %s", converterClazz.getSimpleName(), filter), ex);
                }
            }
            else    //NOTICE: default flow (fields without custom mapper)
            {
                filterPredicate = this.createPredicate(cb, query, filterColumn, filter);
            }

            return filterPredicate;
        }

        private <T extends Comparable<? super T>> Predicate createPredicateFromEntityPath(CriteriaBuilder cb, CriteriaQuery<?> query, Path<?> entityPath, QueryExpression.Filter<T> filter)
        {
            String columnName = ReflectionHelper.getFieldNameFromPath(filter.getColumnPath());
            Path<? extends T> filterColumn = entityPath.get(columnName);

            return this.createPredicate(cb, query, filterColumn, filter);
        }

        private <T extends Comparable<? super T>> Predicate createPredicate(CriteriaBuilder cb, CriteriaQuery<?> query, Path<? extends T> filterColumn, QueryExpression.Filter<T> filter)
        {
            Predicate p = null;

            //Parameter<Comparable> columnParameter = cb.parameter(Comparable.class);
            QueryExpression.Filter.Cmp comparator = filter.getCmp();
            if(comparator == null) throw new RuntimeException(String.format("Filter (%s) - QueryExpression.Filter.Cmp must NOT be null!", filter.toString()));

            Expression<? extends T> cmpFilterColumn = filterColumn;
            Subquery<Long> subquery = null;

            //convert filter column to comparison type
            if(QueryExpression.Filter.booleanComparators.contains(comparator))
            {
                cmpFilterColumn = (Expression<? extends T>) filterColumn.as(Boolean.class);
            }
            else if(comparator == QueryExpression.Filter.Cmp.LIKE)
            {
                cmpFilterColumn = (Expression<? extends T>) filterColumn.as(String.class);
            }
            else if(QueryExpression.Filter.subqueryComparators.contains(comparator))
            {
                //NOTICE: filterColumn is "fictional-query" Path on subqueryComparators types (consistency & value cast/parse purpose only)
                //NOTICE: filterColumn is replaced with "correct-subquery" Path in generateCountSubquery() method
                Map.Entry<Subquery<Long>, Path<? extends T>> subqueryPathEntry = this.generateCountSubquery(cb, query, filterColumn, filter);
                subquery = subqueryPathEntry.getKey();
                cmpFilterColumn = subqueryPathEntry.getValue();
            }

            //convert filter values to comparison type
            if(QueryExpression.Filter.valueComparators.contains(comparator))    //TODO: add subqueryComparators to valueComparators in order to parse values
            {
                if(filter.getMod() != null) filter.setValue(EsUtil.getModValues(filter));

                Class<?> cmpFilterType = cmpFilterColumn.getJavaType();
                //NOTICE: do NOT parse if filter Field is @Convert (parse to existing value type)
                //if(skipValueParsing) cmpFilterType = filter.getValue().getClass().getComponentType();
                Comparable<?>[] values = EsUtil.convertValuesToComparable(cmpFilterType, filter.getValue());
                //values = this.castValues(cmpFilterColumn.getJavaType(), values);

                filter.setValue((T[]) values);
            }

            switch(comparator)
            {
                case TRUE:
                {
                    p = cb.isTrue((Expression<Boolean>) cmpFilterColumn);
                    break;
                }

                case FALSE:
                {
                    p = cb.isFalse((Expression<Boolean>) cmpFilterColumn);
                    break;
                }

                case IsNULL:
                {
                    p = cb.isNull(cmpFilterColumn);
                    break;
                }

                case IsNotNULL:
                {
                    p = cb.isNotNull(cmpFilterColumn);
                    break;
                }

                case IsEMPTY:
                {
                    p = cb.isEmpty((Expression<? extends Collection<T>>) cmpFilterColumn);
                    break;
                }

                case IsNotEMPTY:
                {
                    p = cb.isNotEmpty((Expression<? extends Collection<T>>) cmpFilterColumn);
                    break;
                }

                case EQ:
                {
                    Predicate eq = null;
                    for(Comparable<?> val : filter.getValue())
                    {
                        if(eq == null) eq = cb.equal(cmpFilterColumn, val);
                        else eq = cb.or(eq, cb.equal(cmpFilterColumn, val));
                    }
                    p = eq;
                    break;
                }

                case NEQ:
                {
                    Predicate neq = null;
                    for(Comparable<?> val : filter.getValue())
                    {
                        if(neq == null) neq = cb.notEqual(cmpFilterColumn, val);
                        else neq = cb.or(neq, cb.notEqual(cmpFilterColumn, val));
                    }
                    //NOTICE: non-existing join value could not be resolved with not-equals (results as false)
                    //SOLUTION: non-existing left-join OR not-equals value
                    p = cb.or(cb.isNull(filterColumn.getParentPath()), neq);
                    break;
                }

                case LIKE:
                {
                    Predicate like = null;
                    for(Comparable<?> val : filter.getValue())
                    {
                        if(like == null) like = cb.like((Expression<String>) cmpFilterColumn, String.valueOf(val));
                        else like = cb.or(like, cb.like((Expression<String>) cmpFilterColumn, String.valueOf(val)));
                    }
                    p = like;
                    break;
                }

                case GT:
                {
                    Predicate gt = null;
                    for(Comparable<?> val : filter.getValue())
                    {
                        if(gt == null) gt = cb.greaterThan(cmpFilterColumn, (T) val);
                        else gt = cb.or(gt, cb.greaterThan(cmpFilterColumn, (T) val));
                    }
                    p = gt;
                    break;
                }

                case GToE:
                {
                    Predicate gte = null;
                    for(Comparable<?> val : filter.getValue())
                    {
                        if(gte == null) gte = cb.greaterThanOrEqualTo(cmpFilterColumn, (T) val);
                        else gte = cb.or(gte, cb.greaterThanOrEqualTo(cmpFilterColumn, (T) val));
                    }
                    p = gte;
                    break;
                }

                case LT:
                {
                    Predicate lt = null;
                    for(Comparable<?> val : filter.getValue())
                    {
                        if(lt == null) lt = cb.lessThan(cmpFilterColumn, (T) val);
                        else lt = cb.or(lt, cb.lessThan(cmpFilterColumn, (T) val));
                    }
                    p = lt;
                    break;
                }

                case LToE:
                {
                    Predicate lte = null;
                    for(Comparable<?> val : filter.getValue())
                    {
                        if(lte == null) lte = cb.lessThanOrEqualTo(cmpFilterColumn, (T) val);
                        else lte = cb.or(lte, cb.lessThanOrEqualTo(cmpFilterColumn, (T) val));
                    }
                    p = lte;
                    break;
                }

                case IN:
                {
                    if(ArrayUtils.isNotEmpty(filter.getValue()))
                    {
                        CriteriaBuilder.In<Comparable<?>> in = cb.in(cmpFilterColumn);
                        Arrays.stream(filter.getValue()).forEach(in::value);
                        p = in;
                    }
                    else
                    {
                        //NOTICE: empty IN will fail as SQL syntax error
                        //SOLUTION: always false statement
                        p = cb.isNull(Core.findEntityRootPath(query.getRoots(), es.getEntityClass()));
                    }
                    break;
                }

                case NotIN:
                {
                    if(ArrayUtils.isNotEmpty(filter.getValue()))
                    {
                        CriteriaBuilder.In<Comparable<?>> notIn = cb.in(cmpFilterColumn);
                        Arrays.stream(filter.getValue()).forEach(notIn::value);
                        p = notIn.not();
                    }
                    else
                    {
                        //NOTICE: empty NOT-IN will fail as SQL syntax error
                        //SOLUTION: always true statement
                        p = cb.isNotNull(Core.findEntityRootPath(query.getRoots(), es.getEntityClass()));
                    }
                    break;
                }

                case EACH: //every/each listed
                //case EVERY:
                {
                    CriteriaBuilder.In<Comparable<?>> subqueryIn = cb.in(cmpFilterColumn);
                    Arrays.stream(filter.getValue()).forEach(subqueryIn::value);

                    List<Expression<?>> subqueryPredicates = new ArrayList<>(subquery.getRestriction().getExpressions());
                    subqueryPredicates.add(subqueryIn);
                    subquery.where(subqueryPredicates.toArray(new Predicate[]{}));

                    p = cb.equal(subquery, cb.literal(filter.getValue().length));
                    break;
                }

                case NotEACH: //none listed
                //case NotEVERY:
                {
                    CriteriaBuilder.In<Comparable<?>> subqueryIn = cb.in(cmpFilterColumn);
                    Arrays.stream(filter.getValue()).forEach(subqueryIn::value);

                    List<Expression<?>> subqueryPredicates = new ArrayList<>(subquery.getRestriction().getExpressions());
                    subqueryPredicates.add(subqueryIn);
                    subquery.where(subqueryPredicates.toArray(new Predicate[]{}));

                    p = cb.equal(subquery, cb.literal(0L));
                    break;
                }

                case EXCEPT: //any except listed (at least one)
                //case AnyEXCEPT:
                //case SomeEXCEPT:
                {
                    CriteriaBuilder.In<Comparable<?>> subqueryIn = cb.in(cmpFilterColumn);
                    Arrays.stream(filter.getValue()).forEach(subqueryIn::value);

                    List<Expression<?>> subqueryPredicates = new ArrayList<>(subquery.getRestriction().getExpressions());
                    subqueryPredicates.add(subqueryIn.not());
                    subquery.where(subqueryPredicates.toArray(new Predicate[]{}));

                    p = cb.greaterThan(subquery, cb.literal(0L));
                    break;
                }

                case NotEXCEPT: //none except listed
                //case NoneEXCEPT:
                {
                    CriteriaBuilder.In<Comparable<?>> subqueryIn = cb.in(cmpFilterColumn);
                    Arrays.stream(filter.getValue()).forEach(subqueryIn::value);

                    List<Expression<?>> subqueryPredicates = new ArrayList<>(subquery.getRestriction().getExpressions());
                    subqueryPredicates.add(subqueryIn.not());
                    subquery.where(subqueryPredicates.toArray(new Predicate[]{}));

                    p = cb.equal(subquery, cb.literal(0L));
                    break;
                }

                //case ONLY: IN + NotEXCEPT
                //case EXACTLY: EACH + NotEXCEPT

                //case ANY: //'IN' comparator alternative with subquery
                //{
                //    Subquery<Long> subquery = this.generateCountSubquery(cb, query, filterColumn, filter);
                //    p = cb.greaterThan(subquery, cb.literal(0L));
                //    break;
                //}

                //case ALL: //TODO: Implement cb.all(subquery) method!
                //case EXISTS: //TODO: Implement cb.exists(subquery) method!
                //case NotEXISTS: //TODO: Implement cb.exists(subquery).not() method!
                //NOTICE case ANY/SOME: IN comparator alternatives (cb.any(subquery)/cb.some(subquery))
                //NOTICE example: https://www.logicbig.com/tutorials/java-ee-tutorial/jpa/criteria-api-all-any-some-methods.html
                default: throw new NotImplementedException(String.format("Missing Predicate implementation for '%s' comparator!", comparator));
            }

            return p;
        }

        private <T extends Comparable<? super T>> Map.Entry<Subquery<Long>, Path<? extends T>> generateCountSubquery(CriteriaBuilder cb, CriteriaQuery<?> query, Path<? extends T> filterColumn, QueryExpression.Filter<T> filter)
        {
            //Root root = Core.findRootPath(query.getRoots(), this.getEntityClass());
            //Path<?> parentFilterPath = filterColumn.getParentPath();
            //Path<?> grandparentFilterPath = parentFilterPath.getParentPath();

            String filterEntityPath = Helper.getPathWithoutLastItem(filter.getColumnPath());
            String filterEntityParentPath = Helper.getPathWithoutLastItem(filterEntityPath);
            Class<?> filterEntityParentType = es.getEntityClass(); //grandparentFilterPath.getJavaType();
            if(filterEntityParentPath != null)
            {
                Optional<Field> filterEntityParentField = ReflectionHelper.findFieldByPath(filterEntityParentType, filterEntityParentPath);

                if(filterEntityParentField.isPresent()) filterEntityParentType = filterEntityParentField.get().getType();
                else throw new RuntimeException(String.format("FilterEntityParentField '%s' NOT FOUND in Parent ENTITY Class: %s!", filterEntityParentPath, query.getResultType().getSimpleName()));
            }

            Path<?> grandparentFilterPath = Core.findFromPath(query.getRoots(), filterEntityParentType, filterEntityParentPath);
            Class<?> joinEntity = grandparentFilterPath.getJavaType();
            String joinColumnName = ReflectionHelper.getFieldNameFromPath(Helper.getPathWithoutLastItem(filter.getColumnPath()));

            //NOTICE: case when filterColumn is grandparentPath of filter column
            //Path<?> grandparentFilterPath = filterColumn;
            //Class<?> joinEntity = filterColumn.getJavaType();
            //String filterEntityParentPath = EntityService.getPathWithoutLastItem(filter.getColumnPath());
            //String joinColumnName = ReflectionHelper.getFieldNameFromPath(filterEntityParentPath);

            Subquery<Long> subquery = query.subquery(Long.class);
            //Subquery<?> subquery = query.subquery(filterColumn.getJavaType());
            Root<?> filterEntityParentRoot = subquery.from(joinEntity);
            Path<?> filterEntityJoinPath = filterEntityParentRoot.join(joinColumnName);

            String columnName = ReflectionHelper.getFieldNameFromPath(filter.getColumnPath());
            filterColumn = filterEntityJoinPath.get(columnName); //NOTICE: replace "fictional" fieldPath with subquery filter-column Path

            Predicate queryJoinRestriction = cb.equal(filterEntityParentRoot, grandparentFilterPath);
            subquery.where(Collections.singletonList(queryJoinRestriction).toArray(new Predicate[]{}));
            subquery.select(cb.count(filterEntityJoinPath)); //subquery.select(filterColumn);

            return new AbstractMap.SimpleImmutableEntry<>(subquery, filterColumn);
        }
    }


    public static class Core
    {
        private static <E> CriteriaQuery<E> createEntityQuery(CriteriaBuilder cb, Class<E> clazz)
        {
            CriteriaQuery<E> query = cb.createQuery(clazz);

            Root<E> root = query.from(clazz);
            root.alias(clazz.getSimpleName());

            //TODO: EntityService.this.extendFromQuery(root);

            return query;
        }

        private static <E> CriteriaQuery<E> replicateEntityQuery(CriteriaBuilder cb, CriteriaQuery<E> orgQuery)
        {
            CriteriaQuery<E> query = cb.createQuery(orgQuery.getResultType());

            return Core.replicateQuery(query, orgQuery);
        }

        private static <E> CriteriaQuery<Tuple> createTupleQuery(CriteriaBuilder cb, Class<E> clazz)
        {
            CriteriaQuery<E> typeQuery = Core.createEntityQuery(cb, clazz);
            CriteriaQuery<Tuple> query = Core.replicateTupleQuery(cb, typeQuery);

            return query;
        }

        private static CriteriaQuery<Tuple> replicateTupleQuery(CriteriaBuilder cb, CriteriaQuery<?> orgQuery)
        {
            CriteriaQuery<Tuple> query = cb.createTupleQuery();

            return Core.replicateQuery(query, orgQuery);
        }

        private static <T> CriteriaQuery<T> replicateQuery(CriteriaQuery<T> newQuery, CriteriaQuery<?> orgQuery)
        {
            //NOTICE: replicate all FromPaths (Root & Join) from original CriteriaQuery
            Core.replicateFromRoots(newQuery, orgQuery.getRoots());

            //NOTICE: using same Predicate from original CriteriaQuery
            if(orgQuery.getRestriction() != null) newQuery.where(orgQuery.getRestriction());
            if(orgQuery.getGroupRestriction() != null) newQuery.having(orgQuery.getGroupRestriction());

            //TODO if needed - copy all Predicate.Expression from original CriteriaQuery:
            //List<Expression> orgPredicates = new ArrayList<>(orgQuery.getRestriction().getExpressions());
            //newQuery.where(orgPredicates.toArray(new Predicate[]{}));

            return newQuery;
        }

        private static void replicateFromRoots(CriteriaQuery<?> query, Set<Root<?>> roots)
        {
            for(Root<?> r : roots)
            {
                From<?,?> root = null;

                //String joinPathString = this.getJoinPathString(r);
                //root = this.findFromPath(query.getRoots(), r.getJavaType(), joinPathString);
                //if(root == null && r.getParentPath() == null) //NOTICE: avoid replicating duplicate Roots (might produce bug)
                {
                    root = query.from(r.getModel());
                    root.alias(r.getAlias());
                }
                //else logger.warning(String.format("Existing RootPath (%s) FOUND for ENTITY Class: %s!", root.getAlias(), r.getJavaType().getSimpleName()));

                Core.replicateFromJoins(root, r.getJoins());
            }
        }

        private static void replicateFromJoins(From<?, ?> root, Set<? extends Join<?, ?>> joins)
        {
            if(root == null) throw new RuntimeException("JoinPath ROOT NOT FOUND!");

            for(Join<?, ?> j : joins)
            {
                From<?,?> join = null;

                //Class<?> joinClass = j.getJavaType();
                //String joinPathString = this.getJoinPathString(j);
                //join = Core.findFromPath(root.getJoins(), joinClass, joinPathString);
                //if(join == null) //NOTICE: avoid replicating duplicate Joins (might produce bug)
                {
                    join = Core.addJoin(root, j.getAttribute().getName(), j.getJoinType(), j.getAlias());
                }
                //else logger.warning(String.format("Existing JoinPath (%s) at '%s' FOUND for ENTITY Class: %s!", join.getAlias(), root.getAlias(), joinClass.getSimpleName()));

                Core.replicateFromJoins(join, j.getJoins());
            }
        }

        private static Integer getJoinCount(From<?,?> path)
        {
            Integer joinCount = 0;

            Path<?> rootPath = path;
            while(rootPath.getParentPath() != null) rootPath = rootPath.getParentPath();

            if(!(rootPath instanceof Root)) throw new RuntimeException("Could not find RootPath of JoinPath: " + path.getJavaType().getSimpleName());
            else joinCount = Core.getJoinCount(((Root<?>) rootPath).getJoins());

            return joinCount;
        }

        private static Integer getJoinCount(Set<? extends From<?,?>> roots)
        {
            int joinCount = 0;

            for(From<?,?> r : roots)
            {
                joinCount++;
                joinCount += Core.getJoinCount(r.getJoins());
            }

            return joinCount;
        }

        private static <E> boolean queryContainsJoinToMany(CriteriaQuery<E> query)
        {
            if(query.getRoots().size() > 1) return true;
            else return Core.rootsContainJoinToMany(query.getRoots());
        }

        private static boolean rootsContainJoinToMany(Set<? extends From<?, ?>> froms)
        {
            boolean joinToMany = false;

            for(From<?,?> f : froms)
            {
                joinToMany = f.isCompoundSelection(); //WARNING: not certain join-to-many indicator
                if(joinToMany) break;

                Bindable.BindableType bindableType = f.getModel().getBindableType();
                joinToMany = (bindableType == Bindable.BindableType.PLURAL_ATTRIBUTE);
                if(joinToMany) break;

                if(f instanceof Join)
                {
                    Attribute.PersistentAttributeType joinAttributeType = ((Join<?, ?>) f).getAttribute().getPersistentAttributeType();

                    joinToMany = (joinAttributeType == Attribute.PersistentAttributeType.ONE_TO_MANY);
                    if(joinToMany) break;

                    joinToMany = (joinAttributeType == Attribute.PersistentAttributeType.MANY_TO_MANY);
                    if(joinToMany) break;

                    joinToMany = (joinAttributeType == Attribute.PersistentAttributeType.ELEMENT_COLLECTION);
                    if(joinToMany) break;
                }

                joinToMany = Core.rootsContainJoinToMany(f.getJoins());
                if(joinToMany) break;
            }

            return joinToMany;
        }

        private static <T> Join<?, T> addLeftJoin(From<?, ?> path, String joinField)
        {
            //TODO: refactor auto-join implementation to work without aliases (PROBLEM: Predicates reference original query Path aliases)
            Integer joinCount = Core.getJoinCount(path);
            return Core.addLeftJoin(path, joinField, joinField+joinCount+"join");
        }

        private static <T> Join<?, T> addLeftJoin(From<?, ?> path, String joinField, String alias)
        {
            return Core.addJoin(path, joinField, JoinType.LEFT, alias);
        }

        private static <T> Join<?, T> addJoin(From<?, ?> path, String joinField, JoinType joinType, String alias)
        {
            Join<?, T> join = path.join(joinField, joinType);
            String joinAlias = (alias != null) ? alias : joinField;
            join.alias(joinAlias);

            return join;
        }

        //NOTICE: Join with ON for unrelated/unassociated entities (available since Hibernate v5.1)
        private static <T> Join<?, T> addLeftJoinOn(CriteriaBuilder cb, CriteriaQuery<?> query, From<?,?> path, String joinField, Class<?> joinEntity, String remoteJoinField, String alias)
        {
            //TODO: implement using Hibernate v5.1
            throw new NotImplementedException("JPA CriteriaAPI missing JOIN with ON for unrelated entities!");

            //Root<?> joinPath = query.from(joinEntity);
            //Path<?> joinFieldPath = path.get(joinField);
            //Path<?> remoteJoinFieldPath = joinPath.get(remoteJoinField);
            //Predicate joinRestriction = cb.equal(joinFieldPath, remoteJoinFieldPath);
            //Join join = path.join(joinField, JoinType.LEFT).on(joinRestriction);
            //String joinAlias = (alias != null) ? alias : joinField;
            //join.alias(joinAlias);

            //return join;
        }

        private static <T> Fetch<?, T> addLeftFetch(From<?,?> path, String joinField)
        {
            Fetch fetch = path.fetch(joinField, JoinType.LEFT);

            return fetch;
        }

        private static <T> Join<?, T> generateJoinPath(From<?, ?> parentPath, Class<T> childEntity, LinkedList<String> fieldJoinPaths)
        {
            Join<?, T> childPath = null;

            String joinField = fieldJoinPaths.removeFirst();
            Class<?> parentEntity = parentPath.getJavaType();
            Optional<Field> fieldOptional = ReflectionHelper.findModelField(parentEntity, joinField);
            if(!fieldOptional.isPresent()) throw new RuntimeException(String.format("JoinField '%s' (%s) NOT FOUND in Parent ENTITY Class: %s!", joinField, childEntity.getSimpleName(), parentEntity.getSimpleName()));
            //else logger.info(String.format("JoinField '%s' (%s) FOUND in Parent ENTITY Class: %s!", joinField, fieldOptional.get().getType().getSimpleName(), parentEntity.getSimpleName()));

            Field field = fieldOptional.get();
            if(fieldJoinPaths.size() > 1)   //RECURSIVE-JOIN until ONLY Field name left in fieldJoinPaths
            {
                /*
                Class<?> newParentEntity = field.getType();
                if(Collection.class.isAssignableFrom(newParentEntity))
                {
                    newParentEntity = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                }

                return Core.createJoinPath(roots, newParentEntity, childEntity, fieldJoinPaths);
                */

                Join<?,?> newRoot = Core.addLeftJoin(parentPath, field.getName());
                childPath = Core.generateJoinPath(newRoot, childEntity, fieldJoinPaths);
            }
            else if(fieldJoinPaths.size() == 1)
            {
                childPath = Core.addLeftJoin(parentPath, field.getName());
            }
            else
            {
                throw new RuntimeException(String.format("JoinPath NOT RESOLVED for Child ENTITY Class: %s!", childEntity.getSimpleName()));
            }

            return childPath;
        }

        private static <T> From<?, T> findOrGenerateJoinPath(Set<? extends From<?, ?>> roots, Class<?> parentEntity, Class<T> childEntity, LinkedList<String> fieldJoinPaths)
        {
            if(fieldJoinPaths.isEmpty()) throw new RuntimeException(String.format("FieldJoinPath EMPTY for Child ENTITY Class (%s) in Parent ENTITY Class (%s)!", Optional.ofNullable(childEntity).map(Class::getSimpleName).orElse(null), parentEntity.getSimpleName()));

            //TODO: remove childEntity completely (not needed since auto-joins & field-paths)
            //NOTICE: use NULL as childEntity Class parameter if field Entity is UNKNOWN (resolved from fieldPath)
            if(childEntity == null)
            {
                String fieldPath = String.join(ReflectionHelper.PATH_SEPARATOR, fieldJoinPaths);
                Map.Entry<Class<?>, String> field = ReflectionHelper.findEntityFieldByPath(parentEntity, fieldPath, true);
                childEntity = (Class<T>) field.getKey();
            }

            String fieldPath = String.join(ReflectionHelper.PATH_SEPARATOR, fieldJoinPaths);
            String joinPath = Helper.getPathWithoutLastItem(fieldPath);
            From<?,T> childPath = Core.findFromPath(roots, childEntity, joinPath);
            if(childPath == null)
            {
                if(fieldJoinPaths.size() <= 1) throw new RuntimeException(String.format("JoinPath NOT FOUND for Child ENTITY Class: %s!", childEntity.getSimpleName()));

                From<?,?> parentPath = null;
                do
                {
                    //Class<?> joinedEntity = parentEntity;
                    Map.Entry<Class<?>, String> entityClassField = ReflectionHelper.findEntityFieldByPath(parentEntity, joinPath, true);
                    Class<?> joinedEntity = entityClassField.getKey();
                    joinPath = Helper.getPathWithoutLastItem(joinPath);

                    //if(joinPath != null)
                    //{
                    //    Optional<Field> parentEntityField = ReflectionHelper.findFieldByPath(parentEntity, joinPath);
                    //    if(parentEntityField.isPresent()) joinedEntity = parentEntityField.get().getType();
                    //    else throw new RuntimeException(String.format("Field NOT FOUND by path '%s' in ENTITY Class: %s!", joinPath, parentEntity.getSimpleName()));
                    //    //TODO: resolve Collection type: if(Collection.class.isAssignableFrom(joinedEntity)) joinedEntity = (Class<?>) ((ParameterizedType) parentEntityField.get().getGenericType()).getActualTypeArguments()[0];
                    //}

                    parentPath = Core.findFromPath(roots, joinedEntity, joinPath);
                }
                while(parentPath == null && StringUtils.isNotEmpty(joinPath));

                //NOTICE: WHILE alternative (instead of DO-WHILE) - preserve joinPath value
                //String parentJoinPath = Helper.getPathWithoutLastItem(joinPath);
                //From<?,?> parentPath = Core.findFromPath(roots, parentEntity, parentJoinPath);
                //while(parentPath == null && StringUtils.isNotEmpty(parentJoinPath))
                //{
                //    parentJoinPath = Helper.getPathWithoutLastItem(parentJoinPath);
                //    parentPath = Core.findFromPath(roots, parentEntity, parentJoinPath);
                //}

                if(parentPath != null)
                {
                    String parentJoinPath = Core.getJoinPathString(parentPath);
                    if(parentJoinPath != null)
                    {
                        LinkedList<String> parentJoinPaths = ReflectionHelper.pathToLinkedList(parentJoinPath);
                        parentJoinPaths.forEach(fieldJoinPaths::removeFirstOccurrence);    //remove existing join paths from fieldJoinPaths
                    }

                    childPath = Core.generateJoinPath(parentPath, childEntity, fieldJoinPaths);
                }
                else throw new RuntimeException(String.format("JoinPath NOT FOUND for Parent ENTITY Class: %s!", parentEntity.getSimpleName()));
            }

            //if(fieldJoinPaths.size() <= 1) //NOT NEEDED: resolved childPath Entity must contain this field
            {
                String fieldName = fieldJoinPaths.getLast();
                childEntity = (Class<T>) childPath.getJavaType();    //override childEntity by next fieldPath entity (resolve fieldPath completely)
                Optional<Field> fieldOptional = ReflectionHelper.findModelField(childEntity, fieldName);
                if(!fieldOptional.isPresent()) throw new RuntimeException(String.format("Field '%s' NOT FOUND in ENTITY Class: %s!", fieldName, childEntity.getSimpleName()));
            }

            return childPath;
        }

        private static <T> From<?, T> findFromPath(Set<? extends From<?, ?>> roots, Class<T> entity, String joinPath)
        {
            //TODO: PERFOMANCE IMPROVEMENT - save entity Path by Class into Map<Class, Path> (avoid findFromPath for every filter)

            //NOTICE: String joinPath is path from Root to Join Entity without final field (joinEntity1.joinEntity2)
            //NOTICE: findFromPath will fail to find FromPath for Entity by joinPath if wrong entity parameter is provided for path
            //NOTICE-FIXED: method findFromPath is NOT parentEntity agnostic - Path verification by joinPath string

            From<?,T> path = null;
            //Path<?> path = query.getRoots().stream().filter(e -> e.getJavaType() == entity).findAny().orElse(null);

            int i=0;
            for(From<?, ?> r : roots)
            {
                //TODO: implement for all Roots - findFromPath is root agnostic (extend path with root entity):
                //resolve only 1st Root path (Queried <E> Root Entity)
                if(r instanceof Root && ++i > 1) break; //NOTICE: possible BUG if cross-join RootPath is at first position instead of queried <E> Root Entity

                String entityJoinPath = (StringUtils.isNotEmpty(joinPath)) ? Core.getJoinPathString(r) : null;

                //logger.info(entity.getSimpleName() + " ?= " + f.getJavaType().getSimpleName());
                if(r.getJavaType() == entity)
                {
                    //NOTICE: check if Entity joinPath is correct in case joinPath exists (NOT root Entity)
                    if(StringUtils.isNotEmpty(joinPath))
                    {
                        if(entityJoinPath != null && joinPath.equals(entityJoinPath)) path = (From<?, T>) r;
                    }
                    else //TODO: if(path instanceof Root) //verify only Root paths are resolved when null/empty joinPath
                    {
                        path = (From<?, T>) r;
                    }
                }

                if(path == null)
                {
                    if(StringUtils.isNotEmpty(joinPath))    //verify path
                    {
                        if(entityJoinPath == null || joinPath.startsWith(entityJoinPath)) path = Core.findFromPath(r.getJoins(), entity, joinPath);
                    }
                    else
                    {
                        path = Core.findFromPath(r.getJoins(), entity, joinPath);
                    }
                }

                if(path != null) break;
            }

            return path;
        }

        private static String getJoinPathString(Path<?> path)
        {
            String joinPathString = null;

            if(path instanceof Join)
            {
                Join<?, ?> joinPath = (Join<?,?>) path;
                joinPathString = joinPath.getAttribute().getName();

                From<?,?> parentPath = joinPath.getParent();
                if(parentPath != null)
                {
                    String parentPathString = Core.getJoinPathString(parentPath);
                    if(parentPathString != null) joinPathString = parentPathString + ReflectionHelper.PATH_SEPARATOR + joinPathString;
                }
            }

            return joinPathString;
        }

        private static <T> Root<T> findOrGenerateRootPath(CriteriaBuilder cb, CriteriaQuery<?> query, Path<?> parentFieldPath, Class<T> entity, String joinColumn)
        {
            Root<T> root = Core.findEntityRootPath(query.getRoots(), entity);
            List<Expression> orgPredicates = (query.getRestriction() != null) ? new ArrayList(query.getRestriction().getExpressions()) : new ArrayList();

            /*
            if(root != null)    //TODO: verify cross-join where restriction (BUG with same cross-join entity over different field)
            {
                Path<?> joinFieldPath = root.get(joinColumn);
                Predicate crossJoinRestriction = cb.equal(parentFieldPath, joinFieldPath);

                boolean crossJoinVerified = false;
                for(Expression p : orgPredicates)
                {
                    logger.info(String.format("CrossJoin restriction validation: %s <> %s", p.toString(), crossJoinRestriction.toString()));
                    if(p.toString().equals(crossJoinRestriction.toString()))
                    {
                        crossJoinVerified = true;
                        break;
                    }
                }

                if(!crossJoinVerified) root = null;
            }
            */

            if(root == null)
            {
                //Simulate LEFT OUTER JOIN using CROSS JOIN & SubQuery - check if join reference does NOT exist
                //BUG WARNING: Entities without cross-join references have wildcard values (any value from join-table as result of Cartesian product)
                Subquery<T> subQuery = query.subquery(entity);
                Root subRoot = subQuery.from(entity);
                Path<?> subJoinFieldPath = subRoot.get(joinColumn);
                Predicate subQueryRestriction = cb.equal(parentFieldPath, subJoinFieldPath);
                subQuery.select(subRoot).where(subQueryRestriction);

                root = query.from(entity);
                root.alias(entity.getSimpleName()+(query.getRoots().size()-1));
                Path<?> joinFieldPath = root.get(joinColumn);
                Predicate crossJoinRestriction = cb.or(cb.equal(parentFieldPath, joinFieldPath), cb.not(cb.exists(subQuery)));
                orgPredicates.add(crossJoinRestriction);
                query.where(orgPredicates.toArray(new Predicate[]{}));
            }

            return root;
        }

        private static <T> Root<T> findEntityRootPath(Set<? extends Root<?>> roots, Class<T> entity)
        {
            return (Root<T>) Core.findEntityPath(roots, entity);
        }

        private static <T> From<T,?> findEntityPath(Set<? extends From<?, ?>> froms, Class<T> entity)
        {
            From<T,?> from = null;

            for(From<?,?> f : froms)
            {
                if(f.getJavaType() == entity)
                {
                    from = (From<T,?>) f;
                    break;
                }
            }

            return from;
        }

        //NOTICE: JPA/Hibernate auto-join using fieldPath directly - avoiding Join paths resolving (possibly suitable for Sort & Cross-Join-Root references)
        //NOTICE: generates cross-join + where (by default) for implicit @OneToOne/@ManyToOne associations in fieldPath
        //WARNING: join for implicit @OneToMany associations (PluralAttributes) throws IllegalStateException exception: Illegal attempt to dereference path source of basic type
        private static Path<?> generateFieldPath(Set<? extends From<?,?>> roots, Class<?> parentEntity, String fieldPath)
        {
            Path<?> fieldColumn = null;

            if(StringUtils.isNotBlank(fieldPath))
            {
                fieldColumn = Core.findEntityPath(roots, parentEntity);
                LinkedList<String> fieldPaths = ReflectionHelper.pathToLinkedList(fieldPath);

                for(String p : fieldPaths) fieldColumn = fieldColumn.get(p);
            }

            return fieldColumn;
        }

        private static <T> Path<T> findOrGenerateFieldJoinPath(Set<? extends From<?,?>> roots, Class<?> parentEntity, Class<?> fieldEntity, String fieldPath)
        {
            Path<T> fieldColumnPath = null;

            if(StringUtils.isNotBlank(fieldPath))
            {
                LinkedList<String> fieldPaths = ReflectionHelper.pathToLinkedList(fieldPath);
                Path<?> path = Core.findOrGenerateJoinPath(roots, parentEntity, fieldEntity, fieldPaths);

                String fieldName = fieldPaths.getLast();
                if(StringUtils.isNotBlank(fieldName)) fieldColumnPath = path.get(fieldName);
            }

            return fieldColumnPath;
        }

        //NOTICE: supports cross-join fieldPath
        //WARNING: cross-join is NOT RECOMMENDED for large datasets - significantly reduces performance!
        //BEST PRACTICE: recommended Entity model mapping of unrelated associations using @JoinColumn + @Where & FetchType.LAZY
        private static <T> Path<T> findOrGenerateFieldJoinPath(CriteriaBuilder cb, CriteriaQuery<?> query, Class<?> parentEntity, Class<?> fieldEntity, String fieldPath)
        {
            Path<T> fieldColumnPath = null;

            if(StringUtils.isNotBlank(fieldPath))
            {
                String crossJoinPathRegex = "\\(([^\\[\\]\\s]+)\\*([^\\[\\]\\s\\.]+)\\)\\.([^\\[\\]\\s]+)";
                Pattern crossJoinRegex = Pattern.compile(crossJoinPathRegex);
                Matcher crossJoinMatcher = crossJoinRegex.matcher(fieldPath);
                if(crossJoinMatcher.matches())
                {
                    String parentJoinColumn = crossJoinMatcher.group(1);
                    Path<?> parentJoinPath = Core.findOrGenerateFieldJoinPath(query.getRoots(), parentEntity, null, parentJoinColumn);

                    String fieldJoinColumn = crossJoinMatcher.group(2);
                    Root<?> crossJoinRoot = Core.findOrGenerateRootPath(cb, query, parentJoinPath, fieldEntity, fieldJoinColumn);
                    //Join<?,?> crossJoinRoot = this.addLeftJoinOn(cb, query, (From<?,?>) parentJoinPath.getParentPath(), ReflectionHelper.getFieldNameFromPath(parentJoinColumn), fieldEntity, fieldJoinColumn, null);

                    String fieldColumn = crossJoinMatcher.group(3);
                    //fieldColumnPath = this.generateFieldPath(Sets.newHashSet(crossJoinRoot), crossJoinRoot.getJavaType(), fieldColumn);
                    fieldColumnPath = Core.findOrGenerateFieldJoinPath(Sets.newHashSet(crossJoinRoot), crossJoinRoot.getJavaType(), null, fieldColumn);
                }
                else    //NOTICE: default flow (regular entity-model joins)
                {
                    fieldColumnPath = Core.findOrGenerateFieldJoinPath(query.getRoots(), parentEntity, fieldEntity, fieldPath);
                }
            }

            return fieldColumnPath;
        }
    }

    public static class PredicateUtil
    {
        private static Predicate combinePredicates(CriteriaBuilder cb, Predicate pred1, Predicate pred2, QueryExpression.Conditional.Operator operator)
        {
            Predicate predicate = null;

            if(pred1 != null && pred2 == null) predicate = pred1;
            else if(pred1 == null && pred2 != null) predicate = pred2;
            else if(pred1 != null && pred2 != null)
            {
                switch(operator)
                {
                    case AND:
                    {
                        predicate = cb.and(pred1, pred2);
                        break;
                    }

                    case OR:
                    {
                        predicate = cb.or(pred1, pred2);
                        break;
                    }
                }
            }

            return predicate;
        }
    }

    //UTIL METHODS

    public static class EsUtil
    {
        public static <E> void logQuery(QueryType queryType, TypedQuery<E> dataQuery, long queryStartTimeNs)
        {
            long queryTimeMs = EsUtil.calculateQueryTimeMs(queryStartTimeNs);
            EsUtil.logQuery(queryType.toString(), dataQuery, queryTimeMs);
        }

        private static void logQuery(String queryType, Query query, long queryTimeMs)
        {
            String queryString = query.unwrap(org.hibernate.query.Query.class).getQueryString();
            logger.log(Level.INFO, String.format("EntityService %s Query took %d ms: %s", queryType, queryTimeMs, queryString));
        }

        private static long calculateQueryTimeMs(long queryStartTimeNs)
        {
            //query performance statistics
            long queryEndTimeNs = System.nanoTime();
            long queryTimeMs = (queryEndTimeNs-queryStartTimeNs)/1000000;

            return queryTimeMs;
        }

        private static <T extends Comparable<? super T>> T[] getModValues(QueryExpression.Filter<T> filter)
        {
            switch(filter.getMod())
            {
                case NONE: return filter.getValue();
                case LikeL: return (T[]) Arrays.stream(filter.getValue()).map(v -> "%"+v).toArray(String[]::new);
                case LikeR: return (T[]) Arrays.stream(filter.getValue()).map(v -> v+"%").toArray(String[]::new);
                case LikeLR: return (T[]) Arrays.stream(filter.getValue()).map(v -> "%"+v+"%").toArray(String[]::new);
                case SPLIT: return (T[]) Arrays.stream(filter.getValue()).map(v -> String.valueOf(v).trim().split(StringUtils.SPACE)).flatMap(v -> Arrays.stream(v.clone())).toArray(String[]::new);
                case SplitLikeL: return (T[]) Arrays.stream(filter.getValue()).map(v -> String.valueOf(v).trim().split(StringUtils.SPACE)).flatMap(v -> Arrays.stream(v.clone())).map(v -> "%"+v).toArray(String[]::new);
                case SplitLikeR: return (T[]) Arrays.stream(filter.getValue()).map(v -> String.valueOf(v).trim().split(StringUtils.SPACE)).flatMap(v -> Arrays.stream(v.clone())).map(v -> v+"%").toArray(String[]::new);
                case SplitLikeLR: return (T[]) Arrays.stream(filter.getValue()).map(v -> String.valueOf(v).trim().split(StringUtils.SPACE)).flatMap(v -> Arrays.stream(v.clone())).map(v -> "%"+v+"%").toArray(String[]::new);
                default: throw new NotImplementedException(String.format("Missing implementation for Filter modifier: %s", filter.getMod()));
            }
        }

        private static <T> Comparable<?>[] convertValuesToComparable(Class typeClazz, T[] values)
        {
            return Arrays.stream(values).map(v -> EsUtil.convertValueToComparable(typeClazz, v)).toArray(Comparable[]::new);
        }

        private static Comparable<?> convertValueToComparable(Class<?> typeClazz, Object value)
        {
            if(value instanceof String) value = Helper.parseValue(typeClazz, (String) value); //TODO fix: bug when using EntityService with Database Type String

            //TODO: cast value if needed
            //if(!typeClazz.isAssignableFrom(value.getClass())) value = Helper.castObject(typeClazz, value);

            //check if value conforms to Comparable type
            if(!Comparable.class.isAssignableFrom(value.getClass())) throw new RuntimeException(String.format("Value '%s' (%s) with %s type not assignable to Comparable!", value, typeClazz.getSimpleName(), value.getClass().getSimpleName()));

            return (Comparable<?>) value;
        }

        private static String getDistinctValueKey(QueryExpression distinctQuery)
        {
            String key = distinctQuery.getName();

            if(key == null)
            {
                //generate distinctValue KEY
                key = distinctQuery.getEntity().getSimpleName();
                if(distinctQuery.getValueColumnPath() != null) key += " " + ReflectionHelper.getFieldNameFromPath(distinctQuery.getValueColumnPath());
                if(distinctQuery.getLabelColumnPath() != null) key += " and " + distinctQuery.getLabelColumnPath();

                key = WordUtils.capitalize(key.replaceAll("[\\._]", " "));
                key = StringUtils.uncapitalize(StringUtils.deleteWhitespace(key));
            }

            return key;
        }

        private static String getMetaValueKey(QueryExpression metaQuery)
        {
            String key = metaQuery.getName();

            if(key == null)
            {
                //generate metaValue KEY and ADD to metaValues
                key = metaQuery.getEntity().getSimpleName();
                if(metaQuery.getValueColumnPath() != null) key += " " + ReflectionHelper.getFieldNameFromPath(metaQuery.getValueColumnPath());
                if(metaQuery.getLabelColumnPath() != null) key += " by " + metaQuery.getLabelColumnPath().replaceAll(EntityService.LABEL_PATHS_SEPARATOR, " and ");
                if(metaQuery.getFunc() != null) key += " " + StringUtils.lowerCase(metaQuery.getFunc().name());
                if(metaQuery.getFilters() != null) for(QueryExpression.Filter f : metaQuery.getFilters()) key += " " + f.getName();

                key = WordUtils.capitalize(key.replaceAll("[\\._]", " "));
                key = StringUtils.uncapitalize(StringUtils.deleteWhitespace(key));
            }

            return key;
        }
    }

    //HELPER METHODS

    public static class Helper
    {
        private static Class<?> findGenericClassImplementation(Class<?> genericImpl, Class<?> genericClass)
        {
            Optional<Type> genericInterface = Helper.findGenericClassInterface(genericImpl, genericClass);

            if(!genericInterface.isPresent())
            {
                Type newGenericClass = genericImpl.getSuperclass();

                if(newGenericClass == null)
                {
                    //throw new RuntimeException(String.format("Superclass type of %s class UNKNOWN!", genericClass.getSimpleName()));

                    newGenericClass = genericImpl.getGenericSuperclass();
                    if(newGenericClass == null) throw new RuntimeException(String.format("GenericSuperclass type of %s class UNKNOWN!", genericImpl.getSimpleName()));

                    if(newGenericClass instanceof ParameterizedType) newGenericClass = ((Class<?>) ((ParameterizedType) newGenericClass).getRawType()).getGenericSuperclass();
                }

                genericImpl = Helper.findGenericClassImplementation((Class<?>) newGenericClass, genericClass);
            }

            return genericImpl;
        }

        private static Optional<Type> findGenericClassInterface(Class<?> genericImpl, Class<?> genericClass)
        {
            Optional<Type> genericInterface = Optional.empty();

            Type[] genericInterfaces = genericImpl.getGenericInterfaces();
            //genericInterface = Arrays.stream(genericInterfaces).filter(i -> i.getTypeName().startsWith(genericClass.getName())).findAny();
            genericInterface = Arrays.stream(genericInterfaces).filter(i ->  (i instanceof ParameterizedType) && ((ParameterizedType) i).getRawType() == genericClass).findAny();

            if(!genericInterface.isPresent()) logger.warning(String.format("GenericInterface %s of %s class NOT FOUND!", genericClass.getSimpleName(), genericImpl.getSimpleName()));

            return genericInterface;
        }

        private static String getPathWithoutLastItem(String fieldPath)
        {
            String path = null;

            if(fieldPath != null)
            {
                int fieldStartIndex = fieldPath.lastIndexOf(ReflectionHelper.PATH_SEPARATOR);
                if(fieldStartIndex >= 0) path = fieldPath.substring(0, fieldStartIndex);
            }

            return path;
        }

        //PARSE values for other data-types (e.g. type Boolean & func EQ)
        private static <T extends Comparable<? super T>> Comparable[] parseValues(Class typeClazz, T... values)
        {
            Comparable[] vals = values;

            //NOTICE: CAST won't work for String -> Integer/Boolean... parse manually by column JavaType
            //Set<T> vals = Arrays.stream(values).map(v -> (E) EntityService.castObject(typeClazz, v)).collect(Collectors.toSet());

            if(typeClazz != String.class)   //no need to parse to String (values already String or Comparable)
            {
                Class arrayClass = values.getClass().getComponentType();

                //if(Comparable.class == arrayClass) vals = Arrays.stream(values).map(v -> EntityService.parseValue(typeClazz, v.toString())).toArray(Comparable[]::new);
                if(Comparable.class == arrayClass) vals = Arrays.stream(values).map(v -> (v instanceof String) ? Helper.parseValue(typeClazz, v.toString()) : v).toArray(Comparable[]::new);
                else if(String.class.isAssignableFrom(arrayClass)) vals = Arrays.stream(values).map(v -> Helper.parseValue(typeClazz, v.toString())).toArray(Comparable[]::new);
                //else logger.warning(String.format("Could NOT parse values to %s type - values array is not Comparable/String type: %s", typeClazz.getSimpleName(), arrayClass.getSimpleName()));
            }

            return vals;
        }

        private static Comparable parseValue(Class clazz, String value)
        {
            try
            {
                if(Boolean.class.isAssignableFrom(clazz) || boolean.class.isAssignableFrom(clazz)) return Boolean.parseBoolean(value);
                if(Byte.class.isAssignableFrom(clazz) || byte.class.isAssignableFrom(clazz)) return Byte.parseByte(value);
                if(Short.class.isAssignableFrom(clazz) || short.class.isAssignableFrom(clazz)) return Short.parseShort(value);
                if(Integer.class.isAssignableFrom(clazz) || int.class.isAssignableFrom(clazz)) return Integer.parseInt(value);
                if(Long.class.isAssignableFrom(clazz) || long.class.isAssignableFrom(clazz)) return Long.parseLong(value);
                if(Float.class.isAssignableFrom(clazz) || float.class.isAssignableFrom(clazz)) return Float.parseFloat(value);
                if(Double.class.isAssignableFrom(clazz) || double.class.isAssignableFrom(clazz)) return Double.parseDouble(value);
                if(clazz.isEnum() || Enum.class.isAssignableFrom(clazz)) return Enum.valueOf(clazz, value);
                //if(clazz.isEnum() || Enum.class.isAssignableFrom(clazz)) return (Comparable) Arrays.stream(clazz.getEnumConstants()).filter(v -> value.equalsIgnoreCase(v.toString())).findAny().orElse(value);
                if(Date.class.isAssignableFrom(clazz))
                {
                    Date dateValue = DateTimeFormatUtil.parseDateTimeISO8601(value);
                    return (dateValue != null) ? dateValue : value;
                }
            }
            catch(Exception ex)
            {
                logger.log(Level.WARNING, String.format("Could NOT parse String value '%s' to Comparable<%s> type - using original value!", value, clazz.getSimpleName()), ex);
            }

            return value;
        }

        private static <T> T[] castValues(Class<T> castClazz, Object... values)
        {
            T[] vals = (T[]) Array.newInstance(castClazz, values.length);

            for(int i=0; i < values.length; i++)
            {
                vals[i] = Helper.castObject(castClazz, values[i]);
            }

            return vals;
        }

        private static <T> T castObject(Class<T> clazz, Object o)
        {
            try
            {
                return clazz.cast(o);
            }
            catch(ClassCastException e)
            {
                return (T) o;   //returns original (non-casted) value
            }
        }
    }

    //EntityService.AttributeMapper
    //NOTICE: filtering by EntityService.AttributeMapper Fields works only for same type AttributeConverter<T,T>
    //WARNING: EntityService.AttributeMapper Fields should (at the moment) be read-only when using different type AttributeConverter<X,Y>
    public static interface AttributeMapper<X,Y extends Comparable<? super Y>> extends AttributeConverter<X,Y>
    {
        //public Class<X> getEntityType();
        //public Class<Y> getDatabaseType();

        public X getDefaultValue();

        //public Y[] getMappedValues(X value);
        public Y[] getAllMappedValues();

        public Y[] mapToDatabaseValues(X x);
        public Y convertToDatabaseValue(X x);
        //public X mapToEntityValue(Y[] y);
        public X convertToEntityValue(Y y);

        default Class<X> getEntityType()
        {
            Class<X> dataType = null;

            try
            {
                dataType = ReflectionHelper.getGenericClassParameter(this.getClass(), AttributeMapper.class, 0);
            }
            catch(RuntimeException ex)
            {
                throw new RuntimeException("AttributeConverter.EntityType NOT FOUND - Override EntityService.AttributeMapper.getEntityType() method: " + ex.getMessage());
            }

            return dataType;
        }

        default Class<Y> getDatabaseType()
        {
            Class<Y> dataType = null;

            try
            {
                dataType = ReflectionHelper.getGenericClassParameter(this.getClass(), AttributeMapper.class, 1);
            }
            catch(RuntimeException ex)
            {
                throw new RuntimeException("AttributeConverter.DatabaseType NOT FOUND - Override EntityService.AttributeMapper.getDatabaseType() method: " + ex.getMessage());
            }

            return dataType;
        }

        default boolean tryCastEntityValueToDatabaseValue()
        {
            return true;
        }

        @Override
        default Y convertToDatabaseColumn(X x)
        {
            if(this.tryCastEntityValueToDatabaseValue() && this.getDatabaseType().isAssignableFrom(x.getClass()))
            {
                return Helper.castObject(this.getDatabaseType(), x);
            }
            else
            {
                return this.convertToDatabaseValue(x);
            }
        }

        @Override
        default X convertToEntityAttribute(Y y)
        {
            if(y == null) return this.getDefaultValue();
            else return this.convertToEntityValue(y);
        }
    }

    //SQL QUERY META-MODEL

    public static class QueryExpression  //NOTICE: Meta/Distinct Values Query meta-model
    {
        public enum Func { COUNT, COUNT_DISTINCT, SUM, AVG, MIN, MAX; }

        private String name;                //arbitrary name for QueryExpression (meta-value fields naming)
        private Class<?> entity;            //query entity
        private String labelColumnPath;     //column with label-value
        private String valueColumnPath;     //column with value
        private Func func = Func.COUNT;     //aggregation function used with value-column (default: Func.COUNT)
        private Filter<?>[] filters;        //additional query filters
        private boolean distinct = false;   //distinct aggregation values (default: false)

        public QueryExpression(Class entity)
        {
            this.entity = entity;
        }

        public QueryExpression(String name, Class entity)
        {
            this(entity);
            this.name = name;
        }

        public QueryExpression(Class entity, String valueColumnPath)
        {
            this(entity);
            this.valueColumnPath = valueColumnPath;
        }

        public QueryExpression(String name, Class entity, String valueColumnPath)
        {
            this(name, entity);
            this.valueColumnPath = valueColumnPath;
        }

        public QueryExpression(Class entity, String valueColumnPath, String labelColumnPath)
        {
            this(entity, valueColumnPath);
            this.labelColumnPath = labelColumnPath;
        }

        public QueryExpression(String name, Class entity, String valueColumnPath, String labelColumnPath)
        {
            this(name, entity, valueColumnPath);
            this.labelColumnPath = labelColumnPath;
        }

        public QueryExpression(Class entity, String valueColumnPath, Func func)
        {
            this(entity, valueColumnPath);
            this.func = func;
        }

        public QueryExpression(String name, Class entity, String valueColumnPath, Func func)
        {
            this(name, entity, valueColumnPath);
            this.func = func;
        }

        public QueryExpression(Class entity, String valueColumnPath, Func func, String labelColumnPath)
        {
            this(entity, valueColumnPath, func);
            this.labelColumnPath = labelColumnPath;
        }

        public QueryExpression(String name, Class entity, String valueColumnPath, Func func, String labelColumnPath)
        {
            this(name, entity, valueColumnPath, func);
            this.labelColumnPath = labelColumnPath;
        }

        public QueryExpression(Class entity, String valueColumnPath, Func func, Filter... filters)
        {
            this(entity, valueColumnPath, func);
            this.filters = filters;
        }

        public QueryExpression(String name, Class entity, String valueColumnPath, Func func, Filter... filters)
        {
            this(name, entity, valueColumnPath, func);
            this.filters = filters;
        }

        public QueryExpression(Class entity, String valueColumnPath, Func func, String labelColumnPath, Filter... filters)
        {
            this(entity, valueColumnPath, func, labelColumnPath);
            this.filters = filters;
        }

        public QueryExpression(String name, Class entity, String valueColumnPath, Func func, String labelColumnPath, Filter... filters)
        {
            this(name, entity, valueColumnPath, func, labelColumnPath);
            this.filters = filters;
        }

        public String getName()
        {
            return name;
        }

        public Class getEntity()
        {
            return entity;
        }

        public String getLabelColumnPath()
        {
            return labelColumnPath;
        }

        public String getValueColumnPath()
        {
            return valueColumnPath;
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
            return String.format("(%s) <%s> %s:%s - FILTERS(%s)", this.getEntity().getSimpleName(), this.getFunc(), this.getValueColumnPath(), this.getLabelColumnPath(), filtersString);
        }

        public static class Conditional //NOTICE: Complex Logical Conditional Filters
        {
            public enum Operator { AND, OR; }

            //NOTICE: filters & conditionals can be null (memory usage reduction) - initialized HashSet only for programming convenience
            private Set<Filter> filters = new HashSet<>();              //simple root filters
            private Set<Conditional> conditionals = new HashSet<>();    //complex/compound nested sub-filters
            private Operator logicOperator = Operator.AND;              //logical compound operator for filters & conditionals

            public Conditional() { }

            public Conditional(Operator logicOperator)
            {
                this.logicOperator = logicOperator;
            }

            public Conditional(Set<Filter> filters)
            {
                this.filters = filters;
            }

            public Conditional(Operator logicOperator, Set<Filter> filters)
            {
                this.logicOperator = logicOperator;
                this.filters = filters;
            }

            public Conditional(Operator logicOperator, Set<Filter> filters, Set<Conditional> conditionals)
            {
                this.logicOperator = logicOperator;
                this.filters = filters;
                this.conditionals = conditionals;
            }

            public Operator getLogicOperator()
            {
                return logicOperator;
            }

            public Set<Filter> getFilters()
            {
                return filters;
            }

            public Set<Conditional> getConditionals()
            {
                return conditionals;
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

            private String name;                        //arbitrary name for Filter (meta-value fields naming)
            private Class<?> entity;                    //filter entity
            private String columnPath;                  //column-name OR column-path from root entity
            private Cmp cmp = Cmp.EQ;                   //filter comparison operator (default: Cmp.EQ)
            private Modifier mod = Modifier.NONE;       //filter value format (default: Modifier.NONE)
            private T[] value;                          //filter value(s)
            //private String subquery;                    //filter subquery

            public Filter(Class entity, String columnPath, Cmp cmp)
            {
                this.entity = entity;
                this.columnPath = columnPath;
                this.cmp = cmp;
            }

            public Filter(Class entity, String columnPath, T... value)
            {
                this.entity = entity;
                this.columnPath = columnPath;
                this.value = value;
            }

            public Filter(Class entity, String columnPath, Cmp cmp, T... value)
            {
                this(entity, columnPath, cmp);
                this.value = value;
            }

            public Filter(Class entity, String columnPath, Cmp cmp, Modifier mod, T[] value)
            {
                this.entity = entity;
                this.columnPath = columnPath;
                this.cmp = cmp;
                this.mod = mod;
                this.value = value;
            }

            public Filter(String name, Class entity, String columnPath, Cmp cmp, T... value)
            {
                this(entity, columnPath, cmp, value);
                this.name = name;
            }

            public String getName()
            {
                return name;
            }

            public Class getEntity()
            {
                return entity;
            }

            public String getColumnPath()
            {
                return columnPath;
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
                return String.format("'%s' (%s) <%s> %s", this.getColumnPath(), this.getEntity() != null ? this.getEntity().getSimpleName() : null, this.getCmp(), Arrays.toString(this.getValue()));
            }
        }
    }

    public static enum QueryType
    {
        DATA, DISTINCT, AGGREGATION;
    }
}
