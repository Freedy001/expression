package com.freedy.expression.core.token;

import com.freedy.expression.core.function.Functional;
import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.exception.IllegalArgumentException;
import com.freedy.expression.standard.Func;
import com.freedy.expression.standard.StandardEvaluationContext;
import com.freedy.expression.utils.ReflectionUtils;
import com.freedy.expression.utils.StringUtils;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author Freedy
 * @date 2021/12/15 14:06
 */
@Getter
@Setter
public final class DirectAccessToken extends ReflectToken implements Assignable {

    private String varName;
    private String methodName;
    private String[] methodArgsName;
    private List<UnsteadyArg> unsteadyArgList;
    private boolean isInlineFunc;

    public DirectAccessToken(String value) {
        super("directAccess", value);
    }

    public void setMethodArgsName(String[] methodArgsName) {
        this.methodArgsName = methodArgsName;
        this.unsteadyArgList = preprocessingArgs(methodArgsName);
    }

    @Override
    protected Object doCalculate(Class<?> desiredType) {
        return checkAndSelfOps(executeSelf(executableCount, true));
    }

    @Override
    protected Object doGenericCalculate(ParameterizedType desiredType) {
        return checkAndSelfOps(executeSelf(executableCount, true));
    }

    @Override
    public void assignFrom(ExecutableToken assignment) {
        ExecuteStep step = getLastPropertyStep();
        if (step != null) {
            relevantAssign(
                    step.getRelevantOps(),
                    () -> executeSelf(executableCount, false),
                    () -> assignment.calculateResult(ANY_TYPE),
                    () -> doChainAssign(assignment, step)
            );
            return;
        }
        if (StringUtils.isEmpty(varName)) {
            throw new EvaluateException("can not assign! because no varName").errToken(this.errStr(varName));
        }
        relevantAssign(
                relevantOps,
                () -> executeSelf(executableCount, false),
                () -> assignment.calculateResult(ANY_TYPE),
                () -> {
                    Object result = assignment.calculateResult(ANY_TYPE);
                    if (ReflectionUtils.hasField(context.getRoot(), varName)) {
                        ReflectionUtils.setter(context.getRoot(), varName, result);
                    } else if (context.containsVariable(varName)) {
                        context.setVariable(varName, result);
                    } else {
                        throw new EvaluateException("you must def ? first", varName).errToken(this.errStr(varName));
                    }
                }
        );

    }

    private void doChainAssign(ExecutableToken assignment, ExecuteStep step) {
        Object last = executeSelf(executableCount - 1, false);
        if (last == null) {
            throw new EvaluateException("can not assign! because the execute chain return a null value").errToken(this);
        }
        Type desiredType = Objects.requireNonNull(ReflectionUtils.getFieldRecursion(last.getClass(), step.getPropertyName())).getGenericType();
        Object result = assignment.calculateResult(desiredType);
        ReflectionUtils.setter(last, step.getPropertyName(), result);
    }


