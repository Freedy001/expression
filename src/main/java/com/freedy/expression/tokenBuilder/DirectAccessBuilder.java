package com.freedy.expression.tokenBuilder;

import com.freedy.expression.TokenStream;
import com.freedy.expression.token.DirectAccessToken;
import com.freedy.expression.utils.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/27 11:42
 */
public class DirectAccessBuilder extends Builder {


    private final Pattern prefix = Pattern.compile("^([a-zA-Z_]\\w*) *(.*)");
    private final Pattern methodPattern = Pattern.compile("^([a-zA-Z_]\\w*) *?\\((.*)\\) *(.*)");


    @Override
    public boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        //构建变量 Token
        Matcher matcher = prefix.matcher(token);

        if (!matcher.find()) return false;
        String propOrMethod = matcher.group(1);
        if (StringUtils.isEmpty(propOrMethod)) {
            return false;
        }
        if (propOrMethod.equals("T")) {
            return false;
        }


        String[] splitWithoutBracket = StringUtils.splitWithoutBracket(token, '(', ')', '.', 2);

        DirectAccessToken directAccessToken = new DirectAccessToken(token);
        Matcher methodMatcher = methodPattern.matcher(splitWithoutBracket[0]);
        if (methodMatcher.find()) {
            //method
            directAccessToken.setMethodName(methodMatcher.group(1));
            String argStr = methodMatcher.group(2);
            directAccessToken.setMethodArgsName(StringUtils.hasText(argStr) ? StringUtils.splitWithoutBracket(argStr, new char[]{'{', '('}, new char[]{'}', ')'}, ',') : new String[0]);
            String suffix = methodMatcher.group(3);
            if (StringUtils.hasText(suffix = suffix.trim()) && !directAccessToken.setRelevantOpsSafely(suffix)) {
                return false;
            }
        } else {
            matcher = prefix.matcher(splitWithoutBracket[0]);
            if (matcher.find()) {
                directAccessToken.setVarName(matcher.group(1));
                String suffix = matcher.group(2);
                if (StringUtils.hasText(suffix = suffix.trim()) && !directAccessToken.setRelevantOpsSafely(suffix)) {
                    return false;
                }
            }else {
                holder.setMsg("illegal token");
                return false;
            }
        }


        if (splitWithoutBracket.length == 2) {
            buildExecuteChain(directAccessToken, splitWithoutBracket[1], holder);
            if (holder.isErr()) {
                return false;
            }
        }
        tokenStream.addToken(directAccessToken);
        return true;
    }

    @Override
    public int priority() {
        return 4;
    }
}
