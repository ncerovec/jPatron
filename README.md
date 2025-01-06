# jPatron - Object-Oriented JPA Data Library
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
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/info.nino.jpatron/jpatron/badge.svg)](https://maven-badges.herokuapp.com/maven-central/info.nino.jpatron/jpatron)
```xml
<dependency>
  <groupId>info.nino.jpatron</groupId>
  <artifactId>database-service</artifactId>
  <version>${jpatron.version}</version>
</dependency>
```

## [JSON-API Interface - Documentation](./json-api-interface/README.md)
- Library implements JSON:API REST interface for RestEasy resources (endpoints)
- Hooks onto jPatron database-service which enables JSON:API requests similar to GraphQL
- Provides custom filtering & searching options using HTTP Query Parameters

### Maven (pom.xml)
```xml
<dependency>
  <groupId>info.nino.jpatron</groupId>
  <artifactId>json-api-interface</artifactId>
  <version>${jpatron.version}</version>
</dependency>
```
## jPatron BOM
 - Easiest way to keep all jPatron artefacts of compatible versions is to include
**jpatron-bom** in `<dependencyManagement>` section of your `pom.xml`
 - When using BOM you don't have to specify version for every other jPatron artefact 
in `<dependencies>` section of your  `pom.xml`, just specify it as `<scope>provided</scope>`
and all jPatron dependencies will follow version of **jpatron-bom** artefact.

### Maven (pom.xml)
```xml
<dependency>
    <groupId>info.nino.jpatron</groupId>
    <artifactId>jpatron-bom</artifactId>
    <version>${jpatron.version}</version>
    <scope>import</scope>
    <type>pom</type>
</dependency>
```