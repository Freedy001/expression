package com.freedy.expression.stander.standerFunc;

import com.freedy.expression.Expression;
import com.freedy.expression.stander.StanderEvaluationContext;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.Locale;

/**
 * @author Freedy
 * @date 2022/3/6 15:09
 */
public abstract class AbstractStanderFunc {
    public final static String CHARSET = System.getProperty("file.encoding") == null ? "UTF-8" : System.getProperty("file.encoding");
    protected final Expression selfExp = new Expression();
    protected final static String SEPARATOR = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "\\\\" : "/";

    @Setter
    protected StanderEvaluationContext context;


    @SneakyThrows
    protected Class<?> getClassByArg(Object arg) {
        if (arg instanceof String s) {
            return context.findClass(s);
        } else {
            if (arg == null) return null;
            return arg.getClass();
        }
    }
}
