package com.freedy.expression.token;

import com.alibaba.fastjson.annotation.JSONType;
import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.utils.ReflectionUtils;
import com.freedy.expression.utils.StringUtils;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * @author Freedy
 * @date 2021/12/14 15:51
 */
@Getter
@Setter
@JSONType(includes = {"type", "value", "nullCheck", "propertyName", "methodArgs"})
public final class ReferenceToken extends ReflectToken {

    public ReferenceToken(String token) {
        super("reference", token);
    }

    @Override
    protected Object doCalculate(Class<?> desiredType) {
        Object variable = doRelevantOps(getVariable(), reference);
        if (variable == null) return null;
        return checkAndSelfOps(executeChain(variable.getClass(), variable, executableCount));
    }


    @Override
    public void assignFrom(ExecutableToken assignment) {
        ExecuteStep step = getLastPropertyStep();
        Object variable = getVariable();
        if (step == null) {
            relevantAssign(
                    relevantOps,
                    () -> variable,
                    () -> assignment.calculateResult(ANY_TYPE),
                    () -> doAssign(assignment.calculateResult(ANY_TYPE))
            );
            return;
        }
        if (variable==null){
            throw new EvaluateException("? is null", reference).errToken(this.errStr(reference));
        }
        relevantAssign(
                step.getRelevantOps(),
                () -> executeChain(variable.getClass(), variable, executableCount,false),
                () -> assignment.calculateResult(ANY_TYPE),
                () -> doChainAssign(assignment, step, variable)
        );
    }


    private void doChainAssign(ExecutableToken assignment, ExecuteStep step, Object variable) {
        variable = executeChain(variable.getClass(), variable, executableCount - 1);
        if (variable==null){
            throw new EvaluateException("can not assign! because the execute chain return a null value").errToken(this);
        }
        Type desiredType = Objects.requireNonNull(ReflectionUtils.getFieldRecursion(variable.getClass(), step.getPropertyName())).getGenericType();
        Object result = assignment.calculateResult(desiredType);
        ReflectionUtils.setter(variable, step.getPropertyName(), result);
    }


    private void doAssign(Object result) {
        if (!context.containsVariable(reference)) {
            throw new EvaluateException("you must def ? first", reference);
        }
        context.setVariable(reference, result);
    }

    private Object getVariable() {
        if (StringUtils.isEmpty(reference)) {
            throw new EvaluateException("reference is null");
        }
        checkContext();
        if (!context.containsVariable(reference)) {
            if (reference.startsWith("%")) {
                return reference.substring(1);
            }
            throw new EvaluateException("? is not defined", reference).errToken(this.errStr(reference));
        }
        return context.getVariable(reference);
    }
}
