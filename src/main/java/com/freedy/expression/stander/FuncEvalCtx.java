package com.freedy.expression.stander;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeFilterable;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.freedy.expression.core.EvaluationContext;
import com.freedy.expression.core.PureEvaluationContext;
import com.freedy.expression.function.Functional;

import java.util.Set;

/**
 * @author Freedy
 * @date 2022/7/1 13:07
 */
public class FuncEvalCtx extends PureEvaluationContext {

    EvaluationContext superContext;

    public FuncEvalCtx(EvaluationContext superContext) {
        super.setVariable("ctx", this);
        this.superContext = superContext;
        root = superContext.getRoot();
    }

    void putVar(String name, Object value) {
        super.setVariable(name, value);
    }

    @Override
    public Object getVariable(String name) {
        //@ 开头直接去父容器拿值
        if (name.startsWith("@")) {
            return superContext.getVariable(name);
        }
        //# 开头先去子容器拿值如果没有就去父容器拿值
        if (super.containsVariable(name)) {
            return super.getVariable(name);
        }
        return superContext.getVariable(name);
    }

    @Override
    public Object setVariable(String name, Object variable) {
        if (name.startsWith("@")) {
            return superContext.setVariable(name, variable);
        }
        if (super.containsVariable(name)) {
            return super.setVariable(name, variable);
        }
        if (superContext.containsVariable(name)) {
            return superContext.setVariable(name, variable);
        }
        return super.setVariable(name, variable);
    }


    @Override
    public boolean containsVariable(String name) {
        String className = Thread.currentThread().getStackTrace()[2].getClassName();
        if (className.equals("com.freedy.expression.token.LoopToken") || className.equals("com.freedy.expression.token.DefToken")) {
            return super.containsVariable(name);
        }
        if (name.startsWith("@")) {
            return superContext.containsVariable(name);
        }
        return super.containsVariable(name)||superContext.containsVariable(name);
    }

    @Override
    public Class<?> findClass(String className) throws ClassNotFoundException {
        return superContext.findClass(className);
    }

    @Override
    public Functional registerFunction(String name, Functional function) {
        return superContext.registerFunction(name, function);
    }

    @Override
    public Functional getFunction(String name) {
        return superContext.getFunction(name);
    }

    @Override
    public Set<String> getFunctionNameSet() {
        return superContext.getFunctionNameSet();
    }

    @Override
    public boolean containsFunction(String funcName) {
        return superContext.containsFunction(funcName);
    }

    @Override
    public Functional removeFunction(String name) {
        return superContext.removeFunction(name);
    }

    @Override
    public void clearFunction() {
        superContext.clearFunction();
    }


}
