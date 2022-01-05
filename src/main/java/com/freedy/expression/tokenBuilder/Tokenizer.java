package com.freedy.expression.tokenBuilder;

import com.freedy.expression.TokenStream;
import com.freedy.expression.exception.ExpressionSyntaxException;
import com.freedy.expression.token.ErrMsgToken;
import com.freedy.expression.token.OpsToken;
import com.freedy.expression.token.TernaryToken;
import com.freedy.expression.token.Token;
import com.freedy.expression.utils.StringUtils;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.freedy.expression.utils.StringUtils.splitWithoutBracket;

/**
 * @author Freedy
 * @date 2021/12/14 15:28
 */
public class Tokenizer {

    //[<=>| static !+_*?()]
    private static final Set<Character> operationSet = Set.of('=', '<', '>', '|', '&', '!', '+', '-', '*', '/', '(', ')', '?');
    private static final Set<Character> bracket = Set.of('(', ')');
    private static final List<Builder> builderSet = new ArrayList<>();

    static {
        scanBuilder();
    }

    @SneakyThrows
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void scanBuilder() {
        for (Class aClass : doScan(new String[]{"com.freedy.expression.tokenBuilder"}, null)) {
            if (Optional.ofNullable(aClass.getSuperclass()).orElse(Object.class).getName().equals("com.freedy.expression.tokenBuilder.Builder")) {
                builderSet.add((Builder) aClass.getConstructor().newInstance());
            }
        }
        builderSet.sort(Comparator.comparing(Builder::priority));
    }

