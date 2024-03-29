package com.freedy.expression.exception;

import com.freedy.expression.core.TokenStream;
import com.freedy.expression.core.token.ExecutableToken;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Freedy
 * @date 2021/12/16 9:39
 */
public class EvaluateException extends FunBaseException {

    @Getter
    private final List<ExecutableToken> tokenList=new ArrayList<>();
    @Getter
    private String expression;
    @Getter
    private String[] errPart;

    public EvaluateException(String msg) {
        super(msg);
    }

    public EvaluateException(String msg, Object... placeholder) {
        super(msg, placeholder);
    }

    public EvaluateException subExpression(String expression){
        this.expression=expression;
        return this;
    }

    public EvaluateException errToken(ExecutableToken...syntaxErrSubStr) {
        tokenList.addAll(Arrays.asList(syntaxErrSubStr));
        return this;
    }

    public EvaluateException errorPart(String ...part){
        this.errPart=part;
        return this;
    }

    public EvaluateException tokenStream(TokenStream stream){
        this.expression=stream.getExpression();
        tokenList.addAll(stream.getAllTokens());
        return this;
    }
}
