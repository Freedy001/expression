package com.freedy.expression.stander.standerFunc;

import com.freedy.expression.TokenStream;
import com.freedy.expression.function.Functional;
import com.freedy.expression.stander.CustomStringJavaCompiler;
import com.freedy.expression.stander.ExpressionFunc;
import com.freedy.expression.stander.Func;
import com.freedy.expression.stander.StanderEvaluationContext;
import com.freedy.expression.utils.PlaceholderParser;
import com.freedy.expression.utils.ReflectionUtils;
import com.freedy.expression.utils.StringUtils;
import jdk.internal.misc.Unsafe;

import java.util.*;

/**
 * @author Freedy
 * @date 2022/3/6 15:28
 */
public class StanderDefiner extends AbstractStanderFunc {
    private final static Unsafe UNSAFE = (Unsafe) ReflectionUtils.getter(Unsafe.class, null, "theUnsafe");
    private final static ClassLoader APP_CLASSLOADER = StanderEvaluationContext.class.getClassLoader();
    private final static long CLASS_OFFSET = UNSAFE.objectFieldOffset(ClassLoader.class, "classes");
    public final static Map<String, TokenStream> CLASS_MAP=new HashMap<>();

    @ExpressionFunc("def a class")
    public Class<?> defClass(String cName, TokenStream tokenStreams){
        CLASS_MAP.put(cName,tokenStreams);
        int lastDot = cName.lastIndexOf(".");
        String packageName = cName.substring(0, lastDot);
        String className = cName.substring(lastDot + 1);
        StanderEvaluationContext context = new StanderEvaluationContext();
        //计算表达式
        selfExp.setDefaultContext(context);
        selfExp.setTokenStream(tokenStreams);
        selfExp.getValue();
        Map<String, Object> varMap = context.getVariableMap();
        Map<String, Functional> funcMap = context.getFunMap();
        Set<String> importSet = new HashSet<>();
        importSet.add("import com.freedy.expression.Expression;");
        importSet.add("import com.freedy.expression.stander.Func;");
        importSet.add("import com.freedy.expression.stander.ExpressionClassEvaluateContext;");
        importSet.add("import com.freedy.expression.TokenStream;");
        importSet.add("import com.freedy.expression.function.Functional;");
        importSet.add("import java.util.Map;");
        StringBuilder code = new StringBuilder(new PlaceholderParser("""
                    \tprivate Map<String, Object> varMap;
                    \tprivate Map<String, Functional> funcMap;
                    \tprivate boolean hasInit=false;
                                        
                    \t{
                    \t    ExpressionClassEvaluateContext ownContext = new ExpressionClassEvaluateContext(this);
                    \t    new Expression(com.freedy.expression.stander.standerFunc.StanderDefiner.CLASS_MAP.get("?"), ownContext).getValue();
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
                                                        
                                """, k, args, k, func.getArgName()).ifEmptyFillWith("").serialParamsSplit(","));
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
        return compiler.loadClass();
    }

    @ExpressionFunc("compile java code")
    public Class<?> compileJava(String code){
        CustomStringJavaCompiler compiler = new CustomStringJavaCompiler(code);
        if (!compiler.compiler()) {
            System.err.println("compile failed！");
            System.err.println(compiler.getCompilerMessage());
        }
        return compiler.loadClass();
    }


    @ExpressionFunc("find loadedClass")
    public Set<Class<?>> loadedClass(String ...tip){
        //noinspection unchecked
        List<Class<?>> extLoad = (List<Class<?>>) UNSAFE.getReference(APP_CLASSLOADER.getParent(), CLASS_OFFSET);
        //noinspection unchecked
        List<Class<?>> appLoad = (List<Class<?>>) UNSAFE.getReference(APP_CLASSLOADER, CLASS_OFFSET);
        //noinspection unchecked
        List<Class<?>> customer = (List<Class<?>>) UNSAFE.getReference(CustomStringJavaCompiler.getSelfClassLoader(), CLASS_OFFSET);
        if (tip.length == 0 || StringUtils.isEmpty(tip[0])) {
            Set<Class<?>> result = new TreeSet<>(Comparator.comparing(Class::toString));
            result.addAll(extLoad);
            result.addAll(appLoad);
            result.addAll(customer);
            return result;
        }

        Set<Class<?>> result = new TreeSet<>(Comparator.comparing(Class::toString));
        for (Class<?> aClass : appLoad) {
            if (aClass.getName().contains(tip[0])) {
                result.add(aClass);
            }
        }
        for (Class<?> aClass : customer) {
            if (aClass.getName().contains(tip[0])) {
                result.add(aClass);
            }
        }
        for (Class<?> aClass : extLoad) {
            if (aClass.getName().contains(tip[0])) {
                result.add(aClass);
            }
        }
        return result;
    }

    @ExpressionFunc("find loadedClass")
    public boolean isDef(String varName){
        return context.containsVariable(varName);
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

}
