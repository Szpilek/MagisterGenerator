package com.tool;

import java.lang.reflect.Method;
import java.util.List;

public class MethodInfo {
    Method method;
    String packageName;
    String className;
    String name;
    List<ParameterInfo> parameters;
    String returnType;

    public MethodInfo(Method method, String name, List<ParameterInfo> parameters, String returnType, String className, String packageName) {
        this.method = method;
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.className = className;
        this.packageName = packageName;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ParameterInfo> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterInfo> parameters) {
        this.parameters = parameters;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }
}
