package smacc.ast.expr;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.types.WACCType;

//  An integer
public class IntLiteralNode extends ExprNode implements ImmediateReplacable {

  private int value;

  public IntLiteralNode(int value) {
    super(WACCType.TYPE_INT);
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public String toString() {
    return Integer.toString(value);
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitIntLiteralNode(this);
    }
  }

  @Override
  public void translate(Translator translator) {
    translator.translateIntLiteralNode(this);
  }

  @Override
  public int getIntValue() {
    return value;
  }
}
