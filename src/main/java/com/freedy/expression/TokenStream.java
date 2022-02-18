package com.freedy.expression;

import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.exception.ExpressionSyntaxException;
import com.freedy.expression.stander.StanderTokenBlockSorter;
import com.freedy.expression.token.Assignable;
import com.freedy.expression.token.ObjectToken;
import com.freedy.expression.token.OpsToken;
import com.freedy.expression.token.Token;
import com.freedy.expression.utils.StringUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * TokenStream是字符串代码的抽象,也就是TokenStream配合Context就可以让代码执行起来 <br/>
 *
 * <h2>构建</h2>
 * <p>TokenStream是由Tokenizer的getTokenStream()方法所构建.其利用分号进行分割,每个分割的单元为一个TokenList</p>
 * 即下面的{@link TokenStream#infixExpression}全局变量,每构建好一个TokenList就会调用{@link TokenStream#splitStream()}方法进行切换(将{@link TokenStream#infixExpression}放入到{@link TokenStream#blockStream}中)
 * <h3>执行</h3>
 * 执行的时候又会将{@link TokenStream#blockStream}里面的内容一个一个的切换回{@link TokenStream#infixExpression}中,然后对对每个token设置context并将TokenList转化为后缀表达式然后交给{@link Expression}执行
 * @author Freedy
 * @date 2021/12/14 19:46
 */
public class TokenStream implements Executable {
    @Getter
    protected List<Token> infixExpression = new ArrayList<>();
    private static final Set<String> doubleOps = Set.of("||", "&&", "!=", "==", ">=", "<=", "++", "--", "+=", "-=", "/=", "*=");
    private static final Set<String> permitOps = Set.of("=!", "||!", "&&!", "==!", ">++", "<++", "++<", "++>", "=++", "++=", ">--", "<--", "-->", "--<", "=--", "--=", "+-", "-+", "++-", "--+");
    private static final Set<String> single2TokenOps = Set.of("+++", "---");
    private static final Set<String> singleOps = Set.of("++", "--", "!");
    private static final List<Set<String>> priorityOps = Arrays.asList(
            Set.of("=", "||", "&&"),
            Set.of("?"),
            Set.of(">", "<", ">=", "<=", "==", "!="),
            Set.of("+", "-", "+=", "-="),
            Set.of("*", "/"),
            Set.of(".")
    );//从上往下 优先级逐渐变大
    @Getter
    protected final String expression;
    @Setter
    private Function<List<List<Token>>, List<List<Token>>> sorter = new StanderTokenBlockSorter();
    private EvaluationContext context;


    // b<a=2+3+(5*4/2)
    // ba2=
    // <+
    public TokenStream(String expression) {
        this.expression = expression;
    }

    /**
     * 执行时遍历该list
     */
    private List<List<Token>> blockStream = new ArrayList<>();
    @Getter
    private final List<String> defTokenList = new ArrayList<>();
    private boolean hasSort = false;

    public void splitStream() {
        blockStream.add(infixExpression);
        infixExpression = new ArrayList<>();
        if (hasSort){
            throw new IllegalArgumentException("token stream has fixed,could not split stream");
        }
    }

    /**
     * 遍历所有token并交由indexSuffixList执行
     */
    public void forEachStream(EvaluationContext context, BiConsumer<Integer, List<Token>> indexSuffixList) {
        if (this.context != context) {
            this.context = context;
            //注册销毁
            if (StringUtils.hasText(System.getProperty("cleanMode"))) {
                context.registerClean(this, defTokenList);
            }
        }
        int size = blockStream.size();
        if (!hasSort) {
            //对blockStream 进行排序
            blockStream = sorter.apply(this.blockStream);
            hasSort = true;
        }
        for (int i = 0; i < size; i++) {
            infixExpression = blockStream.get(i);
            setTokenContext(context, infixExpression);
            List<Token> suffix = calculateSuffix();
            indexSuffixList.accept(i, suffix);
        }
    }


    public int blockSize() {
        return blockStream.size();
    }

    public List<Token> getAllTokens() {
        return blockStream.stream().flatMap(Collection::stream).toList();
    }

    public static int opsPriority(String ops) {
        for (int i = 0; i < priorityOps.size(); i++) {
            if (priorityOps.get(i).contains(ops)) {
                return i;
            }
        }
        return -1;
    }



    /**
     * 合并双元操作
     */
    public boolean mergeOps(char currentOps) {
        int size = infixExpression.size();
        if (size == 0) return false;
        Token token = infixExpression.get(size - 1);
        if (token.isType("operation") && !token.isAnyValue("(", ")")) {
            String nOps = token.getValue() + currentOps;
            if (doubleOps.contains(nOps)) {
                token.setValue(nOps);
            } else {
                //区分a++ + 5
                if (single2TokenOps.contains(nOps)) {
                    //+++    ---
                    Token preToken = infixExpression.get(size - 2);
                    if (preToken.isType("operation")) {
                        // a ++ +++  --->  a ++ + ++
                        token.setValue(nOps.substring(0, 1));
                        infixExpression.add(new OpsToken(nOps.substring(1, 3)));
                    } else if (preToken instanceof Assignable) {
                        // a +++ --->  a ++ +
                        infixExpression.add(new OpsToken(nOps.substring(2, 3)));
                    } else {
                        // a +++ --->  a + ++
                        infixExpression.add(new OpsToken(nOps.substring(2, 3)));
                    }
                    //这里不会出现++- --+ +-- -++的情况
                    return true;
                }
                if (!permitOps.contains(nOps)) {
                    ExpressionSyntaxException.thr(expression, nOps);
                    return true;
                }
                infixExpression.add(new OpsToken(currentOps + ""));
            }
            return true;
        }
        return false;
    }


    int bracketsPares = 0;

    public void addBracket(boolean isLeft) {
        if (isLeft) {
            infixExpression.add(new OpsToken("("));
            bracketsPares++;
        } else {
            if (bracketsPares == 0) {
                ExpressionSyntaxException.thrWithMsg("() are not paired!", expression, (infixExpression.size() == 0 ? "" : infixExpression.get(infixExpression.size() - 1).getValue()) + "@)");
            }
            infixExpression.add(new OpsToken(")"));
            bracketsPares--;
        }
    }

    public void addToken(Token token) {
        if (token.isType("obj")) {
            defTokenList.add(((ObjectToken) token).getVariableName());
        }
        if (hasSort){
            throw new IllegalArgumentException("token stream has fixed,could not add element");
        }
        infixExpression.add(token);
    }

    /**
     * 对所有的Token设置context
     */
    private void setTokenContext(EvaluationContext context, List<Token> tokenList) {
        for (Token token : tokenList) {
            List<Token> originToken = token.getOriginToken();
            if (originToken != null) {
                setTokenContext(context, originToken);
            }
            token.setContext(context);
        }
    }

    public Token getLastToken() {
        return infixExpression.size() == 0 ? null : infixExpression.get(infixExpression.size() - 1);
    }

    private final Map<List<Token>, List<Token>> suffixCache = new HashMap<>();


    /**
     * 计算后缀表达式
     */
    private List<Token> calculateSuffix() {
        List<Token> suffix = suffixCache.get(infixExpression);
        if (suffix != null) return suffix;
        //合并单值操作
        mergeSingleTokenOps();
        //计算偏移量
        calculateOffset(0, infixExpression);
        List<Token> suffixExpression = new ArrayList<>();
        Stack<Token> opsStack = new Stack<>();
        //扫描中缀
        for (Token token : infixExpression) {
            if (token.isType("operation")) {
                Token pop;
                if (token.isValue(")")) {
                    try {
                        while (!(pop = opsStack.pop()).isValue("(")) {
                            suffixExpression.add(pop);
                        }
                        continue;
                    } catch (EmptyStackException e) {
                        ExpressionSyntaxException.tokenThr("brackets are not paired!", expression, token);
                    }
                }
                while (true) {
                    pop = opsStack.isEmpty() ? null : opsStack.peek();
                    if (pop != null && !token.isValue("(") && opsPriority(pop.getValue()) >= opsPriority(token.getValue())) {
                        //opsStack中的优先级较大 --> 压不住要跳出来
                        suffixExpression.add(opsStack.pop());
                    } else {
                        opsStack.add(token);
                        break;
                    }
                }
            } else {
                suffixExpression.add(token);
            }
        }
        while (!opsStack.isEmpty()) {
            suffixExpression.add(opsStack.pop());
        }
        suffixCache.put(infixExpression, suffixExpression);
        return suffixExpression;
    }

    /**
     * 合并单操作符,将 !var,var++等合并为一个token
     */
    private void mergeSingleTokenOps() {
        for (int i = 0; i < infixExpression.size(); i++) {
            Token token = infixExpression.get(i);
            String ops = token.getValue();
            try {
                if (token.isType("operation") && singleOps.contains(ops)) {
                    if ("!".equals(ops)) {
                        if (i + 1 >= infixExpression.size()) {
                            ExpressionSyntaxException.tokenThr(expression, token);
                        }
                        Token nextToken = infixExpression.get(i + 1);
                        if (nextToken.isType("operation")) {
                            ExpressionSyntaxException.tokenThr(expression, token, nextToken);
                        }
                        nextToken.setNotFlag(true);
                        infixExpression.remove(i);
                    } else {
                        Token preToken = null;
                        if (i - 1 >= 0) {
                            preToken = infixExpression.get(i - 1);
                        }
                        Token nextToken = null;
                        if (i + 1 < infixExpression.size()) {
                            nextToken = infixExpression.get(i + 1);
                        }
                        if (preToken == null || preToken.isType("operation")) {
                            if (nextToken == null) {
                                ExpressionSyntaxException.tokenThr(expression, token);
                            }
                            assert nextToken != null;
                            nextToken.setOriginToken(token, nextToken);
                            if (ops.equals("++")) {
                                nextToken.setPreSelfAddFlag(true);
                            } else
                                nextToken.setPreSelfSubFlag(true);
                            infixExpression.remove(i);
                            continue;
                        }
                        if (nextToken == null || nextToken.isType("operation")) {
                            preToken.setOriginToken(preToken, token);
                            if (ops.equals("++"))
                                preToken.setPostSelfAddFlag(true);
                            else
                                preToken.setPostSelfSubFlag(true);
                            infixExpression.remove(i);
                        }
                    }
                }
            } catch (EvaluateException e) {
                ExpressionSyntaxException.thrEvaluateException(e, expression, token);
            } catch (Exception e) {
                ExpressionSyntaxException.tokenThr(e, expression, token);
            }
        }

    }

    /**
     * 计算每个token在expression中的偏移量,方便异常时进行彩色标记
     */
    private void calculateOffset(int cursor, List<Token> tokenList) {
        for (Token token : tokenList) {
            List<Token> originToken = token.getOriginToken();
            if (originToken != null) {
                calculateOffset(cursor, originToken);
            }
            int[] index = findSubStrIndex(expression, token.getValue(), cursor);
            assert index != null;
            token.setOffset(index[0]);
            cursor = index[1];
        }
    }

    /**
     * 计算偏移
     */
    @SuppressWarnings("StatementWithEmptyBody")
    private int[] findSubStrIndex(String str, String subStr, int startIndex) {
        char[] chars = str.toCharArray();
        int len = chars.length;
        char[] subChars = subStr.toCharArray();
        int subLen = subChars.length;
        for (int i = startIndex; i < len; i++) {
            if (chars[i] == ' ') continue;
            int start = i;
            for (int j = 0; ; j++, i++) {
                for (; i < len && chars[i] == ' '; i++) ;
                for (; j < subLen && subChars[j] == ' '; j++) ;
                if (i == len && j == subLen) {
                    return new int[]{start, i};
                } else if (i == len || j == subLen) {
                    break;
                }
                if (chars[i] != subChars[j]) {
                    break;
                } else {
                    if (j == subLen - 1) {
                        return new int[]{start, i + 1};
                    }
                }
            }

        }
        return null;
    }

    /**
     * 变量清除,可以理解为回收该tokenStream中def定义的变量
     */
    public void close() {
        if (context == null) return;
        for (String varName : defTokenList) {
            context.removeVariable(varName);
        }
    }
}
