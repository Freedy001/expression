package com.freedy.expression.utils;


import com.freedy.expression.exception.IllegalArgumentException;
import com.freedy.expression.standard.Func;
import com.freedy.expression.standard.standardFunc.StandardUtils;
import lombok.SneakyThrows;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * 反射工具类.
 *
 * @author Freedy
 * @date 2021/12/2 16:01
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ReflectionUtils {

    public static String getLocalJarPath() {
        URL localUrl = StandardUtils.class.getProtectionDomain().getCodeSource().getLocation();
        String path;
        path = URLDecoder.decode(localUrl.getFile().replace("+", "%2B"), StandardCharsets.UTF_8);
        File file = new File(path);
        path = file.getAbsolutePath();
        return path;
    }

    /**
     * 通过getter方法获取指定字段名的值
     *
     * @param object    需要被获取的对象
     * @param fieldName 字段值
     * @return 对应字段的值 没有则返回null
     */
    public static Object getter(Object object, String fieldName) {
        return getter(object.getClass(), object, fieldName);
    }

    public static Object getter(Class<?> objectClass, Object object, String fieldName) {
        try {
            if (object == null) {
                return getFieldVal(objectClass, null, fieldName);
            } else {
                String getterMethodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                return objectClass.getMethod(getterMethodName).invoke(object);
            }
        } catch (NoSuchMethodException e) {
            return getFieldVal(objectClass, object, fieldName);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("get value failed,because ?", e);
        }
    }

    public static <T> T copyReference(Object source, Class<T> destClass) {
        if (source == null) return null;
        Class<?> sourceClass = source.getClass();
        T dest;
        try {
            dest = destClass.getDeclaredConstructor().newInstance();
            for (Field destField : getFieldsRecursion(destClass)) {
                Field sourceField = getFieldRecursion(sourceClass, destField.getName());
                if (sourceField == null) continue;
                if (destField.getType().isAssignableFrom(sourceField.getType())) {
                    sourceField.setAccessible(true);
                    destField.setAccessible(true);
                    destField.set(dest, sourceField.get(source));
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("copy reference failed,because ?", e);
        }
        return dest;
    }


    public static Object getFieldVal(Class<?> objectClass, Object object, String fieldName) {
        try {
            if (objectClass.getName().startsWith("[") && fieldName.equals("length")) {
                return Array.getLength(object);
            }
            if (fieldName.equals("class")) return objectClass;
            Field field = getFieldRecursion(objectClass, fieldName);
            if (field == null) {
                throw new IllegalArgumentException("no such field: ?", fieldName);
            }
            field.setAccessible(true);
            return field.get(object);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception ex) {
            throw new IllegalArgumentException("get value failed! ?", object == null ? "Is this field static?" : "", ex);
        }
    }

    /**
     * 调用setter方法对相应字段进行设置
     *
     * @param object     需要被设置字段的对象
     * @param fieldName  字段名
     * @param fieldValue 需要设置的值
     */
    public static void setter(Object object, String fieldName, Object fieldValue) {
        setter(object.getClass(), object, fieldName, fieldValue);
    }

    public static void setter(Class<?> objectClass, Object object, String fieldName, Object fieldValue) {
        try {
            if (object == null) {
                setFieldValue(objectClass, null, fieldName, fieldValue);
            } else {
                String setterMethodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                objectClass.getMethod(setterMethodName, getFieldRecursion(object.getClass(), fieldName).getType()).invoke(object, fieldValue);
            }
        } catch (NoSuchMethodException e) {
            setFieldValue(objectClass, object, fieldName, fieldValue);
        } catch (Exception e) {
            throw new IllegalArgumentException("set value failed,because ?", e);
        }
    }


    public static void setFieldValue(Class<?> objectClass, Object object, String fieldName, Object fieldValue) {
        try {
            Field field = getFieldRecursion(objectClass, fieldName);
            if (field == null) {
                throw new IllegalArgumentException("no such field: ?", fieldName);
            }
            field.setAccessible(true);
            field.set(object, fieldValue);
        } catch (Exception ex) {
            throw new IllegalArgumentException("set value failed,because ?", ex);
        }
    }


    public static <T extends Annotation> List<Field> getFieldsByAnnotationValue(Class<?> clazz, Class<T> annotationClazz, String regex) {
        try {
            List<Field> list = new ArrayList<>();
            Method valueMethod = annotationClazz.getDeclaredMethod("value");
            for (Field field : getFieldsRecursion(clazz)) {
                T annotation = field.getAnnotation(annotationClazz);
                if (annotation == null) continue;
                Object o = valueMethod.invoke(annotation);
                String realValue;
                if (o instanceof String) {
                    realValue = (String) o;
                } else if (o instanceof String[]) {
                    realValue = ((String[]) o)[0];
                } else throw new UnsupportedOperationException("仅支持注解类型为String的操作");
                if (realValue.matches(regex)) {
                    list.add(field);
                }
            }
            return list.size() == 0 ? null : list;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException("当前注解不包含value字段");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Set<Method> getMethodsRecursion(Class<?> clazz) {
        Set<Method> list = new HashSet<>();
        for (Class<?> aClass : getClassRecursion(clazz)) {
            list.addAll(List.of(aClass.getDeclaredMethods()));
        }
        for (Class<?> aClass : getInterfaceRecursion(clazz)) {
            list.addAll(List.of(aClass.getDeclaredMethods()));
        }
        return list;
    }

    public static List<Field> getStaticFieldsRecursion(Class clazz) {
        return getFieldsRecursion(clazz).stream().filter(field -> Modifier.isStatic(field.getModifiers())).toList();
    }

    public static List<Method> getStaticMethodsRecursion(Class clazz) {
        return getMethodsRecursion(clazz).stream().filter(method -> Modifier.isStatic(method.getModifiers())).toList();
    }

    /**
     * 获取包括父类在内的所有Field对象
     */
    public static List<Field> getFieldsRecursion(Class clazz) {
        List<Field> list = new ArrayList<>();
        for (Class aClass : getClassRecursion(clazz)) {
            list.addAll(Arrays.asList(aClass.getDeclaredFields()));
        }
        return list;
    }

    /**
     * 获取包括父类在内的指定Field对象
     */
    public static Field getFieldRecursion(Class clazz, String fieldName) {
        Field field = null;
        try {
            field = clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException ignored) {
        }
        if (field == null) {
            Class superclass = clazz.getSuperclass();
            while (superclass != null && field == null) {
                try {
                    field = superclass.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ignored) {
                }
                superclass = superclass.getSuperclass();
            }
        }
        return field;
    }

    public static boolean hasField(Object obj, String fieldName) {
        if (obj == null) return false;
        return getFieldRecursion(obj.getClass(), fieldName) != null;
    }

    public static boolean hasStaticField(Class<?> clazz, String fieldName) {
        if (clazz == null || fieldName == null) return false;
        return Modifier.isStatic(getFieldRecursion(clazz, fieldName).getModifiers());
    }

    /**
     * 通过递归获取注解
     *
     * @param clazz           要进行扫描的类
     * @param annotationClass 注解类型
     * @param fieldName       注解类型
     */
    public static <T extends Annotation, A> List<T> getAnnotationRecursion(Class<A> clazz, Class<T> annotationClass, ElementType... fieldName) {
        List<T> annotationList = new ArrayList<>();
        Set<Class<?>> superclass = getClassRecursion(clazz);
        if (fieldName != null) {
            Arrays.stream(fieldName).distinct().toList().forEach(type -> {
                switch (type) {
                    case TYPE:
                        for (Class aClass : superclass) {
                            T annotation = (T) aClass.getAnnotation(annotationClass);
                            if (annotation != null)
                                annotationList.add(annotation);
                        }
                        break;
                    case METHOD:
                        for (Class aClass : superclass) {
                            for (Method method : aClass.getDeclaredMethods()) {
                                T annotation = method.getAnnotation(annotationClass);
                                if (annotation != null)
                                    annotationList.add(annotation);
                            }
                        }
                        break;
                    case FIELD:
                        for (Class aClass : superclass) {
                            for (Field field : aClass.getDeclaredFields()) {
                                T annotation = field.getAnnotation(annotationClass);
                                if (annotation != null)
                                    annotationList.add(annotation);
                            }
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("此方法仅支持type、field、method类型");
                }
            });
        } else {
            for (Class aClass : superclass) {
                T fieldAnnotation = (T) aClass.getAnnotation(annotationClass);
                if (fieldAnnotation != null)
                    annotationList.add(fieldAnnotation);
                for (Method method : aClass.getDeclaredMethods()) {
                    T annotation = method.getAnnotation(annotationClass);
                    if (annotation != null)
                        annotationList.add(annotation);
                }
                for (Field field : aClass.getDeclaredFields()) {
                    T annotation = field.getAnnotation(annotationClass);
                    if (annotation != null)
                        annotationList.add(annotation);
                }
            }
        }
        return annotationList;
    }

    /**
     * 获取该类及所有他的父类
     */
    public static Set<Class<?>> getClassRecursion(Class<?> clazz) {
        Set<Class<?>> list = new HashSet<>();
        list.add(clazz);
        Class superclass = clazz.getSuperclass();
        while (superclass != null) {
            list.add(superclass);
            superclass = superclass.getSuperclass();
        }
        return list;
    }

    /**
     * 获取该类及所有他的父类
     */
    public static Set<Class<?>> getClassRecursion(Object o) {
        return getClassRecursion(o.getClass());
    }

    /**
     * 判断某个类是不是指定接口的实现类
     *
     * @param clazz               需要被判断的类
     * @param fatherInterfaceName 用于判断是否是类的实现接口
     * @return 是不是指定接口的实现类
     */
    public static boolean isSonInterface(Class<?> clazz, String... fatherInterfaceName) {
        Stack<Class<?>> stack = new Stack<>();
        stack.push(clazz);
        for (Class<?> aClass : clazz.getInterfaces()) {
            stack.push(aClass);
        }
        while (!stack.isEmpty()) {
            Class<?> pop = stack.pop();
            for (String s : fatherInterfaceName) {
                if (pop.getName().equals(s)) return true;
            }
            for (Class<?> popInterface : pop.getInterfaces()) stack.push(popInterface);
        }
        return false;
    }

    public static Set<Class<?>> getInterfaceRecursion(Object o) {
        Set<Class<?>> set = new HashSet<>();
        getInterfaceRecursion(o.getClass(), set);
        return set;
    }

    public static Set<Class<?>> getInterfaceRecursion(Class<?> clazz) {
        Set<Class<?>> set = new HashSet<>();
        getInterfaceRecursion(clazz, set);
        return set;
    }


    public static void getInterfaceRecursion(Class<?> clazz, Collection<Class<?>> interfaces) {
        for (Class<?> aClass : clazz.getInterfaces()) {
            interfaces.add(aClass);
            getInterfaceRecursion(aClass, interfaces);
        }
    }


    public static boolean isRegularType(Class<?> type) {
        switch (type.getSimpleName()) {
            case "Integer", "int", "Long", "long", "Double", "double", "Float", "float", "Boolean", "boolean", "short", "Short", "Byte", "byte", "Character", "char", "String" -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public static boolean isBasicType(Class<?> type) {
        switch (type.getSimpleName()) {
            case "int", "long", "double", "float", "boolean", "short", "byte", "char", "void" -> {
                return true;
            }
        }
        return false;
    }

    public static Class<?> convertToWrapper(Class<?> type) {
        switch (type.getSimpleName()) {
            case "int" -> {
                return Integer.class;
            }
            case "long" -> {
                return Long.class;
            }
            case "double" -> {
                return Double.class;
            }
            case "float" -> {
                return Float.class;
            }
            case "boolean" -> {
                return Boolean.class;
            }
            case "short" -> {
                return Short.class;
            }
            case "byte" -> {
                return Byte.class;
            }
            case "char" -> {
                return Character.class;
            }
        }
        return type;
    }

    public static <T> T convertType(Object strValue, Class<T> type) {
        if (strValue == null) return null;
        Object returnValue;
        switch (type.getSimpleName()) {
            case "Integer", "int" -> returnValue = Integer.parseInt(strValue.toString());
            case "Long", "long" -> returnValue = Long.parseLong(strValue.toString());
            case "Double", "double" -> returnValue = Double.parseDouble(strValue.toString());
            case "Float", "float" -> returnValue = Float.parseFloat(strValue.toString());
            case "Boolean", "boolean" -> returnValue = Boolean.parseBoolean(strValue.toString());
            case "short", "Short" -> returnValue = Short.parseShort(strValue.toString());
            case "Byte", "byte" -> returnValue = Byte.parseByte(strValue.toString());
            case "Character", "char" -> returnValue = strValue.toString().charAt(0);
            case "String", "Object" -> returnValue = strValue.toString();
            default -> throw new UnsupportedOperationException("can not convert String to " + type.getName());
        }
        return (T) returnValue;
    }


    @SneakyThrows
    public static Collection<Object> buildCollectionByType(Class<?> interfaceType) {
        if (!isSonInterface(interfaceType, "java.util.Collection"))
            throw new IllegalArgumentException("the type you give is not a collection type or son of collection type!");

        if (!interfaceType.isInterface()) {
            return (Collection<Object>) interfaceType.getConstructor().newInstance();
        }
        switch (interfaceType.getName()) {
            case "java.util.List" -> {
                return new ArrayList<>();
            }
            case "java.util.Set" -> {
                return new HashSet<>();
            }
            case "java.util.Queue" -> {
                return new ArrayDeque<>();
            }
            default ->
                    throw new UnsupportedOperationException("unsupported type for type ?,please change a supported(List,Set,Queue) type");
        }
    }


    /**
     * 通过filed对象 与需要被设置的值进行 collection的构建
     *
     * @param field 通过field来获取满足其泛型的collection
     * @param arg   用于对新collection进行赋值
     * @return 新collection
     */
    public static Collection<Object> buildCollectionByFiledAndValue(Field field, String[] arg) {
        return buildCollectionByTypeAndValue(field.getGenericType(), arg);
    }

    public static Collection<Object> buildCollectionByTypeAndValue(Type type, String[] arg) {
        Collection<Object> collection;

        if (type instanceof ParameterizedType parameterizedType) {
            Type[] genericType = parameterizedType.getActualTypeArguments();
            Class<?> listType = (Class<?>) genericType[0];
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            //创建实例
            collection = ReflectionUtils.buildCollectionByType(rawType);
            if (arg == null) return collection;
            for (String s : arg) {
                collection.add(ReflectionUtils.convertType(s, listType));
            }
        } else {
            //没有声明泛型 默认string
            //创建实例
            collection = ReflectionUtils.buildCollectionByType((Class<?>) type);
            if (arg == null) return collection;
            collection.addAll(Arrays.asList(arg));
        }
        return collection;
    }

    /**
     * 通过给定filed和给定map对来构建新的map。
     * 其构建过程为，对给定map的符合给点前缀的键进行对新map赋值
     *
     * @param field  通过field来获取满足其泛型的map
     * @param prefix 前缀
     * @param valMap 用于给新map赋值的mao
     * @return 新构建的map
     */
    @SuppressWarnings("unchecked")
    public static Map<Object, Object> buildMapByFiledAndValue(Field field, String prefix, Map<String, String> valMap) throws Exception {
        Map<Object, Object> map;
        Class<?>[] _1stType = new Class[1];
        Class<?>[] _2ndType = new Class[1];
        if (field.getGenericType() instanceof ParameterizedType parameterizedType) {
            Type[] genericType = parameterizedType.getActualTypeArguments();
            _1stType[0] = (Class<?>) genericType[0];
            _2ndType[0] = (Class<?>) genericType[1];
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            if (!rawType.isInterface()) {
                //非interface
                map = (Map<Object, Object>) rawType.getConstructor().newInstance();
            } else {
                map = new HashMap<>();
            }
            valMap.forEach((key, v) -> {
                if (key.startsWith(prefix)) {
                    String mapKey = key.substring(prefix.length() + 1);
                    map.put(ReflectionUtils.convertType(mapKey, _1stType[0]), ReflectionUtils.convertType(v, _2ndType[0]));
                }
            });

        } else {
            //没有声明泛型 默认string.string
            map = new HashMap<>();
            valMap.forEach((key, v) -> {
                if (key.startsWith(prefix)) {
                    String mapKey = key.substring(prefix.length() + 1);
                    map.put(mapKey, v);
                }
            });
        }
        return map;
    }

    /**
     * 从arrClass和val字符串中构建数组
     */
    public static Object buildArrByArrFieldAndVal(Class<?> type, String[] valArr) {
        if (!type.isArray() || valArr == null) return null;
        Object fieldVal;
        Class<?> arrayType = type.getComponentType();
        int arrLen = valArr.length;
        Object newArray = Array.newInstance(arrayType, arrLen);
        for (int i = 0; i < arrLen; i++) {
            Array.set(newArray, i, ReflectionUtils.convertType(valArr[i], arrayType));
        }
        fieldVal = newArray;
        return fieldVal;
    }

    /**
     * 检测一个对象是否所有字段都为空
     *
     * @param obj 需要被检测的字段
     * @return 返回null表示不是所有字段为空  返回field表示为空的那个field
     */
    @SneakyThrows
    public static Field checkNull(Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object fieldObj = field.get(obj);
            if (fieldObj == null) return field;
            if (!isRegularType(field.getType())) {
                Field checkNone = checkNull(fieldObj);
                if (checkNone != null) {
                    return field;
                }
            }
        }
        return null;
    }


    public static Object invokeMethod(String methodName, Object target, Object... args) throws Throwable {
        return invokeMethod(methodName, target.getClass(), target, args);
    }

    public static Object invokeMethod(String methodName, Class<?> targetClass, Object target, Object... args) throws Throwable {
        List<Executable> list = new ArrayList<>();
        List<Executable> similar = new ArrayList<>();
        if (methodName.equals("<init>")) {
            list.addAll(List.of(targetClass.getDeclaredConstructors()));
        } else {
            for (Method method : getMethodsRecursion(targetClass)) {
                String name = method.getName();
                if (name.equals(methodName)) {
                    list.add(method);
                }
                if (name.contains(methodName) || methodName.contains(name)) {
                    similar.add(method);
                }
            }
        }
        Map<Integer, Class<?>> lambdaIndex = new HashMap<>();
        for (Executable method : list) {
            lambdaIndex.clear();
            int count;
            boolean isVar = method.isVarArgs();
            if ((count = method.getParameterCount()) == args.length || isVar) {
                Class<?>[] clazz = method.getParameterTypes();
                int i = 0;
                for (; i < count; i++) {
                    Class<?> originMethodArgs = convertToWrapper(clazz[i]);
                    if (i == count - 1 && isVar && originMethodArgs.isArray()) {
                        args = convertArgsForVarArgsMethod(count, method, args);
                    } else {
                        Class<?> supplyMethodArgs = convertToWrapper(args.length == 0 ? null : args[i] == null ? clazz[i] : args[i].getClass());
                        if (!originMethodArgs.isAssignableFrom(supplyMethodArgs)) {
                            Object o = tryConvert(originMethodArgs, args[i]);
                            if (o != Boolean.FALSE) {
                                args[i] = o;
                            } else {
                                Func.LambdaAdapter adapter;
                                if (args[i] instanceof Func f && (adapter = f.getAdapter()) != null) {
                                    Class<?> type = adapter.getInterfaceType();
                                    if ((type != null && !originMethodArgs.isAssignableFrom(type)) || adapter.notFunctional(originMethodArgs)) {
                                        break;
                                    }
                                    lambdaIndex.put(i, originMethodArgs);
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
                if (i == count) {
                    method.setAccessible(true);
                    //参数lambda参数替换
                    if (lambdaIndex.size() > 0) {
                        args = Arrays.copyOf(args, args.length, Object[].class);
                        Func.LambdaAdapter adapter;
                        for (Map.Entry<Integer, Class<?>> entry : lambdaIndex.entrySet()) {
                            if (args[entry.getKey()] instanceof Func f && (adapter=f.getAdapter())!=null) {
                                args[entry.getKey()] = adapter.getInstance(entry.getValue());
                            }
                        }
                    }
                    try {
                        if (method instanceof Method m) return m.invoke(target, args);
                        if (method instanceof Constructor m) return m.newInstance(args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause() != null ? e.getCause() : e;
                    }
                }
            }
        }
        if (methodName.equals("getClass") && args.length == 0) return targetClass;
        throw thr(methodName, targetClass, similar, args);
    }

    private static Object[] convertArgsForVarArgsMethod(int count, Executable e, Object[] args) {
//        if (count == 1) return args;
        Object[] nArgs = new Object[count];
        //拷贝非可变参数参数
        System.arraycopy(args, 0, nArgs, 0, count - 1);
        //检测可变参数数组的所有元素是否都是相同类型
        Class<?> eleType = checkArrType(args, count - 1, args.length);
        Object[] varArg;
        //拷贝可变数组
        if (eleType != null) {
            //noinspection unchecked
            varArg = Arrays.copyOfRange(args, count - 1, args.length, (Class<Object[]>) eleType.arrayType());
        } else {
            varArg = Arrays.copyOfRange(args, count - 1, args.length, (Class<Object[]>) e.getParameters()[count - 1].getType());
        }
        nArgs[count - 1] = varArg;
        args = nArgs;
        return args;
    }

    private static Class<?> checkArrType(Object[] arr, int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException("start index ? gt end index ?", start, end);
        }
        if (start >= arr.length) {
            return null;
        }
        Class<?> cl = arr[start].getClass();
        for (int i = start + 1; i < end; i++) {
            if (arr[i].getClass() != cl) {
                return null;
            }
        }
        return cl;
    }

    private static Exception thr(String methodName, Class<?> targetClass, List<Executable> similar, Object[] args) throws NoSuchMethodException {
        StringJoiner argStr = new StringJoiner(",", "(", ")");
        for (Object arg : args) {
            argStr.add(arg == null ? "null" : arg.getClass().getSimpleName());
        }
        int length = methodName.length();
        similar.sort(Comparator.comparing(o -> Math.abs(o.getName().length() - length)));
        return new NoSuchMethodException(new PlaceholderParser(
                "no such executable ? in\033[34m ? !\033[0;39myou can call these similar method: ?*",
                methodName + argStr,
                targetClass.getSimpleName(),
                similar.stream().map(method -> {
                    StringJoiner argString = new StringJoiner(",", "(", ")");
                    for (Parameter arg : method.getParameters()) {
                        argString.add(arg.getType().getSimpleName() + " " + arg.getName());
                    }
                    return method.getName() + argString;
                }).toList()
        ).serialParamsSplit(" , ")
                .ifEmptyFillWith("not find matched method")
                .configPlaceholderHighLight(PlaceholderParser.PlaceholderHighLight.HIGH_LIGHT_BLUE)
                .toString()
        );
    }


    @SneakyThrows
    public static <T> T copyProperties(T target, String... excludes) {
        Class<T> targetClass = (Class<T>) target.getClass();
        Set<String> excludeSet = Set.of(excludes);
        Object instance = targetClass.getConstructor().newInstance();
        for (Class<?> aClass : getClassRecursion(targetClass)) {
            for (Field field : aClass.getDeclaredFields()) {
                if (excludeSet.contains(field.getName())) continue;
                field.setAccessible(true);
                try {
                    field.set(instance, field.get(target));
                } catch (Exception ignored) {
                }
            }
        }
        return targetClass.cast(instance);
    }

    private static final Map<String, Set<String>> convertableMap = new HashMap<>();

    static {
        convertableMap.put("java.lang.Long", Set.of("java.lang.Integer", "java.lang.Short", "int", "long", "short"));
        convertableMap.put("java.lang.Integer", Set.of("java.lang.Long", "java.lang.Short", "int", "long", "short"));
        convertableMap.put("java.lang.Short", Set.of("java.lang.Integer", "java.lang.Long", "int", "long", "short"));
        convertableMap.put("java.lang.Double", Set.of("java.lang.Float", "double", "float"));
        convertableMap.put("java.lang.Float", Set.of("java.lang.Double", "double", "float"));
    }


    public static boolean isConvertable(Class<?> originMethodArgs, Class<?> supplyMethodArgs) {
        return Optional.ofNullable(convertableMap.get(originMethodArgs.getName())).orElse(Set.of()).contains(supplyMethodArgs.getName());
    }

    public static Object tryConvert(Class<?> originMethodArgs, Object obj) {
        if (obj == null) return null;
        if (!isConvertable(originMethodArgs, obj.getClass())) return false;
        try {
            Method valueOf = originMethodArgs.getMethod("valueOf", String.class);
            return valueOf.invoke(null, obj + "");
        } catch (Throwable ignored) {
        }
        return false;
    }

}

