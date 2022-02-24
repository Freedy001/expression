package com.freedy.expression.stander;

import com.freedy.expression.*;
import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.exception.IllegalArgumentException;
import com.freedy.expression.function.*;
import com.freedy.expression.tokenBuilder.Tokenizer;
import com.freedy.expression.utils.PlaceholderParser;
import com.freedy.expression.utils.ReflectionUtils;
import com.freedy.expression.utils.StringUtils;
import jdk.internal.misc.Unsafe;
import lombok.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
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
@Getter
@Setter
@NoArgsConstructor
public class StanderEvaluationContext extends PureEvaluationContext {
    private final String id = DateTimeFormatter.ofPattern("hh:mm:ss-SSS").format(LocalDateTime.now());
    private final Map<String, String> selfFuncHelp = new HashMap<>();
    private Object root = new ROOT(this);
    private EvaluationContext superContext;
    private final HashMap<String, String> importMap = new HashMap<>();
    private final Expression selfExp = new Expression();

    {
        variableMap.put("context", this);
        importMap.put("package:java.lang", "*");
        importMap.put("package:java.util", "*");
        importMap.put("package:java.io", "*");
        importMap.put("package:java.time", "*");
        importMap.put("package:java.math", "*");
        importMap.put("package:java.util.function", "*");
        importMap.put("package:java.util.regex", "*");
        importMap.put("package:java.util.stream", "*");
    }

    public static final Map<String, TokenStream> CLASS_MAP = new HashMap<>();
    private final static Unsafe UNSAFE = (Unsafe) ReflectionUtils.getter(Unsafe.class, null, "theUnsafe");
    private final static ClassLoader APP_CLASSLOADER = StanderEvaluationContext.class.getClassLoader();
    private final static long CLASS_OFFSET = UNSAFE.objectFieldOffset(ClassLoader.class, "classes");
    public final static String CHARSET = System.getProperty("file.encoding") == null ? "UTF-8" : System.getProperty("file.encoding");

    @Getter
    @Setter
    public static class ROOT {
        private final static String pwd = new File("").getAbsolutePath();
        private final String ls = String.join("\n", Objects.requireNonNull(new File(".").list()));
        private final StanderEvaluationContext context;
        private final Properties env = System.getProperties();
        private Date time;
        private String help;
        private Set<String> allVar;

        public ROOT(StanderEvaluationContext context) {
            this.context = context;
        }

        public Date getTime() {
            return new Date();
        }

        public String getHelp() {
            if (StringUtils.hasText(help)) {
                return help;
            }
            StringBuilder builder = new StringBuilder();
            new TreeMap<>(context.selfFuncHelp).forEach((k, v) -> builder.append("\033[95m").append(k).append("\033[0;39m").append(50 - k.length() < 0 ? "\n\t---" : " ".repeat(50 - k.length())).append(v.contains("\n") ? v.substring(0, v.indexOf("\n")) : v).append("\n"));
            help = builder.toString();
            return help;
        }

        public Set<String> getAllVar(){
            return context.allVariables();
        }
    }

    public StanderEvaluationContext(Object root) {
        this.root = root;
    }

    public StanderEvaluationContext(EvaluationContext superContext) {
        this.root = superContext.getRoot();
        this.superContext = superContext;
    }

