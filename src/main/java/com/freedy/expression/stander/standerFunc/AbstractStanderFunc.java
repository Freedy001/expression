package com.freedy.expression.stander.standerFunc;

import com.freedy.expression.core.Expression;
import com.freedy.expression.stander.CMDParameter;
import com.freedy.expression.stander.StanderEvaluationContext;
import com.freedy.expression.utils.ReflectionUtils;
import lombok.Setter;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

/**
 * @author Freedy
 * @date 2022/3/6 15:09
 */
public abstract class AbstractStanderFunc {
    public final static String CHARSET = System.getProperty("file.encoding") == null ? "UTF-8" : System.getProperty("file.encoding");
    protected final Expression selfExp = new Expression();
    protected final static String SEPARATOR = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "\\\\" : "/";

    @Setter
    protected StanderEvaluationContext context;


    @SneakyThrows
    protected Class<?> getClassByArg(Object arg) {
        if (arg instanceof String s) {
            return context.findClass(s);
        } else {
            if (arg == null) return null;
            return arg.getClass();
        }
    }

    protected static <T> T parseArgs(Class<T> resultClass,String[] args) throws Exception {
        HashMap<String, ValInjector> cmdParamMap = new HashMap<>();
        for (Field field : ReflectionUtils.getFieldsRecursion(resultClass)) {
            CMDParameter cmd = field.getAnnotation(CMDParameter.class);
            if (cmd == null) continue;
            if (!cmd.value().startsWith("-"))
                throw new IllegalArgumentException("illegal parameter name,it should start with '-'");
            cmdParamMap.put(cmd.value(), new ValInjector(field, cmd.value()));
        }
        for (Method method : ReflectionUtils.getMethodsRecursion(resultClass)) {
            CMDParameter cmd = method.getAnnotation(CMDParameter.class);
            if (cmd == null) continue;
            if (!cmd.value().startsWith("-"))
                throw new IllegalArgumentException("illegal parameter name,it should start with '-'");
            cmdParamMap.put(cmd.value(), new ValInjector(method, cmd.value()));
        }
        Object obj = resultClass.getDeclaredConstructor().newInstance();
        int argLen = args.length;
        for (int i = 0; i < argLen; i++) {
            if (!args[i].startsWith("-")) continue;
            ValInjector injector = cmdParamMap.get(args[i]);
            if (injector == null) throw new IllegalArgumentException("unknown parameter" + args[i]);
            int j = i + 1;
            for (; j < argLen; j++) {
                if (args[j].startsWith("-")) break;
            }
            injector.inject(obj, Arrays.copyOfRange(args, i + 1, j));
        }
        return resultClass.cast(obj);
    }


    private static class ValInjector {
        private final Class<?> origClass;
        private final String parameter;
        private Field valField;
        private Method valMethod;

        public ValInjector(Field valField, String parameter) {
            this.valField = valField;
            origClass = valField.getDeclaringClass();
            this.parameter = parameter;
        }

        public ValInjector(Method valMethod, String parameter) {
            this.valMethod = valMethod;
            origClass = valMethod.getDeclaringClass();
            this.parameter = parameter;
        }

        @SneakyThrows
        private void inject(Object o, String[] val) {
            Method setMethod = valMethod;
            if (valField != null) {
                String fieldName = valField.getName();
                String setMethodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                for (Method method : ReflectionUtils.getMethodsRecursion(origClass)) {
                    if (method.getName().equals(setMethodName)) {
                        if (setMethod != null) {
                            setMethod = null;
                            break;
                        }
                        setMethod = method;
                    }
                }
                if (setMethod == null) {
                    if (val.length >= 2) throw new IllegalArgumentException("The number of parameters must be one");
                    valField.set(o, val[0]);
                    return;
                }
            }
            Class<?>[] types = setMethod.getParameterTypes();
            int typeLen = types.length;
            int valLen = val.length;
            if (valLen % typeLen != 0) {
                throw new IllegalArgumentException("wrong number of parameter " + parameter + "'s value! it should be multiple of " + typeLen);
            }
            for (Class<?> type : types) {
                if (!type.getName().contains("java.lang.String"))
                    throw new IllegalArgumentException("can not parse parameter " + parameter + "! because the parameter type of the setter method of the target object is incorrect");
            }
            if (types[typeLen - 1].isArray()) {
                Object[] args=new Object[typeLen];
                System.arraycopy(val,0,args,0,typeLen-1);
                args[typeLen-1]=Arrays.copyOfRange(val,typeLen-1,valLen-1);
                setMethod.invoke(o, args);
            } else {
                int multi = valLen / typeLen;
                for (int i = 0; i < multi; i++) {
                    setMethod.invoke(o, (Object[]) Arrays.copyOfRange(val, i * typeLen, (i + 1) * typeLen));
                }
            }
        }
    }


}
