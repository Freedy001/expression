package com.freedy.expression;

import com.freedy.expression.tokenBuilder.Tokenizer;

/**
 * @author Freedy
 * @date 2021/12/14 10:26
 */
public class ExpressionPasser {

    public Expression parseExpression(String expression) {
        return new Expression(Tokenizer.getTokenStream(expression));
    }

}
