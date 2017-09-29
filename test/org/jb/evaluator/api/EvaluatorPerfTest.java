package org.jb.evaluator.api;

import org.junit.Test;

/**
 *
 * @author vkvashin
 */
public class EvaluatorPerfTest extends EvaluatorTestBase {


    static {
        //System.setProperty("jbs.suppress.prepare.expressions", "true");
        System.setProperty("jbs.par.count", "9"); // 100000000 Integer.MAX_VALUE
        System.setProperty("jbs.threads", "4");
    }

    @Test
    public void testCompareReducePerf50mln() throws Exception {
        doTestCompareReducePerf(50000000);
    }

    @Test
    public void testCompareReducePerf100mln() throws Exception {
        doTestCompareReducePerf(100000000);
    }

//    @Test
//    public void testCompareReducePerf200mln() throws Exception {
//        doTestCompareReducePerf(200000000);
//    }

    private void doTestCompareReducePerf(final int cnt) throws Exception {
        String source = 
            "var n = " + cnt + "\n" + 
            "var sequence = map({0, n}, i -> (-1)^i / (2.0 * i + 1))\n" + 
            "var pi = 4 * reduce(sequence, 0, x y -> x + y)\n" +
            "out pi";
        //setDebug(true);
        
        long time1 = System.currentTimeMillis();
        String[] out = doTestEvaluator(source, null/*"3.1415926*"*/);
        time1 = System.currentTimeMillis() - time1;
        
        long time2 = System.currentTimeMillis();
        double[] map = new double[cnt];
        for (int i = 0; i < cnt; i++) {
            map[i] = (double) ((i%2 == 0 ? 1 : -1) / (2.0 * i + 1));
        }        
        double reduce = 0;
        for (int i = 0; i < cnt; i++) {
            reduce += map[i];
        }
        double pi = 4 * reduce;
        time2 = System.currentTimeMillis() - time2;
        System.out.println("----------");
        System.out.println("Iterations count: " + cnt);
        System.out.println("Script: pi=" + out[0]);
        System.out.println("Script time " + time1 + "ms");
        System.out.println("Java: pi=" + pi);
        System.out.println("Java time " + time2 + "ms");
    }    
}

