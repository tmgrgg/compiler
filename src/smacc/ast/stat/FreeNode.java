package smacc.ast.stat;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.ast.expr.ExprNode;

public class FreeNode extends StatNode {

  private ExprNode expr;

  public FreeNode(ExprNode expr) {
    this.expr = expr;
  }

  public String toString() {
    return "free";
  }

  public ExprNode getExpr() {
    return expr;
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitFreeNode(this);
    }
    expr.visit(listeners);
  }

  @Override
  public void translate(Translator translator) {
    translator.translateFreeNode(this);
  }
}
