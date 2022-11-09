package com.freedy.expression.token;

import com.freedy.expression.core.Expression;
import com.freedy.expression.core.TokenStream;
import com.freedy.expression.exception.EvaluateException;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Freedy
 * @date 2021/12/21 16:58
 */
@Setter
@Getter
public final class TernaryToken extends ExecutableToken {
    private TokenStream trueTokenStream;
    private TokenStream falseTokenStream;
    private ExecutableToken boolToken;

    public TernaryToken(String value) {
        super("ternary", value);
    }

    public void setBoolToken(ExecutableToken boolToken) {
        this.boolToken = boolToken;
    }

    @Override
    protected Object doCalculate(Class<?> desiredType) {
        try {
            Boolean result = (Boolean) boolToken.calculateResult(Boolean.class);
            if (result == null) {
                throw new EvaluateException("? calculate a null value", boolToken.getValue()).errToken(boolToken);
            }
            if (result) {
                return checkAndSelfOps(new Expression(trueTokenStream).getValue(context));
            } else {
                return checkAndSelfOps(new Expression(falseTokenStream).getValue(context));
            }
        } finally {
            trueTokenStream.close();
            falseTokenStream.close();
        }
    }
}
