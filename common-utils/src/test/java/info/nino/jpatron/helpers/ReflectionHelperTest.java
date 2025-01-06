package info.nino.jpatron.helpers;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReflectionHelperTest {

    private class GenericTypeClass<T> {}
    private class TypedClass extends GenericTypeClass<String> { }

    @Test
    public void testFindGenericClassParameterTypeOnTypedClass() {
        TypedClass typedClass = new TypedClass();

        var stringTypeTypedClassDef = ReflectionHelper.findGenericClassParameterType(TypedClass.class, GenericTypeClass.class, 0);
        assertEquals(String.class, stringTypeTypedClassDef);

        var stringTypeTypedClassInstance = ReflectionHelper.findGenericClassParameterType(typedClass.getClass(), GenericTypeClass.class, 0);
        assertEquals(String.class, stringTypeTypedClassInstance);
    }

    @Test
    public void testFindGenericClassParameterTypeOnGenericClass() {
        //NOTICE: Using "Anonymous Subclass" for type preservation - due to "Type Erasure" of generic parameters/variables in Java runtime
        GenericTypeClass<Integer> genericClass = new GenericTypeClass<>() {};

        var integerTypeGenericClass = ReflectionHelper.findGenericClassParameterType(genericClass.getClass(), GenericTypeClass.class, 0);
        assertEquals(Integer.class, integerTypeGenericClass);
    }

    private class TypedCollection extends ArrayList<String> { }

    @Test
    public void testFindGenericClassParameterTypeOnTypedCollection() {
        TypedCollection typedCollection = new TypedCollection();

        var stringTypeArrayList = ReflectionHelper.findGenericClassParameterType(typedCollection.getClass(), ArrayList.class, 0);
        assertEquals(String.class, stringTypeArrayList);

        var stringTypeList = ReflectionHelper.findGenericClassParameterType(typedCollection.getClass(), List.class, 0);
        assertEquals(String.class, stringTypeList);
    }

    //NOTICE: very important test class structure
    private interface MockInterface<E> { }
    private interface FakeMockInterface<E> { }
    private interface DummyMockInterface<T> extends MockInterface<T> {}
    private abstract class MockAbstract<E, T> implements FakeMockInterface<E>, MockInterface<T> { }
    private class GenericClass<T, E> extends MockAbstract<T, E> implements FakeMockInterface<T> {}
    private class MockClass extends GenericClass<Integer, String>  {}
    private class TheClass extends MockClass implements DummyMockInterface<String> {}

    @Test
    public void testFindGenericClassParameterTypeOnInterface() {
        TheClass theClass = new TheClass();

        var stringTypeDummyInterface = ReflectionHelper.findGenericClassParameterType(theClass.getClass(), DummyMockInterface.class, 0);
        assertEquals(String.class, stringTypeDummyInterface);

        var integerTypeFakeInterface = ReflectionHelper.findGenericClassParameterType(theClass.getClass(), FakeMockInterface.class, 0);
        assertEquals(Integer.class, integerTypeFakeInterface);

        var stringTypeMockInterface = ReflectionHelper.findGenericClassParameterType(theClass.getClass(), MockInterface.class, 0);
        assertEquals(String.class, stringTypeMockInterface);

        var integerTypeMockAbstract = ReflectionHelper.findGenericClassParameterType(theClass.getClass(), MockAbstract.class, 0);
        assertEquals(Integer.class, integerTypeMockAbstract);

        var stringTypeMockAbstract = ReflectionHelper.findGenericClassParameterType(theClass.getClass(), MockAbstract.class, 1);
        assertEquals(String.class, stringTypeMockAbstract);
    }


    private interface IConverter<I, O> {}
    private static class ToViewConverter<I, O> implements IConverter<I, O> {}
    private static class NameToViewConverter<O> extends ToViewConverter<String, O> {}
    private static class RoleNameToViewConverter extends NameToViewConverter<Integer> {}

    @Test
    public void testFindGenericClassParameterTypeOnDeepInterface() {
        RoleNameToViewConverter converterClass = new RoleNameToViewConverter();

        var type = ReflectionHelper.findGenericClassParameterType(converterClass.getClass(), IConverter.class, 1);
        assertEquals(Integer.class, type);
    }
}
