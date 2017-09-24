/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jb.parser.api;

import java.io.File;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.jb.ast.api.ASTNode;
import org.jb.ast.api.DeclStatement;
import org.jb.ast.api.Expr;
import org.jb.ast.api.Type;
import org.jb.ast.diagnostics.Diagnostic;
import org.jb.lexer.api.Token;
import org.jb.lexer.api.TokenStreamException;
import org.jb.parser.impl.TokenBuffer;
import org.jb.parser.impl.ArrayTokenBuffer;
import org.jb.parser.impl.WindowTokenBuffer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.jb.lexer.api.TokenStream;

/**
 *
 * @author vkvashin
 */
public class ParserTypeTest extends ParserTestBase {

    public ParserTypeTest() {
    }

    @Test
    public void testUndeclaredVar1() throws Exception {
        String source =
                "var x = 500\n" +
                "var y = 3.14 + w\n";
        //setDebug(true);
        getAst(source);        
        assertDiagnosticEquals(0, 2, 16, "undeclared variable w");
    }

    @Test
    public void testUndeclaredVar2() throws Exception {
        String source =
                "var x = 500\n" +
                "var y = 3.14 + w\n" +
                "var w = x\n";
        //setDebug(true);
        getAst(source);        
        assertDiagnosticEquals(0, 2, 16, "undeclared variable w");
    }

    @Test
    public void testDuplicateVar() throws Exception {
        String source =
                "var x = 500\n" +
                "var x = 3.14";
        //setDebug(true);
        getAst(source);        
        assertDiagnosticEquals(0, 2, 5, "duplicate*");
    }

    @Test
    public void testNotIntegerSeq() throws Exception {
        String source = 
            "var x = { 1.2, 2}\n" + 
            "var y = { 3, 4.2}";        
        setDebug(true);
        setPrintTypes(true);
        getAst(source);    
        assertDiagnosticEquals(0, 1, 11, "seqence bound is not an integer*");
        assertDiagnosticEquals(1, 2, 14, "seqence bound is not an integer*");
    }

    @Test
    public void testIntAndSeq() throws Exception {
        String source =
                "var x = 500\n" +
                "var y = {1, 10 }\n" +
                "var z = x + y";
        setDebug(true);
        getAst(source);        
        assertDiagnosticEquals(0, 3, 9, "incompatible operand types*");
    }

    @Test
    public void testSimpleIntMap() throws Exception {
        String source
                = "var n = 500\n"
                + "var sequence = map({0, n}, i -> i*2)\n";
        setDebug(true);
        List<ASTNode> ast = getAstAsList(source);
        doTestAST(ast.get(0), null);
        assertEmptyDiagnostics();
        DeclStatement decl = (DeclStatement) ast.get(1);
        Expr init = decl.getInitializer();
        Type type = init.getType();
        assertTypeEquals(type.SEQ_INT, type);
    }
    
    @Test
    public void testSimpleFloatMap() throws Exception {
        String source
                = "var n = 500\n"
                + "var sequence = map({0, n}, i -> i*0.1)\n";
        setDebug(true);
        List<ASTNode> ast = getAstAsList(source);
        doTestAST(ast.get(0), null);
        assertEmptyDiagnostics();
        DeclStatement decl = (DeclStatement) ast.get(1);
        Expr init = decl.getInitializer();
        Type type = init.getType();
        assertTypeEquals(type.SEQ_FLOAT, type);
    }    
    
    @Test
    public void testPower() throws Exception {
        String source =
                "var x = 2 ^ 3\n" +
                "var y = 2.7 ^ 4\n" +
                "var z = 2 ^ -4" + // this is checked only by runtime
                "var q = 2 ^ 1.4";
        setDebug(true);
        getAst(source);        
        assertDiagnosticEquals(0, 3, 23, "wrong 2-nd operand in operation ^*");
    }
}
