package com.freedy.expression.core;

import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.exception.ExpressionSyntaxException;
import com.freedy.expression.token.*;
import com.freedy.expression.utils.ReflectionUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static com.freedy.expression.token.Token.ANY_TYPE;

/**
 * 执行器,在{@link Expression#defaultContext}环境中调用{@link Expression#getValue()}执行{@link TokenStream}
 *
 * @author Freedy
 * @date 2021/12/14 11:18
 */
@NoArgsConstructor
public class Expression {
    private String expression;
    private TokenStream stream;
    @Getter
    @Setter
    private EvaluationContext defaultContext;

    public Expression(String expression) {
        this.stream = Tokenizer.getTokenStream(expression);
        this.expression = expression;
    }

    public Expression(String expression, EvaluationContext defaultContext) {
        this(expression);
        this.defaultContext = defaultContext;
    }

    public Expression(TokenStream stream) {
        this.stream = stream;
        this.expression = stream.getExpression();
    }


    public Expression(TokenStream stream, EvaluationContext defaultContext) {
        this.stream = stream;
        this.expression = stream.getExpression();
        this.defaultContext = defaultContext;
    }

    public Expression(EvaluationContext defaultContext) {
        this.defaultContext = defaultContext;
    }

    public Expression setTokenStream(TokenStream stream) {
        this.stream = stream;
        this.expression = stream.getExpression();
        return this;
    }

    public Object getValue() {
        return evaluate(ANY_TYPE, defaultContext);
    }

    public <T> T getValue(Class<T> desiredResultType) {
        return desiredResultType.cast(evaluate(desiredResultType, defaultContext));
    }

    public Object getValue(EvaluationContext context) {
        return evaluate(ANY_TYPE, context);
    }

    public <T> T getValue(EvaluationContext context, Class<T> desiredResultType) {
        return desiredResultType.cast(evaluate(desiredResultType, context));
    }


    private Object evaluate(Class<?> desired, EvaluationContext context) {
        if (stream == null) throw new IllegalArgumentException("please set a tokenStream");
        if (context == null)
            throw new IllegalArgumentException("please set a context or call getValue method with context param");
        int size = stream.blockSize();
        Object[] result = new Object[1];
        stream.forEachStream(context, (i, suffixList) -> {
            result[0] = doEvaluate(suffixList, i == size - 1 ? desired : ANY_TYPE);
        });
        return result[0];
    }

    private Object doEvaluate(List<Token> suffixTokenList, Class<?> desired) {
        Stack<Token> varStack = new Stack<>();
        List<Token> list = new ArrayList<>();
        for (Token token : suffixTokenList) {
            try {
                if (token.isType("operation")) {
                    list.add(token);
                    Token token1 = varStack.pop();
                    Token token2 = varStack.pop();
                    varStack.push(calculate(token, token2, token1));
                    continue;
                }
                varStack.push(token);
            } catch (ExpressionSyntaxException e) {
                e.clearErrorStr().buildToken(token);
                ExpressionSyntaxException.thrThis(expression, e);
            } catch (EvaluateException e) {
                ExpressionSyntaxException.thrEvaluateException(e, expression, token);
            } catch (Throwable e) {
                ExpressionSyntaxException.tokenThr(e, expression, token);
            }
        }
        if (varStack.size() == 1) {
            Token token = varStack.pop();
            Object result = null;
            try {
                result = token.calculateResult(desired);
            } catch (ExpressionSyntaxException e) {
                e.clearErrorStr().buildToken(token);
                ExpressionSyntaxException.thrThis(expression, e);
            } catch (EvaluateException e) {
                ExpressionSyntaxException.thrEvaluateException(e, expression, token);
            } catch (Throwable e) {
                ExpressionSyntaxException.tokenThr(e, expression, token);
            }
            return result;
        }
        if (varStack.size() == 0) return null;
        ExpressionSyntaxException.tokenThr(expression, list.toArray(Token[]::new));
        throw new IllegalArgumentException("unreachable statement");
    }


