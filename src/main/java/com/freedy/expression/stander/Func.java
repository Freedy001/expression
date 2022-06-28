package com.freedy.expression.stander;

import com.freedy.expression.core.EvaluationContext;
import com.freedy.expression.core.Expression;
import com.freedy.expression.core.TokenStream;
import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.exception.IllegalArgumentException;
import com.freedy.expression.exception.StopSignal;
import com.freedy.expression.function.VarFunction;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Proxy;

/**
 * @author Freedy
 * @date 2022/1/4 17:36
 */
@Setter
@Getter
public class Func implements VarFunction._1ParameterFunction<Object, Object> {

    private EvaluationContext superContext;
    private String funcName;
    private String[] argName;
    private TokenStream funcBody;

    public Func(EvaluationContext superContext) {
        this.superContext = superContext;
    }

    @Override
    public Object apply(Object... obj) {
        if (obj != null && obj.length != argName.length) {
            throw new IllegalArgumentException("unmatched args ?(?*)", funcName, argName);
        }
        StanderEvaluationContext subContext = new StanderEvaluationContext(superContext);
        if (obj != null) {
            for (int i = 0; i < argName.length; i++) {
                subContext.setVariable(argName[i], obj[i]);
            }
        }

        Expression expression = new Expression(funcBody, (EvaluationContext) Proxy.newProxyInstance(subContext.getClass().getClassLoader(), new Class[]{EvaluationContext.class}, (proxy1, method, args) -> {
            String methodName = method.getName();
            if (methodName.equals("getVariable")) {
                String varName = (String) args[0];
                //@ 开头直接去父容器拿值
                if (varName.startsWith("@")) {
                    if (superContext.containsVariable(varName)) {
                        return superContext.getVariable(varName);
                    }
                    throw new EvaluateException("no var ? in the context", varName);
                }
                //# 开头先去子容器拿值如果没有就去父容器拿值
                if (subContext.containsVariable(varName)) {
                    return subContext.getVariable(varName);
                }
                if (superContext.containsVariable(varName)) {
                    return superContext.getVariable(varName);
                }
                throw new EvaluateException("no var ? in the context", varName);
            }
            if (methodName.equals("setVariable")) {
                String varName = (String) args[0];
                StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
                if (stackTraceElement.getClassName().equals("com.freedy.expression.token.ObjectToken")) {
                    if (!subContext.containsVariable(varName)) {
                        return subContext.setVariable(varName, args[1]);
                    }
                    throw new EvaluateException("you have already def ?", varName);
                } else if (stackTraceElement.getClassName().equals("com.freedy.expression.token.LoopToken")) {
                    return subContext.setVariable(varName, args[1]);
                } else {
                    if (varName.startsWith("@")) {
                        return superContext.setVariable(varName, args[1]);
                    }
                    if (subContext.containsVariable(varName)) {
                        return subContext.setVariable(varName, args[1]);
                    }
                    if (superContext.containsVariable(varName)) {
                        return superContext.setVariable(varName, args[1]);
                    }
                    return subContext.setVariable(varName, args[1]);
                }
            }
            if (methodName.matches("containsVariable")) {
                String varName = (String) args[0];
                String className = Thread.currentThread().getStackTrace()[3].getClassName();
                if (className.equals("com.freedy.expression.token.LoopToken") || className.equals("com.freedy.expression.token.ObjectToken")) {
                    return subContext.containsVariable(varName);
                }
                if (!varName.startsWith("@")) {
                    if (subContext.containsVariable(varName)) {
                        return true;
                    }
                }
                return superContext.containsVariable(varName);
            }
            if (methodName.matches("containsFunction|getFunction|registerFunction")) {
                return method.invoke(superContext, args);
            }
            return method.invoke(subContext, args);
        }));

        try {
            return expression.getValue();
        } catch (Throwable e) {
            StopSignal signal = StopSignal.getInnerSignal(e);
            if (signal != null) {
                if (signal.getSignal().contains("return")) {
                    TokenStream subStream = signal.getReturnStream();
                    if (subStream != null) {
                        expression.setTokenStream(subStream);
                        return expression.getValue();
                    }
                    return null;
                }
            }
            throw e;
        }
    }

}
