package com.freedy.expression.core.tokenBuilder;

import com.freedy.expression.core.TokenStream;
import com.freedy.expression.core.token.DotSplitToken;
import com.freedy.expression.core.token.OpsToken;

/**
 * @author Freedy
 * @date 2021/12/27 12:47
 */
public class DotSplitBuilder extends Builder {
    @Override
    public boolean build(TokenStream tokenStream, String token, ExceptionMsgHolder holder) {
        //点分token
        if (tokenStream.getLastToken() == null || !tokenStream.getLastToken().isValue(")")) return false;
        DotSplitToken dotToken = new DotSplitToken(token.substring(1));
        if (token.matches("^\\? *?\\..+")) {
            dotToken.setRelevantOps("?");
            buildExecuteChain(dotToken, token.split("\\.", 2)[1], holder);
            if (holder.isErr) return false;
        } else if (token.startsWith(".")) {
            buildExecuteChain(dotToken, token.substring(1), holder);
            if (holder.isErr) return false;
        } else {
            return false;
        }

        tokenStream.addToken(new OpsToken("."));
        tokenStream.addToken(dotToken);
        return true;
    }

    @Override
    public int priority() {
        return 9;
    }
}
