package com.freedy.expression.entrance.cmd;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.freedy.expression.core.Expression;
import com.freedy.expression.standard.StandardEvaluationContext;
import com.freedy.expression.utils.Color;
import lombok.Getter;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;


public class TerminalExpr {
    private final Expression ex = new Expression();
    @Getter
    private final StandardEvaluationContext context;

    public TerminalExpr(StandardEvaluationContext context) {
        this.context = context;
    }

    public void eval(String completeScript) {
        eval(completeScript, Color.dYellow("unknown"));
    }

    public void eval(String completeScript, String nullValTips) {
        try {
            Object value = ex.getValue(completeScript, context);
            context.setVariable("_lastReturn", value);
            if (value == null) {
                System.out.println(nullValTips);
                return;
            }
            if (value instanceof Collection<?> collection) {
                System.out.println("\033[95mCollection:\033[0;39m");
                for (Object o : collection) {
                    System.out.println("\t" + toString(o));
                }
            } else if (value instanceof Map<?, ?> map) {
                System.out.println("\033[95mMap:\033[0;39m");
                map.forEach((k, v) -> System.out.println("\t" + toString(k) + " --- " + toString(v)));
            } else if (value.getClass().getName().startsWith("[")) {
                int length = Array.getLength(value);
                StringJoiner joiner = new StringJoiner(",", "[", "]");
                for (int i = 0; i < length; i++) {
                    joiner.add(toString(Array.get(value, i), true));
                }
                System.out.println(joiner);
            } else {
                System.out.println(toString(value, true));
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private String toString(Object o) {
        return toString(o, false);
    }

    private String toString(Object o, boolean pretty) {
        Object format = context.getVariable("jsonFormat");
        if (format instanceof Boolean f && f) {
            return o.toString().equals(o.getClass().getName() + "@" + Integer.toHexString(o.hashCode())) ?
                    pretty ? JSON.toJSONString(o, SerializerFeature.PrettyFormat) : JSON.toJSONString(o) : o.toString();
        } else return o.toString();
    }
}
