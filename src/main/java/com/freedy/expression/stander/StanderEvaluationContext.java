package com.freedy.expression.stander;

import com.freedy.expression.EvaluationContext;
import com.freedy.expression.TokenStream;
import com.freedy.expression.exception.IllegalArgumentException;
import com.freedy.expression.function.*;
import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.utils.PlaceholderParser;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;


import java.lang.ref.Cleaner;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.freedy.expression.utils.ReflectionUtils.convertToWrapper;
import static com.freedy.expression.utils.ReflectionUtils.tryConvert;

/**
 * @author Freedy
 * @date 2021/12/15 16:22
 */
@Data
@NoArgsConstructor
public class StanderEvaluationContext implements EvaluationContext {
    private final String id = DateTimeFormatter.ofPattern("hh:mm:ss-SSS").format(LocalDateTime.now());
    private final Map<String, Functional> funMap = new HashMap<>();
    private final static Cleaner cleaner = Cleaner.create();

    {
        registerFunction("print", (Consumer._1ParameterConsumer<Object>) System.out::println);

        registerFunction("printInline", (Consumer._1ParameterConsumer<Object>) System.out::print);

        registerFunction("range", (Function._2ParameterFunction<Integer, Integer, List<Integer>>) (a, b) -> {
            ArrayList<Integer> list = new ArrayList<>();
            for (int i = a; i <= b; i++) {
                list.add(i);
            }
            return list;
        });

        registerFunction("stepRange", (Function._3ParameterFunction<Integer, Integer, Integer, List<Integer>>) (a, b, step) -> {
            ArrayList<Integer> list = new ArrayList<>();
            for (int i = a; i <= b; i += step) {
                list.add(i);
            }
            return list;
        });

        registerFunction("condition", (Function._1ParameterFunction<TokenStream, TokenStream>) t -> t);

        registerFunction("new", (VarFunction._2ParameterFunction<String, Object, Object>) (className, args) -> {
            Class<?> aClass = Class.forName(className);
            List<Constructor<?>> constructorList = new ArrayList<>();
            List<Constructor<?>> seminary = new ArrayList<>();
            int length = args.length;
            for (Constructor<?> cst : aClass.getConstructors()) {
                if (cst.getParameterCount() == length) {
                    constructorList.add(cst);
                }
                seminary.add(cst);
            }
            for (Constructor<?> constructor : constructorList) {
                Class<?>[] types = constructor.getParameterTypes();
                int i = 0;
                for (; i < length; i++) {
                    Class<?> originMethodArgs = convertToWrapper(types[i]);
                    Class<?> supplyMethodArgs = convertToWrapper(args[i] == null ? types[i] : args[i].getClass());
                    if (!originMethodArgs.isAssignableFrom(supplyMethodArgs)) {
                        Object o = tryConvert(originMethodArgs, args[i]);
                        if (o != Boolean.FALSE) {
                            args[i] = o;
                        } else {
                            break;
                        }
                    }
                }
                if (i == length) {
                    constructor.setAccessible(true);
                    return constructor.newInstance(args);
                }
            }
            StringJoiner argStr = new StringJoiner(",", "(", ")");
            for (Object arg : args) {
                argStr.add(arg.getClass().getName());
            }
            throw new NoSuchMethodException("no constructor" + argStr + "!you can call these constructors:" + new PlaceholderParser("?*", seminary.stream().map(method -> {
                StringJoiner argString = new StringJoiner(",", "(", ")");
                for (Parameter arg : method.getParameters()) {
                    argString.add(arg.getType().getSimpleName() + " " + arg.getName());
                }
                return method.getName() + argString;
            }).toList()).serialParamsSplit(" , ").ifEmptyFillWith("not find matched method"));
        });

        registerFunction("newInterface", (VarFunction._2ParameterFunction<String, Object, Object>) (name, funcPara) -> {
            Class<?> clazz = Class.forName(name);
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
        });

        registerFunction("lambda", (VarFunction._1ParameterFunction<Object, LambdaAdapter>) par -> {
            Object[] newParam = new Object[par.length + 1];
            System.arraycopy(par, 0, newParam, 1, par.length);
            newParam[0] = "lambda";
            return new LambdaAdapter(getFunc(newParam));
        });

        registerFunction("func", (VarConsumer._1ParameterConsumer<Object>) funcPar -> {
            Func func = getFunc(funcPar);
            if (containsFunction(func.getFuncName())) {
                throw new EvaluateException("same method name ?!", func.getFuncName());
            }
            registerFunction(func.getFuncName(), func);
        });

        registerFunction("class", (Function._1ParameterFunction<Object, Class<?>>) arg -> {
            if (arg instanceof String s) {
                return Class.forName(s);
            } else {
                if (arg == null) return null;
                return arg.getClass();
            }
        });

        registerFunction("int", (Function._1ParameterFunction<Object, Integer>) o -> o==null?null: new BigDecimal(o.toString()).setScale(0, RoundingMode.DOWN).intValue());
    }

    @NonNull
    private Func getFunc(Object[] funcPar) {
        int length = funcPar.length;
        if (length <= 1) throw new IllegalArgumentException("func() must specify function body");
        if (!(funcPar[0] instanceof String)) throw new IllegalArgumentException("func()'s fist arg must be string");
        Func func = new Func(this);
        func.setFuncName((String) funcPar[0]);
        func.setArgName(Arrays.stream(Arrays.copyOfRange(funcPar, 1, length - 1)).map(arg -> {
            if (arg instanceof String str) {
                return str;
            } else {
                throw new IllegalArgumentException("func()'s fist arg must be string");
            }
        }).toArray(String[]::new));
        func.setFuncBody((TokenStream) funcPar[length - 1]);
        return func;
    }

    private Object root;
    private Map<String, Object> variableMap = new HashMap<>();
    private EvaluationContext superContext;

    public StanderEvaluationContext(Object root) {
        this.root = root;
    }

    public StanderEvaluationContext(EvaluationContext superContext) {
        this.root = superContext.getRoot();
        this.superContext = superContext;
    }


    public Object setRoot(Object root) {
        this.root = root;
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

    @Override
    public Object setVariable(String name, Object variable) {
        name = filterName(name);
        if (name.equals("root")) {
            root = variable;
        }
        return variableMap.put(name, variable);
    }

    @Override
    public Object getVariable(String name) {
        try {
            name = filterName(name);
            if (name.equals("root")) {
                return root;
            }
            return variableMap.get(name);
        } catch (Exception e) {
            throw new IllegalArgumentException("get variable ? failed,because ?", name, e);
        }
    }

    @Override
    public boolean containsVariable(String name) {
        name = filterName(name);
        return variableMap.containsKey(name);
    }

    @Override
    public Object removeVariable(String name) {
        return variableMap.remove(name);
    }

    @Override
    public void clearVariable() {
        variableMap.clear();
    }
}
