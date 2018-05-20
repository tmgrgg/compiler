package smacc.ast.stat;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.ast.expr.ExprNode;

public class ExitNode extends StatNode {

  private ExprNode expr;

  public ExitNode(ExprNode expr) {
    this.expr = expr;
  }

  public String toString() {
    return "<EXIT>";
  }

  public ExprNode getExpr() {
    return expr;
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitExitNode(this);
    }
    expr.visit(listeners);
  }

  @Override
  public void translate(Translator translator) {
    translator.translateExitNode(this);
  }

}
