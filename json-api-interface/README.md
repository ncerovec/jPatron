# Object-Oriented Data Service


## JsonApiRequest
### Example
```shell
[GET] {{base_url}}/person/json-api
    ?page[number]=1
    &page[size]=1
    &sort=name    
    &filter[name]=Nino
    &meta[id][COUNT]=gender
    &distinct[gender]=
```

## JsonApiResponse
### Example
```json
{
    "data": [
        {
            "id": 1,
            "type": "info.nino.test.dto.PersonDTO",
            "attributes": {
                "id": 1,
                "name": "Nino",
                "lastname": "Cerovec",
                "gender": "male"
            },
            "meta": {}
        }
    ],
    "meta": {
        "metaValues": {
            "personEntityIdByGenderCount": {
                "female": 1,
                "male": 2
            }
        },
        "distinctValues": {
            "personEntityGender": {
                "female": "female",
                "male": "male"
            }
        },
        "page": {
            "total": 3,
            "size": 1,
            "current": 1,
            "pages": 3
        }
    }
}
```

## JSON:API request Query Parameters

### Pagination Query Parameters
#### page[number] = <page-number>
|         **Segment**         | **Description**         |
|:---------------------------:|:------------------------|
| **\*page-number** (Integer) | Number of the Data page |
Examples:
- `page[number]=1`

#### page[size] = <page-size>
|        **Segment**        | **Description**  |
|:-------------------------:|:-----------------|
| **\*page-size** (Integer) | Size of the page |
Examples:
- `page[size]=5`

### Sorting Query Parameters
#### sort = [sort-direction]<column-path> [, [sort-direction]<column-path>, ...]
|          **Segment**           | **Description**                                                                                                                       |
|:------------------------------:|:--------------------------------------------------------------------------------------------------------------------------------------|
| **\*column-path** (String CSV) | DTO/Entity column                                                                                                                     |
|  **sort-direction** (`+`/`-`)  | Order direction prefix: <br/> `+` Ascending (ASC) order-direction prefix (default) <br/> `-` Descending (DESC) order-direction prefix |
Examples:
- `sort=-id`
- `sort=-id,+name,-gender.code`

### Filter Parameter
#### filter[<column-path>][\<comparator\>] = [<filter-value>]
|        **Segment**         | **Description**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
|:--------------------------:|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **\*column-path** (String) | DTO/Entity model path from requested root-entity to filter-column (only root DTO properties are allowed by default) - filter property <br/> - DTO paths correspond to JSON:API “attributes” parameter properties <br/> - Entity paths correspond to underlying entity model properties                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|   **comparator** (Enum)    | Compare operator for filtering <br/><br/> Single-Value comparators: <br/> `EQ` (default) - Column EQUALS value (filter-value parsed) <br/> `NEQ` - Column NOT-EQUALS value (filter-value parsed) <br/> `LIKE` - Column LIKE value (filter-value parsed) <br/> `GT` - Column GREATER-THAN value (filter-value parsed) <br/> `LT` - Column LESS-THAN value (filter-value parsed) <br/> `GToE` - Column GREATER-THAN or EQUALS value (filter-value parsed) <br/> `LToE` - Column LESS-THAN or EQUALS value (filter-value parsed) <br/><br/> Multi-Value comparators: <br/> `IN` - Column IN values (multiple filter-value) <br/> `NotIN` - Column NOT-IN values (multiple filter-value) <br/><br/> Non-Value comparators: <br/> `TRUE` - Column is TRUE (filter-value ignored) <br/> `FALSE` - Column is FALSE (filter-value ignored) <br/> `IsNULL` - Column is NULL (filter-value ignored) <br/> `IsNotNULL` - Column is NOT-NULL (filter-value ignored) <br/> `IsEMPTY` - Column Collection is EMPTY (filter-value ignored) <br/> `IsNotEMPTY` - Column Collection is NOT-EMPTY (filter-value ignored)     |
| **filter-value** (String)  | Value used for filtering by specified column and comparator (URL encoded values) <br/> - multiple values are supported as separate query parameter with same column-path & comparator but different filter-value <br/> - multi-value comparators (IN, NOT-IN) <br/> - single-value comparators (each value is added with OR operator)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
Examples:
- `filter[age][GToE]=5`
- `filter[gender.id][LToE]=2` 
- `filter[gender.code]=m` 
- `filter[name][LIKE]=%25Nino%25` (`%25`=`%` URL encoded representation)
- `filter[dateCreated][GToE]=2020-01-01T00:00:00.000Z` 
- `filter[dateCreated][LToE]=2020-12-31T23:59:59.999Z`

