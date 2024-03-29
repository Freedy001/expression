package com.freedy.expression.core.tokenBuilder;

import com.freedy.expression.core.TokenStream;
import com.freedy.expression.core.Tokenizer;
import com.freedy.expression.core.token.IfToken;
import com.freedy.expression.utils.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/27 11:22
 */
public class IfBuilder extends Builder {

    // if
    //(if|else +if) *?\((.*?)\) *?\{(?:(.*?)}(?= *else)|(.*)})|else *?\{(.*)}
    private static final Pattern ifPattern = Pattern.compile("(if|else +if) *?(.*?) *?\\{(?:(.*?)}(?= *else)|(.*)})|else *?\\{(.*)}");

    @Override
    public boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        if (!token.startsWith("if")) return false;
        if (!token.endsWith("}")) {
            int i = token.lastIndexOf("}");
            holder.setMsg("illegal if statement,if token must end with '};'")
                    .setErrorPart(i == -1 ? token : token.substring(i));
            return false;
        }
        Matcher matcher = ifPattern.matcher(token);
        if (!matcher.find()) return false;

        IfToken ifToken = new IfToken(token);
        tokenStream.addToken(ifToken);
        boolean first = true;
        do {
            String ifOrIfElse = matcher.group(1);
            if (StringUtils.hasText(ifOrIfElse)) {
                if (first && ifOrIfElse.equals("else if")) {
                    holder.setErrorPart("else if");
                    return false;
                }
                String boolBlock = matcher.group(2);
                if (StringUtils.isEmpty(boolBlock)) {
                    holder.setMsg("condition area can not be empty")
                            .setErrorPart("if");
                    return false;
                }
                String mainBody = matcher.group(3);
                if (StringUtils.isEmpty(mainBody)) {
                    mainBody = matcher.group(4);
                    if (StringUtils.isEmpty(mainBody)) {
                        holder.setMsg("mainBody area can not be empty")
                                .setErrorPart(":()");
                        return false;
                    }
                }
                TokenStream bStream = Tokenizer.doGetTokenStream(boolBlock);
                TokenStream mStream = Tokenizer.doGetTokenStream(mainBody);
                ifToken.addStatement(bStream, mStream);
            }
            //构建else
            String elseStatement = matcher.group(5);
            if (StringUtils.hasText(elseStatement)) {
                TokenStream eStream = Tokenizer.doGetTokenStream(elseStatement);
                ifToken.setElseTokenStream(eStream);
            }

            first = false;
        } while (matcher.find());
        return true;
    }



    @Override
    public int priority() {
        return -1;
    }
}
