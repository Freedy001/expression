package com.freedy.expression.core.tokenBuilder;

import com.freedy.expression.core.TokenStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2022/10/30 17:13
 */
public class LambdaTokenBuilder extends Builder {

    private static final Pattern blockTokenStream = Pattern.compile("^@block(?:\\[([\\w$]*?)])? *(?:\\(( *[\\w$]+ *|(?: *[\\w$]+ *, *)+ *[\\w$]+ *)?\\))? *\\{(.*)}$");


    @Override
    public boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        Matcher matcher = blockTokenStream.matcher(token);
        if (!matcher.find()) return false;


        return true;
    }

    @Override
    public int priority() {
        return 10;
    }
}
