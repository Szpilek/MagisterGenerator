package com.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.CompilationUnit;
import com.tool.communication_model.RemoteCall;
import com.tool.communication_model.RemoteArgument;
import com.tool.communication_model.RemoteType;
import org.springframework.context.ApplicationContext;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

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
            "import com.tool.communication_model.RemoteCall;"
    );

    static String wrapperImports = multilineString(
            "import java.lang.reflect.InvocationTargetException;",
            "import com.fasterxml.jackson.databind.JavaType;",
            "import org.springframework.context.ApplicationContext;",
            "import java.io.IOException;",
            "import java.io.InputStream;",
            "import java.io.OutputStream;",
            "import java.util.stream.Collectors;"
    );

    public static void generateClients(Map<Class<?>, List<Class<?>>> dependenciesFromProject, List<CompilationUnit> compilationUnits) {
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

            Generator.generateClient(clazz, imports, methodInfos);
            Generator.generateServiceWrapper(clazz, imports, methodInfos);
        });
    }


    private static void generateServiceWrapper(Class<?> clazz, String imports, List<MethodInfo> methodInfos) {
       String classString = multilineString(
        imports,
        customImports,
        wrapperImports,
        "public class StreamLambdaHandler implements RequestStreamHandler {",
        "    private static ApplicationContext context;",
        "    private ObjectMapper objectMapper = new ObjectMapper();",
        "    static {",
        "        try {",
        "            context = SpringBootLambdaContainerHandler.getAwsProxyHandler(Application.class).get;",
        "        } catch (ContainerInitializationException e) {",
        "            e.printStackTrace();",
        "            throw new RuntimeException(\"Could not initialize Spring Boot application\", e);",
        "        }",
        "    }",
        "",
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
        "        var argTypes = toArray(arguments.stream().map(Object::getClass).collect(Collectors.toList());)",
        "        var serviceBean  = context.getBean(call.getService());",
        "        try {",
        "            return serviceBean.getClass().getMethod(call.getMethod(), argTypes)",
        "            .invoke(serviceBean, toArray(arguments));",
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
        "                toArray(typeParameters)",
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

    private static void generateClient(Class<?> clazz, String imports, List<MethodInfo> methodInfos) {
        String interfaceName = clazz.getName().replace(clazz.getPackageName() + ".", "");
        String s = multilineString(
                        clazz.getPackage().toString() + ";",
                        imports,
                        customImports,
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
