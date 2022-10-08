import com.freedy.expression.core.TokenStream;
import com.freedy.expression.token.BasicVarToken;
import com.freedy.expression.token.Token;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Freedy
 * @date 2022/7/15 14:57
 */
public class Test1 {
    public static void main(String[] args) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("netstat", "-ano").redirectErrorStream(true);
        Process start = pb.start();
        for (int s : Arrays.stream(new String(start.getInputStream().readAllBytes()).split("\n")).flatMap(s -> Arrays.stream(s.split(" "))).map(String::strip).filter(s -> s.contains(":") && s.substring(s.indexOf(":") + 1).matches("\\d+")).map(s->Integer.parseInt(s.substring(s.indexOf(":") + 1))).collect(Collectors.toSet())) {
            System.out.println(s);
        }
    }

    public static void t(lambdaA a) {
        a.test(new BasicVarToken());
    }

    public static void t(lambdaB a) {
        a.test(new TokenStream(""));
    }

    public interface lambdaA {
        void test(Token token);
    }

    public interface lambdaB {
        void test(TokenStream token);
    }
}
