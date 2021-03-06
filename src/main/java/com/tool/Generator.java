package com.tool;

import com.github.javaparser.ast.CompilationUnit;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.tool.ParserUtils.*;
import static com.tool.Utils.*;
import static com.tool.Configuration.SOURCE_PROJECT_PATH;
import static com.tool.Configuration.TARGET_PROJECT_PATH;
public class Generator {
    // TODO nie hardcodować
//    public final static String source_project_home = "/home/marta/Desktop/Magisterka/monolit/src/main/java/";
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

    static String clientImports = multilineString(
            "import org.springframework.web.client.RestTemplate;",
            "import org.springframework.http.*;"
    );

    public static void generateClients(Map<Class<?>, List<Class<?>>> dependenciesFromProject, List<CompilationUnit> compilationUnits, Class<?> homeClass) {
        Set<Class<?>> dependenciesToCreateClient = new HashSet<>();
        dependenciesFromProject.values().forEach(dependenciesToCreateClient::addAll);
        dependenciesToCreateClient.forEach(clazz -> {
            var methodInfos = Arrays.stream(clazz.getMethods())
                    .map(method -> {
                        String returnType = getReturnType(method, compilationUnits);
                        List<ParameterInfo> parameterInfos = getParameters(method, compilationUnits);
                        return new MethodInfo(method, method.getName(), parameterInfos, returnType, ReflectionUtils.getMethodClassName(method), method.getDeclaringClass().getPackageName());
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
            var clazz = getClassOrInterface(ReflectionUtils.getPrettyClassOrInterfaceName(it), compilationUnits);
            clazz.addSingleMemberAnnotation(Profile.class, quoted(ReflectionUtils.getPrettyClassOrInterfaceName(it)));
            clazz.tryAddImportToParentCompilationUnit(Profile.class);
            writeFile(Configuration.TARGET_JAVA_PATH, it.getName().replace(".", "/") + ".java", clazz.getParentNode().get().toString());
        });

        compilationUnits.forEach(it -> System.out.println(it));
    }

    private static String findSpringProfilesForClient(Map<Class<?>, List<Class<?>>> dependenciesFromProject, Class<?> clazz){
        return dependenciesFromProject.keySet().stream()
                .filter(key ->
                        dependenciesFromProject.get(key)
                                .stream().anyMatch(value -> value.isAssignableFrom(clazz)))
                .map(ReflectionUtils::getPrettyClassOrInterfaceName)
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
        "public class "+ ReflectionUtils.getPrettyClassOrInterfaceName(clazz) + "Lambda implements RequestStreamHandler {",
        "    private static ApplicationContext context;",
        "    static {",
        "        try {",
        "            var a = SpringBootLambdaContainerHandler.getAwsProxyHandler(" + ReflectionUtils.getPrettyClassOrInterfaceName(homeClass) + ".class, "+ quoted(ReflectionUtils.getPrettyClassOrInterfaceName(clazz)) + ");",
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
        writeFile(Configuration.TARGET_JAVA_PATH, clazz.getName().replace(".", "/") + "Lambda.java", classString);
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
            File targetFile = new File(Configuration.TARGET_JAVA_PATH + "com/tool/communication_model/"+ className +".java");
            targetFile.getParentFile().mkdirs();
            var classFileBytes = Generator.class.getClassLoader().getResourceAsStream(className + ".java").readAllBytes();
            //                    Paths.get("/home/marta/Desktop/Magisterka/graphSolverReflections/src/main/java/com/tool/communication_model/"+ className +".java"),
            Files.write(
                    // TODO nie hardcodować
                    Paths.get(Configuration.TARGET_JAVA_PATH + "com/tool/communication_model/"+ className +".java"),
                    classFileBytes);
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
                        clientImports,
                        profiles,
                        "public class " + interfaceName + "Client implements " + interfaceName + "{",
                        "String lambdaUrl = System.getenv(\""+ interfaceName +"Url\");",
                        methodInfos.stream().map(it -> methodForClient(it, interfaceName)).collect(Collectors.joining()),
                        "}"
                    );
        writeFile(Configuration.TARGET_JAVA_PATH, clazz.getName().replace(".", "/") + "Client.java", s);
    }

    private static String methodForClient(MethodInfo mi, String serviceName) {
        return multilineString(
                "@Override\n" + Modifier.toString(mi.getMethod().getModifiers()).replace("abstract", ""),
                " " + mi.getReturnType() + " " + mi.getName(),
                " (" + mi.getParameters().stream().map(it -> it.getType() + " " + it.getName()).collect(Collectors.joining(", ")),
                "){",
                clientMethodBody(mi, serviceName),
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
                : "return mapper.readValue(responseEntity.getBody(), new TypeReference<"+ mi.getReturnType() +">(){});";

        return multilineString(
                "ObjectMapper mapper = new ObjectMapper();",
                "try {",
                    "String body = mapper.writeValueAsString("+ remoteCall + ");",
                    "RestTemplate restTemplate = new RestTemplate();",
                    "HttpHeaders headers = new HttpHeaders();",
                    "headers.setContentType(MediaType.APPLICATION_JSON);",
                    "HttpEntity<String> request = new HttpEntity<String>(body, headers);",
                    "ResponseEntity<String> responseEntity = restTemplate.postForEntity(lambdaUrl, request, String.class);",
                    "if(HttpStatus.OK == responseEntity.getStatusCode()){",
                    returnValue,
                    "} else {",
                    "throw new RuntimeException(\"Unexpected HTTP Status \" + responseEntity.getStatusCode());",
                    "}",
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
