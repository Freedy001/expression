package com.freedy.expression.tokenBuilder;

import com.freedy.expression.core.TokenStream;
import com.freedy.expression.core.Tokenizer;
import com.freedy.expression.token.StopToken;
import com.freedy.expression.utils.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/27 12:45
 */
public class StopBuilder extends Builder {

    public static final Pattern returnPattern = Pattern.compile("^return +(.*)|^return");


    @Override
    public boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        //构建return
        Matcher matcher = returnPattern.matcher(token);
        if (matcher.find()) {
            StopToken stopToken = new StopToken(token);
            String group = matcher.group(1);
            if (StringUtils.hasText(group)) {
                stopToken.setReturnStream(Tokenizer.doGetTokenStream(group));
            }
            tokenStream.addToken(stopToken);
            return true;
        }
        //构建break
        if (token.matches("break|continue")) {
            tokenStream.addToken(new StopToken(token));
            return true;
        }
        return false;
    }

    @Override
    public int priority() {
        return -1;
    }
}
