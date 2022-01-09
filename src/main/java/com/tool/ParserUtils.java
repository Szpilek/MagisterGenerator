
package com.tool;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class ParserUtils {
    public static ClassOrInterfaceDeclaration getClassOrInterface(String name, List<CompilationUnit> parseResults){
        return parseResults.stream()
                .map(it -> it.getClassByName(name).or(() -> it.getInterfaceByName(name)))
                .filter(Optional::isPresent)
                .findFirst().get().get();
    }

    public static MethodDeclaration getMethod(Method method, ClassOrInterfaceDeclaration classOrInterface){
        return classOrInterface.getChildNodes().stream()
                .filter(it -> it instanceof MethodDeclaration)
                .map(it -> (MethodDeclaration) it)
                .filter(it -> it.getName().asString().equals(method.getName()))
                .filter(it -> checkIfMethodParametersEqual(it.getParameters(), method.getParameters()))
                .findFirst().get();
    }
    public static boolean checkIfMethodParametersEqual(
            NodeList<Parameter> parserParameters,
            java.lang.reflect.Parameter[] methodParameters){
        if (methodParameters.length == 0 && parserParameters.isEmpty()){
            return true;
        } else if (methodParameters.length != parserParameters.size()){
            return false;
        }

        for(int i = 0; i < parserParameters.size(); i++){
            if(!parserParameters.get(i).getName().asString().equals(methodParameters[i].getName())){
                return false;
            }
        }
        return true;
    }

    public static String getReturnType(Method method, List<CompilationUnit> parseResults) {
        var classOrInterface = getClassOrInterface(getMethodClassName(method), parseResults);
        var methodDeclaration = getMethod(method, classOrInterface);
        return methodDeclaration.getType().asString();
    }

    public static String getMethodClassName(Method method){
        return Arrays.stream(method.getDeclaringClass().getName().split("[.]")).reduce((first, second) -> second).get();
    }

    public static String getImports(Class<?> clazz, List<CompilationUnit> parseResults){
        String clazzName = Arrays.stream(clazz.getName().split("[.]")).reduce((first, second) -> second).get();
        var classOrInterface = getClassOrInterface(clazzName, parseResults);
        var imports = classOrInterface.getParentNode().map(it -> (CompilationUnit) it).get().getImports();
        return imports.stream().map(it -> "import " + it.getName().asString() + ";\n").collect(Collectors.joining());
    }

    public static List<ParameterInfo> getParameters(Method method, List<CompilationUnit> parseResults) {
        var classOrInterface = getClassOrInterface(getMethodClassName(method), parseResults);
        var methodInfo = getMethod(method, classOrInterface);
        return methodInfo.getParameters().stream().map(it -> new ParameterInfo(it.getType().asString(), it.getNameAsString())).collect(Collectors.toList());
    }
}
