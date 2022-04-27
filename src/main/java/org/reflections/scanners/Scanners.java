package org.reflections.scanners;

import javassist.bytecode.ClassFile;
import org.reflections.util.FilterBuilder;
import org.reflections.util.NameHelper;
import org.reflections.util.QueryBuilder;

import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;



/**
 * base Reflections {@link Scanner}s such as:
 * <ul>
 *   <li>{@link #SubTypes}</li>
 *   <li>{@link #TypesAnnotated}</li>
 * </ul>
 * <i>note that scanners must be configured in {@link org.reflections.Configuration} in order to be queried</i>
 * */
public enum Scanners implements Scanner, QueryBuilder, NameHelper {

    /** scan type superclasses and interfaces
     * <p></p>
     * <i>Note that {@code Object} class is excluded by default, in order to reduce store size.
     * <br>Use {@link #filterResultsBy(Predicate)} to change, for example {@code SubTypes.filterResultsBy(c -> true)}</i>
     * */
    SubTypes {
        /* Object class is excluded by default from subtypes indexing */
        { filterResultsBy(new FilterBuilder().excludePattern("java\\.lang\\.Object")); }

        @Override
        public void scan(ClassFile classFile, List<Map.Entry<String, String>> entries) {
            entries.add(entry(classFile.getSuperclass(), classFile.getName()));
            entries.addAll(entries(Arrays.asList(classFile.getInterfaces()), classFile.getName()));
        }
    },

    /** scan type annotations */
    TypesAnnotated {
        @Override
        public boolean acceptResult(String annotation) {
            return super.acceptResult(annotation) || annotation.equals(Inherited.class.getName());
        }

        @Override
        public void scan(ClassFile classFile, List<Map.Entry<String, String>> entries) {
            //entries.addAll(entries(getAnnotations(classFile::getAttribute), classFile.getName()));
        }
    };

    private Predicate<String> resultFilter = s -> true; //accept all by default

    @Override
    public String index() {
        return name();
    }

    public Scanners filterResultsBy(Predicate<String> filter) {
        this.resultFilter = filter;
        return this;
    }

    @Override
    public final List<Map.Entry<String, String>> scan(ClassFile classFile) {
        List<Map.Entry<String, String>> entries = new ArrayList<>();
        scan(classFile, entries);
        return entries.stream().filter(a -> acceptResult(a.getKey())).collect(Collectors.toList());
    }

    abstract void scan(ClassFile classFile, List<Map.Entry<String, String>> entries);

    protected boolean acceptResult(String fqn) {
        return fqn != null && resultFilter.test(fqn);
    }
}
