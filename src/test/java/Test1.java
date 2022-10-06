import com.freedy.expression.core.TokenStream;
import com.freedy.expression.log.LogRecorder;
import com.freedy.expression.token.BasicVarToken;
import com.freedy.expression.token.Token;

/**
 * @author Freedy
 * @date 2022/7/15 14:57
 */
public class Test1 {
    public static void main(String[] args) {

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
