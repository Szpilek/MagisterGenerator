package com.tool;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
    public static <T> boolean  anyMatch(Collection<T> coll, Predicate<T> predicate) {
        return coll.stream().anyMatch(predicate);
    }

    public static <T> Set<T> combineToSet(Collection<T> coll1, Collection<T> coll2 ) {
        return Stream.concat(coll1.stream(), coll2.stream())
                .collect(Collectors.toSet());
    }

    public static <T, N> List<N> map(Collection<T> coll, Function<T, N> function){
        return coll.stream().map(function).collect(Collectors.toList());
    }
}
