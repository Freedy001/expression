package com.freedy.expression.tokenBuilder;

import com.freedy.expression.core.TokenStream;
import com.freedy.expression.exception.MethodOrPropBuildFailedException;
import com.freedy.expression.token.DirectAccessToken;
import com.freedy.expression.utils.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/27 11:42
 */
public class DirectAccessBuilder extends Builder {


    private final Pattern prefix = Pattern.compile("^([a-zA-Z_$][\\w$]*)(?: *\\(.*?\\))?(?: *([?\\[.].*))?");


    @Override
    public boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        //构建变量 ExecutableToken
        Matcher matcher = prefix.matcher(removeLF(token));
        if (!matcher.matches()) return false;
        matcher = prefix.matcher(removeLF(token));
        if (!matcher.find()) return false;
        String propOrMethod = matcher.group(1);
        if (StringUtils.isEmpty(propOrMethod) || propOrMethod.equals("T")) {
            return false;
        }

        DirectAccessToken directAccessToken = new DirectAccessToken(token);

        String[] splitWithoutBracket = StringUtils.splitWithoutBracket(token, '(', ')', '.', 2);
        try {
            buildMethodOrProp((s1, s2, s3) -> {
                if (s3 != null) {
                    directAccessToken.setRelevantOps(s1);
                    directAccessToken.setMethodName(s2);
                    directAccessToken.setMethodArgsName(s3);
                } else {
                    directAccessToken.setRelevantOps(s1);
                    directAccessToken.setVarName(s2);
                }
            }, splitWithoutBracket[0]);
        } catch (MethodOrPropBuildFailedException e) {
            holder.setMsg(e.getMessage());
            return false;
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
