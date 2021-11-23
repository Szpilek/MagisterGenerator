package com.tool;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        var dependenciesFromProject = getDependencies(classInfos);

    }
}
