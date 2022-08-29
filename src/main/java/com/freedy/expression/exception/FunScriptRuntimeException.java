package com.freedy.expression.exception;

/**
 * @author Freedy
 * @date 2022/8/30 0:16
 */
public class FunScriptRuntimeException extends RuntimeException {
    public FunScriptRuntimeException(String msg) {
        super(msg, null, false, false);
    }
}
