package com.tool;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.SourceRoot;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tool.ClassInfoProcesser.createDependencyMap;
import static com.tool.ClassInfoProcesser.findSpringBootApplicationClass;

public class Main {
    public static void main(String[] args) throws Exception {
        if(args[0] == null || args[0].equals("")){
            throw new RuntimeException("Incorrect path");
        }
        copyProject();
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .setScanners(new SubTypesScanner(false /* don't exclude Object.class */), new ResourcesScanner())
                        .setUrls(ClasspathHelper.forPackage("com.example"))
        );

       //Zbierać tylko klasy annotowane service
        Set<Class<?>> allClasses = reflections.getSubTypesOf(Object.class);
        Class<?> springBootApplicationClazz = findSpringBootApplicationClass(allClasses);
        var serviceClasses = allClasses.stream()
                .filter(it -> it.isAnnotationPresent(Service.class))
                .collect(Collectors.toList());
        List<ClassInfo> classInfos = serviceClasses.stream()
                .map(ClassInfoProcesser::processClass)
                .collect(Collectors.toList());

        var serviceToServiceDependencies = createDependencyMap(classInfos);
        var parseResults = parseWithJavaParser(Configuration.TARGET_PROJECT_PATH);
        Generator.generateCommunicationModel();
        Generator.generateSpringProfiles(serviceToServiceDependencies, parseResults);
        Generator.generateClients(serviceToServiceDependencies, parseResults, springBootApplicationClazz);
    }

    static void copyProject() throws Exception {
        boolean wasProjectAlreadyGenerated = new File(Configuration.TARGET_PROJECT_PATH).exists();
        if(wasProjectAlreadyGenerated){
            System.out.print("Target project path: " + Configuration.TARGET_PROJECT_PATH +  "already exists");
            System.out.println("Do You want to remove it? [Y/n]");
            Scanner input = new Scanner(System.in);
            var answer = input.nextLine().trim().toUpperCase();
            if ("Y".equals(answer) || "".equals(answer)) {
                System.out.println("Attempting to remove " + Configuration.TARGET_PROJECT_PATH);
                CommandLine.executeCommand("rm", "-rf", Configuration.TARGET_PROJECT_PATH);
            } else {
                throw new RuntimeException("Project already generated");
            }
        }
        CommandLine.executeCommand("cp", "-R", Configuration.SOURCE_PROJECT_PATH, Configuration.TARGET_PROJECT_PATH);
    }

    private static List<CompilationUnit> parseWithJavaParser(String generatedPath) {
//        SourceRoot sourceRoot = new SourceRoot(Path.of("/home/marta/Desktop/Magisterka/monolit_gen/monolit"));
        SourceRoot sourceRoot = new SourceRoot(Path.of(generatedPath));
        List<CompilationUnit> parseResults = null;
        try {
            parseResults = sourceRoot.tryToParse("").stream()
                    .map(it -> it.getResult().get()) // TODO fail with meaningful error message
                    .collect(Collectors.toList());
//            Parser.classParser(sourceRoot.tryToParse(""));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return parseResults;
    }
}

