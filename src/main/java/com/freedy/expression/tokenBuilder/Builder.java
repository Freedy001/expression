package com.freedy.expression.tokenBuilder;

import com.freedy.expression.core.TokenStream;
import com.freedy.expression.exception.MethodOrPropBuildFailedException;
import com.freedy.expression.token.ReflectToken;
import com.freedy.expression.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/27 9:27
 */
public abstract class Builder {

    public static final Pattern methodPattern = Pattern.compile("(.*?)\\((.*)\\)");

    public static final Pattern varPattern = Pattern.compile("^[a-zA-Z_$][\\w$]*");

    public static final Pattern relevantAccess = Pattern.compile("(.*?)((?:\\?|\\[.*]|\\? *?\\[.*])+)");

    public static final Pattern numericPattern = Pattern.compile("-?\\d+|-?\\d+[lL]|-?\\d+?\\.\\d+");

    public static final Pattern boolPattern = Pattern.compile("^true$|^false$");

    public static final Pattern strPattern = Pattern.compile("^'([^']*?)'$|^\"([^\"]*?)\"$");

    /**
     * 用于构建 token
     *
     * @param tokenStream token容器
     * @param token       需要被构建的字符串
     * @param holder      异常holder,抛异常应该往holder里面放然后返回false 不应该直接抛异常
     *                    否则该token字符串不会被后续的token构建器解析
     * @return 是否构建成功
     */
    public abstract boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder);

    /**
     * @return 优先级 越小越高
     */
    public abstract int priority();

    /**
     * 构建执行链
     */
    protected void buildExecuteChain(ReflectToken token, String suffixStr, ExceptionMsgHolder holder) {
        //构建执行链
        String[] step = StringUtils.splitWithoutBracket(suffixStr, '(', ')', '.');
        for (int i = 0; i < step.length; i++) {
            try {
                buildMethodOrProp((s1, s2, s3) -> {
                    if (s3 != null) {
                        token.addMethod(s1, s2, s3);
                    } else {
                        token.addProperties(s1, s2);
                    }
                }, step[i]);
            } catch (MethodOrPropBuildFailedException e) {
                StringJoiner joiner = new StringJoiner(".");
                for (int j = 0; j < i; j++) {
                    joiner.add(step[j]);
                }
                holder.setMsg(e.getMessage())
                        .setErrorPart(StringUtils.hasText(joiner + "") ? (joiner + ".@" + e.getMethodOrPropName()) : e.getMethodOrPropName());
                return;
            }
        }
    }

    /**
     * 构建方法或者属性
     * @param handle    构建完毕后需要执行的操作
     * @param stepStr   需要被构建的字符串
     * @throws MethodOrPropBuildFailedException  构建失败异常
     */
    protected void buildMethodOrProp(ResultHandle handle, String stepStr) throws MethodOrPropBuildFailedException {

        ReplacedStr replacedStr = convertStr(stepStr.trim());
        String propOrMethodName = replacedStr == null ? stepStr.trim() : replacedStr.result;

        //检测是否包含list access
        Matcher listMatcher = relevantAccess.matcher(propOrMethodName);
        String relevantOps = null;
        if (listMatcher.find()) {
            propOrMethodName = listMatcher.group(1).trim();
            relevantOps = listMatcher.group(2).trim();
        }

        Matcher matcher = methodPattern.matcher(propOrMethodName);
        if (matcher.find()) {
            //replacedStr 不可能为空
            assert replacedStr != null;
            String methodName = matcher.group(1).trim();
            if (!varPattern.matcher(methodName).matches()) {
                throw new MethodOrPropBuildFailedException("illegal method name", methodName);
            }
            String group = matcher.group(2);
            if (group.equals("REPLACE")) {
                String aimedStr = replacedStr.aimedStr;
                handle.handle(relevantOps, methodName, StringUtils.hasText(aimedStr) ? StringUtils.splitWithoutBracket(aimedStr, new char[]{'{', '('}, new char[]{'}', ')'}, ',') : new String[0]);
            } else {
                throw new IllegalArgumentException("unreachable exception");
            }
        } else {
            if (!varPattern.matcher(propOrMethodName).matches()) {
                throw new MethodOrPropBuildFailedException("illegal prop name", propOrMethodName);
            }
            handle.handle(relevantOps, propOrMethodName, (String[]) null);
        }

    }

    protected String removeLF(String str){
        return str.replace("\r\n", " ").replace("\r", " ");
    }

    /**
     * 将方法参数用REPLACE字符串代替，利于正则表达式的解析
     *
     * @param token 需要转化字符串
     * @return 被替换后的结果，如果传入的字符串不是方法样式则返回null
     */
    protected ReplacedStr convertStr(String token) {
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
        return new ReplacedStr(token.substring(0, startIndex) + "REPLACE" + token.substring(endIndex),
                token.substring(startIndex, endIndex).trim(), startIndex);
    }


    @ToString
    @AllArgsConstructor
    protected static class ReplacedStr {
        /**
         * 替换后的结果
         */
        String result;
        /**
         * 被替换字符串的原本值
         */
        String aimedStr;
        /**
         * 被替换字符串的初始下标
         */
        int aimedIndex;
    }

    @FunctionalInterface
    protected interface ResultHandle {
        /**
         * 方法或属性构建完毕后要执行的操作
         *
         * @param relevantOps      相关后缀操作
         * @param methodOrPropName 方法或属性名称
         * @param args             方法参数/null表示是属性操作非null表示是方法操作
         */
        void handle(String relevantOps, String methodOrPropName, String... args);
    }

    @Getter
    public static class ExceptionMsgHolder {
        boolean isErr = false;
        StackTraceElement[] elements;
        String msg;
        String[] errPart;

        public ExceptionMsgHolder setMsg(String msg) {
            err();
            this.msg = msg;
            return this;
        }

        public ExceptionMsgHolder setErrorPart(String... errPart) {
            err();
            this.errPart = errPart;
            return this;
        }

        void err() {
            isErr = true;
            StackTraceElement[] trace = Thread.currentThread().getStackTrace();
            elements = Arrays.copyOfRange(trace, 3, trace.length);
        }
    }


}
