package com.freedy.expression.tokenBuilder;

import com.freedy.expression.TokenStream;
import com.freedy.expression.exception.ExpressionSyntaxException;
import com.freedy.expression.token.ErrMsgToken;
import com.freedy.expression.token.OpsToken;
import com.freedy.expression.token.TernaryToken;
import com.freedy.expression.token.Token;
import com.freedy.expression.utils.PackageScanner;
import com.freedy.expression.utils.StringUtils;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.util.*;

import static com.freedy.expression.utils.StringUtils.splitWithoutBracket;

/**
 * 序列化器,调用{@link Tokenizer#getTokenStream(String)}方法将字符串代码序列化为一连串Token
 *
 * @author Freedy
 * @date 2021/12/14 15:28
 */
public class Tokenizer {

    //[<=>| static !+_*?()]
    private static final Set<Character> operationSet = Set.of('=', '<', '>', '|', '&', '!', '+', '-', '*', '/', '(', ')', '?', '^');
    private static final Set<Character> operationWithOutBracketSet = Set.of('=', '<', '>', '|', '&', '!', '+', '-', '*', '/', '?', '^');
    private static final Set<Character> bracket = Set.of('(', ')');
    private static final List<Builder> builderSet = new ArrayList<>();

    static {
        //扫描所有的token builder
        scanBuilder();
    }

    @SneakyThrows
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void scanBuilder() {
        for (Class aClass : PackageScanner.doScan(new String[]{"com.freedy.expression.tokenBuilder"}, null)) {
            if (Optional.ofNullable(aClass.getSuperclass()).orElse(Object.class).getName().equals("com.freedy.expression.tokenBuilder.Builder")) {
                builderSet.add((Builder) aClass.getConstructor().newInstance());
            }
        }
        builderSet.sort(Comparator.comparing(Builder::priority));
    }


    public static TokenStream getTokenStream(String expression) {
        //处理注释
        StringJoiner joiner = new StringJoiner(" ");
        for (String sub : StringUtils.splitWithoutBracket(expression, new char[0], new char[0], '\n')) {
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

        int leftBracesCount = 0;    // { }
        int leftBracketCount = 0;   // [ ]

        boolean quote = false;
        boolean bigQuote = false;
        for (int i = 0; i < length; i++) {
            char inspectChar = chars[i];

            if (!quote && inspectChar == '"') {
                bigQuote = !bigQuote;
                continue;
            }
            if (bigQuote) continue;
            if (inspectChar == '\'') {
                quote = !quote;
                continue;
            }
            if (quote) continue;


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


            if (inspectChar == ' ' || !operationSet.contains(inspectChar)) {
                continue;
            }

            String token = expression.substring(lastOps, i).trim();

            if (inspectChar == '?') {
                int index = StringUtils.nextNonempty(chars, i);
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
                    int index = StringUtils.preNonempty(chars, i);
                    if (index == -1 || (operationWithOutBracketSet.contains(chars[index]))) {
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

            if (StringUtils.isEmpty(token) && inspectChar == '-') {
                continue;
            }

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
        Class<?> errBuilder = null;
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
            ExpressionSyntaxException.buildThr(errBuilder, holder.msg, tokenStream.getExpression(), holder.getElements(), new ErrMsgToken(token).errStr(holder.getErrPart()));
        }
    }


}
