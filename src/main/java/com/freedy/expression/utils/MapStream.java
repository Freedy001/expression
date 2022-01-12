package com.freedy.expression.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Freedy
 * @date 2022/1/10 10:12
 */
public class MapStream {

    public static <K,V> Stream<Map.Entry<K,V>> stream(Map<K,V> map){
        return new ArrayList<>(map.entrySet()).stream();
    }

}