    private Token calculate(Token opsToken, Token t1, Token t2) {
        switch (opsToken.getValue()) {
            case "." -> {
                return mergeDotSplit(t1, t2, opsToken);
            }
            case "?" -> {
                return ternaryOps(t1, t2, opsToken);
            }
            case "=" -> {
                return assign(t1, t2, opsToken);
            }
            case "||", "&&" -> {
                return logicOps(t1, t2, opsToken);
            }
            //比较运输
            case "<", "==", ">=", "<=", ">", "!=" -> {
                return compare(t1, t2, opsToken);
            }
            //数字原始
            case "+", "/=", "*=", "-=", "+=", "/", "*", "-", "<<", ">>", ">>>", "^", "|", "&", "|=", "&=", "^=" -> {
                return numOps(t1, t2, opsToken);
            }
            case "--" -> {
                //弥补--token识别问题
                t2.setValue("-" + t2.getValue());
                return numOps(t1, t2, new OpsToken("-"));
            }
            default -> throw new EvaluateException("unrecognized ops ?", opsToken.getValue());
        }
    }

    private Token ternaryOps(Token t1, Token t2, Token opsToken) {
        if (t2 instanceof TernaryToken token) {
            token.setBoolToken(t1);
            return t2;
        }
        throw new EvaluateException("can not do ternary ops,because ? is not ternary token", t2.getValue()).errToken(opsToken).errToken(t2);

    }

    private Token mergeDotSplit(Token t1, Token t2, Token opsToken) {
        if (t2 instanceof DotSplitToken dotSplitToken) {
            dotSplitToken.setBaseToken(t1);
            return dotSplitToken;
        }
        throw new EvaluateException("can not merge dot split token,because ? is not dot split token", t2.getValue()).errToken(opsToken).errToken(t2);
    }


    private Token assign(Token t1, Token t2, Token opsToken) {
        if (t1 instanceof Assignable token) {
            try {
                token.assignFrom(t2);
            } catch (Exception e) {
                throw new EvaluateException("assign failed!", e).errToken(opsToken);
            }
            return t1;
        } else {
            throw new EvaluateException("illegal assign").errToken(t1, opsToken, t2);
        }
    }

    private Token logicOps(Token t1, Token t2, Token ops) {
        return new BasicVarToken("bool", t1.logicOps(t2, ops) + "").setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
    }


    private Token compare(Token t1, Token t2, Token ops) {
        switch (ops.getValue()) {
            case "<" -> {
                return new BasicVarToken("bool", (t1.compareTo(t2) < 0) + "").setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
            }
            case ">" -> {
                return new BasicVarToken("bool", (t1.compareTo(t2) > 0) + "").setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
            }
            case "<=" -> {
                return new BasicVarToken("bool", (t1.compareTo(t2) <= 0) + "").setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
            }
            case ">=" -> {
                return new BasicVarToken("bool", (t1.compareTo(t2) >= 0) + "").setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
            }
            case "==", "!=" -> {
                boolean flag = false;
                Object o1 = t1.calculateResult(ANY_TYPE);
                Object o2 = t2.calculateResult(ANY_TYPE);
                if (o1 != null && o2 != null) {
                    if (ReflectionUtils.isRegularType(o1.getClass()) && ReflectionUtils.isRegularType(o2.getClass())) {
                        flag = String.valueOf(o1).equals(String.valueOf(o2));
                    } else {
                        //至少有一个不是常规类型
                        flag = o1 == o2;
                    }
                } else if (o1 == null && o2 == null) {
                    flag = true;
                }
                return new BasicVarToken("bool", (ops.getValue().equals("==") == flag) + "").setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
            }
            default -> throw new EvaluateException("unrecognized type ?", ops.getValue());
        }
    }


    private Token numOps(Token t1, Token t2, Token ops) {
        String selfOps = t1.numSelfOps(t2, ops);
        if (selfOps.matches("^'.*'$")) {
            return new BasicVarToken("str", selfOps).setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
        } else {
            return new BasicVarToken("numeric", selfOps).setOriginToken(t1, ops, t2).setOffset(t1.getOffset());
        }
    }


}
