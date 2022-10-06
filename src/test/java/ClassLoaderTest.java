/**
 * @author Freedy
 * @date 2022/9/15 23:05
 */
public class ClassLoaderTest {



    public static void main(String[] args) throws ClassNotFoundException {
        ClassLoader loader1 = new ClassLoader() {};
        ClassLoader loader2 = new ClassLoader() {};
        Class<?> foo = loader1.loadClass("Foo1");
        Class<?> bar = loader2.loadClass("Bar");

        System.out.println(foo.getClassLoader());
        System.out.println(bar.getClassLoader());
    }
}