    public static List<Class<?>> doScan(@NonNull String[] PackageNames, String[] exclude) {

        ClassLoader classLoader = Tokenizer.class.getClassLoader();
        List<Class<?>> list = new ArrayList<>();

        for (String PackageName : PackageNames) {
            try {
                URL url = classLoader.getResource(PackageName.replaceAll("\\.", "/"));
                assert url != null;
                String protocol = url.getProtocol();
                if (protocol.equals("file")) {
                    fileScan(exclude, list, PackageName, url);
                } else if (protocol.equals("jar")) {
                    jarScan(exclude, list, PackageName, classLoader);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return list;
    }

    private static void fileScan(String[] exclude, List<Class<?>> list, String PackageName, URL url) throws IOException, URISyntaxException {
        String[] packSplit = PackageName.split("\\.");
        String lastPackageName = packSplit[packSplit.length - 1];
        Files.walk(Paths.get(url.toURI())).forEach(pa -> {
            if (Files.isRegularFile(pa)) {
                String[] split = pa.toString().split("\\\\");
                int length = split.length;
                int index = length - 1;
                for (; index >= 0; index--) {
                    if (split[index].toLowerCase(Locale.ROOT).equals(lastPackageName.toLowerCase(Locale.ROOT))) {
                        break;
                    }
                }

                if (index == -1) return;
                StringJoiner joiner = new StringJoiner(".");
                for (int i = index + 1; i < length; i++) {
                    if (i == length - 1) {
                        String[] s = split[i].split("\\.");
                        if (!s[1].equals("class")) return;
                        joiner.add(s[0]);
                        break;
                    }
                    joiner.add(split[i]);
                }

                try {
                    String fullClassName = PackageName + "." + joiner;
                    if (exclude != null) {
                        for (String s : exclude) {
                            if (fullClassName.contains(s)) return;
                        }
                    }

                    Class<?> aClass = Class.forName(fullClassName);
                    list.add(aClass);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    private static void jarScan(String[] exclude, List<Class<?>> list, String PackageName, ClassLoader loader) throws Exception {
        String pathName = PackageName.replace(".", "/");
        URL url = loader.getResource(pathName);
        assert url != null;
        JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
        JarFile jarFile = jarURLConnection.getJarFile();

        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            String jarName = jarEntry.getName();
            if (jarName.contains(pathName) &&
                    !jarName.equals(pathName + "/") &&
                    !jarEntry.isDirectory() &&
                    jarName.endsWith(".class")) {
                String fullClazzName = jarName.replace("/", ".").replace(".class", "");
                if (exclude != null) {
                    for (String s : exclude) {
                        if (fullClazzName.contains(s)) return;
                    }
                }
                list.add(Class.forName(fullClazzName));
            }
        }
    }

    public static TokenStream getTokenStream(String expression) {
        //处理注释
        StringJoiner joiner = new StringJoiner(" ");
        for (String sub : expression.split("\n")) {
            int i1 = sub.indexOf("//");
            sub = sub.substring(0, i1 == -1 ? sub.length() : i1).trim();
            joiner.add(sub);
        }
        return doGetTokenStream(joiner.toString().trim());
    }


    @NonNull
    static TokenStream doGetTokenStream(String expression) {
        TokenStream stream = new TokenStream(expression);
        return doGetTokenStream(expression, stream);
    }

    private static TokenStream doGetTokenStream(String expression, TokenStream tokenStream) {
        String[] bracket = splitWithoutBracket(expression, new char[]{'{', '('}, new char[]{'}', ')'}, ';');
        for (String sub : bracket) {
            if (StringUtils.isEmpty(sub = sub.trim())) continue;
            if (sub.startsWith("{") && sub.endsWith("}")) {
                sub = sub.substring(1, sub.length() - 1);
                doGetTokenStream(sub, tokenStream);
            } else {
                parseExpression(sub, tokenStream);
                tokenStream.splitStream();
            }
        }
        return tokenStream;
    }


    private static void parseExpression(String expression, TokenStream tokenStream) {

        if (StringUtils.isEmpty(expression)) return;

        char[] chars = expression.toCharArray();
        final int length = chars.length;

        int lastOps = 0;
        int expressionLeftBracket = 0;

        boolean quoteInside = false;
        int leftBracesCount = 0;
        int leftBracketCount = 0;


        for (int i = 0; i < length; i++) {
            char inspectChar = chars[i];

            if (inspectChar == '{') {
                leftBracesCount++;
                continue;
            }

            if (inspectChar == '}') {
                leftBracesCount--;
                continue;
            }

            if (inspectChar == '[') {
                leftBracketCount++;
                continue;
            }

            if (inspectChar == ']') {
                leftBracketCount--;
                continue;
            }

            if (leftBracesCount > 0 || leftBracketCount > 0) continue;


            if (inspectChar == '\'') {
                quoteInside = !quoteInside;
                continue;
            }


            if (inspectChar == ' ' || !operationSet.contains(inspectChar) || quoteInside) {
                continue;
            }

            String token = expression.substring(lastOps, i).trim();

            if (inspectChar == '?') {
                int index = nextNonempty(chars, i);
                if (index == -1) continue;
                if (chars[index] == '.') continue;
                if (chars[index] == '?') continue;

                //尝试构建三元token
                int[] ternaryIndex = {i};
                TernaryToken ternaryToken = buildTernary(expression, ternaryIndex);
                if (ternaryToken == null) continue;

                i = ternaryIndex[0] - 1;
                lastOps = ternaryIndex[0];

                //构建前置token
                buildToken(tokenStream, token);
                //构建前置操作token
                tokenStream.addToken(new OpsToken("?"));
                tokenStream.addToken(ternaryToken);
                continue;
            }

            if (bracket.contains(inspectChar)) {
                //构建token
                if (inspectChar == '(') {
                    int index = preNonempty(chars, i);
                    if (index == -1 || operationSet.contains(chars[index])) {
                        if (StringUtils.hasText(token)) {
                            ExpressionSyntaxException.thr(expression, token + "@(");
                        }
                        tokenStream.addBracket(true);
                    } else {
                        expressionLeftBracket++;
                        continue;
                    }
                }
                if (inspectChar == ')') {
                    if (expressionLeftBracket > 0) {
                        expressionLeftBracket--;
                        continue;
                    }
                    buildToken(tokenStream, token);
                    tokenStream.addBracket(false);
                }
                lastOps = i + 1;
                continue;
            }

            if (expressionLeftBracket > 0) continue;

            //如果是双操作符，合并双操作符
            if (token.length() == 0 && tokenStream.mergeOps(inspectChar)) {
                lastOps = i + 1;
                continue;
            }


            //构建token
            buildToken(tokenStream, token);

            OpsToken opsToken = new OpsToken(inspectChar + "");
            tokenStream.addToken(opsToken);
            lastOps = i + 1;

        }

        if (leftBracketCount != 0) {
            ExpressionSyntaxException.thrWithMsg("[] are not paired", expression, "[", "]");
        }
        if (leftBracesCount != 0) {
            ExpressionSyntaxException.thrWithMsg("{} are not paired", expression, "{", "}");
        }

        //build last
        String token = expression.substring(lastOps, length).trim();
        buildToken(tokenStream, token);


    }


    //a==b? b==c?1:b==c?1:2 : b==c?1:2
    private static TernaryToken buildTernary(String expression, int[] i) {
        int nestCount = 0;
        int leftBracket = 0;
        int divide = -1;
        int end = -1;
        char[] chars = expression.toCharArray();
        for (int index = i[0] + 1; index < chars.length; index++) {
            char c = chars[index];
            if (c == '?') {
                nestCount++;
                continue;
            }
            if (c == ':') {
                if (nestCount != 0) {
                    nestCount--;
                    divide = -2;
                } else {
                    divide = index;
                }
            }
            if (divide < 0) continue;
            if (c == '(') {
                leftBracket++;
                continue;
            }
            if (c == ')') {
                leftBracket--;
            }
            if (leftBracket < 0) {
                end = index;
                break;
            }
        }
        if (end == -1) {
            end = expression.length();
        }
        if (divide == -2) {
            ExpressionSyntaxException.thrWithMsg("illegal ternary expression", expression, "?$" + expression.substring(i[0] + 1));
        }
        if (divide == -1) {
            //没有检测到三元表达式
            return null;
        }
        TernaryToken token = new TernaryToken(expression.substring(i[0] + 1, end));
        try {
            token.setTrueTokenStream(doGetTokenStream(expression.substring(i[0] + 1, divide)));
            token.setFalseTokenStream(doGetTokenStream(expression.substring(divide + 1, end)));
        } catch (ExpressionSyntaxException e) {
            new ExpressionSyntaxException(expression)
                    .buildErrorStr(e.getSyntaxErrStr().toArray(new String[0]))
                    .buildToken(e.getLayer().toArray(new Token[0]))
                    .buildMsg("sub expression err")
                    .buildCause(e)
                    .buildConsoleErrorMsg()
                    .thr();
        }
        i[0] = end;
        return token;
    }


    private static void buildToken(TokenStream tokenStream, String token) {
        if (StringUtils.isEmpty(token)) return;
        Builder.ExceptionMsgHolder holder = new Builder.ExceptionMsgHolder();
        Class<?> errBuilder = Object.class;
        err:
        try {
            boolean success = false;
            for (Builder builder : builderSet) {
                success = builder.build(tokenStream, token, holder);
                if (success) {
                    break;
                }
                if (holder.isErr) {
                    errBuilder = builder.getClass();
                    break err;
                }
            }
            if (!success) {
                //遍历完所有处理器 都不能处理
                holder.setMsg("unrecognized token!");
                break err;
            }
            return;
        } catch (ExpressionSyntaxException e) {
            new ExpressionSyntaxException(tokenStream.getExpression())
                    .buildErrorStr(e.getSyntaxErrStr().toArray(new String[0]))
                    .buildToken(e.getLayer().toArray(new Token[0]))
                    .buildMsg("sub expression err")
                    .buildCause(e)
                    .buildConsoleErrorMsg()
                    .thr();
        } catch (Exception e) {
            new ExpressionSyntaxException(tokenStream.getExpression())
                    .buildErrorStr(token)
                    .buildCause(e)
                    .buildConsoleErrorMsg()
                    .buildStackTrace()
                    .thr();
        }
        if (holder.isErr) {
            ExpressionSyntaxException.tokenThr(errBuilder.getSimpleName() + " throw a exception ->" + holder.msg, tokenStream.getExpression(), new ErrMsgToken(token).errStr(holder.getErrPart()));
        }
    }

    private static int nextNonempty(char[] charArray, int cursor) {
        cursor++;
        for (; cursor < charArray.length; cursor++) {
            if (charArray[cursor] != ' ') return cursor;
        }
        return -1;
    }

    private static int preNonempty(char[] charArray, int cursor) {
        cursor--;
        for (; cursor >= 0; cursor--) {
            if (charArray[cursor] != ' ') return cursor;
        }
        return -1;
    }


}
