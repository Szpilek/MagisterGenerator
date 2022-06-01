package com.tool;

import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.SourceRoot;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class JavaParser {
    static List<CompilationUnit> parse(String generatedPath) throws Exception {
        return new SourceRoot(Path.of(generatedPath))
                .tryToParse("").stream()
                .map(parseResult ->  {
                    if (parseResult.isSuccessful()) {
                        return parseResult.getResult().get();
                    } else {
                        var problemDescriptions = Utils.map(parseResult.getProblems(), Problem::toString);
                        throw new RuntimeException(String.join("\n", problemDescriptions));
                    }
                }).collect(Collectors.toList());
    }
}
