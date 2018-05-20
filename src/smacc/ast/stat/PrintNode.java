package smacc.ast.stat;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.ast.expr.ExprNode;

public class PrintNode extends StatNode {

  private ExprNode expr;
  // True iff built from println stat
  private boolean appendNewline;

  public PrintNode(ExprNode expr, boolean appendNewline) {
    this.expr = expr;
    this.appendNewline = appendNewline;
  }

  public String toString() {
    return appendNewline ? "<PRINTLN>" : "<PRINT>";
  }

  public ExprNode getExpr() {
    return expr;
  }

  public boolean getIsPrintln() {
    return appendNewline;
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitPrintNode(this);
    }
    expr.visit(listeners);
  }

  @Override
  public void translate(Translator translator) {
    translator.translatePrintNode(this);
  }
}
