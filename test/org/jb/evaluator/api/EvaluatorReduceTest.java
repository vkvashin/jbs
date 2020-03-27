package org.jb.evaluator.api;

import org.junit.Test;

/**
 *
 * @author vkvashin
 */
public class EvaluatorReduceTest extends EvaluatorTestBase {

//    static {
//        System.setProperty("jbs.suppress.prepare.expressions", "true");
//    }

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
    public void testReduceType1() throws Exception {
        String source =
            "var x = {1, 3}\n" +
            "var y = reduce(x, 0, x y -> x+y+0.1)\n" +
            "out y\n";
        setDebug(true);
        doTestEvaluator(source, "6.3*");
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
}
