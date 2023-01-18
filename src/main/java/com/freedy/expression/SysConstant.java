package com.freedy.expression;

import com.freedy.expression.standard.standardFunc.StandardNet;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Freedy
 * @date 2022/10/4 10:22
 */
public record SysConstant() {
    public final static boolean DEV = Boolean.parseBoolean(Optional.ofNullable(System.getProperty("funScriptDevMode")).orElse("false"));
    public final static String CHARSET = System.getProperty("file.encoding") == null ? "UTF-8" : System.getProperty("file.encoding");
    public final static String SEPARATOR = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "\\\\" : "/";
    public final static StandardNet.ConnectConfig DEFAULT_CONNECT_CONFIG = new StandardNet.ConnectConfig("noTag", "127.0.0.1", "2678", "0123456789abcdef", "anyString");
    public final static String DEFAULT_KEY = DEFAULT_CONNECT_CONFIG.toString();
    public final static boolean JAR_ENV =  Objects.requireNonNull(SysConstant.class.getResource("")).getProtocol().equals("jar");;
    public final static String DETACH ="uninstall";
}
