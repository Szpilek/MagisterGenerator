package com.tool;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

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

public class Main {
    public static synchronized void loadLibrary(java.io.File jar) {
        try {
            java.net.URL url = jar.toURI().toURL();
            System.out.println("krk1");
            java.lang.reflect.Method method = java.net.URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{java.net.URL.class});
            System.out.println("krk2");
            method.setAccessible(true); /*promote the method to public access*/
            System.out.println("krk3");
            method.invoke(URLClassLoader.getPlatformClassLoader(), new Object[]{url});
            System.out.println("krk4");
        } catch (Exception ex) {
            throw new RuntimeException("Cannot load library from jar file '" + jar.getAbsolutePath() + "'. Reason: " + ex.getMessage());
        }
    }

    public static void main(String[] args) throws ClassNotFoundException, MalformedURLException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
//        File file = new File("/home/marta/Desktop/Magisterka/monolit/target/monolit-0.0.1-SNAPSHOT.jar");
//        MyClassLoader.addFile("/home/marta/Desktop/Magisterka/monolit/target/monolit-0.0.1-SNAPSHOT.jar");

//        loadLibrary(file);
//        URLClassLoader child = new URLClassLoader(
//                new URL[] {file.toURI().toURL()},
//                Main.class.getClassLoader()
//        );
//        Class classToLoad = Class.forName("com.MyClass", true, child);
//        Method method = classToLoad.getDeclaredMethod("myMethod");
//        Object instance = classToLoad.newInstance();
//        Object result = method.invoke(instance);
//        URL url = file.toURI().toURL();
//
//        URLClassLoader classLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
//        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
//        method.setAccessible(true);
//        method.invoke(classLoader, url);

//        List<ClassLoader> classLoadersList = new LinkedList<ClassLoader>();
//        classLoadersList.add(ClassLoader.getSystemClassLoader());
//        classLoadersList.add(ClasspathHelper.contextClassLoader());
//        classLoadersList.add(ClasspathHelper.staticClassLoader());
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
//                             .forPackage("com.example.monolit.api")
                        .setScanners(new SubTypesScanner(false /* don't exclude Object.class */), new ResourcesScanner())
//                        .setUrls(ClasspathHelper.forClassLoader(ClassLoader.getSystemClassLoader()))
                        .setUrls(ClasspathHelper.forPackage("com.example"))
        );
//        System.out.println(
//                Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator)
//                ).collect(Collectors.joining("\n"))
//        );
//        System.out.println(Arrays.stream(MyClassLoader.classLoader.getDefinedPackages()).map(it -> it.getName()).collect(Collectors.joining()));
//        System.out.println(ClassLoader.getSystemClassLoader().loadClass("com.example.monolit.api.FlowerControler"));
        Set<Class<? extends Object>> allClasses = reflections.getSubTypesOf(Object.class);
        System.out.println("allClasses.size()");
        System.out.println(allClasses.size());

        allClasses.stream().map(Main::getAutowiredFields).forEach(System.out::println);

        allClasses.forEach(Main::processClass);
    }

    private static void processClass(Class<?> it){
        ClassInfo newClass = new ClassInfo(getAutowiredFields(it), it);
    }

    private static boolean checkAnnotation(Field field) {
        return Arrays.stream(field.getDeclaredAnnotations()).anyMatch(
                it -> "org.springframework.beans.factory.annotation.Autowired".equals(it.annotationType().getName())
        );
    }

    private static List<Field> getAutowiredFields(Class<?> it) {
        return Arrays
                .stream(it.getDeclaredFields())
                .filter(Main::checkAnnotation)
                .collect(Collectors.toList());
    }
}
