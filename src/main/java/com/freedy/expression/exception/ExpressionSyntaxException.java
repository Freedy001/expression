package com.freedy.expression.exception;

import com.freedy.expression.token.Token;
import com.freedy.expression.utils.PlaceholderParser;
import com.freedy.expression.utils.StringUtils;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author Freedy
 * @date 2021/12/15 9:49
 */
public class ExpressionSyntaxException extends RuntimeException {

    public static void buildThr(Class<?> builderClass, String msg, String expression, StackTraceElement[] ele, Token... tokens) {
        new ExpressionSyntaxException(expression)
                .buildMsg(msg)
                .buildToken(tokens)
                .buildCause(new BuildFailException(ele, builderClass == null ? "no builder detected" : builderClass.getSimpleName() + ":build failed!"))
                .buildConsoleErrorMsg()
                .buildStackTrace()
                .thr();
    }


    public static void tokenThr(String msg, String expression, Token... tokens) {
        new ExpressionSyntaxException(expression)
                .buildMsg(msg)
                .buildToken(tokens)
                .buildConsoleErrorMsg()
                .buildStackTrace()
                .thr();
    }


    public static void tokenThr(String expression, Token... tokens) {
        new ExpressionSyntaxException(expression)
                .buildToken(tokens)
                .buildConsoleErrorMsg()
                .buildStackTrace()
                .thr();
    }


    public static void tokenThr(Throwable cause, String expression, Token... tokens) {
        new ExpressionSyntaxException(expression)
                .buildCause(cause)
                .buildToken(tokens)
                .buildConsoleErrorMsg()
                .buildStackTrace()
                .thr();
    }


    public static void thrEvaluateException(EvaluateException e, String expression, Token token) {
        List<Token> tokens = e.getTokenList();
        String sub = e.getExpression();
        new ExpressionSyntaxException(StringUtils.hasText(sub) ? sub : expression)
                .buildCause(e)
                .buildToken(tokens.isEmpty() ? new Token[]{token} : tokens.toArray(Token[]::new))
                .buildConsoleErrorMsg()
                .buildStackTrace()
                .thr();
    }


    public static void thr(String expression, String... syntaxErrSubStr) {
        new ExpressionSyntaxException(expression)
                .buildErrorStr(syntaxErrSubStr)
                .buildConsoleErrorMsg()
                .thr();
    }


    public static void thrWithMsg(String msg, String expression, String... syntaxErrSubStr) {
        new ExpressionSyntaxException(expression)
                .buildErrorStr(syntaxErrSubStr)
                .buildMsg(msg)
                .buildConsoleErrorMsg()
                .thr();
    }

    public static void thrThis(String expression, ExpressionSyntaxException thisException) {
        new ExpressionSyntaxException(expression)
                .buildErrorStr(thisException.getSyntaxErrStr().toArray(String[]::new))
                .buildToken(thisException.getLayer().toArray(Token[]::new))
                .buildMsg("sub expression err")
                .buildCause(thisException)
                .buildConsoleErrorMsg()
                .thr();
    }

    private final String expression;
    private String msg;
    private Throwable cause;
    @Getter
    private final List<Token> layer = new ArrayList<>();
    @Getter
    private final List<String> syntaxErrStr = new ArrayList<>();
    private PlaceholderParser placeholder;
    //???????????????token???????????????
    private Map<Token, int[]> currentTokenIndex = new TreeMap<>(Comparator.comparing(Token::getOffset));

    public ExpressionSyntaxException(String expression) {
        this.expression = expression;
    }

    public ExpressionSyntaxException buildToken(Token... tokens) {
        if (tokens == null || tokens.length == 0) {
            return this;
        }
        layer.clear();

        LinkedList<Token> queue = new LinkedList<>(Arrays.asList(tokens));
        queue.sort(Comparator.comparingInt(Token::getOffset));

        while (!queue.isEmpty()) {
            Token poll = queue.poll();

            List<Token> originToken = poll.getOriginToken();
            if (originToken != null) {
                for (Token origin : originToken) {
                    origin.setSonToken(poll);
                    queue.add(origin);
                }
            } else {
                layer.add(poll);
            }
        }
        return this;
    }


    public ExpressionSyntaxException buildErrorStr(String... str) {
        if (str == null) return this;
        for (String s : str) {
            if (StringUtils.hasText(s)) {
                syntaxErrStr.add(s);
            }
        }
        return this;
    }

    public ExpressionSyntaxException clearErrorStr() {
        syntaxErrStr.clear();
        return this;
    }

    public ExpressionSyntaxException buildMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public ExpressionSyntaxException buildCause(Throwable cause) {
        this.cause = cause;
        return this;
    }

    public ExpressionSyntaxException buildConsoleErrorMsg() {
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
                //??????
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

    public ExpressionSyntaxException buildStackTrace() {
        if (placeholder == null) {
            throw new java.lang.UnsupportedOperationException("please call buildConsoleErrorMsg() first!");
        }
        Map<Token, int[]> layer;

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
        Class<Throwable> aClass = Throwable.class;
        //??????msg
        Field exceptionMsg;
        exceptionMsg = aClass.getDeclaredField("detailMessage");
        exceptionMsg.setAccessible(true);
        exceptionMsg.set(this, placeholder == null ? "white blank" : placeholder.toString());
        if (cause != null) {
            Field causeField = aClass.getDeclaredField("cause");
            causeField.setAccessible(true);
            causeField.set(this, cause);
        }
        throw this;
    }

    private Map<Token, int[]> getNextLayer() {
        Token sonToken = null;
        int startIndex = 0;
        int endIndex = 0;
        Map<Token, int[]> resultMap = new LinkedHashMap<>();
        Map<Token, int[]> tokenMap = new LinkedHashMap<>();
        boolean noSonToken = true;
        for (Map.Entry<Token, int[]> entry : currentTokenIndex.entrySet()) {
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

    private boolean isAllFind(Token sonToken, int startIndex, int endIndex, Map<Token, int[]> resultMap, Map<Token, int[]> tokenMap, boolean noSonToken) {
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
        for (SyntaxErr err : errList) {
            //??????token?????????   ????????????????????????????????????2????????????????????????
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
                    i=start;
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
        Token relevantToken;
        int middleIndex;

        boolean isTokenType() {
            return relevantToken != null;
        }

        public SyntaxErr(String info) {
            this.info = info;
        }

        public SyntaxErr(Token token) {
            info = token.getValue();
            startIndex = token.getOffset();
            relevantToken = token;
        }
    }


}
