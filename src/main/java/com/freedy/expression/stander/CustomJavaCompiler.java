package com.freedy.expression.stander;

import com.freedy.expression.exception.IllegalArgumentException;
import lombok.Getter;
import lombok.SneakyThrows;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2022/1/8 14:54
 */
public class CustomJavaCompiler {
    //类全名
    private final String fullClassName;
    private final String sourceCode;
    //存放编译过程中输出的信息
    private final DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();
    //获取java的编译器
    private final static JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    //存放编译之后的字节码(key:类全名,value:编译之后输出的字节码)
    @Getter
    private final static Map<String, ByteJavaFileObject> javaFileObjectMap = new ConcurrentHashMap<>();
    @Getter
    private final static ClassLoader selfClassLoader = new StringClassLoader();

    public CustomJavaCompiler(String sourceCode) {
        this.sourceCode = sourceCode;
        this.fullClassName = getFullClassName(sourceCode);
    }

    /**
     * 编译字符串源代码,编译失败在 diagnosticsCollector 中获取提示信息
     *
     * @return true:编译成功 false:编译失败
     */
    public boolean compiler() {
        //标准的内容管理器,更换成自己的实现，覆盖部分方法
        StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(diagnosticsCollector, null, null);
        JavaFileManager javaFileManager = new StringJavaFileManage(standardFileManager);
        //构造源代码对象
        JavaFileObject javaFileObject = new StringJavaFileObject(fullClassName, sourceCode);
        //获取一个编译任务
        JavaCompiler.CompilationTask task = compiler.getTask(null, javaFileManager, diagnosticsCollector, null, null, List.of(javaFileObject));
        return task.call();
    }


    /**
     * @return 编译信息(错误 警告)
     */
    public String getCompilerMessage() {
        StringBuilder sb = new StringBuilder();
        List<Diagnostic<? extends JavaFileObject>> diagnostics = diagnosticsCollector.getDiagnostics();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
            sb.append(diagnostic.toString()).append("\r\n");
        }
        return sb.toString();
    }

    @SneakyThrows
    public Class<?> loadClass() {
        return selfClassLoader.loadClass(fullClassName);
    }


    /**
     * 获取类的全名称
     *
     * @param sourceCode 源码
     * @return 类的全名称
     */
    public static String getFullClassName(String sourceCode) {
        String className = "";
        Pattern pattern = Pattern.compile("package\\s+\\S+\\s*;");
        Matcher matcher = pattern.matcher(sourceCode);
        if (matcher.find()) {
            className = matcher.group().replaceFirst("package", "").replace(";", "").trim() + ".";
        }

        pattern = Pattern.compile(".*?class\\s+(.+)\\{");
        matcher = pattern.matcher(sourceCode);
        if (matcher.find()) {
            className += matcher.group(1).trim();
        }
        return className;
    }

    /**
     * 自定义一个字符串的源码对象
     */
    private static class StringJavaFileObject extends SimpleJavaFileObject {
        //等待编译的源码字段
        private final String contents;

        //java源代码 => StringJavaFileObject对象 的时候使用
        public StringJavaFileObject(String className, String contents) {
            super(URI.create("string:///" + className.replaceAll("\\.", "/") + Kind.SOURCE.extension), Kind.SOURCE);
            this.contents = contents;
        }

        //字符串源码会调用该方法
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return contents;
        }

    }

    /**
     * 自定义一个编译之后的字节码对象
     */
    public static class ByteJavaFileObject extends SimpleJavaFileObject {
        //存放编译后的字节码
        private ByteArrayOutputStream outPutStream;

        public ByteJavaFileObject(String className, Kind kind) {
            super(URI.create("string:///" + className.replaceAll("\\.", "/") + Kind.SOURCE.extension), kind);
        }

        //StringJavaFileManage 编译之后的字节码输出会调用该方法（把字节码输出到outputStream）
        @Override
        public OutputStream openOutputStream() {
            outPutStream = new ByteArrayOutputStream();
            return outPutStream;
        }

        //在类加载器加载的时候需要用到
        public byte[] getCompiledBytes() {
            return outPutStream.toByteArray();
        }
    }

    /**
     * 自定义一个JavaFileManage来控制编译之后字节码的输出位置
     */
    private static class StringJavaFileManage extends ForwardingJavaFileManager<StandardJavaFileManager> {
        StringJavaFileManage(StandardJavaFileManager fileManager) {
            super(fileManager);
        }

        //获取输出的文件对象，它表示给定位置处指定类型的指定类。
        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
            ByteJavaFileObject javaFileObject = new ByteJavaFileObject(className, kind);
            if (javaFileObjectMap.put(className, javaFileObject) != null) {
                throw new IllegalArgumentException("you have def ?", className);
            }
            return javaFileObject;
        }
    }

    /**
     * 自定义类加载器, 用来加载动态的字节码
     */
    private static class StringClassLoader extends ClassLoader {

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            ByteJavaFileObject fileObject = javaFileObjectMap.get(name);
            if (fileObject != null) {
                byte[] bytes = fileObject.getCompiledBytes();
                return defineClass(name, bytes, 0, bytes.length);
            }
            return super.findClass(name);
        }
    }
}
