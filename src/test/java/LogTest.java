import com.freedy.expression.log.LogRecorder;

/**
 * @author Freedy
 * @date 2022/8/9 0:16
 */
public class LogTest {
    static LogRecorder recorder = new LogRecorder();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("TEST0");
        System.out.println("TEST1");
        System.out.println("TEST2");
        System.out.println("TEST3");
        System.out.println("TEST4");
        System.out.println("TEST5");
        System.out.println("TEST6");
        System.out.println("TEST7");
        System.out.println("TEST8");
        System.out.println("TEST9");
        System.out.println(recorder.getLog());
        recorder=null;
        System.gc();
        Thread.sleep(1000);
        System.out.println("TEST0");
        System.out.println("TEST1");
        System.out.println("TEST2");
        System.out.println("TEST3");
        System.out.println("TEST4");
        System.out.println("TEST5");
        System.out.println("TEST6");
        System.out.println("TEST7");
        System.out.println("TEST8");
        System.out.println("TEST9");
    }

}