    {

        registerFunctionWithHelp("print", "same as System.out.println()", (Consumer._1ParameterConsumer<Object>) System.out::println);

        registerFunctionWithHelp("printInline", "same as System.out.print()", (Consumer._1ParameterConsumer<Object>) System.out::print);

        registerFunctionWithHelp("range", "generate a list start with param 1 and end with param 2", (Function._2ParameterFunction<Integer, Integer, List<Integer>>) (a, b) -> {
            ArrayList<Integer> list = new ArrayList<>();
            for (int i = a; i <= b; i++) {
                list.add(i);
            }
            return list;
        });

        registerFunctionWithHelp("stepRange", "generate a list start with param 1 and end with param 2 and each interval param 3", (Function._3ParameterFunction<Integer, Integer, Integer, List<Integer>>) (a, b, step) -> {
            ArrayList<Integer> list = new ArrayList<>();
            for (int i = a; i <= b; i += step) {
                list.add(i);
            }
            return list;
        });

        registerFunctionWithHelp("condition", "use in for statement,to indicate whether should stop loop.\n\texample:def a=10; for(i:condition(@block{a++<10})){print(num);}; \n\t it will print 1 to 10", (Function._1ParameterFunction<TokenStream, TokenStream>) t -> t);

        registerFunctionWithHelp("new", "new a instance,param 1 is full class name,the rest param is constructor's param", (VarFunction._2ParameterFunction<String, Object, Object>) (className, args) -> {
            Class<?> aClass = findClas(className);
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

        registerFunctionWithHelp("newInterface", """
                it will generate a object which implement the interface you delivered.
                params are like:
                    (interface-name,
                    func1-name,func1-param1,func1-param2,func1-body,
                    func2-name,func2-param1,func2-body,
                    func3-name,func3-body,
                    ...................................)
                form.
                example:
                you hava a java interface like this
                public interface com.freedy.expression.com.freedy.expression.Test{
                    void test1(int o1,int o2);
                    void test1(Object o1);
                }
                then code below:
                def a=newInterface('package.com.freedy.expression.com.freedy.expression.Test',
                    'test1','o1','o2',@block{
                        //your code
                        print('i am test1'+o1+o2);
                    },'test2','o2',@block{
                        //your code
                        print(o2);
                    });
                a.test1('1','2');
                a.test2('ni hao');
                                
                it will print:
                i am test112
                ni hao
                """, (VarFunction._2ParameterFunction<String, Object, Object>) (name, funcPara) -> {
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
        registerFunctionWithHelp("lambda", """
                simple use for newInterface() function,you just need interface full class name param and func-body.
                node: interface must have only one non-default-method;
                example:
                def list=[43,15,76,2,6];
                list.sort(lambda('java.util.Comparator','o1','o2',@block{return {o1-o2};}));
                print(list);
                                
                it will print:
                [2,6,15,43,76]
                """, (VarFunction._1ParameterFunction<Object, LambdaAdapter>) par -> {
            Object[] newParam = new Object[par.length + 1];
            System.arraycopy(par, 0, newParam, 1, par.length);
            newParam[0] = "lambda";
            return new LambdaAdapter(getFunc(newParam));
        });

        registerFunctionWithHelp("func", """
                use for def a function;
                the fist param is function name;
                the rest is function param and body;
                node:the function body use @block surround.
                example:
                func('add','a','b',@block{return {a+b};});
                print(add(54+46));
                                
                it will print:100
                """, (VarConsumer._1ParameterConsumer<Object>) funcPar -> {
            Func func = getFunc(funcPar);
            if (containsFunction(func.getFuncName())) {
                throw new EvaluateException("same method name ?!", func.getFuncName());
            }
            registerFunction(func.getFuncName(), func);
        });

        registerFunctionWithHelp("class", "get class .If the param is a string, it is taken as the full class name otherwise as Object", (Function._1ParameterFunction<Object, Class<?>>) this::getClassByArg);

        registerFunctionWithHelp("int", "transfer to int", (Function._1ParameterFunction<Object, Integer>) o -> o == null ? null : new BigDecimal(o.toString()).setScale(0, RoundingMode.DOWN).intValue());

        registerFunctionWithHelp("lf", "list function", (Function._1ParameterFunction<Object, List<String>>) o -> getMethod(getClassByArg(o)));

        registerFunctionWithHelp("lv", "list variable", (Function._1ParameterFunction<Object, List<String>>) o -> {
            Class<?> oClass = getClassByArg(o);
            return ReflectionUtils.getFieldsRecursion(oClass).stream().map(field -> {
                field.setAccessible(true);
                Type fieldType = field.getGenericType();
                String fieldTypeName = "?";
                if (fieldType instanceof ParameterizedType parameterizedType) {
                    String genericStr = getParameterizedType(parameterizedType);
                    fieldTypeName = ((Class<?>) parameterizedType.getRawType()).getSimpleName() + genericStr;
                } else if (fieldType instanceof Class<?> clazz) {
                    fieldTypeName = clazz.getSimpleName();
                }
                try {
                    return (Modifier.isStatic(field.getModifiers()) ? "\033[92mstatic \033[0;39m" : "") + "\033[91m" + fieldTypeName + "\033[0:39m \033[93m" + field.getName() + "\033[0;39m \033[34m" + (Modifier.isStatic(field.getModifiers()) ? getVarString(field.get(null)) : getVarString(field.get(o))) + "\033[0;39m";
                } catch (Exception e) {
                    throw new EvaluateException("?", e);
                }
            }).collect(Collectors.toList());
        });

        registerFunctionWithHelp("tokenStream", "transfer to token stream", (Function._1ParameterFunction<TokenStream, TokenStream>) o -> o);

        registerFunctionWithHelp("code", "decompile class to java source", (VarFunction._1ParameterFunction<Object, String>) o -> {
            if (o.length != 1 && o.length != 2) {
                throw new EvaluateException("parameters count must be 1 or 2");
            }
            if (o.length == 1) {
                return CodeDeCompiler.getCode(getClassByArg(o[0]), false, "");
            }
            return CodeDeCompiler.getCode(getClassByArg(o[0]), false, String.valueOf(o[1]));
        });

        registerFunctionWithHelp("dumpCode", "decompile class to java source and dump source file to specific location", (VarConsumer._1ParameterConsumer<Object>) o -> {
            if (o.length != 2 && o.length != 3) {
                throw new EvaluateException("parameters count must be 2 or 3");
            }
            Class<?> clazz = getClassByArg(o[0]);
            if (o.length == 2) {
                dump(clazz, "", String.valueOf(o[1]));
                return;
            }
            dump(clazz, String.valueOf(o[1]), String.valueOf(o[2]));
        });

        registerFunctionWithHelp("pwd", "relevant path to absolute path", (Suppler<String>) () -> new File("").getAbsolutePath());

        registerFunctionWithHelp("defClass", "def a class", (Function._2ParameterFunction<String, TokenStream, Class<?>>) (cName, tokenStreams) -> {
            int lastDot = cName.lastIndexOf(".");
            String packageName = cName.substring(0, lastDot);
            String className = cName.substring(lastDot + 1);

            StanderEvaluationContext context = new StanderEvaluationContext();
            //计算表达式
            selfExp.setContext(context);
            selfExp.setTokenStream(tokenStreams);
            selfExp.getValue();
            Map<String, Object> varMap = context.getVariableMap();
            Map<String, Functional> funcMap = context.getFunMap();
            Set<String> importSet = new HashSet<>();
            importSet.add("import com.freedy.expression.Expression;");
            importSet.add("import com.freedy.expression.stander.Func;");
            importSet.add("import com.freedy.expression.TokenStream;");
            importSet.add("import com.freedy.expression.function.Functional;");
            importSet.add("import java.util.Map;");
            StringBuilder code = new StringBuilder(new PlaceholderParser("""
                    \tprivate Map<String, Object> varMap;
                    \tprivate Map<String, Functional> funcMap;
                    \tprivate boolean hasInit=false;
                                        
                    \t{
                    \t    ExpressionClassEvaluateContext ownContext = new ExpressionClassEvaluateContext(this);
                    \t    new Expression(StanderEvaluationContext.CLASS_MAP.get("?"), ownContext).getValue();
                    \t    funcMap = ownContext.getFunMap();
                    \t    varMap = ownContext.getVariableMap();
                    \t    hasInit=true;
                    \t}
                    \t//below are user defined
                    """, cName).toString());
            varMap.forEach((k, v) -> {
                String fieldType;
                if (v == null) {
                    code.append(new PlaceholderParser("\tprivate Object ?=null;\n", k));
                    fieldType = "Object";
                } else {
                    Class<?> typeClass = v.getClass();
                    String simpleName = getSimpleName(importSet, typeClass);
                    code.append(new PlaceholderParser("""
                            \tprivate ? ? = (?) varMap.get("?");
                            """, simpleName, k, simpleName, k));
                    fieldType = simpleName;
                }
                String firstUpperName = k.substring(0, 1).toUpperCase(Locale.ROOT) + k.substring(1);
                code.append(new PlaceholderParser("""
                        \tpublic ? get?() {
                        \t    return ?;
                        \t}
                                                
                        """, fieldType, firstUpperName, k));
                code.append(new PlaceholderParser("""
                        \tpublic void set?(? ?) {
                        \t    if(hasInit){
                        \t        varMap.put("?", ?);
                        \t    }
                        \t    this.? = ?;
                        \t}
                                                
                        """, firstUpperName, fieldType, k, k, k, k, k));
            });
            funcMap.forEach((k, v) -> {
                if (v instanceof Func func) {
                    Object[] args = Arrays.stream(func.getArgName()).map(argName -> "Object " + argName).toArray();
                    if (k.equals("construct")) {
                        code.append(new PlaceholderParser("""
                                \tpublic ?(?*) {
                                \t    ((Func) funcMap.get("?")).apply(?*);
                                \t}
                                                        
                                """, className, args, k, func.getArgName()).ifEmptyFillWith("").serialParamsSplit(","));
                    } else {
                        code.append(new PlaceholderParser("""
                                \tpublic Object ?(?*) {
                                \t    return ((Func) funcMap.get("?")).apply(?*);
                                \t}
                                                        
                                """, k, args, k, func.getArgName()).serialParamsSplit(","));
                    }
                }
            });

            CustomStringJavaCompiler compiler = new CustomStringJavaCompiler(new PlaceholderParser("""
                    package ?;
                                        
                    ?*
                                        
                    public class ?{
                    ?
                    }
                    """, packageName, importSet, className, code).serialParamsSplit("\n").toString());
            if (!compiler.compiler()) {
                System.err.println("compile failed！");
                System.err.println(compiler.getCompilerMessage());
            }
            CLASS_MAP.put(cName, tokenStreams);
            return compiler.loadClass();
        });

        registerFunctionWithHelp("ls", "list all files under relative path", (Function._1ParameterFunction<String, String>) path -> String.join("\n", Objects.requireNonNull(new File(path).list())));

        registerFunctionWithHelp("lic", "list all inner class", (Function._1ParameterFunction<Object, List<Class<?>>>) o -> List.of(getClassByArg(o).getDeclaredClasses()));

        registerFunctionWithHelp("stdin", "stander input same as new Scanner(System.in).nextLine()", (Suppler<String>) () -> {
            if (CommanderLine.JAR_ENV) {
                return CommanderLine.READER.readLine();
            } else {
                return CommanderLine.SCANNER.nextLine();
            }
        });

        registerFunctionWithHelp("help", "detail help", (Consumer._1ParameterConsumer<String>) funcName -> selfFuncHelp.forEach((k, v) -> {
            if (k.toLowerCase(Locale.ROOT).contains(funcName.toLowerCase(Locale.ROOT))) {
                System.out.println("function:");
                System.out.println("\t\033[95m" + k + "\033[0;39m");
                System.out.println("explain:");
                System.out.println("\t\033[34m" + v.replace("\n", "\n    ") + "\033[0;39m");
                System.out.println();
                System.out.println();
            }
        }));

        registerFunctionWithHelp("loadedClass", "list loaded classes by your string", (VarFunction._1ParameterFunction<Object, Set<Class<?>>>) tip -> {
            //noinspection unchecked
            List<Class<?>> extLoad = (List<Class<?>>) UNSAFE.getReference(APP_CLASSLOADER.getParent(), CLASS_OFFSET);
            //noinspection unchecked
            List<Class<?>> appLoad = (List<Class<?>>) UNSAFE.getReference(APP_CLASSLOADER, CLASS_OFFSET);
            //noinspection unchecked
            List<Class<?>> customer = (List<Class<?>>) UNSAFE.getReference(CustomStringJavaCompiler.getSelfClassLoader(), CLASS_OFFSET);
            if (tip.length == 0 || StringUtils.isEmpty((String) tip[0])) {
                Set<Class<?>> result = new TreeSet<>(Comparator.comparing(Class::toString));
                result.addAll(extLoad);
                result.addAll(appLoad);
                result.addAll(customer);
                return result;
            }

            Set<Class<?>> result = new TreeSet<>(Comparator.comparing(Class::toString));
            for (Class<?> aClass : appLoad) {
                if (aClass.getName().contains((String) tip[0])) {
                    result.add(aClass);
                }
            }
            for (Class<?> aClass : customer) {
                if (aClass.getName().contains((String) tip[0])) {
                    result.add(aClass);
                }
            }
            for (Class<?> aClass : extLoad) {
                if (aClass.getName().contains((String) tip[0])) {
                    result.add(aClass);
                }
            }
            return result;
        });

        registerFunctionWithHelp("clearVar", "clear specific var", (VarConsumer._1ParameterConsumer<String>) varName -> {
            if (varName == null) return;
            for (String s : varName) {
                variableMap.remove(filterName(s));
            }
        });

        registerFunctionWithHelp("readFile", "read all byte from giving file", (Function._1ParameterFunction<String, String>) path -> {
            @Cleanup FileInputStream inputStream = new FileInputStream(path);
            return new String(inputStream.readAllBytes(), CHARSET);
        });


        registerFunctionWithHelp("import", "import statement for expression language", (VarConsumer._1ParameterConsumer<String>) packageName -> {
            for (String s : packageName) {
                if (!s.matches("\\w+|(?:\\w+\\.)+(?:\\*|\\w+)")) {
                    throw new IllegalArgumentException("illegal package name ?", s);
                }
                if (s.endsWith(".*")) {
                    importMap.put("package:" + s.substring(0, s.lastIndexOf(".")), "*");
                } else if (s.contains(".")) {
                    importMap.put(s.substring(s.lastIndexOf(".") + 1), s);
                } else {
                    importMap.put(s, s);
                }
            }
        });

        registerFunctionWithHelp("importInfo", "", (Suppler<Map<String, String>>) () -> importMap);

        registerFunctionWithHelp("clearImport", "", (VarConsumer._1ParameterConsumer<String>) name -> {
            for (String s : name) {
                if (!s.matches("\\w+|(?:\\w+\\.)+(?:\\*|\\w+)")) {
                    throw new IllegalArgumentException("illegal package name ?", s);
                }
                if (s.endsWith(".*")) {
                    String packageName = s.substring(0, s.lastIndexOf("."));
                    for (String s1 : importMap.entrySet().stream()
                            .filter(entry -> entry.getValue().startsWith(packageName)||entry.getKey().substring(8).startsWith(packageName))
                            .map(Map.Entry::getKey).toList()) {
                        importMap.remove(s1);
                    }
                } else {
                    importMap.remove(s);
                }
            }
        });

        registerFunctionWithHelp("grep", "", (Function._2ParameterFunction<String, String, String>) (str, pat) -> {
            StringBuilder builder = new StringBuilder();
            for (String s : str.split("\n")) {
                if (s.toLowerCase(Locale.ROOT).contains(pat.toLowerCase(Locale.ROOT))) {
                    builder.append(s.replace(pat, "\033[91m" + pat + "\033[0;39m")).append("\n");
                }
            }
            return builder.toString();
        });

        registerFunctionWithHelp("esc", "input a escape char", (Function._1ParameterFunction<String, String>) o -> {
            StringBuilder builder = new StringBuilder();
            int length = o.length();
            if (length % 2 != 0) {
                throw new java.lang.IllegalArgumentException("illegal escape character");
            }
            int lastSplit = 0;
            for (int i = 2; i <= length; i += 2) {
                switch (o.substring(lastSplit, i)) {
                    case "\\\"" -> builder.append("\"");
                    case "\\'" -> builder.append("'");
                    case "\\t" -> builder.append("\t");
                    case "\\b" -> builder.append("\b");
                    case "\\n" -> builder.append("\n");
                    case "\\r" -> builder.append("\r");
                    case "\\f" -> builder.append("\f");
                    case "\\\\" -> builder.append("\\");
                    default -> {
                        if (o.contains("\\")) {
                            throw new java.lang.IllegalArgumentException("illegal escape character");
                        }
                        return o;
                    }
                }
                lastSplit = i;
            }
            return builder.toString();
        });

        registerFunctionWithHelp("require", "execute script", (Consumer._1ParameterConsumer<String>) path -> {
            @Cleanup FileInputStream stream = new FileInputStream(path);
            selfExp.setContext(this);
            selfExp.setTokenStream(Tokenizer.getTokenStream(new String(stream.readAllBytes(),CHARSET)));
            selfExp.getValue();
        });
    }

    private String getSimpleName(Set<String> importCode, Class<?> returnType) {
        String returnName;
        if (ReflectionUtils.isBasicType(returnType)) {
            returnName = returnType.getName();
        } else {
            String className = returnType.getName();
            importCode.add("import " + className + ";");
            returnName = className.substring(className.lastIndexOf(".") + 1);
        }

        return returnName;
    }

    @SneakyThrows
    private Class<?> getClassByArg(Object arg) {
        if (arg instanceof String s) {
            return findClas(s);
        } else {
            if (arg == null) return null;
            return arg.getClass();
        }
    }

    private void dump(Class<?> clazz, String method, String path) throws IOException {
        File file = new File(String.valueOf(path));
        if (!file.isDirectory()) {
            throw new EvaluateException("path must be directory");
        }
        file = new File(file, StringUtils.hasText(method) ? clazz.getSimpleName() + ".txt" : clazz.getSimpleName() + ".java");
        @Cleanup FileOutputStream stream = new FileOutputStream(file);
        stream.write(CodeDeCompiler.getCode(clazz, true, method).getBytes(StandardCharsets.UTF_8));
        System.out.println("dump success to " + file.getAbsolutePath());
    }

    private String getParameterizedType(ParameterizedType parameterizedType) {
        StringJoiner joiner = new StringJoiner(",", "<", ">");
        for (Type type : parameterizedType.getActualTypeArguments()) {
            if (type instanceof ParameterizedType sub) {
                joiner.add(((Class<?>) sub.getRawType()).getSimpleName() + getParameterizedType(sub));
            } else if (type instanceof Class<?> clazz) {
                joiner.add(clazz.getSimpleName());
            }
        }
        return joiner.toString();
    }

    private List<String> getMethod(Class<?> aClass) {
        if (aClass == null) return null;
        List<String> list = new ArrayList<>();
        for (Method method : aClass.getDeclaredMethods()) {
            StringJoiner joiner = new StringJoiner(",", "(", ")");
            for (Class<?> type : method.getParameterTypes()) {
                joiner.add(type.getSimpleName());
            }
            list.add((Modifier.isStatic(method.getModifiers()) ? "\033[91mstatic \033[0;39m\033[94m" : "\033[94m") +
                    method.getReturnType().getSimpleName() + "\033[0;39m \033[93m" +
                    method.getName() + "\033[0;39m\033[95m" + joiner + "\033[39m");
        }
        return list;
    }

    private String getVarString(Object obj) {
        String str = String.valueOf(obj);
        if (str.contains("\n")) {
            return "[" + str.replaceAll("\n", ",") + "]";
        }
        return str;
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

    private void registerFunctionWithHelp(String funcName, String help, Functional functional) {
        selfFuncHelp.put(funcName + Arrays.stream(functional.getClass().getDeclaredMethods()[0].getParameters()).map(param -> param.getType().getSimpleName() + " " + param.getName()).collect(Collectors.joining(",", "(", ")")), help);
        registerFunction(funcName, functional);
    }

    @Override
    public Class<?> findClas(String className) throws ClassNotFoundException {
        if (!className.matches("\\w+|(?:\\w+\\.)+\\w+")) {
            throw new IllegalArgumentException("illegal class name ?", className);
        }
        ClassLoader loader = CustomStringJavaCompiler.getSelfClassLoader();
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

    public Object setRoot(Object root) {
        this.root = root;
        return root;
    }

    public Object getRoot() {
        return root;
    }

    @Override
    public boolean containsVariable(String name) {
        if (name.equals("root")) return true;
        return super.containsVariable(name);
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
}
