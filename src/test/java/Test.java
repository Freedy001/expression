/**
 * @author Freedy
 * @date 2022/1/19 11:52
 */
public class Test {

    long b=1;

    public static void main(String[] args) {
        Test test = new Test();
        long temp=0;
        long start=System.currentTimeMillis();
        long a=-1;
        for (long i = 0; i < 100_000_000_00L; i++) {
            temp=a;
            a=i;
        }
        System.out.println(System.currentTimeMillis()-start);
        System.out.println();
        System.out.println(temp);
        System.out.println(a);
        System.out.println(test.b);
    }

}
