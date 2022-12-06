package com.freedy.expression;

import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;

/**
 * @author Freedy
 * @date 2022/10/4 10:22
 */
public class SysConstant {
public SysConstant(){}
    public final static boolean DEV = Boolean.parseBoolean(Optional.ofNullable(System.getProperty("funScriptDevMode")).orElse("false"));
    public final static Scanner SCANNER = new Scanner(System.in);
    public final static String CHARSET = System.getProperty("file.encoding") == null ? "UTF-8" : System.getProperty("file.encoding");
    public final static String SEPARATOR = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "\\\\" : "/";
}