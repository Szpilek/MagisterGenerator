package com.tool;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
//    {
//        "type": "java.util.List",
//        "paramters" : [
//            {
//                "type": "java.lang.String",
//                "parameters" : []
//            }
//        ]
//    }
public class TypeInfo {
    //drzewo parametrów generycznych,
        List<TypeInfo> parameters;
        String name;
        TypeInfo(List<TypeInfo> parameters,
                 String type) {
            this.parameters = parameters;
            this.name = type;
        }
        static TypeInfo fromString(String str) {
            if (str.contains("<") && str.contains(">")) {
                String type = str.substring(0, str.indexOf("<"));
                var parameters = Arrays.stream(str.substring(str.indexOf("<")  + 1, str.lastIndexOf(">"))
                        .split(","))
                        .map(TypeInfo::fromString)
                        .collect(Collectors.toList());
                return new TypeInfo(parameters, type);

            } else {
                return new TypeInfo(List.of(), str);
            }
        }
}
