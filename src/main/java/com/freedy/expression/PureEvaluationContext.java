package com.freedy.expression;

import com.freedy.expression.function.Functional;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Freedy
 * @date 2022/1/8 0:33
 */
@Getter
@Setter
public class PureEvaluationContext implements EvaluationContext{
    public final Map<String, Object> variableMap = new HashMap<>();
    public final Map<String, Functional> funMap = new HashMap<>();

    @Override
    public Object setVariable(String name, Object variable) {
        return variableMap.put(filterName(name),variable);
    }

    @Override
    public Object getVariable(String name) {
        return variableMap.get(filterName(name));
    }

    @Override
    public boolean containsVariable(String name) {
        return variableMap.containsKey(filterName(name));
    }

    @Override
    public Object removeVariable(String name) {
        return variableMap.remove(filterName(name));
    }

    @Override
    public void clearVariable() {
        variableMap.clear();
    }

    @Override
    public Object setRoot(Object root) {
        return null;
    }

    @Override
    public Object getRoot() {
        return null;
    }

    @Override
    public Functional registerFunction(String name, Functional function) {
        return funMap.put(name,function);
    }

    @Override
    public Functional getFunction(String name) {
        return funMap.get(name);
    }

    @Override
    public Set<String> getFunctionNameSet() {
        return funMap.keySet();
    }

    @Override
    public boolean containsFunction(String funcName) {
        return funMap.containsKey(funcName);
    }

    @Override
    public Functional removeFunction(String name) {
        return funMap.remove(name);
    }

    @Override
    public void clearFunction() {
        funMap.clear();
    }
}
