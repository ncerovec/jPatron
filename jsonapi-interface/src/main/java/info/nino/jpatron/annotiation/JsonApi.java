package info.nino.jpatron.annotiation;

import javax.ws.rs.NameBinding;

import java.lang.annotation.*;

@Inherited
@NameBinding
//NOTICE: Annotation MUST be @NameBinding otherwise @Provider interceptor/filter will be invoked for all JAX-RS endpoints!
//Name binding is a concept that allows to say to a JAX-RS runtime that a specific
//filter or interceptor will be executed only for a specific resource method.
//When a filter or an interceptor is limited only to a specific resource method we say that it is name-bound.
//Filters and interceptors that do not have such a limitation are called global.
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface JsonApi
{
    Class<?> value() default Object.class;      //DTO Class for Entity
    boolean pagination() default true;          //default pagination (page=1/size=10)
    boolean distinctDataset() default true;     //default only distinct values
    boolean readOnlyDataset() default true;     //default only read values
    boolean allowEntityPaths() default false;   //default not allowed (DTO only)
    String[] allowedPaths() default {"."};      //default allow DTO root fields
    String[] fetchEntityPaths() default {};     //bulk fetch entities (optimize fetching related entities)
    String[] entityGraphPaths() default {};     //preload entities (optimize fetching related entities)

}
