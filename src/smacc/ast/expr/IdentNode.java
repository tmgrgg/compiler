package smacc.ast.expr;

import smacc.Variable;
import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.types.WACCType;

//  An identifier - "a" or "variable_id" or "ad123"
public class IdentNode extends ExprNode {

  // Variable does not hold a ident so we keep its string here
  private String ident;
  private Variable variable;

  public IdentNode(String ident, Variable variable, WACCType evaluatedType) {
    super(evaluatedType);
    this.variable = variable;
    this.ident = ident;
  }

  public String toString() {
    return ident;
  }

  public Variable getVariable() {
    return this.variable;
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitIdentNode(this);
    }
  }

  @Override
  public void translate(Translator translator) {
    translator.translateIdentNode(this);
  }
}