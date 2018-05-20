package smacc.ast.stat;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.ast.expr.ExprNode;

/*
 * Can be used for assignment and declaration
 * Just check if the variable is in the symbol table, 
 * and if it isn't, you put it in the next free register, otherwise
 * lookup the register it is in, and put new value in there
 */

//  Of form "int i = 2" or "x = fst p"
public class AssignmentNode extends StatNode {

  private ExprNode lhs;
  private ExprNode rhs;

  public AssignmentNode(ExprNode lhs, ExprNode rhs) {
    this.lhs = lhs;
    this.rhs = rhs;
  }

  public String toString() {
    return "<ASSIGNMENT>";
  }

  public ExprNode getLHS() {
    return lhs;
  }

  public ExprNode getRHS() {
    return rhs;
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitAssignmentNode(this);
    }
    lhs.visit(listeners);
    rhs.visit(listeners);
  }

  @Override
  public void translate(Translator translator) {
    translator.translateAssignmentNode(this);
  }
}
