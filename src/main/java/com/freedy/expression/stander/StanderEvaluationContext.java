package com.freedy.expression.stander;

import com.freedy.expression.EvaluationContext;
import com.freedy.expression.PureEvaluationContext;
import com.freedy.expression.TokenStream;
import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.exception.IllegalArgumentException;
import com.freedy.expression.function.Function;
import com.freedy.expression.function.Functional;
import com.freedy.expression.stander.standerFunc.AbstractStanderFunc;
import com.freedy.expression.utils.PackageScanner;
import com.freedy.expression.utils.StringUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
    private String currentPath = ".";

    {
        variableMap.put("context", this);
        importMap.put("package:java.lang", "*");
        importMap.put("package:java.util", "*");
        importMap.put("package:java.io", "*");
        importMap.put("package:java.net", "*");
        importMap.put("package:java.time", "*");
        importMap.put("package:java.math", "*");
        importMap.put("package:java.util.function", "*");
        importMap.put("package:java.util.regex", "*");
        importMap.put("package:java.util.stream", "*");
    }

    private final static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    @Getter
    @Setter
    public static class ROOT {
        private final StanderEvaluationContext context;
        private final Properties env = System.getProperties();
        private String pwd;
        private String ls;
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

        public Set<String> getAllVar() {
            return context.allVariables();
        }

        public String getLs() {
            String[] fileArr = Arrays.stream(Objects.requireNonNull(new File(context.currentPath).listFiles())).map(f -> {
                StringBuilder builder = new StringBuilder();
                builder.append(f.isDirectory() ? "d" : "-");
                builder.append(f.canRead() ? "r" : "-");
                builder.append(f.canWrite() ? "w" : "-");
                builder.append(f.canExecute() ? "x" : "-");
                builder.append("\t");


                BasicFileAttributeView basicview = Files.getFileAttributeView(f.toPath(), BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
                BasicFileAttributes attr = null;
                try {
                    attr = basicview.readAttributes();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                builder.append(attr != null ? StanderEvaluationContext.SIMPLE_DATE_FORMAT.format(new Date(attr.creationTime().toMillis())) : "????-??-?? ??:??:??");
                builder.append("\t\t");

                StringBuilder b = new StringBuilder();
                char[] chars = (f.length() + "").toCharArray();
                for (int i = chars.length - 1, r = 1; i >= 0; i--, r++) {
                    b.append(r % 3 == 0 && i != 0 ? chars[i] + "," : chars[i]);
                }
                b.reverse();
                if (b.length() < 20) {
                    b.append(" ".repeat(20 - b.length()));
                }
                builder.append(b);
                builder.append("\t");
                builder.append(f.isDirectory() ? "\033[91m" : f.canExecute() ? "\033[93m" : "").append(f.getName()).append("\033[0;39m");

                return builder.toString();
            }).toArray(String[]::new);
            return String.join("\n", fileArr);
        }

        public String getPwd() {
            return new File(context.currentPath).getAbsolutePath();
        }
    }

    public record FunctionalMethod(Object funcObj, Method func) implements Functional {
    }

    public StanderEvaluationContext(Object root) {
        this.root = root;
    }

    public StanderEvaluationContext(EvaluationContext superContext) {
        this.root = superContext.getRoot();
        this.superContext = superContext;
    }

    {
        registerFunctionWithHelp("condition", "use in for statement,to indicate whether should stop loop.\n\texample:def a=10; for(i:condition(@block{a++<10})){print(num);}; \n\t it will print 1 to 10", (Function._1ParameterFunction<TokenStream, TokenStream>) t -> t);
        registerFunctionWithHelp("tokenStream", "transfer to token stream", (Function._1ParameterFunction<TokenStream, TokenStream>) o -> o);
        Set<String> keywords = CodeDeCompiler.getKeywords();
        for (Class<?> aClass : PackageScanner.doScan("com.freedy.expression.stander.standerFunc")) {
            if (aClass.getSuperclass() != AbstractStanderFunc.class) continue;
            AbstractStanderFunc o;
            try {
                o = (AbstractStanderFunc) aClass.getConstructor().newInstance();
            } catch (Exception e) {
                throw new EvaluateException("init stander func failed", e);
            }
            o.setContext(this);
            for (Method method : aClass.getDeclaredMethods()) {
                ExpressionFunc func = method.getAnnotation(ExpressionFunc.class);
                if (func != null) {
                    String name = method.getName();
                    if (name.startsWith("_") && keywords.contains(name.substring(1))) {
                        name = name.substring(1);
                    }
                    registerFunctionWithHelp(name, func.value(), new FunctionalMethod(o, method));
                }
            }
        }
    }


    public void registerFunctionWithHelp(String funcName, String help, Functional functional) {
        Method method = functional instanceof FunctionalMethod m ? m.func : functional.getClass().getDeclaredMethods()[0];
        selfFuncHelp.put(funcName + Arrays.stream(method.getParameters()).map(param -> param.getType().getSimpleName() + " " + param.getName()).collect(Collectors.joining(",", "(", ")")), help);
        registerFunction(funcName, functional);
    }


    @Override
    public Class<?> findClass(String className) throws ClassNotFoundException {
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
