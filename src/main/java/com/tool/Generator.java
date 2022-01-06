package com.tool;

//import com.github.javaparser.JavaParser;
//import com.github.javaparser.ParseResult;
//import com.github.javaparser.StaticJavaParser;
//import com.github.javaparser.ast.CompilationUnit;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Generator {

    public final static String home = "/home/marta/Desktop/Magisterka/monolit/src/main/java/";

    public static void generateClients(Map<Class<?>, List<Class<?>>> dependenciesFromProject, List<CompilationUnit> parseResults) {
        Set<Class<?>> dependenciesToCreateClient = new HashSet<>();
        dependenciesFromProject.values().forEach(dependenciesToCreateClient::addAll);
        dependenciesToCreateClient.forEach(it -> Generator.generateClient(it, parseResults));
    }

    public static void generateClient(Class<?> clazz, List<CompilationUnit> parseResults) {
        String interfaceName = clazz.getName().replace(clazz.getPackageName() + ".", "");

        String s = clazz.getPackage() + ";\n public class " + interfaceName + "Client implements " + interfaceName + " {\n";

        clazz.getGenericSuperclass();

//        Class genericParameter0OfThisClass = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];


        s += Arrays.stream(clazz.getMethods()).map(it -> Generator.generateMethodForClient(it, parseResults)).collect(Collectors.joining());
        s += "}";
        String fileName = home + clazz.getName().replace(".", "/") + "Client.java";
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

    private static String generateMethodForClient(Method method, List<CompilationUnit> parseResults) {
        method.getDeclaredAnnotations();
        method.getExceptionTypes();
        Random r = new Random(); //zamienić random na kolejne numerowanie argumentów a1, a2, a3
        char parameterName = (char) (r.nextInt(26) + 'a');
        String parameters = Arrays.stream(method.getParameterTypes()).map(it -> it.getName() + " " + parameterName)
                .collect(Collectors.joining());

        String returnType = method.getReturnType().getName();
//        if(method.getReturnType().getGenericSuperclass() != null){
        returnType = getGenericType(method, parseResults, method.getReturnType());
//        }

        String s = "@Override\n" + Modifier.toString(method.getModifiers()).replace("abstract", "")
                + " " + returnType + " " + method.getName()
                + " (" + parameters
                + "){\n System.out.println(\"TEST\");}\n";

        return s;
    }

    private static String getGenericType(Method method, List<CompilationUnit> parseResults, Class<?> clazz) {
        var methodClassName = Arrays.stream(method.getDeclaringClass().getName().split("[.]")).reduce((first, second) -> second).get();
        var classOrInterface = parseResults.stream()
//                .filter(it -> it.getPackageDeclaration().get().getName().asString().equals(method.getDeclaringClass().getPackageName()))
                .map(it -> it.getClassByName(methodClassName).or(() -> it.getInterfaceByName(methodClassName)))
                .filter(Optional::isPresent)
                .findFirst().get().get();
       var methodInfo = classOrInterface.getChildNodes().stream()
               .filter(it -> it instanceof MethodDeclaration)
               .map(it -> (MethodDeclaration) it)
               .filter(it -> it.getName().asString().equals(method.getName()))
               .filter(it -> checkIfMethodParametersEqual(it.getParameters(), method.getParameters()))
               .findFirst().get();

        return methodInfo.getType().asString();
    }


    public static boolean checkIfMethodParametersEqual(
            NodeList<Parameter> parserParameters,
            java.lang.reflect.Parameter[] methodParameters){
        if (methodParameters.length == 0 && parserParameters.isEmpty()){
            return true;
        } else if (methodParameters.length != parserParameters.size()){
            return false;
        }

        for(int i = 0; i < parserParameters.size(); i++){
            for(int j = 0; j < methodParameters.length; j++){
                if(!parserParameters.get(i).getName().asString().equals(methodParameters[j].getName())){
                    return false;
                }
            }
        }
        return true;
    }

}
