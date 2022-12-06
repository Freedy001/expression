package com.freedy.expression.token;

import com.freedy.expression.exception.EvaluateException;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

/**
 * @author Freedy
 * @date 2021/12/20 17:33
 */
@Getter
@Setter
public final class DotSplitToken extends ClassToken {

    private Token baseToken;

    public DotSplitToken(String token) {
        super("dotSplit", token);
    }

    @Override
    protected Object doCalculate(Class<?> desiredType) {
        Object baseObject = baseToken.calculateResult(ANY_TYPE);
        if (baseObject == null) {
            if (Optional.ofNullable(getRelevantOps()).orElse("").equals("?")){
                return null;
            }
            throw new EvaluateException("? calculate a null value",baseToken);
        }
        return checkAndSelfOps(executeChain(baseObject.getClass(), baseObject, executableCount));
    }

    @Override
    public void assignFrom(Token assignment) {
        throw new UnsupportedOperationException("DotSplitToken can not be assigned");
    }


}