package com.freedy.expression.token;

import com.freedy.expression.EvaluationContext;
import com.freedy.expression.Expression;
import com.freedy.expression.TokenStream;
import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.exception.StopSignal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

/**
 * @author Freedy
 * @date 2021/12/22 19:54
 */
@Getter
@Setter
@NoArgsConstructor
public final class LoopToken extends Token {
    //for i in 100:(// do some thing)
    private String variableName;
    // 100
    private TokenStream executeTokenStream;
    // (// do some thing)
    private TokenStream loopTokenStream;

    private Expression subExpression;

    private boolean isDesc;

    public LoopToken(String value) {
        super("loop", value);
    }

    @Override
    public void setContext(EvaluationContext context) {
        super.setContext(context);
        subExpression = new Expression(context);
    }

    @Override
    protected Object doCalculate(Class<?> desiredType) {
        subExpression.setTokenStream(executeTokenStream);
        Object iterable = subExpression.getValue();
        //准备执行loop
        subExpression.setTokenStream(loopTokenStream);
        Object result = null;

        if (context.containsVariable(variableName)){
            throw new EvaluateException("you have already def var ?",variableName);
        }

        try {
            if (iterable instanceof Number num) {
                if (isDesc) {
                    for (long i = num.longValue() - 1; i >= 0; i--) {
                        try {
                            context.setVariable(variableName, i);
                            result = subExpression.getValue();
                        } catch (Throwable e) {
                            StopSignal signal = StopSignal.getInnerSignal(e);
                            if (signal != null) {
                                String signalMsg = signal.getMessage();
                                if ("continue".equals(signalMsg)) {
                                    continue;
                                }
                                if ("break".equals(signalMsg)) {
                                    break;
                                }
                            } else {
                                throw e;
                            }
                        } finally {
                            loopTokenStream.close();
                        }
                    }
                } else {
                    for (long i = 0; i < num.longValue(); i++) {
                        try {
                            context.setVariable(variableName, i);
                            result = subExpression.getValue();
                        } catch (Throwable e) {
                            StopSignal signal = StopSignal.getInnerSignal(e);
                            if (signal != null) {
                                String signalMsg = signal.getMessage();
                                if ("continue".equals(signalMsg)) {
                                    continue;
                                }
                                if ("break".equals(signalMsg)) {
                                    break;
                                }
                            } else {
                                throw e;
                            }
                        } finally {
                            loopTokenStream.close();
                        }
                    }
                }
                return result;
            }
            if (iterable instanceof Object[] array) {
                iterable = Arrays.asList(array);
            }
            if (iterable instanceof Iterable collection) {
                if (isDesc) {
                    listMode:
                    if (iterable instanceof List<?> list) {
                        try {
                            Collections.reverse(list);
                        } catch (Exception e) {
                            break listMode;
                        }
                        for (Object o : list) {
                            try {
                                context.setVariable(variableName, o);
                                result = subExpression.getValue();
                            } catch (Throwable e) {
                                StopSignal signal = StopSignal.getInnerSignal(e);
                                if (signal != null) {
                                    String signalMsg = signal.getMessage();
                                    if ("continue".equals(signalMsg)) {
                                        continue;
                                    }
                                    if ("break".equals(signalMsg)) {
                                        break;
                                    }
                                } else {
                                    throw e;
                                }
                            } finally {
                                loopTokenStream.close();
                            }
                        }
                        return result;
                    }
                    if (iterable instanceof Collection<?> c) {
                        Object[] array = c.toArray();
                        for (int i = array.length - 1; i >= 0; i--) {
                            try {
                                context.setVariable(variableName, array[i]);
                                result = subExpression.getValue();
                            } catch (Throwable e) {
                                StopSignal signal = StopSignal.getInnerSignal(e);
                                if (signal != null) {
                                    String signalMsg = signal.getMessage();
                                    if ("continue".equals(signalMsg)) {
                                        continue;
                                    }
                                    if ("break".equals(signalMsg)) {
                                        break;
                                    }
                                } else {
                                    throw e;
                                }
                            } finally {
                                loopTokenStream.close();
                            }
                        }
                        return result;
                    }
                    LinkedList<Object> list = new LinkedList<>();
                    for (Object o : collection) {
                        list.addFirst(o);
                    }
                    for (Object o : list) {
                        try {
                            context.setVariable(variableName, o);
                            result = subExpression.getValue();
                        } catch (Throwable e) {
                            StopSignal signal = StopSignal.getInnerSignal(e);
                            if (signal != null) {
                                String signalMsg = signal.getMessage();
                                if ("continue".equals(signalMsg)) {
                                    continue;
                                }
                                if ("break".equals(signalMsg)) {
                                    break;
                                }
                            } else {
                                throw e;
                            }
                        } finally {
                            loopTokenStream.close();
                        }
                    }
                    return result;
                }
                for (Object o : collection) {
                    try {
                        context.setVariable(variableName, o);
                        result = subExpression.getValue();
                    } catch (Throwable e) {
                        StopSignal signal = StopSignal.getInnerSignal(e);
                        if (signal != null) {
                            String signalMsg = signal.getMessage();
                            if ("continue".equals(signalMsg)) {
                                continue;
                            }
                            if ("break".equals(signalMsg)) {
                                break;
                            }
                        } else {
                            throw e;
                        }
                    } finally {
                        loopTokenStream.close();
                    }
                }
                return result;

            }
            if (iterable instanceof TokenStream ifStream){
                int loopCount = 0;
                while (true){
                    Boolean value = subExpression.setTokenStream(ifStream).getValue(Boolean.class);
                    if (value==null){
                        throw new EvaluateException("null value return").errToken(this.errStr(executeTokenStream.getExpression()));
                    }
                    if (value){
                        try {
                            context.setVariable(variableName, loopCount++);
                            result = subExpression.setTokenStream(loopTokenStream).getValue();
                        } catch (Throwable e) {
                            StopSignal signal = StopSignal.getInnerSignal(e);
                            if (signal != null) {
                                String signalMsg = signal.getMessage();
                                if ("continue".equals(signalMsg)) {
                                    continue;
                                }
                                if ("break".equals(signalMsg)) {
                                    break;
                                }
                            } else {
                                throw e;
                            }
                        } finally {
                            loopTokenStream.close();
                        }
                    }else {
                        return result;
                    }
                    ifStream.close();
                }
                return result;
            }
            //非可迭代元素
            try {
                context.setVariable(variableName, iterable);
                result = subExpression.getValue();
                loopTokenStream.close();
            } catch (Throwable e) {
                StopSignal signal = StopSignal.getInnerSignal(e);
                if (signal == null) {
                    throw e;
                }
            }
        } finally {
            context.removeVariable(variableName);
        }


        return result;
    }
}
