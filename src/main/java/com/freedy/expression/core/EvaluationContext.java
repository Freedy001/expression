package com.freedy.expression.core;

import com.freedy.expression.function.Functional;

import java.lang.ref.Cleaner;
import java.util.List;
import java.util.Set;

/**
 * <h2>表达式上下文</h2>
 * 表达式的执行必须配合上下文对象。它用于存放表达式执行过程中产生的变量或者方法。<br/>
 * 其本质是两个map和一个root对象。两个map分别代表变量与方法。root对象可以理解为key值固定的map也代表表达式中的变量。
 * 引入root对象可使操作java对象更为便捷<br/>
 *  <br/>
 * 无root对象的相关实现可以使用纯净上下文({@link PureEvaluationContext})。具体使用如下<br/>
 * <pre>{@code
 *           PureEvaluationContext ctx= new PureEvaluationContext();
 *           Expression exp = new Expression("you fun expression is here");
 *           exp.getValue(ctx);
 *      }</pre>
 * 表达式中变量值的获取与定义分是通过{@link EvaluationContext#getVariable(String)}和{@link EvaluationContext#setVariable(String, Object)}实现。
 * root对象的获取与修改可以通过{@link EvaluationContext#getRoot()}和{@link EvaluationContext#setRoot(Object)}实现。
 * <h2>标准上下文({@link com.freedy.expression.stander.StanderEvaluationContext})</h2>
 * 标准上下文相比与纯净上下文相比内嵌了很多标准方法与标准root对象。标准方法可以使用root对象的help域来查看。标准方法的详细信息可以使用help方法来查看。
 * 并且它重新定义{@link EvaluationContext#findClass(String)}方法,配合提供的import标准方法可以简化寻找class类的类名的书写。
 * 用于与PureEvaluationContext一致。
 * @author Freedy
 * @date 2021/12/14 11:11
 */
@SuppressWarnings("UnusedReturnValue")
public interface EvaluationContext {
    Cleaner cleaner = Cleaner.create();

    Object setVariable(String name, Object variable);

    Object getVariable(String name);

    boolean containsVariable(String name);

    Set<String> allVariables();

    Object removeVariable(String name);

    void clearVariable();

    Object setRoot(Object root);

    Object getRoot();

    Functional registerFunction(String name, Functional function);

    Functional getFunction(String name);

    Set<String> getFunctionNameSet();

    boolean containsFunction(String funcName);

    Functional removeFunction(String name);

    void clearFunction();


    default Class<?> findClass(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }

    default String filterName(String name){
        if (name.matches("^[@#%].*")){
            name=name.substring(1);
        }
        return name;
    }

   default void registerClean(TokenStream stream, List<String> defTokenList) {
       cleaner.register(stream,()->defTokenList.forEach(this::removeVariable));
   }
}
