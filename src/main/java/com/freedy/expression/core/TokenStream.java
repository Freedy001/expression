package com.freedy.expression.core;

import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.exception.FunRuntimeException;
import com.freedy.expression.token.*;
import com.freedy.expression.utils.StringUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * TokenStream是字符串代码的抽象,也就是TokenStream需要配合Context才可以让代码执行起来。 <br/>
 *
 * <h2>构建</h2>
 * <p>TokenStream是由Tokenizer的getTokenStream()方法所构建.其利用分号进行分割,每个分割的单元为一个TokenList</p>
 * 即下面的{@link TokenStream#infixExpression}全局变量,每构建好一个TokenList就会调用{@link TokenStream#splitStream()}方法进行切换(将{@link TokenStream#infixExpression}放入到{@link TokenStream#blockStream}中),
 * {@link TokenStream#blockStream}代表一个完整的代码段。
 * <h2>执行</h2>
 * 执行的时候又会将{@link TokenStream#blockStream}里面的内容一个一个的切换回{@link TokenStream#infixExpression}中,然后对对每个token设置context并将TokenList转化为后缀表达式然后交给{@link Expression}来执行。
 *
 * @author Freedy
 * @date 2021/12/14 19:46
 */
public class TokenStream {
    private static final Set<String> doubleOps = Set.of("||", "&&", "!=", "==", ">=", "<=", "++", "--", "+=", "-=", "/=", "*=", "^=", "&=", "|=", "<<", ">>", ">>>");
    private static final Set<String> permitOps = Set.of("=!", "||!", "&&!", "==!", ">++", "<++", "++<", "++>", "=++", "++=", ">--", "<--", "-->", "--<", "=--", "--=", "+-", "-+", "++-", "--+");
    private static final Set<String> single2TokenOps = Set.of("+++", "---");
    private static final Set<String> singleOps = Set.of("++", "--", "!");
    private static final List<Set<String>> priorityOps = Arrays.asList(
            Set.of("=", "||", "&&"),
            Set.of("?"),
            Set.of(">", "<", ">=", "<=", "==", "!="),
            Set.of("+", "-", "+=", "-=", ">>", ">>>", "<<", "|", "&", "^", "|=", "^=", "&="),
            Set.of("*", "/"),
            Set.of(".")
    );//从上往下 优先级逐渐变大

    @Getter
    private List<Token> infixExpression = new ArrayList<>();
    /**
     * 执行时遍历该list
     */
    private List<List<Token>> blockStream = new ArrayList<>();
    @Getter
    private final String expression;

    @Setter
    private Function<List<List<Token>>, List<List<Token>>> sorter = lists->{
        List<List<Token>> funcList = new ArrayList<>();
        List<List<Token>> normList = new ArrayList<>();
        for (List<Token> stream : lists) {
            if ((stream.size() == 1 && stream.get(0) instanceof DefToken def &&
                    def != null && def.isFunc()) ||
                    (stream.size() == 1 && stream.get(0) instanceof DirectAccessToken dir &&
                            dir != null && "func".equals(dir.getMethodName()))) {
                funcList.add(stream);
            } else {
                normList.add(stream);
            }
        }
        funcList.addAll(normList);
        return funcList;
    };
    private boolean hasSort = false;
    private EvaluationContext context;

    //可以在fun运行时调用 T(com.freedy.expression.core.TokenStream).cleanMode=true 改变cleanMode
    @SuppressWarnings("FieldMayBeFinal")
    private static boolean cleanMode = StringUtils.hasText(System.getProperty("cleanMode"));
    @Getter
    private final List<String> defTokenList = new ArrayList<>();
    private int bracketsPares = 0;
    @Getter
    @Setter
    private Object metadata;

    public TokenStream(String expression) {
        this.expression = expression;
    }


    public void addBracket(boolean isLeft) {
        if (hasSort) {
            throw new IllegalArgumentException("token stream has fixed,could not add element");
        }
        if (isLeft) {
            infixExpression.add(new OpsToken("("));
            bracketsPares++;
        } else {
            if (bracketsPares == 0) {
                FunRuntimeException.thrWithMsg("() are not paired!", expression, (infixExpression.size() == 0 ? "" : infixExpression.get(infixExpression.size() - 1).getValue()) + "@)");
            }
            infixExpression.add(new OpsToken(")"));
            bracketsPares--;
        }
    }

    public void addToken(Token token) {
        if (token.isType("def")) {
            defTokenList.add(((DefToken) token).getVariableName());
        }
        if (hasSort) {
            throw new IllegalArgumentException("token stream has fixed,could not add element");
        }
        infixExpression.add(token);
    }

    /**
     * 切割,每当一个中缀表达式构建完毕后调用此方法来构建下一条中缀表达式
     */
    public void splitStream() {
        blockStream.add(infixExpression);
        infixExpression = new ArrayList<>();
        if (hasSort) {
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
            if (cleanMode) {
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

    /**
     * 获取该tokenStream中有多少个infixExpression
     */
    public int blockSize() {
        return blockStream.size();
    }

    /**
     * 获取所有token，会将blockStream扁平化输出
     */
    public List<Token> getAllTokens() {
        return blockStream.stream().flatMap(Collection::stream).toList();
    }

    private static int opsPriority(String ops) {
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
                if (nOps.length() == 2 && Tokenizer.operationWithOutBracketSet.contains(nOps.charAt(0)) && nOps.charAt(1) == '-') {
                    return false;
                }
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
                    FunRuntimeException.thr(expression, nOps);
                    return true;
                }
                infixExpression.add(new OpsToken(currentOps + ""));
            }
            return true;
        }
        return false;
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
        calculateOffset(infixExpression);
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
                        FunRuntimeException.tokenThr("brackets are not paired!", expression, token);
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
                            FunRuntimeException.tokenThr(expression, token);
                        }
                        Token nextToken = infixExpression.get(i + 1);
                        if (nextToken.isType("operation")) {
                            if (nextToken.isValue("(")) { // 非操作被括号包围
                                int leftBreaker = 1;
                                int j = i + 2;
                                StringBuilder subExp = new StringBuilder("(");
                                for (; j < infixExpression.size() && leftBreaker != 0; j++) {
                                    Token inner = infixExpression.get(j);
                                    if (inner.isValue("(")) leftBreaker++;
                                    if (inner.isValue(")")) leftBreaker--;
                                    subExp.append(inner.getValue());
                                }
                                if (leftBreaker != 0) {
                                    FunRuntimeException.tokenThr("brackets are not paired!", expression, nextToken);
                                }
                                TokenStream stream = new TokenStream(subExp.toString());
                                StreamWrapperToken wrapperToken = new StreamWrapperToken(stream);
                                for (int k = i + 1; k < j; k++) {
                                    stream.infixExpression.add(infixExpression.remove(i + 1));
                                    stream.blockStream.add(stream.infixExpression);
                                }
                                wrapperToken.setContext(context);
                                wrapperToken.setNotFlag(true);
                                infixExpression.set(i, wrapperToken);
                                continue;
                            } else {  //其他操作 直接报错
                                FunRuntimeException.tokenThr(expression, token, nextToken);
                            }
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
                                FunRuntimeException.tokenThr(expression, token);
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
                            //// TODO: 2022/6/7
                            infixExpression.remove(i);
                        }
                    }
                }
            } catch (EvaluateException e) {
                FunRuntimeException.thrEvaluateException(e, expression, token);
            } catch (Exception e) {
                FunRuntimeException.tokenThr(e, expression, token);
            }
        }

    }

    private int currentCursor;

    /**
     * 计算每个token在expression中的偏移量,方便异常时进行彩色标记
     */
    private void calculateOffset(List<Token> tokenList) {
        for (Token token : tokenList) {
            List<Token> originToken = token.getOriginToken();
            if (originToken != null) {
                int lastCursor = currentCursor;
                calculateOffset(originToken);
                currentCursor = lastCursor;
            }
            int[] index = findSubStrIndex(expression, token.getValue(), currentCursor);
            if (index == null) { // 没找到token位置可能因为方法代码被自动被抽取到代码段前方导致在当前坐标下无法找到相应token的便宜
                index = findSubStrIndex(expression, token.getValue(), 0);
                assert index != null;  // 如果这里为空 是一个严重的bug
                currentCursor=Math.max(currentCursor,index[1]);
            } else {
                currentCursor = index[1];
            }
            token.setOffset(index[0]);
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
