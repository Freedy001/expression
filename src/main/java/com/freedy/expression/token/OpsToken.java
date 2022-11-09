package com.freedy.expression.token;


/**
 * @author Freedy
 * @date 2021/12/14 15:50
 */

public final class OpsToken extends ExecutableToken {
    public OpsToken(String value) {
        super("operation", value);
    }
}
