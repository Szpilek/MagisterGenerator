package com.tool;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tool.config.JsonConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

import static com.tool.Utils.multilineString;

public class Configuration {
    static String CONFIG_JSON_PATH = getEnv("CONFIG_JSON_PATH");
    static String SOURCE_PROJECT_PATH = getEnv("SOURCE_PROJECT_PATH");
    static String PATH_TO_JAVA_FILES = getEnv("PATH_TO_JAVA_FILES", "src/main/java/");
    static String TARGET_PROJECT_PATH = getEnv("TARGET_PROJECT_PATH");
    static String SOURCE_JAVA_PATH = SOURCE_PROJECT_PATH + "/" + PATH_TO_JAVA_FILES;
    static String TARGET_JAVA_PATH = TARGET_PROJECT_PATH + "/" + PATH_TO_JAVA_FILES;
    static String APPLICATION_PACKAGE  = getEnv("APPLICATION_PACKAGE");
    static HashMap<String, Class<?>> interfaceImplementations = getInterfaceImplementations();
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


    public static Class<?> getInterfaceImplementationForDeserialization(Class<?> clazz) {
        System.out.println("Searching for an implementation to use when deserializing to " + clazz.getCanonicalName());
        return
                clazz.isInterface()
                ? java.util.Optional.ofNullable(interfaceImplementations.get(clazz.getCanonicalName()))
                .orElseThrow(() -> new RuntimeException("No configured implementation to use when deserializing to " + clazz.getCanonicalName()))
                : clazz;
    }


     public static HashMap<String, Class<?>> getInterfaceImplementations() {
         byte[] bytes;
         JsonConfig jsonConfig;
         var result = new HashMap<String, Class<?>>();
         try {
             bytes = java.nio.file.Files.readAllBytes(Path.of(CONFIG_JSON_PATH));
             var string = new String(bytes);
             jsonConfig = JsonMapper.builder().build().readValue(string, new TypeReference<JsonConfig>() {});
             jsonConfig.interfaceImplementations.forEach((k, v) -> {
                 try {
                     result.put(k, Configuration.class.getClassLoader().loadClass(v));
                 } catch (ClassNotFoundException e) {
                     e.printStackTrace();
                 }
             });
         } catch (Exception e) {
             e.printStackTrace();
             throw new RuntimeException(e);
         }
         return result;
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