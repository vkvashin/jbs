package org.jb.evaluator.api;

import org.jb.ast.api.ASTNode;
import org.junit.Test;

/**
 *
 * @author vkvashin
 */
public class EvaluatorSimpleTest extends EvaluatorTestBase {

    @Test
    public void testPrint() throws Exception {
        String toPrint = "Hi there";
        String source = "print \"" + toPrint + "\"";
        //setDebug(true);
        doTestEvaluator(source, toPrint);
    }
    
    @Test
    public void testOut() throws Exception {
        String source = 
            "var x = 5\n" + 
            "out x\n";
        //setDebug(true);
        doTestEvaluator(source, "5");
    }    

    @Test    
    public void testAddInt() throws Exception {
        String source = 
            "var x = 5\n" + 
            "var y = 7\n" + 
            "var z = x + y\n" + 
            "out z\n";
        //setDebug(true);
        doTestEvaluator(source, "12");
    }
    
    @Test    
    public void testAddFloat() throws Exception {
        String source = 
            "var x = 3.14\n" + 
            "var y = 2.71\n" + 
            "var z = x + y\n" + 
            "out z\n";
        //setDebug(true);
        doTestEvaluator(source, "5.8500*");
    }
    
    @Test    
    public void testAddIntFloat() throws Exception {
        String source = 
            "var x = 3\n" + 
            "var y = 2.71\n" + 
            "var z = x + y\n" + 
            "out z\n";
        //setDebug(true);
        doTestEvaluator(source, "5.71*");
    }
    
    @Test    
    public void testSubInt() throws Exception {
        String source = 
            "var x = 15 - 9\n" + 
            "out x\n";
        //setDebug(true);
        doTestEvaluator(source, "6");
    }
    
    @Test    
    public void testSubIntFloat() throws Exception {
        String source = 
            "var x = 15 - 9.99\n" + 
            "out x\n";
        //setDebug(true);
        doTestEvaluator(source, "5.*");
    }    

    @Test    
    public void testMulInt() throws Exception {
        String source = 
            "var x = 22 * 118\n" + 
            "out x\n";
        //setDebug(true);
        doTestEvaluator(source, "2596");
    }

    @Test    
    public void testMulIntFloat() throws Exception {
        String source = 
            "var x = 15 * 9.99\n" + 
            "out x\n";
        //setDebug(true);
        doTestEvaluator(source, "149.*");
    }    
    
    @Test    
    public void testDivInt1() throws Exception {
        String source = 
            "var x = 125 / 25\n" + 
            "out x\n";
        //setDebug(true);
        doTestEvaluator(source, "5");
    }

    @Test    
    public void testDivZero1() throws Exception {
        String source = 
            "var x = 125 / 0\n" + 
            "out x\n";
        //setDebug(true);
        doTestEvaluator(source);
        assertDiagnosticEquals(0, 1, 9, "zero division");
    }

    @Test    
    public void testDivZero2() throws Exception {
        String source = 
            "var x = 125 / (1 + 2 - 3)\n" + 
            "out x\n";
        //setDebug(true);
        doTestEvaluator(source);
        assertDiagnosticEquals(0, 1, 9, "zero division");
    }

    @Test    
    public void testDivInt2() throws Exception {
        String source = 
            "var x = 125 / 20\n" + 
            "out x\n";
        //setDebug(true);
        doTestEvaluator(source, "6");
    }

    @Test    
    public void testDivFloat() throws Exception {
        String source = 
            "var x = 2.7 / 3.14\n" + 
            "out x\n";
        //setDebug(true);
        doTestEvaluator(source, "0.8598*");
    }

    @Test    
    public void testPowInt() throws Exception {
        String source = 
            "var x = 2 ^ 10\n" + 
            "out x\n";
        //setDebug(true);
        doTestEvaluator(source, "1024");
    }

    @Test    
    public void testPowFloat() throws Exception {
        String source = 
            "var x = 0.1 ^ 3\n" + 
            "out x\n";
        //setDebug(true);
        doTestEvaluator(source, "0.001*");
    }

    @Test    
    public void testSeq() throws Exception {
        String source = 
            "var x = {8,11}\n" + 
            "out x\n";
        //setDebug(true);
        doTestEvaluator(source, "8, 9, 10, 11");
    }

    @Test    
    public void testExpr1() throws Exception {
        String source = 
            "var x = 2*3+4+(2*3^4)\n" + 
            "out x\n";
        //setDebug(true);
        doTestEvaluator(source, "172");
    }    
    
    @Test    
    public void simpleTest1() throws Exception {
        String source = 
            "var a = 7\n"+
            "var b = 13\n"+
            "var c = (a + b) * 100\n"+
            "print \"What would you expect?\"\n"+
            "out c\n";
        setDebug(true);
        doTestEvaluator(source, null);
        assertEmptyDiagnostics();
    }    
}
