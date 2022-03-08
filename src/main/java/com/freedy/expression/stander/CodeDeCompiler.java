package com.freedy.expression.stander;

import com.freedy.expression.utils.StringUtils;
import lombok.*;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Freedy
 * @date 2022/1/7 22:37
 */
public class CodeDeCompiler {
    @Setter
    private static String KEYWORD = "\033[91m";
    @Setter
    private static String BIG_BRACKET = "\033[34m";
    @Setter
    private static String SMALL_BRACKET = "\033[36m";
    @Setter
    private static String ANNOTATION = "\033[93m";
    @Setter
    private static String NUMBER = "\033[94m";
    @Setter
    private static String STRING = "\033[32m";
    @Setter
    public static String END = "\033[0;39m";
    public static final Set<Character> splitSet = Set.of(' ', '\n', '.', '<', '>', ';', '=', '(', ')', '{', '}');
    @Getter
    private static final Set<String> keywords = Set.of("abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double",
            "else", "enum", "extends", "false", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "null", "package", "private",
            "protected", "public", "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws", "transient", "try",
            "true", "void", "volatile", "while", "sealed", "permits");
    private static final Pattern annotationPattern = Pattern.compile("@ *?[a-zA-Z]\\w+");
    private static final Pattern numPattern = Pattern.compile("[*/+-]?\\d*_*\\d*\\.?\\d*_*\\d*");

    @Data
    @AllArgsConstructor
    public static class ClassByteCode {
        private String className;
        private byte[] bytecode;
    }

