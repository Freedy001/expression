package com.freedy.expression.tokenBuilder;

import com.freedy.expression.core.TokenStream;

/**
 * @author Freedy
 * @date 2022/10/19 1:14
 */
public class LambdaBuilder extends Builder{

    @Override
    public boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        return false;
    }

    @Override
    public int priority() {
        return 10;
    }
}
