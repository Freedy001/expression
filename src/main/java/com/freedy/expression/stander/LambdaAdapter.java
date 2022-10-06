package com.freedy.expression.stander;

import com.freedy.expression.core.EvaluationContext;
import com.freedy.expression.exception.IllegalArgumentException;
import com.freedy.expression.utils.ReflectionUtils;
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
 * @date 2021/12/31 9:26
 */
public class LambdaAdapter {

    private final Func func;
    private Class<?> interfaceType;
    @Setter
    private String interfaceName;

    public LambdaAdapter(Func func) {
        this.func = func;
    }


    public Class<?> getInterfaceType() throws ClassNotFoundException {
        if (interfaceType != null) return interfaceType;
        if (interfaceName == null) return null;
        interfaceType = func.getSuperContext().findClass(interfaceName);
        if (notFunctional(interfaceType))
            throw new IllegalArgumentException("? is not a matching lambda function,please refer to interface ?", func, interfaceType.getName());
        return interfaceType;
    }

    public Object getInstance(Class<?> lambdaType) throws Exception {
        if (interfaceType != null) {
            if (lambdaType != interfaceType)
                throw new IllegalArgumentException("wrong interface type ?,should be ?", lambdaType, interfaceType);
        } else if (notFunctional(lambdaType))
            throw new IllegalArgumentException("? is not lambda interface", interfaceType.getName());
        EvaluationContext ctx = func.getSuperContext();
        //noinspection resource
        Class<?> generatedClass = new ByteBuddy()
                .subclass(Object.class)
                .defineField("func", Func.class, Visibility.PRIVATE)
                .implement(lambdaType)
                .intercept(MethodDelegation.to(LambdaAdapter.class))
                .make()
                .load(ctx instanceof StanderEvaluationContext s ? s.getLoader() : Thread.currentThread().getContextClassLoader())
                .getLoaded();
        Object proxy = generatedClass.getConstructor().newInstance();
        ReflectionUtils.setter(proxy, "func", func);
        return proxy;
//        return Proxy.newProxyInstance(lambdaType.getClassLoader(), new Class[]{lambdaType}, (o, m, a) -> {
//            if (m.isDefault() || m.getName().matches("toString|equals|hashCode|wait|getClass|clone|notifyAll|finalize")) {
//                return MethodHandles.lookup().in(m.getDeclaringClass()).unreflectSpecial(m, m.getDeclaringClass()).bindTo(o).invokeWithArguments(a);
//            } else {
//                return func.apply(a);
//            }
//        });
    }

    @RuntimeType
    public static Object lambda(@AllArguments Object[] args, @This Object _this) throws Exception {
        return ((Func) ReflectionUtils.getter(_this, "func")).apply(args);
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
        return delegate.getParameterCount() != func.getArgName().length;
    }


}
