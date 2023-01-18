package com.freedy.expression.entrance;

import com.freedy.expression.core.EvaluationContext;
import com.freedy.expression.core.Expression;
import com.freedy.expression.core.function.Functional;
import com.freedy.expression.standard.StandardEvaluationContext;
import lombok.Getter;
import lombok.Setter;

public class Expr {
    @Setter
    @Getter
    private static EvaluationContext e = new StandardEvaluationContext();
    private static final Expression exp = new Expression();

    public static void setVar(String varName, Object obj) {
        e.setVariable(varName, obj);
    }

    public static void rmVar(String varName) {
        e.removeVariable(varName);
    }

    public static Object getRoot() {
        return e.getRoot();
    }

    public static void setRoot(Object root) {
        e.setRoot(root);
    }

    public static void addFunc(String name, Functional function) {
        e.registerFunction(name, function);
    }

    public static void rmFunc(String name) {
        e.removeFunction(name);
    }

    public static Object eval(String expr) {
        return eval(expr, Object.class);
    }

    public static <T> T eval(String expr, Class<T> desiredResultType) {
        return exp.getValue(expr, e, desiredResultType);
    }


}
