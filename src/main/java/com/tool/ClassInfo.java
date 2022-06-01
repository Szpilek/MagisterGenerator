package com.tool;

import java.util.List;

public class ClassInfo {
    List<Class<?>> autowiredFields;
    List<Class<?>> constructorArgs;
    Class<?> clazz;



    public ClassInfo(List<Class<?>> autowiredFields, List<Class<?>> constructorArgs, Class<?> clazz){
        this.autowiredFields=autowiredFields;
        this.constructorArgs=constructorArgs;
        this.clazz = clazz;
    }

    public List<Class<?>> getAutowiredFields() {
        return autowiredFields;
    }

    public void setAutowiredFields(List<Class<?>> autowiredFields) {
        this.autowiredFields = autowiredFields;
    }

    public List<Class<?>> getConstructorArgs() {
        return constructorArgs;
    }

    public void setConstructorArgs(List<Class<?>> constructorArgs) {
        this.constructorArgs = constructorArgs;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }
}
