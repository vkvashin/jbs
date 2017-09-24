package org.jb.evaluator.api;

import org.junit.Test;

/**
 *
 * @author vkvashin
 */
public class EvaluatorMapTest extends EvaluatorTestBase {

    @Test
    public void testSimpleMapInt() throws Exception {
        String source = 
            "var x = {3, 5}\n" + 
            "var y = map(x, i -> i*2)\n" + 
            "out y\n";
        setDebug(true);
        doTestEvaluator(source, "6, 8, 10");
    }

    @Test
    public void testSimpleMapInt3Float() throws Exception {
        String source = 
            "var x = {3, 5}\n" + 
            "var y = map(x, i -> i+0.1)\n" + 
            "out y\n";
        setDebug(true);
        doTestEvaluator(source, "3.1, 4.1, 5.1");
    }
}
