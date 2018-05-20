package smacc.ast.expr;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.types.WACCType;

//  Rhs of an assignment - "newpair(x + 1, true)" for example
public class NewPairNode extends ExprNode {

  ExprNode left;
  ExprNode right;

  public NewPairNode(ExprNode left, ExprNode right, WACCType evaluatedType) {
    super(evaluatedType);
    this.left = left;
    this.right = right;
  }

  public ExprNode getLeft() {
    return left;
  }

  public ExprNode getRight() {
    return right;
  }

  // toString of the ExprNodes will be used when visiting them, not here
  public String toString() {
    return "newpair";
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitNewPairNode(this);
    }
    left.visit(listeners);
    right.visit(listeners);
  }

  @Override
  public void translate(Translator translator) {
    translator.translateNewPairNode(this);
  }
}
