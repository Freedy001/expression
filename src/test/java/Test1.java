import com.freedy.expression.core.Expression;
import com.freedy.expression.log.LogRecorder;
import com.freedy.expression.stander.StanderEvaluationContext;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * @author Freedy
 * @date 2022/7/15 14:57
 */
public class Test1 {
    public static void main(String[] args) {
        LogRecorder recorder = new LogRecorder();
        for (int i = 0; i < 10; i++) {
            System.out.println("abc" + i);
        }
        System.err.println(recorder.getLog());
    }
}
