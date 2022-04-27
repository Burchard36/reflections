package org.reflections;

import org.reflections.util.NameHelper;

public class TestCLass {

    public static void main(String[] args) {
        new Reflections("org.reflections").getSubTypesOf(NameHelper.class).forEach((clazz) -> {
            System.out.println("Class: " + clazz.getName());
        });
    }

    public TestCLass() {
    }
}
