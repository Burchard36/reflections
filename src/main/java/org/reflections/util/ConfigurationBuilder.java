package org.reflections.util;

import org.reflections.Configuration;
import org.reflections.ReflectionsException;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.Scanners;

import java.net.URL;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ConfigurationBuilder implements Configuration {
    public static final Set<Scanner> DEFAULT_SCANNERS = new HashSet<>(Collections.singletonList(Scanners.SubTypes));
    public static final Predicate<String> DEFAULT_INPUTS_FILTER = t -> true;

    private Set<Scanner> scanners;
    private Set<URL> urls;
    private Predicate<String> inputsFilter;
    private boolean isParallel = true;
    private ClassLoader[] classLoaders;
    private boolean expandSuperTypes = true;

    public ConfigurationBuilder() {
        urls = new HashSet<>();
    }

    /** constructs a {@link ConfigurationBuilder}.
     * <p>each parameter in {@code params} is referred by its type:
     * <ul>
     *     <li>{@link String} - add urls using {@link ClasspathHelper#forPackage(String, ClassLoader...)} and an input filter
     *     <li>{@link Scanner} - use scanner, overriding default scanners
     *     <li>{@link URL} - add url for scanning
     *     <li>{@link Predicate} - set/override inputs filter
     *     <li>{@link ClassLoader} - use these classloaders in order to find urls using ClasspathHelper and for resolving types
     *     <li>{@code Object[]} - flatten and use each element as above
     * </ul>
     * input filter will be set according to given packages
     * <p></p><i>prefer using the explicit accessor methods instead:</i>
     * <pre>{@code new ConfigurationBuilder().forPackage(...).setScanners(...)}</pre>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ConfigurationBuilder build(Object... params) {
        final ConfigurationBuilder builder = new ConfigurationBuilder();

        // flatten
        List<Object> parameters = new ArrayList<>();
        for (Object param : params) {
            if (param.getClass().isArray()) { for (Object p : (Object[]) param) parameters.add(p); }
            else if (param instanceof Iterable) { for (Object p : (Iterable) param) parameters.add(p); }
            else parameters.add(param);
        }

        ClassLoader[] loaders = Stream.of(params).filter(p -> p instanceof ClassLoader).distinct().toArray(ClassLoader[]::new);
        if (loaders.length != 0) { builder.addClassLoaders(loaders); }

        FilterBuilder inputsFilter = new FilterBuilder();
        builder.filterInputsBy(inputsFilter);

        for (Object param : parameters) {
            if (param instanceof String && !((String) param).isEmpty()) {
                builder.forPackage((String) param, loaders);
                inputsFilter.includePackage((String) param);
            }  else throw new ReflectionsException("could not use param '" + param + "'");
        }

        if (builder.getUrls().isEmpty()) {
            // scan all classpath if no urls provided todo avoid
            builder.addUrls(ClasspathHelper.forClassLoader(loaders));
        }

        return builder;
    }

    public ConfigurationBuilder forPackage(String pkg, ClassLoader... classLoaders) {
        return addUrls(ClasspathHelper.forPackage(pkg, classLoaders));
    }

    @Override
    /* @inherited */
    public Set<Scanner> getScanners() {
        return scanners != null ? scanners : DEFAULT_SCANNERS;
	}


    @Override
    /* @inherited */
    public Set<URL> getUrls() {
        return urls;
    }

    public ConfigurationBuilder addUrls(Collection<URL> urls) {
        this.urls.addAll(urls);
        return this;
    }

    @Override
    /* @inherited */
    public Predicate<String> getInputsFilter() {
        return inputsFilter != null ? inputsFilter : DEFAULT_INPUTS_FILTER;
    }

    /** sets the input filter for all resources to be scanned.
     * <p>prefer using {@link FilterBuilder} */
    public ConfigurationBuilder setInputsFilter(Predicate<String> inputsFilter) {
        this.inputsFilter = inputsFilter;
        return this;
    }

    /** sets the input filter for all resources to be scanned.
     * <p>prefer using {@link FilterBuilder} */
    public ConfigurationBuilder filterInputsBy(Predicate<String> inputsFilter) {
        return setInputsFilter(inputsFilter);
    }

    @Override
    /* @inherited */
    public boolean isParallel() {
        return isParallel;
    }

    @Override
    /* @inherited */
    public ClassLoader[] getClassLoaders() {
        return classLoaders;
    }


    /** add optional class loaders used for resolving types. */
    public ConfigurationBuilder addClassLoaders(ClassLoader... classLoaders) {
        this.classLoaders = this.classLoaders == null ? classLoaders :
            Stream.concat(Arrays.stream(this.classLoaders), Arrays.stream(classLoaders)).distinct().toArray(ClassLoader[]::new);
        return this;
    }

    @Override
    /* @inherited */
    public boolean shouldExpandSuperTypes() {
        return expandSuperTypes;
    }
}
