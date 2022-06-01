package com.tool;

import java.io.File;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws Exception {
        copyProject();
        Set<Class<?>> allClasses = ReflectionUtils.getApplicationClasses();
        var serviceClasses = ReflectionUtils.findServiceClasses(allClasses);
        List<ClassInfo> classInfos = Utils.map(serviceClasses, ClassInfoProcessor::toClassInfo);
        var serviceToServiceDependencies = ClassInfoProcessor.createDependencyMap(classInfos);

        var parseResults = JavaParser.parse(Configuration.TARGET_PROJECT_PATH);

        Generator.generateCommunicationModel();
        Generator.generateSpringProfiles(serviceToServiceDependencies, parseResults);
        Generator.generateClients(serviceToServiceDependencies, parseResults, ReflectionUtils.findSpringBootApplicationClass(allClasses));
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

