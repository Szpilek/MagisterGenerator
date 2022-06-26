package com.tool;

import java.util.*;
import java.util.stream.Collectors;

import static com.tool.Utils.*;

public class ConfigGenerator {
    public static void generateConfig(Map<Class<?>, List<Class<?>>> dependenciesFromProject, List<Class<?>> ControllerDependencies, Class<?> homeClass){
        generateSamconfig(homeClass);
        generateTemplate(dependenciesFromProject, ControllerDependencies, homeClass);
    }


    private static void generateTemplate(Map<Class<?>, List<Class<?>>> dependenciesFromProject, List<Class<?>> ControllerDependencies, Class<?> homeClass){
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
                generateConfigForServiceLambdas(dependenciesFromProject),
                generateConfigForControllerLambda(ControllerDependencies, homeClass),
                "Outputs:",
                indent("HelloWorldApi:", 1),
                indent("Description: \"API Gateway endpoint URL for Prod stage\"", 2),
                indent("Value: !Sub \"https://${ServerlessRestApi}.execute-api.${AWS::Region}.amazonaws.com/Prod/\"", 2)

        );
        writeFile(Configuration.TARGET_JAVA_PATH, homeClass.getPackageName().replace(".", "/") + "/template.yaml", s);
    }

    private static String generateConfigForLambda(){
        return multilineString(
        );
    }

    private static String generateConfigForServiceLambdas(Map<Class<?>, List<Class<?>>> dependenciesFromProject){
        return "";
    }

    private static String generateConfigForControllerLambda(List<Class<?>> ControllerDependencies, Class<?> homeClass){
        String homePackage = homeClass.getPackageName();
        return multilineString(
                indent("ControllerLambda:", 1),
                indent("Type: AWS::Serverless::Function", 2),
                indent("Properties:", 2),
                indent("CodeUri: " + homePackage ,3),
                indent("Handler: " + homePackage + ".ControllerLambda::handleRequest", 3),
                indent("Runtime: java11", 3),
                indent("Architectures:", 3),
                indent("- x86_64", 4),
                indent("MemorySize: 512", 3),
                indent("Policies:", 3),
                indent("- LambdaInvokePolicy:", 4),
                indent("FunctionName:", 5),
                indent(generateDependencies(ControllerDependencies), 6),
                indent("!Ref ServiceLambda", 6), //todo przekazać zależności do innych lambd
                indent("Environment:", 3),
                indent("Variables:", 4), //todo przekazać zmienne środowiskowe
                indent("SERVICE_LAMBDA_ARN: !Ref ServiceLambda", 5), //todo przekazać zależności do innych lambd
                indent("PARAM1: VALUE", 5), //?
                indent("FLAG: 1", 5),
                indent("JAVA_TOOL_OPTIONS: -XX:+TieredCompilation -XX:TieredStopAtLevel=1", 5),
                indent("Events:", 3),
                indent("HelloWorld:", 4), //przekazac prawdziwe eventy
                indent("Type: Api", 5),
                indent("Properties:", 5),
                indent("Path: /{proxy+}", 6), //prawdziwa ścieżka + proxy
                indent("Method: ANY", 6)
        );
    }

    private static String generateDependencies(List<Class<?>> dependencies) {
        return dependencies.stream().map(ConfigGenerator::generateDependencyInfo).collect(Collectors.joining());
    }

    private static String generateDependencyInfo(Class<?> clazz){
        String clazzName = clazz.getName() + "Lambda";
        String clazzNameUppercase = String.join("_", clazzName.split("([A-Z])")) + "_ARN";
        return multilineString(clazzNameUppercase + ": !Ref " + clazzName);
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

        writeFile(Configuration.TARGET_JAVA_PATH, homeClass.getPackageName().replace(".", "/") + "/samconfig.toml", s);
    }


}
