import com.freedy.expression.utils.ReflectionUtils;
import jdk.internal.misc.Unsafe;

import java.util.ArrayList;

/**
 * @author Freedy
 * @date 2022/1/19 11:52
 */
public class Test {


    public static void main(String[] args) {
        Unsafe UNSAFE = (Unsafe) ReflectionUtils.getter(Unsafe.class, null, "theUnsafe");

        long l = UNSAFE.allocateMemory(8);

        UNSAFE.putInt(l, -1);
        UNSAFE.putInt(l +4, -1);

        System.out.println(Long.toBinaryString(UNSAFE.getLong(l)));
    }




}
