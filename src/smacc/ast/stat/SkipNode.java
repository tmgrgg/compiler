package smacc.ast.stat;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;

/**
 * Created by tgg14 on 29/11/15.
 */
public class SkipNode extends StatNode {

  public String toString() {
    return "<SKIP>";
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    // Nothing to do
  }

  @Override
  public void translate(Translator translator) {
    translator.translateSkipNode(this);
  }
}
