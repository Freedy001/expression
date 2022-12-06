package com.freedy.expression.stander.standerFunc;

import com.freedy.expression.core.Expression;
import com.freedy.expression.stander.CMDParameter;
import com.freedy.expression.stander.StanderEvaluationContext;
import com.freedy.expression.utils.ReflectionUtils;
import lombok.Setter;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

/**
 * @author Freedy
 * @date 2022/3/6 15:09
 */
public abstract class AbstractStanderFunc {
    protected final Expression selfExp = new Expression();

    @Setter
    protected StanderEvaluationContext context;


    @SneakyThrows
    protected Class<?> getClassByArg(Object arg) {
        if (arg instanceof String ) {
String s = (String) arg;
            return context.findClass(s);
        } else if (arg instanceof Class<?> ) {
Class<?> cl = (Class<?>) arg;
            return cl;
        } else if (arg != null) {
            return arg.getClass();
        }
        return null;
    }


}