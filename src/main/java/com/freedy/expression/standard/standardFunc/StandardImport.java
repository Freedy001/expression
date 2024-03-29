package com.freedy.expression.standard.standardFunc;

import com.freedy.expression.core.Tokenizer;
import com.freedy.expression.exception.IllegalArgumentException;
import com.freedy.expression.standard.ExpressionFunc;
import com.freedy.expression.standard.StandardClassLoader;
import lombok.Cleanup;
import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import static com.freedy.expression.SysConstant.CHARSET;

/**
 * @author Freedy
 * @date 2022/3/6 15:29
 */
public class StandardImport extends AbstractStandardFunc {

    @ExpressionFunc(value = "import statement for expression language")
    public void _import(String... packageName) {
        for (String s : packageName) {
            if (!s.matches("\\w+|(?:\\w+\\.)+(?:\\*|\\w+)")) {
                throw new IllegalArgumentException("illegal package name ?", s);
            }
            if (s.endsWith(".*")) {
                context.getImportMap().put("package:" + s.substring(0, s.lastIndexOf(".")), "*");
            } else if (s.contains(".")) {
                context.getImportMap().put(s.substring(s.lastIndexOf(".") + 1), s);
            } else {
                context.getImportMap().put(s, s);
            }
        }
    }

    @ExpressionFunc
    public Map<String, String> importInfo(){
        return context.getImportMap();
    }

    @ExpressionFunc
    public void clearImport(String ...name){
        Map<String, String> importMap = context.getImportMap();
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
    }

    @ExpressionFunc("""
            import a pom file or a class directory or a jar file to class path
            The return value represents the path where the jar package was not successfully added
            if you import a wrong path you can call clearPath() function or ctx.regardLoader() method!
            """)
    public Collection<Path> addPath(String path) throws Exception {
        return context.addPath(path);
    }

    @ExpressionFunc("clear added path!")
    public StandardClassLoader clearPath(){
        return context.regardLoader();
    }


    @SneakyThrows
    @ExpressionFunc("execute script")
    public void require(String path){
        @Cleanup FileInputStream stream = new FileInputStream(path);
        selfExp.setDefaultContext(context);
        selfExp.setTokenStream(Tokenizer.getTokenStream(new String(stream.readAllBytes(), CHARSET)));
        selfExp.getValue();
    }

}
