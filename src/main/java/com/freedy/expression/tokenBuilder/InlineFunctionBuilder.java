package com.freedy.expression.tokenBuilder;

import com.freedy.expression.TokenStream;
import com.freedy.expression.token.DirectAccessToken;

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
    boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        Matcher matcher = inline.matcher(token);
        if (!matcher.find()) return false;

        String funcName = matcher.group(1);
        DirectAccessToken directAccessToken = new DirectAccessToken(token);
        directAccessToken.setMethodName(funcName);
        directAccessToken.setMethodArgsName(Arrays.stream(matcher.group(2).split(" ")).distinct().map(i -> {
            if (i.contains(".")) {
                return i.startsWith("'") && i.endsWith("'") ? i : "'" + i + "'";
            } else if (i.startsWith("%") || i.startsWith("'") && i.endsWith("'") || i.startsWith("\"") && i.endsWith("\"")) {
                return i;
            } else {
                return "%" + i;
            }
        }).toArray(String[]::new));
        tokenStream.addToken(directAccessToken);
        return true;
    }

    @Override
    int priority() {
        return 10;
    }
}
