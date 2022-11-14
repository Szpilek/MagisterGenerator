package com.tool;

import java.util.*;
import java.util.stream.Collectors;

import static com.tool.Utils.*;

public class ConfigGenerator {
    public static void generateConfig(Map<Class<?>, List<Class<?>>> dependenciesFromProject, List<Class<?>> serviceClasses, List<Class<?>> controllerDependencies, Class<?> homeClass){
        generateSamconfig(homeClass);
        generateTemplate(dependenciesFromProject, serviceClasses, controllerDependencies, homeClass);
    }


    private static void generateTemplate(Map<Class<?>, List<Class<?>>> dependenciesFromProject, List<Class<?>> serviceClasses, List<Class<?>> ControllerDependencies, Class<?> homeClass){
        Set<Class<?>> dependenciesToCreateClient = new HashSet<>();
        dependenciesFromProject.values().forEach(dependenciesToCreateClient::addAll);

        String s = multilineString(
                "AWSTemplateFormatVersion: '2010-09-09'",
                "Transform: AWS::Serverless-2016-10-31",
                "Description: >",
                indent("sam-app-test", 1),
                indent("Sample SAM Template for sam-app-test", 1),
                "Globals:",
                indent("Function:", 1),
                indent("Timeout: 20", 2),
                "Resources:",
                generateConfigForServiceLambdas(dependenciesFromProject, serviceClasses, homeClass),
                generateConfigForControllerLambda(ControllerDependencies, homeClass),
                "Outputs:",
                indent("Api:", 1),
                indent("Description: \"API Gateway endpoint URL for Prod stage\"", 2),
                indent("Value: !Sub \"https://${ServerlessRestApi}.execute-api.${AWS::Region}.amazonaws.com/Prod/\"", 2)

        );
        writeFile(Configuration.TARGET_PROJECT_PATH,  "template.yaml", s);
    }

    private static String generateConfigForLambda(Class<?> lambdaClassImpl, List<Class<?>> dependencies, Class<?> homeClass){
        String homePackage = homeClass.getPackageName();
        Class<?>[] lambdaClasses = lambdaClassImpl.getInterfaces();
        Class<?> lambdaClass = Arrays.stream(lambdaClasses).filter(it -> ReflectionUtils.getPrettyClassOrInterfaceName(lambdaClassImpl).contains(ReflectionUtils.getPrettyClassOrInterfaceName(it)))
                .findFirst().orElseThrow(() -> new RuntimeException("Could not find"));

        String policies = dependencies.isEmpty() ? "" : multilineString(
                indent("Policies:", 3),
                indent("- LambdaInvokePolicy:", 3),
                indent("FunctionName:", 5),
                indent(generateInvokePolicyRows(dependencies), 6)
        );

        return multilineString(
                indent(getLambdaName(lambdaClass)+":", 1),
                indent("Type: AWS::Serverless::Function", 2),
                indent("Properties:", 2),
                indent("CodeUri: ", 3), // dodać odnośnik do projektu
                indent("Handler: " + lambdaClass.getPackage().getName() + "." + getLambdaName(lambdaClass) + "::handleRequest", 3), //odnośnik do klasy od java
                indent("Runtime: java11", 3),
                indent("Architectures:", 3),
                indent("- x86_64",3),
                indent("MemorySize: 1024",3),
                policies,
                indent("Environment:",3),
                indent("Variables:",4),
                indent("JAVA_TOOL_OPTIONS: -XX:+TieredCompilation -XX:TieredStopAtLevel=1",5)
        );
    }

    private static String generateConfigForServiceLambdas(Map<Class<?>, List<Class<?>>> dependenciesFromProject, List<Class<?>> serviceClasses, Class<?> homeClass){
//        Set<Class<?>> dependenciesToGenerateConfig = new HashSet<>();
//        dependenciesFromProject.values().forEach(dependenciesToGenerateConfig::addAll);
//        dependenciesToGenerateConfig.addAll(dependenciesFromProject.keySet());
        return serviceClasses.stream().map(it -> generateConfigForLambda(it, dependenciesFromProject.get(it), homeClass) + "\n").collect(Collectors.joining());
    }

    private static String generateConfigForControllerLambda(List<Class<?>> controllerDependencies, Class<?> homeClass){
        String homePackage = homeClass.getPackageName();
        return multilineString(
                indent("ControllerLambda:", 1),
                indent("Type: AWS::Serverless::Function", 2),
                indent("Properties:", 2),
                indent("CodeUri: ." ,3),
                indent("Handler: " + homePackage + ".ControllerLambda::handleRequest", 3),
                indent("Runtime: java11", 3),
                indent("Architectures:", 3),
                indent("- x86_64", 3),
                indent("MemorySize: 1024", 3),
                indent("Policies:", 3),
//                indent("- LambdaInvokePolicy:", 3),
//                indent("FunctionName:", 5),
//                indent(generateInvokePolicyRows(controllerDependencies), 6),
                generateInvokePolicyRows(controllerDependencies),
                indent("Environment:", 3),
                indent("Variables:", 4), //todo przekazać zmienne środowiskowe
                indent(generateEnvVariables(controllerDependencies), 5),
                indent("JAVA_TOOL_OPTIONS: -XX:+TieredCompilation -XX:TieredStopAtLevel=1", 5),
                indent("Events:", 3),
                indent("HTTP:", 4),
                indent("Type: Api", 5),
                indent("Properties:", 5),
                indent("Path: /{proxy+}", 6),
                indent("Method: ANY", 6)
        );
    }

    private static String generateInvokePolicyRows(List<Class<?>> dependencies) {
        return dependencies.stream().map(ConfigGenerator::generateInvokePolicyRow).collect(Collectors.joining());
    }

    private static String generateInvokePolicyRow(Class<?> clazz){
        return multilineString(indent("- LambdaInvokePolicy:", 3),
                indent("FunctionName:", 5),
                indent("Ref: " + getLambdaName(clazz) , 6)+ "\n");
    }

    private static String generateEnvVariables(List<Class<?>> dependencies) {
        return multilineString(Utils.map(dependencies, ConfigGenerator::generateEnvVariable));
//        return dependencies.stream().map(ConfigGenerator::generateEnvVariable).collect(Collectors.joining());
    }

    private static String generateEnvVariable(Class<?> clazz){
        String clazzNameUppercase = getLambdaName(clazz).replaceAll("([A-Z])", "_" + "$1").toUpperCase() + "_ARN";
        return multilineString(
                clazzNameUppercase.substring(1) + ":",
                indent("Ref: " + getLambdaName(clazz), 1)
        );
    }

    public static String getLambdaName(Class<?> clazz) {
        return ReflectionUtils.getPrettyClassOrInterfaceName(clazz) + "Lambda";
    }

    private static void generateSamconfig(Class<?> homeClass){
        String s = multilineString(
        "version = 0.1",
        "[default]",
        "[default.deploy]",
        "[default.deploy.parameters]",
        "stack_name = \"java-basic\"",
        "s3_bucket = \"sam-test-asdasd\"", //zmienić do konfiguracji po testach
        "s3_prefix = \"sam-test\"", //zmienić do konfiguracji po testach
        "region = \"eu-west-1\"",
        "confirm_changeset = true",
        "capabilities = \"CAPABILITY_IAM\"",
        "image_repositories = []"
        );

        writeFile(Configuration.TARGET_PROJECT_PATH, "samconfig.toml", s);
    }


}
