package info.nino.jpatron.annotiation;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface EntityClass
{
    Class<?> value() default Object.class;
    String fieldPath() default "";  //NOTICE: don't use @EntityClass.value when using fieldPath as path value (only with single field name)

    @Deprecated
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface EntityClassField
    {
        String value() default "";
    }
}
