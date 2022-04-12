package com.tool;

import com.example.monolit.MonolitApplication;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.tool.communication_model.RemoteCall;
import com.tool.communication_model.RemoteArgument;
import com.tool.communication_model.RemoteType;
import javassist.bytecode.annotation.Annotation;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

import static com.tool.ParserUtils.getPrettyClassOrInterfaceName;
import static com.tool.Utils.*;
import static com.tool.ParserUtils.*;

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
            "import com.tool.communication_model.RemoteCall;",
            "import org.springframework.context.annotation.Profile;"
    );

    static String wrapperImports = multilineString(
            "import java.lang.reflect.Field;",
            "import java.lang.reflect.InvocationTargetException;",
            "import com.fasterxml.jackson.databind.JavaType;",
            "import org.springframework.context.ApplicationContext;",
            "import com.amazonaws.serverless.exceptions.ContainerInitializationException;",
            "import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;",
            "import com.amazonaws.services.lambda.runtime.Context;",
            "import com.amazonaws.services.lambda.runtime.RequestStreamHandler;",
            "import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;",
            "import java.io.IOException;",
            "import java.io.InputStream;",
            "import java.io.OutputStream;",
            "import java.util.stream.Collectors;"
    );

    public static void generateClients(Map<Class<?>, List<Class<?>>> dependenciesFromProject, List<CompilationUnit> compilationUnits, Class<?> homeClass) {
        Set<Class<?>> dependenciesToCreateClient = new HashSet<>();
        dependenciesFromProject.values().forEach(dependenciesToCreateClient::addAll);
        dependenciesToCreateClient.forEach(clazz -> {
            var methodInfos = Arrays.stream(clazz.getMethods())
                    .map(method -> {
                        String returnType = getReturnType(method, compilationUnits);
                        List<ParameterInfo> parameterInfos = getParameters(method, compilationUnits);
                        return new MethodInfo(method, method.getName(), parameterInfos, returnType, getMethodClassName(method), method.getDeclaringClass().getPackageName());
                    }).collect(Collectors.toList());
            String imports = getImports(clazz, compilationUnits);
            var profiles = findSpringProfilesForClient(dependenciesFromProject, clazz);
            Generator.generateClient(clazz, imports, methodInfos, profiles);
//            Generator.generateServiceWrapper(clazz, imports, methodInfos, homeClass);
        });

        dependenciesFromProject.keySet().forEach(clazz -> {
            String imports = getImports(clazz, compilationUnits);
            Generator.generateServiceWrapper(clazz, imports, homeClass);
        });
    }

    public static void generateSpringProfiles(Map<Class<?>, List<Class<?>>> dependenciesFromProject, List<CompilationUnit> compilationUnits){
        dependenciesFromProject.keySet().forEach((it) -> {
            var clazz = getClassOrInterface(getPrettyClassOrInterfaceName(it), compilationUnits);
            clazz.addSingleMemberAnnotation(Profile.class, quoted(getPrettyClassOrInterfaceName(it)));
            clazz.tryAddImportToParentCompilationUnit(Profile.class);
            writeFile(home, it.getName().replace(".", "/") + ".java", clazz.getParentNode().get().toString());
        });

        compilationUnits.forEach(it -> System.out.println(it));
    }

    private static String findSpringProfilesForClient(Map<Class<?>, List<Class<?>>> dependenciesFromProject, Class<?> clazz){
        return dependenciesFromProject.keySet().stream()
                .filter(key ->
                        dependenciesFromProject.get(key)
                                .stream().anyMatch(value -> value.isAssignableFrom(clazz)))
                .map(ParserUtils::getPrettyClassOrInterfaceName)
                .map(it -> "@Profile(" + quoted(it) + ")")
                .collect(Collectors.joining("\n"));
    }

    private static void generateServiceWrapper(Class<?> clazz, String imports, Class<?> homeClass ) {
       String classString = multilineString(
        clazz.getPackage() + ";",
        "import " + homeClass.getName() + ";",
        imports,
        customImports,
        wrapperImports,
        "public class StreamLambdaHandler implements RequestStreamHandler {",
        "    private static ApplicationContext context;",
        "    static {",
        "        try {",
        "            var a = SpringBootLambdaContainerHandler.getAwsProxyHandler(" + getPrettyClassOrInterfaceName(homeClass) + ".class, "+ quoted(getPrettyClassOrInterfaceName(clazz)) + ");",
        "            Field field = SpringBootLambdaContainerHandler.class.getDeclaredField(\"applicationContext\");",
        "            field.setAccessible(true);",
        "            context = (ApplicationContext) field.get(a);",
        "        } catch (ContainerInitializationException | NoSuchFieldException | IllegalAccessException e) {",
        "            e.printStackTrace();",
        "            throw new RuntimeException(e);",
        "        }",
        "    }",
        "    private ObjectMapper objectMapper = new ObjectMapper();",
        "    @Override",
        "    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)",
        "            throws IOException {",
        "        RemoteCall call = objectMapper.readValue(inputStream, RemoteCall.class);",
        "        objectMapper.writeValue(outputStream, execute(call));",
        "    }",
        "    Object execute(RemoteCall call) {",
        "        var arguments = call.getArguments().stream()",
        "                .map(arg -> {",
        "                    try {",
        "                        return objectMapper.readValue(arg.getValue(), javaType(arg.getType()));",
        "                    } catch (JsonProcessingException e) {",
        "                        throw new RuntimeException(\"Cannot deserialize parameter\", e);",
        "                    }",
        "                }).collect(Collectors.toList());",
        "        var argTypes = arguments.stream().map(Object::getClass).collect(Collectors.toList());",
        "        var serviceBean  = context.getBean(call.getService());",
        "        try {",
        "            return serviceBean.getClass().getMethod(call.getMethod(), argTypes.toArray(Class[]::new))",
        "            .invoke(serviceBean, arguments.toArray(Object[]::new));",
        "        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {",
        "            throw new RuntimeException(e);",
        "        }",
        "",
        "    }",
        "",
        "    JavaType javaType(RemoteType remoteType) {",
        "        var simpleType = objectMapper.getTypeFactory().constructFromCanonical(remoteType.getType());",
        "        var typeParameters = remoteType.getParameters().stream()",
        "                .map(it -> javaType(it))",
        "                .collect(Collectors.toList());",
        "        return remoteType.getParameters().isEmpty()",
        "                ? simpleType",
        "                : objectMapper.getTypeFactory().constructParametricType(",
        "                simpleType.getRawClass(),",
        "                typeParameters.toArray(JavaType[]::new)",
        "        );",
        "    }",
        "}");
        writeFile(home, clazz.getName().replace(".", "/") + "Lambda.java", classString);
    }





    private static void generateControllerWrapper(Class<?> clazz, String imports, List<MethodInfo> methodInfos) {
        String s =multilineString(
                "public class StreamLambdaHandler implements RequestStreamHandler {",
                "    private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;",
                "    static {",
                "        try {",
                "            handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(Application.class);",
                "        } catch (ContainerInitializationException e) {",
                "            e.printStackTrace();",
                "            throw new RuntimeException(\"Could not initialize Spring Boot application\", e);",
                "        }",
                "    }",
                "",
                "    @Override",
                "    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)",
                "            throws IOException {",
                "        handler.proxyStream(inputStream, outputStream, context);",
                "    }",
                "}");
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

    private static void generateClient(Class<?> clazz, String imports, List<MethodInfo> methodInfos, String profiles) {
        String interfaceName = clazz.getName().replace(clazz.getPackageName() + ".", "");
        String s = multilineString(
                        clazz.getPackage().toString() + ";",
                        imports,
                        customImports,
                        profiles,
                        "public class " + interfaceName + "Client implements " + interfaceName + "{",
                        methodInfos.stream().map(Generator::methodForClient).collect(Collectors.joining()),
                        "}"
                    );
        writeFile(home, clazz.getName().replace(".", "/") + "Client.java", s);
    }

    private static String methodForClient(MethodInfo mi) {
        return multilineString(
                "@Override\n" + Modifier.toString(mi.getMethod().getModifiers()).replace("abstract", ""),
                " " + mi.getReturnType() + " " + mi.getName(),
                " (" + mi.getParameters().stream().map(it -> it.getType() + " " + it.getName()).collect(Collectors.joining(", ")),
                "){",
                clientMethodBody(mi, "asd"),
                "}"
        );
    }

    private static String clientMethodBody(MethodInfo mi, String serviceName) {
        String remoteCall = instantiate("RemoteCall" ,
                                quoted(serviceName),
                                quoted(mi.getName()),
                        "List.of(" +
                                mi.getParameters().stream().map(Generator::serializeParameter).collect(Collectors.joining(","))
                        + ")");

        String returnValue = "void".equals(mi.getReturnType())
                ? ""
                : "return mapper.readValue(mockReturn, new TypeReference<"+ mi.getReturnType() +">(){});";

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
}
