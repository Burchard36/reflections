package org.reflections;

import org.reflections.util.QueryFunction;
import org.reflections.util.UtilQueryBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ReflectionUtils {

    /** get type elements {@code <T>} by applying {@link QueryFunction} <pre>{@code get(SuperTypes.of(type))}</pre> */
    public static <C, T> Set<T> get(QueryFunction<C, T> function) {
        return function.apply(null);
    }

    /** get type elements {@code <T>} by applying {@link QueryFunction} and {@code predicates} */
    public static <T> Set<T> get(QueryFunction<Store, T> queryFunction, Predicate<? super T>... predicates) {
        return get(queryFunction.filter(Arrays.stream((Predicate[]) predicates).reduce(t -> true, Predicate::and)));
    }


    /** query super class <pre>{@code get(SuperClass.of(element)) -> Set<Class<?>>}</pre>
     * <p>see also {@link ReflectionUtils#SuperTypes}, {@link ReflectionUtils#Interfaces} */
    public static final UtilQueryBuilder<Class<?>, Class<?>> SuperClass =
        element -> ctx -> {
            Class<?> superclass = element.getSuperclass();
            return superclass != null && !superclass.equals(Object.class) ? Collections.singleton(superclass) : Collections.emptySet();
        };

    /** query interfaces <pre>{@code get(Interfaces.of(element)) -> Set<Class<?>>}</pre> */
    public static final UtilQueryBuilder<Class<?>, Class<?>> Interfaces =
        element -> ctx -> Stream.of(element.getInterfaces()).collect(Collectors.toCollection(LinkedHashSet::new));

    /** query super classes and interfaces including element <pre>{@code get(SuperTypes.of(element)) -> Set<Class<?>> }</pre> */
    public static final UtilQueryBuilder<Class<?>, Class<?>> SuperTypes =
        new UtilQueryBuilder<Class<?>, Class<?>>() {
            @Override
            public QueryFunction<Store, Class<?>> get(Class<?> element) {
                return SuperClass.get(element).add(Interfaces.get(element));
            }

        };

    /** query annotations <pre>{@code get(Annotation.of(element)) -> Set<Annotation> }</pre> */
    public static final UtilQueryBuilder<AnnotatedElement, Annotation> Annotations =
        new UtilQueryBuilder<AnnotatedElement, Annotation>() {
            @Override
            public QueryFunction<Store, Annotation> get(AnnotatedElement element) {
                return ctx -> Arrays.stream(element.getAnnotations()).collect(Collectors.toCollection(LinkedHashSet::new));
            }


        };

    /** get the immediate supertype and interfaces of the given {@code type}
     * <p>marked for removal, use instead {@code get(SuperTypes.get())} */
    public static Set<Class<?>> getSuperTypes(Class<?> type) {
        return get(SuperTypes.get(type));
    }


    /** get annotations of given {@code type}, optionally honorInherited, optionally filtered by {@code predicates}
     * <p>marked for removal, use instead {@code get(Annotations.get())} */
    public static <T extends AnnotatedElement> Set<Annotation> getAnnotations(T type, Predicate<Annotation>... predicates) {
        return get(Annotations.get(type), predicates);
    }

}
