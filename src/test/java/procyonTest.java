/**
 * @author Freedy
 * @date 2022/9/3 12:23
 */

public class procyonTest {
    public static void main(String[] args) {
        test();
    }

    public static void test1(){
        // This lambda closes over a complex constant (a String array).
//        final ConstantExpression items = constant(
//                new String[]{"one", "two", "three", "four", "five"}
//        );
        // If written in Java, the constructed expression would look something like this:
        // () -> {
        //     for (String item : <closure>items)
        //         System.out.printf("Got item: %s\n", item);
        // }
//        final ParameterExpression item = variable(Types.String, "item");
//        final LambdaExpression<Runnable> runnable = lambda(
//                Type.of(Runnable.class),
//                forEach(
//                        item,
//                        items,
//                        call(
//                                field(null, Types.System.getField("out")),
//                                "printf",
//                                constant("Got item: %s\n"),
//                                item
//                        )
//                )
//        );
//        System.out.println(runnable);
//        final Runnable delegate = runnable.compile();
//        delegate.run();
    }

    public static void test(){
//        final Type<Map> map = Type.of(Map.class);
//        final Type<?> rawMap = map.getErasedType();
//        final Type<Map<String, Integer>> boundMap = map.makeGenericType(Types.String, Types.Integer);
//
//
//        System.out.println(map.getDeclaredMethods().get(1));
//        System.out.println(rawMap.getDeclaredMethods().get(1));
//        System.out.println(boundMap.getDeclaredMethods().get(1));
//
//
//        System.out.println(boundMap.getGenericTypeParameters());
//        System.out.println(boundMap.getTypeArguments());
    }
}
