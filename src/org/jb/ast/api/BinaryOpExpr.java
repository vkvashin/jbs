/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jb.ast.api;

import org.jb.ast.diagnostics.DefaultDiagnosticListener;
import org.jb.ast.diagnostics.Diagnostic;
import org.jb.ast.diagnostics.DiagnosticListener;

/**
 *
 * @author vkvashin
 */
public final class BinaryOpExpr extends Expr {

    public enum OpKind {
        ADD('+', 0),
        SUB('-', 0),
        DIV('/', 1),
        MUL('*', 1),
        POW('^', 2);
        public final char id;
        public final byte weight;
        private OpKind(char id, int weight) {
            this.id = id;
            this.weight = (byte) weight; // otherwise we have to cast in all ctors
        }
        public boolean isStronger(OpKind other) {
            return other == null || this.weight > other.weight;
        }
        public boolean isWeaker(OpKind other) {
            return other != null && this.weight < other.weight;
        }
        public char getShortDisplayName() {
            return id;
        }        
    }

    private final OpKind opKind;
    private final Expr left;
    private final Expr right;

    // TODO: should we cache it into a field?
    // Only profiler in "real-like" situation will tell us,
    // whether it is better to calc it now and not waste memory for one more field
    // or vice versa. Now we cache it.
    private final Type type;

    public BinaryOpExpr(int line, int column, OpKind kind, Expr left, Expr right, DiagnosticListener diagnosticListener) {
        super(line, column);
        this.opKind = kind;
        this.left = left;
        this.right = right;
        this.left.setNextSibling(this.right);
        this.right.setNextSibling(null);
        type = calculateType(true, diagnosticListener);
    }

    @Override
    public final NodeKind getNodeKind() {
        return NodeKind.OP;
    }

    public final OpKind getOpKind() {
        return opKind;
    }

    @Override
    public Expr getFirstChild() {
        return left;
    }

    public final Expr getLeft() {
        return left;
    }

    public final Expr getRight() {
        return right;
    }

    @Override
    public Type getType() {
        // TODO: should we calculate it each time?
        return type;
    }
    
    private Type calculateType(boolean reportError, DiagnosticListener diagnosticListener) {
        final Type leftType = left.getType();
        final Type rightType = right.getType();
        if(rightType == Type.ERRONEOUS) {
            return Type.ERRONEOUS;
        }
        switch (leftType) {
            case INT:
                switch (rightType) {                    
                    case INT:
                        return Type.INT;
                    case FLOAT:
                        return Type.FLOAT;
                    default:
                        if (reportError) {
                            reportIncompatibleOpTypes(diagnosticListener, leftType, rightType);
                        }
                        return Type.ERRONEOUS;
                }
            case FLOAT:
                switch (rightType) {                    
                    case INT:
                    case FLOAT:
                        return Type.FLOAT;
                    default:
                        if (reportError) {
                            reportIncompatibleOpTypes(diagnosticListener, leftType, rightType);
                        }
                        return Type.ERRONEOUS;
                }
            case SEQ_INT:
            case SEQ_FLOAT:
            case STRING:
                if (reportError) {
                    reportError(diagnosticListener,
                            "operation " + opKind.getShortDisplayName() + " can not be applied to " + leftType.getDisplayName());
                }                
                return Type.ERRONEOUS;
            case ERRONEOUS:
                return Type.ERRONEOUS;
            default:
                throw new AssertionError(leftType.name());
        }
    }

    private void reportIncompatibleOpTypes(DiagnosticListener diagnosticListener, Type leftType, Type rightType) {
        diagnosticListener.report(
                Diagnostic.error(getLine(), getColumn(),
                        "incompatible operand types in operation " + opKind.getShortDisplayName() + ": " +
                                leftType.getDisplayName() + " and " + rightType.getDisplayName()));
    }

    protected void reportError(DiagnosticListener diagnosticListener, String message) {
        diagnosticListener.report(
                Diagnostic.error(getLine(), getColumn(), message));
    }


    @Override
    public String toString() {
        return super.toString() + opKind.id; // + ' ' + toString(left, right);
    }
}
