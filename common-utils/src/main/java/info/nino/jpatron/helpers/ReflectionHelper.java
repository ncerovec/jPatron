package info.nino.jpatron.helpers;

import info.nino.jpatron.annotiation.EntityClass;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class ReflectionHelper
{
    public static final String PATH_SEPARATOR = "."; //PATH_DELIMITER
    private static final List<String> ENTITY_ANNOTATION_CLASSES = Arrays.asList("jakarta.persistence.Entity", "jakarta.persistence.Embeddable");

    public static String getFieldNameFromPath(String fieldPath)
    {
        String fieldName = fieldPath;

        if(fieldPath != null)
        {
            //String[] fieldPaths = fieldPath.split("\\"+ReflectionHelper.PATH_SEPARATOR);
            //if(fieldPaths.length > 0) fieldName = fieldPaths[fieldPaths.length-1];

            int fieldStartIndex = fieldPath.lastIndexOf(ReflectionHelper.PATH_SEPARATOR);
            if(fieldStartIndex >= 0) fieldName = fieldPath.substring(fieldStartIndex+1);
        }

        return fieldName;
    }

    public static String getPathWithoutLastItem(String fieldPath)
    {
        String path = null;

        if(fieldPath != null)
        {
            int fieldStartIndex = fieldPath.lastIndexOf(ReflectionHelper.PATH_SEPARATOR);
            if(fieldStartIndex >= 0) path = fieldPath.substring(0, fieldStartIndex);
        }

        return path;
    }

    public static LinkedList<String> pathToLinkedList(String path)
    {
        String[] paths = {};

        if(path != null) paths = path.split("\\"+ReflectionHelper.PATH_SEPARATOR);

        return new LinkedList<>(Arrays.asList(paths));
    }

    public static Optional<Field> findModelField(Class<?> clazz, String fieldName)
    {
        Optional<Field> field = Optional.empty();

        if(clazz != null)
        {
            try
            {
                field = Optional.of(clazz.getDeclaredField(fieldName));
                //field = Arrays.stream(clazz.getDeclaredFields()).filter(f -> fieldName.equals(f.getName())).findFirst();
            }
            catch(NoSuchFieldException e)
            {
                clazz = clazz.getSuperclass();
                field = ReflectionHelper.findModelField(clazz, fieldName);
            }
        }

        return field;
    }

    /*
    @Deprecated
    public static Optional<Field> findModelFieldByType(Class<?> clazz, Class<?> fieldType)
    {
        Optional<Field> field = Optional.empty();

        if(clazz != null)
        {
            List<Field> foundFields = ReflectionHelper.getAllModelFields(clazz).stream().filter(f ->
            {
                Class<?> fType = f.getType();

                if(Collection.class.isAssignableFrom(f.getType()))    //collection types
                {
                    fType = (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
                }

                return fieldType == fType;
            }).collect(Collectors.toList());

            if(!foundFields.isEmpty())
            {
                //exact type match priority over collection types
                if(foundFields.size() > 1) field = foundFields.stream().filter(f -> fieldType == f.getType()).findFirst();

                if(!field.isPresent()) field = Optional.of(foundFields.get(0));
            }
        }

        return field;
    }
    */

    public static List<Field> getAllModelFields(Class clazz)
    {
        List<Field> fields = new ArrayList<>();

        while(clazz != null)
        {
            Collections.addAll(fields, clazz.getDeclaredFields());
            clazz = clazz.getSuperclass();
        }

        return fields;
    }

    public static Optional<Field> findFieldByPath(Class<?> rootClass, String fieldPath) throws RuntimeException
    {
        Optional<Field> fieldOptional = Optional.empty();

        LinkedList<String> fieldPaths = ReflectionHelper.pathToLinkedList(fieldPath);
        if(!fieldPaths.isEmpty())
        {
            String findField = fieldPaths.removeFirst();

            fieldOptional = ReflectionHelper.findModelField(rootClass, findField);
            //if(!fieldOptional.isPresent()) throw new IllegalStateException(String.format("Field '%s' NOT FOUND in Class: %s!", findField, rootClass.getSimpleName()));
            //else System.out.println(String.format("Field '%s' (%s) FOUND in Class: %s!", findField, fieldOptional.get().getType().getSimpleName(), clazz.getSimpleName()));

            if(fieldOptional.isPresent() && !fieldPaths.isEmpty())
            {
                Field field = fieldOptional.get();
                String remainingPath = String.join(ReflectionHelper.PATH_SEPARATOR, fieldPaths);

                Class<?> nextClass = field.getType();
                if(Collection.class.isAssignableFrom(nextClass)) nextClass = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];

                fieldOptional = ReflectionHelper.findFieldByPath(nextClass, remainingPath);
            }
        }

        return fieldOptional;
    }

    public static Pair<Class<?>, String> findEntityFieldByPath(Class<?> clazz, String path, boolean allowEntityDive) throws RuntimeException
    {
        LinkedList<String> paths = ReflectionHelper.pathToLinkedList(path);
        return ReflectionHelper.findEntityFieldByPath(clazz, paths, null, allowEntityDive);
    }

    private static Pair<Class<?>, String> findEntityFieldByPath(Class<?> clazz, LinkedList<String> paths, String prefixPath, boolean allowEntityDive) throws RuntimeException
    {
        String findField = paths.removeFirst();

        boolean isEntityDive = false;
        //NOTICE: find field in DTO Class (or ENTITY class if recursion already dives into ENTITY Class)
        Optional<Field> fieldOptional = ReflectionHelper.findModelField(clazz, findField);
        if(!fieldOptional.isPresent()) //FALLBACK: find field in ENTITY Class
        {
            //replace DTO with ENTITY Class -> dive into entity field search
            if(clazz.isAnnotationPresent(EntityClass.class)) clazz = clazz.getAnnotation(EntityClass.class).value();
            fieldOptional = ReflectionHelper.findModelField(clazz, findField);
            isEntityDive = true;
        }

        if(!fieldOptional.isPresent()) throw new IllegalStateException(String.format("Field '%s' NOT FOUND in DTO/ENTITY Class: %s!", findField, clazz.getSimpleName()));
        else if(isEntityDive && !allowEntityDive) throw new IllegalStateException("Entity level Field paths NOT ALLOWED!");
        //else logger.info(String.format("Field '%s' (%s) FOUND in DTO/ENTITY Class: %s!", findField, fieldOptional.get().getType().getSimpleName(), clazz.getSimpleName()));

        Field field = fieldOptional.get();
        if(!paths.isEmpty())
        {
            Class<?> nextClass = field.getType();
            if(Collection.class.isAssignableFrom(nextClass))
            {
                nextClass = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                //logger.info(String.format("DTO/ENTITY Class resolved from Collection (%s): %s", nextClass.getSimpleName(), field.getType()));
            }

            Pair<Class<?>, String> pathField = ReflectionHelper.resolveEntityClassAndFieldName(clazz, field);
            prefixPath = (prefixPath != null) ? prefixPath + ReflectionHelper.PATH_SEPARATOR + pathField.getValue() : pathField.getValue();   //current field entity-path

            //String remainingPath = String.join(ReflectionHelper.PATH_SEPARATOR, paths);
            //Pair<Class<?>, String> nextField = ReflectionHelper.findClassFieldByPath(nextClass, remainingPath, allowEntityDive);
            Pair<Class<?>, String> nextField = ReflectionHelper.findEntityFieldByPath(nextClass, paths, prefixPath, allowEntityDive);

            return nextField;
        }
        else
        {
            //NOTICE: method resolveEntityClassAndFieldName() invokes back findFieldByPath() recursion if field is path!
            Pair<Class<?>, String> finalField = ReflectionHelper.resolveEntityClassAndFieldName(clazz, field);

            //extend finalField name with path to root entity
            if(prefixPath != null) finalField.setValue(prefixPath + ReflectionHelper.PATH_SEPARATOR + finalField.getValue());

            return finalField;
        }
    }

    //NOTICE: resolve real entity class & field name - with annotation fallbacks
    private static Pair<Class<?>, String> resolveEntityClassAndFieldName(Class<?> clazz, Field field)
    {
        EntityClass fieldEntityClass = ReflectionHelper.getFieldOrAccessorOrMutatorAnnotation(clazz, field, EntityClass.class);
        if(clazz.isAnnotationPresent(EntityClass.class)) clazz = clazz.getAnnotation(EntityClass.class).value();
        String fieldName = (field.isAnnotationPresent(EntityClass.EntityClassField.class)) ? field.getAnnotation(EntityClass.EntityClassField.class).value() : field.getName();
        if(fieldEntityClass != null) //field (or getter/setter) level @EntityClass
        {
            if(!Object.class.equals(fieldEntityClass.value())) clazz = fieldEntityClass.value();
            if(!fieldEntityClass.fieldPath().isEmpty()) fieldName = fieldEntityClass.fieldPath();
        }

        if(fieldName.contains(ReflectionHelper.PATH_SEPARATOR)) //check if fieldName is path
        {
            Pair<Class<?>, String> pathField = ReflectionHelper.findEntityFieldByPath(clazz, fieldName,true);
            clazz = pathField.getKey();
            fieldName = pathField.getValue();
        }

        ReflectionHelper.verifyIsEntityClass(clazz);
        ReflectionHelper.verifyClassHasField(clazz, fieldName);

        return new ImmutablePair<>(clazz, fieldName);
    }

    public static Class<?> resolveEntityClassFromDtoClass(Class<?> clazz)
    {
        if(clazz.isAnnotationPresent(EntityClass.class)) clazz = clazz.getAnnotation(EntityClass.class).value();

        ReflectionHelper.verifyIsEntityClass(clazz);

        return clazz;
    }

    private static void verifyIsEntityClass(Class<?> clazz)
    {
        boolean isEntityClass = Arrays.stream(clazz.getAnnotations()).anyMatch(a -> ENTITY_ANNOTATION_CLASSES.contains(a.annotationType().getName()));
        if(!isEntityClass) throw new IllegalStateException(String.format("FINAL ENTITY Class is not @Entity/@Embeddable: %s!", clazz.getSimpleName()));
    }

    private static void verifyClassHasField(Class<?> clazz, String fieldName)
    {
        Optional<Field> foundField = ReflectionHelper.findModelField(clazz, ReflectionHelper.getFieldNameFromPath(fieldName));
        if(!foundField.isPresent()) throw new IllegalStateException(String.format("Field '%s' NOT FOUND in FINAL ENTITY Class: %s!", fieldName, clazz.getSimpleName()));
    }

    public static <T extends Annotation> T getFieldOrAccessorOrMutatorAnnotation(Field field, Class<T> annotationClass)
    {
        return ReflectionHelper.getFieldOrAccessorOrMutatorAnnotation(field.getDeclaringClass(), field, annotationClass);
    }

    public static <T extends Annotation> T getFieldOrAccessorOrMutatorAnnotation(Class<?> declaringClass, Field field, Class<T> annotationClass)
    {
        T fieldAnnotation = field.getAnnotation(annotationClass);

        if(fieldAnnotation == null) //if(!field.isAnnotationPresent(annotationClass))
        {
            //resolve @EntityClass from Field getter/setter Method
            BeanInfo clazzBeanInfo = ReflectionHelper.getClassBeanInfo(declaringClass);
            PropertyDescriptor[] clazzProps = clazzBeanInfo.getPropertyDescriptors();

            Optional<PropertyDescriptor> fieldPd = Arrays.stream(clazzProps).filter(pd -> field.getName().equals(pd.getDisplayName())).findAny();
            if(fieldPd.isPresent())
            {
                if(fieldAnnotation == null && fieldPd.get().getReadMethod() != null) fieldAnnotation = fieldPd.get().getReadMethod().getAnnotation(annotationClass);
                if(fieldAnnotation == null && fieldPd.get().getWriteMethod() != null) fieldAnnotation = fieldPd.get().getWriteMethod().getAnnotation(annotationClass);
            }
        }

        return fieldAnnotation;
    }

    private static BeanInfo getClassBeanInfo(Class<?> clazz)
    {
        BeanInfo clazzBeanInfo = null;

        try
        {
            clazzBeanInfo = Introspector.getBeanInfo(clazz);
        }
        catch(IntrospectionException e)
        {
            throw new IllegalStateException(String.format("Error reading BeanInfo for class: %s", clazz.getSimpleName()), e);
        }

        //PropertyDescriptor[] clazzProperties = clazzBeanInfo.getPropertyDescriptors(); //get all the properties for the class
        //Method[] clazzMethods = clazz.getDeclaredMethods(); //get all methods for the class

        return clazzBeanInfo;
    }

    /*
    @Deprecated
    //NOTICE: resolve Entity path field name by field Type - possibly ambiguous Field if more than one relation to same Entity type
    private static String resolveEntityFieldPath(Class<?> clazz, Field field)
    {
        if(clazz.isAnnotationPresent(EntityClass.class)) clazz = clazz.getAnnotation(EntityClass.class).value();

        Class<?> fieldType = field.getType();
        if(field.isAnnotationPresent(EntityClass.class))
        {
            EntityClass fieldEntityClass = field.getAnnotation(EntityClass.class);
            if(!Object.class.equals(fieldEntityClass.value())) fieldType = fieldEntityClass.value();
        }
        if(Collection.class.isAssignableFrom(fieldType)) fieldType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        if(fieldType.isAnnotationPresent(EntityClass.class)) fieldType = fieldType.getAnnotation(EntityClass.class).value();

        String pathFieldName = null;
        boolean isEntityClass = Arrays.stream(fieldType.getAnnotations()).anyMatch(a -> ENTITY_ANNOTATION_CLASSES.contains(a.annotationType().getName()));
        if(isEntityClass)
        {
            Optional<Field> entityField = ReflectionHelper.findModelFieldByType(clazz, fieldType);
            if(!entityField.isPresent()) throw new IllegalStateException(String.format("Field type '%s' NOT FOUND in ENTITY Class: %s!", fieldType.getSimpleName(), clazz.getSimpleName()));
            else pathFieldName = entityField.get().getName();
        }

        return pathFieldName;
    }
    */

    @SuppressWarnings("unchecked")  //TODO: verify this method (might have bugzzz)
    public static <T> Class<T> getGenericClassParameter(final Class<?> parameterizedSubClass, final Class<?> genericSuperClass, final int pos)
    {
        //Mapping from type variables to actual values (classes)
        Map<TypeVariable<?>, Class<?>> mapping = new HashMap<>();

        Class<?> clazz = parameterizedSubClass;
        while(clazz != null)
        {
            ArrayList<Type> types = new ArrayList(Arrays.asList(clazz.getGenericInterfaces()));
            types.add(clazz.getGenericSuperclass());

            for(Type type : types)
            {
                if(type instanceof ParameterizedType)
                {
                    ParameterizedType parType = (ParameterizedType) type;
                    Type rawType = parType.getRawType();

                    if(rawType == genericSuperClass)
                    {
                        //found
                        Type t = parType.getActualTypeArguments()[pos];
                        if(t instanceof Class<?>) return (Class<T>) t;
                        else return (Class<T>) mapping.get((TypeVariable<?>) t);
                    }

                    //resolve
                    Type[] vars = ((GenericDeclaration) (parType.getRawType())).getTypeParameters();
                    Type[] args = parType.getActualTypeArguments();
                    for(int i = 0; i < vars.length; i++)
                    {
                        if(args[i] instanceof Class<?>) mapping.put((TypeVariable) vars[i], (Class<?>) args[i]);
                        else mapping.put((TypeVariable) vars[i], mapping.get((TypeVariable<?>) (args[i])));
                    }

                    clazz = (Class<?>) rawType;
                }
                else
                {
                    //clazz = clazz.getSuperclass();    //NOTICE: was only superclass implementation
                    Type genericType = clazz.getGenericSuperclass();
                    clazz = (genericType instanceof  Class<?>) ? (Class<?>) genericType : clazz.getSuperclass();
                }
            }
        }

        throw new IllegalStateException(String.format("Generic supertype %s for %s class NOT FOUND!", genericSuperClass.getSimpleName(), parameterizedSubClass.getSimpleName()));
    }
}
