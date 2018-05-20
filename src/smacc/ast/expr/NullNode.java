package smacc.ast.expr;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.types.WACCType;

//  Used only for pair-literal
public class NullNode extends ExprNode {

  public NullNode(WACCType evaluatedType) {
    super(evaluatedType);
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    // Nothing to do
  }

  @Override
  public String toString() {
    return "<NULL>";
  }

  @Override
  public void translate(Translator translator) {
    translator.translateNullNode(this);
  }
}
