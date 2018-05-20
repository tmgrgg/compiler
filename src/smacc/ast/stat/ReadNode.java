package smacc.ast.stat;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.ast.expr.ExprNode;

public class ReadNode extends StatNode {

  private ExprNode expr;

  public ReadNode(ExprNode expr) {
    this.expr = expr;
  }

  public String toString() {
    return "read ";
  }

  public ExprNode getExpr() {
    return expr;
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitReadNode(this);
    }
    expr.visit(listeners);
  }

  @Override
  public void translate(Translator translator) {
    translator.translateReadNode(this);
  }
}
