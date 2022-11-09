package com.freedy.expression.exception;

import com.freedy.expression.SysConstant;
import com.freedy.expression.token.ExecutableToken;
import com.freedy.expression.utils.Color;
import com.freedy.expression.utils.PlaceholderParser;
import com.freedy.expression.utils.StringUtils;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.InaccessibleObjectException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将FunBaseException包装成更加可视化的异常信息
 * @author Freedy
 * @date 2021/12/15 9:49
 */
public class FunRuntimeException extends RuntimeException {

    public static void buildThr(Class<?> builderClass, String msg, String expression, StackTraceElement[] ele, ExecutableToken... tokens) {
        new FunRuntimeException(expression)
                .buildMsg(msg)
                .buildToken(tokens)
                .buildCause(new BuildFailException(ele, builderClass == null ? "no builder detected" : builderClass.getSimpleName() + ":build failed!"))
                .buildConsoleErrorMsg()
                .buildStackTrace()
                .thr();
    }


    public static void tokenThr(String msg, String expression, ExecutableToken... tokens) {
        new FunRuntimeException(expression)
                .buildMsg(msg)
                .buildToken(tokens)
                .buildConsoleErrorMsg()
                .buildStackTrace()
                .thr();
    }


    public static void tokenThr(String expression, ExecutableToken... tokens) {
        new FunRuntimeException(expression)
                .buildToken(tokens)
                .buildConsoleErrorMsg()
                .buildStackTrace()
                .thr();
    }


    public static void tokenThr(Throwable cause, String expression, ExecutableToken... tokens) {
        new FunRuntimeException(expression)
                .buildCause(cause)
                .buildToken(tokens)
                .buildConsoleErrorMsg()
                .buildStackTrace()
                .thr();
    }


    public static void thrEvaluateException(EvaluateException e, String expression, ExecutableToken token) {
        List<ExecutableToken> tokens = e.getTokenList();
        String sub = e.getExpression();
        new FunRuntimeException(StringUtils.hasText(sub) ? sub : expression)
                .buildCause(e)
                .buildToken(tokens.isEmpty() ? new ExecutableToken[]{token} : tokens.toArray(ExecutableToken[]::new))
                .buildConsoleErrorMsg()
                .buildStackTrace()
                .thr();
    }


    public static void thr(String expression, String... syntaxErrSubStr) {
        new FunRuntimeException(expression)
                .buildErrorStr(syntaxErrSubStr)
                .buildConsoleErrorMsg()
                .thr();
    }


    public static void thrWithMsg(String msg, String expression, String... syntaxErrSubStr) {
        new FunRuntimeException(expression)
                .buildErrorStr(syntaxErrSubStr)
                .buildMsg(msg)
                .buildConsoleErrorMsg()
                .thr();
    }

    public static void thrThis(String expression, FunRuntimeException thisException) {
        new FunRuntimeException(expression)
                .buildErrorStr(thisException.getSyntaxErrStr().toArray(String[]::new))
                .buildToken(thisException.getLayer().toArray(ExecutableToken[]::new))
                .buildMsg("sub expression err")
                .buildCause(thisException)
                .buildConsoleErrorMsg()
                .thr();
    }

    private String expression;
    private String msg;
    private Throwable cause;
    @Getter
    private final List<ExecutableToken> layer = new ArrayList<>();
    @Getter
    private final List<String> syntaxErrStr = new ArrayList<>();
    private PlaceholderParser placeholder;
    //每一层相应token对应的坐标
    private Map<ExecutableToken, int[]> currentTokenIndex = new TreeMap<>(Comparator.comparing(ExecutableToken::getOffset));

    public FunRuntimeException(String expression) {
        this.expression = expression;
    }

    public FunRuntimeException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public FunRuntimeException buildToken(ExecutableToken... tokens) {
        if (tokens == null || tokens.length == 0) {
            return this;
        }
        layer.clear();

        LinkedList<ExecutableToken> queue = new LinkedList<>(Arrays.asList(tokens));
        queue.sort(Comparator.comparingInt(ExecutableToken::getOffset));

        while (!queue.isEmpty()) {
            ExecutableToken poll = queue.poll();

            List<ExecutableToken> originToken = poll.getOriginToken();
            if (originToken != null) {
                for (ExecutableToken origin : originToken) {
                    origin.setSonToken(poll);
                    queue.add(origin);
                }
            } else {
                layer.add(poll);
            }
        }
        return this;
    }


    public FunRuntimeException buildErrorStr(String... str) {
        if (str == null) return this;
        for (String s : str) {
            if (StringUtils.hasText(s)) {
                syntaxErrStr.add(s);
            }
        }
        return this;
    }

    public FunRuntimeException clearErrorStr() {
        syntaxErrStr.clear();
        return this;
    }

    public FunRuntimeException buildMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public FunRuntimeException buildCause(Throwable cause) {
        this.cause = cause;
        return this;
    }

