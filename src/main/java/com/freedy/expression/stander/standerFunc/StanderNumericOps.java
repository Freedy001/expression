package com.freedy.expression.stander.standerFunc;

import com.freedy.expression.stander.ExpressionFunc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Freedy
 * @date 2022/3/6 15:40
 */
public class StanderNumericOps extends AbstractStanderFunc {

    @ExpressionFunc("generate a list start with param 1 and end with param 2")
    public List<Integer> range(Integer a, Integer b) {
        return stepRange(a, b, 1);
    }

    @ExpressionFunc("generate a list start with param 1 and end with param 2 and each interval param 3")
    public List<Integer> stepRange(Integer a, Integer b, Integer step) {
        ArrayList<Integer> list = new ArrayList<>();
        for (int i = a; i <= b; i += step) {
            list.add(i);
        }
        return list;
    }

    @ExpressionFunc(value = "transfer to int")
    public int _int(Object o) {
        if (o == null) return 0;
        if (o instanceof Character c){
            return c;
        }
        return new BigDecimal(o.toString()).setScale(0, RoundingMode.DOWN).intValue();
    }

}
