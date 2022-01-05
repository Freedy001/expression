package com.freedy.expression.token;

import com.freedy.expression.function.Functional;
import com.freedy.expression.function.VarargsFunction;
import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.utils.ReflectionUtils;
import com.freedy.expression.utils.StringUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Freedy
 * @date 2021/12/15 14:06
 */
@Getter
@Setter
@NoArgsConstructor
public final class DirectAccessToken extends ClassToken implements Assignable {

    private String varName;
    private String methodName;
    private String[] methodArgsName;
    private List<UnsteadyArg> unsteadyArgList;

    public DirectAccessToken(String value) {
        super("directAccess", value);
    }

    public void setMethodArgsName(String[] methodArgsName) {
        this.methodArgsName = methodArgsName;
        this.unsteadyArgList=preprocessingArgs(methodArgsName);
    }

    @Override
    protected Object doCalculate(Class<?> desiredType) {
        return checkAndSelfOps(executeSelf(executableCount, true));
    }


    @Override
    public void assignFrom(Token assignment) {
        ExecuteStep step = getLastPropertyStep();
        if (step == null) {
            if (StringUtils.isEmpty(varName)) {
                throw new EvaluateException("can not assign! because no varName").errToken(this.getNextToken());
            }
            relevantAssign(
                    relevantOps,
                    () -> executeSelf(executableCount, false),
                    () -> assignment.calculateResult(Token.ANY_TYPE),
                    () -> {
                        Object result = assignment.calculateResult(Token.ANY_TYPE);
                        if (ReflectionUtils.hasField(context.getRoot(), varName)) {
                            ReflectionUtils.setter(context.getRoot(), varName, result);
                        } else {
                            context.setVariable(varName, result);
                        }
                    }
            );
            return;
        }
        relevantAssign(
                step.getRelevantOps(),
                () -> executeSelf(executableCount, false),
                () -> assignment.calculateResult(Token.ANY_TYPE),
                () -> doChainAssign(assignment, step)
        );
    }

    private void doChainAssign(Token assignment, ExecuteStep step) {
        Object last = executeSelf(executableCount - 1, false);
        if (last == null) {
            throw new EvaluateException("can not assign! because the execute chain return a null value").errToken(this.getNextToken());
        }
        Type desiredType = Objects.requireNonNull(ReflectionUtils.getFieldRecursion(last.getClass(), step.getPropertyName())).getGenericType();
        Object result = assignment.calculateResult(desiredType);
        ReflectionUtils.setter(context.getRoot(), varName, result);
    }


    private Object executeSelf(int executeCount, boolean executeLastRelevantOps) {
        checkContext();
        if (StringUtils.hasText(varName)) {
            Object root = context.getRoot();

            Object result;
            if (!ReflectionUtils.hasField(root, varName)) {
                result = doRelevantOps(context.getVariable(varName), varName);
            } else {
                result = doRelevantOps(ReflectionUtils.getter(root, varName), varName);
            }
            if (result == null) {
                return checkAndSelfOps(null);
            }
            return executeChain(result.getClass(), result, executeCount, executeLastRelevantOps);
        } else {
            Functional function = context.getFunction(methodName);
            if (function == null) {
                throw new EvaluateException("no such function ?", getFullMethodName(methodName, methodArgsName)).errToken(this.errStr(methodName));
            }
            Object[] args = getMethodArgs(unsteadyArgList);
            Object invoke;
            try {
                Class<? extends Functional> functionClass = function.getClass();
                Method method = functionClass.getInterfaces()[0].getDeclaredMethods()[0];
                method.setAccessible(true);
                VarargsFunction var = method.getAnnotation(VarargsFunction.class);
                if (var != null && args != null) {
                    int count = method.getParameterCount();
                    Object[] nArgs = new Object[count];
                    System.arraycopy(args, 0, nArgs, 0, count - 1);
                    Object[] varArg = Arrays.copyOfRange(args, count - 1, args.length);
                    nArgs[count - 1] = varArg;
                    args = nArgs;
                }
                invoke = method.invoke(function, args);
            } catch (Exception e) {
                throw new EvaluateException("invoke target function ? failed!->?", getFullMethodName(methodName, methodArgsName), (e instanceof InvocationTargetException ex) ? ex.getCause() : e).errToken(this.errStr(methodArgsName));
            }
            Object result = doRelevantOps(invoke, getFullMethodName(methodName, methodArgsName));
            if (result == null) {
                return checkAndSelfOps(null);
            }
            return executeChain(result.getClass(), result, executeCount, executeLastRelevantOps);
        }
    }

}
