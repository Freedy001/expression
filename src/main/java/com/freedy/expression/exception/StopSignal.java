package com.freedy.expression.exception;

import com.freedy.expression.core.TokenStream;
import lombok.Getter;

/**
 * @author Freedy
 * @date 2021/12/23 21:11
 */
@Getter
public class StopSignal extends FunBaseException {

    String signal;
    TokenStream returnStream;

    public StopSignal(String signal) {
        super(signal);
        this.signal=signal;
    }


    public StopSignal setReturnStream(TokenStream returnStream){
        this.returnStream=returnStream;
        return this;
    }

    public static StopSignal getInnerSignal(Throwable e) {
        if (e instanceof StopSignal signal) return signal;
        if (e == null) return null;
        return getInnerSignal(e.getCause());
    }

}
