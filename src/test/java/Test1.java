import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2022/7/15 14:57
 */
public class Test1 {
    public static void main(String[] args) {
        System.out.println(Arrays.toString(Pattern.compile("aa,bb,cc").split(",")));
    }
}
