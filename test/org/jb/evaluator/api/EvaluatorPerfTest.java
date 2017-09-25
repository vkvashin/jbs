package org.jb.evaluator.api;

import org.junit.Test;

/**
 *
 * @author vkvashin
 */
public class EvaluatorPerfTest extends EvaluatorTestBase {


    @Test
    public void testCompareReducePerf() throws Exception {
        int cnt = 5000000;
        String source = 
            "var n = " + cnt + "\n" + 
            "var sequence = map({0, n}, i -> (-1)^i / (2.0 * i + 1))\n" + 
            "var pi = 4 * reduce(sequence, 0, x y -> x + y)\n" +
            "out pi";
        //setDebug(true);
        
        long time1 = System.currentTimeMillis();
        String[] out = doTestEvaluator(source, null);        
        time1 = System.currentTimeMillis() - time1;
        
        long time2 = System.currentTimeMillis();
        float[] map = new float[cnt];
        for (int i = 0; i < cnt; i++) {
            map[i] = (float) ((i%2 == 0 ? 1 : -1) / (2.0 * i + 1));
        }        
        float reduce = 0;
        for (int i = 0; i < cnt; i++) {
            reduce += map[i];
        }
        float pi = 4 * reduce;
        time2 = System.currentTimeMillis() - time2;

        System.out.println("Script: pi=" + out[0]);
        System.out.println("Script time " + time1 + "ms");
        System.out.println("Java: pi=" + pi);
        System.out.println("Java time " + time2 + "ms");
    }    
}
