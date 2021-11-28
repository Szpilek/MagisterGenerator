package com.tool;

//import com.github.javaparser.JavaParser;
//import com.github.javaparser.ParseResult;
//import com.github.javaparser.StaticJavaParser;
//import com.github.javaparser.ast.CompilationUnit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Generator {

    public final static String home = "/home/marta/Desktop/Magisterka/monolit/src/main/java/";
    public static void generateClients(Map<Class<?>, List<Class<?>>> dependenciesFromProject){
        Set<Class<?>> dependenciesToCreateClient = new HashSet<>();
        for (Class<?> dep : dependenciesFromProject.keySet()){
          dependenciesToCreateClient.addAll(dependenciesFromProject.get(dep));
        }

        dependenciesToCreateClient.forEach(Generator::generateClient);
    }

    public static void generateClient(Class<?> depToCreateClient){
        String depToCreateClientName = depToCreateClient.getName().replace(depToCreateClient.getPackageName() + ".", "");

        String s = depToCreateClient.getPackage() +
                ";\n public class " + depToCreateClientName + "Client implements " + depToCreateClientName + " {\n";


        s += Arrays.stream(depToCreateClient.getMethods()).map(Generator::generateMethodForClient).collect(Collectors.joining());
        s += "}";
        String fileName = home + depToCreateClient.getName().replace(".","/") + "Client.java";
        Path path = Paths.get(fileName);
        File targetFile = new File(fileName);
        try {
            targetFile.createNewFile();
            Files.write(path, s.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(s);
    }

    private static String generateMethodForClient(Method method){
        method.getDeclaredAnnotations();
        method.getExceptionTypes();
        Random r = new Random();
        char parameterName = (char)(r.nextInt(26) + 'a');
        String parameters = Arrays.stream(method.getParameterTypes()).map(it -> it.getName() + " " + parameterName)
                .collect(Collectors.joining());

        String s = "@Override\n" + Modifier.toString(method.getModifiers()).replace("abstract", "")
                + " " + method.getReturnType().getName() + " " + method.getName()
                + " ("  + parameters
                + "){\n System.out.println(\"TEST\");}\n";

        return s;
    }


}
