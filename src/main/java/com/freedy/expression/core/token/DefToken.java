package com.freedy.expression.core.token;

import com.freedy.expression.core.TokenStream;
import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.standard.Func;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Freedy
 * @date 2021/12/21 22:47
 */
@Getter
@Setter
public final class DefToken extends ExecutableToken implements Assignable {
    private String variableName;

    private String methodName;
    private String[] methodArgs;
    private TokenStream methodBody;

    public DefToken(String value) {
        super("def", value);
    }

    public boolean isFunc(){
        return methodName!=null;
    }

    @Override
    public void assignFrom(ExecutableToken assignment) {
        if (isFunc()){
            throw new EvaluateException("function can't be assign!");
        }
        if (context.containsVariable(variableName)){
            throw new EvaluateException("You have defined variable ?",variableName);
        }
        context.setVariable(variableName, assignment.calculateResult(ANY_TYPE));
    }

    @Override
    protected Object doCalculate(Class<?> desiredType) {
        if (!isFunc()) return checkAndSelfOps(context.getVariable(variableName));
        Func func = new Func(context);
        func.setFuncName(methodName);
        func.setArgName(methodArgs);
        func.setFuncBody(methodBody);
        if (context.containsFunction(methodName)){
            throw new EvaluateException("You have defined function ?",methodName);
        }
        context.registerFunction(methodName,func);
        return func;
    }
}
