package com.tool;


import com.sun.xml.bind.v2.model.runtime.RuntimeElement;

import javax.xml.transform.Source;
import java.util.Optional;

public class Configuration {
    static String SOURCE_PROJECT_PATH = getEnv("SOURCE_PROJECT_PATH");
    static String PATH_TO_JAVA_FILES = getEnv("PATH_TO_JAVA_FILES", "src/main/java/");
    static String TARGET_PROJECT_PATH = getEnv("TARGET_PROJECT_PATH");
    static String SOURCE_JAVA_PATH = SOURCE_PROJECT_PATH + "/" + PATH_TO_JAVA_FILES;
    static String TARGET_JAVA_PATH = TARGET_PROJECT_PATH + "/" + PATH_TO_JAVA_FILES;
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
}