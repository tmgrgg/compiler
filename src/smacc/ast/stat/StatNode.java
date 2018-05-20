package smacc.ast.stat;

import smacc.ast.ASTNode;
import smacc.ast.ASTVisitorListener;

public abstract class StatNode extends ASTNode {

  public abstract void visit(ASTVisitorListener... listener);

}
