package com.freedy.expression.stander;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Freedy
 * @date 2022/3/6 14:45
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExpressionFunc {
    //注释
    String value() default "";

    boolean enableCMDParameter() default false;

}