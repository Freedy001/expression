package com.freedy.expression.stander;

import com.freedy.expression.core.EvaluationContext;
import com.freedy.expression.core.Expression;
import com.freedy.expression.core.TokenStream;
import com.freedy.expression.exception.IllegalArgumentException;
import com.freedy.expression.exception.StopSignal;
import com.freedy.expression.function.VarFunction;
import com.freedy.expression.utils.PlaceholderParser;
import lombok.Getter;
import lombok.Setter;

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
            throw new IllegalArgumentException("wrong number of arguments,reference ?(?*)", funcName, argName);
        }
        FuncEvalCtx funcCtx = new FuncEvalCtx(superContext);
        if (obj != null) for (int i = 0; i < argName.length; i++) funcCtx.putVar(argName[i], obj[i]);

        Expression expression = new Expression(funcBody, funcCtx);
        try {
            return expression.getValue();
        } catch (Throwable e) {
            StopSignal signal = StopSignal.getInnerSignal(e);
            if (signal == null) throw e;
            if (signal.getSignal().contains("return")) {
                TokenStream subStream = signal.getReturnStream();
                if (subStream != null) {
                    expression.setTokenStream(subStream);
                    return expression.getValue();
                }
            }
            return null;
        }
    }

    @Override
    public String toString() {
        return new PlaceholderParser("[FUNCTION:?(?*)]", funcName, argName).ifEmptyFillWith("").toString();
    }
}
