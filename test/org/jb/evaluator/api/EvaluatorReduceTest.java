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
    
//    @Test
//    public void testReduceTZ() throws Exception {
//        String source = 
//            "var n = 5\n" + 
//            "var sequence = map({0, n}, i -> (-1)^i / (2.0 * i + 1))\n" + 
//            "var pi = 4 * reduce(sequence, 0, x y -> x + y)\n" +
//            "out pi";
//        setDebug(true);
//        //doTestAST(source, null);
//        doTestEvaluator(source, null);
//    }    
}
