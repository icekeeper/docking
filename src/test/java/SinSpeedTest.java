import org.apache.commons.math3.util.FastMath;
import org.junit.Test;

import java.util.function.Function;

public class SinSpeedTest {
    @Test
    public void testAsinSpeed() throws Exception {
        long start = System.currentTimeMillis();
        compute(FastMath::asin);
        System.out.println("FastMath: " + (System.currentTimeMillis() - start));
        start = System.currentTimeMillis();
        compute(Math::asin);
        System.out.println("Math: " + (System.currentTimeMillis() - start));
    }

    private void compute(Function<Double, Double> function) {
        for (int i = 0; i < 10000000; i++) {
            function.apply(Math.random());
        }
    }
}
