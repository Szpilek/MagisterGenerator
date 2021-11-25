package com.tool;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.tool.ClassInfoProcesser.*;

public class Main {
    public static void main(String[] args){
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .setScanners(new SubTypesScanner(false /* don't exclude Object.class */), new ResourcesScanner())
                        .setUrls(ClasspathHelper.forPackage("com.example"))
        );

       //Zbierać tylko klasy annotowane service
        Set<Class<?>> allClasses = reflections.getSubTypesOf(Object.class);
        var serviceClasses = allClasses.stream()
                .filter(it -> it.isAnnotationPresent(Service.class))
                .collect(Collectors.toList());
        List<ClassInfo> classInfos = serviceClasses.stream()
                .map(ClassInfoProcesser::processClass)
                .collect(Collectors.toList());

        var dependenciesFromProject = createDependencyMap(classInfos);

//        for(Class<?> dependency : dependenciesFromProject){
//            Generator.generateClient(dependency);
//        }

    }
}

