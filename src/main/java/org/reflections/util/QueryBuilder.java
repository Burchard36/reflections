package org.reflections.util;

import org.reflections.Store;

import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

/** builder for store query <pre>{@code QueryBuilder builder = element -> store -> Set<String>}</pre> */
public interface QueryBuilder extends NameHelper {

	default String index() { return getClass().getSimpleName(); }

	/** direct values indexed for {@code key} String
	 * <p>safely returns an empty {@code Set<String>} if {@code index/key} not found
	 * <p>this is the only function accessing the {@link Store} multimap */
	default QueryFunction<Store, String> get(String key) {
		return store -> new LinkedHashSet<>(store.getOrDefault(index(), Collections.emptyMap()).getOrDefault(key, Collections.emptySet()));
	}


	/** transitive values indexed for {@code keys} String collection, not including {@code keys} */
	default QueryFunction<Store, String> getAll(Collection<String> keys) { return QueryFunction.set(keys).getAll(this::get); }

	/** transitive values indexed for {@code AnnotatedElement} varargs, not including */
	default QueryFunction<Store, String> of(AnnotatedElement... elements) { return getAll(toNames(elements)); }

}
