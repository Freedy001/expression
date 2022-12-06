package com.freedy.expression.stander.standerFunc;

import com.freedy.expression.core.TokenStream;
import com.freedy.expression.function.Functional;
import com.freedy.expression.stander.CustomJavaCompiler;
import com.freedy.expression.stander.ExpressionFunc;
import com.freedy.expression.stander.Func;
import com.freedy.expression.stander.StanderEvaluationContext;
import com.freedy.expression.utils.PlaceholderParser;
import com.freedy.expression.utils.ReflectionUtils;

import java.util.*;

/**
 * @author Freedy
 * @date 2022/3/6 15:28
 */
public class StanderDefiner extends AbstractStanderFunc {
    public final static Map<String, TokenStream> CLASS_MAP = new HashMap<>();


    @ExpressionFunc("def a class")
    public Class<?> defClass(String cName, TokenStream tokenStreams) {
        CLASS_MAP.put(cName, tokenStreams);
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
        importSet.add("import com.freedy.expression.core.Expression;");
        importSet.add("import com.freedy.expression.stander.Func;");
        importSet.add("import com.freedy.expression.stander.ExpressionClassEvaluateContext;");
        importSet.add("import com.freedy.expression.core.TokenStream;");
        importSet.add("import com.freedy.expression.function.Functional;");
        importSet.add("import java.util.Map;");
        StringBuilder code = new StringBuilder(new PlaceholderParser("\n"+"\tprivate Map<String, Object> varMap;\n"+"\tprivate Map<String, Functional> funcMap;\n"+"\tprivate boolean hasInit=false;\n"+"\n"+"\t{\n"+"\t    ExpressionClassEvaluateContext ownContext = new ExpressionClassEvaluateContext(this);\n"+"\t    new Expression(com.freedy.expression.stander.standerFunc.StanderDefiner.CLASS_MAP.get(\"?\"), ownContext).getValue();\n"+"\t    funcMap = ownContext.getFunMap();\n"+"\t    varMap = ownContext.getVariableMap();\n"+"\t    hasInit=true;\n"+"\t}\n"+"\t//below are user defined\n"+"", cName).toString());
        varMap.forEach((k, v) -> {
            String fieldType;
            if (v == null) {
                code.append(new PlaceholderParser("\tprivate Object ?=null;\n", k));
                fieldType = "Object";
            } else {
                Class<?> typeClass = v.getClass();
                String simpleName = getSimpleName(importSet, typeClass);
                code.append(new PlaceholderParser("\n"+"\tprivate ? ? = (?) varMap.get(\"?\");\n"+"", simpleName, k, simpleName, k));
                fieldType = simpleName;
            }
            String firstUpperName = k.substring(0, 1).toUpperCase(Locale.ROOT) + k.substring(1);
            code.append(new PlaceholderParser("\n"+"\tpublic ? get?() {\n"+"\t    return ?;\n"+"\t}\n"+"\n"+"", fieldType, firstUpperName, k));
            code.append(new PlaceholderParser("\n"+"\tpublic void set?(? ?) {\n"+"\t    if(hasInit){\n"+"\t        varMap.put(\"?\", ?);\n"+"\t    }\n"+"\t    this.? = ?;\n"+"\t}\n"+"\n"+"", firstUpperName, fieldType, k, k, k, k, k));
        });
        funcMap.forEach((k, v) -> {
            if (v instanceof Func ) {
Func func = (Func) v;
                Object[] args = Arrays.stream(func.getArgName()).map(argName -> "Object " + argName).toArray();
                if (k.equals("construct")) {
                    code.append(new PlaceholderParser("\n"+"\tpublic ?(?*) {\n"+"\t    ((Func) funcMap.get(\"?\")).apply(?*);\n"+"\t}\n"+"\n"+"", className, args, k, func.getArgName()).ifEmptyFillWith("").serialParamsSplit(","));
                } else {
                    code.append(new PlaceholderParser("\n"+"\tpublic Object ?(?*) {\n"+"\t    return ((Func) funcMap.get(\"?\")).apply(?*);\n"+"\t}\n"+"\n"+"", k, args, k, func.getArgName()).ifEmptyFillWith("").serialParamsSplit(","));
                }
            }
        });

        CustomJavaCompiler compiler = new CustomJavaCompiler(new PlaceholderParser("\n"+"package ?;\n"+"\n"+"?*\n"+"\n"+"public class ?{\n"+"?\n"+"}\n"+"", packageName, importSet, className, code).serialParamsSplit("\n").toString());
        if (!compiler.compiler()) {
            System.err.println("compile failed！");
            System.err.println(compiler.getCompilerMessage());
        }
        return compiler.loadClass();
    }

    @ExpressionFunc("compile java code")
    public Class<?> compileJava(String code) {
        CustomJavaCompiler compiler = new CustomJavaCompiler(code);
        if (!compiler.compiler()) {
            System.err.println("compile failed！");
            System.err.println(compiler.getCompilerMessage());
        }
        return compiler.loadClass();
    }


    @ExpressionFunc("find loadedClass")
    public boolean isDef(String varName) {
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
