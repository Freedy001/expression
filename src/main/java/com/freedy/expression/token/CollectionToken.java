package com.freedy.expression.token;

import com.alibaba.fastjson.annotation.JSONType;
import com.freedy.expression.core.EvaluationContext;
import com.freedy.expression.core.Expression;
import com.freedy.expression.core.TokenStream;
import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.utils.ReflectionUtils;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Freedy
 * @date 2021/12/14 15:51
 */
@Getter
@Setter
@JSONType(includes = {"type", "value"})
public final class CollectionToken extends ExecutableToken {
    private List<TokenStream> subTokenStream = new ArrayList<>();
    private TokenStream relevantOps;
    private Expression expression;

    public CollectionToken(String value) {
        super("collection", value);
    }

    public void addTokenStream(TokenStream e) {
        subTokenStream.add(e);
    }

    @Override
    public void setContext(EvaluationContext context) {
        super.setContext(context);
        expression = new Expression(context);
    }

    @Override
    public Object doCalculate(Class<?> desiredType) {
        if (subTokenStream == null) {
            throw new EvaluateException("null elements").errToken(this);
        }
        if (ReflectionUtils.isSonInterface(desiredType, "java.util.Collection")) {
            if (relevantOps != null) {
                throw new EvaluateException("relevant ops [?] are not allow here", relevantOps.getExpression()).errToken(this.errStr("[" + relevantOps.getExpression() + "]"));
            }
            //集合没指定泛型
            return checkAndSelfOps(subTokenStream.stream()
                    .map(stream -> {
                        expression.setTokenStream(stream);
                        try {
                            return expression.getValue();
                        } finally {
                            stream.close();
                        }
                    })
                    .collect(Collectors.toCollection(
                            () -> ReflectionUtils.buildCollectionByType(desiredType)
                    )));
        } else {
            //非集合
            if (relevantOps != null) {
                expression.setTokenStream(relevantOps);
                Integer index = expression.getValue(Integer.class);
                relevantOps.close();
                if (index == null) {
                    throw new EvaluateException("illegal ops [?], ops can not be null", relevantOps.getExpression()).errToken(this.errStr("[" + relevantOps.getExpression() + "]"));
                }
                if (index >= subTokenStream.size()) {
                    throw new EvaluateException("illegal ops(Array[?]),index out of bound", relevantOps.getExpression()).errToken(this.errStr("[" + relevantOps.getExpression() + "]"));
                }
                @Cleanup TokenStream stream = subTokenStream.get(index);
                expression.setTokenStream(stream);
                return checkAndSelfOps(expression.getValue());
            } else {
                //无relevantOpsName
                return checkAndSelfOps(subTokenStream.stream()
                        .map(stream -> {
                            expression.setTokenStream(stream);
                            try {
                                return expression.getValue();
                            } finally {
                                stream.close();
                            }
                        })
                        .collect(Collectors.toList()));
            }
        }
    }

    @Override
    protected Object doGenericCalculate(ParameterizedType parameterizedType) {
        if (relevantOps != null) {
            throw new EvaluateException("relevant ops ? are not allow here", relevantOps.getExpression()).errToken(this.errStr(relevantOps.getExpression()));
        }
        Type[] genericType = parameterizedType.getActualTypeArguments();
        Class<?> listType = (Class<?>) genericType[0];
        Class<?> rawType = (Class<?>) parameterizedType.getRawType();

        return checkAndSelfOps(subTokenStream.stream()
                .map(stream -> {
                    expression.setTokenStream(stream);
                    try {
                        return expression.getValue(listType);
                    } finally {
                        stream.close();
                    }
                })
                .collect(Collectors.toCollection(
                        () -> ReflectionUtils.buildCollectionByType(rawType)
                )));
    }

}
