package com.freedy.expression;

import com.freedy.expression.function.Functional;

import java.lang.ref.Cleaner;
import java.util.List;

/**
 * @author Freedy
 * @date 2021/12/14 11:11
 */

@SuppressWarnings("UnusedReturnValue")
public interface EvaluationContext {
    Cleaner cleaner = Cleaner.create();
    Object setVariable(String name, Object variable);

    Object getVariable(String name);

    boolean containsVariable(String name);

    Object removeVariable(String name);

    void clearVariable();

    Object setRoot(Object root);

    Object getRoot();

    Functional registerFunction(String name, Functional function);

    Functional getFunction(String name);

    boolean containsFunction(String funcName);

    Functional removeFunction(String name);

    void clearFunction();

    default String filterName(String name){
        if (name.matches("^[@#].*")){
            name=name.substring(1);
        }
        return name;
    }

   default void registerClean(TokenStream stream, List<String> defTokenList) {
       cleaner.register(stream,()->defTokenList.forEach(this::removeVariable));
   }
}
