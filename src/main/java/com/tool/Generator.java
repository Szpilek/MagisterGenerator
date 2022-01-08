package com.tool;

//import com.github.javaparser.JavaParser;
//import com.github.javaparser.ParseResult;
//import com.github.javaparser.StaticJavaParser;
//import com.github.javaparser.ast.CompilationUnit;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.symbolsolver.resolution.typeinference.TypeInference;
import org.springframework.remoting.RemoteAccessException;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

import static com.tool.Utils.*;

public class Generator {
    public static List<MethodInfo> methodInfos = new ArrayList<>();

    // TODO nie hardcodować
    public final static String home = "/home/marta/Desktop/Magisterka/monolit/src/main/java/";
    public static String customImports = multilineString(
            "import com.fasterxml.jackson.databind.ObjectMapper;",
            "import com.fasterxml.jackson.core.type.TypeReference;",
            "import com.fasterxml.jackson.core.JsonProcessingException;",
            "import com.tool.communication_model.RemoteType;",
            "import com.tool.communication_model.RemoteArgument;" ,
            "import com.tool.communication_model.RemoteCall;"
    );

    public static void generateClients(Map<Class<?>, List<Class<?>>> dependenciesFromProject, List<CompilationUnit> parseResults) {
        generateCommunicationModel();
        Set<Class<?>> dependenciesToCreateClient = new HashSet<>();
        dependenciesFromProject.values().forEach(dependenciesToCreateClient::addAll);
        dependenciesToCreateClient.forEach(it -> Generator.generateClient(it, parseResults));
    }

    public static void copyModelFile(String className) {
        try {
            File targetFile = new File(home + "com/tool/communication_model/"+ className +".java");
            targetFile.getParentFile().mkdirs();
            Files.copy(
                    // TODO nie hardcodować
                    Paths.get("/home/marta/Desktop/Magisterka/graphSolverReflections/src/main/java/com/tool/communication_model/"+ className +".java"),
                    Paths.get(home + "com/tool/communication_model/"+ className +".java"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void generateCommunicationModel(){
        List.of("RemoteArgument", "RemoteCall", "RemoteType")
                .forEach(Generator::copyModelFile);
    }

    public static void generateClient(Class<?> clazz, List<CompilationUnit> parseResults) {
        String interfaceName = clazz.getName().replace(clazz.getPackageName() + ".", "");
        String imports = getImports(clazz, parseResults);
        String s = multilineString(
                        clazz.getPackage().toString() + ";",
                        imports,
                        customImports,
                        "public class " + interfaceName + "Client implements " + interfaceName + "{"
                        );
        clazz.getGenericSuperclass();

//        Class genericParameter0OfThisClass = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];


        s += Arrays.stream(clazz.getMethods()).map(it -> Generator.generateMethodForClient(it, parseResults)).collect(Collectors.joining());
        s += "}";
        writeFile(home, clazz.getName().replace(".", "/") + "Client.java", s);
        System.out.println(s);
    }

    private static String generateMethodForClient(Method method, List<CompilationUnit> parseResults) {
        String returnType = getGenericReturnType(method, parseResults);
        List<ParameterInfo> parameterInfos = getParameters(method, parseResults);
        methodInfos.add(new MethodInfo(method.getName(), parameterInfos, returnType, getMethodClassName(method), method.getDeclaringClass().getPackageName()));

        String s = "@Override\n" + Modifier.toString(method.getModifiers()).replace("abstract", "")
                + " " + returnType + " " + method.getName()
                + " (" + parameterInfos.stream().map(it -> it.getType() + " " + it.getName()).collect(Collectors.joining(", "))
                + "){\n "
                + generateMethodBody(parameterInfos, method.getName(), "aaaa", returnType)
                + "}\n";

        return s;
    }

    private static String generateMethodBody(List<ParameterInfo> parameterInfos, String mthodName, String serviceName, String returnType) {
        String remoteCall = instantiate("RemoteCall" ,
                                quoted(serviceName),
                                quoted(serviceName),
                        "List.of(" +
                        parameterInfos.stream().map(Generator::serializeParameter).collect(Collectors.joining(","))
                        + ")");

        String returnValue = "void".equals(returnType)
                ? ""
                : "return mapper.readValue(mockReturn, new TypeReference<"+ returnType +">(){});";

        return multilineString(
                "ObjectMapper mapper = new ObjectMapper();",
                "try {",
                    "String body = mapper.writeValueAsString("+ remoteCall + ");",
                    "String mockReturn = \"{}\";",
                    returnValue,
                "} catch (JsonProcessingException e) {",
//                    TODO throw custom error ?
                     "throw new RuntimeException(e);",
                "}"

        );
    }

    static String serializeParameterType(TypeInfo type) {
        return instantiate("RemoteType",
                type.name + ".class.getName()",
                 "List.of(" + type.parameters.stream().map(Generator::serializeParameterType).collect(Collectors.joining(",")) + ")"
                );
    }

    static String serializeParameter(ParameterInfo parameter){
        return instantiate("RemoteArgument",
                            "mapper.writeValueAsString("+ parameter.getName() + ")" ,
                                serializeParameterType(TypeInfo.fromString(parameter.getType()))
                    );
    }

    private static String getGenericReturnType(Method method, List<CompilationUnit> parseResults) {
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

    private static List<ParameterInfo> getParameters(Method method, List<CompilationUnit> parseResults) {
        var classOrInterface = getClassOrInterface(getMethodClassName(method), parseResults);
        var methodInfo = getMethod(method, classOrInterface);
        return methodInfo.getParameters().stream().map(it -> new ParameterInfo(it.getType().asString(), it.getNameAsString())).collect(Collectors.toList());
    }

    private static ClassOrInterfaceDeclaration getClassOrInterface(String name, List<CompilationUnit> parseResults){
        return parseResults.stream()
                .map(it -> it.getClassByName(name).or(() -> it.getInterfaceByName(name)))
                .filter(Optional::isPresent)
                .findFirst().get().get();
    }

    private static MethodDeclaration getMethod(Method method, ClassOrInterfaceDeclaration classOrInterface){
        return classOrInterface.getChildNodes().stream()
                .filter(it -> it instanceof MethodDeclaration)
                .map(it -> (MethodDeclaration) it)
                .filter(it -> it.getName().asString().equals(method.getName()))
                .filter(it -> checkIfMethodParametersEqual(it.getParameters(), method.getParameters()))
                .findFirst().get();
    }

}
