import java.util.Map;

/**
 * @author Freedy
 * @date 2022/9/28 23:14
 */
public class Jdk19Test {


    public static void main(String[] args) {
//        Thread.ofVirtual().start(()->{
//
//        });

        Map<String, String> map = System.getenv();
        map.forEach((k, v) -> System.out.println(k + " = " + v));
    }

}
