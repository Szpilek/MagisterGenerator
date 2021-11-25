package com.tool;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassInfoProcesser {
    public static ClassInfo processClass(Class<?> it){
        return new ClassInfo(getAutowiredFields(it), getConstructorArgs(it), it);
    }

    public static Map<Class<?>, List<Class<?>>> createDependencyMap(List<ClassInfo> classInfos){
        List<Class<?>> projectClasses = classInfos.stream()
                .map(ClassInfo::getClazz)
                .collect(Collectors.toList());

        return classInfos.stream()
                .collect(Collectors.toMap(ClassInfo::getClazz, it-> getDependenciesInProject(projectClasses, it)));
    }

    private static List<Class<?>> getDependenciesInProject(List<Class<?>> allProjectServices, ClassInfo info){
        var allClassDependencies = Stream.concat(info.getAutowiredFields().stream(), info.getConstructorArgs().stream())
                .collect(Collectors.toSet());
        return allClassDependencies.stream()
                .filter(dependency ->
                        allProjectServices.stream()
                            .anyMatch(dependency::isAssignableFrom))
                .collect(Collectors.toList());
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
                .map(Field::getType)
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
