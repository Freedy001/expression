package com.freedy.expression.stander.standerFunc;

import com.freedy.expression.JavaAdapter;
import com.freedy.expression.core.EvaluationContext;
import com.freedy.expression.core.TokenStream;
import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.exception.IllegalArgumentException;
import com.freedy.expression.stander.ExpressionFunc;
import com.freedy.expression.stander.Func;
import com.freedy.expression.stander.LambdaAdapter;
import com.freedy.expression.utils.PlaceholderParser;
import com.freedy.expression.utils.ReflectionUtils;
import com.freedy.expression.utils.StringUtils;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.freedy.expression.utils.ReflectionUtils.convertToWrapper;
import static com.freedy.expression.utils.ReflectionUtils.tryConvert;

/**
 * @author Freedy
 * @date 2022/3/6 14:55
 */
public class StanderAdapter extends AbstractStanderFunc {


    @SneakyThrows
    @ExpressionFunc(value = "new a instance,param 1 is full class name,the rest param is constructor's param")
    public Object _new(String className, Object... args) {
        return ReflectionUtils.invokeMethod("<init>", context.findClass(className), null, args);
//        Class<?> aClass = context.findClass(className);
//        List<Constructor<?>> constructorList = new ArrayList<>();
//        List<Constructor<?>> seminary = new ArrayList<>();
//        int length = args.length;
//        for (Constructor<?> cst : aClass.getConstructors()) {
//            if (cst.getParameterCount() == length) {
//                constructorList.add(cst);
//            }
//            seminary.add(cst);
//        }
//        args = Arrays.copyOf(args, args.length, Object[].class);
//        for (Constructor<?> constructor : constructorList) {
//            Class<?>[] types = constructor.getParameterTypes();
//            int i = 0;
//            for (; i < length; i++) {
//                Class<?> originMethodArgs = convertToWrapper(types[i]);
//                Class<?> supplyMethodArgs = convertToWrapper(args[i] == null ? types[i] : args[i].getClass());
//                if (!originMethodArgs.isAssignableFrom(supplyMethodArgs)) {
//                    Object o = tryConvert(originMethodArgs, args[i]);
//                    if (o != Boolean.FALSE) {
//                        args[i] = o;
//                    } else {
//                        break;
//                    }
//                }
//            }
//            if (i == length) {
//                constructor.setAccessible(true);
//                return constructor.newInstance(args);
//            }
//        }
//        StringJoiner argStr = new StringJoiner(",", "(", ")");
//        for (Object arg : args) {
//            argStr.add(arg.getClass().getName());
//        }
//        throw new NoSuchMethodException("no constructor" + argStr + "!you can call these constructors:" + new PlaceholderParser("?*", seminary.stream().map(method -> {
//            StringJoiner argString = new StringJoiner(",", "(", ")");
//            for (Parameter arg : method.getParameters()) {
//                argString.add(arg.getType().getSimpleName() + " " + arg.getName());
//            }
//            return method.getName() + argString;
//        }).toList()).serialParamsSplit(" , ").ifEmptyFillWith("not find matched method"));
    }


    @SneakyThrows
    @ExpressionFunc("\n"+"it will generate a object which implement the interface you delivered.\n"+"params are like:\n"+"(interface-name,\n"+"func1-name,func1-param1,func1-param2,func1-body,\n"+"func2-name,func2-param1,func2-body,\n"+"func3-name,func3-body,\n"+"...................................)\n"+"form.\n"+"example:\n"+"you hava a java interface like this\n"+"public interface com.freedy.expression.com.freedy.expression.Test{\n"+"void test1(int o1,int o2);\n"+"void test1(Object o1);\n"+"}\n"+"then code below:\n"+"def a=newInterface('package.com.freedy.expression.com.freedy.expression.Test',\n"+"'test1','o1','o2',@block{\n"+"//your code\n"+"print('i am test1'+o1+o2);\n"+"},'test2','o2',@block{\n"+"//your code\n"+"print(o2);\n"+"});\n"+"a.test1('1','2');\n"+"a.test2('ni hao');\n"+"\n"+"it will print:\n"+"i am test112\n"+"ni hao\n"+"")
    public Object newInterface(String name, Object... funcPara) {
        Class<?> clazz = context.findClass(name);
        if (!clazz.isInterface()) {
            throw new IllegalArgumentException("? is not interface", name);
        }
        int len = funcPara.length;
        List<Object[]> rowFuncList = new ArrayList<>();
        int lastSplit = 0;
        for (int i = 0; i < len; i++) {
            if (funcPara[i] instanceof TokenStream) {
                rowFuncList.add(Arrays.copyOfRange(funcPara, lastSplit, lastSplit = i + 1));
            }
        }
        Method[] declaredMethods = clazz.getDeclaredMethods();
        int methodCount = declaredMethods.length;
        int rawMethodCount = rowFuncList.size();
        if (methodCount != rawMethodCount) {
            throw new IllegalArgumentException("the method count[?] which you declare is not match the interface[?]'s method count[?]", rawMethodCount, name, methodCount);
        }
        Map<String, Func> nameFuncMapping = rowFuncList.stream().map(this::getFunc).collect(Collectors.toMap(Func::getFuncName, o -> o));
        for (Method method : declaredMethods) {
            Func func = nameFuncMapping.get(method.getName());
            if (func == null) {
                throw new IllegalArgumentException("you don't declare the method ?", method.getName());
            } else {
                if (method.getParameterCount() != func.getArgName().length) {
                    throw new IllegalArgumentException("your method[?] parameter count[?] is not match to the interface's[?]", method.getName(), func.getArgName().length, method.getParameterCount());
                }
            }
        }
        return Proxy.newProxyInstance(EvaluationContext.class.getClassLoader(), new Class[]{clazz}, (proxy, method, args) -> nameFuncMapping.get(method.getName()).apply(args));
    }

