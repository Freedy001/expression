package com.freedy.expression.exception;

import com.freedy.expression.utils.ReflectionUtils;

/**
 * @author Freedy
 * @date 2022/1/25 12:29
 */
public class BuildFailException extends FunBaseException {

    public BuildFailException(StackTraceElement[] elements,String msg) {
        super(msg);
        ReflectionUtils.setter(this, "stackTrace", elements);
    }
}
