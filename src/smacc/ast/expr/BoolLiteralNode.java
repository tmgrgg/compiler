package smacc.ast.expr;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.types.WACCType;

//  Either true or false
public class BoolLiteralNode extends ExprNode implements ImmediateReplacable {

  private boolean value;

  public BoolLiteralNode(boolean value) {
    super(WACCType.TYPE_BOOL);
    this.value = value;
  }

  public boolean getValue() {
    return value;
  }

  public String toString() {
    return Boolean.toString(value);
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitBoolLiteralNode(this);
    }
  }

  @Override
  public void translate(Translator translator) {
    translator.translateBoolLiteralNode(this);
  }

  @Override
  public int getSizeInBytes() {
    return 1;
  }

  @Override
  public int getIntValue() {
    return value ? 1 : 0;
  }
}
