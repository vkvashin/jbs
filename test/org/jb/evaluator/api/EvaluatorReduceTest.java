package org.jb.evaluator.api;

import org.junit.Test;

/**
 *
 * @author vkvashin
 */
public class EvaluatorReduceTest extends EvaluatorTestBase {

    @Test
    public void testReduceError1() throws Exception {
        String source = 
            "var x = {3, 5}\n" + 
            "var y = reduce(\"qwe\", x y -> x*y)\n" + 
            "out y\n";
        setDebug(true);
        //doTestAST(source, null);
        doTestEvaluator(source);
        assertDiagnosticEquals(0, 2, 16, "wrong type*");
    }
    
    @Test
    public void testSimpleReduce1() throws Exception {
        String source = 
            "var x = {1, 3}\n" + 
            "var y = reduce(x, 0, x y -> x+y)\n" + 
            "out y\n";
        setDebug(true);
        doTestEvaluator(source, "6");
        assertEmptyDiagnostics();
    }
    
    @Test
    public void testReduceTZ1() throws Exception {
        String source = 
            "var n = 5\n" + 
            "var sequence = map({0, n}, i -> (-1)^i / (2.0 * i + 1))\n" + 
            "var pi = 4 * reduce(sequence, 0, x y -> x + y)\n" +
            "out pi";
        setDebug(true);
        doTestEvaluator(source, "2.97*");
    }
    
    @Test
    public void testReduceTZ2() throws Exception {
        String source = 
            "var n = 5000\n" + 
            "var sequence = map({0, n}, i -> (-1)^i / (2.0 * i + 1))\n" + 
            "var pi = 4 * reduce(sequence, 0, x y -> x + y)\n" +
            "out pi";
        setDebug(true);
        doTestEvaluator(source, "3.14*");
    }    

    @Test
    public void testReducePerf() throws Exception {
        /*final*/ int cnt = 50000;
        String source = 
            "var n = " + cnt + "\n" + 
            "var sequence = map({0, n}, i -> (-1)^i / (2.0 * i + 1))\n" + 
            "var pi = 4 * reduce(sequence, 0, x y -> x + y)\n" +
            "out pi";
        //setDebug(true);
        
        long time1 = System.currentTimeMillis();
        String[] out = doTestEvaluator(source, null);        
        time1 = System.currentTimeMillis() - time1;

        cnt = 5000000;
        
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
