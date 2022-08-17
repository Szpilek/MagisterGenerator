package com.tool;

import java.util.ArrayList;
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


    public static List<Class<?>> createDependencyList(List<ClassInfo> controllersClassInfos, List<ClassInfo> servicesClassInfos){
        List<Class<?>> controllerDependencies = new ArrayList<>();
        // not all controllers are from the target project, exclude Spring ones
        var controllersClasses = Utils.map(controllersClassInfos, ClassInfo::getClazz);
        var servicesClasses = Utils.map(servicesClassInfos, ClassInfo::getClazz);

        for(Class<?> controllerClazz : controllersClasses){
            for(Class<?> serviceClazz : servicesClasses){
                if(isDependency(controllerClazz, serviceClazz)){
                    controllerDependencies.add(serviceClazz);
                }
            }
        }

        return controllerDependencies;
    }

    private static boolean isDependency(Class<?> controllerClazz, Class<?> serviceClazz){
        return serviceClazz.isAssignableFrom(controllerClazz);
    }

    private static List<Class<?>> getDependenciesInProject(List<Class<?>> allProjectServices, ClassInfo info){
        var allClassDependencies = Utils.combineToSet(info.getAutowiredFields(), info.getConstructorArgs());
        return allClassDependencies.stream()
                .filter(dependency -> Utils.anyMatch(allProjectServices, dependency::isAssignableFrom))
                .collect(Collectors.toList());
    }

//    private static List<Class<?>> x(List<Class<?>> allProjectServices, ClassInfo info){
//        var dependencies = Utils.combineToSet(info.getAutowiredFields(), info.getConstructorArgs());
//        return dependencies.stream()
//                .flatMap(dependency -> allProjectServices.stream().filter(projectService -> dependency.isAssignableFrom(projectService)))
//                .collect(Collectors.toList());
//    }
}
