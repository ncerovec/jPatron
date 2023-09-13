# jPatron - Object-Oriented JPA Data Framework
- Open Source JAVA libraries for object-oriented data handling - from API request to DB query back to API response.


## [Object-Oriented Database Service - Documentation](./database-service/README.md)
- Library implements powerful Query Engine built on top of JPA/Hibernate Criteria API
- Wires right onto your existing JPA/Hibernate entity model (no changes required)
- Provides intuitive Object-Oriented Query Language (OQL) for SQL databases.
- Simplifies building your DB queries using OQL based on your entity model paths
- OQL provides advanced filtering & search options (mostly anything SQL supports) 
- Supports Distinct & Aggregate queries, alongside regular Data query
- Query Engine will convert your OQL to JPA Criteria query effortlessly

### Maven (pom.xml)
```xml
<dependency>
  <groupId>info.nino.jpatron</groupId>
  <artifactId>database-service</artifactId>
  <version>${data-service.version}</version>
</dependency>
```

## [JSON-API Interface - Documentation](./jsonapi-interface/README.md)
- Library implements JSON:API REST interface for RestEasy resources (endpoints)
- Hooks onto jPatron database-service which enables JSON:API requests similar to GraphQL
- Provides custom filtering & searching options using HTTP Query Parameters
- 

### Maven (pom.xml)
```xml
<dependency>
  <groupId>info.nino.jpatron</groupId>
  <artifactId>jsonapi-interface</artifactId>
  <version>${data-service.version}</version>
</dependency>
```
