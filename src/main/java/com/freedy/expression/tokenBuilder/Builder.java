package com.freedy.expression.tokenBuilder;

import com.freedy.expression.TokenStream;
import com.freedy.expression.token.ClassToken;
import com.freedy.expression.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/27 9:27
 */
public abstract class Builder {


    public final Pattern methodPattern = Pattern.compile("(.*?)\\((.*)\\)");

    public final Pattern varPattern = Pattern.compile("^[a-zA-Z_]\\w*");

    public final Pattern relevantAccess = Pattern.compile("(.*?)((?:\\?|\\[.*]|\\? *?\\[.*])+)");

    abstract boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder);

    //优先级 越小越高
    abstract int priority();


    protected void buildExecuteChain(ClassToken token, String suffixStr, ExceptionMsgHolder holder) {

        //构建执行链
        String[] step = StringUtils.splitWithoutBracket(suffixStr, '(', ')', '.');
        for (int i = 0; i < step.length; i++) {
            Node node = convertStr(step[i].trim());
            String propOrMethodName = node == null ? step[i].trim() : node.result;

            //检测是否包含list access
            Matcher listMatcher = relevantAccess.matcher(propOrMethodName);
            String relevantOps = null;
            if (listMatcher.find()) {
                propOrMethodName = listMatcher.group(1).trim();
                relevantOps = listMatcher.group(2).trim();
            }

            Matcher matcher = methodPattern.matcher(propOrMethodName);
            if (matcher.find()) {
                //node 不可能为空
                assert node != null;
                String methodName = matcher.group(1).trim();
                if (!varPattern.matcher(methodName).matches()) {
                    StringJoiner joiner = new StringJoiner(".");
                    for (int j = 0; j < i; j++) {
                        joiner.add(step[j]);
                    }
                    holder.setMsg("illegal method name")
                            .setErrorPart(StringUtils.hasText(joiner.toString()) ? (joiner + ".@" + methodName) : methodName);
                    return;
                }
                String group = matcher.group(2);
                if (group.equals("REPLACE")) {
                    String aimedStr = node.aimedStr;
                    token.addMethod(relevantOps, methodName, StringUtils.hasText(aimedStr) ? StringUtils.splitWithoutBracket(aimedStr, new char[]{'{', '('}, new char[]{'}', ')'}, ',') : new String[0]);
                } else {
                    throw new IllegalArgumentException("unreachable exception");
                }
            } else {
                if (!varPattern.matcher(propOrMethodName).matches()) {
                    StringJoiner joiner = new StringJoiner(".");
                    for (int j = 0; j < i; j++) {
                        joiner.add(step[j]);
                    }
                    holder.setMsg("illegal prop name")
                            .setErrorPart(joiner + ".@" + propOrMethodName);
                }
                token.addProperties(relevantOps, propOrMethodName);
            }
        }

    }

    protected Node convertStr(String token) {
        int length = token.length();
        char[] chars = token.toCharArray();
        int bracket = 0;
        int startIndex = -1;
        int endIndex = 0;
        for (; endIndex < length; endIndex++) {
            if (chars[endIndex] == '(') {
                if (startIndex == -1) {
                    startIndex = endIndex + 1;
                }
                bracket++;
            }
            if (chars[endIndex] == ')') {
                if (--bracket == 0) {
                    break;
                }
            }
        }
        if (startIndex == -1 || bracket != 0) {
            return null;
        }
        return new Node(token.substring(0, startIndex) + "REPLACE" + token.substring(endIndex),
                token.substring(startIndex, endIndex).trim(), startIndex);
    }


    @ToString
    @AllArgsConstructor
    protected static class Node {
        String result;
        String aimedStr;
        int aimedIndex;
    }

    @Getter
    public static class ExceptionMsgHolder {
        boolean isErr = false;
        String msg;
        String[] errPart;

        public ExceptionMsgHolder setMsg(String msg) {
            isErr = true;
            this.msg = msg;
            return this;
        }

        public ExceptionMsgHolder setErrorPart(String... errPart) {
            isErr = true;
            this.errPart = errPart;
            return this;
        }
    }


}
