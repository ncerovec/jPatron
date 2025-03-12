## EntityService
EntityService is JPA Criteria API implementation of generic DB entity querying mechanism. 
EntityService supports generic pagination, filtering, distinct-query & meta-query 
over full Entity model structure with automatic Joins while using related Entities.

### EntityService interface
//TODO describe: EntityService<> interface description

### Entity Manager scope
//TODO describe: EntityService.getEntityManager() mandatory method description

### Entity scope
//TODO describe: EntityService.getEntityClass() optional method description

### Static/Fixed Query Joins
//TODO describe: EntityService.extendFromQuery() optional method description

### Data Query
//TODO describe: EntityService.dataQuery() optional method description

### Distinct Query
//TODO describe: EntityService.distinctQuery() optional method description

### Meta Query
//TODO describe: EntityService.metaQuery() optional method description

- `QueryExpression.labelColumnPath` can accept multiple column-path values separated by comma (`,`) in order to group result meta values by multiple columns.

```java
@Stateless
//@LocalBean //WARNING JavaEE: JavaBean has to be annotated as @LocalBean when implementing interface!
public class PersonService implements EntityService<PersonEntity>
{
    @PersistenceContext//(unitName = "primary")
    EntityManager em;

    @Override
    public EntityManager getEntityManager()
    {
        return this.em;
    }
}
```

//TODO: Static filters/distinct/meta definitions

//TODO: Static filters/distinct/meta examples

//TODO: Compound QueryExpression.Conditional expression definition

### <a name="sysnetguidelineforjson:apiws-cross-joindefinitions-usingunrelated/unasociatedentity"></a>**Cross-Join definitions - using unrelated/unasociated Entity**
- Simulates LEFT OUTER JOIN using CROSS-JOIN:
  - Avoid if possible (this is more like a gimmick than a feature) 
  - Cartesian product of base Entity and related entity by specified relation.
  - Returns all matching records by specified relation or any record with NO related table values (avoid missing base Entity records)
- Example: 
  - `(personEntity.id*entityId).sourceType.name`
- NOT RECOMMENDED for large datasets - significantly reduces performance (slow query)!
- BUG WARNING: cross-join restriction not verified 
  - same cross-join Entity over different field/path will use existing cross-join relation.
  - make sure cross-join Entity is used over only one single relation.
- BUG WARNING: records without cross-join references will have wildcard join values
  - any value from cross-join table - result of Cartesian product join-constraint with OR NOT EXISTS (left-join simulation).
  - make sure each Entity record has at least one join value in cross-join table to avoid bug.
- BEST PRACTICE: recommended Entity model mapping of unrelated associations
  - @JoinColumn + @Where (if needed)
  - FetchType.LAZY (avoid unnecessary eager data-loading DB queries)
  - ```java
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name="entity_id", referencedColumnName="id", insertable=false, updatable=false)
    @Where(clause = "(SELECT se.class_name FROM source_entity se WHERE se.id = source_entity_id)='info.nino.test.model.PersonEntity'")
    private Set<SourceData> sourceDataSet;
    ```
  - This kind of mapping also enables DTO field mapping to Entity fields out of the box - simple @EntityClass annotation.

