package com.tool;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClassInfoProcesser {
    public static void processClass(Class<?> it){
        ClassInfo newClass = new ClassInfo(getAutowiredFields(it), getConstructorArgs(it), it);
    }

    private static boolean checkAnnotation(Field field) {
        return Arrays.stream(field.getDeclaredAnnotations()).anyMatch(
                it -> "org.springframework.beans.factory.annotation.Autowired".equals(it.annotationType().getName())
        );
    }

    private static List<Class<?>> getAutowiredFields(Class<?> it) {
        return Arrays
                .stream(it.getDeclaredFields())
                .filter(ClassInfoProcesser::checkAnnotation)
                .map(Field::getDeclaringClass)
                .collect(Collectors.toList());
    }

    private static List<Class<?>> getConstructorArgs(Class<?> it){
        return Arrays.stream(it.getConstructors())
                .flatMap(constructor -> checkConstructor(constructor).stream())
                .collect(Collectors.toList());
    }

    private static List<Class<?>> checkConstructor(Constructor<?> constructor){
        return Arrays.stream(constructor.getParameterTypes()).collect(Collectors.toList());
    }



}
