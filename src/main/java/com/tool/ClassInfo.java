package com.tool;

import java.lang.reflect.Field;
import java.util.List;

public class ClassInfo {
    List<Field> autowiredFields;
    List<Class<?>> constructorArgs;
    Class<?> classDescription;

    public ClassInfo(List<Field> autowiredFields, List<Class<?>> constructorArgs, Class<?> classDescription){
        this.autowiredFields=autowiredFields;
        this.constructorArgs=constructorArgs;
        this.classDescription=classDescription;
    }
}
