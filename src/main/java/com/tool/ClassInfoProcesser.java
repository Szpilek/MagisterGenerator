package com.tool;

import jdk.jshell.execution.Util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassInfoProcesser {
    public static ClassInfo processClass(Class<?> it){
        return new ClassInfo(getAutowiredFields(it), getConstructorArgs(it), it);
    }

    public static Map<Class<?>, List<Class<?>>> createDependencyMap(List<ClassInfo> classInfos){
        var projectClasses = Utils.map(classInfos, ClassInfo::getClazz);

        return classInfos.stream()
                .collect(Collectors.toMap(ClassInfo::getClazz, it-> getDependenciesInProject(projectClasses, it)));
    }

//    public static List<Class<?>> checkMissingGenericInfo(Map<Class<?>, List<Class<?>>> dependencyMap){
//        Set<Class<?>> dependenciesToCheck = new HashSet<>();
//        for (Class<?> dep : dependencyMap.keySet()){
//            dependenciesToCheck.addAll(dependencyMap.get(dep));
//        }
//
//        dependenciesToCheck.forEach();
//    }

    private static List<Class<?>> getDependenciesInProject(List<Class<?>> allProjectServices, ClassInfo info){
        var allClassDependencies = Utils.combineToSet(info.getAutowiredFields(), info.getConstructorArgs());
        return allClassDependencies.stream()
                .filter(dependency -> Utils.anyMatch(allProjectServices, dependency::isAssignableFrom))
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
