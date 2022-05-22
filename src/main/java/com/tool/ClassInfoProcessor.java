package com.tool;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClassInfoProcessor {
    public static ClassInfo toClassInfo(Class<?> it){
        return new ClassInfo(ReflectionUtils.getAutowiredFields(it), ReflectionUtils.getConstructorArgs(it), it);
    }

    public static Map<Class<?>, List<Class<?>>> createDependencyMap(List<ClassInfo> classInfos){
        var projectClasses = Utils.map(classInfos, ClassInfo::getClazz);

        return classInfos.stream()
                .collect(Collectors.toMap(ClassInfo::getClazz, it-> getDependenciesInProject(projectClasses, it)));
    }

    private static List<Class<?>> getDependenciesInProject(List<Class<?>> allProjectServices, ClassInfo info){
        var allClassDependencies = Utils.combineToSet(info.getAutowiredFields(), info.getConstructorArgs());
        return allClassDependencies.stream()
                .filter(dependency -> Utils.anyMatch(allProjectServices, dependency::isAssignableFrom))
                .collect(Collectors.toList());
    }
}