    @ExpressionFunc("\n"+"simple use for newInterface() function,you just need interface full class name param and func-body.\n"+"replacedStr: interface must have only one non-default-method;\n"+"example:\n"+"def list=[43,15,76,2,6];\n"+"list.sort(lambda('o1','o2',@block{return {o1-o2};}));\n"+"print(list);\n"+"\n"+"it will print:\n"+"[2,6,15,43,76]\n"+"")
    public Object lambda(Object... par) {
        Object[] newParam = new Object[par.length + 1];
        System.arraycopy(par, 0, newParam, 1, par.length);
        newParam[0] = "lambda";
        return new LambdaAdapter(getFunc(newParam));
    }

    @ExpressionFunc("\n"+"use for def a function;\n"+"the fist param is function name;\n"+"the rest is function param and body;\n"+"replacedStr:the function body use @block surround.\n"+"example:\n"+"func('add','a','b',@block{return {a+b};});\n"+"print(add(54+46));\n"+"\n"+"it will print:100\n"+"")
    public void func(Object... funcPar) {
        Func func = getFunc(funcPar);
        if (context.containsFunction(func.getFuncName())) {
            throw new EvaluateException("same method name ?!", func.getFuncName());
        }
        context.registerFunction(func.getFuncName(), func);
    }

    @SneakyThrows
    @ExpressionFunc(value = "get class .If the param is a string, it is taken as the full class name otherwise as Object")
    public Class<?> _class(Object arg) {
        return getClassByArg(arg);
    }

    @ExpressionFunc(value = "create a java array")
    public Object[] arr(Object... arr) {
        return arr;
    }

    @ExpressionFunc(value = "run cmd,you can specify the charset by defining _cmdCharset variable")
    public String cmd(String cmd) throws Exception {
        Process exec = Runtime.getRuntime().exec(cmd);
        InputStream es = exec.getErrorStream();
        InputStream is = exec.getInputStream();
        String cmdCharset = Optional.ofNullable(context.getVariable("_cmdCharset")).orElse("").toString();
        StringBuffer buffer = new StringBuffer();
        new Thread(() -> {
            if (es != null) {
                try {
                    String s = new String(JavaAdapter.readAllBytes(es), StringUtils.hasText(cmdCharset) ? cmdCharset : "UTF-8");
                    if (StringUtils.hasText(s))
                        buffer.append(s);
                } catch (Exception ignored) {
                }
            }
        }).start();
        new Thread(() -> {
            if (is != null) {
                try {
                    String s = new String(JavaAdapter.readAllBytes(is), StringUtils.hasText(cmdCharset) ? cmdCharset : "UTF-8");
                    if (StringUtils.hasText(s))
                        buffer.append(s);
                } catch (Exception ignored) {
                }
            }
        }).start();
        if (!exec.waitFor(3, TimeUnit.SECONDS)) {
            exec.destroy();
            throw new java.lang.IllegalArgumentException("Execution timeout(3S)");
        }
        return buffer.toString();
    }

    @NonNull
    private Func getFunc(Object[] funcPar) {
        int length = funcPar.length;
        if (length <= 1) throw new IllegalArgumentException("func() must specify function body");
        if (!(funcPar[0] instanceof String)) throw new IllegalArgumentException("func()'s fist arg must be string");
        Func func = new Func(context);
        func.setFuncName((String) funcPar[0]);
        func.setArgName(Arrays.stream(Arrays.copyOfRange(funcPar, 1, length - 1)).map(arg -> {
            if (arg instanceof String ) {
String str = (String) arg;
                return str;
            } else {
                throw new IllegalArgumentException("func()'s fist arg must be string");
            }
        }).toArray(String[]::new));
        func.setFuncBody((TokenStream) funcPar[length - 1]);
        return func;
    }


}
