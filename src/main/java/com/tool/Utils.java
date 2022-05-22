package com.tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
    public static <A> boolean  anyMatch(Collection<A> coll, Predicate<A> predicate) {
        return coll.stream().anyMatch(predicate);
    }

    public static <A> Set<A> combineToSet(Collection<A> coll1, Collection<A> coll2 ) {
        return Stream.concat(coll1.stream(), coll2.stream())
                .collect(Collectors.toSet());
    }

    public static <A, B> List<B> map(Collection<A> collection, Function<A, B> function){
        return collection.stream()
                .map(function)
                .collect(Collectors.toList());
    }

    static void writeFile(String filePath, String fileName, String content) {
        Path path = Paths.get(filePath + fileName);
        File targetFile = new File(filePath + fileName);
        try {
            targetFile.createNewFile();
            Files.write(path, content.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String quoted(String s) {
        return "\""+ s + "\"";
    }

    static String instantiate(String clazz, String... args) {
        return "new " + clazz + "(" + Arrays.stream(args).collect(Collectors.joining(",")) + ")";
    }
    static String multilineString(String... lines) {
        return Arrays.stream(lines).collect(Collectors.joining("\n"));
    }
    static <T> T[]  toArray(List<T> list) {
        return (T[]) list.toArray(new Object[list.size()]);
    }
}
