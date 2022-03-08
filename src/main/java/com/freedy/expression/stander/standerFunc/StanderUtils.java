package com.freedy.expression.stander.standerFunc;

import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.stander.CodeDeCompiler;
import com.freedy.expression.stander.ExpressionFunc;
import com.freedy.expression.utils.ReflectionUtils;
import com.freedy.expression.utils.StringUtils;
import lombok.Cleanup;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * @author Freedy
 * @date 2022/3/6 15:38
 */
public class StanderUtils extends AbstractStanderFunc {

    @ExpressionFunc("list function")
    public List<String> lf(Object o) {
        return getMethod(getClassByArg(o));
    }

    @ExpressionFunc("list variable")
    public List<String> lv(Object o) {
        Class<?> oClass = getClassByArg(o);
        return ReflectionUtils.getFieldsRecursion(oClass).stream().map(field -> {
            field.setAccessible(true);
            Type fieldType = field.getGenericType();
            String fieldTypeName = "?";
            if (fieldType instanceof ParameterizedType parameterizedType) {
                String genericStr = getParameterizedType(parameterizedType);
                fieldTypeName = ((Class<?>) parameterizedType.getRawType()).getSimpleName() + genericStr;
            } else if (fieldType instanceof Class<?> clazz) {
                fieldTypeName = clazz.getSimpleName();
            }
            try {
                return (Modifier.isStatic(field.getModifiers()) ? "\033[92mstatic \033[0;39m" : "") + "\033[91m" + fieldTypeName + "\033[0:39m \033[93m" + field.getName() + "\033[0;39m \033[34m" + (Modifier.isStatic(field.getModifiers()) ? getVarString(field.get(null)) : getVarString(field.get(o))) + "\033[0;39m";
            } catch (Exception e) {
                throw new EvaluateException("?", e);
            }
        }).collect(Collectors.toList());
    }


    @ExpressionFunc("decompile class to java source")
    public String code(Object ...o) {
        if (o.length != 1 && o.length != 2) {
            throw new EvaluateException("parameters count must be 1 or 2");
        }
        if (o.length == 1) {
            return CodeDeCompiler.getCode(getClassByArg(o[0]), false, "");
        }
        return CodeDeCompiler.getCode(getClassByArg(o[0]), false, String.valueOf(o[1]));
    }


    @ExpressionFunc("decompile class to java source and dump source file to specific location")
    public void dumpCode(Object ...o) {
        if (o.length != 2 && o.length != 3) {
            throw new EvaluateException("parameters count must be 2 or 3");
        }
        Class<?> clazz = getClassByArg(o[0]);
        if (o.length == 2) {
            dump(clazz, "", String.valueOf(o[1]));
            return;
        }
        dump(clazz, String.valueOf(o[1]), String.valueOf(o[2]));
    }

    @ExpressionFunc("list all inner class")
    public List<Class<?>> lic(Object o) {
        return List.of(getClassByArg(o).getDeclaredClasses());
    }

    @ExpressionFunc("detail help")
    public void help(String funcName) {
        context.getSelfFuncHelp().forEach((k, v) -> {
            if (k.toLowerCase(Locale.ROOT).contains(funcName.toLowerCase(Locale.ROOT))) {
                System.out.println("function:");
                System.out.println("\t\033[95m" + k + "\033[0;39m");
                System.out.println("explain:");
                System.out.println("\t\033[34m" + v.replace("\n", "\n    ") + "\033[0;39m");
                System.out.println();
                System.out.println();
            }
        });
    }

    @ExpressionFunc("clear specific var")
    public void clearVar(String ...varName){
        if (varName == null) return;
        for (String s : varName) {
            context.getVariableMap().remove(context.filterName(s));
        }
    }



    private List<String> getMethod(Class<?> aClass) {
        if (aClass == null) return null;
        List<String> list = new ArrayList<>();
        for (Method method : aClass.getDeclaredMethods()) {
            StringJoiner joiner = new StringJoiner(",", "(", ")");
            for (Class<?> type : method.getParameterTypes()) {
                joiner.add(type.getSimpleName());
            }
            list.add((Modifier.isStatic(method.getModifiers()) ? "\033[91mstatic \033[0;39m\033[94m" : "\033[94m") +
                    method.getReturnType().getSimpleName() + "\033[0;39m \033[93m" +
                    method.getName() + "\033[0;39m\033[95m" + joiner + "\033[39m");
        }
        return list;
    }

    private String getParameterizedType(ParameterizedType parameterizedType) {
        StringJoiner joiner = new StringJoiner(",", "<", ">");
        for (Type type : parameterizedType.getActualTypeArguments()) {
            if (type instanceof ParameterizedType sub) {
                joiner.add(((Class<?>) sub.getRawType()).getSimpleName() + getParameterizedType(sub));
            } else if (type instanceof Class<?> clazz) {
                joiner.add(clazz.getSimpleName());
            }
        }
        return joiner.toString();
    }

    private String getVarString(Object obj) {
        String str = String.valueOf(obj);
        if (str.contains("\n")) {
            return "[" + str.replaceAll("\n", ",") + "]";
        }
        return str;
    }

    @SneakyThrows
    private void dump(Class<?> clazz, String method, String path) {
        File file = new File(String.valueOf(path));
        if (!file.isDirectory()) {
            throw new EvaluateException("path must be directory");
        }
        file = new File(file, StringUtils.hasText(method) ? clazz.getSimpleName() + ".txt" : clazz.getSimpleName() + ".java");
        @Cleanup FileOutputStream stream = new FileOutputStream(file);
        stream.write(CodeDeCompiler.getCode(clazz, true, method).getBytes(StandardCharsets.UTF_8));
        System.out.println("dump success to " + file.getAbsolutePath());
    }
}
