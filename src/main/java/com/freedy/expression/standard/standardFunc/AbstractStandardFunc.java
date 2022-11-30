package com.freedy.expression.standard.standardFunc;

import com.freedy.expression.core.Expression;
import com.freedy.expression.standard.StandardEvaluationContext;
import lombok.Setter;
import lombok.SneakyThrows;

/**
 * @author Freedy
 * @date 2022/3/6 15:09
 */
public abstract class AbstractStandardFunc {
    protected final Expression selfExp = new Expression();

    @Setter
    protected StandardEvaluationContext context;


    @SneakyThrows
    protected Class<?> getClassByArg(Object arg) {
        if (arg instanceof String s) {
            return context.findClass(s);
        } else if (arg instanceof Class<?> cl) {
            return cl;
        } else if (arg != null) {
            return arg.getClass();
        }
        return null;
    }


}
