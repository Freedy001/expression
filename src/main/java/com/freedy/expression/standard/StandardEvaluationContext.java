package com.freedy.expression.standard;

import com.freedy.expression.core.PureEvaluationContext;
import com.freedy.expression.core.TokenStream;
import com.freedy.expression.core.function.Function;
import com.freedy.expression.core.function.Functional;
import com.freedy.expression.exception.CombineException;
import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.exception.IllegalArgumentException;
import com.freedy.expression.standard.standardFunc.AbstractStandardFunc;
import com.freedy.expression.utils.PackageScanner;
import com.freedy.expression.utils.ReflectionUtils;
import com.freedy.expression.utils.StringUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Freedy
 * @date 2021/12/15 16:22
 */
@Getter
public class StandardEvaluationContext extends PureEvaluationContext {
    /**
     * 标准方法帮助信息
     */
    private final Map<String, String> selfFuncHelp = new HashMap<>();
    /**
     * import信息可以简化表达式中全类名的书写
     */
    private final Map<String, String> importMap = new ConcurrentHashMap<>();
    /**
     * 当前路径
     */
    @Setter
    private String currentPath = ".";

    public StandardEvaluationContext(String... packages) {
        HashSet<String> set = new HashSet<>(Arrays.asList(packages));
        set.add("com.freedy.expression.standard.standardFunc");
        initDefaultBehaves();
        initStandardFunc(set);
        this.root = new StandardRoot(this);
    }


    public StandardEvaluationContext root(Object root) {
        this.root = root;
        return this;
    }

    public StandardEvaluationContext putVar(String key, Object val) {
        super.setVariable(key, val);
        return this;
    }

    public StandardEvaluationContext importPack(String pack) {
        importMap.put("package:" + pack.strip(), "*");
        return this;
    }

    public StandardEvaluationContext importClass(String simpleClassname, String fullClassname) {
        importMap.put(simpleClassname, fullClassname);
        return this;
    }


    private void initDefaultBehaves() {
        super.setVariable("ctx", this);
        importMap.put("package:java.lang", "*");
        importMap.put("package:java.util", "*");
        importMap.put("package:java.net", "*");
        importMap.put("package:java.io", "*");
        importMap.put("package:java.time", "*");
        importMap.put("package:java.math", "*");
        importMap.put("package:java.util.function", "*");
        importMap.put("package:java.util.regex", "*");
        importMap.put("package:java.util.stream", "*");
        importMap.put("package:java.nio", "*");
        importMap.put("package:java.nio.file", "*");
        importMap.put("package:java.nio.charset", "*");
        importMap.put("package:java.nio.channels", "*");
        importMap.put("package:com.freedy.expression", "*");
        importMap.put("package:com.freedy.expression.core", "*");
        importMap.put("package:com.freedy.expression.jline", "*");
        importMap.put("package:com.freedy.expression.log", "*");
        importMap.put("package:com.freedy.expression.spring", "*");
        importMap.put("package:com.freedy.expression.token", "*");
        importMap.put("package:com.freedy.expression.tokenBuilder", "*");
        importMap.put("package:com.freedy.expression.utils", "*");
        importMap.put("package:com.freedy.expression.standard", "*");
        importMap.put("package:com.freedy.expression.standard.standardFunc", "*");
        registerFunctionWithHelp("condition", "use in for statement,to indicate whether should stop loop.\n\texample:def a=10; for(i:condition(@block{a++<10})){print(num);}; \n\t it will print 1 to 10", (Function._1ParameterFunction<TokenStream, TokenStream>) t -> t);
        registerFunctionWithHelp("tokenStream", "transfer to token stream", (Function._1ParameterFunction<TokenStream, TokenStream>) o -> o);
    }

