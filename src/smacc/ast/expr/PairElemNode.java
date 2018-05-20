package smacc.ast.expr;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.types.WACCType;

//  Looks like "fst p" or "snd pair_name"
public class PairElemNode extends ExprNode {

  // Denotes which pair elem to access
  boolean isFst;
  ExprNode expr;

  public PairElemNode(ExprNode expr, boolean isFst, WACCType evaluatedType) {
    super(evaluatedType);
    this.expr = expr;
    this.isFst = isFst;
  }

  public ExprNode getExpr() {
    return expr;
  }

  public boolean getIsFst() {
    return isFst;
  }

  public String toString() {
    return (isFst ? "fst " : "snd ") + expr;
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitPairElemNode(this);
    }
  }

  @Override
  public void translate(Translator translator) {
    translator.translatePairElemNode(this);
  }
}
