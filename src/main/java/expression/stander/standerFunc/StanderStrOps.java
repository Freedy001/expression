package com.freedy.expression.stander.standerFunc;

import com.freedy.expression.stander.ExpressionFunc;

import java.util.Locale;

/**
 * @author Freedy
 * @date 2022/3/6 15:48
 */
public class StanderStrOps extends AbstractStanderFunc {

    @ExpressionFunc("similar to linux grep")
    public String grep(String str, String pat) {
        StringBuilder builder = new StringBuilder();
        for (String s : str.split("\n")) {
            if (s.toLowerCase(Locale.ROOT).contains(pat.toLowerCase(Locale.ROOT))) {
                builder.append(s.replace(pat, "\033[91m" + pat + "\033[0;39m")).append("\n");
            }
        }
        return builder.toString();
    }

    @ExpressionFunc("convert str to char")
    public char _char(String str) {
        if (str.length() != 1) throw new UnsupportedOperationException("string length must be 1");
        return str.toCharArray()[0];
    }

    @ExpressionFunc("input a escape char")
    public String esc(String o) {
        StringBuilder builder = new StringBuilder();
        int length = o.length();
        if (length % 2 != 0) {
            throw new java.lang.IllegalArgumentException("illegal escape character");
        }
        int lastSplit = 0;
        for (int i = 2; i <= length; i += 2) {
            switch (o.substring(lastSplit, i)) {
                case "\\\"" : builder.append("\""); break;
                case "\\'" : builder.append("'"); break;
                case "\\t" : builder.append("\t"); break;
                case "\\b" : builder.append("\b"); break;
                case "\\n" : builder.append("\n"); break;
                case "\\r" : builder.append("\r"); break;
                case "\\f" : builder.append("\f"); break;
                case "\\\\" : builder.append("\\"); break;
                default : {
                    if (o.contains("\\")) {
                        throw new java.lang.IllegalArgumentException("illegal escape character");
                    }
                    return o;
                }
            }
            lastSplit = i;
        }
        return builder.toString();
    }

}