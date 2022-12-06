package com.freedy.expression;

import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;

public class JavaAdapter {


    public static byte[] readAllBytes(InputStream is) throws IOException {
        int available = is.available();
        byte[] b = new byte[available];
        //noinspection ResultOfMethodCallIgnored
        is.read(b);
        return b;
    }

    public static String repeat(String str, int time) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < time; i++) {
            builder.append(str);
        }
        return builder.toString();
    }

    @SneakyThrows
    public static Class<?> arrayType(Class<?> type) {
        return Array.newInstance(type, 0).getClass();
    }


}
