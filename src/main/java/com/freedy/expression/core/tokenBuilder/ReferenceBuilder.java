package com.freedy.expression.core.tokenBuilder;

import com.freedy.expression.core.TokenStream;
import com.freedy.expression.core.token.ReferenceToken;
import com.freedy.expression.utils.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/27 12:40
 */
public class ReferenceBuilder extends Builder {

    private static final Pattern referencePattern = Pattern.compile("^([#@%])(.*?) *?\\. *?(.*)");

    @Override
    public boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        //构建reference ExecutableToken
        Matcher matcher = referencePattern.matcher(token);
        if (matcher.find()) {
            ReferenceToken referenceToken = new ReferenceToken(token);

            String reference = matcher.group(2).strip();
            Matcher listMatcher = relevantAccess.matcher(reference);
            String relevantOps = null;
            if (listMatcher.find()) {
                reference = listMatcher.group(1).trim();
                String ops = listMatcher.group(2);
                if (StringUtils.isEmpty(ops)) {
                    ops = listMatcher.group(3);
                    if (StringUtils.isEmpty(ops)) {
                        ops = listMatcher.group(4);
                    }
                }
                relevantOps = ops.trim();
            }

            referenceToken.setReference(matcher.group(1).trim() + reference);
            referenceToken.setRelevantOps(relevantOps);
            //构建执行链
            buildExecuteChain(referenceToken, matcher.group(3).trim(), holder);
            if (holder.isErr) {
                return false;
            }
            tokenStream.addToken(referenceToken);
            return true;
        } else {
            if (token.matches("^[@#%].*")) {
                ReferenceToken referenceToken = new ReferenceToken(token);
                Matcher listMatcher = relevantAccess.matcher(token);
                String relevantOps = null;
                if (listMatcher.find()) {
                    token = listMatcher.group(1).trim();
                    String ops = listMatcher.group(2);
                    if (StringUtils.isEmpty(ops)) {
                        ops = listMatcher.group(3);
                        if (StringUtils.isEmpty(ops)) {
                            ops = listMatcher.group(4);
                        }
                    }
                    relevantOps = ops.trim();
                }


                if (!varPattern.matcher(token.substring(1)).matches()) {
                    holder.setErrorPart(token)
                            .setMsg("illegal prop name");
                    return false;
                }
                referenceToken.setReference(token);
                referenceToken.setRelevantOps(relevantOps);
                tokenStream.addToken(referenceToken);
                return true;
            }
        }
        return false;
    }

    @Override
    public int priority() {
        return 6;
    }
}
