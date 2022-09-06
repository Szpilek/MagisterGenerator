package com.tool;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws Exception {
        Configuration.printConfiguration();
        copyProject();
        Set<Class<?>> allClasses = ReflectionUtils.getApplicationClasses();
        var serviceClasses = ReflectionUtils.findServiceClasses(allClasses);
        var controllerClasses = ReflectionUtils.findControllerClasses(allClasses);
        List<ClassInfo> serviceInfos = Utils.map(serviceClasses, ClassInfoProcessor::toClassInfo);
        List<ClassInfo> controllerInfos = Utils.map(controllerClasses, ClassInfoProcessor::toClassInfo);
        var dependencyMap = ClassInfoProcessor.createDependencyMap(
                Utils.combine(serviceInfos, controllerInfos)
        );
        var serviceDependencies = ClassInfoProcessor.createDependencyMap(serviceInfos);
        var controllerDependencies = getControllerDependencies(controllerInfos, dependencyMap);

        var parseResults = JavaParser.parse(Configuration.SOURCE_JAVA_PATH);

        Generator.generateCommunicationModel();
        Generator.generateSpringProfiles(serviceDependencies, parseResults);
        Generator.generateClients(dependencyMap, parseResults, ReflectionUtils.findSpringBootApplicationClass(allClasses), serviceClasses, controllerClasses);
        Generator.generateSpringProfilesForController(Utils.map(controllerInfos, ClassInfo::getClazz), parseResults, ReflectionUtils.findSpringBootApplicationClass(allClasses));
        Generator.generateController(Utils.map(controllerInfos, ClassInfo::getClazz), parseResults, ReflectionUtils.findSpringBootApplicationClass(allClasses));
        ConfigGenerator.generateConfig(dependencyMap, serviceClasses, controllerDependencies, ReflectionUtils.findSpringBootApplicationClass(allClasses));
    }

    private static List<Class<?>> getControllerDependencies(List<ClassInfo> controllerInfos, Map<Class<?>, List<Class<?>>> serviceToServiceDependencies) {
        var controllerDependencies = Utils.map(controllerInfos, controllerInfo -> serviceToServiceDependencies.get(controllerInfo.clazz));
        List<Class<?>> result = new ArrayList<>();
        controllerDependencies.forEach(result::addAll);
        return result;
    }

    static void copyProject() throws Exception {
        boolean wasProjectAlreadyGenerated = new File(Configuration.TARGET_PROJECT_PATH).exists();
        if(wasProjectAlreadyGenerated){
//            System.out.print("Target project path: " + Configuration.TARGET_PROJECT_PATH +  "already exists");
//            System.out.println("Do You want to remove it? [Y/n]");
//            Scanner input = new Scanner(System.in);
//            var answer = input.nextLine().trim().toUpperCase();
//            if ("Y".equals(answer) || "".equals(answer)) {
//            System.out.println("Attempting to remove " + Configuration.TARGET_PROJECT_PATH);
            System.out.println("Attempting to remove " + Configuration.TARGET_PROJECT_PATH);
                CommandLine.executeCommand("rm", "-rf", Configuration.TARGET_PROJECT_PATH);
//            } else {
//                throw new RuntimeException("Project already generated");
//            }
        }
        CommandLine.executeCommand("cp", "-R", Configuration.SOURCE_PROJECT_PATH, Configuration.TARGET_PROJECT_PATH);
    }
}

