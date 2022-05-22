package com.tool;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReflectionUtils {
    static List<Class<?>> findServiceClasses(Set<Class<?>> classes) {
        return classes.stream()
                .filter(it -> it.isAnnotationPresent(Service.class))
                .collect(Collectors.toList());
    }

    static Set<Class<?>> getApplicationClasses() {
        return new Reflections(
                new ConfigurationBuilder()
                        .setScanners(new SubTypesScanner(false /* don't exclude Object.class */), new ResourcesScanner())
                        .setUrls(ClasspathHelper.forPackage(Configuration.APPLICATION_PACKAGE))
        ).getSubTypesOf(Object.class);
    }

    public static String getPrettyClassOrInterfaceName(Class<?> clazz){
        return clazz.getName().replace(clazz.getPackageName() + ".", "");
    }

    public static String getMethodClassName(Method method){
        return Arrays.stream(method.getDeclaringClass().getName().split("[.]")).reduce((first, second) -> second).get();
    }
}
