import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Freedy
 * @date 2022/1/19 11:52
 */
public class Test {



    public static void main(String[] args) {
        Map<String, String> test = cast(new ArrayList<String>());


        System.out.println(test);
    }

    public static <T> T cast(Object cast){
        return (T)cast;
    }

}
