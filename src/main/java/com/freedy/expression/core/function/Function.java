package com.freedy.expression.core.function;

/**
 * @author Freedy
 * @date 2021/12/24 16:59
 */
public interface Function extends Functional {

    @FunctionalInterface
    interface _1ParameterFunction<A, Z> extends Function {
        Z apply(A param1) throws Exception;
    }

    @FunctionalInterface
    interface _2ParameterFunction<A, B, Z> extends Function {
        Z apply(A param1, B param2) throws Exception;
    }

    @FunctionalInterface
    interface _3ParameterFunction<A, B, C, Z> extends Function {
        Z apply(A param1, B param2, C param3) throws Exception;
    }

    @FunctionalInterface
    interface _4ParameterFunction<A, B, C, D, Z> extends Function {
        Z apply(A param1, B param2, C param3, D param4) throws Exception;
    }

    @FunctionalInterface
    interface _5ParameterFunction<A, B, C, D, E, Z> extends Function {
        Z apply(A param1, B param2, C param3, D param4, E param5) throws Exception;
    }

    @FunctionalInterface
    interface _6ParameterFunction<A, B, C, D, E, F, Z> extends Function {
        Z apply(A param1, B param2, C param3, D param4, E param5, F param6) throws Exception;
    }

    @FunctionalInterface
    interface _7ParameterFunction<A, B, C, D, E, F, G, Z> extends Function {
        Z apply(A param1, B param2, C param3, D param4, E param5, F param6, G param7) throws Exception;
    }

    @FunctionalInterface
    interface _9ParameterFunction<A, B, C, D, E, F, G, Z> extends Function {
        Z apply(A param1, B param2, C param3, D param4, E param5, F param6, G param7, G param8) throws Exception;
    }

    @FunctionalInterface
    interface _10ParameterFunction<A, B, C, D, E, F, G, H, Z> extends Function {
        Z apply(A param1, B param2, C param3, D param4, E param5, F param6, G param7, G param8, H param9) throws Exception;
    }
}
