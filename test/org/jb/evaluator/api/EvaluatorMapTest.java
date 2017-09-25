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
        //setDebug(true);
        doTestEvaluator(source, "6, 8, 10");
    }

    @Test
    public void testSimpleMapInt2Float() throws Exception {
        String source = 
            "var x = {3, 5}\n" + 
            "var y = map(x, i -> i+0.1)\n" + 
            "out y\n";
        //setDebug(true);
        doTestEvaluator(source, "3.1, 4.1, 5.1");
    }
    
    @Test
    public void testSimpleMapFloat2Int() throws Exception {
        String source = 
            "var x = {0, 2}\n" + 
            "var y = map(x, i -> i+0.1)\n" + 
            "var z = map(y, i -> 1)\n" + 
            "out z\n";
        //setDebug(true);
        doTestEvaluator(source, "1, 1, 1");
    }
    
    @Test
    public void testSimpleMapIntFloat() throws Exception {
        String source = 
            "var x = {3, 5}\n" + 
            "var y = map(x, i -> i+0.1)\n" + 
            "var z = map(y, i -> i*2)\n" + 
            "out z\n";
        //setDebug(true);
        doTestEvaluator(source, "6.2, 8.2, 10.2");
    }

    @Test
    public void testMapOuterSymtabNotVisible() throws Exception {
        String source = 
            "var pi = 3.1415\n" + 
            "var x = {1, 2}\n" + 
            "var y = map(x, i -> i*pi)\n" + 
            "var z = 2*pi\n" + 
            "out y\n";
        setDebug(true);
        doTestEvaluator(source);
        assertDiagnosticEquals(0, 3, 23, "undeclared variable pi");
    }    

    @Test
    public void testMapPow2() throws Exception {
        String source = 
            "var n = 10\n" + 
            "var sequence = map({1, n}, i -> 2^i)\n" + 
            "out sequence\n";
        setDebug(true);
        doTestEvaluator(source, "2, 4, 8, 16, 32, 64, 128, 256, 512, 1024");
        assertEmptyDiagnostics();
    }

    @Test
    public void testMapFromTZ() throws Exception {
        String source = 
            "var n = 3\n" + 
            "var sequence = map({0, n}, i -> (-1)^i / (2.0 * i + 1))\n" + 
            "out sequence\n";
        setDebug(true);
        doTestEvaluator(source, "1.0, -0.3333*");
        assertEmptyDiagnostics();
    }        
}

