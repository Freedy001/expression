package com.freedy.expression.token;

import com.freedy.expression.core.Expression;
import com.freedy.expression.core.TokenStream;
import lombok.Setter;

/**
 * @author Freedy
 * @date 2021/12/20 21:38
 */

public final class StreamWrapperToken extends Token{

    @Setter
    private TokenStream wrappedStream;

    public StreamWrapperToken(TokenStream wrappedStream) {
        super("wrapper", wrappedStream.getExpression());
        this.wrappedStream=wrappedStream;
    }


    @Override
    protected Object doCalculate(Class<?> desiredType) {
        if (wrappedStream==null) throw new UnsupportedOperationException("please set a wrappedStream");
        return checkAndSelfOps(new Expression(wrappedStream).getValue(context,desiredType));
    }
}