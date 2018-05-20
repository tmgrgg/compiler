package smacc.ast.stat;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.ast.expr.ExprNode;

public class ReturnNode extends StatNode {

  private ExprNode expr;

  public ReturnNode(ExprNode expr) {
    this.expr = expr;
  }

  public String toString() {
    return "<RETURN>";
  }

  public ExprNode getExpr() {
    return expr;
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitReturnNode(this);
    }
    expr.visit(listeners);
  }

  @Override
  public void translate(Translator translator) {
    translator.translateReturnNode(this);
  }
}
