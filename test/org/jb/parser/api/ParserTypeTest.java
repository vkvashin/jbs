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
    public void testSimpleMap() throws Exception {
        String source
                = "var n = 500\n"
                + "var sequence = map({0, n}, i -> i*2)\n";
        String[] expected = new String[]{
            "DECL [1:1] n",
            "    INT [1:9] 500",
            "DECL [2:1] sequence",
            "    MAP [2:16] ",
            "        SEQ [2:20] ",
            "            INT [2:21] 0",
            "            ID [2:24] n",
            "        DECL [2:28] i",
            "        OP [2:33] *",
            "            ID [2:33] i",
            "            INT [2:35] 2"
        };
        setDebug(true);
        doTestAST(source, expected);
        assertEmptyDiagnostics();
    }
}
