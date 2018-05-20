package smacc.ast.expr;

import smacc.arm.Translator;
import smacc.ast.ASTVisitorListener;
import smacc.types.WACCType;

//  Of form "!true" or "chr 45"
public class UnaryOpNode extends ExprNode {

  private UnOp op;
  private ExprNode arg;

  public UnaryOpNode(String op, ExprNode arg) {
    switch (op) {
      case ("!"):
        this.op = UnOp.LOGICAL_NOT;
        this.evaluatedType = WACCType.TYPE_BOOL;
        break;
      case ("-"):
        this.op = UnOp.NEGATION;
        this.evaluatedType = WACCType.TYPE_INT;
        break;
      case ("len"):
        this.op = UnOp.LEN;
        this.evaluatedType = WACCType.TYPE_INT;
        break;
      case ("ord"):
        this.op = UnOp.ORD;
        this.evaluatedType = WACCType.TYPE_INT;
        break;
      case ("chr"):
        this.op = UnOp.CHR;
        this.evaluatedType = WACCType.TYPE_CHAR;
        break;
    }
    this.arg = arg;
  }

  public UnOp getOp() {
    return op;
  }

  public ExprNode getArg() {
    return arg;
  }

  @Override
  public String toString() {
    return op.name();
  }

  @Override
  public void visit(ASTVisitorListener... listeners) {
    for (ASTVisitorListener listener : listeners) {
      listener.visitUnaryOpNode(this);
    }
    arg.visit(listeners);
  }

  @Override
  public void translate(Translator translator) {
    translator.translateUnaryOpNode(this);
  }
}