    public FunRuntimeException buildConsoleErrorMsg() {
        List<SyntaxErr> syntaxErrSubStr = new ArrayList<>(layer.stream().map(SyntaxErr::new).toList());
        syntaxErrSubStr.addAll((syntaxErrStr.stream().map(SyntaxErr::new).toList()));

        StringBuilder highlightExpression = new StringBuilder();
        StringBuilder underLine = new StringBuilder();
        TreeMap<Integer, int[]> map = new TreeMap<>();
        for (int[] i : findAllIndex(expression, syntaxErrSubStr)) {
            if (i == null) {
                highlightExpression.append(new PlaceholderParser("expression: ? illegal at ?*", expression, syntaxErrSubStr.stream().map(SyntaxErr::getInfo).toArray()).configPlaceholderHighLight(PlaceholderParser.PlaceholderHighLight.HIGH_LIGHT_CYAN));
                break;
            } else {
                //排序
                map.put(i[0], i);
            }
        }
        if (highlightExpression.isEmpty()) {
            int lastSplit = 0;
            for (int[] i : map.values()) {
                highlightExpression.append("\033[93m").append(expression, lastSplit, i[0]).append("\033[0;39m");
                highlightExpression.append("\033[91m").append(expression, i[0], i[1]).append("\033[0;39m");
                underLine.append(" ".repeat(i[0] - lastSplit)).append("^".repeat(i[1] - i[0]));
                lastSplit = i[1];
            }

            highlightExpression.append("\033[93m").append(expression.substring(lastSplit)).append("\033[0;39m");
        }

        placeholder = new PlaceholderParser("""
                            
                            
                \033[93m:)?\033[93m at:\033[0;39m
                    ?
                    \033[91m?\033[0;39m""", msg == null ? cause == null ? "syntax error" : cause.getMessage() == null ? cause.getClass().getName() : cause.getMessage() : msg, highlightExpression, underLine
        );
        return this;
    }

    public FunRuntimeException buildStackTrace() {
        if (placeholder == null) {
            throw new java.lang.UnsupportedOperationException("please call buildConsoleErrorMsg() first!");
        }
        Map<ExecutableToken, int[]> layer;

        while ((layer = getNextLayer()) != null) {
            currentTokenIndex = layer;
            StringBuilder builder = new StringBuilder();
            int[] lastSplit = {0};
            layer.forEach((k, v) -> {
                if (k.isType("operation")) return;
                builder.append(" ".repeat(v[0] - lastSplit[0])).append(k.getValue(), 0, v[1] - v[0]);
                lastSplit[0] = v[1];
            });
            builder.append(" ".repeat(expression.length() - lastSplit[0]));
            placeholder.join(true, """
                                                        
                            \t\033[4:95m?\033[0;39m""",
                    builder
            );
        }
        placeholder.join(true, "\n");
        return this;
    }


    @SneakyThrows
    public void thr() {
        throw new FunRuntimeException(placeholder == null ? "white blank" : placeholder.toString(), cause);
    }

    private Map<ExecutableToken, int[]> getNextLayer() {
        ExecutableToken sonToken = null;
        int startIndex = 0;
        int endIndex = 0;
        Map<ExecutableToken, int[]> resultMap = new LinkedHashMap<>();
        Map<ExecutableToken, int[]> tokenMap = new LinkedHashMap<>();
        boolean noSonToken = true;
        for (Map.Entry<ExecutableToken, int[]> entry : currentTokenIndex.entrySet()) {
            while (true) {
                if (sonToken == null) {
                    sonToken = entry.getKey().getSonToken();
                    if (sonToken == null) {
                        resultMap.put(entry.getKey(), entry.getValue());
                        break;
                    }
                    int[] i = entry.getValue();
                    startIndex = i[0];
                    endIndex = i[1];
                    tokenMap.put(entry.getKey(), entry.getValue());
                    break;
                }

                if (sonToken == entry.getKey().getSonToken()) {
                    endIndex = entry.getValue()[1];
                    tokenMap.put(entry.getKey(), entry.getValue());
                    break;
                }

                noSonToken = isAllFind(sonToken, startIndex, endIndex, resultMap, tokenMap, noSonToken);
                tokenMap.clear();
                sonToken = null;
            }
        }
        if (sonToken != null) {
            noSonToken = isAllFind(sonToken, startIndex, endIndex, resultMap, tokenMap, noSonToken);
        }
        return noSonToken ? null : resultMap;
    }

    private boolean isAllFind(ExecutableToken sonToken, int startIndex, int endIndex, Map<ExecutableToken, int[]> resultMap, Map<ExecutableToken, int[]> tokenMap, boolean noSonToken) {
        if (sonToken.getOriginToken().size() == tokenMap.size()) {
            int midIndex = (endIndex + startIndex) / 2;
            int len = sonToken.getValue().length();
            int midLen = len / 2;
            resultMap.put(sonToken, new int[]{Math.max(midIndex - midLen, startIndex), Math.min(midIndex + (len - midLen), endIndex)});
            noSonToken = false;
        } else {
            resultMap.putAll(tokenMap);
        }
        return noSonToken;
    }


