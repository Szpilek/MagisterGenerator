package com.tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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

    public static <A> List<A> combine(List<A> a, List<A> b){
        List<A> list = new ArrayList<A>();
        list.addAll(a);
        list.addAll(b);
        return list;
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

    static String indent(String s, int number){
        return Arrays.stream(s.split("\n")).map(i -> "  ".repeat(number) + i)
                .collect(Collectors.joining("\n"));
    }

    static String quoted(String s) {
        return "\""+ s + "\"";
    }

    static String instantiate(String clazz, String... args) {
        return "new " + clazz + "(" + Arrays.stream(args).collect(Collectors.joining(",")) + ")";
    }

    static String multilineString(List<String> lines) {
        return lines.stream().collect(Collectors.joining("\n"));
    }
    static String multilineString(String... lines) {
        return multilineString(Arrays.stream(lines).collect(Collectors.toList()));
    }
    static <T> T[]  toArray(List<T> list) {
        return (T[]) list.toArray(new Object[list.size()]);
    }
}
