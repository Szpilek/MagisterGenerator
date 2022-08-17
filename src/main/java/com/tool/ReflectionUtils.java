package com.tool;

import org.hibernate.id.Configurable;
import org.hibernate.internal.util.ReflectHelper;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReflectionUtils {
    static boolean isInTargetApplication(Class<?> clazz)  {
        return clazz.getPackageName().startsWith(Configuration.APPLICATION_PACKAGE);
    }

    static List<Class<?>> findServiceClasses(Set<Class<?>> classes) {
        return classes.stream()
                .filter(it -> it.isAnnotationPresent(Service.class))
                .filter(ReflectionUtils::isInTargetApplication)
                .collect(Collectors.toList());
    }

    static List<Class<?>> findControllerClasses(Set<Class<?>> classes) {
        return classes.stream()
                .filter(it -> it.isAnnotationPresent(RestController.class) || it.isAnnotationPresent(Controller.class))
                .filter(ReflectionUtils::isInTargetApplication)
                .collect(Collectors.toList());
    }

    static Set<Class<?>> getApplicationClasses() {
        return new Reflections(
                new ConfigurationBuilder()
                        .setScanners(new SubTypesScanner(false /* don't exclude Object.class */), new ResourcesScanner())
                        .setUrls(ClasspathHelper.forPackage(Configuration.APPLICATION_PACKAGE))
        ).getSubTypesOf(Object.class);
    }
    public static Class<?> findSpringBootApplicationClass(Set<Class<?>> allClasses){
        return allClasses.stream()
                .filter(it -> it.isAnnotationPresent(SpringBootApplication.class))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No class annotated with @SpringBootApplication"));
    }


    public static String getPrettyClassOrInterfaceName(Class<?> clazz){
        return clazz.getName().replace(clazz.getPackageName() + ".", "");
    }

    public static String getMethodClassName(Method method){
        return Arrays.stream(method.getDeclaringClass().getName().split("[.]")).reduce((first, second) -> second).get();
    }

    public static List<Class<?>> getAutowiredFields(Class<?> it) {
        return Arrays
                .stream(it.getDeclaredFields())
                .filter(ReflectionUtils::hasAutowiredAnnotation)
                .map(Field::getType)
                .collect(Collectors.toList());
    }

    public static List<Class<?>> getConstructorArgs(Class<?> it){
        return Arrays.stream(it.getConstructors())
                .flatMap(constructor -> getParameterTypes(constructor).stream())
                .collect(Collectors.toList());
    }

    private static List<Class<?>> getParameterTypes(Constructor<?> constructor){
        return Arrays.stream(constructor.getParameterTypes()).collect(Collectors.toList());
    }

    public static boolean hasAutowiredAnnotation(Field field) {
        return Arrays.stream(field.getDeclaredAnnotations()).anyMatch(
                it -> "org.springframework.beans.factory.annotation.Autowired".equals(it.annotationType().getName())
        );
    }
}
