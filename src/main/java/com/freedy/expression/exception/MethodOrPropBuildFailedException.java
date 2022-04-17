package com.freedy.expression.exception;

import lombok.Getter;

/**
 * @author Freedy
 * @date 2022/4/17 10:30
 */
public class MethodOrPropBuildFailedException extends Exception{
    @Getter
    private final String methodOrPropName;
    public MethodOrPropBuildFailedException(String message,String methodOrPropName) {
        super(message);
        this.methodOrPropName=methodOrPropName;
    }
}
