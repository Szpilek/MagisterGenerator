package com.tool;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    public static void classParser(List<ParseResult<CompilationUnit>> parseResults){
        List<String> nodes = new ArrayList<>();
        VoidVisitor<List<String>> classCollector = new ClassNameCollector();
        for(ParseResult<CompilationUnit> r : parseResults){
            if (r.getResult().isEmpty()){
                continue;
            }
            classCollector.visit(r.getResult().get(), nodes);
            nodes.forEach(n -> {
                System.out.println("Class Name Collected: " + n);
            });
        }
    }

    private static class ClassNameCollector extends VoidVisitorAdapter<List<String>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration md, List<String> collector) {
            super.visit(md, collector);
            collector.add("Nodes " + md.getChildNodes());
            //md.getChildNodes().get(7).getChildNodes().get(4) w debbugerze aby sprawdzic typ zwrtony
//            md.findAll()
            //znaleźć node ktory jest klasy class declaration ktory ma parent node któy jest klasy, w child nodach szukam method declaration który jest tej samemj nazwy a potem typ zwrotny
        }
    }
}
