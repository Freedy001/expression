package com.freedy.expression.stander;

import com.freedy.expression.utils.ReflectionUtils;

/**
 * @author Freedy
 * @date 2022/1/10 20:11
 */
public class ExpressionClassEvaluateContext extends StanderEvaluationContext {

    Object expressionObj;

    public ExpressionClassEvaluateContext(Object expressionObj) {
        this.expressionObj = expressionObj;
    }


    @Override
    public Object setVariable(String name, Object variable) {
        ReflectionUtils.setter(expressionObj, filterName(name), variable);
        return super.setVariable(name, variable);
    }
}