    private int[][] findAllIndex(String str, List<SyntaxErr> errList) {
        List<int[]> result = new ArrayList<>();
        try {
            for (SyntaxErr err : errList) {
                //找到token的坐标   返回的结果数组的数量大于2表示匹配到多个值
                int[] subStrIndex = findSubStrIndex(str, err.info, err.startIndex);
                if (subStrIndex == null) {
                    result.add(null);
                    continue;
                }

                if (err.isTokenType()) {
                    List<String> subStrList = err.getRelevantToken().getErrStr();
                    if (subStrList != null && !subStrList.isEmpty()) {
                        for (String sub : subStrList) {
                            assert subStrIndex != null;
                            subStrIndex = findSubStrIndex(err.info,
                                    StringUtils.isSurroundByQuote(sub) ? sub.substring(1, sub.length() - 1) : sub,
                                    subStrIndex[0]);
                            result.add(subStrIndex);
                        }
                    } else {
                        result.add(subStrIndex);
                    }

                    currentTokenIndex.put(err.relevantToken, subStrIndex);
                } else {
                    result.add(subStrIndex);
                }

            }
        } catch (Exception ig) {
            ig.printStackTrace();
        }
        return result.toArray(int[][]::new);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    private int[] findSubStrIndex(String str, String subStr, int startIndex) {
        char[] chars = str.toCharArray();
        int len = chars.length;
        char[] subChars = subStr.toCharArray();
        int subLen = subChars.length;
        int behindIndex = -1;

        for (int i = startIndex; i < len; i++) {
            if (chars[i] == ' ') continue;
            int start = i;
            for (int j = 0; j < subLen; j++, i++) {
                for (; i < len && chars[i] == ' '; i++) ;
                for (; j < subLen && subChars[j] == ' '; j++) ;
                if (j == subLen) {
                    return new int[]{start, behindIndex != -1 ? behindIndex : i};
                }
                if (i == len) {
                    return null;
                }

                if (subChars[j] == '@') {
                    start = i;
                    j++;
                    if (j == subLen) {
                        return null;
                    }
                }
                if (subChars[j] == '$') {
                    behindIndex = i;
                    j++;
                    if (j == subLen) {
                        return new int[]{start, i};
                    }
                }
                if (chars[i] != subChars[j]) {
                    i = start;
                    break;
                } else {
                    if (j == subLen - 1) {
                        return new int[]{start, behindIndex != -1 ? behindIndex : i + 1};
                    }
                }
            }
        }
        return null;
    }


    @Data
    static class SyntaxErr {
        String info;
        int startIndex;
        ExecutableToken relevantToken;
        int middleIndex;

        boolean isTokenType() {
            return relevantToken != null;
        }

        public SyntaxErr(String info) {
            this.info = info;
        }

        public SyntaxErr(ExecutableToken token) {
            info = token.getValue();
            startIndex = token.getOffset();
            relevantToken = token;
        }
    }

    /*--add-opens
        java.base/java.lang=ALL-UNNAMED
    --add-opens
        java.base/java.util=ALL-UNNAMED
    --add-opens
        java.base/jdk.internal.misc=ALL-UNNAMED
    --add-opens
        java.base/jdk.internal.loader=ALL-UNNAMED
    --add-opens
        java.base/java.security=ALL-UNNAMED
    --add-opens
        java.base/java.util.stream=ALL-UNNAMED*/
    @Override
    public void printStackTrace() {
        if (SysConstant.DEV) {
            super.printStackTrace();
            return;
        }
        Throwable thr = this;
        String option = null;
        while (thr != null) {
            if (thr instanceof InaccessibleObjectException) {
                option = getOption(thr, "module (.*?) does not", "not \"opens (.*?)\" to");
            }
            if (thr instanceof IllegalAccessError) {
                option = getOption(thr, "\\(in module (.*?)\\)", "does not export (.*?) to");
            }
            thr = thr.getCause();
        }
        if (option != null) {
            System.out.println(Color.dRed("""
                    stuck due to InaccessibleObjectException,please add a vm option then restart and retry!
                    vm option -> """ + option));
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(this.getClass().getName())
                .append(":")
                .append("\n")
                .append(this.getMessage().strip())
                .append("\n")
                .append(Color.red("----------------------------------------------------------------------------------------------"))
                .append("\n")
                .append(Color.red("cause:"))
                .append("\n");
        Throwable cause = this.getCause();
        while (cause != null) {
            builder.append("\t")
                    .append(Color.red(cause.getClass().getSimpleName()))
                    .append(" -> ")
                    .append(cause.getMessage())
                    .append("\n");
            cause = cause.getCause();
        }
        System.out.println(builder);
    }

    private static String getOption(Throwable thr, String moduleReg, String packReg) {
        String option = null;
        String message = thr.getMessage();
        Matcher matcher = Pattern.compile(moduleReg).matcher(message);
        String module = null;
        if (matcher.find()) {
            module = matcher.group(1).strip();
        }
        matcher = Pattern.compile(packReg).matcher(message);
        String pack = null;
        if (matcher.find()) {
            pack = matcher.group(1).strip();
        }
        if (module != null && pack != null) {
            option = "--add-opens " + module + "/" + pack + "=ALL-UNNAMED";
        }
        return option;
    }

}