    private void initStandardFunc(Set<String> standardFuncPackage) {
        Set<String> keywords = CodeDeCompiler.getKeywords();
        for (Class<?> funClass : PackageScanner.doScan(standardFuncPackage.toArray(String[]::new), new String[0])) {
            if (funClass.getSuperclass() != AbstractStandardFunc.class) continue;
            AbstractStandardFunc o;
            try {
                o = (AbstractStandardFunc) funClass.getConstructor().newInstance();
            } catch (Exception e) {
                throw new EvaluateException("init standard func failed", e);
            }
            o.setContext(this);
            for (Method method : funClass.getDeclaredMethods()) {
                ExpressionFunc func = method.getAnnotation(ExpressionFunc.class);
                if (func == null) continue;
                HashMap<String, FunctionalMethod.ValInjector> valInjectorMap = null;
                StringBuilder paramHelp = null;
                if (func.enableCMDParameter()) {
                    Class<?> types = getToBeParsedType(method);
                    valInjectorMap = new HashMap<>();
                    paramHelp = new StringBuilder();
                    for (Field f : ReflectionUtils.getFieldsRecursion(types)) {
                        CMDParameter cmd = f.getAnnotation(CMDParameter.class);
                        if (cmd == null) continue;
                        if (!cmd.value().startsWith("-")) {
                            throw new IllegalArgumentException("illegal parameter name ? in ?(?*),it should start with '-'",
                                    cmd.value(), method.getName(), Arrays.stream(method.getParameters())
                                    .map(p -> p.getType().getSimpleName() + " " + p.getName()).toList());
                        }
                        paramHelp.append(cmd.value()).append(" ".repeat(Math.max(20 - cmd.value().length(), 5))).append(cmd.helpText()).append("\n");
                        valInjectorMap.put(cmd.value(), new FunctionalMethod.ValInjector(f, cmd.value()));
                    }
                    for (Method m : ReflectionUtils.getMethodsRecursion(types)) {
                        CMDParameter cmd = m.getAnnotation(CMDParameter.class);
                        if (cmd == null) continue;
                        if (!cmd.value().startsWith("-")) {
                            throw new IllegalArgumentException("illegal parameter name? in ?(?*),it should start with '-'",
                                    cmd.value(), method.getName(), Arrays.stream(method.getParameters())
                                    .map(p -> p.getType().getSimpleName() + " " + p.getName()).toList());
                        }
                        paramHelp.append(cmd.value()).append(" ".repeat(Math.max(20 - cmd.value().length(), 5))).append(cmd.helpText()).append("\n");
                        valInjectorMap.put(cmd.value(), new FunctionalMethod.ValInjector(m, cmd.value()));
                    }
                    if (valInjectorMap.size() == 0) {
                        throw new IllegalArgumentException("no CMDParameter annotation found in standard function's arg ?(?*)",
                                method.getName(), Arrays.stream(method.getParameters())
                                .map(p -> p.getType().getSimpleName() + " " + p.getName()).toList());
                    }
                }
                String name = method.getName();
                registerFunctionWithHelp(
                        getRealName(keywords, name),
                        func.value() + (paramHelp == null ? "" : "\ndetail usage:\n\033[95m" + paramHelp + "\033[0;39m"),
                        new FunctionalMethod(o, method, valInjectorMap)
                );
            }
        }
    }

    private static String getRealName(Set<String> keywords, String name) {
        return name.startsWith("_") && keywords.contains(name.substring(1)) ? name.substring(1) : name;
    }

    private static Class<?> getToBeParsedType(Method method) {
        Class<?>[] types = method.getParameterTypes();
        if (types.length != 1) {
            throw new IllegalArgumentException("?(?*) couldn't enable CMDParameter,because the number of parameter of this function is not 1",
                    method.getName(), Arrays.stream(method.getParameters())
                    .map(p -> p.getType().getSimpleName() + " " + p.getName()).toList());
        }
        return types[0];
    }

