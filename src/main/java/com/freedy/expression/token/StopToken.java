package com.freedy.expression.token;

import com.freedy.expression.core.TokenStream;
import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.exception.StopSignal;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Freedy
 * @date 2021/12/23 20:57
 */
@Getter
@Setter
public final class StopToken extends Token {

    TokenStream returnStream;

    public StopToken(String value) {
        super("keyword", value);
        if (!value.matches("break|continue|return.*")) {
            throw new EvaluateException("StopToken's value must be break or continue or return sth");
        }
    }

    @Override
    protected Object doCalculate(Class<?> desiredType) {
        throw new StopSignal(value).setReturnStream(returnStream);
    }
}
