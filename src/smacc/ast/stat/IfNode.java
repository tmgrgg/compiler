package smacc.ast.stat;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.ast.expr.ExprNode;

public class IfNode extends StatNode {
  private ExprNode conditional;
  private StatNode trueBody;
  private StatNode falseBody;

  public IfNode(ExprNode conditional, StatNode trueBody, StatNode falseBody) {
    this.conditional = conditional;
    this.trueBody = trueBody;
    this.falseBody = falseBody;
  }

  public ExprNode getConditional() {
    return conditional;
  }

  public StatNode getTrueBody() {
    return trueBody;
  }

  public StatNode getFalseBody() {
    return falseBody;
  }

  public String toString() {
    return "<IF/ELSE>";
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitIfNode(this);
    };
    conditional.visit(listeners);
    trueBody.visit(listeners);
    falseBody.visit(listeners);
  }

  @Override
  public void translate(Translator translator) {
    translator.translateIfNode(this);
  }
}
