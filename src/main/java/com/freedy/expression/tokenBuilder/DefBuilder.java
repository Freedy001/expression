package com.freedy.expression.tokenBuilder;

import com.freedy.expression.core.TokenStream;
import com.freedy.expression.core.Tokenizer;
import com.freedy.expression.token.DefToken;
import com.freedy.expression.utils.StringUtils;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/27 12:43
 */
public class DefBuilder extends Builder {

    private static final Pattern defValPattern = Pattern.compile("^def +([a-zA-Z_$][\\w$]*)");
    private static final Pattern defFuncPattern = Pattern.compile("^def +([a-zA-Z_$][\\w$]*) *\\(( *[\\w$]+ *|(?: *[\\w$]+ *, *)+ *[\\w$]+ *)?\\) *\\{(.*)}");


    @Override
    public boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        //构建def token
        Matcher matcher = defValPattern.matcher(token);
        if (!matcher.find()) return false;
        Matcher funcMatcher = defFuncPattern.matcher(token);
        DefToken objectToken = new DefToken(token);
        if (funcMatcher.find()) {
            objectToken.setMethodName(funcMatcher.group(1)); 
            String args = funcMatcher.group(2);
            objectToken.setMethodArgs(StringUtils.isEmpty(args) ? new String[0] : Arrays.stream(args.split(",")).map(String::strip).toArray(String[]::new));
            objectToken.setMethodBody(Tokenizer.doGetTokenStream(funcMatcher.group(3)));
        } else if (defValPattern.matcher(token).matches()) {
            objectToken.setVariableName(matcher.group(1));
        } else {
            holder.setMsg("illegal def statement").setErrorPart("def");
            return false;
        }
        tokenStream.addToken(objectToken);
        return true;
    }

    @Override
    public int priority() {
        return -1;
    }
}