    private static String getCode(List<ClassByteCode> classNameAndByteCode, boolean raw, String method) {
        StringBuilder codeStr = new StringBuilder();
        Map<String, byte[]> classNameCodeMap = classNameAndByteCode.stream().collect(Collectors.toMap(ClassByteCode::getClassName, ClassByteCode::getBytecode));
        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(StringUtils.isEmpty(method) ?
                        Map.of("decodestringswitch", "false", "sealed", "true") :
                        Map.of("decodestringswitch", "false", "sealed", "true", "methodname", method))
                .withOutputSink(new OutputSinkFactory() {
                    @Override
                    public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
                        return Collections.singletonList(SinkClass.STRING);
                    }

                    @Override
                    public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                        return sinkType == SinkType.JAVA ? code -> {
                            if (raw) {
                                codeStr.append(code);
                            } else {
                                codeStr.append(syntaxHighlight(String.valueOf(code)));
                            }
                        } : ignore -> {
                        };
                    }

                }).withOverrideClassFileSource(new ClassFileSource() {
                    @Override
                    public void informAnalysisRelativePathDetail(String s, String s1) {
                    }

                    @Override
                    public Collection<String> addJar(String s) {
                        return null;
                    }

                    @Override
                    public String getPossiblyRenamedPath(String s) {
                        return null;
                    }

                    @Override
                    public Pair<byte[], String> getClassFileContent(String className) throws IOException {
                        return Pair.make(classNameCodeMap.get(className), className);
                    }
                }).build();
        driver.analyse(classNameAndByteCode.stream().map(ClassByteCode::getClassName).toList());
        return codeStr.toString();
    }


    /**
     * 反编译指定字节码对象
     *
     * @param clazz  字节码对象
     * @param raw    是否展示语法高亮
     * @param method 反编译的方法,如需反编译整个类则只需要传入 ""
     * @return 反编译后的java代码
     */
    @SneakyThrows
    public static String getCode(Class<?> clazz, boolean raw, String method) {
        String fullName = clazz.getName();
        Map<String, CustomStringJavaCompiler.ByteJavaFileObject> javaFileObjectMap = CustomStringJavaCompiler.getJavaFileObjectMap();
        if (javaFileObjectMap.get(fullName) != null) {
            List<ClassByteCode> list = new ArrayList<>();
            getExpressionByteCode(clazz, javaFileObjectMap, list);
            return getCode(list, raw, method);
        }
        return getCode(getFiles(clazz), raw, method);
    }

    @SneakyThrows
    private static List<ClassByteCode> getFiles(Class<?> clazz) {
        String fullName = clazz.getName().replace(".", "/");
        URL url = clazz.getResource("");
        assert url != null;
        List<ClassByteCode> codeList = new ArrayList<>();
        if (url.getProtocol().equals("jar") && url.openConnection() instanceof JarURLConnection connection) {
            JarFile jarFile = connection.getJarFile();
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                String name = jarEntry.getName();
                if (name.equals(fullName + ".class") || name.startsWith(fullName + "$")) {
                    InputStream inputStream = jarFile.getInputStream(jarEntry);
                    codeList.add(new ClassByteCode(name, inputStream.readAllBytes()));
                }
            }
        } else {
            int lastDot = fullName.lastIndexOf("/");
            String basePath = fullName.substring(0, lastDot) + "/";
            String simpleName = fullName.substring(lastDot + 1) + ".class";
            for (String fileName : Objects.requireNonNull(new File(Objects.requireNonNull(clazz.getResource("")).toURI())
                    .list((dir1, name) -> name.equals(simpleName) || name.startsWith(simpleName + "$")))) {
                codeList.add(new ClassByteCode(basePath + fileName, getBytecode(clazz, fileName)));
            }
        }
        return codeList;
    }

    private static void getExpressionByteCode(Class<?> clazz, Map<String, CustomStringJavaCompiler.ByteJavaFileObject> javaFileObjectMap, List<ClassByteCode> list) {
        String fullName = clazz.getName();
        CustomStringJavaCompiler.ByteJavaFileObject fileObject = javaFileObjectMap.get(fullName);
        if (fileObject != null) {
            list.add(new ClassByteCode(fullName.replace(".", "/") + ".class", fileObject.getCompiledBytes()));
            for (Class<?> aClass : clazz.getDeclaredClasses()) {
                getExpressionByteCode(aClass, javaFileObjectMap, list);
            }
        }
    }

    @SneakyThrows
    private static byte[] getBytecode(Class<?> clazz, String fileName) {
        return Objects.requireNonNull(clazz.getResourceAsStream(fileName)).readAllBytes();
    }

    /**
     * 语法高亮
     */
    public static String syntaxHighlight(String codeString) {
        StringBuilder builder = new StringBuilder();
        codeString = "\n" + codeString;
        char[] chars = codeString.toCharArray();

        int lastSplit = 0;
        int quoteIndex = -1;
        boolean singleQuote = false;
        int length = chars.length;
        for (int i = 0; i < length; i++) {
            if (chars[i] == '"') {
                if (quoteIndex != -1) {
                    builder.append(codeString, lastSplit, quoteIndex)
                            .append(STRING)
                            .append(codeString, quoteIndex, i + 1)
                            .append(END);
                    quoteIndex = -1;
                    lastSplit = i + 1;
                    continue;
                } else {
                    quoteIndex = i;
                }
            }
            if (quoteIndex != -1) continue;
            if (chars[i] == '\'') singleQuote = !singleQuote;
            if (singleQuote) continue;


            if (splitSet.contains(chars[i])) {
                checkAndHighLight(codeString, builder, lastSplit, i);
                lastSplit = i;
            }
        }
        checkAndHighLight(codeString, builder, lastSplit, length);

        return builder.toString().replaceFirst("\n", "");
    }

    private static void checkAndHighLight(String codeString, StringBuilder builder, int lastSplit, int i) {
        if (lastSplit == i) return;
        String symbolHighLight = codeString.substring(lastSplit, lastSplit + 1);
        switch (symbolHighLight) {
            case "(", ")" -> builder.append(SMALL_BRACKET).append(symbolHighLight).append(END);
            case "{", "}" -> builder.append(BIG_BRACKET).append(symbolHighLight).append(END);
            case "<", ">" -> builder.append(NUMBER).append(symbolHighLight).append(END);
            case ";" -> builder.append(KEYWORD).append(symbolHighLight).append(END);
            default -> builder.append(symbolHighLight);
        }

        String testArea = codeString.substring(lastSplit + 1, i);
        if (testArea.length() == 0) return;
        if (keywords.contains(testArea.trim())) {
            builder.append(KEYWORD).append(testArea).append(END);
            return;
        }
        Matcher matcher = annotationPattern.matcher(testArea.trim());
        if (matcher.matches()) {
            builder.append(ANNOTATION).append(testArea).append(END);
            return;
        }
        matcher = numPattern.matcher(testArea.trim());
        if (matcher.matches()) {
            builder.append(NUMBER).append(testArea).append(END);
            return;
        }
        builder.append(testArea);
    }

}
