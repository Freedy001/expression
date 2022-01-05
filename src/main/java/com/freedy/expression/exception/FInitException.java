package com.freedy.expression.exception;

/**
 * @author Freedy
 * @date 2021/12/4 21:57
 */
public class FInitException extends FException {
    public FInitException(Throwable cause) {
        super(cause);
    }

    public FInitException(String msg, Object... placeholder) {
        super(msg, placeholder);
    }
}
