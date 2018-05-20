package smacc.ast.stat;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.ast.expr.ExprNode;
import smacc.ast.smartalloc.LiveRangeAnalyser;

//  Of form "while conditional do body"
public class WhileNode extends StatNode {

  private ExprNode conditional;
  private StatNode body;

  public WhileNode(ExprNode conditional, StatNode body) {
    this.conditional = conditional;
    this.body = body;
  }

  public ExprNode getConditional() {
    return conditional;
  }

  public StatNode getBody() {
    return body;
  }

  public String toString() {
    return "<WHILE>";
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      boolean analysing = false;
      LiveRangeAnalyser analyser = null;
      if (listener instanceof LiveRangeAnalyser) {
        analysing = true;
        analyser = (LiveRangeAnalyser) listener;
        analyser.inWhileLoop();
      }
      listener.visitWhileNode(this);

      if (analysing) {
        analyser.outWhileLoop();
      }
    }

    conditional.visit(listeners);
    body.visit(listeners);

    for (ASTVisitorListener listener : listeners) {
      if (listener instanceof LiveRangeAnalyser) {
        ((LiveRangeAnalyser) listener).outWhileLoop();
      }
    }

  }

  @Override
  public void translate(Translator translator) {
    translator.translateWhileNode(this);
  }
}
