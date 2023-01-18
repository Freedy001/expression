import com.freedy.expression.core.Expression;
import com.freedy.expression.core.token.ExecutableToken;
import com.freedy.expression.standard.standardFunc.StandardUtils;
import lombok.SneakyThrows;
import lombok.ToString;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author Freedy
 * @date 2022/9/9 17:42
 */
public class ByteBuddyTest {
    @SneakyThrows
    public static void main(String[] args) {
//        System.out.println(Expression.class.getClassLoader());
//        System.out.println(ByteBuddy.class.getClassLoader());
//        /*Class<?> aClass = */new ByteBuddy()
//                .rebase(TokenStream.class)
//                .name("Hello")
//
//
//                .method(ElementMatchers.named("splitStream"))
//                .intercept(FixedValue.value("Hello World!"))
//                .make()
//                .saveIn(new File("abc.class"));
////                .load(ByteBuddyTest.class.getClassLoader())
////                .getLoaded();
//        System.out.println(ByteBuddyTest.class.getName());
//        System.out.println(aClass.getName());
//        System.out.println(aClass.getConstructors()[0].newInstance(""));
//        System.in.read();
//        context.setVariable("a", aClass);
//        System.out.println(ex.getValue("""
//                for(i:lf# #a){
//                    print(i);
//                };
//                """));
//        ClassLoader loader = new ClassLoader() {
//            @SneakyThrows
//            @Override
//            protected Class<?> findClass(String name) throws ClassNotFoundException {
//                byte[] bytes = new FileInputStream("C:\\Users\\Freedy\\Desktop\\code\\expression\\abc.class\\Hello.class").readAllBytes();
//                return defineClass(name, bytes, 0, bytes.length);
//            }
//        };
//        Class<?> hello1 = loader.loadClass("Hello");
//        System.out.println(hello1);
//        Class<?> hello = Class.forName("Hello");
//        System.out.println(hello);
        testMethod();
    }

    public static void testUnloadedClass() {
        TypePool typePool = TypePool.Default.ofSystemLoader();
        Class bar = new ByteBuddy()
                .redefine(typePool.describe("com.freedy.expression.core.Expression").resolve(), // do not use 'Bar.class'
                        ClassFileLocator.ForClassLoader.ofSystemLoader())
                .defineField("qux", ExecutableToken.class) // we learn more about defining fields later
                .make()
                .load(ClassLoader.getSystemClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        new StandardUtils().lf(Expression.class);
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        new AgentBuilder.Default()
                .type(isAnnotatedWith(ToString.class))
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, ProtectionDomain protectionDomain) {
                        return builder.method(named("toString"))
                                .intercept(FixedValue.value("transformed"));
                    }

                }).installOn(instrumentation);
    }

    public static class Foo {
        public String bar() { return null; }
        public String foo() { return null; }
        public String foo(Object o) { return null; }
    }


    public static void testMethod() throws InstantiationException, IllegalAccessException {
        Foo dynamicFoo = new ByteBuddy()
                .subclass(Foo.class)
                .name("Hello")
                .method(isDeclaredBy(Foo.class)).intercept(FixedValue.value("One!"))
                .method(named("foo")).intercept(FixedValue.value("Two!"))
                .method(named("foo").and(takesArguments(1))).intercept(FixedValue.value("Three!"))
                .make()
                .load(ByteBuddyTest.class.getClassLoader())
                .getLoaded()
                .newInstance();
        System.out.println(dynamicFoo.bar());
        System.out.println(dynamicFoo.foo());
        System.out.println(dynamicFoo.foo(null));
    }

}
