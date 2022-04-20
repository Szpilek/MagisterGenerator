package com.tool;

import com.github.javaparser.ParseResult;
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
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.tool.ClassInfoProcesser.*;
import static com.tool.ParserUtils.getPrettyClassOrInterfaceName;

public class Main {
    public static void main(String[] args) throws IOException {
        if(args[0] == null || args[0].equals("")){
            throw new RuntimeException("Incorrect path");
        }
        String projectPath = args[0];
        String generatedPath = projectPath + "_gen";
        boolean wasProjectAlreadyGenerated = new File(generatedPath).exists();
        if(wasProjectAlreadyGenerated){
            throw new RuntimeException("Project already generated");
        }
        String[] cmd = new String[]{"cp", "-R", projectPath, generatedPath};
        Process pr = Runtime.getRuntime().exec(cmd);

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
        var parseResults = parseWithJavaParser(generatedPath);
        Generator.generateCommunicationModel();
        Generator.generateSpringProfiles(serviceToServiceDependencies, parseResults);
        Generator.generateClients(serviceToServiceDependencies, parseResults, springBootApplicationClazz);
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