### Search Parameter
#### search[column-path][modifier] = <search-value>
|        **Segment**         | **Description**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|:--------------------------:|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **\*column-path** (String) | DTO/Entity model path from requested root-entity to search-column (only root DTO properties are allowed by default) - search property <br/> - DTO paths correspond to JSON:API “attributes” parameter properties <br/> - Entity paths correspond to underlying entity model properties                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|    **modifier** (Enum)     | Modifier operator for search-value keywords <br/><br/> `LikeLR` (default) - appends '%' to both ends of search-value (%search value%) <br/> `LikeL` - appends '%' to left side of search-value (%search value) <br/> `LikeR` - appends '%' to right side of search-value (search value%) <br/> `SPLIT` - splits search-value by spaces into keywords (search,value) <br/> `SplitLikeLR` - splits search-value by spaces & appends '%' to both ends of each search-value keyword (%search%,%value%) <br/> `SplitLikeL` - splits search-value by spaces & appends '%' to left side of each search-value keyword (%search,%value) <br/> `SplitLikeR` - splits search-value by spaces & appends '%' to right side of each search-value keyword (%search,%value) <br/> `NONE` - no modification of search-value (search value) <br/> |
| **search-value** (String)  | Value used for searching by specified column with applied modifier (URL encoded values) <br/> - each search-value is added with OR operator <br/> - multiple values are supported as separate query parameter with same column-path, where modifier & search-value can be different                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
Examples: 
- `search[name]=Nino`
- `search[name][SplitLikeLR]=Nino Cerovec`

### Distinct Values Query Parameters
#### distinct[key-column-path] = [value-column-path]
|      **Segment**       | **Description**                                                                                                                                                                                                                                                                 |
|:----------------------:|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **\*key-column-path:** | DTO/Entity model path from requested root-entity to key-column (only root DTO properties are allowed by default) - key property of distinct-values                                                                                                                              |
| **value-column-path:** | DTO/Entity model path from requested root-entity to value-column (only root DTO properties are allowed by default) - value property of distinct-values <br/> - WARNING: multiple values per key are not allowed (each distinct key must have unique value in requested dataset) |
Examples:
- `distinct[id]=name`
- `distinct[gender.id]`
- `distinct[gender.id]=gender.name`

### Meta Values Query Parameters
#### meta[value-column-path][agg-function] = [group-by-column-path]
|        **Value**         | **Description**                                                                                                                                                                                                                                                                                                                         |
|:------------------------:|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **\*value-column-path:** | DTO/Entity model path from requested root-entity to value-column (only root DTO properties are allowed by default) - value property to be calculated using aggregation function                                                                                                                                                         |
|      **agg-function:**       | Aggregation function for meta-values <br/><br/> `COUNT` (default) - count of value-column values <br/> `COUNT_DISTINCT` - count of distinct value-column values <br/> `SUM` - sum of value-column values <br/> `AVG` - average of value-column values <br/> `MIN` - minimum value-column value <br/> `MAX` - maximum value-column value |
| **group-by-column-path** | DTO/Entity model path from requested root-entity to group-by-column (only root DTO properties are allowed by default) - group-by property to separate/group meta-values by its values                                                                                                                                                   |
Examples:
- `meta[gender.id]`
- `meta[gender.id][COUNT_DISTINCT]=gender.name`
- `meta[id][COUNT_DISTINCT]=dataStatus`
- `meta[dateCreated][MIN]`
- `meta[dateCreated][MAX]`