    private Object executeSelf(int executeCount, boolean executeChainTailOps) {
        checkContext();
        if (StringUtils.hasText(varName)) {
            Object root = context.getRoot();

            Object result;
            if (!ReflectionUtils.hasField(root, varName)) {
                if (!context.containsVariable(varName)) {
                    throw new EvaluateException("? is not defined", varName);
                }
                result = executeCount > 0 || executeChainTailOps ? doRelevantOps(context.getVariable(varName), varName) : context.getVariable(varName);
            } else {
                result = executeCount > 0 || executeChainTailOps ? doRelevantOps(ReflectionUtils.getter(root, varName), varName) : ReflectionUtils.getter(root, varName);
            }
            if (result == null) {
                return checkAndSelfOps(null);
            }
            return executeChain(result.getClass(), result, executeCount, executeChainTailOps);
        } else {
            Object funcObj = context.getFunction(methodName);
            if (funcObj == null) thrNoSuchFunctionException();
            Object[] args = getMethodArgs(unsteadyArgList);
            Object invoke;
            try {
                Method method;
                if (funcObj instanceof StandardEvaluationContext.FunctionalMethod func) {
                    funcObj = func.funcObj();
                    method = func.func();
                    if (isInlineFunc && func.cmdParamMap() != null && checkArrType(args, 0, args.length) == String.class) {
                        //启用自定义参数解析
                        args = new Object[]{func.parseArgs(Arrays.copyOfRange(args, 0, args.length, String[].class))};
                    }
                } else {
                    //noinspection ConstantConditions
                    Class<?> functionClass = funcObj.getClass();
                    Class<?>[] ins = functionClass.getInterfaces();
                    if (ins.length > 1 && Functional.class.isAssignableFrom(ins[0])) {
                        throw new EvaluateException("invalid function,function class must impl Functional");
                    }
                    method = ins[0].getDeclaredMethods()[0];
                }
                method.setAccessible(true);
                Class<?>[] types = method.getParameterTypes();
                int count = types.length;
                if (args != null) {
                    if (method.isVarArgs()) {
                        Object[] nArgs = new Object[count];
                        //拷贝非可变参数参数
                        System.arraycopy(args, 0, nArgs, 0, count - 1);
                        //检测可变参数数组的所有元素是否都是相同类型
                        Class<?> eleType = checkArrType(args, count - 1, args.length);
                        Object[] varArg;
                        //拷贝可变数组
                        if (eleType != null) {
                            //noinspection unchecked
                            varArg = Arrays.copyOfRange(args, count - 1, args.length, (Class<Object[]>) eleType.arrayType());
                        } else {
                            //noinspection unchecked
                            varArg = Arrays.copyOfRange(args, count - 1, args.length, (Class<Object[]>) types[count - 1]);
                        }
                        nArgs[count - 1] = varArg;
                        args = nArgs;
                    }
                    //解析 Lambda

                    for (int i = 0; i < args.length; i++) {
                        if (args[i] instanceof Func func && func.getAdapter() != null) {
                            args[i] = func.getAdapter().getInstance(types[i]);
                        }
                    }
                }


                invoke = method.invoke(funcObj, args);


            } catch (Exception e) {
                Throwable cause = e.getCause();
                if (cause != null && cause.getClass() == ClassCastException.class) {
                    throw new EvaluateException("class cast failed,please check you delivery param. cause: ?", e.getCause());
                }
                throw new EvaluateException("invoke target function ? failed!->?", getFullMethodName(methodName, methodArgsName), (e instanceof InvocationTargetException ex) ? ex.getCause() : e).errToken(this.errStr(methodArgsName));
            }
            Object result = executeCount > 0 || executeChainTailOps ? doRelevantOps(invoke, getFullMethodName(methodName, methodArgsName)) : invoke;
            if (result == null) {
                return checkAndSelfOps(null);
            }
            return executeChain(result.getClass(), result, executeCount, executeChainTailOps);
        }
    }

    private void thrNoSuchFunctionException() {
        StringJoiner joiner = new StringJoiner(",");
        for (String s : context.getFunctionNameSet()) {
            String methodName = this.methodName.toLowerCase(Locale.ROOT);
            String lowerCase = s.toLowerCase(Locale.ROOT);
            if (lowerCase.contains(methodName) || methodName.contains(lowerCase)) {
                joiner.add(s + "(unknown args)");
            }
        }
        throw new EvaluateException("no such function ?,you can call these function: ?", getFullMethodName(methodName, methodArgsName), joiner).errToken(this.errStr(methodName));
    }

    private Class<?> checkArrType(Object[] arr, int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException("start index ? gt end index ?", start, end);
        }
        if (start >= arr.length) {
            return null;
        }
        Class<?> cl = arr[start].getClass();
        for (int i = start + 1; i < end; i++) {
            if (arr[i].getClass() != cl) {
                return null;
            }
        }
        return cl;
    }
}
