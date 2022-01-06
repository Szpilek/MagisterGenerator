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

import java.io.IOException;
import java.nio.file.Path;
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

        var serviceToServiceDependencies = createDependencyMap(classInfos);
        var parseResults = parseWithJavaParser();
        Generator.generateClients(serviceToServiceDependencies, parseResults);

    }

    private static List<CompilationUnit> parseWithJavaParser() {
        SourceRoot sourceRoot = new SourceRoot(Path.of("/home/marta/Desktop/Magisterka/monolit"));
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

