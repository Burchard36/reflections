package org.reflections;

import javassist.bytecode.ClassFile;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.NameHelper;
import org.reflections.util.QueryFunction;
import org.reflections.vfs.Vfs;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.reflections.scanners.Scanners.*;

public class Reflections implements NameHelper {

    protected final transient Configuration configuration;
    protected final Store store;

    /**
     * constructs Reflections instance and scan according to the given {@link org.reflections.Configuration}
     * <p>it is preferred to use {@link org.reflections.util.ConfigurationBuilder} <pre>{@code new Reflections(new ConfigurationBuilder()...)}</pre>
     */
    public Reflections(Configuration configuration) {
        this.configuration = configuration;
        Map<String, Map<String, Set<String>>> storeMap = scan();
        if (configuration.shouldExpandSuperTypes()) {
            System.out.println("Should expand!");
            expandSuperTypes(storeMap.get(SubTypes.index()), storeMap.get(TypesAnnotated.index()));
        }
        store = new Store(storeMap);
    }

    public Reflections(String prefix) {
        this((Object) prefix);
    }

    /**
     * Convenient constructor for Reflections.
     * <p></p>see the javadoc of {@link ConfigurationBuilder#build(Object...)} for details.
     * <p></p><i>it is preferred to use {@link org.reflections.util.ConfigurationBuilder} instead.</i> */
    public Reflections(Object... params) {
        this(ConfigurationBuilder.build(params));
    }

    protected Reflections() {
        configuration = new ConfigurationBuilder();
        store = new Store(new HashMap<>());
    }

    protected Map<String, Map<String, Set<String>>> scan() {
        long start = System.currentTimeMillis();
        Map<String, Set<Map.Entry<String, String>>> collect = configuration.getScanners().stream().map(Scanner::index).distinct()
            .collect(Collectors.toMap(s -> s, s -> Collections.synchronizedSet(new HashSet<>())));
        Set<URL> urls = configuration.getUrls();

        (configuration.isParallel() ? urls.stream().parallel() : urls.stream()).forEach(url -> {
            Vfs.Dir dir = null;
            try {
                dir = Vfs.fromURL(url);
                for (Vfs.File file : dir.getFiles()) {
                    if (doFilter(file, configuration.getInputsFilter())) {
                        ClassFile classFile = null;
                        for (Scanner scanner : configuration.getScanners()) {
                            try {
                                if (doFilter(file, scanner::acceptsInput)) {
                                    List<Map.Entry<String, String>> entries = scanner.scan(file);
                                    if (entries == null) {
                                        if (classFile == null) classFile = getClassFile(file);
                                        entries = scanner.scan(classFile);
                                    }
                                    if (entries != null) collect.get(scanner.index()).addAll(entries);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (dir != null) dir.close();
            }
        });

        // merge
        Map<String, Map<String, Set<String>>> storeMap =
            collect.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().stream().filter(e -> e.getKey() != null)
                        .collect(Collectors.groupingBy(
                            Map.Entry::getKey,
                            HashMap::new,
                            Collectors.mapping(Map.Entry::getValue, Collectors.toSet())))));
        return storeMap;
    }

    private boolean doFilter(Vfs.File file, Predicate<String> predicate) {
        String path = file.getRelativePath();
        String fqn = path.replace('/', '.');
        return predicate == null || predicate.test(path) || predicate.test(fqn);
    }

    private ClassFile getClassFile(Vfs.File file) {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(file.openInputStream()))) {
            return new ClassFile(dis);
        } catch (Exception e) {
            throw new ReflectionsException("could not create class object from file " + file.getRelativePath(), e);
        }
    }

    /**
     * expand super types after scanning, for super types that were not scanned.
     * <br>this is helpful for finding the transitive closure without scanning all 3rd party dependencies.
     * <p></p>
     * for example, for classes A,B,C where A supertype of B, B supertype of C (A -> B -> C):
     * <ul>
     *     <li>if scanning C resulted in B (B->C in store), but A was not scanned (although A is a supertype of B) - then getSubTypes(A) will not return C</li>
     *     <li>if expanding supertypes, B will be expanded with A (A->B in store) - then getSubTypes(A) will return C</li>
     * </ul>
     */
    public void expandSuperTypes(Map<String, Set<String>> subTypesStore, Map<String, Set<String>> typesAnnotatedStore) {
        if (subTypesStore == null || subTypesStore.isEmpty()) return;
        Set<String> keys = new LinkedHashSet<>(subTypesStore.keySet());
        keys.removeAll(subTypesStore.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()));
        keys.remove("java.lang.Object");
        for (String key : keys) {
            Class<?> type = forClass(key, loaders());
            if (type != null) {
                expandSupertypes(subTypesStore, typesAnnotatedStore, key, type);
            }
        }
    }

    private void expandSupertypes(Map<String, Set<String>> subTypesStore,
              Map<String, Set<String>> typesAnnotatedStore, String key, Class<?> type) {
        Set<Annotation> typeAnnotations = ReflectionUtils.getAnnotations(type);
        if (typesAnnotatedStore != null && !typeAnnotations.isEmpty()) {
            String typeName = type.getName();
            for (Annotation typeAnnotation : typeAnnotations) {
                String annotationName = typeAnnotation.annotationType().getName();
                typesAnnotatedStore.computeIfAbsent(annotationName, s -> new HashSet<>()).add(typeName);
            }
        }
        for (Class<?> supertype : ReflectionUtils.getSuperTypes(type)) {
            String supertypeName = supertype.getName();
            if (subTypesStore.containsKey(supertypeName)) {
                subTypesStore.get(supertypeName).add(key);
            } else {
                subTypesStore.computeIfAbsent(supertypeName, s -> new HashSet<>()).add(key);
                expandSupertypes(subTypesStore, typesAnnotatedStore, supertypeName, supertype);
            }
        }
    }

    /**
     * apply {@link QueryFunction} on {@link Store}
     * <pre>{@code Set<T> ts = get(query)}</pre>
     * <p>use {@link Scanners} and {@link ReflectionUtils} query functions, such as:
     * <pre>{@code
     * Set<String> annotated = get(Scanners.TypesAnnotated.with(A.class))
     * Set<Class<?>> subtypes = get(Scanners.SubTypes.of(B.class).asClass())
     * Set<Method> methods = get(ReflectionUtils.Methods.of(B.class))
     * }</pre>
     */
    public <T> Set<T> get(QueryFunction<Store, T> query) {
        return query.apply(store);
    }

    /**
     * gets all subtypes in hierarchy of a given {@code type}.
     * <p>similar to {@code get(SubTypes.of(type))}
     * <p></p><i>depends on {@link Scanners#SubTypes} configured</i>
     */
    public <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
        //noinspection unchecked
        return (Set<Class<? extends T>>) get(SubTypes.of(type)
            .as((Class<? extends T>) Class.class, loaders()));
    }

    ClassLoader[] loaders() { return configuration.getClassLoaders(); }
}
