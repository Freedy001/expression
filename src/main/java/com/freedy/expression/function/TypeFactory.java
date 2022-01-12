package com.freedy.expression.function;

import com.freedy.expression.utils.PlaceholderParser;
import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2022/1/12 11:38
 */
public abstract class TypeFactory<A, B, C, D, E, F, G, H, Z> {

    public TypeFactory(VarConsumer._1ParameterConsumer<A> f) {}

    public TypeFactory(VarConsumer._2ParameterConsumer<A, B> f) {}

    public TypeFactory(VarConsumer._3ParameterConsumer<A, B, C> f) {}

    public TypeFactory(VarConsumer._4ParameterConsumer<A, B, C, D> f) {}

    public TypeFactory(VarConsumer._5ParameterConsumer<A, B, C, D, E> f) {}

    public TypeFactory(VarConsumer._6ParameterConsumer<A, B, C, D, E, F> f) {}

    public TypeFactory(VarConsumer._7ParameterConsumer<A, B, C, D, E, F, G> f) {}

    public TypeFactory(VarConsumer._9ParameterConsumer<A, B, C, D, E, F, G> f) {}

    public TypeFactory(VarConsumer._10ParameterConsumer<A, B, C, D, E, F, G, H> f) {}

    public TypeFactory(VarFunction._1ParameterFunction<A, Z> f) {}

    public TypeFactory(VarFunction._2ParameterFunction<A, B, Z> f) {}

    public TypeFactory(VarFunction._3ParameterFunction<A, B, C, Z> f) {}

    public TypeFactory(VarFunction._4ParameterFunction<A, B, C, D, Z> f) {}

    public TypeFactory(VarFunction._5ParameterFunction<A, B, C, D, E, Z> f) {}

    public TypeFactory(VarFunction._6ParameterFunction<A, B, C, D, E, F, Z> f) {}

    public TypeFactory(VarFunction._7ParameterFunction<A, B, C, D, E, F, G, Z> f) {}

    public TypeFactory(VarFunction._9ParameterFunction<A, B, C, D, E, F, G, Z> f) {}

    public TypeFactory(VarFunction._10ParameterFunction<A, B, C, D, E, F, G, H, Z> f) {}


    @SneakyThrows
    public static void main(String[] args) {
        FileInputStream i1 = new FileInputStream("C:\\Users\\Freedy\\Desktop\\code\\expression\\src\\main\\java\\com\\freedy\\expression\\function\\VarConsumer.java");
        FileInputStream i2 = new FileInputStream("C:\\Users\\Freedy\\Desktop\\code\\expression\\src\\main\\java\\com\\freedy\\expression\\function\\VarFunction.java");
        System.out.println(generate("VarConsumer", new String(i1.readAllBytes())));
        System.out.println(generate("VarFunction", new String(i2.readAllBytes())));
    }


    public static String generate(String base, String code) {
        StringBuilder builder = new StringBuilder();
        Pattern pattern = Pattern.compile("interface (.*?) extends " + base + " \\{");
        Matcher matcher = pattern.matcher(code);
        while (matcher.find()) {
            builder.append(new PlaceholderParser("public TypeFactory(? f) {}", base + "." + matcher.group(1)));
            builder.append("\n\n");
        }
        return builder.toString();
    }
}
