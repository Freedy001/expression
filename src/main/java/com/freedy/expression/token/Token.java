package com.freedy.expression.token;

import com.alibaba.fastjson.annotation.JSONType;
import com.freedy.expression.Comparable;
import com.freedy.expression.EvaluationContext;
import com.freedy.expression.Executable;
import com.freedy.expression.exception.EvaluateException;
import com.freedy.expression.exception.StopSignal;
import com.freedy.expression.exception.UnsupportedOperationException;
import com.freedy.expression.utils.ReflectionUtils;
import lombok.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 所有Token的基类,提供一些默认token操作/计算方法。 <br/>
 * 所有继承Token的类需要重写 doCalculate() 或者 doGenericCalculate()方法。 <br/>
 * doCalculate():需要返回该token表示的值。<br/>
 * 例如:<br/>
 * new BasicVarToken('abc');    doCalculate()返回的值就是 "abc"(String)<br/>
 * new BasicVarToken(false);    doCalculate()返回的值就是 false(Boolean)<br/>
 * new ReferenceToken(#Test);   doCalculate()返回的值就是 obj(Object) (命名为Test的对象) <br/>
 * doGenericCalculate(); 该方法与上面方法类似只不过其参数是ParameterizedType 可以根据泛型更加精准的计算结果.
 *
 * @author Freedy
 * @date 2021/12/14 15:33
 */
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@JSONType(includes = {"type", "value"})
public sealed abstract class Token implements Comparable, Executable
        permits BasicVarToken, ClassToken, CollectionToken, ErrMsgToken, IfToken, LoopToken, MapToken, ObjectToken, OpsToken, StopToken, TernaryToken, WrapperToken {
    //Token的type 结合isType()方法省去使用使用instance of进行判断
    @ToString.Include
    protected String type;
    //token原始的字符串值
    @ToString.Include
    protected String value;
    //获取原始token 表示此token是由原始token计算而来
    protected List<Token> originToken;
    //获取子token ,其子token是有该token与其他token计算而来
    protected Token sonToken;
    //偏移量
    protected int offset;
    //异常时需要标记的局部字符串
    protected List<String> errStr;
    //执行环境
    protected EvaluationContext context;
    //非门标记       ! a
    protected boolean notFlag = false;
    //前自加        ++ a
    protected boolean preSelfAddFlag = false;
    //前自减        -- a
    protected boolean preSelfSubFlag = false;
    //后自加        a ++
    protected boolean postSelfAddFlag = false;
    //后自减        a --
    protected boolean postSelfSubFlag = false;
    //执行calculateResult()期望的返回类型
    protected Class<?> desiredType;

    public final static Class<Object> ANY_TYPE = Object.class;


    public Token(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public boolean isType(String type) {
        return type.equals(this.type);
    }

    public boolean isValue(String val) {
        return value.equals(val);
    }

    public boolean isAnyType(String... type) {
        for (String s : type) {
            if (isType(s)) return true;
        }
        return false;
    }

    public boolean isAnyValue(String... type) {
        for (String s : type) {
            if (isValue(s)) return true;
        }
        return false;
    }

    public Token setOriginToken(Token... token) {
        if (originToken == null) {
            originToken = new ArrayList<>();
        }
        for (Token t : token) {
            if (t == this) {
                originToken.add(ReflectionUtils.copyProperties(t, "originToken", "sonToken"));
                continue;
            }
            originToken.add(t);
        }
        return this;
    }

    public Token setOffset(int offset) {
        this.offset = offset;
        return this;
    }

    public Token errStr(String... str) {
        if (str == null) return this;
        if (errStr == null) {
            errStr = new ArrayList<>();
        }
        errStr.addAll(Arrays.asList(str));
        return this;
    }

    public String getValue() {
        if (notFlag) {
            return "!" + value;
        }
        if (preSelfAddFlag) {
            return "++" + value;
        }
        if (preSelfSubFlag) {
            return "--" + value;
        }
        if (postSelfAddFlag) {
            return value + "++";
        }
        if (postSelfSubFlag) {
            return value + "--";
        }
        return value;
    }

    /**
     * 两个token进行比较运算
     */
    public double compareTo(Token o) {
        BigDecimal a;
        BigDecimal b;
        try {
            a = new BigDecimal(this.calculateResult(Number.class) + "");
        } catch (StopSignal e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluateException("incomparable token,cause ?", e).errToken(this);
        }
        try {
            b = new BigDecimal(o.calculateResult(Number.class) + "");
        } catch (StopSignal e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluateException("incomparable token,cause ?", e).errToken(o);
        }
        return a.subtract(b).doubleValue();
    }

    /**
     * 两个token进行逻辑运输
     * @param o      需要与该token进行逻辑运算的token
     * @param type   逻辑运算的类型
     */
    @SuppressWarnings("ConstantConditions")
    public boolean logicOps(Token o, Token type) {
        boolean a;
        boolean b;
        try {
            a = (boolean) this.calculateResult(Boolean.class);
        } catch (Exception e) {
            throw new EvaluateException("incomparable token", e).errToken(this);
        }
        try {
            b = (boolean) o.calculateResult(Boolean.class);
        } catch (Exception e) {
            throw new EvaluateException("incomparable token", e).errToken(o);
        }
        switch (type.getValue()) {
            case "||" -> {
                return a || b;
            }
            case "&&" -> {
                return a && b;
            }
            default -> throw new EvaluateException("unrecognized type ?", type).errToken(type);
        }
    }

    /**
     * 两个token进行+,-,*,/,+=,-=,/=,*=运算
     * @param o       需要与该token进行运算的token
     * @param type    运算的类型
     */
    @SuppressWarnings("DuplicateExpressions")
    public String numSelfOps(Token o, Token type) {
        Object o1;
        Object o2;
        try {
            o1 = calculateResult(Object.class);
        } catch (StopSignal e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluateException("incomparable token,cause ?", e).errToken(this);
        }
        try {
            o2 = o.calculateResult(Object.class);
        } catch (StopSignal e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluateException("incomparable token,cause ?", e).errToken(o);
        }
        if (type.isValue("+")) {
            if (o1 instanceof String || o2 instanceof String) {
                return "'" + o1 + o2 + "'";
            }
        }
        if (o1 == null) {
            throw new EvaluateException("can not operation on null").errToken(this);
        }
        if (o2 == null) {
            throw new EvaluateException("can not operation on null").errToken(o);
        }
        BigDecimal a = new BigDecimal(o1 + "");
        BigDecimal b = new BigDecimal(o2 + "");
        switch (type.getValue()) {
            case "+" -> {
                return a.add(b).toString();
            }
            case "-" -> {
                return a.subtract(b).toString();
            }
            case "*" -> {
                return a.multiply(b).toString();
            }
            case "/" -> {
                //scale 必须超出double的范畴
                return a.divide(b, 20, RoundingMode.DOWN).toString();
            }
            case "|" -> {
                intTypeCheck(o, a, b);
                return (a.intValue() | b.intValue()) + "";
            }
            case "&" -> {
                intTypeCheck(o, a, b);
                return (a.intValue() & b.intValue()) + "";
            }
            case "^" -> {
                intTypeCheck(o, a, b);
                return (a.intValue() ^ b.intValue()) + "";
            }
            case ">>" -> {
                intTypeCheck(o, a, b);
                return (a.intValue() >> b.intValue()) + "";
            }
            case ">>>" -> {
                intTypeCheck(o, a, b);
                return (a.intValue() >>> b.intValue()) + "";
            }
            case "<<" -> {
                intTypeCheck(o, a, b);
                return (a.intValue() << b.intValue()) + "";
            }
            case "+=" -> {
                return opsAndAssign(a.add(b).toString(), type);
            }
            case "-=" -> {
                return opsAndAssign(a.subtract(b).toString(), type);
            }
            case "/=" -> {
                return opsAndAssign(a.divide(b, 20, RoundingMode.DOWN).toString(), type);
            }
            case "*=" -> {
                return opsAndAssign(a.multiply(b).toString(), type);
            }
            case "|="->{
                intTypeCheck(o, a, b);
                return opsAndAssign((a.intValue() | b.intValue()) + "", type);
            }
            case "&="->{
                intTypeCheck(o, a, b);
                return opsAndAssign((a.intValue() & b.intValue()) + "", type);
            }
            case "^="->{
                intTypeCheck(o, a, b);
                return opsAndAssign((a.intValue() ^ b.intValue()) + "", type);
            }
            default -> throw new EvaluateException("unrecognized type ?", type).errToken(type);
        }
    }

    private String opsAndAssign(String result, Token type) {
        if (this instanceof Assignable assignable) {
            assignable.assignFrom(new BasicVarToken("numeric", result));
            return calculateResult(ANY_TYPE) + "";
        } else {
            throw new EvaluateException("nonassignable token").errToken(this).errToken(type);
        }
    }

    private void intTypeCheck(Token o, BigDecimal a, BigDecimal b) {
        if (a.toString().contains(".")) {
            throw new EvaluateException("Bit operation only support integer").errToken(this);
        }
        if (b.toString().contains(".")) {
            throw new EvaluateException("Bit operation only support integer").errToken(o);
        }
    }


    protected void checkSetSingleOps(String currentOps, boolean isPost) {
        if (notFlag || preSelfAddFlag || preSelfSubFlag || postSelfAddFlag || postSelfSubFlag)
            throw new EvaluateException("has already set single ops ?", notFlag ? "!" : preSelfAddFlag ? "++a" : preSelfSubFlag ? "--a" : postSelfAddFlag ? "a++" : "a--")
                    .errToken(this.errStr(isPost ? value + "@" + currentOps : currentOps + "$" + value));
    }

    public void setNotFlag(boolean notFlag) {
        checkSetSingleOps("!", false);
        try {
            calculateResult(Boolean.class);
        } catch (StopSignal e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluateException("NOT OPS are not support", e).errToken(this.errStr("!$" + value));
        }
        offset--;
        this.notFlag = notFlag;
    }

    public void setPreSelfAddFlag(boolean preSelfAddFlag) {
        checkSetSingleOps("++", false);
        try {
            calculateResult(Integer.class);
        } catch (StopSignal e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluateException("++a OPS are not support", e).errToken(this.errStr("++" + value));
        }
        offset -= 2;
        this.preSelfAddFlag = preSelfAddFlag;
    }

    public void setPreSelfSubFlag(boolean preSelfSubFlag) {
        checkSetSingleOps("--", false);
        try {
            calculateResult(Integer.class);
        } catch (StopSignal e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluateException("--a OPS are not support", e).errToken(this.errStr("--" + value));
        }
        offset -= 2;
        this.preSelfSubFlag = preSelfSubFlag;
    }

    public void setPostSelfAddFlag(boolean postSelfAddFlag) {
        checkSetSingleOps("++", true);
        try {
            calculateResult(Integer.class);
        } catch (StopSignal e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluateException("a++ OPS are not support", e).errToken(this.errStr(value + "++"));
        }
        this.postSelfAddFlag = postSelfAddFlag;
    }

    public void setPostSelfSubFlag(boolean postSelfSubFlag) {
        checkSetSingleOps("--", true);
        try {
            calculateResult(Integer.class);
        } catch (StopSignal e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluateException("a-- OPS are not support,cause ?", e).errToken(this.errStr(value + "--"));
        }
        this.postSelfSubFlag = postSelfSubFlag;
    }

    public final Object calculateResult(Type desiredType) {
        if (desiredType instanceof Class<?> clazz) {
            this.desiredType = clazz;
            return doCalculate(clazz);
        } else if (desiredType instanceof ParameterizedType clazz) {
            this.desiredType = (Class<?>) clazz.getRawType();
            return doGenericCalculate(clazz);
        }
        return null;
    }

    protected Object doCalculate(Class<?> desiredType) {
        throw new java.lang.UnsupportedOperationException();
    }

    protected Object doGenericCalculate(ParameterizedType desiredType) {
        throw new java.lang.UnsupportedOperationException();
    }

    protected void checkContext() {
        if (context == null)
            throw new EvaluateException("can not evaluate,because evaluationContext is null");
    }

    protected Object check(Object result) {
        if (result == null) {
            if (!notFlag && !postSelfAddFlag && !postSelfSubFlag && !preSelfAddFlag && !preSelfSubFlag) {
                return null;
            }
            throw new EvaluateException("null value can not do ?", notFlag ? "!" : preSelfAddFlag ? "++a" : preSelfSubFlag ? "--a" : postSelfAddFlag ? "a++" : "a--")
                    .errToken(this);
        }
        if (!ReflectionUtils.convertToWrapper(desiredType).isInstance(result)) {
            Object res = ReflectionUtils.tryConvert(desiredType, result);
            if (res != Boolean.FALSE) {
                return res;
            }
            throw new EvaluateException("unmatched type! real type ? desired type ?", result.getClass().getName(), desiredType.getName());
        }
        return result;
    }

    /**
     * 执行自增,自减等操作
     */
    protected Object selfOps(Object result) {
        if (notFlag) {
            if (result instanceof Boolean r) {
                return !r;
            }
            throw new EvaluateException("NOT OPS on none boolean type ?", result.getClass().getName());
        }
        if (postSelfAddFlag) { //a++
            return numSelfOps(result, 1, true);
        }
        if (postSelfSubFlag) { //a--
            return numSelfOps(result, -1, true);
        }
        if (preSelfAddFlag) { //++a
            return numSelfOps(result, 1, false);
        }
        if (preSelfSubFlag) { //--a
            return numSelfOps(result, -1, false);
        }
        return result;
    }

    protected Object checkAndSelfOps(Object result) {
        return check(selfOps(result));
    }

    private Object numSelfOps(Object result, int num, boolean isPost) {
        if (result instanceof Number n) {
            Number add = add(n, num);
            if (this instanceof Assignable assignable) {
                assignable.assignFrom(new BasicVarToken("numeric", add + ""));
            } else {
                throw new EvaluateException("SELF OPS are not supported on ? token", this.getType());
            }
            return isPost ? result : add;
        }
        throw new EvaluateException("SELF OPS [?++] on none Numeric type ?", result, result.getClass().getName());
    }


    protected Number add(Number n, int b) {
        if (n instanceof Integer a) {
            return a + b;
        }
        if (n instanceof Long a) {
            return a + b;
        }
        if (n instanceof Double a) {
            return a + b;
        }
        if (n instanceof Float a) {
            return a + b;
        }
        if (n instanceof Short a) {
            return a + b;
        }
        throw new UnsupportedOperationException("Number type ? not support", n.getClass().getName());
    }


}
