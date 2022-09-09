import lombok.SneakyThrows;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * @author Freedy
 * @date 2022/9/9 17:42
 */
public class ByteBuddyTest {
    @SneakyThrows
    public static void main(String[] args) {
        Class<?> aClass = new ByteBuddy()
                .subclass(Object.class)
                .method(ElementMatchers.named("toString"))
                .intercept(FixedValue.value("Hello World!"))
                .make()
                .load(ByteBuddyTest.class.getClassLoader())
                .getLoaded();

        System.out.println(aClass.getName());
        System.out.println(aClass.getConstructor().newInstance().toString());

    }




}