    public record FunctionalMethod(Object funcObj, Method func,
                                   HashMap<String, ValInjector> cmdParamMap) implements Functional {
        public Object parseArgs(String[] args) throws Exception {
            Class<?>[] types = func.getParameterTypes();
            if (types.length > 1) throw new IllegalArgumentException("this fun couldn't generate parameter object");
            Object obj = types[0].getConstructor().newInstance();
            int argLen = args.length;
            for (int i = 0; i < argLen; i++) {
                if (!args[i].startsWith("-")) continue;
                ValInjector injector = cmdParamMap.get(args[i]);
                if (injector == null) throw new java.lang.IllegalArgumentException("unknown parameter" + args[i]);
                int j = i + 1;
                for (; j < argLen; j++) {
                    if (args[j].startsWith("-")) break;
                }
                injector.inject(obj, Arrays.copyOfRange(args, i + 1, j));
            }
            return obj;
        }


        private static class ValInjector {
            private final Class<?> origClass;
            private final String parameter;
            private Field valField;
            private Method valMethod;

            private ValInjector(Field valField, String parameter) {
                valField.setAccessible(true);
                this.valField = valField;
                origClass = valField.getDeclaringClass();
                this.parameter = parameter;
            }

            private ValInjector(Method valMethod, String parameter) {
                valMethod.setAccessible(true);
                this.valMethod = valMethod;
                origClass = valMethod.getDeclaringClass();
                this.parameter = parameter;
            }

            @SneakyThrows
            private void inject(Object o, String[] val) {
                Method setMethod = valMethod;
                if (valField != null) {
                    String fieldName = valField.getName();
                    String setMethodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                    for (Method method : ReflectionUtils.getMethodsRecursion(origClass)) {
                        if (method.getName().equals(setMethodName)) {
                            if (setMethod != null) {
                                setMethod = null;
                                break;
                            }
                            setMethod = method;
                        }
                    }
                    if (setMethod == null) {
                        if (val.length >= 2)
                            throw new java.lang.IllegalArgumentException("The number of parameters must be one");
                        valField.set(o, val[0]);
                        return;
                    }
                }
                Class<?>[] types = setMethod.getParameterTypes();
                int typeLen = types.length;
                int valLen = val.length;
                if (valLen % typeLen != 0) {
                    throw new java.lang.IllegalArgumentException("wrong number of parameter " + parameter + "'s value! it should be multiple of " + typeLen);
                }
                for (Class<?> type : types) {
                    if (!type.getName().contains("java.lang.String"))
                        throw new java.lang.IllegalArgumentException("can not parse parameter " + parameter + "! because the parameter type of the setter method of the target object is incorrect");
                }
                if (types[typeLen - 1].isArray()) {
                    Object[] args = new Object[typeLen];
                    System.arraycopy(val, 0, args, 0, typeLen - 1);
                    args[typeLen - 1] = Arrays.copyOfRange(val, typeLen - 1, valLen - 1);
                    setMethod.invoke(o, args);
                } else {
                    int multi = valLen / typeLen;
                    for (int i = 0; i < multi; i++) {
                        setMethod.invoke(o, (Object[]) Arrays.copyOfRange(val, i * typeLen, (i + 1) * typeLen));
                    }
                }
            }
        }
    }

    public void registerFunctionWithHelp(String funcName, String help, Functional functional) {
        Method method = functional instanceof FunctionalMethod m ? m.func : functional.getClass().getDeclaredMethods()[0];
        selfFuncHelp.put(funcName + Arrays.stream(method.getParameters()).map(param -> param.getType().getSimpleName() + " " + param.getName()).collect(Collectors.joining(",", "(", ")")), help);
        Functional old = registerFunction(funcName, functional);
        if (old != null) throw new IllegalArgumentException("you have already def ? function", funcName);
    }

    private StandardClassLoader loader = new StandardClassLoader();
    @Setter
    private Set<ClassLoader> loaderSet;

    public StandardClassLoader regardLoader() {
        StandardClassLoader temp = loader;
        loader = new StandardClassLoader();
        return temp;
    }

    public Collection<Path> addPath(String path) throws Exception {
        return loader.addPath(path);
    }


    @Override
    public Class<?> findClass(String className) throws ClassNotFoundException {
        if (!className.matches("\\w+|(?:\\w+[.$])+\\w+")) {
            throw new IllegalArgumentException("illegal class name ?", className);
        }
        try {
            return getaClass(loader, className);
        } catch (ClassNotFoundException e) {
            if (loaderSet == null || loaderSet.size() == 0) throw e;
            CombineException exception = new CombineException();
            exception.addException(e);
            for (ClassLoader loader : loaderSet) {
                try {
                    return getaClass(loader, className);
                } catch (ClassNotFoundException ex) {
                    exception.addException(ex);
                }
            }
            throw exception;
        }
    }

    private Class<?> getaClass(ClassLoader loader, String className) throws ClassNotFoundException {
        try {
            return loader.loadClass(className);
        } catch (ClassNotFoundException e) {
            String s = importMap.get(className);
            if (StringUtils.hasText(s)) {
                return loader.loadClass(s);
            }
            for (Map.Entry<String, String> entry : importMap.entrySet()) {
                if (entry.getKey().startsWith("package:") && entry.getValue().equals("*")) {
                    try {
                        Class<?> loadClass = loader.loadClass(entry.getKey().substring(8) + "." + className);
                        importMap.put(className, loadClass.getName());
                        return loadClass;
                    } catch (ClassNotFoundException ignore) {
                    }
                }
            }
            throw e;
        }
    }
}
