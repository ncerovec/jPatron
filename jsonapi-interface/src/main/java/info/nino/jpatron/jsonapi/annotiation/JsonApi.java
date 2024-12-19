package info.nino.jpatron.jsonapi.annotiation;

import jakarta.ws.rs.NameBinding;

import java.lang.annotation.*;

/**
 * Annotation for tagging JSON:API endpoints
 * Binds to JsonApiRequestFilter which will automatically deserialize HTTP query-parameters to JsonApiRequest object
 *
 */
@Inherited
@NameBinding
//NOTICE: Annotation MUST be @NameBinding otherwise @Provider interceptor/filter will be invoked for all JAX-RS endpoints!
//Name binding is a concept that allows to say to a JAX-RS runtime that a specific filter or interceptor will be executed only for a specific resource method.
//When a filter or an interceptor is limited only to a specific resource method we say that it is name-bound. Filters and interceptors that do not have such limitation are called global.
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface JsonApi
{
    /**
     * Response DTO Class - endpoint result-list DTO Class (must have @EntityClass annotation)
     * @return Class of the Response result-list
     */
    Class<?> value() default Object.class;

    /**
     * Flag which determines if result list should be paginated
     * True by default - default pagination parameters (page=1/size=10)
     * @return boolean flag (enabled/disabled) indicating endpoint returns paginated values
     */
    boolean pagination() default true;

    /**
     * Flag which determines if result list should be distinct
     * True by default - result list values will be de-duplicated
     * @return boolean flag (enabled/disabled) indicating endpoint returns distinct values
     */
    boolean distinctDataset() default true;

    /**
     * Flag which determines if result list should be read-only
     * This is mainly performance optimization flag - it means result values will NOT be used for any other CRUD operation (except READ)
     * True by default - result list will be read-only
     * @return boolean flag (enabled/disabled) indicating endpoint returns read-only values
     */
    boolean readOnlyDataset() default true;

    /**
     * Flag which determines if Entity field-paths (alongside DTO field-paths) are allowed in request
     * Forbidden by default - DTO field-paths only
     * @return boolean flag (allowed/forbidden) indicating permission to do entity-dive while resolving request field-paths
     */
    boolean allowEntityPaths() default false;

    /**
     * List of allowed field-paths in request - represented as exact or wildcard values
     * By default only root fields of the result list DTO object are allowed ('.')
     * '.' - matches all fields in current path
     * '*' - matches all field in current and subsequent paths
     * 'object.field' - exact path/field matching where 'object' is nested-object in the root of result-object and 'field' is exact field name in that object
     * e.g. 'person.*' - path matches all fields under the 'person' object (including all field in the nested objects)
     * e.g. 'person.' - path matches any field directly in the 'person' object (but none of the fields in the nested objects)
     * e.g. 'person.*.number' - path matches any 'number' field in any of the nested objects inside the person object (except in object 'person')
     * @return array of allowed DTO/Entity field-paths
     */
    String[] allowedPaths() default {"."};

    /**
     * List of entity-paths which should be eager-loaded alongside result DTO object - bulk fetch entities (optimize fetching related entities)
     * This is performance optimization property - ensures related entities are eager-loaded (meant for related objects which will be immediately used)
     * @return array of related entity-paths
     */
    String[] fetchEntityPaths() default {};

    /**
     * List of entity-paths which should be eager-loaded alongside result DTO object - preload entities (optimize fetching related entities)
     * This is performance optimization property - ensures related entities are eager-loaded (meant for related objects which will be immediately used)
     * @return array of related entity-paths
     */
    String[] entityGraphPaths() default {};
}
