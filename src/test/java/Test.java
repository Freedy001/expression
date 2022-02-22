import com.freedy.expression.Expression;
import com.freedy.expression.ExpressionPasser;
import com.freedy.expression.exception.ExpressionSyntaxException;
import com.freedy.expression.function.Consumer;
import com.freedy.expression.function.Runnable;
import com.freedy.expression.stander.StanderEvaluationContext;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author Freedy
 * @date 2022/1/19 11:52
 */
public class Test {


    public static void main(String[] args) {
        StanderEvaluationContext context = new StanderEvaluationContext();

        context.registerFunction("try_catch", (Consumer._3ParameterConsumer<Runnable, Class<? extends Throwable >, Consumer._1ParameterConsumer<Throwable>>) (_try, _exception, _catch) -> {
            try {
                _try.run();
            } catch (Throwable ex) {
                if (_exception.isInstance(ex)) {
                    _catch.accept(ex);
                }
            }
        });
        ExpressionPasser parser=new ExpressionPasser();						 //创建解析器
        Expression express=parser.parseExpression("""
                def exType=class("Exception");
                try_catch(newInterface('com.freedy.expression.function.Runnable','run',@block{
                	10/0;
                }),exType,newInterface('com.freedy.expression.function.Consumer$_1ParameterConsumer','accept','ex',@block{
                	print(ex);
                }));
                """);   //输入语句
        express.getValue(context);
    }


}
