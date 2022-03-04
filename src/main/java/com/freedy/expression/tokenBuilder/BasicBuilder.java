package com.freedy.expression.tokenBuilder;

import com.freedy.expression.TokenStream;
import com.freedy.expression.token.BasicVarToken;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2021/12/27 12:46
 */
public class BasicBuilder extends Builder{


    private static final Pattern strPattern = Pattern.compile("^'([^']*?)'$|^\"([^\"]*?)\"$");

    private static final Pattern numericPattern = Pattern.compile("-?\\d+|-?\\d+[lL]|-?\\d+?\\.\\d+");

    private static final Pattern boolPattern = Pattern.compile("^true$|^false$");

    @Override
    boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        //构建 numeric Token
        Matcher matcher = numericPattern.matcher(token);
        if (matcher.matches()) {
            BasicVarToken numeric = new BasicVarToken("numeric", token);
            tokenStream.addToken(numeric);
            return true;
        }
        //构建string Token
        matcher = strPattern.matcher(token);
        if (matcher.find()) {
            BasicVarToken str = new BasicVarToken("str", token);
            tokenStream.addToken(str);
            return true;
        }
        //构建boolean Token
        matcher = boolPattern.matcher(token);
        if (matcher.matches()) {
            BasicVarToken bool = new BasicVarToken("bool", token);
            tokenStream.addToken(bool);
            return true;
        }
        //构建null token
        if (token.equals("null")){
            BasicVarToken _null = new BasicVarToken("null", token);
            tokenStream.addToken(_null);
            return true;
        }
        return false;
    }

    @Override
    int priority() {
        return -1;
    }
}
