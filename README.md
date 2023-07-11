# Object-Oriented Data Service
- Open Source JAVA libraries for object-oriented data handling - from API request to DB query back to API response.


## [Object-Oriented Database Service](./database-service/README.md)
- Library implements Object-Oriented Query Language for SQL databases. 
- Wires right onto your existing JPA Entity model.
- It is built on top of JPA Criteria API to provide more intuitive (object-oriented) way of building queries.

### Maven (pom.xml)
```xml
<dependency>
  <groupId>info.nino.jpatron</groupId>
  <artifactId>database-service</artifactId>
  <version>${data-service.version}</version>
</dependency>
```

## [JSON-API Interface Documentation](./jsonapi-interface/README.md)
- Library implements JSON:API REST interface for RestEasy resources (endpoints)
- Hooks onto Database Service provider which enables JSON:API queries like GraphQL

### Maven (pom.xml)
```xml
<dependency>
  <groupId>info.nino.jpatron</groupId>
  <artifactId>jsonapi-interface</artifactId>
  <version>${data-service.version}</version>
</dependency>
```
