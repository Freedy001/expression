package com.freedy.expression.core;

import com.freedy.expression.core.EvaluationContext;
import com.freedy.expression.exception.IllegalArgumentException;
import com.freedy.expression.function.Functional;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
public class PureEvaluationContext implements EvaluationContext {
    protected final Map<String, Object> variableMap = new HashMap<>();
    protected final Map<String, Functional> funMap = new HashMap<>();
    protected Object root;

    public PureEvaluationContext(Object root) {
        this.root = root;
    }

    @Override
    public Object setVariable(String name, Object variable) {
        name = filterName(name);
        if (name.equals("root")) {
            setRoot(variable);
        }
        return variableMap.put(name, variable);
    }

    @Override
    public Object getVariable(String name) {
        name = filterName(name);
        if (name.equals("root")) {
            return getRoot();
        }
        return variableMap.get(name);
    }

    @Override
    public boolean containsVariable(String name) {
        if (name.equals("root")) return getRoot() != null;
        return variableMap.containsKey(filterName(name));
    }

    @Override
    public Set<String> allVariables() {
        return variableMap.keySet();
    }

    @Override
    public Object removeVariable(String name) {
        String filterName = filterName(name);
        if (filterName.equals("root")) {
            Object root = getRoot();
            setRoot(null);
            return root;
        }
        return variableMap.remove(filterName);
    }

    @Override
    public void clearVariable() {
        variableMap.clear();
    }

    @Override
    public Object setRoot(Object root) {
        Object o = this.root;
        this.root = root;
        return o;
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public Functional registerFunction(String name, Functional function) {
        return funMap.put(name, function);
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
