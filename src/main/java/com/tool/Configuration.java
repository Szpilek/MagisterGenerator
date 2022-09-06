package com.tool;


import java.util.Optional;

import static com.tool.Utils.multilineString;

public class Configuration {
    static String SOURCE_PROJECT_PATH = getEnv("SOURCE_PROJECT_PATH");
    static String PATH_TO_JAVA_FILES = getEnv("PATH_TO_JAVA_FILES", "src/main/java/");
    static String TARGET_PROJECT_PATH = getEnv("TARGET_PROJECT_PATH");
    static String SOURCE_JAVA_PATH = SOURCE_PROJECT_PATH + "/" + PATH_TO_JAVA_FILES;
    static String TARGET_JAVA_PATH = TARGET_PROJECT_PATH + "/" + PATH_TO_JAVA_FILES;
    static String APPLICATION_PACKAGE  = getEnv("APPLICATION_PACKAGE");
    static String getEnv(String env) {
        return getEnv(env, Optional.empty());
    }
    static String getEnv(String env, String defaultValue) {
        return getEnv(env, Optional.of(defaultValue));
    }
    static String getEnv(String env, Optional<String> maybeDefaultValue) {
        var value = Optional.ofNullable(System.getenv(env));
        return value.orElseGet(() -> {
            var defaultValue =  maybeDefaultValue.orElseThrow(() -> new RuntimeException("Default not set for env var: " + env));
            System.out.println("Using default value for env: " + env + " value: " + defaultValue);
            return defaultValue;
        });
     }

     public static void printConfiguration(){
        System.out.println(
                multilineString(
                        "SOURCE_PROJECT_PATH " + SOURCE_PROJECT_PATH,
                        "PATH_TO_JAVA_FILES " + PATH_TO_JAVA_FILES,
                        "TARGET_PROJECT_PATH " + TARGET_PROJECT_PATH,
                        "SOURCE_JAVA_PATH " + SOURCE_JAVA_PATH,
                        "TARGET_JAVA_PATH " + TARGET_JAVA_PATH,
                        "APPLICATION_PACKAGE " + APPLICATION_PACKAGE
                )

        );
     }
}