## @JsonApi annotation
Used for declaring and configuring JSON:API endpoints.

### Pagination
- Pagination is enabled By default, and possible using page query parameters.
  - Default pagination (page=1/size=10)
  - Query parameter `page[size]` can be used standalone to limit the size of dataset result.
  - Query parameter `page[number]` has to be used in combination with page[size] query parameter.
- Pagination can be disabled through @JsonApi(pagination=false) annotation parameter, in which case query returns all results in single request.
  - If disabled, pagination can still be forced through JSON:API using page query parameters.

### Sorting
- Sorting is always available and possible using sort query parameter.
  - Only root DTO properties are available as sort parameters.
    - In order to sort by nested Entity model properties Result Dataset DISTINCT must be disabled using `@JsonApi(distinct=false)` annotation parameter (enabled by default).
    - [Entity paths](#Entity-paths) & [Allowed paths](#Allowed-paths) enables extending available DTO/Entity properties.
  - Sorting is single query parameter with CSV values (in order of sorting sequence) of sorting properties prefixed by order direction parameter: + (ASC) or - (DESC).

### Filtering
- Filtering is always available and possible using filter query parameters.
  - By default, only root DTO properties are available as filter parameters.
    - [Entity paths](#Entity-paths) & [Allowed paths](#Allowed-paths) enables extending available DTO/Entity properties.
  - Each filter parameter value will be added using applied comparator with AND logical operator to root Query filtering conditions.
  - Multi-value filter parameter (multiple parameters with same filter-column & comparator) will be merged into single compound filter using OR logical operator (in case of single-value comparators) and compound will be added with AND logical operator to root Query filtering conditions.
    - In case of multi-value comparators (IN/NotIN) multiple filter values are expected and work accordingly.
    - In case of non-value comparators (TRUE/FALSE/IsNULL/IsNotNULL/IsEMPTY/IsNotEMPTY) filter will be added multiple times (using AND) but with no further effect than single filter (same behaviour).

### Searching
- Searching is always available and possible using search query parameters.
  - By default, only root DTO properties are available as filter parameters.
    - [Entity paths](#Entity-paths) & [Allowed paths](#Allowed-paths) enables extending available DTO/Entity properties.
  - Each search parameter value will be added using LIKE comparator with OR logical operator to root Query filtering conditions.
  - Each search parameter value will be changed according to applied modifier before adding to Query filtering conditions.

### Result Dataset
- Result Dataset is DISTINCT by default, this can be disabled using `@JsonApi(distinctDataset=false)` annotation parameter.
- Result Dataset is READ-ONLY (over JSON:API) by default, this can be disabled using `@JsonApi(readOnlyDataset=false)` annotation parameter.
This parameter is only relevant for backend Entity values handling, it’s only purpose is to optimize JSON:API WS request performance.

### Distinct Values
- Distinct Values enables you to fetch distinct data values from Result Dataset which is suitable for filters and other types of data overview applications.
- Distinct Values are always available and possible using `distinct` query parameters.
  - By default, only root DTO properties are available as distinct parameters.
    - [Entity paths](#Entity-paths) & [Allowed paths](#Allowed-paths) enables extending available DTO/Entity properties.
- Each `distinct` parameter will return as object of key-values in JSON:API response at path `meta.distinctValues` with generated property name by source Class & Key-Property.

### Meta Values
- Meta Values enables you to fetch aggregated data values from Result Dataset such as Count, Distinct Count, Sum, Average… 
These values are suitable for KPI and other data analytics.
- Meta Values are always available and possible using meta query parameters.
  - By default, only root DTO properties are available as meta parameters.
    - [Entity paths](#Entity-paths) & [Allowed paths](#Allowed-paths) enables extending available DTO/Entity properties.
- Each meta parameter will return as single or multiple (if group-by parameter) key-value in JSON:API response at path `meta.metaValues` 
with generated property name by source Class, Value-Property, Group-By-Property & Aggregation-Function.

### Entity paths
- By default, only DTO paths are available as column-path in filter, distinct and meta Query parameters. 
- Entity paths can be enabled through `@JsonApi(allowEntityPaths=true)` annotation parameter, in which case full DTO and Entity model paths are available as column-path in Query parameters.

### Allowed paths
- By default, only root DTO/Entity paths are available as column-path in filter, distinct and meta Query parameters. 
- Allowed paths can be extended through `@JsonApi(allowedPaths={".", …})` annotation parameter array.
- `@JsonApi(allowedPaths)` annotation parameter array is collection of allowed paths which supports wildcards:
  - `.` - all root properties of current DTO/Entity path 
    - e.g. “.“ - allows all root properties of exposed object
    - e.g. “.gender.“ - allows all root properties of ‘gender' object
  - `.\*` - all root and subsequent properties of current DTO/Entity path
    - e.g. “.\*“ - allows every single property (including nested objects) of exposed object
    - e.g. “.gender.\*“ - allows all (including nested objects) properties of ‘gender’ object

## @EntityClass annotation
@EntityClass annotation is used to link DTO classes/fields with their belonging Entity classes/fields.

### Linking DTO to Entity
- Every DTO class used in JSON:API has to be annotated with `@EntityClass(Entity.class)` where value parameter is its corresponding Entity class, and fieldPath parameter is ignored (used only for fields).
  - `@EntityClass(PersonEntity.class)` - PersonDTO class annotation

### Linking DTO field to Entity field
- Every DTO field which name is not exactly the same as its corresponding Entity field 
has to be annotated with `@EntityClass(fieldPath="entity-field-path")` where fieldPath 
parameter corresponds to field path in Entity model.
  - `@EntityClass(fieldPath="personalIdentificationNumber")` - `PersonDTO.pid` field annotation (alias mapping)
  - `@EntityClass(fieldPath="gender.name")` - `PersonDTO.gender` field annotation (path mapping)
- Alternatively (not recommended), DTO field can be annotated with `@EntityClass(value=ChildEntity.class, fieldPath="field-name")` 
where value parameter is fields root Entity class, and fieldPath parameter is corresponding field name in Entity model.
  - `@EntityClass(value=GenderEntity.class, fieldPath="name")` - `PersonDTO.gender` field annotation (corresponds to `@EntityClass(fieldPath="gender.name")`)

## JSON:API Endpoint
```java
@RequestScoped
@Path("/person")
public class PersonEndpoint
{
    @Inject
    @JsonApiInject
    private JsonApiRequest jsonApiRequest;

    //Needed for Quarkus implementation  
    //NOTICE: https://quarkus.io/guides/cdi#events-and-observers
    public void onTaskCompleted(@Observes JsonApiRequest jsonApiRequest)
    {
        this.jsonApiRequest = jsonApiRequest;
    }

    @GET
    @Path("/json-api")
    @JsonApi(PersonDTO.class)    
    @Produces(JsonApiMediaType.APPLICATION_JSON_API)    
    public JsonApiResponseList<PersonDTO> getJsonApiPersonList()
    {
        //Deserialized JSON:API request converted to PageRequest for EntityService
        PageRequest pageReq = new PageRequest(this.jsonApiRequest);

        //EntityService.dataQuery call with PageRequest (PersonService implements EntityService interface)
        Page<PersonEntity> dataPage = personService.dataQuery(pageReq);

        //Convert Page<PersonEntity> to Page<PersonDTO> type
        Page<PersonDTO> page = dataPage.convert(personMapper::mapToDto);
        
        //wrap & return PersonDTO into JSON:API response (JsonApiResponseList)
        return new JsonApiResponseList<PersonDTO>(page);
    }
}
```