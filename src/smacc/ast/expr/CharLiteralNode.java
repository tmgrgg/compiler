package smacc.ast.expr;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.types.WACCType;

//  A character - 'a', etc.
public class CharLiteralNode extends ExprNode implements ImmediateReplacable {

  private char value;

  public CharLiteralNode(char value, WACCType evaluatedType) {
    super(evaluatedType);
    this.value = value;
  }

  public char getValue() {
    return value;
  }

  public String toString() {
    return ("'" + Character.toString(value) + "'");
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitCharLiteralNode(this);
    }
  }

  @Override
  public void translate(Translator translator) {
    translator.translateCharLiteralNode(this);
  }

  @Override
  public int getIntValue() {
    return (int) value;
  }
}
