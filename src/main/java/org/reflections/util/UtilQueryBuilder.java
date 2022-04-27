package org.reflections.util;

import org.reflections.ReflectionUtils;
import org.reflections.Store;

import java.lang.reflect.AnnotatedElement;
import java.util.LinkedHashSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * query builder for {@link QueryFunction}
 * <pre>{@code UtilQueryBuilder<Annotation> builder =
 *   element -> store -> element.getDeclaredAnnotations()} </pre>
 */
public interface UtilQueryBuilder<F, E> {
	/** get direct values of given element */
	QueryFunction<Store, E> get(F element);


}
