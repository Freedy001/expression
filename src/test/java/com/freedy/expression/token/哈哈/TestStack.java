package com.freedy.expression.token.哈哈;

import java.net.URLEncoder;

/**
 * @author Freedy
 * @date 2022/11/9 17:41
 */
public class TestStack {


    public static void main(String[] args) {
        Class<TestStack> aClass = TestStack.class;
        String name = aClass.getName().replace(".", "/") + ".class";
        System.out.println(name);
        System.out.println(aClass.getProtectionDomain().getCodeSource().getLocation().getPath());
        System.out.println( aClass.getResource(URLEncoder.encode(name)));
    }

    public static void test1() {
        System.out.println("i am test1,last call" + Thread.currentThread().getStackTrace()[2]);
        test2();
    }

    public static void test2() {
        System.out.println("i am test2,last call" + Thread.currentThread().getStackTrace()[2]);
    }
}
