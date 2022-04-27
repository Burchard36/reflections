package org.reflections.scanners;

import javassist.bytecode.ClassFile;
import org.reflections.vfs.Vfs;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface Scanner {

    /** scan the given {@code classFile} and produces list of {@link Map.Entry} key/values */
    List<Map.Entry<String, String>> scan(ClassFile classFile);

    /** scan the given {@code file} and produces list of {@link Map.Entry} key/values */
    default List<Map.Entry<String, String>> scan(Vfs.File file) {
        return null;
    }

    /** unique index name for scanner */
    default String index() {
        return getClass().getSimpleName();
    }

    default boolean acceptsInput(String file) {
        return file.endsWith(".class");
    }

    default Map.Entry<String, String> entry(String key, String value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    default List<Map.Entry<String, String>> entries(Collection<String> keys, String value) {
        return keys.stream().map(key -> entry(key, value)).collect(Collectors.toList());
    }

}
