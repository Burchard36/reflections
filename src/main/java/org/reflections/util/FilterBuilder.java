package org.reflections.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FilterBuilder implements Predicate<String> {
    private final List<Predicate<String>> chain = new ArrayList<>();

    public FilterBuilder() {}

    /** include package prefix <pre>{@code new FilterBuilder().includePackage("java.lang")}</pre>
     * <i>note that the {@code value} is mapped into a prefix pattern with a trailing dot, for example {@code "a.b" == "a\\.b\\..*}
     * <p>see more in {@link #prefixPattern(String)} */
    public FilterBuilder includePackage(String value) {
        return includePattern(prefixPattern(value));
    }

    /** include regular expression <pre>{@code new FilterBuilder().includePattern("java\\.lang\\..*")}</pre>
     * see also {@link #includePackage(String)}*/
    public FilterBuilder includePattern(String regex) {
        return add(new FilterBuilder.Include(regex));
    }

    public FilterBuilder excludePattern(String regex) {
        return add(new FilterBuilder.Exclude(regex));
    }

    public FilterBuilder add(Predicate<String> filter) {
        chain.add(filter);
        return this;
    }

    public boolean test(String regex) {
        boolean accept = chain.isEmpty() || chain.get(0) instanceof Exclude;

        for (Predicate<String> filter : chain) {
            if (accept && filter instanceof Include) {continue;} //skip if this filter won't change
            if (!accept && filter instanceof Exclude) {continue;}
            accept = filter.test(regex);
            if (!accept && filter instanceof Exclude) {break;} //break on first exclusion
        }
        return accept;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(chain, ((FilterBuilder) o).chain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chain);
    }

    @Override public String toString() {
        return chain.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    /** maps fqn to prefix pattern with a trailing dot, for example {@code packageNamePrefix("a.b") == "a\\.b\\..*} */
    private static String prefixPattern(String fqn) {
        if (!fqn.endsWith(".")) fqn += ".";
        return fqn.replace(".", "\\.").replace("$", "\\$") + ".*";
    }

    abstract static class Matcher implements Predicate<String> {
        final Pattern pattern;
        Matcher(String regex) { pattern = Pattern.compile(regex); }
        @Override public int hashCode() { return Objects.hash(pattern); }
        @Override public boolean equals(Object o) {
            return this == o || o != null && getClass() == o.getClass() && Objects.equals(pattern.pattern(), ((Matcher) o).pattern.pattern());
        }
        @Override public String toString() { return pattern.pattern(); }
    }

    static class Include extends Matcher {
        Include(String regex) { super(regex); }
        @Override public boolean test(String regex) { return pattern.matcher(regex).matches(); }
        @Override public String toString() { return "+" + pattern; }
    }

    static class Exclude extends Matcher {
        Exclude(String regex) { super(regex); }
        @Override public boolean test(String regex) { return !pattern.matcher(regex).matches(); }
        @Override public String toString() { return "-" + pattern; }
    }
}
