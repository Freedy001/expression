package com.freedy.expression.exception;

import com.freedy.expression.utils.Color;
import com.freedy.expression.utils.PlaceholderParser;
import com.freedy.expression.utils.StringUtils;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;

/**
 * @author Freedy
 * @date 2021/12/4 11:05
 */
public class FunBaseException extends RuntimeException {

    public FunBaseException() {
    }

    public FunBaseException(Throwable cause) {
        super(cause);
    }

    public FunBaseException(String msg) {
        super(msg);
    }

    /**
     * 所有继承还exception的类都具备placeholder解析的能力，并且会自动的设置cause
     * @param msg 异常信息
     * @param placeholder 填充值 请参考 {@link PlaceholderParser}
     */
    @SneakyThrows
    public FunBaseException(String msg, Object... placeholder) {
        try {
            Class<Throwable> aClass = Throwable.class;
            //设置cause
            for (int i = 0; i < placeholder.length; i++) {
                Object o = placeholder[i];
                if (o instanceof Throwable e) {
                    Field cause = aClass.getDeclaredField("cause");
                    cause.setAccessible(true);
                    cause.set(this, o);
                    placeholder[i] = StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
                    break;
                }
            }

            //设置msg
            Field exceptionMsg;
            exceptionMsg = aClass.getDeclaredField("detailMessage");
            exceptionMsg.setAccessible(true);
            exceptionMsg.set(this, new PlaceholderParser(msg, placeholder)
                    .configPlaceholderHighLight(PlaceholderParser.PlaceholderHighLight.HIGH_LIGHT_CYAN)
                    .registerNoneHighLightClass(Throwable.class)
                    .ifEmptyFillWith("")
                    .toString());
        } catch (InaccessibleObjectException e) {
            System.out.println(Color.dRed(new PlaceholderParser("""
                    ? throw a Exception(msg:?,placeholder:?*),see detail Exception please add a vm option then restart and retry!
                    vm option -> --add-opens java.base/java.lang=ALL-UNNAMED""", this.getClass().getSimpleName(), msg, placeholder).ifEmptyFillWith("")));
        }
    }
}
