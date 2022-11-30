package com.freedy.expression.standard;

import com.freedy.expression.core.EvaluationContext;
import com.freedy.expression.core.Expression;
import com.freedy.expression.core.TokenStream;
import com.freedy.expression.exception.IllegalArgumentException;
import com.freedy.expression.exception.StopSignal;
import com.freedy.expression.function.Suppler;
import com.freedy.expression.function.VarFunction;
import com.freedy.expression.utils.PlaceholderParser;
import com.freedy.expression.utils.ReflectionUtils;
import lombok.Data;
import lombok.Setter;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;

/**
 * @author Freedy
 * @date 2022/1/4 17:36
 */
@Data
public class Func implements VarFunction._1ParameterFunction<Object, Object> {

    private EvaluationContext superContext;
    private Suppler<EvaluationContext> ctxProvider;
    private String funcName;
    private String[] argName;
    private TokenStream funcBody;
    private LambdaAdapter adapter;

    public Func(EvaluationContext superContext) {
        this.superContext = superContext;
    }

    public Func(Suppler<EvaluationContext> ctxProvider) {
        this.ctxProvider = ctxProvider;
    }

    public LambdaAdapter initAdapter() {
        return adapter = new LambdaAdapter();
    }

    @Override
    public Object apply(Object... obj) throws Exception {
        if (obj != null && obj.length != argName.length) {
            throw new IllegalArgumentException("wrong number of arguments,reference ?(?*)", funcName, argName);
        }
        if (superContext == null) {
            if (ctxProvider != null) superContext = ctxProvider.supply();
            if (superContext == null) throw new IllegalArgumentException("please set a EvaluateContext!");
        }
        FuncEvalCtx funcCtx = new FuncEvalCtx(superContext);
        if (obj != null) for (int i = 0; i < argName.length; i++) funcCtx.putVar(argName[i], obj[i]);

        Expression expression = new Expression(funcBody, funcCtx);
        try {
            return expression.getValue();
        } catch (Throwable e) {
            StopSignal signal = StopSignal.getInnerSignal(e);
            if (signal == null) throw e;
            if (signal.getSignal().contains("return")) {
                TokenStream subStream = signal.getReturnStream();
                if (subStream != null) {
                    expression.setTokenStream(subStream);
                    return expression.getValue();
                }
            }
            return null;
        }
    }

    public EvaluationContext getSuperContext() {
        try {
            return superContext == null ? ctxProvider.supply() : superContext;
        } catch (Exception e) {
            return null;
        }
    }

    public Func getFunc() {
        return this;
    }

    @Override
    public String toString() {
        return new PlaceholderParser("[FUNCTION:?(?*)]", funcName, argName).ifEmptyFillWith("").toString();
    }


    public class LambdaAdapter {

        private Class<?> interfaceType;
        @Setter
        private String interfaceName;


        public Class<?> getInterfaceType() throws ClassNotFoundException {
            if (interfaceType != null) return interfaceType;
            if (interfaceName == null) return null;
            interfaceType = getSuperContext().findClass(interfaceName);
            if (notFunctional(interfaceType))
                throw new IllegalArgumentException("? is not a matching lambda function,please refer to interface ?", getFunc(), interfaceType.getName());
            return interfaceType;
        }

        public Object getInstance(Class<?> lambdaType) throws Exception {
            if (interfaceType != null) {
                if (lambdaType != interfaceType)
                    throw new IllegalArgumentException("wrong interface type ?,should be ?", lambdaType, interfaceType);
            } else if (notFunctional(lambdaType))
                throw new IllegalArgumentException("? is not lambda interface", interfaceType.getName());
            EvaluationContext ctx = getSuperContext();
            //noinspection resource
            Class<?> generatedClass = new ByteBuddy()
                    .subclass(Object.class)
                    .defineField("func", Func.class, Visibility.PRIVATE)
                    .implement(lambdaType)
                    .intercept(MethodDelegation.to(LambdaAdapter.class))
                    .make()
                    .load(new ClassLoader() {
                        @Override
                        public Class<?> loadClass(String name) throws ClassNotFoundException {
                            return ctx instanceof StandardEvaluationContext std ? std.findClass(name) : Thread.currentThread().getContextClassLoader().loadClass(name);
                        }
                    })
                    .getLoaded();
            Object proxy = generatedClass.getConstructor().newInstance();
            ReflectionUtils.setter(proxy, "func", getFunc());
            return proxy;
        }

        @RuntimeType
        @SuppressWarnings("unused")
        public static Object lambda(@AllArguments Object[] args, @This Object _this) throws Exception {
            Func func = (Func) ReflectionUtils.getter(_this, "func");
            if (func.getArgName().length == 0) {
                int length = args.length;
                String[] argName = new String[length];
                for (int i = 0; i < length; i++) {
                    argName[i] = "a" + (i + 1);
                }
                func.setArgName(argName);
            }
            return func.apply(args);
        }

        public boolean notFunctional(Class<?> lambdaType) {
            if (lambdaType == null) return true;
            if (lambdaType.getAnnotation(FunctionalInterface.class) != null) return false;
            if (!lambdaType.isInterface()) return true;
            Method delegate = null;
            for (Method method : lambdaType.getDeclaredMethods()) {
                if (method.isDefault() || method.isSynthetic()) continue;
                if (delegate != null) return true;
                delegate = method;
            }
            if (delegate == null) return true;
            return delegate.getParameterCount() != getArgName().length;
        }


    }
}
