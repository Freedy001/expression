package com.freedy.expression.tokenBuilder;

import com.freedy.expression.core.TokenStream;
import com.freedy.expression.token.DirectAccessToken;
import com.freedy.expression.utils.StringUtils;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2022/3/8 11:38
 */
public class InlineFunctionBuilder extends Builder {

    private static final Pattern inline = Pattern.compile("([a-zA-Z_]\\w*) +(.*)");

    @Override
    public boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        Matcher matcher = inline.matcher(token);
        if (!matcher.find()) return false;

        String funcName = matcher.group(1);
        DirectAccessToken directAccessToken = new DirectAccessToken(token);
        directAccessToken.setMethodName(funcName);
        directAccessToken.setMethodArgsName(Arrays.stream(
                StringUtils.splitWithoutBracket(matcher.group(2), '\'', '\'', ' ')
        ).filter(StringUtils::hasText).map(s -> StringUtils.isSurroundByQuote(s) ? s : "'" + s + "'").toArray(String[]::new));
        tokenStream.addToken(directAccessToken);
        return true;
    }

    @Override
    public int priority() {
        return 10;
    }
}
