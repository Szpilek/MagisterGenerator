package com.tool;

import com.github.javaparser.Problem;
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
        copyProject();
        Set<Class<?>> allClasses = ReflectionUtils.getApplicationClasses();
        var serviceClasses = ReflectionUtils.findServiceClasses(allClasses);
        List<ClassInfo> classInfos = Utils.map(serviceClasses, ClassInfoProcesser::toClassInfo);
        var serviceToServiceDependencies = ClassInfoProcesser.createDependencyMap(classInfos);

        var parseResults = JavaParser.parse(Configuration.TARGET_PROJECT_PATH);

        Generator.generateCommunicationModel();
        Generator.generateSpringProfiles(serviceToServiceDependencies, parseResults);
        Generator.generateClients(serviceToServiceDependencies, parseResults, findSpringBootApplicationClass(allClasses));
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
}

